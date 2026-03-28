package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "date", "override_type"}))
public class DeliveryOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = true)
    @NotNull(message = "Customer is required")
    private Customer customer;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_type")
    @NotNull(message = "Override type is required")
    private OverrideType overrideType;

    // Only used when overrideType = QUANTITY_MODIFIED
    private Double modifiedQuantity;

    // Optional reason e.g. "Customer on vacation", "Festival extra milk"
    private String reason;

    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
