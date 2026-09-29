package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.service.BulkImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/** Excel bulk creation: template → validate (preview) → create valid rows. */
@RestController
@RequestMapping("/api/v1/bulk-import")
public class BulkImportController {

    @Autowired
    private BulkImportService bulkImportService;

    @GetMapping("/modules")
    public ApiResponse<List<BulkImportService.Module>> modules() {
        return ApiResponse.success(bulkImportService.modules(), "Upload modules");
    }

    @GetMapping("/{module}/template")
    public ResponseEntity<byte[]> template(@PathVariable String module) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + module + "_upload_template.xlsx\"")
                .body(bulkImportService.template(module));
    }

    @PostMapping(value = "/{module}/validate", consumes = "multipart/form-data")
    public ApiResponse<BulkImportService.ValidationResult> validate(@PathVariable String module, @RequestParam("file") MultipartFile file) {
        BulkImportService.ValidationResult r = bulkImportService.validateFile(module, file);
        return ApiResponse.success(r, r.valid() + " valid, " + r.invalid() + " with errors");
    }

    /** Body: {"rows":[{column:value,...}]} — re-validated here; all rows are created together or none. */
    @PostMapping("/{module}/create")
    public ApiResponse<Map<String, Object>> create(@PathVariable String module, @RequestBody Map<String, List<Map<String, String>>> body,
                                                   Principal principal) {
        Map<String, Object> r = bulkImportService.create(module, body.get("rows"), principal != null ? principal.getName() : "system");
        return ApiResponse.success(r, r.get("created") + " record(s) created");
    }
}
