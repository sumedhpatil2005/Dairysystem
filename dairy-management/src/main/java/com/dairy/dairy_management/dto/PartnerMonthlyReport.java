package com.dairy.dairy_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Monthly performance report for a delivery partner.
 * Used by GET /reports/partner/{id}/monthly?month=&year=
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartnerMonthlyReport {

    private Long partnerId;
    private String partnerName;
    private int month;
    private int year;

    /** Total delivery records assigned to this partner's lines that month. */
    private int totalAssigned;
    private int delivered;
    private int skipped;
    private int notReachable;
    private int pending;

    /**
     * Delivered / totalAssigned × 100, rounded to 1 decimal place.
     * 0.0 when totalAssigned is 0.
     */
    private double deliveryRatePercent;

    /** Per-line breakdown within the partner's route. */
    private List<LinePerformance> lines;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LinePerformance {
        private Long lineId;
        private String lineName;
        private int totalAssigned;
        private int delivered;
        private int skipped;
        private int notReachable;
        private int pending;
    }
}
