package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.GenerateDeliveryResult;
import com.dairy.dairy_management.dto.MistakeCorrectionResponse;
import com.dairy.dairy_management.dto.UpdateDeliveryStatusRequest;
import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.service.BillingService;
import com.dairy.dairy_management.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryService service;
    private final BillingService billingService;

    public DeliveryController(DeliveryService service, BillingService billingService) {
        this.service = service;
        this.billingService = billingService;
    }

    @PostMapping
    public Delivery create(@Valid @RequestBody Delivery delivery) {
        return service.create(delivery);
    }

    @GetMapping
    public List<Delivery> getAll() {
        return service.getAll();
    }

    @GetMapping("/subscription/{id}")
    public List<Delivery> getBySubscription(@PathVariable Long id) {
        return service.getBySubscription(id);
    }

    /**
     * Auto-generate delivery records for all active subscriptions on a given date.
     * Defaults to today if no date param is provided.
     * Example: POST /deliveries/generate?date=2025-01-15
     */
    @PostMapping("/generate")
    public GenerateDeliveryResult generateForDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return service.generateForDate(date);
    }

    /**
     * Update delivery status (used by Flutter delivery partner app).
     * Valid statuses: PENDING, DELIVERED, SKIPPED, NOT_REACHABLE
     * Example: PATCH /deliveries/5/status  { "status": "DELIVERED" }
     */
    @PatchMapping("/{id}/status")
    public Delivery updateStatus(@PathVariable Long id,
                                 @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        return service.updateStatus(id, request.getStatus());
    }

    /**
     * Get all deliveries for a specific date.
     * Example: GET /deliveries/date/2025-01-15
     */
    @GetMapping("/date/{date}")
    public List<Delivery> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getByDate(date);
    }

    /**
     * Get all deliveries for a specific customer (across all their subscriptions).
     * Example: GET /deliveries/customer/3
     */
    @GetMapping("/customer/{id}")
    public List<Delivery> getByCustomer(@PathVariable Long id) {
        return service.getByCustomer(id);
    }

    /**
     * Get a single delivery by ID.
     * Example: GET /deliveries/7
     */
    @GetMapping("/{id}")
    public Delivery getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * Marks a delivery as SKIPPED (added by mistake) and auto-recalculates
     * the monthly bill for that customer if one already exists.
     *
     * Use this instead of manually changing status + regenerating the bill separately.
     * Example: POST /deliveries/5/mark-as-mistake
     */
    @PostMapping("/{id}/mark-as-mistake")
    public MistakeCorrectionResponse markAsMistake(@PathVariable Long id) {
        Delivery delivery = service.markAsMistake(id);
        Long customerId = delivery.getSubscription().getCustomer().getId();
        int month = delivery.getDeliveryDate().getMonthValue();
        int year = delivery.getDeliveryDate().getYear();

        var updatedBill = billingService.recalculateIfBillExists(customerId, month, year);
        return new MistakeCorrectionResponse(
                "Delivery marked as skipped successfully",
                updatedBill.isPresent(),
                updatedBill.orElse(null)
        );
    }
}
