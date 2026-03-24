package com.dairy.dairy_management.repository;
import java.util.List;



import com.dairy.dairy_management.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBillId(Long billId);
}
