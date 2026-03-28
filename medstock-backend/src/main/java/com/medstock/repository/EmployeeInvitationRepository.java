package com.medstock.repository;

import com.medstock.entity.EmployeeInvitation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeInvitationRepository extends JpaRepository<EmployeeInvitation, Long> {

    Optional<EmployeeInvitation> findByStoreIdAndInvitedUserIdAndStatus(Long storeId, Long invitedUserId, String status);

    List<EmployeeInvitation> findByInvitedUserIdAndStatusOrderByCreatedAtDesc(Long invitedUserId, String status);
}
