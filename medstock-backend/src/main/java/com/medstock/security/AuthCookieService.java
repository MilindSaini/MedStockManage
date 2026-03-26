package com.medstock.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    public static final String REFRESH_TOKEN_COOKIE = "medstock_rt";

    @Value("${medstock.security.jwt.refresh-token-days:7}")
    private long refreshTokenDays;

    @Value("${medstock.security.jwt.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${medstock.security.jwt.refresh-cookie-same-site:Lax}")
    private String refreshCookieSameSite;

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        long maxAgeSeconds = refreshTokenDays * 24 * 60 * 60;
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
            .httpOnly(true)
            .secure(refreshCookieSecure)
            .path("/api/auth")
            .sameSite(refreshCookieSameSite)
            .maxAge(maxAgeSeconds)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(refreshCookieSecure)
            .path("/api/auth")
            .sameSite(refreshCookieSameSite)
            .maxAge(0)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
            .filter(cookie -> REFRESH_TOKEN_COOKIE.equals(cookie.getName()))
            .map(jakarta.servlet.http.Cookie::getValue)
            .filter(value -> value != null && !value.isBlank())
            .findFirst();
    }
}
