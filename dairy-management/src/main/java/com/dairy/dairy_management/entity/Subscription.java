package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;
@Data
@Entity
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Customer customer;

    @ManyToOne
    private Product product;

    private double quantity;
    private String frequency;

    private LocalDate startDate;
    private LocalDate endDate;
}