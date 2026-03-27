package com.dairy.dairy_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RecordPaymentRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    @NotBlank(message = "Payment mode is required (CASH / UPI / BANK_TRANSFER / CHEQUE)")
    private String mode;

    // Optional: UPI transaction ID, cheque number, etc.
    private String referenceNumber;

    // Optional admin note
    private String note;

    // Defaults to today if not provided
    private LocalDate paymentDate;
}
