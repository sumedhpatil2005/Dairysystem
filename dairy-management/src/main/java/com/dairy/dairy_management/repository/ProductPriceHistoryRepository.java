package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {

    List<ProductPriceHistory> findByProductIdOrderByEffectiveFromDesc(Long productId);

    // The currently active price entry (no end date)
    Optional<ProductPriceHistory> findByProductIdAndEffectiveToIsNull(Long productId);
}
