package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.CreateSubscriptionRequest;
import com.dairy.dairy_management.dto.ModifySubscriptionRequest;
import com.dairy.dairy_management.entity.*;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.ProductRepository;
import com.dairy.dairy_management.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class SubscriptionService {

    private final SubscriptionRepository repo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;

    public SubscriptionService(SubscriptionRepository repo,
                               CustomerRepository customerRepo,
                               ProductRepository productRepo) {
        this.repo = repo;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
    }

    public Subscription create(CreateSubscriptionRequest request) {
        Customer customer = customerRepo.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        validateDeliveryDays(request.getFrequency(), request.getDeliveryDays());

        Subscription sub = new Subscription();
        sub.setCustomer(customer);
        sub.setProduct(product);
        sub.setQuantity(request.getQuantity());
        sub.setFrequency(request.getFrequency());
        sub.setDeliverySlot(request.getDeliverySlot());
        sub.setDeliveryDays(request.getDeliveryDays());
        sub.setStartDate(request.getStartDate());

        return repo.save(sub);
    }

    public List<Subscription> getAll() {
        return repo.findAll();
    }

    public Subscription getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
    }

    public List<Subscription> getByCustomerId(Long customerId) {
        return repo.findByCustomerId(customerId);
    }

    public List<Subscription> getActiveByCustomerId(Long customerId) {
        return repo.findByCustomerIdAndEndDateIsNull(customerId);
    }

    /**
     * Mid-cycle modification:
     * 1. Close the existing subscription (endDate = effectiveDate - 1)
     * 2. Create a new subscription with the updated values from effectiveDate
     * Both records are kept so billing can calculate accurately for each period.
     */
    @Transactional
    public Subscription modify(Long subscriptionId, ModifySubscriptionRequest request) {
        Subscription existing = repo.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (existing.getEndDate() != null && !existing.getEndDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("Cannot modify a cancelled subscription");
        }

        if (!request.getEffectiveDate().isAfter(existing.getStartDate())) {
            throw new RuntimeException("Effective date must be after the subscription start date");
        }

        validateDeliveryDays(request.getFrequency(), request.getDeliveryDays());

        // Close the old subscription one day before the effective date
        existing.setEndDate(request.getEffectiveDate().minusDays(1));
        repo.save(existing);

        // Create the new subscription starting from effective date
        Subscription updated = new Subscription();
        updated.setCustomer(existing.getCustomer());
        updated.setProduct(existing.getProduct());
        updated.setQuantity(request.getQuantity());
        updated.setFrequency(request.getFrequency());
        updated.setDeliverySlot(request.getDeliverySlot());
        updated.setDeliveryDays(request.getDeliveryDays());
        updated.setStartDate(request.getEffectiveDate());

        return repo.save(updated);
    }

    /**
     * Cancel a subscription — sets endDate to today.
     * Historical delivery records are preserved for billing.
     */
    public void cancel(Long subscriptionId) {
        Subscription sub = repo.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (sub.getEndDate() != null && !sub.getEndDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("Subscription is already cancelled");
        }

        sub.setEndDate(LocalDate.now());
        repo.save(sub);
    }

    /**
     * Returns all subscriptions active on a given date.
     * Used by Goal 5 (auto-generate daily deliveries).
     */
    public List<Subscription> getActiveOnDate(LocalDate date) {
        List<Subscription> openEnded = repo.findByStartDateLessThanEqualAndEndDateIsNull(date);
        List<Subscription> withEndDate = repo.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
        openEnded.addAll(withEndDate);
        return openEnded;
    }

    /**
     * Checks whether a subscription should deliver on the given date
     * based on its frequency and deliveryDays settings.
     */
    public boolean shouldDeliverOn(Subscription sub, LocalDate date) {
        return switch (sub.getFrequency()) {
            case DAILY -> true;
            case ALTERNATE_DAY -> {
                long daysBetween = sub.getStartDate().until(date).getDays();
                yield daysBetween % 2 == 0;
            }
            case CUSTOM_WEEKLY -> {
                String dayName = date.getDayOfWeek().name();
                yield sub.getDeliveryDays() != null && sub.getDeliveryDays().contains(dayName);
            }
        };
    }

    private void validateDeliveryDays(FrequencyType frequency, Set<String> deliveryDays) {
        if (frequency == FrequencyType.CUSTOM_WEEKLY) {
            if (deliveryDays == null || deliveryDays.isEmpty()) {
                throw new RuntimeException("deliveryDays is required when frequency is CUSTOM_WEEKLY");
            }
            for (String day : deliveryDays) {
                try {
                    DayOfWeek.valueOf(day.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid day: " + day +
                            ". Valid values: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY");
                }
            }
        }
    }
}
