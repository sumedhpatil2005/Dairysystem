package com.dairy.dairy_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full account statement for a customer across all billing months.
 * Used by GET /customers/{id}/statement
 *
 * Gives the admin (or future customer portal) a complete picture of:
 *   - every month a bill was generated
 *   - how many deliveries happened that month
 *   - amounts billed, paid, and still outstanding
 *   - a running total across the entire account history
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerStatementResponse {

    private Long   customerId;
    private String customerName;
    private String phone;
    private String address;

    /** Sum of totalAmount across all months with a generated bill. */
    private double totalBilled;

    /** Sum of paidAmount across all months. */
    private double totalPaid;

    /** Sum of remainingAmount for all PENDING bills — what the customer still owes. */
    private double totalOutstanding;

    /** Total number of bills generated. */
    private int totalBillsGenerated;

    /** Per-month entries sorted oldest → newest. */
    private List<MonthStatement> months;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthStatement {
        private int    month;
        private int    year;

        /** null if no bill has been generated for this month yet. */
        private Long   billId;

        /** "PAID", "PENDING", or "NOT_GENERATED" */
        private String status;

        private double subscriptionAmount;
        private double addonAmount;
        private double adjustmentAmount;
        private double previousPendingAmount;
        private double totalAmount;
        private double paidAmount;
        private double remainingAmount;

        /** Total delivery records for this month (all statuses). */
        private int deliveriesCount;

        /** Deliveries with status DELIVERED. */
        private int deliveredCount;

        /** Deliveries with status SKIPPED. */
        private int skippedCount;
    }
}
