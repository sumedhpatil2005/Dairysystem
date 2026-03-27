package com.dairy.dairy_management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import jakarta.persistence.*;
import java.util.List;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uq_billing_customer_month_year",
        columnNames = {"customer_id", "month", "year"}))
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

    // Net sum of all manual adjustments (negative = deductions, positive = surcharges)
    private double adjustmentAmount = 0;

    // subscriptionAmount + addonAmount + previousPendingAmount + adjustmentAmount
    private double totalAmount = 0;

    private double paidAmount = 0;

    private double remainingAmount = 0;

    private String status; // PAID / PENDING

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Payment> payments;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<BillAdjustment> billAdjustments;
}
