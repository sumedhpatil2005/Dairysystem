package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findBySubscriptionId(Long subscriptionId);
}