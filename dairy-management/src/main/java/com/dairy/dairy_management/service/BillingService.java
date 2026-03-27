package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.BillAdjustmentRequest;
import com.dairy.dairy_management.dto.BillResponse;
import com.dairy.dairy_management.entity.AddonOrder;
import com.dairy.dairy_management.entity.BillAdjustment;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.exception.ConflictException;
import com.dairy.dairy_management.exception.NotFoundException;
import com.dairy.dairy_management.repository.AddonOrderRepository;
import com.dairy.dairy_management.repository.BillAdjustmentRepository;
import com.dairy.dairy_management.repository.BillingRepository;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.DeliveryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingService {

    private final DeliveryRepository deliveryRepo;
    private final AddonOrderRepository addonRepo;
    private final BillingRepository billingRepo;
    private final CustomerRepository customerRepo;
    private final BillAdjustmentRepository adjustmentRepo;
    private final ProductPriceHistoryService priceHistoryService;

    public BillingService(DeliveryRepository deliveryRepo,
                          AddonOrderRepository addonRepo,
                          BillingRepository billingRepo,
                          CustomerRepository customerRepo,
                          BillAdjustmentRepository adjustmentRepo,
                          ProductPriceHistoryService priceHistoryService) {
        this.deliveryRepo = deliveryRepo;
        this.addonRepo = addonRepo;
        this.billingRepo = billingRepo;
        this.customerRepo = customerRepo;
        this.adjustmentRepo = adjustmentRepo;
        this.priceHistoryService = priceHistoryService;
    }

    /**
     * Generates (or regenerates) the monthly bill for a customer.
     *
     * Calculation:
     *   subscriptionAmount = sum of (quantity × price effective on delivery date) for DELIVERED deliveries
     *   addonAmount        = sum of (quantity × price effective on delivery date) for DELIVERED addon orders
     *   previousPending    = remainingAmount of last month's bill (if any)
     *   adjustmentAmount   = net sum of manual adjustments (preserved on regenerate)
     *   totalAmount        = all four combined
     *   remainingAmount    = totalAmount - paidAmount (paidAmount preserved on regenerate)
     *
     * Uses DB date-range queries instead of in-memory month/year stream filtering.
     */
    @Transactional
    public BillResponse generateBill(Long customerId, int month, int year) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        // Reuse existing bill if present (preserves paidAmount and adjustments)
        Billing bill = billingRepo
                .findByCustomerIdAndMonthAndYear(customerId, month, year)
                .orElse(new Billing());

        bill.setCustomer(customer);
        bill.setMonth(month);
        bill.setYear(year);

        // Date range for the requested month — DB does the filtering, not Java streams
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // --- Subscription deliveries (use historically accurate price) ---
        List<Delivery> deliveries = deliveryRepo
                .findBySubscription_CustomerIdAndDeliveryDateBetween(customerId, start, end)
                .stream()
                .filter(d -> "DELIVERED".equals(d.getStatus()))
                .toList();

        List<BillResponse.LineItem> subItems = new ArrayList<>();
        double subAmount = 0;
        for (Delivery d : deliveries) {
            Long productId = d.getSubscription().getProduct().getId();
            double price = priceHistoryService.getEffectivePriceOnDate(productId, d.getDeliveryDate());
            subItems.add(new BillResponse.LineItem(
                    d.getDeliveryDate(),
                    d.getSubscription().getProduct().getName(),
                    d.getSubscription().getProduct().getUnit(),
                    d.getQuantity(),
                    price));
            subAmount += d.getQuantity() * price;
        }

        // --- Addon orders (use historically accurate price) ---
        List<AddonOrder> addons = addonRepo
                .findByCustomerIdAndDeliveryDateBetween(customerId, start, end)
                .stream()
                .filter(a -> "DELIVERED".equals(a.getStatus()))
                .toList();

        List<BillResponse.LineItem> addonItems = new ArrayList<>();
        double addonAmount = 0;
        for (AddonOrder a : addons) {
            double price = priceHistoryService.getEffectivePriceOnDate(
                    a.getProduct().getId(), a.getDeliveryDate());
            addonItems.add(new BillResponse.LineItem(
                    a.getDeliveryDate(),
                    a.getProduct().getName(),
                    a.getProduct().getUnit(),
                    a.getQuantity(),
                    price));
            addonAmount += a.getQuantity() * price;
        }

        // --- Previous month pending ---
        double prevPending = getPreviousMonthPending(customerId, month, year);

        // --- Manual adjustments (preserved across regenerations) ---
        double adjAmount = adjustmentRepo.findByBillId(bill.getId() != null ? bill.getId() : -1L)
                .stream()
                .mapToDouble(BillAdjustment::getAmount)
                .sum();

        double total = subAmount + addonAmount + prevPending + adjAmount;

        bill.setSubscriptionAmount(subAmount);
        bill.setAddonAmount(addonAmount);
        bill.setPreviousPendingAmount(prevPending);
        bill.setAdjustmentAmount(adjAmount);
        bill.setTotalAmount(total);
        double paid = bill.getPaidAmount();
        bill.setRemainingAmount(total - paid);
        bill.setStatus(bill.getRemainingAmount() <= 0 ? "PAID" : "PENDING");

        Billing saved = billingRepo.save(bill);

        List<BillAdjustment> adjustments = adjustmentRepo.findByBillId(saved.getId());
        return toResponse(saved, subItems, addonItems, adjustments);
    }

    public BillResponse getBillDetail(Long billId) {
        Billing bill = billingRepo.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        Long customerId = bill.getCustomer().getId();
        int month = bill.getMonth();
        int year = bill.getYear();

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Delivery> deliveries = deliveryRepo
                .findBySubscription_CustomerIdAndDeliveryDateBetween(customerId, start, end)
                .stream()
                .filter(d -> "DELIVERED".equals(d.getStatus()))
                .toList();

        List<BillResponse.LineItem> subItems = new ArrayList<>();
        for (Delivery d : deliveries) {
            Long productId = d.getSubscription().getProduct().getId();
            double price = priceHistoryService.getEffectivePriceOnDate(productId, d.getDeliveryDate());
            subItems.add(new BillResponse.LineItem(
                    d.getDeliveryDate(),
                    d.getSubscription().getProduct().getName(),
                    d.getSubscription().getProduct().getUnit(),
                    d.getQuantity(), price));
        }

        List<AddonOrder> addons = addonRepo
                .findByCustomerIdAndDeliveryDateBetween(customerId, start, end)
                .stream()
                .filter(a -> "DELIVERED".equals(a.getStatus()))
                .toList();

        List<BillResponse.LineItem> addonItems = new ArrayList<>();
        for (AddonOrder a : addons) {
            double price = priceHistoryService.getEffectivePriceOnDate(
                    a.getProduct().getId(), a.getDeliveryDate());
            addonItems.add(new BillResponse.LineItem(
                    a.getDeliveryDate(),
                    a.getProduct().getName(),
                    a.getProduct().getUnit(),
                    a.getQuantity(), price));
        }

        List<BillAdjustment> adjustments = adjustmentRepo.findByBillId(billId);
        return toResponse(bill, subItems, addonItems, adjustments);
    }

    // --- Adjustment operations ---

    @Transactional
    public BillAdjustment addAdjustment(Long billId, BillAdjustmentRequest request) {
        Billing bill = billingRepo.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        BillAdjustment adj = new BillAdjustment();
        adj.setBill(bill);
        adj.setAdjustmentType(request.getAdjustmentType());
        adj.setAmount(request.getAmount());
        adj.setDescription(request.getDescription());
        BillAdjustment saved = adjustmentRepo.save(adj);

        recalculate(bill);
        return saved;
    }

    @Transactional
    public void removeAdjustment(Long billId, Long adjId) {
        BillAdjustment adj = adjustmentRepo.findById(adjId)
                .orElseThrow(() -> new NotFoundException("Adjustment not found"));
        if (!adj.getBill().getId().equals(billId)) {
            throw new IllegalArgumentException("Adjustment does not belong to this bill");
        }
        adjustmentRepo.delete(adj);
        Billing bill = billingRepo.findById(billId).orElseThrow();
        recalculate(bill);
    }

    public List<BillAdjustment> getAdjustments(Long billId) {
        billingRepo.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));
        return adjustmentRepo.findByBillId(billId);
    }

    // --- Other read methods ---

    public List<Billing> getBillsByCustomer(Long customerId) {
        customerRepo.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        return billingRepo.findByCustomerId(customerId);
    }

    public Page<Billing> getPendingBills(Pageable pageable) {
        return billingRepo.findByStatus("PENDING", pageable);
    }

    public Page<Billing> getBillsByMonth(int month, int year, Pageable pageable) {
        return billingRepo.findByMonthAndYear(month, year, pageable);
    }

    // Non-paginated — used internally by reports
    public List<Billing> getBillsByMonthList(int month, int year) {
        return billingRepo.findByMonthAndYear(month, year);
    }

    public Billing getBillById(Long id) {
        return billingRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Bill not found"));
    }

    /**
     * Recalculates and returns an existing bill for the given customer/month/year.
     * Returns empty if no bill has been generated yet for that period.
     * Used by mark-as-mistake flow to auto-update the bill after a delivery is voided.
     */
    public java.util.Optional<BillResponse> recalculateIfBillExists(Long customerId, int month, int year) {
        return billingRepo.findByCustomerIdAndMonthAndYear(customerId, month, year)
                .map(b -> generateBill(customerId, month, year));
    }

    // --- private helpers ---

    private void recalculate(Billing bill) {
        double adjAmount = adjustmentRepo.findByBillId(bill.getId())
                .stream().mapToDouble(BillAdjustment::getAmount).sum();
        double total = bill.getSubscriptionAmount() + bill.getAddonAmount()
                + bill.getPreviousPendingAmount() + adjAmount;
        bill.setAdjustmentAmount(adjAmount);
        bill.setTotalAmount(total);
        bill.setRemainingAmount(total - bill.getPaidAmount());
        bill.setStatus(bill.getRemainingAmount() <= 0 ? "PAID" : "PENDING");
        billingRepo.save(bill);
    }

    private double getPreviousMonthPending(Long customerId, int month, int year) {
        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;
        return billingRepo.findByCustomerIdAndMonthAndYear(customerId, prevMonth, prevYear)
                .map(Billing::getRemainingAmount)
                .filter(r -> r > 0)
                .orElse(0.0);
    }

    private BillResponse toResponse(Billing bill,
                                     List<BillResponse.LineItem> subItems,
                                     List<BillResponse.LineItem> addonItems,
                                     List<BillAdjustment> adjustments) {
        BillResponse r = new BillResponse();
        r.setBillId(bill.getId());
        r.setCustomerId(bill.getCustomer().getId());
        r.setCustomerName(bill.getCustomer().getName());
        r.setMonth(bill.getMonth());
        r.setYear(bill.getYear());
        r.setSubscriptionItems(subItems);
        r.setSubscriptionAmount(bill.getSubscriptionAmount());
        r.setAddonItems(addonItems);
        r.setAddonAmount(bill.getAddonAmount());
        r.setPreviousPendingAmount(bill.getPreviousPendingAmount());
        r.setAdjustmentAmount(bill.getAdjustmentAmount());
        r.setAdjustments(adjustments);
        r.setTotalAmount(bill.getTotalAmount());
        r.setPaidAmount(bill.getPaidAmount());
        r.setRemainingAmount(bill.getRemainingAmount());
        r.setStatus(bill.getStatus());
        return r;
    }
}
