package com.dairy.dairy_management.service;

import com.dairy.dairy_management.entity.Subscription;
import com.dairy.dairy_management.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository repo;

    public SubscriptionService(SubscriptionRepository repo) {
        this.repo = repo;
    }

    public Subscription create(Subscription sub) {
        return repo.save(sub);
    }

    public List<Subscription> getAll() {
        return repo.findAll();
    }
    public List<Subscription> getByCustomerId(Long id) {
        return repo.findByCustomerId(id);
    }
}