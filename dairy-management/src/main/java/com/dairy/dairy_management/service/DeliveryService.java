package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.GenerateDeliveryResult;
import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.entity.Subscription;
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

    public DeliveryService(DeliveryRepository repo, SubscriptionService subscriptionService) {
        this.repo = repo;
        this.subscriptionService = subscriptionService;
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
     * For each active subscription:
     *  - Checks frequency logic (DAILY / ALTERNATE_DAY / CUSTOM_WEEKLY)
     *  - Skips if a delivery record already exists for this subscription + date
     *  - Creates a PENDING delivery if the subscription is due
     *
     * Returns a summary of what was created, skipped, and not scheduled.
     */
    @Transactional
    public GenerateDeliveryResult generateForDate(LocalDate date) {
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

            Delivery delivery = new Delivery();
            delivery.setSubscription(sub);
            delivery.setDeliveryDate(date);
            delivery.setQuantity(sub.getQuantity());
            delivery.setStatus("PENDING");

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
