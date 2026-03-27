package com.dairy.dairy_management.dto;

import com.dairy.dairy_management.entity.OverrideType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateDeliveryOverrideRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Override type is required — PAUSED or QUANTITY_MODIFIED")
    private OverrideType overrideType;

    // Required only when overrideType = QUANTITY_MODIFIED
    @Positive(message = "Modified quantity must be greater than 0")
    private Double modifiedQuantity;

    // Optional reason
    private String reason;
}
