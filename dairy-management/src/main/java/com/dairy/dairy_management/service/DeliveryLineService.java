package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.AssignCustomerRequest;
import com.dairy.dairy_management.dto.DeliveryLineResponse;
import com.dairy.dairy_management.dto.ResequenceRequest;
import com.dairy.dairy_management.entity.Customer;
import com.dairy.dairy_management.entity.DeliveryLine;
import com.dairy.dairy_management.repository.CustomerRepository;
import com.dairy.dairy_management.repository.DeliveryLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryLineService {

    private final DeliveryLineRepository lineRepo;
    private final CustomerRepository customerRepo;

    public DeliveryLineService(DeliveryLineRepository lineRepo, CustomerRepository customerRepo) {
        this.lineRepo = lineRepo;
        this.customerRepo = customerRepo;
    }

    public DeliveryLine createLine(DeliveryLine line) {
        if (lineRepo.existsByName(line.getName())) {
            throw new RuntimeException("A delivery line with this name already exists");
        }
        return lineRepo.save(line);
    }

    public List<DeliveryLine> getAllLines() {
        return lineRepo.findAll();
    }

    public DeliveryLineResponse getLineById(Long lineId) {
        DeliveryLine line = lineRepo.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Delivery line not found"));

        List<DeliveryLineResponse.CustomerSummary> customers = line.getCustomers().stream()
                .map(c -> new DeliveryLineResponse.CustomerSummary(
                        c.getId(),
                        c.getName(),
                        c.getPhone(),
                        c.getAddress(),
                        c.getSocietyName(),
                        c.getLineSequence()
                ))
                .toList();

        return new DeliveryLineResponse(line.getId(), line.getName(), customers);
    }

    @Transactional
    public void assignCustomer(Long lineId, AssignCustomerRequest request) {
        DeliveryLine line = lineRepo.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Delivery line not found"));

        Customer customer = customerRepo.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setDeliveryLine(line);
        customer.setLineSequence(request.getSequence());
        customerRepo.save(customer);
    }

    @Transactional
    public void removeCustomerFromLine(Long lineId, Long customerId) {
        lineRepo.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Delivery line not found"));

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (customer.getDeliveryLine() == null || !customer.getDeliveryLine().getId().equals(lineId)) {
            throw new RuntimeException("Customer is not assigned to this line");
        }

        customer.setDeliveryLine(null);
        customer.setLineSequence(null);
        customerRepo.save(customer);
    }

    @Transactional
    public void resequenceCustomers(Long lineId, ResequenceRequest request) {
        lineRepo.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Delivery line not found"));

        List<Long> customerIds = request.getCustomerIds();
        List<Customer> toSave = new ArrayList<>();

        for (int i = 0; i < customerIds.size(); i++) {
            Customer customer = customerRepo.findById(customerIds.get(i))
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            if (customer.getDeliveryLine() == null || !customer.getDeliveryLine().getId().equals(lineId)) {
                throw new RuntimeException("Customer " + customer.getId() + " is not assigned to this line");
            }

            customer.setLineSequence(i + 1);
            toSave.add(customer);
        }

        customerRepo.saveAll(toSave);
    }

    public void deleteLine(Long lineId) {
        DeliveryLine line = lineRepo.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Delivery line not found"));

        if (line.getCustomers() != null && !line.getCustomers().isEmpty()) {
            throw new RuntimeException("Cannot delete a line that has customers assigned to it");
        }

        lineRepo.deleteById(lineId);
    }
}
