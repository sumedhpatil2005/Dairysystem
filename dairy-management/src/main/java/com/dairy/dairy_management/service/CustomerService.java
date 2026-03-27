package com.dairy.dairy_management.service;

import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.exception.ConflictException;
import com.dairy.dairy_management.exception.NotFoundException;
import com.dairy.dairy_management.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository repo;

    public CustomerService(CustomerRepository repo) {
        this.repo = repo;
    }

    public Customer create(Customer customer) {
        if (repo.findByPhone(customer.getPhone()).isPresent()) {
            throw new ConflictException("Phone number already in use");
        }
        customer.setActive(true);
        return repo.save(customer);
    }

    public Customer getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    // Paginated active customer list
    public Page<Customer> getAll(Pageable pageable) {
        return repo.findByIsActiveTrue(pageable);
    }

    // Non-paginated — used internally by delivery generation and search
    public List<Customer> getAllActive() {
        return repo.findByIsActiveTrue();
    }

    // Returns all customers including inactive (admin full view)
    public List<Customer> getAllIncludingInactive() {
        return repo.findAll();
    }

    /**
     * Search active customers by name / phone / society / deliveryLine.
     * All params are optional — any combination works.
     * Filtering is done in-memory on the active customer list.
     * Acceptable for dairy business scale (hundreds to low thousands of customers).
     */
    public List<Customer> search(String name, String phone, String society, Long lineId) {
        return repo.findByIsActiveTrue().stream()
                .filter(c -> isBlank(name) || c.getName().toLowerCase().contains(name.toLowerCase()))
                .filter(c -> isBlank(phone) || c.getPhone().contains(phone))
                .filter(c -> isBlank(society) || (c.getSocietyName() != null
                        && c.getSocietyName().toLowerCase().contains(society.toLowerCase())))
                .filter(c -> lineId == null || (c.getDeliveryLine() != null
                        && c.getDeliveryLine().getId().equals(lineId)))
                .sorted(Comparator.comparing(Customer::getName))
                .toList();
    }

    public Customer update(Long id, Customer updated) {
        Customer existing = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        if (!existing.getPhone().equals(updated.getPhone())
                && repo.findByPhone(updated.getPhone()).isPresent()) {
            throw new ConflictException("Phone number already in use");
        }

        existing.setName(updated.getName());
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        existing.setSocietyName(updated.getSocietyName());

        return repo.save(existing);
    }

    public Customer deactivate(Long id) {
        Customer customer = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        if (!customer.isActive()) {
            throw new ConflictException("Customer is already inactive");
        }
        customer.setActive(false);
        return repo.save(customer);
    }

    public Customer activate(Long id) {
        Customer customer = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        if (customer.isActive()) {
            throw new ConflictException("Customer is already active");
        }
        customer.setActive(true);
        return repo.save(customer);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Customer not found");
        }
        repo.deleteById(id);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
