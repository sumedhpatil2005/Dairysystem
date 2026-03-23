package com.dairy.dairy_management.repository;


import com.dairy.dairy_management.entity.Billing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingRepository extends JpaRepository<Billing, Long> {

}
