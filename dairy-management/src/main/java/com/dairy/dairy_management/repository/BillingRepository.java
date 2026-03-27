package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.Billing;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billing, Long> {

    Optional<Billing> findByCustomerIdAndMonthAndYear(Long customerId, int month, int year);

    /**
     * Locks the bill row for update — prevents concurrent payments from overpaying.
     * Use this in PaymentService instead of findById().
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Billing b WHERE b.id = :id")
    Optional<Billing> findByIdWithLock(@Param("id") Long id);

    List<Billing> findByCustomerId(Long customerId);

    List<Billing> findByCustomerIdAndStatus(Long customerId, String status);

    Page<Billing> findByMonthAndYear(int month, int year, Pageable pageable);

    Page<Billing> findByStatus(String status, Pageable pageable);

    // Non-paginated versions kept for internal service use (report aggregation etc.)
    List<Billing> findByMonthAndYear(int month, int year);

    List<Billing> findByStatus(String status);
}
