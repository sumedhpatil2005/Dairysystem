package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.BillAdjustmentRequest;
import com.dairy.dairy_management.dto.BillResponse;
import com.dairy.dairy_management.dto.PaymentResponse;
import com.dairy.dairy_management.dto.RecordPaymentRequest;
import com.dairy.dairy_management.entity.BillAdjustment;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.service.BillingService;
import com.dairy.dairy_management.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final BillingService service;
    private final PaymentService paymentService;

    public BillingController(BillingService service, PaymentService paymentService) {
        this.service = service;
        this.paymentService = paymentService;
    }

    /**
     * Generate (or regenerate) the monthly bill for a customer.
     * Uses historically accurate prices for each delivery date.
     * Example: POST /billing/generate?customerId=1&month=3&year=2026
     */
    @PostMapping("/generate")
    public BillResponse generate(@RequestParam Long customerId,
                                 @RequestParam int month,
                                 @RequestParam int year) {
        return service.generateBill(customerId, month, year);
    }

    /**
     * Get the raw Billing record by ID.
     */
    @GetMapping("/{id}")
    public Billing getBill(@PathVariable Long id) {
        return service.getBillById(id);
    }

    /**
     * Get full line-item breakdown for a bill.
     * Example: GET /billing/1/detail
     */
    @GetMapping("/{id}/detail")
    public BillResponse getBillDetail(@PathVariable Long id) {
        return service.getBillDetail(id);
    }

    /**
     * Get all bills for a customer (history).
     * Example: GET /billing/customer/1
     */
    @GetMapping("/customer/{id}")
    public List<Billing> getByCustomer(@PathVariable Long id) {
        return service.getBillsByCustomer(id);
    }

    /**
     * Get all pending (unpaid) bills — admin view.
     */
    @GetMapping("/pending")
    public List<Billing> getPending() {
        return service.getPendingBills();
    }

    /**
     * Get all bills for a specific month/year.
     * Example: GET /billing/month?month=3&year=2026
     */
    @GetMapping("/month")
    public List<Billing> getByMonth(@RequestParam int month, @RequestParam int year) {
        return service.getBillsByMonth(month, year);
    }

    // --- Payments ---

    /**
     * Record a full or partial payment against a bill.
     * Bill is automatically marked PAID when remainingAmount reaches 0.
     *
     * Example: POST /billing/1/payments
     * { "amount": 500.0, "mode": "UPI", "referenceNumber": "TXN123456", "note": "Partial payment" }
     */
    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public Payment recordPayment(@PathVariable Long id,
                                 @Valid @RequestBody RecordPaymentRequest request) {
        return paymentService.recordPayment(id, request);
    }

    /**
     * Get all payments recorded against a bill.
     * Example: GET /billing/1/payments
     */
    @GetMapping("/{id}/payments")
    public List<PaymentResponse> getPayments(@PathVariable Long id) {
        return paymentService.getPaymentsByBill(id);
    }

    // --- Bill Adjustments ---

    /**
     * Add a manual adjustment to a bill (negative = deduction, positive = surcharge).
     * Bill totals are recalculated immediately.
     *
     * Example (remove disputed item):
     *   POST /billing/1/adjustments
     *   { "amount": -180.0, "description": "Disputed delivery Mar 15 — 3L removed per customer request" }
     *
     * Example (add surcharge):
     *   POST /billing/1/adjustments
     *   { "amount": 50.0, "description": "Late payment surcharge" }
     */
    @PostMapping("/{id}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public BillAdjustment addAdjustment(@PathVariable Long id,
                                        @Valid @RequestBody BillAdjustmentRequest request) {
        return service.addAdjustment(id, request);
    }

    /**
     * List all adjustments on a bill.
     * Example: GET /billing/1/adjustments
     */
    @GetMapping("/{id}/adjustments")
    public List<BillAdjustment> getAdjustments(@PathVariable Long id) {
        return service.getAdjustments(id);
    }

    /**
     * Remove an adjustment from a bill. Bill totals are recalculated immediately.
     * Example: DELETE /billing/1/adjustments/2
     */
    @DeleteMapping("/{id}/adjustments/{adjId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAdjustment(@PathVariable Long id, @PathVariable Long adjId) {
        service.removeAdjustment(id, adjId);
    }
}
