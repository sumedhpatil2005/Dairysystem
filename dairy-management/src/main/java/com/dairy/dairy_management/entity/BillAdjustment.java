package com.dairy.dairy_management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class BillAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @NotNull
    @JsonIgnore
    private Billing bill;

    // Negative = deduction (e.g. -180 for disputed delivery)
    // Positive = additional charge (e.g. +50 for late fee)
    @NotNull(message = "Amount is required")
    private Double amount;

    @NotBlank(message = "Description is required")
    private String description;

    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
