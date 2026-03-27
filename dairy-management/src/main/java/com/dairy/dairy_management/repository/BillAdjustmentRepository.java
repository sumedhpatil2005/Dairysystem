package com.dairy.dairy_management.repository;

import com.dairy.dairy_management.entity.BillAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillAdjustmentRepository extends JpaRepository<BillAdjustment, Long> {

    List<BillAdjustment> findByBillId(Long billId);

    void deleteByBillId(Long billId);
}
