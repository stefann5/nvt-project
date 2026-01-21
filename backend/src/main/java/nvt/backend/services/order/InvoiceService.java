package nvt.backend.services.order;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nvt.backend.model.order.Order;
import nvt.backend.model.order.OrderItem;
import nvt.backend.services.storage.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final MinioService minioService;

    @Value("${minio.bucket.invoices:invoices}")
    private String invoicesBucket;

    public String generateAndUploadInvoice(Order order) {
        try {
            byte[] pdfBytes = generateInvoicePdf(order);

            minioService.createBucketIfNotExists(invoicesBucket);

            String fileName = "invoice-" + order.getOrderNumber() + ".pdf";
            String minioPath = minioService.uploadFileFromBytes(
                    pdfBytes,
                    invoicesBucket,
                    "invoices/" + order.getId(),
                    fileName,
                    "application/pdf"
            );

            return minioPath;
        } catch (Exception e) {
            log.error("Failed to generate invoice for order {}: {}", order.getOrderNumber(), e.getMessage());
            throw new RuntimeException("Failed to generate invoice", e);
        }
    }

    private byte[] generateInvoicePdf(Order order) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("FAKTURA / INVOICE")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            document.add(new Paragraph("Broj fakture: " + order.getOrderNumber())
                    .setFontSize(12));

            document.add(new Paragraph("Datum: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")))
                    .setFontSize(12)
                    .setMarginBottom(20));

            document.add(new Paragraph("PODACI O KUPCU")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(10));

            document.add(new Paragraph("Ime i prezime: " + order.getCustomer().getName() + " " + order.getCustomer().getSurname())
                    .setFontSize(11));

            document.add(new Paragraph("Firma: " + order.getCompany().getName())
                    .setFontSize(11));

            String address = buildAddress(order);
            document.add(new Paragraph("Adresa dostave: " + address)
                    .setFontSize(11)
                    .setMarginBottom(20));

            document.add(new Paragraph("STAVKE PORUDZBINE")
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(10)
                    .setMarginBottom(10));

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 4, 2, 2, 2}))
                    .useAllAvailableWidth();

            table.addHeaderCell(createHeaderCell("R.br."));
            table.addHeaderCell(createHeaderCell("Proizvod"));
            table.addHeaderCell(createHeaderCell("Kolicina"));
            table.addHeaderCell(createHeaderCell("Jed. cena"));
            table.addHeaderCell(createHeaderCell("Ukupno"));

            int itemNumber = 1;
            for (OrderItem item : order.getItems()) {
                table.addCell(createCell(String.valueOf(itemNumber++)));
                table.addCell(createCell(item.getProduct().getName() + "\n(" + item.getProduct().getSku() + ")"));
                table.addCell(createCell(item.getQuantity().toString()));
                table.addCell(createCell(formatPrice(item.getUnitPrice())));
                table.addCell(createCell(formatPrice(item.getTotalPrice())));
            }

            document.add(table);

            document.add(new Paragraph("UKUPNO ZA UPLATU: " + formatPrice(order.getTotalAmount()))
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(20));

            if (order.getNotes() != null && !order.getNotes().isBlank()) {
                document.add(new Paragraph("Napomena: " + order.getNotes())
                        .setFontSize(10)
                        .setMarginTop(20));
            }

            document.add(new Paragraph("Hvala na poverenju!")
                    .setFontSize(12)
                    .setItalic()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(40));

        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return outputStream.toByteArray();
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell createCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setTextAlignment(TextAlignment.CENTER);
    }

    private String formatPrice(BigDecimal price) {
        return String.format("%.2f EUR", price);
    }

    private String buildAddress(Order order) {
        StringBuilder sb = new StringBuilder();
        if (order.getCompany().getStreet() != null) {
            sb.append(order.getCompany().getStreet());
            if (order.getCompany().getStreetNumber() != null) {
                sb.append(" ").append(order.getCompany().getStreetNumber());
            }
        }
        if (order.getCompany().getCity() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(order.getCompany().getCity().getName());
        }
        if (order.getCompany().getCountry() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(order.getCompany().getCountry().getName());
        }
        return sb.toString();
    }

    public byte[] getInvoicePdf(String invoicePath) {
        try {
            return minioService.downloadFile(invoicesBucket, invoicePath);
        } catch (Exception e) {
            log.error("Failed to download invoice: {}", e.getMessage());
            throw new RuntimeException("Failed to download invoice", e);
        }
    }
}
