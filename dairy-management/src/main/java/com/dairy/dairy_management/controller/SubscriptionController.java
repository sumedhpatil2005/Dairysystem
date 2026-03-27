package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.CreateSubscriptionRequest;
import com.dairy.dairy_management.dto.ModifySubscriptionRequest;
import com.dairy.dairy_management.entity.Subscription;
import com.dairy.dairy_management.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Subscription> create(@Valid @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subscription> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/customer/{id}")
    public ResponseEntity<List<Subscription>> getByCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(service.getByCustomerId(id));
    }

    @GetMapping("/customer/{id}/active")
    public ResponseEntity<List<Subscription>> getActiveByCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(service.getActiveByCustomerId(id));
    }

    // Mid-cycle modification: closes existing subscription and creates a new one from effectiveDate
    @PutMapping("/{id}/modify")
    public ResponseEntity<Subscription> modify(@PathVariable Long id,
                                               @Valid @RequestBody ModifySubscriptionRequest request) {
        return ResponseEntity.ok(service.modify(id, request));
    }

    // Cancel a subscription (sets endDate to today)
    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<String> cancel(@PathVariable Long id) {
        service.cancel(id);
        return ResponseEntity.ok("Subscription cancelled successfully");
    }
}
