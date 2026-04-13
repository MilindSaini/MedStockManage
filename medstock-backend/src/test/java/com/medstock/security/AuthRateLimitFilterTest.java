package com.medstock.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

    @Test
    void shouldReturn429WhenLoginRateLimitIsExceeded() throws ServletException, IOException {
        AuthRateLimitProperties properties = new AuthRateLimitProperties();
        properties.getLogin().setCapacity(1);
        properties.getLogin().setRefillSeconds(60);

        AuthRateLimitFilter filter = new AuthRateLimitFilter(properties);

        MockHttpServletRequest firstRequest = buildLoginRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        AtomicInteger invoked = new AtomicInteger(0);
        FilterChain passThroughChain = (request, response) -> invoked.incrementAndGet();

        filter.doFilter(firstRequest, firstResponse, passThroughChain);

        assertEquals(1, invoked.get());
        assertEquals(200, firstResponse.getStatus());

        MockHttpServletRequest secondRequest = buildLoginRequest();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        FilterChain blockedChain = (request, response) -> invoked.incrementAndGet();

        filter.doFilter(secondRequest, secondResponse, blockedChain);

        assertEquals(429, secondResponse.getStatus());
        assertTrue(secondResponse.getContentAsString().contains("Too many authentication requests"));
        assertNotNull(secondResponse.getHeader("Retry-After"));
        assertEquals(1, invoked.get());
    }

    private MockHttpServletRequest buildLoginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServletPath("/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
