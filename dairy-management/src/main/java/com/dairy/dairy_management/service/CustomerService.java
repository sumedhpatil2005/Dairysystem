package com.dairy.dairy_management.service;

import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository repo;

    public CustomerService(CustomerRepository repo) {
        this.repo = repo;
    }

    public Customer create(Customer customer) {
        if (repo.findByPhone(customer.getPhone()).isPresent()) {
            throw new RuntimeException("Phone already exists");
        }
        customer.setActive(true);
        return repo.save(customer);
    }

    public Customer getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    // Returns only active customers (default list view)
    public List<Customer> getAll() {
        return repo.findByIsActiveTrue();
    }

    // Returns all customers including inactive (admin full view)
    public List<Customer> getAllIncludingInactive() {
        return repo.findAll();
    }

    // Search active customers by name / phone / society / deliveryLine
    public List<Customer> search(String name, String phone, String society, Long lineId) {
        return repo.search(
                isBlank(name) ? null : name,
                isBlank(phone) ? null : phone,
                isBlank(society) ? null : society,
                lineId
        );
    }

    public Customer update(Long id, Customer updated) {
        Customer existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Check phone uniqueness only if changed
        if (!existing.getPhone().equals(updated.getPhone())
                && repo.findByPhone(updated.getPhone()).isPresent()) {
            throw new RuntimeException("Phone already exists");
        }

        existing.setName(updated.getName());
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        existing.setSocietyName(updated.getSocietyName());

        return repo.save(existing);
    }

    // Soft delete — marks customer inactive, does not remove from DB
    public Customer deactivate(Long id) {
        Customer customer = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (!customer.isActive()) {
            throw new RuntimeException("Customer is already inactive");
        }
        customer.setActive(false);
        return repo.save(customer);
    }

    // Re-activate a previously deactivated customer
    public Customer activate(Long id) {
        Customer customer = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (customer.isActive()) {
            throw new RuntimeException("Customer is already active");
        }
        customer.setActive(true);
        return repo.save(customer);
    }

    // Hard delete kept for backward compatibility (use deactivate in production)
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Customer not found");
        }
        repo.deleteById(id);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
