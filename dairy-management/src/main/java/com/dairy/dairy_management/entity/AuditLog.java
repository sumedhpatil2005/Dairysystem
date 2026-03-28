package com.dairy.dairy_management.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Immutable audit trail for every significant write operation in the system.
 *
 * Records who did what to which entity, and when.
 * Entries are never updated or deleted — append-only.
 */
@Data
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
        @Index(name = "idx_audit_performed_by", columnList = "performedBy"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * What happened. Examples:
     *   PAYMENT_RECORDED, PRICE_UPDATED, ADJUSTMENT_ADDED, ADJUSTMENT_REMOVED,
     *   DELIVERY_VOIDED, ADDON_VOIDED, CUSTOMER_DEACTIVATED, CUSTOMER_ACTIVATED,
     *   BILL_GENERATED, BULK_BILL_GENERATED, AUTO_DELIVERY_GENERATED, AUTO_BILL_GENERATED
     */
    @Column(nullable = false, length = 60)
    private String action;

    /** Entity type affected. e.g. BILLING, DELIVERY, PAYMENT, PRODUCT, CUSTOMER */
    @Column(nullable = false, length = 40)
    private String entityType;

    /** Primary key of the affected entity. Null for bulk/system operations. */
    private Long entityId;

    /**
     * Username of the person who triggered this action.
     * "system" for scheduled jobs.
     */
    @Column(nullable = false, length = 80)
    private String performedBy;

    /** Human-readable summary of the change. */
    @Column(length = 500)
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}
