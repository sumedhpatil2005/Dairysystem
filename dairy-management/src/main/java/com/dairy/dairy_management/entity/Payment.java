package com.dairy.dairy_management.entity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

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

    private String mode; // CASH / UPI
}