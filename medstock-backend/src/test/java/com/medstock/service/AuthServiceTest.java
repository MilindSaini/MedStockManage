package com.medstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.medstock.dto.auth.AuthLoginRequest;
import com.medstock.dto.auth.AuthRegisterRequest;
import com.medstock.dto.auth.AuthResponse;
import com.medstock.dto.auth.AuthUserResponse;
import com.medstock.entity.User;
import com.medstock.repository.StoreRepository;
import com.medstock.repository.UserRepository;
import com.medstock.security.JwtUtil;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenSessionService refreshTokenSessionService;

    @Mock
    private OAuthLoginCodeService oAuthLoginCodeService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            storeRepository,
            passwordEncoder,
            authenticationConfiguration,
            jwtUtil,
            refreshTokenSessionService,
            oAuthLoginCodeService,
            activityLogService
        );
    }

    @Test
    void registerShouldCreateUserAndIssueTokens() {
        AuthRegisterRequest request = new AuthRegisterRequest("owner_user", "owner@medstock.com", "Password1");

        when(userRepository.existsByEmailIgnoreCase("owner@medstock.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("owner_user")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        mockTokenFlow("access-token", "refresh-token", 900L);

        AuthResponse response = authService.register(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900L, response.expiresIn());
        assertNotNull(response.user());
        assertEquals("owner_user", response.user().username());
        assertEquals("owner@medstock.com", response.user().email());

        verify(activityLogService).log(anyLong(), any(), anyString(), anyString(), anyLong(), any(Map.class));
        verify(refreshTokenSessionService).storeRefreshToken(anyString(), anyLong(), anyString(), any(Instant.class));
    }

    @Test
    void loginShouldRejectInvalidCredentials() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        doThrow(new BadCredentialsException("bad credentials"))
            .when(authenticationManager)
            .authenticate(any());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(new AuthLoginRequest("bad-user", "wrong"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginShouldReturnTokensForValidCredentials() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        User existing = new User();
        existing.setId(21L);
        existing.setUsername("employee");
        existing.setEmail("employee@medstock.com");
        existing.setRole("EMPLOYEE");
        existing.setIsActive(true);

        when(userRepository.findByUsernameOrEmail("employee", "employee")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockTokenFlow("access-login", "refresh-login", 600L);

        AuthResponse response = authService.login(new AuthLoginRequest("employee", "Password1"));

        assertEquals("access-login", response.accessToken());
        assertEquals("refresh-login", response.refreshToken());
        assertEquals("employee", response.user().username());
    }

    @Test
    void exchangeOAuthCodeShouldFailWhenCodeIsInvalid() {
        when(oAuthLoginCodeService.consumeCode("expired")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> authService.exchangeOAuthCode("expired")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void exchangeOAuthCodeShouldReturnAuthPayloadWhenCodeIsValid() {
        AuthResponse expected = new AuthResponse(
            "oauth-access",
            "oauth-refresh",
            "Bearer",
            900,
            new AuthUserResponse(1L, null, null, "oauthuser", "oauth@medstock.com", null, null, false, "EMPLOYEE", java.util.List.of("EMPLOYEE"))
        );
        when(oAuthLoginCodeService.consumeCode("valid")).thenReturn(Optional.of(expected));

        AuthResponse response = authService.exchangeOAuthCode("valid");

        assertEquals("oauth-access", response.accessToken());
        assertEquals("oauth-refresh", response.refreshToken());
    }

    private void mockTokenFlow(String accessToken, String refreshToken, long expiresIn) {
        DecodedJWT decodedJWT = org.mockito.Mockito.mock(DecodedJWT.class);
        when(jwtUtil.generateAccessToken(any(User.class))).thenReturn(accessToken);
        when(jwtUtil.generateRefreshToken(any(User.class), anyString())).thenReturn(refreshToken);
        when(jwtUtil.extractClaims(refreshToken)).thenReturn(decodedJWT);
        when(decodedJWT.getExpiresAt()).thenReturn(Date.from(Instant.now().plusSeconds(3600)));
        when(jwtUtil.getAccessTokenExpiresInSeconds()).thenReturn(expiresIn);
    }
}
