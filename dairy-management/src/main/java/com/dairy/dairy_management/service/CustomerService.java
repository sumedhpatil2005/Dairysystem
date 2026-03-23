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
        return repo.save(customer);
    }


        public Customer getById(Long id) { return repo.findById(id) .orElseThrow(() -> new RuntimeException("Customer not found")); }



public Customer update(Long id, Customer updated) {
    Customer existing = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found"));

    existing.setName(updated.getName());
    existing.setPhone(updated.getPhone());

    return repo.save(existing);
}
public void delete(Long id) {
    if (!repo.existsById(id)) {
        throw new RuntimeException("Customer not found");
    }
    repo.deleteById(id);
}}