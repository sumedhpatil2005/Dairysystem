package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.GenerateDeliveryResult;
import com.dairy.dairy_management.dto.UpdateDeliveryStatusRequest;
import com.dairy.dairy_management.entity.Delivery;
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

    public DeliveryController(DeliveryService service) {
        this.service = service;
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
}
