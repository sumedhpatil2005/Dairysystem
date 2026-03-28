package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.BillResponse;
import com.dairy.dairy_management.entity.BillAdjustment;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Generates a PDF invoice (byte[]) for any bill.
 * Used by GET /billing/{id}/invoice/pdf
 *
 * Layout:
 *   ┌─────────────────────────────────┐
 *   │  DAIRY INVOICE          BILL-n  │
 *   │  Invoice Date: dd MMM yyyy      │
 *   │  Period: March 2026             │
 *   │                                 │
 *   │  Customer: Name | Phone | Addr  │
 *   ├─────────────────────────────────┤
 *   │  SUBSCRIPTION DELIVERIES table  │
 *   │  ADDON ORDERS table (if any)    │
 *   │  ADJUSTMENTS table (if any)     │
 *   ├─────────────────────────────────┤
 *   │  SUMMARY: Total | Paid | Due    │
 *   └─────────────────────────────────┘
 */
@Service
public class PdfInvoiceService {

    private final BillingService billingService;

    // ── Colour palette ────────────────────────────────────────────────────────
    private static final Color BRAND_BLUE   = new Color(25,  118, 210);
    private static final Color HEADER_BG    = new Color(25,  118, 210);
    private static final Color TABLE_HEADER = new Color(240, 248, 255);
    private static final Color SUBTOTAL_BG  = new Color(232, 245, 253);
    private static final Color DUE_BG       = new Color(255, 243, 205);
    private static final Color TEXT_DARK    = new Color(33,  33,  33);
    private static final Color TEXT_MUTED   = new Color(97,  97,  97);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  20, Color.WHITE);
    private static final Font FONT_INVOICE  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  13, Color.WHITE);
    private static final Font FONT_SECTION  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, BRAND_BLUE);
    private static final Font FONT_TH       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, TEXT_DARK);
    private static final Font FONT_TD       = FontFactory.getFont(FontFactory.HELVETICA,         9, TEXT_DARK);
    private static final Font FONT_LABEL    = FontFactory.getFont(FontFactory.HELVETICA,         9, TEXT_MUTED);
    private static final Font FONT_VALUE    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    9, TEXT_DARK);
    private static final Font FONT_TOTAL    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11, TEXT_DARK);
    private static final Font FONT_DUE      = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   13, new Color(183, 28, 28));
    private static final Font FONT_META     = FontFactory.getFont(FontFactory.HELVETICA,         9, TEXT_MUTED);
    private static final Font FONT_CUST_NAME= FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11, TEXT_DARK);

    public PdfInvoiceService(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Returns the raw PDF bytes for the given bill.
     * The caller sets Content-Type and Content-Disposition headers.
     */
    public byte[] generateInvoicePdf(Long billId) {
        BillResponse bill = billingService.getBillDetail(billId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 45, 45, 50, 50);

        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            addHeader(doc, bill);
            addCustomerInfo(doc, bill);
            doc.add(Chunk.NEWLINE);

            addLineItemsTable(doc, "SUBSCRIPTION DELIVERIES", bill.getSubscriptionItems());

            if (bill.getAddonItems() != null && !bill.getAddonItems().isEmpty()) {
                doc.add(Chunk.NEWLINE);
                addLineItemsTable(doc, "ADDON ORDERS", bill.getAddonItems());
            }

            if (bill.getAdjustments() != null && !bill.getAdjustments().isEmpty()) {
                doc.add(Chunk.NEWLINE);
                addAdjustmentsTable(doc, bill.getAdjustments());
            }

            doc.add(Chunk.NEWLINE);
            addSummary(doc, bill);
            addFooter(doc);

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF invoice", e);
        } finally {
            doc.close();
        }

        return out.toByteArray();
    }

    // ── Section builders ──────────────────────────────────────────────────────

    private void addHeader(Document doc, BillResponse bill) throws DocumentException {
        // Full-width blue banner
        PdfPTable banner = new PdfPTable(2);
        banner.setWidthPercentage(100);
        banner.setWidths(new float[]{60, 40});

        // Left: Dairy name
        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(HEADER_BG);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(12);
        left.addElement(new Phrase("DAIRY INVOICE", FONT_TITLE));
        Phrase sub = new Phrase("Shivaay Dairy Management", FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(187, 222, 251)));
        left.addElement(sub);
        banner.addCell(left);

        // Right: Bill number + date
        String monthName = Month.of(bill.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(HEADER_BG);
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(12);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Phrase("BILL-" + bill.getBillId(), FONT_INVOICE));
        right.addElement(new Phrase("\nPeriod: " + monthName + " " + bill.getYear(),
                FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(187, 222, 251))));
        right.addElement(new Phrase("\nStatus: " + bill.getStatus(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9,
                        "PAID".equals(bill.getStatus()) ? new Color(165, 214, 167) : new Color(255, 204, 128))));
        banner.addCell(right);

        doc.add(banner);
        doc.add(Chunk.NEWLINE);
    }

    private void addCustomerInfo(Document doc, BillResponse bill) throws DocumentException {
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setWidths(new float[]{50, 50});

        // Left: Bill To
        PdfPCell billTo = new PdfPCell();
        billTo.setBorder(Rectangle.BOX);
        billTo.setBorderColor(new Color(224, 224, 224));
        billTo.setPadding(10);
        billTo.addElement(new Phrase("BILL TO", FONT_SECTION));
        billTo.addElement(Chunk.NEWLINE);
        billTo.addElement(new Phrase(bill.getCustomerName() + "\n", FONT_CUST_NAME));
        billTo.addElement(new Phrase("Customer ID: " + bill.getCustomerId() + "\n", FONT_META));
        info.addCell(billTo);

        // Right: Invoice details
        PdfPCell details = new PdfPCell();
        details.setBorder(Rectangle.BOX);
        details.setBorderColor(new Color(224, 224, 224));
        details.setPadding(10);
        details.addElement(new Phrase("INVOICE DETAILS", FONT_SECTION));
        details.addElement(Chunk.NEWLINE);
        details.addElement(new Phrase("Invoice #:  BILL-" + bill.getBillId() + "\n", FONT_META));
        details.addElement(new Phrase("Period:       " + Month.of(bill.getMonth())
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + bill.getYear() + "\n", FONT_META));
        details.addElement(new Phrase("Status:       " + bill.getStatus() + "\n", FONT_META));
        info.addCell(details);

        doc.add(info);
    }

    private void addLineItemsTable(Document doc, String title, List<BillResponse.LineItem> items)
            throws DocumentException {

        // Section heading
        doc.add(new Paragraph(title, FONT_SECTION));
        doc.add(new LineSeparator(0.5f, 100, BRAND_BLUE, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{18, 32, 12, 14, 14});
        table.setSpacingBefore(4);

        // Header row
        addTH(table, "DATE",        Element.ALIGN_LEFT);
        addTH(table, "PRODUCT",     Element.ALIGN_LEFT);
        addTH(table, "UNIT",        Element.ALIGN_CENTER);
        addTH(table, "QTY",         Element.ALIGN_CENTER);
        addTH(table, "AMOUNT (₹)",  Element.ALIGN_RIGHT);

        if (items == null || items.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No records", FONT_TD));
            empty.setColspan(5);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(6);
            table.addCell(empty);
        } else {
            double subtotal = 0;
            boolean alt = false;
            for (BillResponse.LineItem item : items) {
                Color rowBg = alt ? new Color(250, 250, 250) : Color.WHITE;
                subtotal += item.getSubtotal();

                addTD(table, item.getDate().toString(),  Element.ALIGN_LEFT,   rowBg);
                addTD(table, item.getProductName(),      Element.ALIGN_LEFT,   rowBg);
                addTD(table, item.getUnit(),             Element.ALIGN_CENTER, rowBg);
                addTD(table, fmt(item.getQuantity()),    Element.ALIGN_CENTER, rowBg);
                addTD(table, "₹" + fmt2(item.getSubtotal()), Element.ALIGN_RIGHT, rowBg);
                alt = !alt;
            }

            // Subtotal row
            PdfPCell stLabel = new PdfPCell(new Phrase("Subtotal", FONT_VALUE));
            stLabel.setColspan(4);
            stLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            stLabel.setBackgroundColor(SUBTOTAL_BG);
            stLabel.setPadding(6);
            stLabel.setBorder(Rectangle.TOP);
            table.addCell(stLabel);

            PdfPCell stVal = new PdfPCell(new Phrase("₹" + fmt2(subtotal), FONT_VALUE));
            stVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            stVal.setBackgroundColor(SUBTOTAL_BG);
            stVal.setPadding(6);
            stVal.setBorder(Rectangle.TOP);
            table.addCell(stVal);
        }

        doc.add(table);
    }

    private void addAdjustmentsTable(Document doc, List<BillAdjustment> adjustments)
            throws DocumentException {

        doc.add(new Paragraph("ADJUSTMENTS", FONT_SECTION));
        doc.add(new LineSeparator(0.5f, 100, BRAND_BLUE, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 50, 25});
        table.setSpacingBefore(4);

        addTH(table, "TYPE",        Element.ALIGN_LEFT);
        addTH(table, "DESCRIPTION", Element.ALIGN_LEFT);
        addTH(table, "AMOUNT (₹)",  Element.ALIGN_RIGHT);

        boolean alt = false;
        for (BillAdjustment adj : adjustments) {
            Color rowBg = alt ? new Color(250, 250, 250) : Color.WHITE;
            addTD(table, adj.getAdjustmentType() != null ? adj.getAdjustmentType().name() : "-",
                    Element.ALIGN_LEFT, rowBg);
            addTD(table, adj.getDescription() != null ? adj.getDescription() : "-",
                    Element.ALIGN_LEFT, rowBg);
            addTD(table, "₹" + fmt2(adj.getAmount()),           Element.ALIGN_RIGHT, rowBg);
            alt = !alt;
        }

        doc.add(table);
    }

    private void addSummary(Document doc, BillResponse bill) throws DocumentException {
        doc.add(new Paragraph("SUMMARY", FONT_SECTION));
        doc.add(new LineSeparator(0.5f, 100, BRAND_BLUE, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[]{60, 40});
        table.setSpacingBefore(4);

        addSummaryRow(table, "Subscription Total",  "₹" + fmt2(bill.getSubscriptionAmount()), false);
        addSummaryRow(table, "Addon Total",          "₹" + fmt2(bill.getAddonAmount()),        false);

        if (bill.getPreviousPendingAmount() > 0) {
            addSummaryRow(table, "Previous Pending",
                    "₹" + fmt2(bill.getPreviousPendingAmount()), false);
        }
        if (bill.getAdjustmentAmount() != 0) {
            addSummaryRow(table, "Adjustments",
                    (bill.getAdjustmentAmount() < 0 ? "-" : "+") +
                    "₹" + fmt2(Math.abs(bill.getAdjustmentAmount())), false);
        }

        // Divider
        PdfPCell div = new PdfPCell(new Phrase(""));
        div.setColspan(2);
        div.setFixedHeight(1f);
        div.setBackgroundColor(new Color(189, 189, 189));
        div.setBorder(Rectangle.NO_BORDER);
        table.addCell(div);

        // Total
        PdfPCell tLabel = new PdfPCell(new Phrase("TOTAL", FONT_TOTAL));
        tLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
        tLabel.setBackgroundColor(SUBTOTAL_BG);
        tLabel.setPadding(8);
        tLabel.setBorder(Rectangle.NO_BORDER);
        table.addCell(tLabel);

        PdfPCell tVal = new PdfPCell(new Phrase("₹" + fmt2(bill.getTotalAmount()), FONT_TOTAL));
        tVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tVal.setBackgroundColor(SUBTOTAL_BG);
        tVal.setPadding(8);
        tVal.setBorder(Rectangle.NO_BORDER);
        table.addCell(tVal);

        addSummaryRow(table, "Paid", "₹" + fmt2(bill.getPaidAmount()), false);

        // Balance due row
        PdfPCell dLabel = new PdfPCell(new Phrase("BALANCE DUE", FONT_DUE));
        dLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
        dLabel.setBackgroundColor(DUE_BG);
        dLabel.setPadding(8);
        dLabel.setBorder(Rectangle.BOX);
        dLabel.setBorderColor(new Color(255, 193, 7));
        table.addCell(dLabel);

        PdfPCell dVal = new PdfPCell(new Phrase("₹" + fmt2(bill.getRemainingAmount()), FONT_DUE));
        dVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dVal.setBackgroundColor(DUE_BG);
        dVal.setPadding(8);
        dVal.setBorder(Rectangle.BOX);
        dVal.setBorderColor(new Color(255, 193, 7));
        table.addCell(dVal);

        doc.add(table);
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        doc.add(new LineSeparator(0.5f, 100, new Color(200, 200, 200), Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph(
                "This is a computer-generated invoice. For queries, contact your dairy admin.",
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED));
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // ── Cell helpers ──────────────────────────────────────────────────────────

    private void addTH(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TH));
        cell.setBackgroundColor(TABLE_HEADER);
        cell.setHorizontalAlignment(align);
        cell.setPadding(7);
        cell.setBorderColor(new Color(200, 200, 200));
        table.addCell(cell);
    }

    private void addTD(PdfPTable table, String text, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", FONT_TD));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setPadding(6);
        cell.setBorderColor(new Color(224, 224, 224));
        table.addCell(cell);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, boolean bold) {
        Font lf = bold ? FONT_VALUE : FONT_LABEL;
        Font vf = bold ? FONT_VALUE : FONT_TD;

        PdfPCell lCell = new PdfPCell(new Phrase(label, lf));
        lCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        lCell.setPadding(6);
        lCell.setBorder(Rectangle.BOTTOM);
        lCell.setBorderColor(new Color(238, 238, 238));
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, vf));
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vCell.setPadding(6);
        vCell.setBorder(Rectangle.BOTTOM);
        vCell.setBorderColor(new Color(238, 238, 238));
        table.addCell(vCell);
    }

    // ── Number formatters ─────────────────────────────────────────────────────

    /** Format quantity — drops trailing .0 for whole numbers (e.g. 1.0 → "1", 1.5 → "1.5") */
    private String fmt(double qty) {
        return qty == Math.floor(qty) ? String.valueOf((long) qty) : String.valueOf(qty);
    }

    /** Format currency to 2 decimal places */
    private String fmt2(double amount) {
        return String.format("%.2f", amount);
    }
}
