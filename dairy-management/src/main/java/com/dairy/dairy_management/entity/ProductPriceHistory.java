package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
public class ProductPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @NotNull
    private Product product;

    @Positive
    private double price;

    @NotNull
    private LocalDate effectiveFrom;

    // null = this is the current active price
    private LocalDate effectiveTo;

    // Optional e.g. "Seasonal increase", "Festival rate"
    private String reason;

    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
