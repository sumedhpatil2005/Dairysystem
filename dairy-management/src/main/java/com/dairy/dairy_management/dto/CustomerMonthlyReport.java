package com.dairy.dairy_management.dto;

import com.dairy.dairy_management.entity.AddonOrder;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.entity.Delivery;
import lombok.Data;

import java.util.List;

@Data
public class CustomerMonthlyReport {

    private Long customerId;
    private String customerName;
    private String phone;
    private int month;
    private int year;

    private List<Delivery> deliveries;
    private int totalDeliveries;
    private int deliveredCount;
    private int skippedCount;

    private List<AddonOrder> addonOrders;
    private int totalAddons;

    // null if bill has not been generated yet for this month
    private Billing bill;
}
