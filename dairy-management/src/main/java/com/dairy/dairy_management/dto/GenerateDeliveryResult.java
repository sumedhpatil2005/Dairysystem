package com.dairy.dairy_management.dto;

import com.dairy.dairy_management.entity.Delivery;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class GenerateDeliveryResult {

    private LocalDate date;

    // Deliveries newly created
    private int created;

    // Subscriptions that already had a delivery record for this date
    private int alreadyExisted;

    // Subscriptions active but not scheduled for this day (e.g. ALTERNATE_DAY off-day, CUSTOM_WEEKLY wrong day)
    private int notScheduled;

    // Total active subscriptions considered
    private int totalActiveSubscriptions;

    // The actual delivery records created
    private List<Delivery> deliveries;
}
