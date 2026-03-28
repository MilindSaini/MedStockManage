package com.medstock.repository;

import com.medstock.entity.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
}
