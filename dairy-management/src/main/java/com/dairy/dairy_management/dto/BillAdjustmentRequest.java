package com.dairy.dairy_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BillAdjustmentRequest {

    // Negative = deduction, Positive = additional charge
    @NotNull(message = "Amount is required")
    private Double amount;

    @NotBlank(message = "Description is required")
    private String description;
}
