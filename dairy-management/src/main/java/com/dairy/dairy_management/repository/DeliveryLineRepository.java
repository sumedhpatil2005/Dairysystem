package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.DeliveryLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryLineRepository extends JpaRepository<DeliveryLine, Long> {
    boolean existsByName(String name);
}
