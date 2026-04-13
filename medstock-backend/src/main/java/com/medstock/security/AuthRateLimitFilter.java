package com.medstock.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_KEY = "POST:/api/auth/login";
    private static final String REGISTER_KEY = "POST:/api/auth/register";
    private static final String REFRESH_KEY = "POST:/api/auth/refresh";
    private static final String OAUTH_EXCHANGE_KEY = "GET:/api/auth/oauth2/exchange";

    private final AuthRateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(AuthRateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        return resolveEndpointKey(request) == null;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String endpointKey = resolveEndpointKey(request);
        if (endpointKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(bucketKey(endpointKey, request), key ->
            Bucket.builder().addLimit(resolveLimit(endpointKey)).build()
        );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write("{\"message\":\"Too many authentication requests. Please retry later.\"}");
    }

    private String bucketKey(String endpointKey, HttpServletRequest request) {
        return endpointKey + ":" + clientKey(request);
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String first = forwardedFor.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    private Bandwidth resolveLimit(String endpointKey) {
        AuthRateLimitProperties.Endpoint endpoint = switch (endpointKey) {
            case LOGIN_KEY -> properties.getLogin();
            case REGISTER_KEY -> properties.getRegister();
            case REFRESH_KEY -> properties.getRefresh();
            case OAUTH_EXCHANGE_KEY -> properties.getOauthExchange();
            default -> properties.getLogin();
        };

        return Bandwidth.builder()
            .capacity(endpoint.getCapacity())
            .refillGreedy(endpoint.getCapacity(), Duration.ofSeconds(endpoint.getRefillSeconds()))
            .build();
    }

    private String resolveEndpointKey(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String method = request.getMethod();

        if (HttpMethod.POST.matches(method) && "/api/auth/login".equals(servletPath)) {
            return LOGIN_KEY;
        }
        if (HttpMethod.POST.matches(method) && "/api/auth/register".equals(servletPath)) {
            return REGISTER_KEY;
        }
        if (HttpMethod.POST.matches(method) && "/api/auth/refresh".equals(servletPath)) {
            return REFRESH_KEY;
        }
        if (HttpMethod.GET.matches(method) && "/api/auth/oauth2/exchange".equals(servletPath)) {
            return OAUTH_EXCHANGE_KEY;
        }
        return null;
    }
}