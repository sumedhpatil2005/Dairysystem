package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.DeliveryOverride;
import com.dairy.dairy_management.entity.OverrideType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DeliveryOverrideRepository extends JpaRepository<DeliveryOverride, Long> {

    List<DeliveryOverride> findByCustomerId(Long customerId);

    List<DeliveryOverride> findByDate(LocalDate date);

    List<DeliveryOverride> findByCustomerIdAndDate(Long customerId, LocalDate date);

    Optional<DeliveryOverride> findByCustomerIdAndDateAndOverrideType(
            Long customerId, LocalDate date, OverrideType overrideType);
}
