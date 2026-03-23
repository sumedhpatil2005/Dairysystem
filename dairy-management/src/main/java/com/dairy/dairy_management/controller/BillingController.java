package com.dairy.dairy_management.controller;


import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.service.BillingService;
import com.dairy.dairy_management.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final BillingService service;

    public BillingController(BillingService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public Billing generate(@RequestParam Long customerId,
                            @RequestParam int month,
                            @RequestParam int year) {

        Customer customer = new Customer();
        customer.setId(customerId);

        return service.generateBill(customer, month, year);
    }
}