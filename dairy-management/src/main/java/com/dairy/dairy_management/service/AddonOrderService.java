package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.CreateAddonOrderRequest;
import com.dairy.dairy_management.entity.AddonOrder;
import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.entity.Product;
import com.dairy.dairy_management.repository.AddonOrderRepository;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AddonOrderService {

    private final AddonOrderRepository repo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;

    public AddonOrderService(AddonOrderRepository repo,
                             CustomerRepository customerRepo,
                             ProductRepository productRepo) {
        this.repo = repo;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
    }

    public AddonOrder create(CreateAddonOrderRequest request) {
        Customer customer = customerRepo.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Product product = productRepo.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

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
                .orElseThrow(() -> new RuntimeException("Addon order not found"));
    }

    public List<AddonOrder> getByCustomer(Long customerId) {
        customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return repo.findByCustomerId(customerId);
    }

    public List<AddonOrder> getByDate(LocalDate date) {
        return repo.findByDeliveryDate(date);
    }

    public List<AddonOrder> getByCustomerAndDate(Long customerId, LocalDate date) {
        return repo.findByCustomerIdAndDeliveryDate(customerId, date);
    }

    public AddonOrder updateStatus(Long id, String status) {
        List<String> validStatuses = List.of("PENDING", "DELIVERED", "SKIPPED");
        String upperStatus = status.toUpperCase();

        if (!validStatuses.contains(upperStatus)) {
            throw new RuntimeException(
                    "Invalid status '" + status + "'. Valid values: PENDING, DELIVERED, SKIPPED");
        }

        AddonOrder order = getById(id);
        order.setStatus(upperStatus);
        return repo.save(order);
    }

    public void delete(Long id) {
        AddonOrder order = getById(id);
        if ("DELIVERED".equals(order.getStatus())) {
            throw new RuntimeException("Cannot delete a delivered addon order");
        }
        repo.delete(order);
    }
}
