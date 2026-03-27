package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    List<Customer> findByIsActiveTrue();

    List<Customer> findByIsActiveFalse();

    // Search across name, phone, societyName — all filters are optional
    @Query("""
            SELECT c FROM Customer c
            WHERE c.isActive = true
              AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:phone IS NULL OR c.phone LIKE CONCAT('%', :phone, '%'))
              AND (:society IS NULL OR LOWER(c.societyName) LIKE LOWER(CONCAT('%', :society, '%')))
              AND (:lineId IS NULL OR c.deliveryLine.id = :lineId)
            ORDER BY c.name ASC
            """)
    List<Customer> search(@Param("name") String name,
                          @Param("phone") String phone,
                          @Param("society") String society,
                          @Param("lineId") Long lineId);
}
