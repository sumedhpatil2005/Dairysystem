package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {
    Optional<DeliveryPartner> findByUserId(Long userId);
    boolean existsByPhone(String phone);
}
