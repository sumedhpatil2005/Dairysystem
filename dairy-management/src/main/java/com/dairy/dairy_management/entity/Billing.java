package com.dairy.dairy_management.entity;

import lombok.Data;

import jakarta.persistence.*;

@Data
@Entity
public class Billing {
    private double paidAmount = 0;
    private double remainingAmount = 0;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Customer customer;

    private int month;

    private int year;

    private double totalAmount;

    private String status; // PAID / PENDING
}