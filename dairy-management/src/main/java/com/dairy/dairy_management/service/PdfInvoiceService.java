package com.dairy.dairy_management.service;

import com.dairy.dairy_management.dto.BillResponse;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.*;

/**
 * Generates a PDF bill matching the Shivay Dairy bill format.
 *
 * Layout:
 *   ┌─────────────────────────────────────┐
 *   │  OM SAI RAM  /  Shivay Dairy / URL  │  (green banner)
 *   ├──────────────────┬──────────────────┤
 *   │  Customer info   │  Billing summary  │
 *   ├──────────────────┴──────────────────┤
 *   │  Product Summary grid (date x prod) │
 *   │  Total Qty / Rate / Amount / Grand  │
 *   ├─────────────────────────────────────┤
 *   │  Bank / UPI payment details         │
 *   └─────────────────────────────────────┘
 *       Thank You For Choosing Shivay Dairy
 */
@Service
public class PdfInvoiceService {

    private final BillingService billingService;

    // ── Dairy constants ────────────────────────────────────────────────────────
    private static final String DAIRY_NAME   = "Shivay Dairy";
    private static final String WEBSITE      = "shivaydairy.com";
    /** ॐ साईं राम */
    private static final String OM_TEXT      = "\u0950 \u0938\u093E\u0908\u0902 \u0930\u093E\u092E";
    private static final String BANK_ACCOUNT = "924020000770140";
    private static final String IFSC_CODE    = "UTIB0002652";
    private static final String UPI_NUMBER   = "8796254008";

    // ── Colour palette ─────────────────────────────────────────────────────────
    private static final Color GREEN_DARK  = new Color(27,  94,  32);
    private static final Color GREEN_MID   = new Color(56, 142,  60);
    private static final Color GREEN_LIGHT = new Color(232, 245, 233);
    private static final Color TEXT_DARK   = new Color(33,  33,  33);
    private static final Color TEXT_MUTED  = new Color(97,  97,  97);
    private static final Color ABSENT_BG   = new Color(255, 243, 224);
    private static final Color BORDER_CLR  = new Color(200, 200, 200);
    private static final Color TH_BORDER   = new Color(200, 230, 201);

