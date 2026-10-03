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

    @GetMapping("/tickets/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long id) {
        return ApiResponse.success(supportTickets.getForClient(id), "Ticket fetched");
    }

    @PostMapping("/tickets/{id}/replies")
    public ApiResponse<Map<String, Object>> reply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(supportTickets.clientReply(id, body), "Reply sent");
    }

    @PostMapping("/tickets/{id}/confirm")
    public ApiResponse<Map<String, Object>> confirm(@PathVariable Long id) {
        return ApiResponse.success(supportTickets.confirmFixed(id), "Ticket closed");
    }

    @PostMapping("/tickets/{id}/reopen")
    public ApiResponse<Map<String, Object>> reopen(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(supportTickets.reopen(id, body), "Ticket reopened");
    }
}
