package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.CreateSubscriptionRequest;
import com.dairy.dairy_management.dto.ModifySubscriptionRequest;
import com.dairy.dairy_management.entity.*;
import com.dairy.dairy_management.exception.ConflictException;
import com.dairy.dairy_management.exception.NotFoundException;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.ProductRepository;
import com.dairy.dairy_management.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        // #12: Block subscriptions starting in the past
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Start date cannot be in the past. Use today's date or a future date.");
        }

        // #17: Validate deliveryDays against frequency
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
                .orElseThrow(() -> new NotFoundException("Subscription not found"));
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
     */
    @Transactional
    public Subscription modify(Long subscriptionId, ModifySubscriptionRequest request) {
        Subscription existing = repo.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));

        if (existing.getEndDate() != null && !existing.getEndDate().isAfter(LocalDate.now())) {
            throw new ConflictException("Cannot modify a cancelled subscription");
        }

        if (!request.getEffectiveDate().isAfter(existing.getStartDate())) {
            throw new IllegalArgumentException("Effective date must be after the subscription start date");
        }

        validateDeliveryDays(request.getFrequency(), request.getDeliveryDays());

        existing.setEndDate(request.getEffectiveDate().minusDays(1));
        repo.save(existing);

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
                .orElseThrow(() -> new NotFoundException("Subscription not found"));

        if (sub.getEndDate() != null && !sub.getEndDate().isAfter(LocalDate.now())) {
            throw new ConflictException("Subscription is already cancelled");
        }

        sub.setEndDate(LocalDate.now());
        repo.save(sub);
    }

    /**
     * Pause a subscription — no deliveries will be generated until resumed.
     * Use DeliveryOverride.PAUSED for single-day pauses; use this for indefinite pauses.
     */
    public Subscription pause(Long subscriptionId) {
        Subscription sub = repo.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));

        if (sub.getEndDate() != null) {
            throw new ConflictException("Cannot pause a cancelled subscription");
        }
        if (sub.isPaused()) {
            throw new ConflictException("Subscription is already paused");
        }

        sub.setPaused(true);
        return repo.save(sub);
    }

    /**
     * Resume a previously paused subscription.
     */
    public Subscription resume(Long subscriptionId) {
        Subscription sub = repo.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));

        if (!sub.isPaused()) {
            throw new ConflictException("Subscription is not paused");
        }

        sub.setPaused(false);
        return repo.save(sub);
    }

    /**
     * Returns all subscriptions active on a given date (not cancelled, not paused-globally).
     * Paused subscriptions are included — the generation loop skips them individually.
     */
    public List<Subscription> getActiveOnDate(LocalDate date) {
        List<Subscription> openEnded = repo.findByStartDateLessThanEqualAndEndDateIsNull(date);
        List<Subscription> withEndDate = repo.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
        openEnded.addAll(withEndDate);
        return openEnded;
    }

    /**
     * Checks whether a subscription should deliver on the given date.
     *
     * ALTERNATE_DAY: Uses ChronoUnit.DAYS.between() to get total elapsed days correctly
     * across month boundaries. (Period.getDays() only returns the days component of a
     * period like P1M2D, which would incorrectly return 2 instead of ~32 days.)
     * Delivers on day 0 from startDate, then day 2, 4, 6, etc.
     */
    public boolean shouldDeliverOn(Subscription sub, LocalDate date) {
        return switch (sub.getFrequency()) {
            case DAILY -> true;
            case ALTERNATE_DAY -> {
                long daysBetween = ChronoUnit.DAYS.between(sub.getStartDate(), date);
                yield daysBetween % 2 == 0;
            }
            case CUSTOM_WEEKLY -> {
                String dayName = date.getDayOfWeek().name();
                yield sub.getDeliveryDays() != null && sub.getDeliveryDays().contains(dayName);
            }
        };
    }

    /**
     * Validates deliveryDays against the chosen frequency:
     * - CUSTOM_WEEKLY: deliveryDays required, must contain valid day names
     * - DAILY / ALTERNATE_DAY: deliveryDays must be null or empty (field is ignored otherwise)
     */
    private void validateDeliveryDays(FrequencyType frequency, Set<String> deliveryDays) {
        if (frequency == FrequencyType.CUSTOM_WEEKLY) {
            if (deliveryDays == null || deliveryDays.isEmpty()) {
                throw new IllegalArgumentException(
                        "deliveryDays is required when frequency is CUSTOM_WEEKLY");
            }
            for (String day : deliveryDays) {
                try {
                    DayOfWeek.valueOf(day.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid day: " + day +
                            ". Valid values: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY");
                }
            }
        } else {
            // DAILY and ALTERNATE_DAY do not use deliveryDays — warn if provided
            if (deliveryDays != null && !deliveryDays.isEmpty()) {
                throw new IllegalArgumentException(
                        "deliveryDays should not be set when frequency is " + frequency +
                        ". Remove deliveryDays from the request.");
            }
        }
    }
}
