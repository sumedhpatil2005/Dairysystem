package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.GenerateDeliveryResult;
import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.entity.OverrideType;
import com.dairy.dairy_management.entity.Subscription;
import com.dairy.dairy_management.exception.ConflictException;
import com.dairy.dairy_management.exception.NotFoundException;
import com.dairy.dairy_management.repository.DeliveryOverrideRepository;
import com.dairy.dairy_management.repository.DeliveryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dairy.dairy_management.service.AuditLogService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DeliveryService {

    private final DeliveryRepository repo;
    private final SubscriptionService subscriptionService;
    private final HolidayService holidayService;
    private final DeliveryOverrideRepository overrideRepo;
    private final AuditLogService auditLogService;

    public DeliveryService(DeliveryRepository repo,
                           SubscriptionService subscriptionService,
                           HolidayService holidayService,
                           DeliveryOverrideRepository overrideRepo,
                           AuditLogService auditLogService) {
        this.repo = repo;
        this.subscriptionService = subscriptionService;
        this.holidayService = holidayService;
        this.overrideRepo = overrideRepo;
        this.auditLogService = auditLogService;
    }

    public Delivery create(Delivery delivery) {
        return repo.save(delivery);
    }

    public List<Delivery> getAll() {
        return repo.findAll();
    }

    public List<Delivery> getBySubscription(Long subscriptionId) {
        return repo.findBySubscriptionId(subscriptionId);
    }

    public Page<Delivery> getByDate(LocalDate date, Pageable pageable) {
        return repo.findByDeliveryDate(date, pageable);
    }

    public List<Delivery> getByCustomer(Long customerId) {
        return repo.findBySubscription_CustomerId(customerId);
    }

    public Delivery getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Delivery not found"));
    }

    /**
     * Auto-generates delivery records for all active subscriptions on the given date.
     *
     * Checks (in order):
     *  1. Is the date a system-wide holiday? → all deliveries skipped
     *  2. Is the subscription paused? → skip
     *  3. Does the customer have a PAUSED override for this date? → status = SKIPPED
     *  4. Does the customer have a QUANTITY_MODIFIED override? → use modified quantity
     *  5. Frequency logic (DAILY / ALTERNATE_DAY / CUSTOM_WEEKLY)
     *     - ALTERNATE_DAY uses ChronoUnit.DAYS.between() for correct cross-month calculation
     *  6. Duplicate guard — skip if delivery already exists for subscription + date
     *     (unique DB constraint also enforces this as a hard guarantee)
     */
    @Transactional
    public GenerateDeliveryResult generateForDate(LocalDate date) {
        if (holidayService.isHoliday(date)) {
            return new GenerateDeliveryResult(date, 0, 0,
                    subscriptionService.getActiveOnDate(date).size(),
                    subscriptionService.getActiveOnDate(date).size(),
                    List.of());
        }

        List<Subscription> activeSubscriptions = subscriptionService.getActiveOnDate(date);

        int created = 0;
        int alreadyExisted = 0;
        int notScheduled = 0;
        List<Delivery> createdDeliveries = new ArrayList<>();

        for (Subscription sub : activeSubscriptions) {
            // Skip paused subscriptions
            if (sub.isPaused()) {
                notScheduled++;
                continue;
            }

            if (!subscriptionService.shouldDeliverOn(sub, date)) {
                notScheduled++;
                continue;
            }

            if (repo.existsBySubscriptionIdAndDeliveryDate(sub.getId(), date)) {
                alreadyExisted++;
                continue;
            }

            Long customerId = sub.getCustomer().getId();
            String status = "PENDING";
            double quantity = sub.getQuantity();

            boolean paused = overrideRepo
                    .findByCustomerIdAndDateAndOverrideType(customerId, date, OverrideType.PAUSED)
                    .isPresent();
            if (paused) {
                status = "SKIPPED";
            } else {
                var qtyOverride = overrideRepo
                        .findByCustomerIdAndDateAndOverrideType(customerId, date, OverrideType.QUANTITY_MODIFIED);
                if (qtyOverride.isPresent() && qtyOverride.get().getModifiedQuantity() != null) {
                    quantity = qtyOverride.get().getModifiedQuantity();
                }
            }

            Delivery delivery = new Delivery();
            delivery.setSubscription(sub);
            delivery.setDeliveryDate(date);
            delivery.setQuantity(quantity);
            delivery.setStatus(status);

            createdDeliveries.add(repo.save(delivery));
            created++;
        }

        return new GenerateDeliveryResult(
                date, created, alreadyExisted, notScheduled,
                activeSubscriptions.size(), createdDeliveries);
    }

    /**
     * Marks a delivery as SKIPPED (voided by admin due to mistake).
     * Allowed from any non-SKIPPED status.
     */
    public Delivery markAsMistake(Long deliveryId) {
        Delivery delivery = repo.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Delivery not found"));
        if ("SKIPPED".equals(delivery.getStatus())) {
            throw new ConflictException("Delivery is already marked as skipped");
        }
        delivery.setStatus("SKIPPED");
        Delivery saved = repo.save(delivery);

        auditLogService.log("DELIVERY_VOIDED", "DELIVERY", deliveryId,
                String.format("Delivery %d marked as mistake (SKIPPED). Date=%s Customer=%s",
                        deliveryId,
                        delivery.getDeliveryDate(),
                        delivery.getSubscription().getCustomer().getName()));

        return saved;
    }

    /**
     * Updates delivery status — used by the delivery partner Flutter app.
     *
     * Allowed transitions:
     *   PENDING       → DELIVERED, SKIPPED, NOT_REACHABLE
     *   NOT_REACHABLE → DELIVERED, SKIPPED
     *   DELIVERED     → not allowed via this endpoint (use /mark-as-mistake)
     *   SKIPPED       → not allowed via this endpoint
     *
     * Valid target statuses: PENDING, DELIVERED, SKIPPED, NOT_REACHABLE
     */
    public Delivery updateStatus(Long deliveryId, String status) {
        List<String> validStatuses = List.of("PENDING", "DELIVERED", "SKIPPED", "NOT_REACHABLE");
        String newStatus = status.toUpperCase();

        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status '" + status +
                    "'. Valid values: PENDING, DELIVERED, SKIPPED, NOT_REACHABLE");
        }

        Delivery delivery = repo.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Delivery not found"));

        String current = delivery.getStatus();

        // Define allowed transitions
        Map<String, List<String>> allowed = Map.of(
                "PENDING",       List.of("DELIVERED", "SKIPPED", "NOT_REACHABLE"),
                "NOT_REACHABLE", List.of("DELIVERED", "SKIPPED"),
                "DELIVERED",     List.of(),
                "SKIPPED",       List.of()
        );

        List<String> allowedNext = allowed.getOrDefault(current, List.of());
        if (!allowedNext.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Cannot transition delivery from " + current + " to " + newStatus +
                    (current.equals("DELIVERED") || current.equals("SKIPPED")
                            ? ". Use /mark-as-mistake to void a delivery."
                            : ""));
        }

        delivery.setStatus(newStatus);
        return repo.save(delivery);
    }
}
