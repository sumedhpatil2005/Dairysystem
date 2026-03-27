package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Set;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@Entity
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @NotNull(message = "Customer is required")
    private Customer customer;

    @ManyToOne
    @NotNull(message = "Product is required")
    private Product product;

    @Positive(message = "Quantity must be greater than 0")
    private double quantity;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Frequency is required")
    private FrequencyType frequency;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Delivery slot is required")
    private DeliverySlot deliverySlot;

    // Only required when frequency is CUSTOM_WEEKLY
    // Valid values: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    @ElementCollection
    @CollectionTable(name = "subscription_delivery_days",
            joinColumns = @JoinColumn(name = "subscription_id"))
    @Column(name = "day")
    private Set<String> deliveryDays;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    // null means active; set when subscription is cancelled or modified mid-cycle
    private LocalDate endDate;
}
