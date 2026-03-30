package com.medstock.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final MediaType FORM_MEDIA_TYPE = MediaType.get("application/x-www-form-urlencoded");
    private static final java.util.regex.Pattern E164_PATTERN = java.util.regex.Pattern.compile("^\\+[1-9]\\d{6,14}$");
    private static final Pattern TWILIO_MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TWILIO_CODE_PATTERN = Pattern.compile("\"code\"\\s*:\\s*(\\d+)");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final OkHttpClient httpClient = new OkHttpClient();

    @Value("${medstock.notifications.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${medstock.notifications.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${medstock.notifications.twilio.from-phone:}")
    private String twilioFromPhone;

    @Value("${medstock.notifications.twilio.from-whatsapp:}")
    private String twilioFromWhatsapp;

    @Value("${medstock.notifications.default-country-code:+91}")
    private String defaultCountryCode;

    @Value("${medstock.env.mail-username:}")
    private String fromEmail;

    public record SmsSendResult(
        boolean attempted,
        boolean accepted,
        String to,
        String reason
    ) {
    }

    public void sendSms(String phone, String msg) {
        sendSmsWithResult(phone, msg);
    }

    public SmsSendResult sendWhatsApp(String phone, String msg) {
        if (isBlank(phone) || isBlank(msg)) {
            return new SmsSendResult(false, false, normalizePhone(phone), "phone_or_message_blank");
        }

        if (!isTwilioConfigured()) {
            return new SmsSendResult(false, false, normalizePhone(phone), "twilio_not_configured");
        }

        if (isBlank(twilioFromWhatsapp)) {
            return new SmsSendResult(false, false, normalizePhone(phone), "twilio_whatsapp_sender_not_configured");
        }

        String to = normalizePhone(phone);
        if (!isValidE164(to)) {
            return new SmsSendResult(false, false, to, "invalid_destination_phone");
        }

        String from = twilioFromWhatsapp.trim().startsWith("whatsapp:")
            ? twilioFromWhatsapp.trim()
            : "whatsapp:" + twilioFromWhatsapp.trim();

        return sendTwilioMessage("whatsapp:" + to, from, msg, "WHATSAPP");
    }

    public SmsSendResult sendSmsWithResult(String phone, String msg) {
        if (isBlank(phone) || isBlank(msg)) {
            log.debug("SMS skipped: phone or message is blank");
            return new SmsSendResult(false, false, normalizePhone(phone), "phone_or_message_blank");
        }

        if (!isTwilioConfigured()) {
            log.warn("SMS skipped: Twilio SID/Auth token is not configured");
            return new SmsSendResult(false, false, normalizePhone(phone), "twilio_not_configured");
        }

        String to = normalizePhone(phone);
        String from = normalizePhone(twilioFromPhone);

        if (!isValidE164(to)) {
            log.warn("SMS skipped: destination phone '{}' is not in E.164 format", phone);
            return new SmsSendResult(false, false, to, "invalid_destination_phone");
        }

        if (!isValidE164(from)) {
            log.warn("SMS skipped: Twilio from-phone is missing or invalid (expected E.164)");
            return new SmsSendResult(false, false, to, "invalid_twilio_from_phone");
        }

        return sendTwilioMessage(to, from, msg, "SMS");
    }

    public void sendEmail(String to, String subject, String html) {
        if (isBlank(to) || isBlank(subject) || isBlank(html)) {
            return;
        }

        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to.trim());
            helper.setSubject(subject.trim());
            helper.setText(html, true);
            if (!isBlank(fromEmail)) {
                helper.setFrom(fromEmail.trim());
            }
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Email send failed to {}", to, ex);
        }
    }

    public String renderTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }

    private SmsSendResult sendTwilioMessage(String to, String from, String body, String channel) {
        String endpoint = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json";
        String payload = "To=" + urlEncode(to)
            + "&From=" + urlEncode(from)
            + "&Body=" + urlEncode(body);

        Request request = new Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", Credentials.basic(twilioAccountSid, twilioAuthToken))
            .post(RequestBody.create(FORM_MEDIA_TYPE, payload))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.warn("{} send failed with status {} and response {}", channel, response.code(), errorBody);
                String reason = buildTwilioHttpReason(response.code(), errorBody);
                return new SmsSendResult(true, false, to, reason);
            } else {
                log.info("{} send request accepted by Twilio", channel);
                return new SmsSendResult(true, true, to, "accepted");
            }
        } catch (Exception ex) {
            log.warn("{} send failed", channel, ex);
            return new SmsSendResult(true, false, to, "transport_error");
        }
    }

    private boolean isTwilioConfigured() {
        return !isBlank(twilioAccountSid) && !isBlank(twilioAuthToken);
    }

    private String normalizePhone(String phone) {
        if (isBlank(phone)) {
            return "";
        }

        String normalized = phone.trim().replaceAll("[\\s()-]", "");

        if (normalized.startsWith("00")) {
            normalized = "+" + normalized.substring(2);
        }

        if (!normalized.startsWith("+") && normalized.matches("^\\d{10}$")) {
            String code = isBlank(defaultCountryCode) ? "+91" : defaultCountryCode.trim();
            if (!code.startsWith("+")) {
                code = "+" + code;
            }
            normalized = code + normalized;
        }

        if (!normalized.startsWith("+") && normalized.matches("^[1-9]\\d{10,14}$")) {
            normalized = "+" + normalized;
        }

        return normalized;
    }

    private String urlEncode(String input) {
        return java.net.URLEncoder.encode(input, java.nio.charset.StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidE164(String phone) {
        if (isBlank(phone)) {
            return false;
        }
        return E164_PATTERN.matcher(phone).matches();
    }

    private String buildTwilioHttpReason(int status, String errorBody) {
        if (isBlank(errorBody)) {
            return "twilio_http_" + status;
        }

        Matcher codeMatcher = TWILIO_CODE_PATTERN.matcher(errorBody);
        Matcher messageMatcher = TWILIO_MESSAGE_PATTERN.matcher(errorBody);

        String twilioCode = codeMatcher.find() ? codeMatcher.group(1) : null;
        String twilioMessage = messageMatcher.find() ? sanitizeReasonText(messageMatcher.group(1)) : null;

        if (twilioCode != null && twilioMessage != null) {
            return "twilio_http_" + status + "_code_" + twilioCode + ": " + twilioMessage;
        }

        if (twilioMessage != null) {
            return "twilio_http_" + status + ": " + twilioMessage;
        }

        return "twilio_http_" + status;
    }

    private String sanitizeReasonText(String text) {
        return text.replace("\\\\\"", "\"").replace("\\n", " ").trim();
    }
}
