package com.example.visceralmassageapi.finance.service;

import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.finance.repository.SpecialistFinanceSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.example.visceralmassageapi.booking.repository.BookingSpecifications.financeFilter;

@Service
@RequiredArgsConstructor
public class FinanceExportService {

    private final BookingRepository bookingRepository;
    private final SpecialistFinanceSettingsRepository specialistFinanceSettingsRepository;

    @Transactional(readOnly = true)
    public byte[] exportExcel(BookingStatus status, Long officeId, OffsetDateTime from, OffsetDateTime to) {
        return buildXlsx(rows(status, officeId, from, to));
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(BookingStatus status, Long officeId, OffsetDateTime from, OffsetDateTime to) {
        List<List<String>> rows = rows(status, officeId, from, to);
        List<String> lines = new ArrayList<>();
        lines.add("Ataraksia finance report");
        lines.add("Generated rows: " + Math.max(rows.size() - 1, 0));
        lines.add("");
        for (List<String> row : rows) {
            lines.add(String.join(" | ", row));
        }
        return buildPdf(lines);
    }

    private List<List<String>> rows(BookingStatus status, Long officeId, OffsetDateTime from, OffsetDateTime to) {
        List<Booking> bookings = bookingRepository.findAll(financeFilter(status, officeId, from, to));
        Map<Long, BigDecimal> sharePercents = specialistFinanceSettingsRepository
                .findBySpecialistUserIdIn(bookings.stream().map(booking -> booking.getSpecialist().getId()).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(settings -> settings.getSpecialist().getId(), settings -> settings.getSpecialistSharePercent()));

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of(
                "Booking ID",
                "Status",
                "Client",
                "Specialist",
                "Service",
                "Office",
                "Starts at",
                "Booked price",
                "Specialist share",
                "Business share",
                "Payout status",
                "Payout paid at"
        ));

        for (Booking booking : bookings) {
            BigDecimal sharePercent = sharePercents.getOrDefault(booking.getSpecialist().getId(), BigDecimal.ZERO);
            BigDecimal specialistShare = FinanceShareCalculator.specialistShare(booking.getBookedPrice(), sharePercent);
            BigDecimal businessShare = FinanceShareCalculator.businessShare(booking.getBookedPrice(), specialistShare);

            rows.add(List.of(
                    String.valueOf(booking.getId()),
                    booking.getStatus().name(),
                    displayName(booking.getUser()),
                    displayName(booking.getSpecialist()),
                    booking.getService().getTitleUa(),
                    booking.getOffice() == null ? "" : booking.getOffice().getName(),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(booking.getStartsAt()),
                    booking.getBookedPrice().toPlainString(),
                    specialistShare.toPlainString(),
                    businessShare.toPlainString(),
                    booking.getSpecialistPayoutStatus().name(),
                    booking.getSpecialistPayoutPaidAt() == null ? "" : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(booking.getSpecialistPayoutPaidAt())
            ));
        }
        return rows;
    }

    private byte[] buildXlsx(List<List<String>> rows) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
                addZipEntry(zip, "[Content_Types].xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                          <Default Extension="xml" ContentType="application/xml"/>
                          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                        </Types>
                        """);
                addZipEntry(zip, "_rels/.rels", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                        </Relationships>
                        """);
                addZipEntry(zip, "xl/_rels/workbook.xml.rels", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                        </Relationships>
                        """);
                addZipEntry(zip, "xl/workbook.xml", """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                          <sheets><sheet name="Finance" sheetId="1" r:id="rId1"/></sheets>
                        </workbook>
                        """);
                addZipEntry(zip, "xl/worksheets/sheet1.xml", worksheetXml(rows));
            }
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to build XLSX export", ex);
        }
    }

    private String worksheetXml(List<List<String>> rows) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            xml.append("<row r=\"").append(rowIndex + 1).append("\">");
            List<String> row = rows.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                xml.append("<c r=\"").append(columnName(columnIndex)).append(rowIndex + 1).append("\" t=\"inlineStr\"><is><t>")
                        .append(xml(row.get(columnIndex)))
                        .append("</t></is></c>");
            }
            xml.append("</row>");
        }
        xml.append("</sheetData></worksheet>");
        return xml.toString();
    }

    private byte[] buildPdf(List<String> lines) {
        StringBuilder content = new StringBuilder("BT /F1 10 Tf 40 790 Td 14 TL ");
        for (String line : lines.stream().limit(48).toList()) {
            content.append("(").append(pdf(line.length() > 120 ? line.substring(0, 120) : line)).append(") Tj T* ");
        }
        content.append("ET");

        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + content.toString().getBytes(StandardCharsets.UTF_8).length + " >>\nstream\n" + content + "\nendstream"
        );

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.UTF_8).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xrefOffset = pdf.toString().getBytes(StandardCharsets.UTF_8).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append("%010d 00000 n \n".formatted(offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xrefOffset)
                .append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void addZipEntry(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int current = index;
        do {
            name.insert(0, (char) ('A' + current % 26));
            current = current / 26 - 1;
        } while (current >= 0);
        return name.toString();
    }

    private String xml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String pdf(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String displayName(com.example.visceralmassageapi.auth.domain.User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String name = (firstName + " " + lastName).trim();
        if (!name.isBlank()) return name;
        if (user.getPhone() != null) return user.getPhone();
        if (user.getEmail() != null) return user.getEmail();
        return "User " + user.getId();
    }
}
