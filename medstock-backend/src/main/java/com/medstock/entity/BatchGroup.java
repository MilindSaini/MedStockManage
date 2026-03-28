package com.medstock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "batch_groups")
public class BatchGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "medicine_id", nullable = false)
    private Long medicineId;

    @Column(name = "batch_code", nullable = false)
    private String batchCode;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "purchase_price", nullable = false)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
