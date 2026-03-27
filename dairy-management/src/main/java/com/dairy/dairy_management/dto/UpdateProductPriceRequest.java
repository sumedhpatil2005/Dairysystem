package com.dairy.dairy_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProductPriceRequest {

    @NotNull(message = "New price is required")
    @Positive(message = "Price must be greater than 0")
    private Double newPrice;

    // Defaults to today if not provided
    private LocalDate effectiveFrom;

    // Optional e.g. "Seasonal increase"
    private String reason;
}
