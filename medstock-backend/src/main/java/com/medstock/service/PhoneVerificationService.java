package com.medstock.service;

import com.medstock.entity.PhoneVerificationOtp;
import com.medstock.entity.User;
import com.medstock.repository.PhoneVerificationOtpRepository;
import com.medstock.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;

    private final PhoneVerificationOtpRepository phoneVerificationOtpRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${medstock.notifications.default-country-code:+91}")
    private String defaultCountryCode;

    public NotificationService.SmsSendResult sendOtp(User user, String requestedPhone) {
        String phone = normalizeNullable(requestedPhone);
        if (phone == null) {
            phone = normalizeNullable(user.getPhone());
        }

        if (phone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is missing in profile");
        }

        String otp = generateOtp();
        NotificationService.SmsSendResult sendResult = notificationService.sendSmsWithResult(
            phone,
            "Your MedStock verification OTP is " + otp + ". It expires in 10 minutes."
        );

        if (!sendResult.accepted()) {
            return sendResult;
        }

        PhoneVerificationOtp entity = new PhoneVerificationOtp();
        entity.setUserId(user.getId());
        entity.setPhone(sendResult.to());
        entity.setOtpCode(otp);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        entity.setCreatedAt(LocalDateTime.now());
        phoneVerificationOtpRepository.save(entity);

        return sendResult;
    }

    public void verifyOtp(User user, String otp) {
        PhoneVerificationOtp latestOtp = phoneVerificationOtpRepository
            .findTopByUserIdOrderByCreatedAtDesc(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No OTP found for this phone"));

        String otpPhone = normalizePhone(latestOtp.getPhone());
        if (otpPhone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP phone is invalid");
        }

        if (latestOtp.getVerifiedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP already used");
        }

        if (latestOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired");
        }

        int attempts = latestOtp.getAttemptCount() == null ? 0 : latestOtp.getAttemptCount();
        if (attempts >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many invalid OTP attempts");
        }

        if (!latestOtp.getOtpCode().equals(otp.trim())) {
            latestOtp.setAttemptCount(attempts + 1);
            phoneVerificationOtpRepository.save(latestOtp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        latestOtp.setVerifiedAt(LocalDateTime.now());
        phoneVerificationOtpRepository.save(latestOtp);

        // Bind verification to the phone used for OTP so profile edits are not required before verify.
        user.setPhone(otpPhone);
        user.setPhoneVerified(Boolean.TRUE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private String generateOtp() {
        int min = 100000;
        int max = 1000000;
        return String.valueOf(secureRandom.nextInt(max - min) + min);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String normalized = phone.trim().replaceAll("[\\s()-]", "");
        if (normalized.startsWith("00")) {
            normalized = "+" + normalized.substring(2);
        }

        if (!normalized.startsWith("+") && normalized.matches("^\\d{10}$")) {
            String code = defaultCountryCode == null || defaultCountryCode.isBlank()
                ? "+91"
                : defaultCountryCode.trim();
            if (!code.startsWith("+")) {
                code = "+" + code;
            }
            normalized = code + normalized;
        }

        if (!normalized.startsWith("+") && normalized.matches("^[1-9]\\d{10,14}$")) {
            normalized = "+" + normalized;
        }

        if (!normalized.matches("^\\+[1-9]\\d{6,14}$")) {
            return null;
        }

        return normalized;
    }
}
