package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Entity
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Unit is required (e.g. LITRE, KG, PACKET)")
    private String unit;

    @Positive(message = "Price per unit must be greater than 0")
    private double pricePerUnit;
}
