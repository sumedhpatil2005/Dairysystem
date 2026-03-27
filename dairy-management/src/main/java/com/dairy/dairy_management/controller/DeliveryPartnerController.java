package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.*;
import com.dairy.dairy_management.service.DeliveryPartnerService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/delivery-partners")
public class DeliveryPartnerController {

    private final DeliveryPartnerService service;

    public DeliveryPartnerController(DeliveryPartnerService service) {
        this.service = service;
    }

    // Create a delivery partner profile (links to an existing DELIVERY_PARTNER user)
    @PostMapping
    public ResponseEntity<DeliveryPartnerResponse> create(@Valid @RequestBody CreateDeliveryPartnerRequest request) {
        return ResponseEntity.ok(service.createPartner(request));
    }

    // List all delivery partners
    @GetMapping
    public ResponseEntity<List<DeliveryPartnerResponse>> getAll() {
        return ResponseEntity.ok(service.getAllPartners());
    }

    // Get a single delivery partner with their assigned lines
    @GetMapping("/{id}")
    public ResponseEntity<DeliveryPartnerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPartnerById(id));
    }

    // Assign a delivery line to this partner
    @PostMapping("/{id}/lines")
    public ResponseEntity<String> assignLine(@PathVariable Long id,
                                              @Valid @RequestBody AssignLineRequest request) {
        service.assignLine(id, request);
        return ResponseEntity.ok("Line assigned to delivery partner successfully");
    }

    // Remove a delivery line from this partner
    @DeleteMapping("/{partnerId}/lines/{lineId}")
    public ResponseEntity<String> removeLine(@PathVariable Long partnerId,
                                              @PathVariable Long lineId) {
        service.removeLine(partnerId, lineId);
        return ResponseEntity.ok("Line removed from delivery partner successfully");
    }

    // Reorder assigned lines — pass list of line IDs in desired delivery order
    @PutMapping("/{id}/lines/resequence")
    public ResponseEntity<String> resequenceLines(@PathVariable Long id,
                                                   @Valid @RequestBody ResequenceLinesRequest request) {
        service.resequenceLines(id, request);
        return ResponseEntity.ok("Line sequence updated successfully");
    }

    // Get daily delivery list for a partner — used by Flutter app
    @GetMapping("/{id}/daily-list")
    public ResponseEntity<DailyDeliveryListResponse> getDailyList(
            @PathVariable Long id,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().toString()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.getDailyList(id, date));
    }

    /**
     * Delivery partner gets their OWN daily list using their JWT token — no need to know their partner ID.
     * Example: GET /delivery-partners/me/daily-list
     * Example: GET /delivery-partners/me/daily-list?date=2026-03-27
     */
    @GetMapping("/me/daily-list")
    public ResponseEntity<DailyDeliveryListResponse> getMyDailyList(
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        String username = authentication.getName();
        return ResponseEntity.ok(service.getDailyListByUsername(username, date));
    }
}
