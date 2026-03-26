package com.dairy.dairy_management.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PaymentResponse {

    private Long id;
    private double amount;
    private String mode;
    private LocalDate paymentDate;

    private Long billId;
    private double totalAmount;
    private double paidAmount;
    private double remainingAmount;
}
