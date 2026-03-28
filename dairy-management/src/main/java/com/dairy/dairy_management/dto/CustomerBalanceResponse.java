package com.dairy.dairy_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Total outstanding balance for a customer across all unpaid months.
 * Used by GET /customers/{id}/balance
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerBalanceResponse {

    private Long customerId;
    private String customerName;
    private String phone;

    /** Number of months with an outstanding balance. */
    private int unpaidBillsCount;

    /** Sum of remainingAmount across all PENDING bills. */
    private double totalOutstanding;

    /** Per-month breakdown so the admin knows which months are unpaid. */
    private List<BillSummary> unpaidBills;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BillSummary {
        private Long billId;
        private int month;
        private int year;
        private double totalAmount;
        private double paidAmount;
        private double remainingAmount;
    }
}
