package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
public class AddonOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = true)
    @NotNull(message = "Customer is required")
    private Customer customer;

    @ManyToOne(optional = true)
    @NotNull(message = "Product is required")
    private Product product;

    @Positive(message = "Quantity must be greater than 0")
    private double quantity;

    @NotNull(message = "Delivery date is required")
    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Delivery slot is required")
    private DeliverySlot deliverySlot;

    // PENDING, DELIVERED, SKIPPED
    private String status = "PENDING";

    // Optional admin note (e.g. "Extra order for festival")
    private String notes;

    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
