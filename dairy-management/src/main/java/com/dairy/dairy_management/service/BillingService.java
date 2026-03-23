package com.dairy.dairy_management.service;

import com.dairy.dairy_management.entity.Billing;
import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.entity.Delivery;
import com.dairy.dairy_management.repository.BillingRepository;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.DeliveryRepository;
import jakarta.persistence.Entity;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
@Data

@Service
public class BillingService {

    private final DeliveryRepository deliveryRepo;
    private final BillingRepository billingRepo;

    public BillingService(DeliveryRepository deliveryRepo, BillingRepository billingRepo) {
        this.deliveryRepo = deliveryRepo;
        this.billingRepo = billingRepo;
    }

    public Billing generateBill(Customer customer, int month, int year) {

        // Step 1: get deliveries (we'll improve this later)
        List<Delivery> deliveries = deliveryRepo.findAll();

        double totalQuantity = 0;

        for (Delivery d : deliveries) {
            if (d.getSubscription().getCustomer().getId().equals(customer.getId())
                    && d.getDeliveryDate().getMonthValue() == month
                    && d.getDeliveryDate().getYear() == year
                    && d.getStatus().equals("DELIVERED")) {

                totalQuantity += d.getQuantity();
            }
        }

        // Step 2: get price (from subscription → product)
        if (deliveries.isEmpty()) {
            throw new RuntimeException("No deliveries found for billing");
        }




        Delivery firstValid = null;

        for (Delivery d : deliveries) {
            if (d.getSubscription().getCustomer().getId().equals(customer.getId())
                    && d.getDeliveryDate().getMonthValue() == month
                    && d.getDeliveryDate().getYear() == year
                    && "DELIVERED".equals(d.getStatus())) {

                totalQuantity += d.getQuantity();

                if (firstValid == null) {
                    firstValid = d;
                }
            }
        }

        if (firstValid == null) {
            throw new RuntimeException("No valid deliveries found");
        }

        double price = firstValid
                .getSubscription()
                .getProduct()
                .getPricePerUnit();

        Double totalAmount = totalQuantity * price;

        // Step 3: create billing
        Billing bill = new Billing();
        bill.setCustomer(customer);
        bill.setMonth(month);
        bill.setYear(year);
        bill.setTotalAmount(totalAmount);
        bill.setStatus("PENDING");

        return billingRepo.save(bill);
    }
}