package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.CreateAddonOrderRequest;
import com.dairy.dairy_management.entity.AddonOrder;
import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.entity.OverrideType;
import com.dairy.dairy_management.entity.Product;
import com.dairy.dairy_management.exception.ConflictException;
import com.dairy.dairy_management.exception.NotFoundException;
import com.dairy.dairy_management.repository.AddonOrderRepository;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.DeliveryOverrideRepository;
import com.dairy.dairy_management.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class AddonOrderService {

    private final AddonOrderRepository repo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final DeliveryOverrideRepository overrideRepo;
    private final AuditLogService auditLogService;

    public AddonOrderService(AddonOrderRepository repo,
                             CustomerRepository customerRepo,
                             ProductRepository productRepo,
                             DeliveryOverrideRepository overrideRepo,
                             AuditLogService auditLogService) {
        this.repo = repo;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
        this.overrideRepo = overrideRepo;
        this.auditLogService = auditLogService;
    }

    public AddonOrder create(CreateAddonOrderRequest request) {
        Customer customer = customerRepo.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        // Block addon orders when the customer has a delivery pause override for that date
        boolean customerPaused = overrideRepo
                .findByCustomerIdAndDateAndOverrideType(
                        customer.getId(), request.getDeliveryDate(), OverrideType.PAUSED)
                .isPresent();
        if (customerPaused) {
            throw new ConflictException(
                    "Customer '" + customer.getName() + "' has a delivery pause on "
                    + request.getDeliveryDate() + ". Remove the pause override first.");
        }

        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        AddonOrder order = new AddonOrder();
        order.setCustomer(customer);
        order.setProduct(product);
        order.setQuantity(request.getQuantity());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setDeliverySlot(request.getDeliverySlot());
        order.setNotes(request.getNotes());
        order.setStatus("PENDING");

        return repo.save(order);
    }

    public AddonOrder getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Addon order not found"));
    }

    public List<AddonOrder> getByCustomer(Long customerId) {
        customerRepo.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        return repo.findByCustomerId(customerId);
    }

    public List<AddonOrder> getByDate(LocalDate date) {
        return repo.findByDeliveryDate(date);
    }

    public List<AddonOrder> getByCustomerAndDate(Long customerId, LocalDate date) {
        return repo.findByCustomerIdAndDeliveryDate(customerId, date);
    }

    /**
     * Updates addon order status.
     *
     * Allowed transitions:
     *   PENDING   → DELIVERED, SKIPPED
     *   DELIVERED → not allowed (use /mark-as-mistake)
     *   SKIPPED   → not allowed
     */
    public AddonOrder updateStatus(Long id, String status) {
        List<String> validStatuses = List.of("PENDING", "DELIVERED", "SKIPPED");
        String newStatus = status.toUpperCase();

        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status '" + status + "'. Valid values: PENDING, DELIVERED, SKIPPED");
        }

        AddonOrder order = getById(id);
        String current = order.getStatus();

        Map<String, List<String>> allowed = Map.of(
                "PENDING",   List.of("DELIVERED", "SKIPPED"),
                "DELIVERED", List.of(),
                "SKIPPED",   List.of()
        );

        List<String> allowedNext = allowed.getOrDefault(current, List.of());
        if (!allowedNext.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Cannot transition addon order from " + current + " to " + newStatus +
                    (current.equals("DELIVERED") || current.equals("SKIPPED")
                            ? ". Use /mark-as-mistake to void a delivered order."
                            : ""));
        }

        order.setStatus(newStatus);
        return repo.save(order);
    }

    /**
     * Marks an addon order as SKIPPED (voided by admin due to mistake).
     * Allowed from any non-SKIPPED status.
     */
    public AddonOrder markAsMistake(Long id) {
        AddonOrder order = getById(id);
        if ("SKIPPED".equals(order.getStatus())) {
            throw new ConflictException("Addon order is already marked as skipped");
        }
        order.setStatus("SKIPPED");
        AddonOrder saved = repo.save(order);

        auditLogService.log("ADDON_VOIDED", "ADDON_ORDER", id,
                String.format("Addon order %d marked as mistake (SKIPPED). Customer=%s Product=%s Date=%s",
                        id, order.getCustomer().getName(),
                        order.getProduct().getName(), order.getDeliveryDate()));

        return saved;
    }

    public void delete(Long id) {
        AddonOrder order = getById(id);
        if ("DELIVERED".equals(order.getStatus())) {
            throw new ConflictException("Cannot delete a delivered addon order");
        }
        repo.delete(order);
    }
}
