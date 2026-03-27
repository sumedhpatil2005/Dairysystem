package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.CreateDeliveryOverrideRequest;
import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.entity.DeliveryOverride;
import com.dairy.dairy_management.entity.OverrideType;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.DeliveryOverrideRepository;
import com.dairy.dairy_management.repository.DeliveryRepository;
import com.dairy.dairy_management.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryOverrideService {

    private final DeliveryOverrideRepository repo;
    private final CustomerRepository customerRepo;
    private final DeliveryRepository deliveryRepo;
    private final SubscriptionRepository subscriptionRepo;

    public DeliveryOverrideService(DeliveryOverrideRepository repo,
                                   CustomerRepository customerRepo,
                                   DeliveryRepository deliveryRepo,
                                   SubscriptionRepository subscriptionRepo) {
        this.repo = repo;
        this.customerRepo = customerRepo;
        this.deliveryRepo = deliveryRepo;
        this.subscriptionRepo = subscriptionRepo;
    }

    @Transactional
    public DeliveryOverride create(CreateDeliveryOverrideRequest request) {
        Customer customer = customerRepo.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (request.getOverrideType() == OverrideType.QUANTITY_MODIFIED
                && request.getModifiedQuantity() == null) {
            throw new RuntimeException("modifiedQuantity is required for QUANTITY_MODIFIED override");
        }

        // Prevent duplicate override of same type for same customer+date
        Optional<DeliveryOverride> existing = repo.findByCustomerIdAndDateAndOverrideType(
                request.getCustomerId(), request.getDate(), request.getOverrideType());
        if (existing.isPresent()) {
            throw new RuntimeException("An override of type " + request.getOverrideType()
                    + " already exists for this customer on " + request.getDate());
        }

        DeliveryOverride override = new DeliveryOverride();
        override.setCustomer(customer);
        override.setDate(request.getDate());
        override.setOverrideType(request.getOverrideType());
        override.setModifiedQuantity(request.getModifiedQuantity());
        override.setReason(request.getReason());

        DeliveryOverride saved = repo.save(override);

        // Retroactively update any already-generated delivery records for this customer on this date
        applyToExistingDeliveries(customer.getId(), request.getDate(), request.getOverrideType(),
                request.getModifiedQuantity());

        return saved;
    }

    public List<DeliveryOverride> getByCustomer(Long customerId) {
        customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return repo.findByCustomerId(customerId);
    }

    public List<DeliveryOverride> getByDate(LocalDate date) {
        return repo.findByDate(date);
    }

    public List<DeliveryOverride> getByCustomerAndDate(Long customerId, LocalDate date) {
        return repo.findByCustomerIdAndDate(customerId, date);
    }

    public DeliveryOverride getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery override not found"));
    }

    @Transactional
    public void delete(Long id) {
        DeliveryOverride override = getById(id);
        repo.delete(override);

        // Revert deliveries: if we remove a PAUSED override, set affected deliveries back to PENDING
        // If we remove QUANTITY_MODIFIED, restore quantity from subscription
        if (override.getOverrideType() == OverrideType.PAUSED) {
            revertPausedDeliveries(override.getCustomer().getId(), override.getDate());
        } else if (override.getOverrideType() == OverrideType.QUANTITY_MODIFIED) {
            revertQuantityDeliveries(override.getCustomer().getId(), override.getDate());
        }
    }

    // --- private helpers ---

    private void applyToExistingDeliveries(Long customerId, LocalDate date,
                                            OverrideType type, Double modifiedQty) {
        List<Delivery> deliveries = deliveryRepo
                .findBySubscription_CustomerIdAndDeliveryDate(customerId, date);

        for (Delivery d : deliveries) {
            if (type == OverrideType.PAUSED) {
                d.setStatus("SKIPPED");
            } else if (type == OverrideType.QUANTITY_MODIFIED && modifiedQty != null) {
                d.setQuantity(modifiedQty);
            }
            deliveryRepo.save(d);
        }
    }

    private void revertPausedDeliveries(Long customerId, LocalDate date) {
        List<Delivery> deliveries = deliveryRepo
                .findBySubscription_CustomerIdAndDeliveryDate(customerId, date);
        for (Delivery d : deliveries) {
            if ("SKIPPED".equals(d.getStatus())) {
                d.setStatus("PENDING");
                deliveryRepo.save(d);
            }
        }
    }

    private void revertQuantityDeliveries(Long customerId, LocalDate date) {
        List<Delivery> deliveries = deliveryRepo
                .findBySubscription_CustomerIdAndDeliveryDate(customerId, date);
        for (Delivery d : deliveries) {
            // Restore quantity from the subscription
            d.setQuantity(d.getSubscription().getQuantity());
            deliveryRepo.save(d);
        }
    }
}
