package com.medstock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_provider_config")
public class AiProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "provider_key", nullable = false)
    private String providerKey;

    @Column(name = "use_case", nullable = false)
    private String useCase = "DEFAULT";

    @Column(name = "model")
    private String model;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "api_key_env_var")
    private String apiKeyEnvVar;

    @Column(nullable = false)
    private Boolean active = Boolean.FALSE;

    @Column(name = "last_test_status")
    private String lastTestStatus;

    @Column(name = "last_tested_at")
    private LocalDateTime lastTestedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
