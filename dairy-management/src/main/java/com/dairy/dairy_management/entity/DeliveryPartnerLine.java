package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class DeliveryPartnerLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "delivery_partner_id", nullable = false)
    private DeliveryPartner deliveryPartner;

    @ManyToOne
    @JoinColumn(name = "delivery_line_id", nullable = false)
    private DeliveryLine line;

    private Integer lineSequence;
}
