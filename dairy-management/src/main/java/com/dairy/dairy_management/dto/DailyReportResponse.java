package com.dairy.dairy_management.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyReportResponse {

    private LocalDate date;
    private int totalDeliveries;
    private int delivered;
    private int pending;
    private int skipped;
    private int notReachable;

    private List<LineReport> lines;

    @Data
    public static class LineReport {
        private Long lineId;
        private String lineName;
        private String partnerName; // null if no partner assigned
        private List<DeliveryItem> deliveries;
    }

    @Data
    public static class DeliveryItem {
        private Long deliveryId;
        private Long customerId;
        private String customerName;
        private String productName;
        private double quantity;
        private String slot;
        private String status;
    }
}
