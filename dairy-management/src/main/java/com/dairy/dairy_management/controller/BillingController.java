package com.dairy.dairy_management.controller;

import com.dairy.dairy_management.dto.BillAdjustmentRequest;
import com.dairy.dairy_management.dto.BillResponse;
import com.dairy.dairy_management.dto.BulkBillResult;
import com.dairy.dairy_management.dto.PaymentResponse;
import com.dairy.dairy_management.dto.RecordPaymentRequest;
import com.dairy.dairy_management.entity.BillAdjustment;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.entity.Payment;
import com.dairy.dairy_management.service.BillingService;
import com.dairy.dairy_management.service.PaymentService;
import com.dairy.dairy_management.service.PdfInvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final BillingService service;
    private final PaymentService paymentService;
    private final PdfInvoiceService pdfInvoiceService;

    public BillingController(BillingService service,
                             PaymentService paymentService,
                             PdfInvoiceService pdfInvoiceService) {
        this.service = service;
        this.paymentService = paymentService;
        this.pdfInvoiceService = pdfInvoiceService;
    }

    /**
     * Generate (or regenerate) the monthly bill for a customer.
     * month must be 1–12, year must be >= 2000.
     * Example: POST /billing/generate?customerId=1&month=3&year=2026
     */
    @PostMapping("/generate")
    public BillResponse generate(@RequestParam Long customerId,
                                 @RequestParam int month,
                                 @RequestParam int year) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        if (year < 2000) {
            throw new IllegalArgumentException("year must be 2000 or later");
        }
        return service.generateBill(customerId, month, year);
    }

    @GetMapping("/{id}")
    public Billing getBill(@PathVariable Long id) {
        return service.getBillById(id);
    }

    /**
     * Get full line-item breakdown for a bill.
     * Example: GET /billing/1/detail
     */
    @GetMapping("/{id}/detail")
    public BillResponse getBillDetail(@PathVariable Long id) {
        return service.getBillDetail(id);
    }

    /**
     * Get all bills for a customer (history).
     * Example: GET /billing/customer/1
     */
    /**
     * Generate bills for ALL active customers for a given month — in one call.
     * Idempotent: regenerates existing bills without losing payments or adjustments.
     * Example: POST /billing/generate-all?month=3&year=2026
     */
    @PostMapping("/generate-all")
    public BulkBillResult generateAll(@RequestParam int month, @RequestParam int year) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        if (year < 2000) {
            throw new IllegalArgumentException("year must be 2000 or later");
        }
        return service.generateAllBills(month, year);
    }

    @GetMapping("/customer/{id}")
    public List<Billing> getByCustomer(@PathVariable Long id) {
        return service.getBillsByCustomer(id);
    }

    /**
     * Get all pending (unpaid) bills — paginated.
     * Example: GET /billing/pending?page=0&size=20
     */
    @GetMapping("/pending")
    public Page<Billing> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.getPendingBills(PageRequest.of(page, size, Sort.by("year", "month")));
    }

    /**
     * Get all bills for a specific month/year — paginated.
     * Example: GET /billing/month?month=3&year=2026&page=0&size=20
     */
    @GetMapping("/month")
    public Page<Billing> getByMonth(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        if (year < 2000) {
            throw new IllegalArgumentException("year must be 2000 or later");
        }
        return service.getBillsByMonth(month, year, PageRequest.of(page, size));
    }

    // --- Payments ---

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public Payment recordPayment(@PathVariable Long id,
                                 @Valid @RequestBody RecordPaymentRequest request) {
        return paymentService.recordPayment(id, request);
    }

    @GetMapping("/{id}/payments")
    public List<PaymentResponse> getPayments(@PathVariable Long id) {
        return paymentService.getPaymentsByBill(id);
    }

    // --- Bill Adjustments ---

    @PostMapping("/{id}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public BillAdjustment addAdjustment(@PathVariable Long id,
                                        @Valid @RequestBody BillAdjustmentRequest request) {
        return service.addAdjustment(id, request);
    }

    @GetMapping("/{id}/adjustments")
    public List<BillAdjustment> getAdjustments(@PathVariable Long id) {
        return service.getAdjustments(id);
    }

    @DeleteMapping("/{id}/adjustments/{adjId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAdjustment(@PathVariable Long id, @PathVariable Long adjId) {
        service.removeAdjustment(id, adjId);
    }

    /**
     * Download a PDF invoice for a bill.
     * The file is named "invoice-BILL-{id}.pdf" in the Content-Disposition header.
     * Example: GET /billing/1/invoice/pdf
     */
    @GetMapping("/{id}/invoice/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        byte[] pdf = pdfInvoiceService.generateInvoicePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("invoice-BILL-" + id + ".pdf")
                        .build());
        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
