package com.example.spip_draft.service;

import com.example.spip_draft.entity.Booking;
import com.example.spip_draft.entity.BookingAgreement;
import com.example.spip_draft.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AgreementPdfService {

    private final FileStorageService fileStorageService;

    private static final Color BRAND_DARK = new Color(19, 36, 64);
    private static final Color BRAND_SOFT = new Color(240, 248, 255);

    public String generateAgreementPdf(BookingAgreement agreement) {
        try {
            Booking booking = agreement.getBooking();
            Payment payment = agreement.getPayment();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 42, 42, 48, 48);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte canvas = writer.getDirectContentUnder();
                    Font watermarkFont = new Font(Font.HELVETICA, 54, Font.BOLD, new Color(220, 230, 240));

                    ColumnText.showTextAligned(
                            canvas,
                            Element.ALIGN_CENTER,
                            new Phrase("BrickSquare", watermarkFont),
                            300,
                            420,
                            45
                    );
                }
            });

            document.open();

            Font titleFont = new Font(Font.HELVETICA, 21, Font.BOLD, BRAND_DARK);
            Font sectionFont = new Font(Font.HELVETICA, 13, Font.BOLD, BRAND_DARK);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, BRAND_DARK);
            Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);

            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);

            PdfPCell headCell = new PdfPCell();
            headCell.setBorder(Rectangle.NO_BORDER);
            headCell.setBackgroundColor(BRAND_SOFT);
            headCell.setPadding(14f);

            Paragraph brand = new Paragraph("BrickSquare Smart Plot Investment Platform", titleFont);
            String agreementHeading = agreement.getStatus() != null && agreement.getStatus().name().equals("SIGNED")
                    ? "Executed Booking & Escrow Agreement"
                    : "Digital Booking & Escrow Agreement";
            Paragraph sub = new Paragraph(agreementHeading, sectionFont);
            Paragraph meta = new Paragraph(
                    "Agreement No: " + agreement.getAgreementNumber()
                            + "\nGenerated On: " + safeDate(agreement.getGeneratedAt())
                            + "\nBooking ID: " + booking.getId(),
                    normalFont
            );

            headCell.addElement(brand);
            headCell.addElement(new Paragraph(" "));
            headCell.addElement(sub);
            headCell.addElement(new Paragraph(" "));
            headCell.addElement(meta);

            header.addCell(headCell);
            document.add(header);
            document.add(new Paragraph(" "));

            addSectionTitle(document, "1. Parties", sectionFont);
            document.add(new Paragraph(
                    "Investor: " + safeText(booking.getInvestor().getUser().getFullName())
                            + "\nBuilder: " + safeText(booking.getPlot().getProject().getBuilder().getCompanyName())
                            + "\nBuilder Contact User: " + safeText(booking.getPlot().getProject().getBuilder().getUser().getFullName()),
                    normalFont
            ));
            document.add(new Paragraph(" "));

            addSectionTitle(document, "2. Property Details", sectionFont);
            document.add(new Paragraph(
                    "Project: " + safeText(booking.getPlot().getProject().getProjectName())
                            + "\nPlot Number: " + safeText(booking.getPlot().getPlotNumber())
                            + "\nTenant ID: " + booking.getTenantId(),
                    normalFont
            ));
            document.add(new Paragraph(" "));

            addSectionTitle(document, "3. Payment Summary", sectionFont);
            BigDecimal totalPrice = booking.getPlot().getPrice() != null ? booking.getPlot().getPrice() : BigDecimal.ZERO;
            BigDecimal tokenAmount = booking.getAmount() != null ? booking.getAmount() : BigDecimal.ZERO;
            BigDecimal remaining = totalPrice.subtract(tokenAmount);

            document.add(new Paragraph(
                    "Total Property Price: ₹" + totalPrice.toPlainString()
                            + "\nToken Paid: ₹" + tokenAmount.toPlainString()
                            + "\nRemaining Amount: ₹" + remaining.max(BigDecimal.ZERO).toPlainString()
                            + "\nEscrow Status: " + safeText(payment != null && payment.getEscrowStatus() != null ? payment.getEscrowStatus().name() : "HELD"),
                    normalFont
            ));
            document.add(new Paragraph(" "));

            addSectionTitle(document, "4. Investor Responsibilities", sectionFont);
            addBullet(document, "Review booking, payment, and property details carefully before acceptance.", normalFont);
            addBullet(document, "Complete payment commitments as per agreed flow or EMI plan, if applicable.", normalFont);
            addBullet(document, "Raise disputes promptly if legal, payment, or escrow issues are identified.", normalFont);

            document.add(new Paragraph(" "));

            addSectionTitle(document, "5. Builder Responsibilities", sectionFont);
            addBullet(document, "Provide verified property, project, and legal details through the platform.", normalFont);
            addBullet(document, "Honor the approved booking and support ownership transfer / documentation flow.", normalFont);
            addBullet(document, "Cooperate on dispute, compliance, and escrow review steps if any issue arises.", normalFont);

            document.add(new Paragraph(" "));

            addSectionTitle(document, "6. Escrow & Compliance Note", sectionFont);
            document.add(new Paragraph(
                    "All final payment funds remain subject to escrow review rules configured by the platform. "
                            + "Funds may be held, reviewed, frozen, released, or refunded according to legal, risk, and transaction conditions. "
                            + "Agreement completion does not automatically guarantee immediate payout.",
                    normalFont
            ));
            document.add(new Paragraph(" "));

            addSectionTitle(document, "7. Digital Acceptance & Signature Record", sectionFont);

            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setSpacingBefore(8f);
            signTable.setWidths(new float[]{1f, 1f});

            signTable.addCell(signatureCell(
                    "Investor",
                    booking.getInvestor().getUser().getFullName(),
                    Boolean.TRUE.equals(agreement.getInvestorSigned()),
                    agreement.getInvestorSignedAt() != null ? safeDateTime(agreement.getInvestorSignedAt()) : "Pending",
                    boldFont,
                    normalFont
            ));

            signTable.addCell(signatureCell(
                    "Builder",
                    booking.getPlot().getProject().getBuilder().getCompanyName(),
                    Boolean.TRUE.equals(agreement.getBuilderSigned()),
                    agreement.getBuilderSignedAt() != null ? safeDateTime(agreement.getBuilderSignedAt()) : "Pending",
                    boldFont,
                    normalFont
            ));

            document.add(signTable);
            document.add(new Paragraph(" "));

            String footerText = agreement.getStatus() != null && agreement.getStatus().name().equals("SIGNED")
                    ? "This is the executed agreement version generated by BrickSquare after both parties completed digital signature steps."
                    : "This document was generated digitally by BrickSquare. For operational and demonstration purposes, digital acceptance and signature events are recorded in the platform database.";

            Paragraph footer = new Paragraph(footerText, smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            return fileStorageService.storeAgreementPdf(
                    baos.toByteArray(),
                    agreement.getAgreementNumber() + ".pdf"
            );

        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate agreement PDF: " + ex.getMessage(), ex);
        }
    }

    private void addSectionTitle(Document document, String title, Font font) throws DocumentException {
        Paragraph p = new Paragraph(title, font);
        p.setSpacingAfter(6f);
        document.add(p);
    }

    private void addBullet(Document document, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph("• " + text, font);
        p.setSpacingAfter(4f);
        document.add(p);
    }

    private PdfPCell signatureCell(String role,
                                   String name,
                                   boolean signed,
                                   String signedAt,
                                   Font boldFont,
                                   Font normalFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12f);
        cell.setBackgroundColor(BRAND_SOFT);
        cell.setBorderColor(new Color(200, 215, 230));

        cell.addElement(new Paragraph(role, boldFont));
        cell.addElement(new Paragraph("Name: " + safeText(name), normalFont));
        cell.addElement(new Paragraph("Status: " + (signed ? "Signed" : "Pending"), normalFont));
        cell.addElement(new Paragraph("Signed At: " + safeText(signedAt), normalFont));

        return cell;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String safeDate(java.time.LocalDateTime value) {
        if (value == null) return "N/A";
        return value.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String safeDateTime(java.time.LocalDateTime value) {
        if (value == null) return "N/A";
        return value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}