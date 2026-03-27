package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Customer customer;

    @ManyToOne
    private Billing bill;

    private double amount;

    private LocalDate paymentDate;

    private String mode; // CASH / UPI / BANK_TRANSFER / CHEQUE

    // Optional: UPI transaction ID, cheque number, etc.
    private String referenceNumber;

    // Optional note from admin e.g. "Partial payment — rest next week"
    private String note;

    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
