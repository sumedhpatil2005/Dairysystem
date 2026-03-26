package com.dairy.dairy_management.controller;


import com.dairy.dairy_management.dto.PaymentResponse;
import com.dairy.dairy_management.dto.PaymentResponse;
import com.dairy.dairy_management.entity.Payment;
import com.dairy.dairy_management.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }
    @GetMapping("/bill/{billId}")
    public List<PaymentResponse> getByBill(@PathVariable Long billId) {
        return service.getPaymentsByBill(billId);
    }

    @PostMapping
    public Payment pay(@RequestParam Long billId,
                       @RequestParam double amount,
                       @RequestParam String mode) {

        return service.makePayment(billId, amount, mode);
    }
}
