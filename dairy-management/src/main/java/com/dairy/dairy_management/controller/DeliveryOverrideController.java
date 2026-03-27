package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.CreateDeliveryOverrideRequest;
import com.dairy.dairy_management.entity.DeliveryOverride;
import com.dairy.dairy_management.service.DeliveryOverrideService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/overrides")
public class DeliveryOverrideController {

    private final DeliveryOverrideService service;

    public DeliveryOverrideController(DeliveryOverrideService service) {
        this.service = service;
    }

    /**
     * Create a per-customer delivery override for a specific date.
     *
     * PAUSED example:
     *   POST /overrides  { "customerId": 1, "date": "2026-03-28", "overrideType": "PAUSED", "reason": "Customer on vacation" }
     *
     * QUANTITY_MODIFIED example:
     *   POST /overrides  { "customerId": 1, "date": "2026-03-28", "overrideType": "QUANTITY_MODIFIED", "modifiedQuantity": 3.0 }
     *
     * If a delivery record already exists for this customer on this date, it is updated immediately.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryOverride create(@Valid @RequestBody CreateDeliveryOverrideRequest request) {
        return service.create(request);
    }

    /**
     * Get a single override by ID.
     */
    @GetMapping("/{id}")
    public DeliveryOverride getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * Get all overrides for a customer (optionally filtered by date).
     * Example: GET /overrides/customer/1
     * Example: GET /overrides/customer/1?date=2026-03-28
     */
    @GetMapping("/customer/{id}")
    public List<DeliveryOverride> getByCustomer(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date != null) {
            return service.getByCustomerAndDate(id, date);
        }
        return service.getByCustomer(id);
    }

    /**
     * Get all overrides across all customers for a specific date.
     * Example: GET /overrides/date/2026-03-28
     */
    @GetMapping("/date/{date}")
    public List<DeliveryOverride> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getByDate(date);
    }

    /**
     * Remove an override (reverts any already-generated delivery records).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
