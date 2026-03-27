package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.AddonOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AddonOrderRepository extends JpaRepository<AddonOrder, Long> {

    List<AddonOrder> findByCustomerId(Long customerId);

    List<AddonOrder> findByDeliveryDate(LocalDate date);

    List<AddonOrder> findByCustomerIdAndDeliveryDate(Long customerId, LocalDate date);

    // Date-range query — used by BillingService to avoid in-memory month/year filtering
    List<AddonOrder> findByCustomerIdAndDeliveryDateBetween(
            Long customerId, LocalDate start, LocalDate end);
}