    // ── Static fonts (Helvetica) ───────────────────────────────────────────────
    private static final Font F_DAIRY   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  20, Color.WHITE);
    private static final Font F_WEB     = FontFactory.getFont(FontFactory.HELVETICA,         9, new Color(200, 230, 201));
    private static final Font F_LABEL   = FontFactory.getFont(FontFactory.HELVETICA,         9, TEXT_MUTED);
    private static final Font F_VALUE   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    9, TEXT_DARK);
    private static final Font F_TH      = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    8, Color.WHITE);
    private static final Font F_TD      = FontFactory.getFont(FontFactory.HELVETICA,          8, TEXT_DARK);
    private static final Font F_TOTAL   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    9, TEXT_DARK);
    private static final Font F_GRAND   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   11, GREEN_DARK);
    private static final Font F_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   10, GREEN_DARK);
    private static final Font F_FOOTER  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   10, GREEN_DARK);
    private static final Font F_ABSENT  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    8, new Color(183, 28, 28));

    public PdfInvoiceService(BillingService billingService) {
        this.billingService = billingService;
    }

    /** Returns raw PDF bytes for the given bill. */
    public byte[] generateInvoicePdf(Long billId) {
        BillResponse bill = billingService.getBillDetail(billId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font fOm       = tryDevanagariFont(13, Color.WHITE);
            Font fCustName = tryDevanagariFont(11, TEXT_DARK);

            addHeader(doc, fOm);
            addInfoSection(doc, bill, fCustName);
            doc.add(Chunk.NEWLINE);
            addProductGrid(doc, bill);
            doc.add(Chunk.NEWLINE);
            addPaymentSection(doc);
            addFooter(doc);

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF invoice", e);
        } finally {
            doc.close();
        }

        return out.toByteArray();
    }

    // ── Section builders ───────────────────────────────────────────────────────

    private void addHeader(Document doc, Font fOm) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(GREEN_DARK);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(12);
        cell.setPaddingBottom(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph omPara = new Paragraph(OM_TEXT, fOm);
        omPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(omPara);

        Paragraph namePara = new Paragraph(DAIRY_NAME, F_DAIRY);
        namePara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(namePara);

        Paragraph webPara = new Paragraph(WEBSITE, F_WEB);
        webPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(webPara);

        banner.addCell(cell);
        doc.add(banner);
    }

    private void addInfoSection(Document doc, BillResponse bill, Font fCustName) throws DocumentException {
        // "Oct-24" format
        String period = Month.of(bill.getMonth()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                + "-" + String.valueOf(bill.getYear()).substring(2);

        double billThisMonth = bill.getSubscriptionAmount() + bill.getAddonAmount()
                + bill.getAdjustmentAmount();

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{55, 45});
        table.setSpacingBefore(8);

        // ── Left: customer info ──────────────────────────────────────────────
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.BOX);
        left.setBorderColor(BORDER_CLR);
        left.setPadding(10);

        Paragraph nameP = new Paragraph(bill.getCustomerName(), fCustName);
        left.addElement(nameP);
        left.addElement(Chunk.NEWLINE);
        addLV(left, "Number",         safeStr(bill.getCustomerPhone()));
        addLV(left, "Address",        safeStr(bill.getCustomerAddress()));
        addLV(left, "Billing Period", period);

        table.addCell(left);

        // ── Right: billing summary ───────────────────────────────────────────
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.BOX);
        right.setBorderColor(BORDER_CLR);
        right.setPadding(10);

        addLV(right, "Total Bill This Month", fmt2(billThisMonth));
        addLV(right, "Previous Balance",      fmt2(bill.getPreviousPendingAmount()));

        right.addElement(Chunk.NEWLINE);

        Paragraph totalP = new Paragraph();
        totalP.add(new Chunk("Total   ", F_LABEL));
        totalP.add(new Chunk(fmt2(bill.getTotalAmount()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, GREEN_DARK)));
        right.addElement(totalP);

        right.addElement(Chunk.NEWLINE);
        right.addElement(new Paragraph("Scan To Pay",
                FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED)));
        right.addElement(new Paragraph(UPI_NUMBER,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GREEN_DARK)));

        table.addCell(right);
        doc.add(table);
    }

    private void addProductGrid(Document doc, BillResponse bill) throws DocumentException {
        doc.add(new Paragraph("Product Summary", F_SECTION));
        doc.add(new LineSeparator(0.5f, 100, GREEN_MID, Element.ALIGN_LEFT, -2));
        doc.add(Chunk.NEWLINE);

        List<BillResponse.LineItem> items = bill.getSubscriptionItems();
        if (items == null) items = Collections.emptyList();

        // Distinct products sorted alphabetically
        List<String> products = items.stream()
                .map(BillResponse.LineItem::getProductName)
                .distinct().sorted().collect(Collectors.toList());

        // Map: date → productName → LineItem
        Map<LocalDate, Map<String, BillResponse.LineItem>> grid = new HashMap<>();
        for (BillResponse.LineItem li : items) {
            grid.computeIfAbsent(li.getDate(), k -> new HashMap<>()).put(li.getProductName(), li);
        }

        int numProds = Math.max(products.size(), 1); // at least 1 col so table renders
        int totalCols = 1 + numProds;

        float[] widths = new float[totalCols];
        widths[0] = 5f;
        float prodColW = Math.max(7f, 75f / numProds);
        Arrays.fill(widths, 1, totalCols, prodColW);

        PdfPTable table = new PdfPTable(totalCols);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setSpacingBefore(4);

        // Header row
        addGridTH(table, "Date");
        for (String p : products) addGridTH(table, p);
        if (products.isEmpty()) addGridTH(table, "Deliveries");

        // Day rows
        int daysInMonth = YearMonth.of(bill.getYear(), bill.getMonth()).lengthOfMonth();
        LocalDate monthStart = LocalDate.of(bill.getYear(), bill.getMonth(), 1);

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = monthStart.withDayOfMonth(day);
            boolean isWeekend = (date.getDayOfWeek() == DayOfWeek.SUNDAY);
            Color rowBg = isWeekend ? new Color(245, 245, 245) : Color.WHITE;

            addGridTD(table, String.valueOf(day), Element.ALIGN_CENTER, rowBg, F_TD);

            Map<String, BillResponse.LineItem> row = grid.getOrDefault(date, Collections.emptyMap());
            if (products.isEmpty()) {
                addGridTD(table, "-", Element.ALIGN_CENTER, rowBg, F_TD);
            } else {
                for (String p : products) {
                    BillResponse.LineItem li = row.get(p);
                    if (li != null) {
                        addGridTD(table, fmt(li.getQuantity()), Element.ALIGN_CENTER, rowBg, F_TD);
                    } else {
                        addGridTD(table, "A", Element.ALIGN_CENTER, ABSENT_BG, F_ABSENT);
                    }
                }
            }
        }

        // ── Total Qty row ────────────────────────────────────────────────────
        addGridTH2(table, "Total Qty");
        final List<BillResponse.LineItem> finalItems = items;
        for (String p : products) {
            double totalQty = finalItems.stream()
                    .filter(li -> li.getProductName().equals(p))
                    .mapToDouble(BillResponse.LineItem::getQuantity).sum();
            addGridTD(table, fmt(totalQty), Element.ALIGN_CENTER, GREEN_LIGHT, F_TOTAL);
        }
        if (products.isEmpty()) addGridTD(table, "-", Element.ALIGN_CENTER, GREEN_LIGHT, F_TOTAL);

        // ── Rate/Unit row ────────────────────────────────────────────────────
        addGridTH2(table, "Rate/Unit");
        for (String p : products) {
            double rate = finalItems.stream()
                    .filter(li -> li.getProductName().equals(p))
                    .mapToDouble(BillResponse.LineItem::getPricePerUnit)
                    .average().orElse(0);
            addGridTD(table, fmt2(rate), Element.ALIGN_CENTER, GREEN_LIGHT, F_TOTAL);
        }
        if (products.isEmpty()) addGridTD(table, "-", Element.ALIGN_CENTER, GREEN_LIGHT, F_TOTAL);

        // ── Total Amount row ─────────────────────────────────────────────────
        addGridTH2(table, "Total Amount");
        for (String p : products) {
            double amt = finalItems.stream()
                    .filter(li -> li.getProductName().equals(p))
                    .mapToDouble(BillResponse.LineItem::getSubtotal).sum();
            addGridTD(table, fmtComma(amt), Element.ALIGN_CENTER, GREEN_LIGHT, F_TOTAL);
        }
        if (products.isEmpty()) addGridTD(table, "-", Element.ALIGN_CENTER, GREEN_LIGHT, F_TOTAL);

        // ── Grand Total row ──────────────────────────────────────────────────
        // First cell (Date column): label
        PdfPCell grandLabel = new PdfPCell(new Phrase("GRAND TOTAL", F_GRAND));
        grandLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
        grandLabel.setBackgroundColor(GREEN_LIGHT);
        grandLabel.setPadding(7);
        grandLabel.setBorderColor(BORDER_CLR);
        table.addCell(grandLabel);

        // Remaining cells: merge all product columns into one value
        PdfPCell grandVal = new PdfPCell(new Phrase(fmt2(bill.getTotalAmount()), F_GRAND));
        grandVal.setColspan(numProds);
        grandVal.setHorizontalAlignment(Element.ALIGN_CENTER);
        grandVal.setBackgroundColor(GREEN_LIGHT);
        grandVal.setPadding(7);
        grandVal.setBorderColor(BORDER_CLR);
        table.addCell(grandVal);

        doc.add(table);

        // Addon orders note (if any)
        if (bill.getAddonItems() != null && !bill.getAddonItems().isEmpty()) {
            doc.add(Chunk.NEWLINE);
            Paragraph note = new Paragraph(
                    "* Grand Total includes addon orders of \u20b9" + fmt2(bill.getAddonAmount()),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED));
            doc.add(note);
        }
    }

    private void addPaymentSection(Document doc) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{42, 58});
        table.setSpacingBefore(4);

        PdfPCell title = new PdfPCell(new Phrase("IMPS / NEFT / RTGS / UPI", F_SECTION));
        title.setColspan(2);
        title.setBackgroundColor(GREEN_LIGHT);
        title.setPadding(7);
        title.setBorderColor(BORDER_CLR);
        table.addCell(title);

        addPayRow(table, "A/C NUMBER", BANK_ACCOUNT);
        addPayRow(table, "IFSC CODE", IFSC_CODE);
        addPayRow(table, "GOOGLE PAY / PHONEPE / PAYTM", UPI_NUMBER);

        doc.add(table);
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("Thank You For Choosing " + DAIRY_NAME, F_FOOTER);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // ── Cell helpers ───────────────────────────────────────────────────────────

    /** Label: value line inside a cell */
    private void addLV(PdfPCell cell, String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ":  ", F_LABEL));
        p.add(new Chunk(value, F_VALUE));
        cell.addElement(p);
    }

    /** Green header cell for the product grid */
    private void addGridTH(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, F_TH));
        cell.setBackgroundColor(GREEN_MID);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        cell.setBorderColor(TH_BORDER);
        table.addCell(cell);
    }

    /** Light-green header cell for Total Qty / Rate / Amount rows */
    private void addGridTH2(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, F_TOTAL));
        cell.setBackgroundColor(GREEN_LIGHT);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_CLR);
        table.addCell(cell);
    }

    private void addGridTD(PdfPTable table, String text, int align, Color bg, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        cell.setBorderColor(BORDER_CLR);
        table.addCell(cell);
    }

    private void addPayRow(PdfPTable table, String label, String value) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, F_LABEL));
        lCell.setPadding(7);
        lCell.setBorderColor(BORDER_CLR);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, F_VALUE));
        vCell.setPadding(7);
        vCell.setBorderColor(BORDER_CLR);
        table.addCell(vCell);
    }

    // ── Devanagari font loader ─────────────────────────────────────────────────

    /**
     * Tries to load a Devanagari-capable TrueType font from common system locations.
     * Falls back to Helvetica-Bold if none found (Sanskrit text will render as boxes).
     */
    private Font tryDevanagariFont(float size, Color color) {
        String[] paths = {
            "C:\\Windows\\Fonts\\mangal.ttf",
            "C:\\Windows\\Fonts\\NirmalaUI.ttf",
            "C:\\Windows\\Fonts\\Nirmala.ttf",
            "/usr/share/fonts/truetype/lohit-devanagari/Lohit-Devanagari.ttf",
            "/usr/share/fonts/truetype/noto/NotoSansDevanagari-Regular.ttf"
        };
        for (String path : paths) {
            try {
                BaseFont bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(bf, size, Font.NORMAL, color);
            } catch (Exception ignored) {
                // try next
            }
        }
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, size, color);
    }

    // ── Number formatters ──────────────────────────────────────────────────────

    /** Drops trailing .0 for whole numbers: 1.0 → "1", 1.5 → "1.5" */
    private String fmt(double qty) {
        return qty == Math.floor(qty) ? String.valueOf((long) qty) : String.valueOf(qty);
    }

    /** Two decimal places */
    private String fmt2(double amount) {
        return String.format("%.2f", amount);
    }

    /** Comma-formatted, no decimals (e.g. 1,728) */
    private String fmtComma(double amount) {
        return String.format("%,.0f", amount);
    }

    private String safeStr(String s) {
        return s != null ? s : "-";
    }
}
