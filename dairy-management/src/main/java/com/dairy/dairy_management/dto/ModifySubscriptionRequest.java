package com.dairy.dairy_management.dto;

import com.dairy.dairy_management.entity.DeliverySlot;
import com.dairy.dairy_management.entity.FrequencyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class ModifySubscriptionRequest {

    @Positive(message = "Quantity must be greater than 0")
    private double quantity;

    @NotNull(message = "Frequency is required")
    private FrequencyType frequency;

    @NotNull(message = "Delivery slot is required")
    private DeliverySlot deliverySlot;

    // Required only when frequency is CUSTOM_WEEKLY
    private Set<String> deliveryDays;

    // The date from which the new subscription takes effect
    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
}
