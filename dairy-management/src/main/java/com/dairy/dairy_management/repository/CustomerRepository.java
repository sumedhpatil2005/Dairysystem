package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    List<Customer> findByIsActiveTrue();

    List<Customer> findByIsActiveFalse();
}
