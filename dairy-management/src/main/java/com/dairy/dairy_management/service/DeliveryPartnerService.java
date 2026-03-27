package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.*;
import com.dairy.dairy_management.entity.*;
import com.dairy.dairy_management.repository.*;
import com.dairy.dairy_management.repository.AddonOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryPartnerService {

    private final DeliveryPartnerRepository partnerRepo;
    private final DeliveryPartnerLineRepository partnerLineRepo;
    private final DeliveryLineRepository lineRepo;
    private final UserRepository userRepo;
    private final DeliveryRepository deliveryRepo;
    private final AddonOrderRepository addonOrderRepo;

    public DeliveryPartnerService(DeliveryPartnerRepository partnerRepo,
                                  DeliveryPartnerLineRepository partnerLineRepo,
                                  DeliveryLineRepository lineRepo,
                                  UserRepository userRepo,
                                  DeliveryRepository deliveryRepo,
                                  AddonOrderRepository addonOrderRepo) {
        this.partnerRepo = partnerRepo;
        this.partnerLineRepo = partnerLineRepo;
        this.lineRepo = lineRepo;
        this.userRepo = userRepo;
        this.deliveryRepo = deliveryRepo;
        this.addonOrderRepo = addonOrderRepo;
    }

    /**
     * Gets the daily list for the currently logged-in delivery partner using their username from JWT.
     */
    public DailyDeliveryListResponse getDailyListByUsername(String username, LocalDate date) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DeliveryPartner partner = partnerRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("No delivery partner profile found for this user"));
        return getDailyList(partner.getId(), date);
    }

    public DeliveryPartnerResponse createPartner(CreateDeliveryPartnerRequest request) {
        User user = userRepo.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.DELIVERY_PARTNER) {
            throw new RuntimeException("User is not a DELIVERY_PARTNER");
        }

        if (partnerRepo.findByUserId(user.getId()).isPresent()) {
            throw new RuntimeException("A delivery partner profile already exists for this user");
        }

        if (partnerRepo.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number already in use");
        }

        DeliveryPartner partner = new DeliveryPartner();
        partner.setUser(user);
        partner.setName(request.getName());
        partner.setPhone(request.getPhone());

        DeliveryPartner saved = partnerRepo.save(partner);
        return toResponse(saved);
    }

    public List<DeliveryPartnerResponse> getAllPartners() {
        return partnerRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public DeliveryPartnerResponse getPartnerById(Long partnerId) {
        DeliveryPartner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Delivery partner not found"));
        return toResponse(partner);
    }

    @Transactional
    public void assignLine(Long partnerId, AssignLineRequest request) {
        DeliveryPartner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Delivery partner not found"));

        DeliveryLine line = lineRepo.findById(request.getLineId())
                .orElseThrow(() -> new RuntimeException("Delivery line not found"));

        if (partnerLineRepo.findByDeliveryPartnerIdAndLineId(partnerId, line.getId()).isPresent()) {
            throw new RuntimeException("This line is already assigned to this partner");
        }

        DeliveryPartnerLine assignment = new DeliveryPartnerLine();
        assignment.setDeliveryPartner(partner);
        assignment.setLine(line);
        assignment.setLineSequence(request.getSequence());
        partnerLineRepo.save(assignment);
    }

    @Transactional
    public void removeLine(Long partnerId, Long lineId) {
        DeliveryPartnerLine assignment = partnerLineRepo
                .findByDeliveryPartnerIdAndLineId(partnerId, lineId)
                .orElseThrow(() -> new RuntimeException("This line is not assigned to this partner"));

        partnerLineRepo.delete(assignment);
    }

    @Transactional
    public void resequenceLines(Long partnerId, ResequenceLinesRequest request) {
        partnerRepo.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Delivery partner not found"));

        List<Long> lineIds = request.getLineIds();
        List<DeliveryPartnerLine> toSave = new ArrayList<>();

        for (int i = 0; i < lineIds.size(); i++) {
            final Long lineId = lineIds.get(i);
            final int sequence = i + 1;
            DeliveryPartnerLine assignment = partnerLineRepo
                    .findByDeliveryPartnerIdAndLineId(partnerId, lineId)
                    .orElseThrow(() -> new RuntimeException("Line " + lineId + " is not assigned to this partner"));

            assignment.setLineSequence(sequence);
            toSave.add(assignment);
        }

        partnerLineRepo.saveAll(toSave);
    }

    public DailyDeliveryListResponse getDailyList(Long partnerId, LocalDate date) {
        DeliveryPartner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Delivery partner not found"));

        List<DailyDeliveryListResponse.LineDeliveries> lineDeliveries = new ArrayList<>();

        for (DeliveryPartnerLine assignment : partner.getAssignedLines()) {
            DeliveryLine line = assignment.getLine();
            List<DailyDeliveryListResponse.CustomerDelivery> customerDeliveries = new ArrayList<>();

            for (Customer customer : line.getCustomers()) {
                List<Delivery> deliveries = deliveryRepo
                        .findBySubscription_CustomerIdAndDeliveryDate(customer.getId(), date);

                if (deliveries.isEmpty()) {
                    // No delivery generated yet for this date — show customer slot with nulls
                    customerDeliveries.add(new DailyDeliveryListResponse.CustomerDelivery(
                            customer.getId(),
                            customer.getName(),
                            customer.getAddress(),
                            customer.getSocietyName(),
                            customer.getLineSequence(),
                            null, null, null
                    ));
                } else {
                    // One or more deliveries exist (e.g. regular + add-on)
                    for (Delivery d : deliveries) {
                        customerDeliveries.add(new DailyDeliveryListResponse.CustomerDelivery(
                                customer.getId(),
                                customer.getName(),
                                customer.getAddress(),
                                customer.getSocietyName(),
                                customer.getLineSequence(),
                                d.getId(),
                                d.getQuantity(),
                                d.getStatus()
                        ));
                    }
                }
            }

            lineDeliveries.add(new DailyDeliveryListResponse.LineDeliveries(
                    line.getId(),
                    line.getName(),
                    assignment.getLineSequence(),
                    customerDeliveries
            ));
        }

        return new DailyDeliveryListResponse(partner.getId(), partner.getName(), date, lineDeliveries);
    }

    private DeliveryPartnerResponse toResponse(DeliveryPartner partner) {
        List<DeliveryPartnerResponse.AssignedLine> lines = new ArrayList<>();

        if (partner.getAssignedLines() != null) {
            for (DeliveryPartnerLine a : partner.getAssignedLines()) {
                int customerCount = a.getLine().getCustomers() != null
                        ? a.getLine().getCustomers().size() : 0;
                lines.add(new DeliveryPartnerResponse.AssignedLine(
                        a.getLine().getId(),
                        a.getLine().getName(),
                        a.getLineSequence(),
                        customerCount
                ));
            }
        }

        String username = partner.getUser() != null ? partner.getUser().getUsername() : null;
        return new DeliveryPartnerResponse(
                partner.getId(),
                partner.getName(),
                partner.getPhone(),
                username,
                lines
        );
    }
}
