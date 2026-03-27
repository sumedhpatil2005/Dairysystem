package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.UpdateProductPriceRequest;
import com.dairy.dairy_management.entity.Product;
import com.dairy.dairy_management.entity.ProductPriceHistory;
import com.dairy.dairy_management.repository.ProductPriceHistoryRepository;
import com.dairy.dairy_management.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProductPriceHistoryService {

    private final ProductPriceHistoryRepository historyRepo;
    private final ProductRepository productRepo;

    public ProductPriceHistoryService(ProductPriceHistoryRepository historyRepo,
                                      ProductRepository productRepo) {
        this.historyRepo = historyRepo;
        this.productRepo = productRepo;
    }

    /**
     * Records initial price history when a product is first created.
     */
    public void seedInitialPrice(Product product) {
        ProductPriceHistory entry = new ProductPriceHistory();
        entry.setProduct(product);
        entry.setPrice(product.getPricePerUnit());
        entry.setEffectiveFrom(LocalDate.now());
        entry.setEffectiveTo(null); // currently active
        entry.setReason("Initial price");
        historyRepo.save(entry);
    }

    /**
     * Updates the product price and closes the current history entry.
     * Creates a new entry from effectiveFrom (defaults to today).
     */
    @Transactional
    public Product updatePrice(Long productId, UpdateProductPriceRequest request) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        LocalDate effectiveFrom = request.getEffectiveFrom() != null
                ? request.getEffectiveFrom()
                : LocalDate.now();

        // Close the current active price entry
        historyRepo.findByProductIdAndEffectiveToIsNull(productId).ifPresent(current -> {
            current.setEffectiveTo(effectiveFrom.minusDays(1));
            historyRepo.save(current);
        });

        // Create new price history entry
        ProductPriceHistory newEntry = new ProductPriceHistory();
        newEntry.setProduct(product);
        newEntry.setPrice(request.getNewPrice());
        newEntry.setEffectiveFrom(effectiveFrom);
        newEntry.setEffectiveTo(null); // now the active price
        newEntry.setReason(request.getReason());
        historyRepo.save(newEntry);

        // Update the product's current price
        product.setPricePerUnit(request.getNewPrice());
        return productRepo.save(product);
    }

    public List<ProductPriceHistory> getHistory(Long productId) {
        productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return historyRepo.findByProductIdOrderByEffectiveFromDesc(productId);
    }

    /**
     * Returns the price effective on a given date.
     * Falls back to current product price if no history found.
     */
    public double getEffectivePriceOnDate(Long productId, LocalDate date) {
        return historyRepo.findByProductIdOrderByEffectiveFromDesc(productId)
                .stream()
                .filter(h -> !h.getEffectiveFrom().isAfter(date)
                        && (h.getEffectiveTo() == null || !h.getEffectiveTo().isBefore(date)))
                .findFirst()
                .map(ProductPriceHistory::getPrice)
                .orElseGet(() -> productRepo.findById(productId)
                        .map(Product::getPricePerUnit)
                        .orElse(0.0));
    }
}
