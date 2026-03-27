package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.BillResponse;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.service.BillingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final BillingService service;

    public BillingController(BillingService service) {
        this.service = service;
    }

    /**
     * Generate (or regenerate) the monthly bill for a customer.
     * Returns a full breakdown including subscription items, addon items, and previous pending.
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
     * Example: GET /billing/1
     */
    @GetMapping("/{id}")
    public Billing getBill(@PathVariable Long id) {
        return service.getBillById(id);
    }

    /**
     * Get the full breakdown (line items) for a bill.
     * Example: GET /billing/1/detail
     */
    @GetMapping("/{id}/detail")
    public BillResponse getBillDetail(@PathVariable Long id) {
        return service.getBillDetail(id);
    }

    /**
     * Get all bills for a customer (billing history).
     * Example: GET /billing/customer/1
     */
    @GetMapping("/customer/{id}")
    public List<Billing> getByCustomer(@PathVariable Long id) {
        return service.getBillsByCustomer(id);
    }

    /**
     * Get all pending (unpaid) bills — admin view.
     * Example: GET /billing/pending
     */
    @GetMapping("/pending")
    public List<Billing> getPending() {
        return service.getPendingBills();
    }

    /**
     * Get all bills generated for a specific month/year.
     * Example: GET /billing/month?month=3&year=2026
     */
    @GetMapping("/month")
    public List<Billing> getByMonth(@RequestParam int month, @RequestParam int year) {
        return service.getBillsByMonth(month, year);
    }
}
