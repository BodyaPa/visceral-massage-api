package com.example.visceralmassageapi.finance.controller;

import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.finance.service.FinanceExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/finance/export")
@RequiredArgsConstructor
public class AdminFinanceExportController {

    private final FinanceExportService exportService;

    @GetMapping("/xlsx")
    public ResponseEntity<byte[]> xlsx(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "ua") String locale
    ) {
        return download(
                exportService.exportExcel(status, officeId, from, to, locale),
                "ataraksia-finance.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        );
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "ua") String locale
    ) {
        return download(exportService.exportPdf(status, officeId, from, to, locale), "ataraksia-finance.pdf", MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<byte[]> download(byte[] body, String filename, MediaType contentType) {
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(body);
    }
}
