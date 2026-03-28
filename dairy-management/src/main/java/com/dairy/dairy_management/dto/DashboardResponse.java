package com.dairy.dairy_management.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DashboardResponse {

    private LocalDate date;

    // Today's delivery stats
    private int todayTotal;
    private int todayDelivered;
    private int todayPending;
    private int todaySkipped;

    // Current month billing stats
    private int currentMonth;
    private int currentYear;
    private double monthlyBilled;
    private double monthlyCollected;
    private double monthlyOutstanding;

    // Pending bills across all months
    private int pendingBillsCount;
    private double pendingBillsTotal;

    // System totals
    private long activeCustomers;
    private long totalDeliveryPartners;
}
