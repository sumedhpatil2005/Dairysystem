package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.GenerateDeliveryResult;
import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.entity.OverrideType;
import com.dairy.dairy_management.entity.Subscription;
import com.dairy.dairy_management.repository.DeliveryOverrideRepository;
import com.dairy.dairy_management.repository.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryService {

    private final DeliveryRepository repo;
    private final SubscriptionService subscriptionService;
    private final HolidayService holidayService;
    private final DeliveryOverrideRepository overrideRepo;

    public DeliveryService(DeliveryRepository repo,
                           SubscriptionService subscriptionService,
                           HolidayService holidayService,
                           DeliveryOverrideRepository overrideRepo) {
        this.repo = repo;
        this.subscriptionService = subscriptionService;
        this.holidayService = holidayService;
        this.overrideRepo = overrideRepo;
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

    public List<Delivery> getByDate(LocalDate date) {
        return repo.findByDeliveryDate(date);
    }

    public List<Delivery> getByCustomer(Long customerId) {
        return repo.findBySubscription_CustomerId(customerId);
    }

    public Delivery getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));
    }

    /**
     * Auto-generates delivery records for all active subscriptions on the given date.
     *
     * Checks (in order):
     *  1. Is the date a system-wide holiday? → all deliveries skipped
     *  2. Does the customer have a PAUSED override? → status = SKIPPED
     *  3. Does the customer have a QUANTITY_MODIFIED override? → use modified quantity
     *  4. Frequency logic (DAILY / ALTERNATE_DAY / CUSTOM_WEEKLY)
     *  5. Duplicate guard — skip if delivery already exists for subscription + date
     *
     * Returns a summary of what was created, skipped, and not scheduled.
     */
    @Transactional
    public GenerateDeliveryResult generateForDate(LocalDate date) {
        // Check system-wide holiday first
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

            // Check per-customer PAUSED override
            boolean paused = overrideRepo
                    .findByCustomerIdAndDateAndOverrideType(customerId, date, OverrideType.PAUSED)
                    .isPresent();
            if (paused) {
                status = "SKIPPED";
            } else {
                // Check per-customer QUANTITY_MODIFIED override
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
                date,
                created,
                alreadyExisted,
                notScheduled,
                activeSubscriptions.size(),
                createdDeliveries
        );
    }

    /**
     * Updates the delivery status — used by the delivery partner Flutter app.
     * Valid statuses: PENDING, DELIVERED, SKIPPED, NOT_REACHABLE
     */
    public Delivery updateStatus(Long deliveryId, String status) {
        List<String> validStatuses = List.of("PENDING", "DELIVERED", "SKIPPED", "NOT_REACHABLE");
        String upperStatus = status.toUpperCase();

        if (!validStatuses.contains(upperStatus)) {
            throw new RuntimeException("Invalid status '" + status +
                    "'. Valid values: PENDING, DELIVERED, SKIPPED, NOT_REACHABLE");
        }

        Delivery delivery = repo.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        delivery.setStatus(upperStatus);
        return repo.save(delivery);
    }
}
