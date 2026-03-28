package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.DashboardResponse;
import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.repository.BillingRepository;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.DeliveryPartnerRepository;
import com.dairy.dairy_management.repository.DeliveryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final DeliveryRepository deliveryRepo;
    private final BillingRepository billingRepo;
    private final CustomerRepository customerRepo;
    private final DeliveryPartnerRepository partnerRepo;

    public DashboardService(DeliveryRepository deliveryRepo,
                            BillingRepository billingRepo,
                            CustomerRepository customerRepo,
                            DeliveryPartnerRepository partnerRepo) {
        this.deliveryRepo = deliveryRepo;
        this.billingRepo = billingRepo;
        this.customerRepo = customerRepo;
        this.partnerRepo = partnerRepo;
    }

    public DashboardResponse getSummary() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        // Today's deliveries
        List<Delivery> todayDeliveries = deliveryRepo.findByDeliveryDate(today);

        // Current month bills
        List<Billing> monthBills = billingRepo.findByMonthAndYear(month, year);

        // All pending bills (across all months)
        List<Billing> pendingBills = billingRepo.findByStatus("PENDING");

        DashboardResponse response = new DashboardResponse();
        response.setDate(today);

        // Today's stats
        response.setTodayTotal(todayDeliveries.size());
        response.setTodayDelivered((int) todayDeliveries.stream()
                .filter(d -> "DELIVERED".equals(d.getStatus())).count());
        response.setTodayPending((int) todayDeliveries.stream()
                .filter(d -> "PENDING".equals(d.getStatus())).count());
        response.setTodaySkipped((int) todayDeliveries.stream()
                .filter(d -> "SKIPPED".equals(d.getStatus())).count());

        // Month billing stats
        response.setCurrentMonth(month);
        response.setCurrentYear(year);
        response.setMonthlyBilled(monthBills.stream().mapToDouble(Billing::getTotalAmount).sum());
        response.setMonthlyCollected(monthBills.stream().mapToDouble(Billing::getPaidAmount).sum());
        response.setMonthlyOutstanding(monthBills.stream().mapToDouble(Billing::getRemainingAmount).sum());

        // Pending bills across all months
        response.setPendingBillsCount(pendingBills.size());
        response.setPendingBillsTotal(pendingBills.stream().mapToDouble(Billing::getRemainingAmount).sum());

        // System totals
        response.setActiveCustomers(customerRepo.findByIsActiveTrue().size());
        response.setTotalDeliveryPartners(partnerRepo.count());

        return response;
    }
}
