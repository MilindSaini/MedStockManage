package com.medstock.repository;

import com.medstock.entity.PhoneVerificationOtp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationOtpRepository extends JpaRepository<PhoneVerificationOtp, Long> {

    Optional<PhoneVerificationOtp> findTopByUserIdAndPhoneOrderByCreatedAtDesc(Long userId, String phone);

    Optional<PhoneVerificationOtp> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
