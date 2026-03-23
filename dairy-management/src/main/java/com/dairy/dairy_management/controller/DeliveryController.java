package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
}
