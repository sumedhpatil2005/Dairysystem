package com.dairy.dairy_management.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ResequenceLinesRequest {

    @NotEmpty(message = "Line ID list is required")
    private List<Long> lineIds;
}
