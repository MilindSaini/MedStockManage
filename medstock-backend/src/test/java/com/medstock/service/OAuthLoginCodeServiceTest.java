package com.medstock.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.medstock.dto.auth.AuthResponse;
import com.medstock.dto.auth.AuthUserResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class OAuthLoginCodeServiceTest {

    private final OAuthLoginCodeService service = new OAuthLoginCodeService();

    @Test
    void consumeCodeShouldSucceedOnceThenInvalidateCode() {
        AuthResponse authResponse = new AuthResponse(
            "access",
            "refresh",
            "Bearer",
            900,
            new AuthUserResponse(1L, null, null, "user", "user@medstock.com", null, null, false, "EMPLOYEE", List.of("EMPLOYEE"))
        );

        String code = service.createCode(authResponse);

        assertTrue(service.consumeCode(code).isPresent());
        assertFalse(service.consumeCode(code).isPresent());
    }

    @Test
    void consumeCodeShouldRejectBlankCode() {
        assertFalse(service.consumeCode(" ").isPresent());
    }
}
