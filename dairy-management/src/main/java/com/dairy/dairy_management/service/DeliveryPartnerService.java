package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.*;
import com.dairy.dairy_management.entity.*;
import com.dairy.dairy_management.exception.ConflictException;
import com.dairy.dairy_management.exception.NotFoundException;
import com.dairy.dairy_management.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class DeliveryPartnerService {

    private final DeliveryPartnerRepository partnerRepo;
    private final DeliveryPartnerLineRepository partnerLineRepo;
    private final DeliveryLineRepository lineRepo;
    private final UserRepository userRepo;
    private final DeliveryRepository deliveryRepo;
    private final AddonOrderRepository addonOrderRepo;
    private final AuditLogService auditLogService;

    public DeliveryPartnerService(DeliveryPartnerRepository partnerRepo,
                                  DeliveryPartnerLineRepository partnerLineRepo,
                                  DeliveryLineRepository lineRepo,
                                  UserRepository userRepo,
                                  DeliveryRepository deliveryRepo,
                                  AddonOrderRepository addonOrderRepo,
                                  AuditLogService auditLogService) {
        this.partnerRepo = partnerRepo;
        this.partnerLineRepo = partnerLineRepo;
        this.lineRepo = lineRepo;
        this.userRepo = userRepo;
        this.deliveryRepo = deliveryRepo;
        this.addonOrderRepo = addonOrderRepo;
        this.auditLogService = auditLogService;
    }

    /**
     * Returns the profile of the currently logged-in delivery partner.
     * Used by GET /delivery-partners/me
     */
    public DeliveryPartnerResponse getPartnerProfileByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DeliveryPartner partner = partnerRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("No delivery partner profile found for this user"));
        return toResponse(partner);
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

    /**
     * Deactivates a delivery partner — blocks their login and marks them inactive.
     * Their assigned lines are preserved; admin can reassign as needed.
     */
    public DeliveryPartnerResponse deactivate(Long partnerId) {
        DeliveryPartner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new NotFoundException("Delivery partner not found"));
        if (!partner.isActive()) {
            throw new ConflictException("Delivery partner is already deactivated");
        }
        partner.setActive(false);
        DeliveryPartner saved = partnerRepo.save(partner);
        auditLogService.log("PARTNER_DEACTIVATED", "DELIVERY_PARTNER", partnerId,
                "Delivery partner deactivated: " + partner.getName());
        return toResponse(saved);
    }

    /**
     * Reactivates a previously deactivated delivery partner.
     */
    public DeliveryPartnerResponse activate(Long partnerId) {
        DeliveryPartner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new NotFoundException("Delivery partner not found"));
        if (partner.isActive()) {
            throw new ConflictException("Delivery partner is already active");
        }
        partner.setActive(true);
        DeliveryPartner saved = partnerRepo.save(partner);
        auditLogService.log("PARTNER_ACTIVATED", "DELIVERY_PARTNER", partnerId,
                "Delivery partner activated: " + partner.getName());
        return toResponse(saved);
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

    /**
     * Builds the full daily delivery list for the Flutter delivery partner app.
     *
     * Response structure:
     *   loadSummary  <- what to load on the vehicle before leaving
     *   lines[]      <- routes sorted by partner's line sequence
     *     societies[]  <- customers grouped by society within each route
     *       customers[]  <- sorted by lineSequence
     *         deliveries[]  <- one item per product subscription
     */
    public DailyDeliveryListResponse getDailyList(Long partnerId, LocalDate date) {
        DeliveryPartner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Delivery partner not found"));

        // Fetch assignments sorted by the partner's own line sequence
        List<DeliveryPartnerLine> assignments = partnerLineRepo.findByDeliveryPartnerId(partnerId);
        assignments.sort(Comparator.comparingInt(a -> a.getLineSequence() != null ? a.getLineSequence() : 999));

        // Load summary accumulators
        // key = "productName|unit"  -> running total quantity
        Map<String, double[]> productTotals = new LinkedHashMap<>();
        int totalCustomers = 0;
        int totalDeliveries = 0;

        List<DailyDeliveryListResponse.LineDeliveries> lineDeliveries = new ArrayList<>();

        for (DeliveryPartnerLine assignment : assignments) {
            DeliveryLine line = assignment.getLine();

            // Sort customers in this line by their route position (lineSequence)
            List<Customer> customers = new ArrayList<>(line.getCustomers());
            customers.sort(Comparator.comparingInt(c -> c.getLineSequence() != null ? c.getLineSequence() : 999));

            // Group customers by society (LinkedHashMap preserves insertion order)
            // Societies appear in the order of the first customer from that society
            Map<String, List<DailyDeliveryListResponse.CustomerDelivery>> societyMap = new LinkedHashMap<>();

            for (Customer customer : customers) {
                totalCustomers++;

                // All deliveries for this customer on the requested date
                List<Delivery> deliveries = deliveryRepo
                        .findBySubscription_CustomerIdAndDeliveryDate(customer.getId(), date);

                // Build one DeliveryItem per product subscription
                List<DailyDeliveryListResponse.DeliveryItem> items = new ArrayList<>();
                for (Delivery d : deliveries) {
                    String productName = d.getSubscription().getProduct().getName();
                    String unit = d.getSubscription().getProduct().getUnit();

                    items.add(new DailyDeliveryListResponse.DeliveryItem(
                            d.getId(),
                            productName,
                            unit,
                            d.getQuantity(),
                            d.getStatus()
                    ));

                    // Count toward load summary only for non-skipped deliveries
                    if (!"SKIPPED".equalsIgnoreCase(d.getStatus())) {
                        totalDeliveries++;
                        String key = productName + "|" + unit;
                        productTotals.computeIfAbsent(key, k -> new double[]{0})[0] += d.getQuantity();
                    }
                }

                DailyDeliveryListResponse.CustomerDelivery cd =
                        new DailyDeliveryListResponse.CustomerDelivery(
                                customer.getId(),
                                customer.getName(),
                                customer.getAddress(),
                                customer.getSocietyName(),
                                customer.getLineSequence(),
                                items
                        );

                // Group into society bucket — "Other" if society is blank
                String society = (customer.getSocietyName() != null && !customer.getSocietyName().isBlank())
                        ? customer.getSocietyName()
                        : "Other";
                societyMap.computeIfAbsent(society, k -> new ArrayList<>()).add(cd);
            }

            // Convert society map -> ordered list of SocietyGroups
            List<DailyDeliveryListResponse.SocietyGroup> societies = new ArrayList<>();
            for (Map.Entry<String, List<DailyDeliveryListResponse.CustomerDelivery>> entry : societyMap.entrySet()) {
                societies.add(new DailyDeliveryListResponse.SocietyGroup(entry.getKey(), entry.getValue()));
            }

            lineDeliveries.add(new DailyDeliveryListResponse.LineDeliveries(
                    line.getId(),
                    line.getName(),
                    assignment.getLineSequence(),
                    societies
            ));
        }

        // Build load summary
        List<DailyDeliveryListResponse.ProductLoad> productLoads = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : productTotals.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            productLoads.add(new DailyDeliveryListResponse.ProductLoad(
                    parts[0],
                    parts.length > 1 ? parts[1] : "",
                    entry.getValue()[0]
            ));
        }

        DailyDeliveryListResponse.LoadSummary loadSummary =
                new DailyDeliveryListResponse.LoadSummary(
                        totalCustomers,
                        totalDeliveries,
                        productLoads
                );

        return new DailyDeliveryListResponse(
                partner.getId(),
                partner.getName(),
                date,
                loadSummary,
                lineDeliveries
        );
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
                partner.isActive(),
                lines
        );
    }
}
