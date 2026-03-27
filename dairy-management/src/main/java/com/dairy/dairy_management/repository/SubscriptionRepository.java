package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByCustomerId(Long customerId);

    // Active subscriptions for a customer (endDate is null = never cancelled)
    List<Subscription> findByCustomerIdAndEndDateIsNull(Long customerId);

    // All subscriptions active on a given date — used by delivery generation (Goal 5)
    List<Subscription> findByStartDateLessThanEqualAndEndDateIsNull(LocalDate date);

    List<Subscription> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate startDate, LocalDate endDate);
}
