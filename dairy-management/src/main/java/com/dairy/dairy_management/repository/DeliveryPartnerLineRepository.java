package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.DeliveryPartnerLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryPartnerLineRepository extends JpaRepository<DeliveryPartnerLine, Long> {
    Optional<DeliveryPartnerLine> findByDeliveryPartnerIdAndLineId(Long partnerId, Long lineId);
    boolean existsByLineId(Long lineId);
    Optional<DeliveryPartnerLine> findByLineId(Long lineId);
    List<DeliveryPartnerLine> findByDeliveryPartnerId(Long partnerId);
}
