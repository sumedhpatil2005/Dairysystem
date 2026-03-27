package com.dairy.dairy_management.service;

import com.dairy.dairy_management.entity.Product;
import com.dairy.dairy_management.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final ProductPriceHistoryService priceHistoryService;

    public ProductService(ProductRepository repo, ProductPriceHistoryService priceHistoryService) {
        this.repo = repo;
        this.priceHistoryService = priceHistoryService;
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
}
