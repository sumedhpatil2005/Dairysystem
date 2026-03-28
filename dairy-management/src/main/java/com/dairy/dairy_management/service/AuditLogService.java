package com.dairy.dairy_management.service;

import com.dairy.dairy_management.entity.AuditLog;
import com.dairy.dairy_management.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Records an immutable audit trail for significant write operations.
 *
 * Usage in other services:
 *   auditLog.log("PAYMENT_RECORDED", "BILLING", billId, "₹500 cash payment recorded");
 *
 * "performedBy" is resolved automatically from the JWT security context.
 * Scheduled jobs get "system" as the performer.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository repo;

    public AuditLogService(AuditLogRepository repo) {
        this.repo = repo;
    }

    /**
     * Appends an audit log entry.
     *
     * @param action     What happened (e.g. "PAYMENT_RECORDED")
     * @param entityType Entity kind (e.g. "BILLING", "DELIVERY", "PRODUCT")
     * @param entityId   Primary key of the affected entity, or null for bulk/system events
     * @param details    Short human-readable description (max 500 chars)
     */
    public void log(String action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setPerformedBy(resolveCurrentUser());
        entry.setDetails(truncate(details, 500));
        repo.save(entry);
    }

    // ── Query methods (used by AuditLogController) ────────────────────────

    public Page<AuditLog> getAll(Pageable pageable) {
        return repo.findAllByOrderByTimestampDesc(pageable);
    }

    public Page<AuditLog> getByEntity(String entityType, Long entityId, Pageable pageable) {
        return repo.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    public Page<AuditLog> getByUser(String username, Pageable pageable) {
        return repo.findByPerformedBy(username, pageable);
    }

    public Page<AuditLog> getByAction(String action, Pageable pageable) {
        return repo.findByAction(action.toUpperCase(), pageable);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Resolves the currently authenticated username from the JWT security context.
     * Returns "system" for scheduled jobs (no authentication context present).
     */
    private String resolveCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {
            // No security context (e.g. in scheduled tasks)
        }
        return "system";
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
