package com.dairy.dairy_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DeliveryLineResponse {

    private Long id;
    private String name;
    private List<CustomerSummary> customers;

    @Data
    @AllArgsConstructor
    public static class CustomerSummary {
        private Long id;
        private String name;
        private String phone;
        private String address;
        private String societyName;
        private Integer lineSequence;
    }
}
