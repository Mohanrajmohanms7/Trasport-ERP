package com.transport.erp.controller;

import com.transport.erp.dto.ApiResponse;
import com.transport.erp.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Help & Support for client users: report an issue, follow own / company tickets, reply, confirm or reopen.
 * The company always comes from the logged-in user (SupportTicketService); internal notes are never returned here.
 */
@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportTicketService supportTickets;

    @PostMapping("/tickets")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body,
                                                   @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        return ApiResponse.success(supportTickets.create(body, userAgent), "Issue reported");
    }

    @GetMapping("/tickets")
    public ApiResponse<Page<Map<String, Object>>> list(@RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String search,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(supportTickets.listForClient(status, search, page, size), "Tickets fetched");
    }

    /** Bell: support replies / status changes not opened yet. */
    @GetMapping("/notifications")
    public ApiResponse<Map<String, Object>> notifications() {
        return ApiResponse.success(supportTickets.clientNotifications(), "Notifications fetched");
    }

    @GetMapping("/tickets/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(supportTickets.getForClient(id), "Ticket fetched");
    }

    @PostMapping("/tickets/{id}/replies")
    public ApiResponse<Map<String, Object>> reply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(supportTickets.clientReply(id, body), "Reply sent");
    }

    /** Attach a screenshot / image / PDF (multipart field "file"). */
    @PostMapping(value = "/tickets/{id}/attachments", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> attach(@PathVariable Long id, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ApiResponse.success(supportTickets.clientAttach(id, file), "File attached");
    }

    @GetMapping("/tickets/{id}/attachments/{attachmentId}")
    public org.springframework.http.ResponseEntity<byte[]> download(@PathVariable Long id, @PathVariable Long attachmentId) {
        return fileResponse(supportTickets.download(id, attachmentId, false));
    }

    @PostMapping("/tickets/{id}/confirm")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long id) {
        return ApiResponse.success(supportTickets.confirmFixed(id), "Ticket closed");
    }

    @PostMapping("/tickets/{id}/reopen")
    public ApiResponse<Map<String, Object>> reopen(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(supportTickets.reopen(id, body), "Ticket reopened");
    }

    /** File download with a safe filename; images / PDFs open in the browser. */
    static org.springframework.http.ResponseEntity<byte[]> fileResponse(com.transport.erp.model.SaaSSupportAttachment a) {
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(a.getContentType()))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.inline().filename(a.getFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(a.getData());
    }
}
