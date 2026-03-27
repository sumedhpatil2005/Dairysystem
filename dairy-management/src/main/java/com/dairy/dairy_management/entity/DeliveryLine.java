package com.dairy.dairy_management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class DeliveryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Line name is required")
    @Column(unique = true)
    private String name;

    @OneToMany(mappedBy = "deliveryLine")
    @OrderBy("lineSequence ASC")
    @JsonIgnore
    private List<Customer> customers;
}
