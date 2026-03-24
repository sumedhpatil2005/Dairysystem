package com.dairy.dairy_management.service;

import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.entity.Product;
import com.dairy.dairy_management.entity.Subscription;
import com.dairy.dairy_management.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.ProductRepository;
import java.util.List;

@Service
public class SubscriptionService {
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final SubscriptionRepository repo;

    public SubscriptionService(SubscriptionRepository repo,
                               CustomerRepository customerRepo,
                               ProductRepository productRepo) {
        this.repo = repo;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
    }

    public Subscription create(Subscription sub) {

        Customer customer = customerRepo.findById(sub.getCustomer().getId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Product product = productRepo.findById(sub.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        sub.setCustomer(customer);
        sub.setProduct(product);

        return repo.save(sub);
    }

    public List<Subscription> getAll() {
        return repo.findAll();
    }
    public List<Subscription> getByCustomerId(Long id) {
        return repo.findByCustomerId(id);
    }
}