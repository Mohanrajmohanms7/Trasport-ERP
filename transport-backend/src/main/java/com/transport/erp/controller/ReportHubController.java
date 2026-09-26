package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.service.ReportHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** GET /api/v1/report-hub/catalog, /run/{key}?filters, /export/{key}?format=xlsx|pdf&filters */
@RestController
@RequestMapping("/api/v1/report-hub")
public class ReportHubController {

    @Autowired
    private ReportHubService reports;

    @GetMapping("/catalog")
    public ApiResponse<List<Map<String, Object>>> catalog() {
        return ApiResponse.success(reports.catalog(), "Reports");
    }

    @GetMapping("/run/{key}")
    public ApiResponse<ReportHubService.Result> run(@PathVariable String key, @RequestParam Map<String, String> params) {
        return ApiResponse.success(reports.run(key, params, ReportHubService.MAX_SCREEN_ROWS), "Report ready");
    }

    @GetMapping("/export/{key}")
    public ResponseEntity<byte[]> export(@PathVariable String key, @RequestParam(defaultValue = "xlsx") String format,
                                         @RequestParam Map<String, String> params) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] body = reports.export(key, params, pdf ? "pdf" : "xlsx");
        String file = key + "_" + LocalDate.now() + (pdf ? ".pdf" : ".xlsx");
        return ResponseEntity.ok()
                .contentType(pdf ? MediaType.APPLICATION_PDF
                        : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .body(body);
    }
}
