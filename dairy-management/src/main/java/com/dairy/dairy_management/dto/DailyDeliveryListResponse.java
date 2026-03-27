package com.dairy.dairy_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class DailyDeliveryListResponse {

    private Long partnerId;
    private String partnerName;
    private LocalDate date;
    private List<LineDeliveries> lines;

    @Data
    @AllArgsConstructor
    public static class LineDeliveries {
        private Long lineId;
        private String lineName;
        private Integer lineSequence;
        private List<CustomerDelivery> deliveries;
    }

    @Data
    @AllArgsConstructor
    public static class CustomerDelivery {
        private Long customerId;
        private String customerName;
        private String address;
        private String societyName;
        private Integer customerSequence;
        private Long deliveryId;     // null if delivery not yet generated for this date
        private Double quantity;     // null if delivery not yet generated
        private String status;       // null if delivery not yet generated
    }
}
