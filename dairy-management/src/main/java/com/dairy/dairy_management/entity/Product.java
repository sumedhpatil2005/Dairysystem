package com.dairy.dairy_management.entity;
import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String unit;
    private  double pricePerUnit;
}
