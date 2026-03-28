package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.DeliverySlot;
import com.dairy.dairy_management.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByCustomerId(Long customerId);

    // Active subscriptions for a customer (endDate is null = never cancelled)
    List<Subscription> findByCustomerIdAndEndDateIsNull(Long customerId);

    // All subscriptions active on a given date — used by delivery generation
    List<Subscription> findByStartDateLessThanEqualAndEndDateIsNull(LocalDate date);

    List<Subscription> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate startDate, LocalDate endDate);

    // #8 — Overlap check: is there already an active subscription for this
    //       customer + product + slot combination?
    boolean existsByCustomerIdAndProductIdAndDeliverySlotAndEndDateIsNull(
            Long customerId, Long productId, DeliverySlot deliverySlot);

    // #6 — Product deletion guard: does any active subscription use this product?
    boolean existsByProductIdAndEndDateIsNull(Long productId);
}
