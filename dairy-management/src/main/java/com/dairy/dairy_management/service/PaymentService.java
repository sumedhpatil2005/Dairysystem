package com.dairy.dairy_management.service;



import com.dairy.dairy_management.dto.PaymentResponse;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.entity.Payment;
import com.dairy.dairy_management.mapper.PaymentMapper;
import com.dairy.dairy_management.repository.BillingRepository;
import com.dairy.dairy_management.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {
    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepo;
    private final BillingRepository billingRepo;

    public PaymentService(PaymentRepository paymentRepo,
                          BillingRepository billingRepo,
                          PaymentMapper paymentMapper) {
        this.paymentRepo = paymentRepo;
        this.billingRepo = billingRepo;
        this.paymentMapper = paymentMapper;
    }
    public List<PaymentResponse> getPaymentsByBill(Long billId) {

        List<Payment> payments = paymentRepo.findByBillId(billId);

        return payments.stream()
                .map(paymentMapper::toDto)
                .toList();
    }
    public Payment makePayment(Long billId, double amount, String mode) {

        Billing bill = billingRepo.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));


        if (amount <= 0) {
            throw new RuntimeException("Payment amount must be greater than 0");
        }


        if ("PAID".equals(bill.getStatus())) {
            throw new RuntimeException("Bill already fully paid");
        }


        double remaining = bill.getTotalAmount() - bill.getPaidAmount();
        if (amount > remaining) {
            throw new RuntimeException("Payment exceeds remaining amount");
        }

        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setCustomer(bill.getCustomer());
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDate.now());
        payment.setMode(mode);


        double newPaid = bill.getPaidAmount() + amount;
        bill.setPaidAmount(newPaid);

        double newRemaining = bill.getTotalAmount() - newPaid;
        bill.setRemainingAmount(newRemaining);

        if (newRemaining == 0) {
            bill.setStatus("PAID");
        } else {
            bill.setStatus("PARTIAL");
        }

        billingRepo.save(bill);

        return paymentRepo.save(payment);
    }
}