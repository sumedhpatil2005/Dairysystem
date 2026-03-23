package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.entity.Subscription;
import com.dairy.dairy_management.service.SubscriptionService;
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
    public Subscription create(@RequestBody Subscription sub) {
        return service.create(sub);
    }

    @GetMapping("/customer/{id}")
    public List<Subscription> getByCustomer(@PathVariable Long id) {
        return service.getByCustomerId(id);
    }
}
