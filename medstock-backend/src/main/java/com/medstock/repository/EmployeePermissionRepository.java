package com.medstock.repository;

import com.medstock.entity.EmployeePermission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeePermissionRepository extends JpaRepository<EmployeePermission, Long> {

    Optional<EmployeePermission> findByStoreIdAndUserId(Long storeId, Long userId);

    void deleteByStoreIdAndUserId(Long storeId, Long userId);
}
