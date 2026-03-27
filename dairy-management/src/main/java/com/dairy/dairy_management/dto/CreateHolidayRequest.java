package com.dairy.dairy_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateHolidayRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    // Optional e.g. "Diwali", "Republic Day"
    private String reason;
}
