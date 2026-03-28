package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findBySubscriptionId(Long subscriptionId);

    List<Delivery> findBySubscription_CustomerIdAndDeliveryDate(Long customerId, LocalDate date);

    Page<Delivery> findByDeliveryDate(LocalDate date, Pageable pageable);

    // Non-paginated — used by report service and generate flow
    List<Delivery> findByDeliveryDate(LocalDate date);

    List<Delivery> findBySubscription_CustomerId(Long customerId);

    // Date-range query — used by BillingService to avoid in-memory month/year filtering
    List<Delivery> findBySubscription_CustomerIdAndDeliveryDateBetween(
            Long customerId, LocalDate start, LocalDate end);

    boolean existsBySubscriptionIdAndDeliveryDate(Long subscriptionId, LocalDate date);

    // Partner monthly report — all deliveries for a line within a date range
    List<Delivery> findBySubscription_Customer_DeliveryLine_IdAndDeliveryDateBetween(
            Long lineId, LocalDate start, LocalDate end);
}
