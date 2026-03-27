package com.dairy.dairy_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AssignLineRequest {

    @NotNull(message = "Line ID is required")
    private Long lineId;

    @NotNull(message = "Sequence is required")
    @Positive(message = "Sequence must be a positive number")
    private Integer sequence;
}
