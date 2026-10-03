package com.transport.erp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Background jobs for Help & Support: auto-close resolved tickets after 7 days without a client answer. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupportTicketJobs {
    private final SupportTicketService supportTickets;

    @Scheduled(cron = "0 15 * * * *")
    public void autoClose() {
        try {
            int n = supportTickets.autoCloseResolved();
            if (n > 0) log.info("Support: auto-closed {} resolved ticket(s)", n);
        } catch (Exception e) {
            log.warn("Support auto-close failed: {}", e.getMessage());
        }
    }
}
