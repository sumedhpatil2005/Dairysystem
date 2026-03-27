package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.CreateAddonOrderRequest;
import com.dairy.dairy_management.dto.UpdateDeliveryStatusRequest;
import com.dairy.dairy_management.entity.AddonOrder;
import com.dairy.dairy_management.service.AddonOrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/orders/addon")
public class AddonOrderController {

    private final AddonOrderService service;

    public AddonOrderController(AddonOrderService service) {
        this.service = service;
    }

    /**
     * Create a one-time addon order for a customer.
     * Example: POST /orders/addon
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddonOrder create(@Valid @RequestBody CreateAddonOrderRequest request) {
        return service.create(request);
    }

    /**
     * Get a single addon order by ID.
     * Example: GET /orders/addon/1
     */
    @GetMapping("/{id}")
    public AddonOrder getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * Get all addon orders for a customer (optionally filtered by date).
     * Example: GET /orders/addon/customer/3
     * Example: GET /orders/addon/customer/3?date=2026-03-27
     */
    @GetMapping("/customer/{id}")
    public List<AddonOrder> getByCustomer(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date != null) {
            return service.getByCustomerAndDate(id, date);
        }
        return service.getByCustomer(id);
    }

    /**
     * Get all addon orders for a specific date.
     * Example: GET /orders/addon/date/2026-03-27
     */
    @GetMapping("/date/{date}")
    public List<AddonOrder> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getByDate(date);
    }

    /**
     * Update addon order status (PENDING / DELIVERED / SKIPPED).
     * Used by the delivery partner Flutter app.
     * Example: PATCH /orders/addon/1/status  { "status": "DELIVERED" }
     */
    @PatchMapping("/{id}/status")
    public AddonOrder updateStatus(@PathVariable Long id,
                                   @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        return service.updateStatus(id, request.getStatus());
    }

    /**
     * Delete a pending addon order (cannot delete if already delivered).
     * Example: DELETE /orders/addon/1
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
