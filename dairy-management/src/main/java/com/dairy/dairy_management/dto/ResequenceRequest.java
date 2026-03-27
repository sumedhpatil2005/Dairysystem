package com.dairy.dairy_management.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ResequenceRequest {

    @NotEmpty(message = "Customer ID list is required")
    private List<Long> customerIds;
}
