package com.medstock.service;

import com.medstock.dto.admin.AiProviderAdminResponse;
import com.medstock.dto.admin.AiProviderTestResponse;
import com.medstock.dto.admin.UpdateAiProviderRequest;
import com.medstock.entity.AiProviderConfig;
import com.medstock.repository.AiProviderConfigRepository;
import com.medstock.security.UserPrincipal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AiProviderAdminService {

    private final AiProviderConfigRepository aiProviderConfigRepository;
    private final AiProviderFactory aiProviderFactory;
    private final ActivityLogService activityLogService;

    public List<AiProviderAdminResponse> list() {
        return aiProviderConfigRepository.findAll().stream()
            .map(AiProviderAdminResponse::from)
            .toList();
    }

    public AiProviderAdminResponse update(Long id, UpdateAiProviderRequest request, UserPrincipal principal) {
        AiProviderConfig config = aiProviderConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI provider not found"));

        config.setName(request.name().trim());
        if (config.getProviderKey() == null || config.getProviderKey().isBlank()) {
            config.setProviderKey(request.name().trim().toUpperCase(Locale.ROOT));
        }
        if (config.getUseCase() == null || config.getUseCase().isBlank()) {
            config.setUseCase("DEFAULT");
        }
        config.setBaseUrl(request.baseUrl().trim());
        // API keys are sourced from environment variables, not persisted in DB.
        config.setApiKey(null);
        if (request.active() != null) {
            config.setActive(request.active());
        }
        config.setUpdatedAt(LocalDateTime.now());

        AiProviderConfig saved = aiProviderConfigRepository.save(config);
        aiProviderFactory.reloadFromDatabase();
        activityLogService.log(
            principal.getId(),
            principal.getStoreId(),
            "ADMIN_AI_PROVIDER_UPDATED",
            "AI_PROVIDER",
            saved.getId(),
            Map.of("name", saved.getName(), "active", saved.getActive())
        );
        return AiProviderAdminResponse.from(saved);
    }

    public AiProviderTestResponse test(Long id, UserPrincipal principal) {
        AiProviderConfig config = aiProviderConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI provider not found"));

        int statusCode = 0;
        boolean ok = false;
        String message;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .timeout(Duration.ofSeconds(5))
                .uri(URI.create(config.getBaseUrl()))
                .build();

            HttpResponse<Void> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
            statusCode = response.statusCode();
            ok = statusCode >= 200 && statusCode < 500;
            message = ok ? "Probe successful" : "Probe failed";
        } catch (Exception exception) {
            message = exception.getMessage() == null ? "Probe failed" : exception.getMessage();
        }

        config.setLastTestStatus(ok ? "OK" : "FAILED");
        config.setLastTestedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        aiProviderConfigRepository.save(config);
        aiProviderFactory.reloadFromDatabase();

        activityLogService.log(
            principal.getId(),
            principal.getStoreId(),
            "ADMIN_AI_PROVIDER_TESTED",
            "AI_PROVIDER",
            config.getId(),
            Map.of("ok", ok, "statusCode", statusCode)
        );

        return new AiProviderTestResponse(config.getId(), config.getName(), ok, statusCode, message);
    }

    public AiProviderAdminResponse activate(Long id, UserPrincipal principal) {
        AiProviderConfig target = aiProviderConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI provider not found"));

        List<AiProviderConfig> providers = aiProviderConfigRepository.findAll();
        for (AiProviderConfig provider : providers) {
            provider.setActive(provider.getId().equals(id));
            provider.setUpdatedAt(LocalDateTime.now());
        }
        aiProviderConfigRepository.saveAll(providers);

        aiProviderFactory.clearCache();
        aiProviderFactory.reloadFromDatabase();

        activityLogService.log(
            principal.getId(),
            principal.getStoreId(),
            "ADMIN_AI_PROVIDER_ACTIVATED",
            "AI_PROVIDER",
            id,
            Map.of("name", target.getName())
        );

        return AiProviderAdminResponse.from(
            aiProviderConfigRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI provider not found"))
        );
    }
}
