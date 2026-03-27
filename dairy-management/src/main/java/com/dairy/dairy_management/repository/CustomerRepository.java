package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    // Paginated — used by GET /customers
    Page<Customer> findByIsActiveTrue(Pageable pageable);

    // Non-paginated — used internally by search (in-memory filter) and delivery generation
    List<Customer> findByIsActiveTrue();

    List<Customer> findByIsActiveFalse();
}
