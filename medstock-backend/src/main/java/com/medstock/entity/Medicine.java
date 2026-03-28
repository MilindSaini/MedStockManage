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
@Table(name = "medicines")
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "generic_name")
    private String genericName;

    private String category;

    private String manufacturer;

    @Column(name = "sku_code")
    private String skuCode;

    @Column(nullable = false)
    private String unit = "pcs";

    @Column(nullable = false)
    private BigDecimal mrp = BigDecimal.ZERO;

    @Column(name = "purchase_price", nullable = false)
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable = 0;

    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 10;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    @Column(name = "active_batch_id")
    private Long activeBatchId;

    @Column(name = "current_stock", insertable = false, updatable = false)
    private Integer currentStock;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
