package com.dairy.dairy_management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateDeliveryStatusRequest {

    // Valid values: PENDING, DELIVERED, SKIPPED, NOT_REACHABLE
    @NotBlank(message = "Status is required")
    private String status;
}
