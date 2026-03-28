package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.DashboardResponse;
import com.dairy.dairy_management.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    /**
     * Admin home screen summary — today's delivery counts, current month billing,
     * pending bills, active customers, and delivery partner count.
     *
     * Example: GET /dashboard
     */
    @GetMapping
    public DashboardResponse getSummary() {
        return service.getSummary();
    }
}
