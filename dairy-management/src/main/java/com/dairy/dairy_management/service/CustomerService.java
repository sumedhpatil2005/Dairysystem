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

    }

