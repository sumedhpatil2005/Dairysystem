package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.CustomerBalanceResponse;
import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.service.BillingService;
import com.dairy.dairy_management.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;
    private final BillingService billingService;

    public CustomerController(CustomerService service, BillingService billingService) {
        this.service = service;
        this.billingService = billingService;
    }

    @PostMapping
    public Customer create(@Valid @RequestBody Customer customer) {
        return service.create(customer);
    }

    @GetMapping("/{id}")
    public Customer getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * List active customers with optional search filters.
     * Without filters: returns paginated list sorted by name.
     * With filters: returns flat list (search across all active customers).
     *
     * Example: GET /customers?page=0&size=20
     * Example: GET /customers?name=ram&society=greenpark&lineId=2
     */
    @GetMapping
    public Object search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String society,
            @RequestParam(required = false) Long lineId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        boolean hasFilter = name != null || phone != null || society != null || lineId != null;
        if (hasFilter) {
            return service.search(name, phone, society, lineId);
        }
        return service.getAll(PageRequest.of(page, size, Sort.by("name")));
    }

    /**
     * List all customers including inactive (admin view).
     * Example: GET /customers/all
     */
    @GetMapping("/all")
    public List<Customer> getAll() {
        return service.getAllIncludingInactive();
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @Valid @RequestBody Customer customer) {
        return service.update(id, customer);
    }

    /**
     * Soft-delete: marks customer as inactive. Preserves all history.
     * Example: PATCH /customers/1/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    public Customer deactivate(@PathVariable Long id) {
        return service.deactivate(id);
    }

    /**
     * Re-activate a previously deactivated customer.
     * Example: PATCH /customers/1/activate
     */
    @PatchMapping("/{id}/activate")
    public Customer activate(@PathVariable Long id) {
        return service.activate(id);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Customer deleted successfully";
    }

    /**
     * Total outstanding balance for a customer across all unpaid months.
     * Includes a per-bill breakdown sorted by year/month.
     * Example: GET /customers/1/balance
     */
    @GetMapping("/{id}/balance")
    public CustomerBalanceResponse getBalance(@PathVariable Long id) {
        return billingService.getCustomerBalance(id);
    }
}
