package com.medstock.repository;

import com.medstock.entity.AiProviderConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderConfigRepository extends JpaRepository<AiProviderConfig, Long> {

	List<AiProviderConfig> findByActiveTrue();

	Optional<AiProviderConfig> findFirstByUseCaseIgnoreCaseAndActiveTrueOrderByUpdatedAtDesc(String useCase);

	Optional<AiProviderConfig> findFirstByProviderKeyIgnoreCaseAndUseCaseIgnoreCaseOrderByUpdatedAtDesc(
		String providerKey,
		String useCase
	);
}
