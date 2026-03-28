package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.entity.AuditLog;
import com.dairy.dairy_management.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only endpoint for audit logs — ADMIN only.
 *
 * GET /audit-logs                                   → all logs (latest first, paginated)
 * GET /audit-logs?action=PAYMENT_RECORDED           → filter by action
 * GET /audit-logs?performedBy=admin1                → filter by user
 * GET /audit-logs?entityType=BILLING&entityId=5     → filter by entity
 */
@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AuditLog> getLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        if (entityType != null && entityId != null) {
            return service.getByEntity(entityType.toUpperCase(), entityId, pageable);
        }
        if (performedBy != null) {
            return service.getByUser(performedBy, pageable);
        }
        if (action != null) {
            return service.getByAction(action, pageable);
        }
        return service.getAll(pageable);
    }
}
