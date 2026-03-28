package com.dairy.dairy_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Result of a bulk bill generation run (POST /billing/generate-all).
 */
@Data
@AllArgsConstructor
public class BulkBillResult {
    private int month;
    private int year;
    private int customersProcessed;
    private int billsGenerated;
    private int billsSkipped;
    private List<String> errors;
}
