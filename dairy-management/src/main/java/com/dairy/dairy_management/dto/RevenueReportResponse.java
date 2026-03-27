package com.dairy.dairy_management.dto;

import lombok.Data;

@Data
public class RevenueReportResponse {

    private int month;
    private int year;

    private int totalBills;
    private int paidBills;
    private int pendingBills;

    private double totalBilled;
    private double totalCollected;
    private double totalPending;
}
