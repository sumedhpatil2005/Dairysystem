package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.UpdateProductPriceRequest;
import com.dairy.dairy_management.entity.Product;
import com.dairy.dairy_management.entity.ProductPriceHistory;
import com.dairy.dairy_management.service.ProductPriceHistoryService;
import com.dairy.dairy_management.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService service;
    private final ProductPriceHistoryService priceHistoryService;

    public ProductController(ProductService service,
                             ProductPriceHistoryService priceHistoryService) {
        this.service = service;
        this.priceHistoryService = priceHistoryService;
    }

    @PostMapping
    public Product create(@Valid @RequestBody Product product) {
        return service.create(product);
    }

    @GetMapping
    public List<Product> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * Update the price of a product. Records price history automatically.
     * Example: PATCH /products/1/price
     * { "newPrice": 65.0, "effectiveFrom": "2026-04-01", "reason": "Seasonal increase" }
     */
    @PatchMapping("/{id}/price")
    public Product updatePrice(@PathVariable Long id,
                               @Valid @RequestBody UpdateProductPriceRequest request) {
        return priceHistoryService.updatePrice(id, request);
    }

    /**
     * Get full price history for a product (newest first).
     * Example: GET /products/1/price-history
     */
    @GetMapping("/{id}/price-history")
    public List<ProductPriceHistory> getPriceHistory(@PathVariable Long id) {
        return priceHistoryService.getHistory(id);
    }

    /**
     * Delete a product. Blocked if any active subscriptions reference it.
     * Example: DELETE /products/1
     */
    @DeleteMapping("/{id}")
    public org.springframework.http.ResponseEntity<String> delete(@PathVariable Long id) {
        service.delete(id);
        return org.springframework.http.ResponseEntity.ok("Product deleted successfully");
    }
}
