package com.dairy.dairy_management.service;

import com.dairy.dairy_management.entity.Product;
import com.dairy.dairy_management.exception.ConflictException;
import com.dairy.dairy_management.exception.NotFoundException;
import com.dairy.dairy_management.repository.ProductRepository;
import com.dairy.dairy_management.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final ProductPriceHistoryService priceHistoryService;
    private final SubscriptionRepository subscriptionRepo;

    public ProductService(ProductRepository repo,
                          ProductPriceHistoryService priceHistoryService,
                          SubscriptionRepository subscriptionRepo) {
        this.repo = repo;
        this.priceHistoryService = priceHistoryService;
        this.subscriptionRepo = subscriptionRepo;
    }

    public Product create(Product product) {
        Product saved = repo.save(product);
        // Seed initial price history entry
        priceHistoryService.seedInitialPrice(saved);
        return saved;
    }

    public Product getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public List<Product> getAll() {
        return repo.findAll();
    }

    /**
     * Deletes a product only if it has no active subscriptions.
     * Prevents silent data corruption in delivery generation and billing.
     */
    public void delete(Long id) {
        Product product = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        if (subscriptionRepo.existsByProductIdAndEndDateIsNull(id)) {
            throw new ConflictException(
                    "Cannot delete product '" + product.getName() + "' — " +
                    "it has active subscriptions. Cancel all subscriptions for this " +
                    "product first.");
        }

        repo.deleteById(id);
    }
}
