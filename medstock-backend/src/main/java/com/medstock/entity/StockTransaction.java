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
@Table(name = "stock_transactions")
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "medicine_id", nullable = false)
    private Long medicineId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private Integer delta;

    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
