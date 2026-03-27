package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.PaymentResponse;
import com.dairy.dairy_management.dto.RecordPaymentRequest;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.entity.Payment;
import com.dairy.dairy_management.exception.ConflictException;
import com.dairy.dairy_management.exception.NotFoundException;
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
        if (!billingRepo.existsById(billId)) {
            throw new NotFoundException("Bill not found");
        }
        List<Payment> payments = paymentRepo.findByBillId(billId);
        return payments.stream().map(paymentMapper::toDto).toList();
    }

    /**
     * Records a payment against a bill.
     * Uses a pessimistic write lock to prevent concurrent payments from overpaying.
     * Blocks zero payments and amounts exceeding the remaining balance.
     */
    @Transactional
    public Payment recordPayment(Long billId, RecordPaymentRequest request) {
        // Lock the bill row — prevents race condition where two concurrent payments both
        // pass the remaining > 0 check and together exceed the balance
        Billing bill = billingRepo.findByIdWithLock(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        if ("PAID".equals(bill.getStatus())) {
            throw new ConflictException("Bill is already fully paid");
        }

        double remaining = bill.getRemainingAmount();

        if (remaining <= 0) {
            throw new ConflictException("No outstanding balance on this bill");
        }

        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0");
        }

        if (request.getAmount() > remaining) {
            throw new IllegalArgumentException(
                    "Payment amount (" + request.getAmount() +
                    ") exceeds remaining balance (" + remaining + ")");
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

        double newPaid = bill.getPaidAmount() + request.getAmount();
        bill.setPaidAmount(newPaid);
        bill.setRemainingAmount(bill.getTotalAmount() - newPaid);
        bill.setStatus(bill.getRemainingAmount() <= 0 ? "PAID" : "PENDING");
        billingRepo.save(bill);

        return paymentRepo.save(payment);
    }

    // Kept for backward compatibility
    @Transactional
    public Payment makePayment(Long billId, double amount, String mode) {
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setAmount(amount);
        req.setMode(mode);
        return recordPayment(billId, req);
    }
}
