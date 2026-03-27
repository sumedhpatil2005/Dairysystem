package com.dairy.dairy_management.dto;

import com.dairy.dairy_management.entity.AdjustmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BillAdjustmentRequest {

    // Defaults to OTHER if not provided
    private AdjustmentType adjustmentType = AdjustmentType.OTHER;

    // Negative = deduction, Positive = additional charge
    @NotNull(message = "Amount is required")
    private Double amount;

    @NotBlank(message = "Description is required")
    private String description;
}
