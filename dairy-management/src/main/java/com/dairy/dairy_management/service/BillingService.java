package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.BillResponse;
import com.dairy.dairy_management.entity.AddonOrder;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.repository.AddonOrderRepository;
import com.dairy.dairy_management.repository.BillingRepository;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BillingService {

    private final DeliveryRepository deliveryRepo;
    private final AddonOrderRepository addonRepo;
    private final BillingRepository billingRepo;
    private final CustomerRepository customerRepo;

    public BillingService(DeliveryRepository deliveryRepo,
                          AddonOrderRepository addonRepo,
                          BillingRepository billingRepo,
                          CustomerRepository customerRepo) {
        this.deliveryRepo = deliveryRepo;
        this.addonRepo = addonRepo;
        this.billingRepo = billingRepo;
        this.customerRepo = customerRepo;
    }

    /**
     * Generates (or regenerates) the monthly bill for a customer.
     *
     * Calculation:
     *   subscriptionAmount = sum of (quantity × pricePerUnit) for DELIVERED subscription deliveries
     *   addonAmount        = sum of (quantity × pricePerUnit) for DELIVERED addon orders
     *   previousPending    = remainingAmount of last month's bill (if any)
     *   totalAmount        = subscriptionAmount + addonAmount + previousPending
     *   remainingAmount    = totalAmount - paidAmount (paidAmount reset to 0 on fresh generate)
     */
    @Transactional
    public BillResponse generateBill(Long customerId, int month, int year) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Prevent duplicate — if bill exists, regenerate it
        Billing bill = billingRepo
                .findByCustomerIdAndMonthAndYear(customerId, month, year)
                .orElse(new Billing());

        bill.setCustomer(customer);
        bill.setMonth(month);
        bill.setYear(year);

        // --- Subscription deliveries ---
        List<Delivery> deliveries = deliveryRepo.findBySubscription_CustomerId(customerId)
                .stream()
                .filter(d -> d.getDeliveryDate().getMonthValue() == month
                        && d.getDeliveryDate().getYear() == year
                        && "DELIVERED".equals(d.getStatus()))
                .toList();

        List<BillResponse.LineItem> subItems = new ArrayList<>();
        double subAmount = 0;
        for (Delivery d : deliveries) {
            double price = d.getSubscription().getProduct().getPricePerUnit();
            subItems.add(new BillResponse.LineItem(
                    d.getDeliveryDate(),
                    d.getSubscription().getProduct().getName(),
                    d.getSubscription().getProduct().getUnit(),
                    d.getQuantity(),
                    price));
            subAmount += d.getQuantity() * price;
        }

        // --- Addon orders ---
        List<AddonOrder> addons = addonRepo
                .findByCustomerIdAndMonthAndYear(customerId, month, year)
                .stream()
                .filter(a -> "DELIVERED".equals(a.getStatus()))
                .toList();

        List<BillResponse.LineItem> addonItems = new ArrayList<>();
        double addonAmount = 0;
        for (AddonOrder a : addons) {
            double price = a.getProduct().getPricePerUnit();
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

        double total = subAmount + addonAmount + prevPending;

        bill.setSubscriptionAmount(subAmount);
        bill.setAddonAmount(addonAmount);
        bill.setPreviousPendingAmount(prevPending);
        bill.setTotalAmount(total);
        // Keep paidAmount if bill already existed (partial payment scenario)
        double paid = bill.getPaidAmount();
        bill.setRemainingAmount(total - paid);
        bill.setStatus(bill.getRemainingAmount() <= 0 ? "PAID" : "PENDING");

        Billing saved = billingRepo.save(bill);
        return toResponse(saved, subItems, addonItems);
    }

    public BillResponse getBillDetail(Long billId) {
        Billing bill = billingRepo.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        Long customerId = bill.getCustomer().getId();
        int month = bill.getMonth();
        int year = bill.getYear();

        List<Delivery> deliveries = deliveryRepo.findBySubscription_CustomerId(customerId)
                .stream()
                .filter(d -> d.getDeliveryDate().getMonthValue() == month
                        && d.getDeliveryDate().getYear() == year
                        && "DELIVERED".equals(d.getStatus()))
                .toList();

        List<BillResponse.LineItem> subItems = new ArrayList<>();
        for (Delivery d : deliveries) {
            subItems.add(new BillResponse.LineItem(
                    d.getDeliveryDate(),
                    d.getSubscription().getProduct().getName(),
                    d.getSubscription().getProduct().getUnit(),
                    d.getQuantity(),
                    d.getSubscription().getProduct().getPricePerUnit()));
        }

        List<AddonOrder> addons = addonRepo
                .findByCustomerIdAndMonthAndYear(customerId, month, year)
                .stream()
                .filter(a -> "DELIVERED".equals(a.getStatus()))
                .toList();

        List<BillResponse.LineItem> addonItems = new ArrayList<>();
        for (AddonOrder a : addons) {
            addonItems.add(new BillResponse.LineItem(
                    a.getDeliveryDate(),
                    a.getProduct().getName(),
                    a.getProduct().getUnit(),
                    a.getQuantity(),
                    a.getProduct().getPricePerUnit()));
        }

        return toResponse(bill, subItems, addonItems);
    }

    public List<Billing> getBillsByCustomer(Long customerId) {
        customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return billingRepo.findByCustomerId(customerId);
    }

    public List<Billing> getPendingBills() {
        return billingRepo.findByStatus("PENDING");
    }

    public List<Billing> getBillsByMonth(int month, int year) {
        return billingRepo.findByMonthAndYear(month, year);
    }

    public Billing getBillById(Long id) {
        return billingRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
    }

    // --- private helpers ---

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
                                     List<BillResponse.LineItem> addonItems) {
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
        r.setTotalAmount(bill.getTotalAmount());
        r.setPaidAmount(bill.getPaidAmount());
        r.setRemainingAmount(bill.getRemainingAmount());
        r.setStatus(bill.getStatus());
        return r;
    }
}
