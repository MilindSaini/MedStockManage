package com.medstock.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "medstock.security.rate-limit")
public class AuthRateLimitProperties {

    private boolean enabled = true;

    @Valid
    private Endpoint register = new Endpoint(5, 60);

    @Valid
    private Endpoint login = new Endpoint(10, 60);

    @Valid
    private Endpoint refresh = new Endpoint(20, 60);

    @Valid
    private Endpoint oauthExchange = new Endpoint(10, 60);

    @Getter
    @Setter
    public static class Endpoint {

        @Min(1)
        private int capacity;

        @Min(1)
        private int refillSeconds;

        public Endpoint() {
        }

        public Endpoint(int capacity, int refillSeconds) {
            this.capacity = capacity;
            this.refillSeconds = refillSeconds;
        }
    }
}