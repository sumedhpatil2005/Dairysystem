package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.AssignCustomerRequest;
import com.dairy.dairy_management.dto.DeliveryLineResponse;
import com.dairy.dairy_management.dto.ResequenceRequest;
import com.dairy.dairy_management.entity.DeliveryLine;
import com.dairy.dairy_management.service.DeliveryLineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lines")
public class DeliveryLineController {

    private final DeliveryLineService service;

    public DeliveryLineController(DeliveryLineService service) {
        this.service = service;
    }

    // Create a new delivery line
    @PostMapping
    public ResponseEntity<DeliveryLine> create(@Valid @RequestBody DeliveryLine line) {
        return ResponseEntity.ok(service.createLine(line));
    }

    // Get all delivery lines
    @GetMapping
    public ResponseEntity<List<DeliveryLine>> getAll() {
        return ResponseEntity.ok(service.getAllLines());
    }

    // Get a single line with customers in sequence order
    @GetMapping("/{id}")
    public ResponseEntity<DeliveryLineResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getLineById(id));
    }

    // Assign a customer to this line with a sequence number
    @PostMapping("/{id}/assign-customer")
    public ResponseEntity<String> assignCustomer(@PathVariable Long id,
                                                  @Valid @RequestBody AssignCustomerRequest request) {
        service.assignCustomer(id, request);
        return ResponseEntity.ok("Customer assigned to line successfully");
    }

    // Remove a customer from this line
    @DeleteMapping("/{lineId}/customers/{customerId}")
    public ResponseEntity<String> removeCustomer(@PathVariable Long lineId,
                                                  @PathVariable Long customerId) {
        service.removeCustomerFromLine(lineId, customerId);
        return ResponseEntity.ok("Customer removed from line successfully");
    }

    // Reorder customers — pass list of customer IDs in the desired delivery sequence
    @PutMapping("/{id}/resequence")
    public ResponseEntity<String> resequence(@PathVariable Long id,
                                              @Valid @RequestBody ResequenceRequest request) {
        service.resequenceCustomers(id, request);
        return ResponseEntity.ok("Customer sequence updated successfully");
    }

    // Delete a line (only if no customers assigned)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.deleteLine(id);
        return ResponseEntity.ok("Delivery line deleted successfully");
    }
}
