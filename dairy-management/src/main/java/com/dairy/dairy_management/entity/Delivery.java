package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

@Data
@Entity
@Table(name = "delivery", uniqueConstraints = @UniqueConstraint(
        name = "uq_delivery_subscription_date",
        columnNames = {"subscription_id", "delivery_date"}))
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // optional = true forces Hibernate 6 to use LEFT JOIN instead of INNER JOIN.
    // Without this, Hibernate 6 infers INNER JOIN from @NotNull causing deliveries
    // with any broken subscription reference to silently disappear from results.
    @ManyToOne(optional = true)
    @NotNull(message = "Subscription is required")
    private Subscription subscription;

    @NotNull(message = "Delivery date is required")
    private LocalDate deliveryDate;

    @Positive(message = "Quantity must be greater than 0")
    private double quantity;

    @NotBlank(message = "Status is required")
    private String status;
}
