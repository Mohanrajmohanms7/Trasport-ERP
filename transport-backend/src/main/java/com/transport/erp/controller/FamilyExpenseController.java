package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.service.FamilyExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Family Expenses (optional add-on, docs/FAMILY_EXPENSES.md). Personal data of the company owner:
 * only COMPANY_ADMIN / ADMIN of the signed-in company — not the platform admin, accountant, branch staff or drivers.
 * The module, its tabs and actions are switched per client in Platform Admin → Feature Access (off by default).
 */
@RestController
@RequestMapping("/api/v1/family-expenses")
@PreAuthorize("hasAnyRole('COMPANY_ADMIN', 'ADMIN')")
public class FamilyExpenseController {

    @Autowired
    private FamilyExpenseService service;

    private static String user(Principal p) { return p != null ? p.getName() : "SYSTEM"; }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(required = false) String mode,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.success(service.list(from, to, category, mode, page, size), "Family expenses");
    }

    @GetMapping("/options")
    public ApiResponse<Map<String, Object>> options() {
        return ApiResponse.success(service.options(), "Options");
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestParam(required = false) String month) {
        return ApiResponse.success(service.summary(month), "Summary");
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(service.get(id), "Family expense");
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body, Principal principal) {
        return ApiResponse.success(service.create(body, user(principal)), "Family expense saved");
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        return ApiResponse.success(service.update(id, body, user(principal)), "Family expense updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Principal principal) {
        service.delete(id, user(principal));
        return ApiResponse.success(null, "Family expense deleted");
    }

    // --- categories ---

    @GetMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> categories() {
        return ApiResponse.success(service.categories(), "Categories");
    }

    @PostMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> createCategory(@RequestBody Map<String, Object> body, Principal principal) {
        return ApiResponse.success(service.createCategory(body, user(principal)), "Category added");
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<List<Map<String, Object>>> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        return ApiResponse.success(service.updateCategory(id, body, user(principal)), "Category updated");
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<List<Map<String, Object>>> deleteCategory(@PathVariable Long id, Principal principal) {
        return ApiResponse.success(service.deleteCategory(id, user(principal)), "Category deleted");
    }

    // --- reports & export ---

    @GetMapping("/reports/{key}")
    public ApiResponse<FamilyExpenseService.Report> report(@PathVariable String key,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                           @RequestParam(required = false) String category,
                                                           @RequestParam(required = false) String mode) {
        return ApiResponse.success(service.report(key, from, to, category, mode), "Report");
    }

    @GetMapping("/export/{key}")
    public ResponseEntity<byte[]> export(@PathVariable String key,
                                         @RequestParam(defaultValue = "xlsx") String format,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String mode) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] body = service.export(key, pdf ? "pdf" : "xlsx", from, to, category, mode);
        String file = "family-expenses-" + key + "_" + LocalDate.now() + (pdf ? ".pdf" : ".xlsx");
        return ResponseEntity.ok()
                .contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .body(body);
    }
}
