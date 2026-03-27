package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.PaymentResponse;
import com.dairy.dairy_management.dto.RecordPaymentRequest;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.entity.Payment;
import com.dairy.dairy_management.mapper.PaymentMapper;
import com.dairy.dairy_management.repository.BillingRepository;
import com.dairy.dairy_management.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return payments.stream().map(paymentMapper::toDto).toList();
    }

    @Transactional
    public Payment recordPayment(Long billId, RecordPaymentRequest request) {
        Billing bill = billingRepo.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        if ("PAID".equals(bill.getStatus())) {
            throw new RuntimeException("Bill is already fully paid");
        }

        double remaining = bill.getRemainingAmount();
        if (request.getAmount() > remaining) {
            throw new RuntimeException(
                    "Payment amount (" + request.getAmount() + ") exceeds remaining balance (" + remaining + ")");
        }

        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setCustomer(bill.getCustomer());
        payment.setAmount(request.getAmount());
        payment.setMode(request.getMode().toUpperCase());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setNote(request.getNote());
        payment.setPaymentDate(request.getPaymentDate() != null
                ? request.getPaymentDate()
                : LocalDate.now());

        // Update bill financials
        double newPaid = bill.getPaidAmount() + request.getAmount();
        bill.setPaidAmount(newPaid);
        bill.setRemainingAmount(bill.getTotalAmount() - newPaid);
        bill.setStatus(bill.getRemainingAmount() <= 0 ? "PAID" : "PENDING");
        billingRepo.save(bill);

        return paymentRepo.save(payment);
    }

    // Kept for backward compatibility with existing /payments endpoint
    @Transactional
    public Payment makePayment(Long billId, double amount, String mode) {
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setAmount(amount);
        req.setMode(mode);
        return recordPayment(billId, req);
    }
}
