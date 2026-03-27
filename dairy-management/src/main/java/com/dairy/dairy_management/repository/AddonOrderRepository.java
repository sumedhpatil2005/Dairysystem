package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.AddonOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AddonOrderRepository extends JpaRepository<AddonOrder, Long> {

    List<AddonOrder> findByCustomerId(Long customerId);

    List<AddonOrder> findByDeliveryDate(LocalDate date);

    List<AddonOrder> findByCustomerIdAndDeliveryDate(Long customerId, LocalDate date);

    @Query("SELECT a FROM AddonOrder a WHERE a.customer.id = :customerId " +
           "AND FUNCTION('MONTH', a.deliveryDate) = :month " +
           "AND FUNCTION('YEAR', a.deliveryDate) = :year")
    List<AddonOrder> findByCustomerIdAndMonthAndYear(
            @Param("customerId") Long customerId,
            @Param("month") int month,
            @Param("year") int year);
}
