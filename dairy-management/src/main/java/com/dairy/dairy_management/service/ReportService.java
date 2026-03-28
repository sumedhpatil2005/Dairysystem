package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.CustomerMonthlyReport;
import com.dairy.dairy_management.dto.CustomerStatementResponse;
import com.dairy.dairy_management.dto.DailyReportResponse;
import com.dairy.dairy_management.dto.PartnerMonthlyReport;
import com.dairy.dairy_management.dto.RevenueReportResponse;
import com.dairy.dairy_management.entity.*;
import com.dairy.dairy_management.exception.NotFoundException;
import com.dairy.dairy_management.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final DeliveryRepository deliveryRepo;
    private final AddonOrderRepository addonRepo;
    private final BillingRepository billingRepo;
    private final CustomerRepository customerRepo;
    private final DeliveryLineRepository lineRepo;
    private final DeliveryPartnerLineRepository partnerLineRepo;
    private final DeliveryPartnerRepository partnerRepo;

    public ReportService(DeliveryRepository deliveryRepo,
                         AddonOrderRepository addonRepo,
                         BillingRepository billingRepo,
                         CustomerRepository customerRepo,
                         DeliveryLineRepository lineRepo,
                         DeliveryPartnerLineRepository partnerLineRepo,
                         DeliveryPartnerRepository partnerRepo) {
        this.deliveryRepo = deliveryRepo;
        this.addonRepo = addonRepo;
        this.billingRepo = billingRepo;
        this.customerRepo = customerRepo;
        this.lineRepo = lineRepo;
        this.partnerLineRepo = partnerLineRepo;
        this.partnerRepo = partnerRepo;
    }

    /**
     * Daily delivery report following the system's hierarchy:
     *   Line → Customers (in lineSequence order) → Deliveries
     *
     * Each line also shows which delivery partner is assigned to it.
     * A partner may have multiple lines; each line has multiple customers in sequence.
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
            // Deliveries for customers on this line, ordered by customer lineSequence
            List<Delivery> lineDeliveries = allDeliveries.stream()
                    .filter(d -> d.getSubscription().getCustomer().getDeliveryLine() != null
                            && d.getSubscription().getCustomer().getDeliveryLine().getId().equals(line.getId()))
                    .sorted((a, b) -> {
                        Integer seqA = a.getSubscription().getCustomer().getLineSequence();
                        Integer seqB = b.getSubscription().getCustomer().getLineSequence();
                        if (seqA == null) return 1;
                        if (seqB == null) return -1;
                        return seqA.compareTo(seqB);
                    })
                    .toList();

            if (lineDeliveries.isEmpty()) continue;

            DailyReportResponse.LineReport lineReport = new DailyReportResponse.LineReport();
            lineReport.setLineId(line.getId());
            lineReport.setLineName(line.getName());

            // Efficient lookup: which partner has this line assigned?
            partnerLineRepo.findByLineId(line.getId())
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

    /**
     * Full account statement for a customer across ALL billing months.
     *
     * Uses 2 bulk queries (all bills + all deliveries for the customer) and
     * groups everything in memory — avoids N+1 per month.
     */
    public CustomerStatementResponse getCustomerStatement(Long customerId) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        // --- Fetch all bills for this customer, sorted oldest → newest ---
        List<Billing> bills = billingRepo.findByCustomerId(customerId)
                .stream()
                .sorted(Comparator.comparingInt(Billing::getYear)
                        .thenComparingInt(Billing::getMonth))
                .toList();

        // --- Fetch ALL deliveries for this customer in one query, group by month key ---
        Map<String, List<Delivery>> deliveriesByMonth =
                deliveryRepo.findBySubscription_CustomerId(customerId)
                        .stream()
                        .collect(Collectors.groupingBy(d ->
                                d.getDeliveryDate().getYear() + "-" +
                                d.getDeliveryDate().getMonthValue()));

        // --- Build per-month entries ---
        List<CustomerStatementResponse.MonthStatement> months = new ArrayList<>();
        double totalBilled = 0, totalPaid = 0, totalOutstanding = 0;

        for (Billing bill : bills) {
            String key = bill.getYear() + "-" + bill.getMonth();
            List<Delivery> monthDeliveries = deliveriesByMonth.getOrDefault(key, List.of());

            int deliveriesCount = monthDeliveries.size();
            int deliveredCount  = (int) monthDeliveries.stream()
                    .filter(d -> "DELIVERED".equals(d.getStatus())).count();
            int skippedCount    = (int) monthDeliveries.stream()
                    .filter(d -> "SKIPPED".equals(d.getStatus())).count();

            months.add(new CustomerStatementResponse.MonthStatement(
                    bill.getMonth(), bill.getYear(),
                    bill.getId(),
                    bill.getStatus(),
                    bill.getSubscriptionAmount(),
                    bill.getAddonAmount(),
                    bill.getAdjustmentAmount(),
                    bill.getPreviousPendingAmount(),
                    bill.getTotalAmount(),
                    bill.getPaidAmount(),
                    bill.getRemainingAmount(),
                    deliveriesCount, deliveredCount, skippedCount
            ));

            totalBilled      += bill.getTotalAmount();
            totalPaid        += bill.getPaidAmount();
            totalOutstanding += bill.getRemainingAmount();
        }

        return new CustomerStatementResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getAddress(),
                totalBilled,
                totalPaid,
                totalOutstanding,
                bills.size(),
                months
        );
    }

    /**
     * Monthly performance breakdown for a delivery partner.
     * Shows total assigned / delivered / skipped / not-reachable / pending
     * at both the partner level and per-line level.
     */
    public PartnerMonthlyReport getPartnerMonthlyReport(Long partnerId, int month, int year) {
        DeliveryPartner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new NotFoundException("Delivery partner not found"));

        List<DeliveryPartnerLine> assignments = partnerLineRepo.findByDeliveryPartnerId(partnerId);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        int totalAssigned = 0, delivered = 0, skipped = 0, notReachable = 0, pending = 0;
        List<PartnerMonthlyReport.LinePerformance> linePerfs = new ArrayList<>();

        for (DeliveryPartnerLine assignment : assignments) {
            DeliveryLine line = assignment.getLine();

            List<Delivery> lineDeliveries = deliveryRepo
                    .findBySubscription_Customer_DeliveryLine_IdAndDeliveryDateBetween(
                            line.getId(), start, end);

            int lTotal       = lineDeliveries.size();
            int lDelivered   = (int) lineDeliveries.stream().filter(d -> "DELIVERED".equals(d.getStatus())).count();
            int lSkipped     = (int) lineDeliveries.stream().filter(d -> "SKIPPED".equals(d.getStatus())).count();
            int lNR          = (int) lineDeliveries.stream().filter(d -> "NOT_REACHABLE".equals(d.getStatus())).count();
            int lPending     = (int) lineDeliveries.stream().filter(d -> "PENDING".equals(d.getStatus())).count();

            linePerfs.add(new PartnerMonthlyReport.LinePerformance(
                    line.getId(), line.getName(), lTotal, lDelivered, lSkipped, lNR, lPending));

            totalAssigned += lTotal;
            delivered     += lDelivered;
            skipped       += lSkipped;
            notReachable  += lNR;
            pending       += lPending;
        }

        // Round to 1 decimal place: e.g. 95.7%
        double rate = totalAssigned == 0 ? 0.0
                : Math.round(delivered * 1000.0 / totalAssigned) / 10.0;

        return new PartnerMonthlyReport(
                partnerId, partner.getName(), month, year,
                totalAssigned, delivered, skipped, notReachable, pending,
                rate, linePerfs);
    }
}
