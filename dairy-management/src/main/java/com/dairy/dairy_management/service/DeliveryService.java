package com.dairy.dairy_management.service;

import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.repository.DeliveryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeliveryService {

    private final DeliveryRepository repo;

    public DeliveryService(DeliveryRepository repo) {
        this.repo = repo;
    }

    public Delivery create(Delivery delivery) {
        return repo.save(delivery);
    }

    public List<Delivery> getAll() {
        return repo.findAll();
    }

    public List<Delivery> getBySubscription(Long subscriptionId) {
        return repo.findBySubscriptionId(subscriptionId);
    }
}