package com.dairy.dairy_management.entity;

import lombok.Data;

import jakarta.persistence.*;

@Data
@Entity
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Customer customer;

    private int month;

    private int year;

    // Amount from subscription deliveries (DELIVERED status only)
    private double subscriptionAmount = 0;

    // Amount from addon orders (DELIVERED status only)
    private double addonAmount = 0;

    // Carried over unpaid balance from the previous month's bill
    private double previousPendingAmount = 0;

    // subscriptionAmount + addonAmount + previousPendingAmount
    private double totalAmount = 0;

    private double paidAmount = 0;

    private double remainingAmount = 0;

    private String status; // PAID / PENDING
}