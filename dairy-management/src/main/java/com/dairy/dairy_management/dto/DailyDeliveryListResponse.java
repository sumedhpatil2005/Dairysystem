package com.dairy.dairy_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response returned to the delivery partner app for their daily work list.
 *
 * Structure:
 *   Partner
 *   └── loadSummary  (what to load on the vehicle before leaving)
 *   └── lines[]      (routes, sorted by partner's lineSequence)
 *       └── societies[]  (grouped by society within the route)
 *           └── customers[]  (sorted by lineSequence)
 *               └── deliveries[]  (one entry per active subscription product)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyDeliveryListResponse {

    private Long partnerId;
    private String partnerName;
    private LocalDate date;

    /** Aggregate load the partner needs to carry before leaving. */
    private LoadSummary loadSummary;

    /** Ordered list of delivery lines assigned to this partner. */
    private List<LineDeliveries> lines;

    // ─────────────────────────────────────────────
    // Load Summary
    // ─────────────────────────────────────────────

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoadSummary {
        /** Total number of customers to visit today. */
        private int totalCustomers;

        /** Total number of individual delivery items (excluding SKIPPED). */
        private int totalDeliveries;

        /** Per-product breakdown — what to load on the vehicle. */
        private List<ProductLoad> products;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductLoad {
        private String productName;
        private String unit;
        private double totalQuantity;
    }

    // ─────────────────────────────────────────────
    // Line → Society → Customer → Delivery
    // ─────────────────────────────────────────────

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LineDeliveries {
        private Long lineId;
        private String lineName;
        private Integer lineSequence;

        /**
         * Customers grouped by society name, in the order they appear along
         * the route (sorted by customer lineSequence within each society).
         */
        private List<SocietyGroup> societies;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SocietyGroup {
        /** Society / apartment complex name. "Other" if not set on customer. */
        private String societyName;
        private List<CustomerDelivery> customers;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerDelivery {
        private Long customerId;
        private String customerName;
        private String address;
        private String societyName;

        /** Position of this customer in the delivery route (lineSequence). */
        private Integer customerSequence;

        /**
         * All products to deliver to this customer today.
         * Empty list means deliveries were not yet generated for this date.
         */
        private List<DeliveryItem> deliveries;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeliveryItem {
        /** The delivery record ID — needed for status update calls. */
        private Long deliveryId;
        private String productName;
        private String unit;
        private double quantity;

        /** PENDING / DELIVERED / SKIPPED / NOT_REACHABLE */
        private String status;
    }
}
