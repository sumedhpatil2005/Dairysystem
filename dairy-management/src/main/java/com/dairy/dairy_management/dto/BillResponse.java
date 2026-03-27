package com.dairy.dairy_management.dto;

import com.dairy.dairy_management.entity.BillAdjustment;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BillResponse {

    private Long billId;
    private Long customerId;
    private String customerName;
    private int month;
    private int year;

    // Line items from subscription deliveries (status = DELIVERED)
    private List<LineItem> subscriptionItems;
    private double subscriptionAmount;

    // Line items from addon orders (status = DELIVERED)
    private List<LineItem> addonItems;
    private double addonAmount;

    // Unpaid balance carried from the previous month
    private double previousPendingAmount;

    // Net sum of manual adjustments (negative = deductions, positive = surcharges)
    private double adjustmentAmount;

    // Manual adjustments list
    private List<BillAdjustment> adjustments;

    // subscriptionAmount + addonAmount + previousPendingAmount + adjustmentAmount
    private double totalAmount;

    private double paidAmount;
    private double remainingAmount;
    private String status; // PENDING / PAID

    @Data
    public static class LineItem {
        private LocalDate date;
        private String productName;
        private String unit;
        private double quantity;
        private double pricePerUnit;
        private double subtotal;

        public LineItem(LocalDate date, String productName, String unit,
                        double quantity, double pricePerUnit) {
            this.date = date;
            this.productName = productName;
            this.unit = unit;
            this.quantity = quantity;
            this.pricePerUnit = pricePerUnit;
            this.subtotal = quantity * pricePerUnit;
        }
    }
}
