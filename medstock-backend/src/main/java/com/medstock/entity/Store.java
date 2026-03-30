package com.medstock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "subscription_status", nullable = false)
    private String subscriptionStatus = "TRIAL";

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "expiry_alert_time", nullable = false)
    private LocalTime expiryAlertTime = LocalTime.of(8, 0);

    @Column(name = "low_stock_alert_time", nullable = false)
    private LocalTime lowStockAlertTime = LocalTime.of(8, 30);

    @Column(name = "out_of_stock_alert_time", nullable = false)
    private LocalTime outOfStockAlertTime = LocalTime.of(9, 0);

    @Column(name = "batch_promotion_time", nullable = false)
    private LocalTime batchPromotionTime = LocalTime.of(6, 0);

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
