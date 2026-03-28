package com.dairy.dairy_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DeliveryPartnerResponse {

    private Long id;
    private String name;
    private String phone;
    private String username;
    private boolean active;
    private List<AssignedLine> assignedLines;

    @Data
    @AllArgsConstructor
    public static class AssignedLine {
        private Long lineId;
        private String lineName;
        private Integer lineSequence;
        private int customerCount;
    }
}
