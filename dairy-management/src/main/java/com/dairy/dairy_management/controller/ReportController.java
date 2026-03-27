package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.CustomerMonthlyReport;
import com.dairy.dairy_management.dto.DailyReportResponse;
import com.dairy.dairy_management.dto.RevenueReportResponse;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    /**
     * Daily delivery report grouped by delivery line.
     * Defaults to today if no date provided.
     * Example: GET /reports/daily?date=2026-03-27
     */
    @GetMapping("/daily")
    public DailyReportResponse daily(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getDailyReport(date != null ? date : LocalDate.now());
    }

    /**
     * Monthly revenue summary — total billed, collected, and pending.
     * Example: GET /reports/revenue?month=3&year=2026
     */
    @GetMapping("/revenue")
    public RevenueReportResponse revenue(@RequestParam int month, @RequestParam int year) {
        return service.getRevenueReport(month, year);
    }

    /**
     * List of all pending (unpaid) bills across all customers.
     * Example: GET /reports/pending-payments
     */
    @GetMapping("/pending-payments")
    public List<Billing> pendingPayments() {
        return service.getPendingPayments();
    }

    /**
     * Full month breakdown for a single customer — deliveries, addons, and bill.
     * Example: GET /reports/customer/1/monthly?month=3&year=2026
     */
    @GetMapping("/customer/{id}/monthly")
    public CustomerMonthlyReport customerMonthly(@PathVariable Long id,
                                                  @RequestParam int month,
                                                  @RequestParam int year) {
        return service.getCustomerMonthlyReport(id, month, year);
    }
}
