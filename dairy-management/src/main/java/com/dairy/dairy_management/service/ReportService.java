package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.CustomerMonthlyReport;
import com.dairy.dairy_management.dto.DailyReportResponse;
import com.dairy.dairy_management.dto.RevenueReportResponse;
import com.dairy.dairy_management.entity.*;
import com.dairy.dairy_management.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final DeliveryRepository deliveryRepo;
    private final AddonOrderRepository addonRepo;
    private final BillingRepository billingRepo;
    private final CustomerRepository customerRepo;
    private final DeliveryLineRepository lineRepo;
    private final DeliveryPartnerLineRepository partnerLineRepo;

    public ReportService(DeliveryRepository deliveryRepo,
                         AddonOrderRepository addonRepo,
                         BillingRepository billingRepo,
                         CustomerRepository customerRepo,
                         DeliveryLineRepository lineRepo,
                         DeliveryPartnerLineRepository partnerLineRepo) {
        this.deliveryRepo = deliveryRepo;
        this.addonRepo = addonRepo;
        this.billingRepo = billingRepo;
        this.customerRepo = customerRepo;
        this.lineRepo = lineRepo;
        this.partnerLineRepo = partnerLineRepo;
    }

    /**
     * Daily delivery report — all deliveries for a date grouped by delivery line.
     * Shows which partner is assigned and status of each customer's delivery.
     */
    public DailyReportResponse getDailyReport(LocalDate date) {
        List<Delivery> allDeliveries = deliveryRepo.findByDeliveryDate(date);

        DailyReportResponse report = new DailyReportResponse();
        report.setDate(date);
        report.setTotalDeliveries(allDeliveries.size());
        report.setDelivered((int) allDeliveries.stream().filter(d -> "DELIVERED".equals(d.getStatus())).count());
        report.setPending((int) allDeliveries.stream().filter(d -> "PENDING".equals(d.getStatus())).count());
        report.setSkipped((int) allDeliveries.stream().filter(d -> "SKIPPED".equals(d.getStatus())).count());
        report.setNotReachable((int) allDeliveries.stream().filter(d -> "NOT_REACHABLE".equals(d.getStatus())).count());

        List<DeliveryLine> lines = lineRepo.findAll();
        List<DailyReportResponse.LineReport> lineReports = new ArrayList<>();

        for (DeliveryLine line : lines) {
            // Find deliveries for customers on this line
            List<Delivery> lineDeliveries = allDeliveries.stream()
                    .filter(d -> d.getSubscription().getCustomer().getDeliveryLine() != null
                            && d.getSubscription().getCustomer().getDeliveryLine().getId().equals(line.getId()))
                    .toList();

            if (lineDeliveries.isEmpty()) continue;

            DailyReportResponse.LineReport lineReport = new DailyReportResponse.LineReport();
            lineReport.setLineId(line.getId());
            lineReport.setLineName(line.getName());

            // Find assigned partner for this line
            partnerLineRepo.findAll().stream()
                    .filter(pl -> pl.getLine().getId().equals(line.getId()))
                    .findFirst()
                    .ifPresent(pl -> lineReport.setPartnerName(pl.getDeliveryPartner().getName()));

            List<DailyReportResponse.DeliveryItem> items = lineDeliveries.stream()
                    .map(d -> {
                        DailyReportResponse.DeliveryItem item = new DailyReportResponse.DeliveryItem();
                        item.setDeliveryId(d.getId());
                        item.setCustomerId(d.getSubscription().getCustomer().getId());
                        item.setCustomerName(d.getSubscription().getCustomer().getName());
                        item.setProductName(d.getSubscription().getProduct().getName());
                        item.setQuantity(d.getQuantity());
                        item.setSlot(d.getSubscription().getDeliverySlot().name());
                        item.setStatus(d.getStatus());
                        return item;
                    })
                    .toList();

            lineReport.setDeliveries(items);
            lineReports.add(lineReport);
        }

        report.setLines(lineReports);
        return report;
    }

    /**
     * Monthly revenue report — totals across all bills for a given month/year.
     */
    public RevenueReportResponse getRevenueReport(int month, int year) {
        List<Billing> bills = billingRepo.findByMonthAndYear(month, year);

        RevenueReportResponse report = new RevenueReportResponse();
        report.setMonth(month);
        report.setYear(year);
        report.setTotalBills(bills.size());
        report.setPaidBills((int) bills.stream().filter(b -> "PAID".equals(b.getStatus())).count());
        report.setPendingBills((int) bills.stream().filter(b -> "PENDING".equals(b.getStatus())).count());
        report.setTotalBilled(bills.stream().mapToDouble(Billing::getTotalAmount).sum());
        report.setTotalCollected(bills.stream().mapToDouble(Billing::getPaidAmount).sum());
        report.setTotalPending(bills.stream().mapToDouble(Billing::getRemainingAmount).sum());

        return report;
    }

    /**
     * List of all customers with outstanding (unpaid) balances.
     */
    public List<Billing> getPendingPayments() {
        return billingRepo.findByStatus("PENDING");
    }

    /**
     * Full month view for a single customer — deliveries, addons, and bill.
     */
    public CustomerMonthlyReport getCustomerMonthlyReport(Long customerId, int month, int year) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        List<Delivery> deliveries = deliveryRepo.findBySubscription_CustomerId(customerId)
                .stream()
                .filter(d -> d.getDeliveryDate().getMonthValue() == month
                        && d.getDeliveryDate().getYear() == year)
                .toList();

        List<AddonOrder> addons = addonRepo.findByCustomerId(customerId)
                .stream()
                .filter(a -> a.getDeliveryDate().getMonthValue() == month
                        && a.getDeliveryDate().getYear() == year)
                .toList();

        Billing bill = billingRepo.findByCustomerIdAndMonthAndYear(customerId, month, year)
                .orElse(null);

        CustomerMonthlyReport report = new CustomerMonthlyReport();
        report.setCustomerId(customer.getId());
        report.setCustomerName(customer.getName());
        report.setPhone(customer.getPhone());
        report.setMonth(month);
        report.setYear(year);
        report.setDeliveries(deliveries);
        report.setTotalDeliveries(deliveries.size());
        report.setDeliveredCount((int) deliveries.stream().filter(d -> "DELIVERED".equals(d.getStatus())).count());
        report.setSkippedCount((int) deliveries.stream().filter(d -> "SKIPPED".equals(d.getStatus())).count());
        report.setAddonOrders(addons);
        report.setTotalAddons(addons.size());
        report.setBill(bill);

        return report;
    }
}
