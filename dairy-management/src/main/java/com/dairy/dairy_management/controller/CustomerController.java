package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
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
     * All params are optional and can be combined.
     * Example: GET /customers?name=ram&society=greenpark&lineId=2
     */
    @GetMapping
    public List<Customer> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String society,
            @RequestParam(required = false) Long lineId) {
        if (name == null && phone == null && society == null && lineId == null) {
            return service.getAll();
        }
        return service.search(name, phone, society, lineId);
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

    // Hard delete — kept for backward compatibility
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Customer deleted successfully";
    }
}
