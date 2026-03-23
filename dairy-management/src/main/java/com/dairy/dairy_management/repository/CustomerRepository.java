package com.dairy.dairy_management.repository;
import java.util.Optional;
import com.dairy.dairy_management.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);
}
