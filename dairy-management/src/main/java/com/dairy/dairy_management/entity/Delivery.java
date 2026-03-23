package com.dairy.dairy_management.entity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Data
@Entity
public class Delivery {
    @Id
    @GeneratedValue ( strategy= GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @NotNull(message = "Subscription is required")
    @JsonIgnore
    private Subscription subscription;

    @NotNull(message = "Delivery date is required")
    private LocalDate deliveryDate;

    @Positive(message = "Quantity must be greater than 0")
    private double quantity;

    @NotBlank(message = "Status is required")
    private String status;


}
