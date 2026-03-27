package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.Billing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    Optional<Billing> findByCustomerIdAndMonthAndYear(Long customerId, int month, int year);

    List<Billing> findByCustomerId(Long customerId);

    List<Billing> findByCustomerIdAndStatus(Long customerId, String status);

    List<Billing> findByMonthAndYear(int month, int year);

    List<Billing> findByStatus(String status);
}
