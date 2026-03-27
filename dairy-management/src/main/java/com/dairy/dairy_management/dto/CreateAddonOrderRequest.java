package com.dairy.dairy_management.dto;

import com.dairy.dairy_management.entity.DeliverySlot;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateAddonOrderRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @Positive(message = "Quantity must be greater than 0")
    private double quantity;

    @NotNull(message = "Delivery date is required")
    private LocalDate deliveryDate;

    @NotNull(message = "Delivery slot is required")
    private DeliverySlot deliverySlot;

    // Optional note for the admin
    private String notes;
}
