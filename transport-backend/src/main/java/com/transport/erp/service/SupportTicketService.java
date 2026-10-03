package com.transport.erp.service;

import com.transport.erp.exception.BusinessValidationException;
import com.transport.erp.model.*;
import com.transport.erp.repository.*;
import com.transport.erp.security.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Help & Support tickets (docs/SUPPORT_TICKETS.md).
 * <ul>
 *   <li>Client users report issues for their own company only; company admins see all of their company's tickets,
 *       other users only their own. The company always comes from the logged-in user, never from the request.</li>
 *   <li>Platform admins (SUPER_ADMIN) see and manage every client's tickets.</li>
 *   <li>Internal notes and internal events are removed on the server before anything is returned to a client.</li>
 *   <li>Ticket numbers carry the client's prefix: PKC-TKT-00001, AKS-TKT-00001 …</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    public static final List<String> PRIORITIES = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    public static final List<String> STATUSES = List.of("OPEN", "IN_PROGRESS", "WAITING_FOR_CLIENT", "RESOLVED", "CLOSED");
    private static final Set<String> CLIENT_ADMIN_ROLES = Set.of("COMPANY_ADMIN", "ADMIN");
    private static final int MAX_NEW_PER_HOUR = 10;

    private final SaaSSupportTicketRepository ticketRepository;
    private final SaaSSupportReplyRepository replyRepository;
    private final SaaSSupportTicketEventRepository eventRepository;
    private final CompanyRepository companyRepository;
    private final TenantAccessService tenantAccess;
    private final AuditService auditService;
    private final NamedParameterJdbcTemplate jdbc;
    private final SaaSSupportAttachmentRepository attachmentRepository;

    public static final int MAX_FILES_PER_TICKET = 20;
    public static final int AUTO_CLOSE_DAYS = 7;

    // ================================================================ client side

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, String userAgent) {
        AppUser user = clientUser();
        Long companyId = user.getCompanyId();
        String subject = text(body.get("subject"), 255);
        String description = text(body.get("description"), 20000);
        if (subject == null || description == null) {
            throw invalid("SUPPORT_REQUIRED", "Add a short title and describe the problem.");
        }
        String priority = upper(body.get("priority"), "MEDIUM");
        if (!PRIORITIES.contains(priority)) throw invalid("SUPPORT_PRIORITY", "Priority must be Low, Medium, High or Critical.");
        long recent = countRecent(user.getUsername());
        if (recent >= MAX_NEW_PER_HOUR) {
            throw invalid("SUPPORT_TOO_MANY", "You have reported " + recent + " issues in the last hour. Please add details to an existing ticket.");
        }

        LocalDateTime now = LocalDateTime.now();
        SaaSSupportTicket t = new SaaSSupportTicket();
        t.setCompanyId(companyId);
        t.setUsername(user.getUsername());
        t.setTicketNumber(nextNumber(companyId));
        t.setSubject(subject);
        t.setDescription(description);
        t.setPriority(priority);
        t.setStatus("OPEN");
        t.setModule(text(body.get("module"), 80));
        t.setScreen(text(body.get("screen"), 150));
        t.setPageUrl(text(body.get("pageUrl"), 500));
        t.setRecordType(text(body.get("recordType"), 50));
        t.setRecordId(longOrNull(body.get("recordId")));
        t.setRecordLabel(text(body.get("recordLabel"), 150));
        t.setAppVersion(text(body.get("appVersion"), 50));
        t.setUserAgent(text(userAgent, 500));
        t.setCreatedDate(now);
        t.setUpdatedDate(now);
        t.setLastClientActivity(now);
        t.setFirstResponseDue(now.plusHours(firstResponseHours(priority)));
        t.setResolveDue(now.plusHours(resolveHours(priority)));
        SaaSSupportTicket saved = ticketRepository.save(t);
        event(saved.getId(), user.getUsername(), "CREATED", null, priority, false);
        auditService.log(user.getUsername(), "SUPPORT_TICKET_CREATED", "saas_support_tickets", saved.getId(), null,
                saved.getTicketNumber() + " " + priority + ": " + subject);
        return summary(saved, false);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listForClient(String status, String search, int page, int size) {
        AppUser user = clientUser();
        boolean admin = isClientAdmin(user);
        String q = like(search);
        Pageable p = PageRequest.of(Math.max(page, 0), clamp(size), Sort.by(Sort.Direction.DESC, "updatedDate"));
        Page<SaaSSupportTicket> rows = ticketRepository.searchForClient(user.getCompanyId(), admin ? null : user.getUsername(),
                blankToNull(upper(status, null)), q, p);
        return rows.map(t -> summary(t, false));
    }

    @Transactional
    public Map<String, Object> getForClient(Long id) {
        SaaSSupportTicket t = ownTicket(id);
        markRead(t.getId(), tenantAccess.requireCurrentUser().getUsername());
        Map<String, Object> m = summary(t, false);
        m.put("description", t.getDescription());
        m.put("resolution", t.getResolution());
        m.put("timeline", timeline(t, false));
        m.put("attachments", attachments(t.getId(), false));
        m.put("autoCloseDays", AUTO_CLOSE_DAYS);
        return m;
    }

    @Transactional
    public Map<String, Object> clientReply(Long id, Map<String, Object> body) {
        SaaSSupportTicket t = ownTicket(id);
        String message = text(body.get("message"), 20000);
        if (message == null) throw invalid("SUPPORT_MESSAGE", "Type a message.");
        if ("CLOSED".equals(t.getStatus())) throw invalid("SUPPORT_CLOSED", "This ticket is closed. Please report a new issue.");
        if ("RESOLVED".equals(t.getStatus())) {
            throw invalid("SUPPORT_RESOLVED", "This ticket is resolved. Use \"Still not working\" to reopen it, or \"Issue fixed\" to close it.");
        }
        String user = tenantAccess.requireCurrentUser().getUsername();
        addReply(t, user, message, false, false);
        if ("WAITING_FOR_CLIENT".equals(t.getStatus())) changeStatus(t, "IN_PROGRESS", user, false);
        t.setLastClientActivity(LocalDateTime.now());
        touch(t);
        return getForClient(id);
    }

    /** Resolved → client confirms → Closed. */
    @Transactional
    public Map<String, Object> confirmFixed(Long id) {
        SaaSSupportTicket t = ownTicket(id);
        if (!"RESOLVED".equals(t.getStatus())) throw invalid("SUPPORT_NOT_RESOLVED", "Only a resolved ticket can be confirmed.");
        String user = tenantAccess.requireCurrentUser().getUsername();
        changeStatus(t, "CLOSED", user, false);
        t.setClosedAt(LocalDateTime.now());
        t.setLastClientActivity(LocalDateTime.now());
        touch(t);
        return getForClient(id);
    }

    /** Resolved → client says it still happens → back to In Progress (reopened). */
    @Transactional
    public Map<String, Object> reopen(Long id, Map<String, Object> body) {
        SaaSSupportTicket t = ownTicket(id);
        if (!"RESOLVED".equals(t.getStatus())) throw invalid("SUPPORT_NOT_RESOLVED", "Only a resolved ticket can be reopened.");
        String message = text(body == null ? null : body.get("message"), 20000);
        if (message == null) throw invalid("SUPPORT_MESSAGE", "Tell support what is still not working.");
        String user = tenantAccess.requireCurrentUser().getUsername();
        addReply(t, user, message, false, false);
        changeStatus(t, "IN_PROGRESS", user, false);
        event(t.getId(), user, "REOPENED", "RESOLVED", "IN_PROGRESS", false);
        t.setReopenCount((t.getReopenCount() == null ? 0 : t.getReopenCount()) + 1);
        t.setResolvedAt(null);
        t.setResolvedBy(null);
        t.setLastClientActivity(LocalDateTime.now());
        t.setResolveDue(LocalDateTime.now().plusHours(resolveHours(t.getPriority())));
        touch(t);
        return getForClient(id);
    }

    // ================================================================ platform admin side

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listForAdmin(String status, String priority, Long companyId, String module, String assignedTo,
                                                   String search, boolean overdueOnly, int page, int size) {
        requirePlatformAdmin();
        Pageable p = PageRequest.of(Math.max(page, 0), clamp(size), Sort.by(Sort.Direction.DESC, "updatedDate"));
        Page<SaaSSupportTicket> rows = ticketRepository.searchForAdmin(blankToNull(upper(status, null)), blankToNull(upper(priority, null)),
                companyId, blankToNull(module), blankToNull(assignedTo), like(search), overdueOnly, LocalDateTime.now(), p);
        return rows.map(t -> summary(t, true));
    }

    @Transactional
    public Map<String, Object> getForAdmin(Long id) {
        String admin = requirePlatformAdmin();
        SaaSSupportTicket t = find(id);
        markRead(t.getId(), admin);
        Map<String, Object> m = summary(t, true);
        m.put("description", t.getDescription());
        m.put("resolution", t.getResolution());
        m.put("timeline", timeline(t, true));
        m.put("attachments", attachments(t.getId(), true));
        return m;
    }

    /** Reply to the client, or an internal note (internal=true) the client never sees. Optionally set a status. */
    @Transactional
    public Map<String, Object> adminReply(Long id, Map<String, Object> body) {
        String admin = requirePlatformAdmin();
        SaaSSupportTicket t = find(id);
        String message = text(body.get("message"), 20000);
        if (message == null) throw invalid("SUPPORT_MESSAGE", "Type a message.");
        boolean internal = Boolean.parseBoolean(String.valueOf(body.getOrDefault("internal", false)));
        if ("CLOSED".equals(t.getStatus()) && !internal) throw invalid("SUPPORT_CLOSED", "The ticket is closed; only internal notes can be added.");
        addReply(t, admin, message, true, internal);
        if (!internal) {
            LocalDateTime now = LocalDateTime.now();
            t.setLastAdminActivity(now);
            if (t.getFirstRespondedAt() == null) t.setFirstRespondedAt(now);
            String next = upper(body.get("status"), null);
            if (next != null && !next.isBlank() && !next.equals(t.getStatus())) {
                applyAdminStatus(t, next, admin, null);
            } else if ("OPEN".equals(t.getStatus())) {
                changeStatus(t, "IN_PROGRESS", admin, false);
            }
        }
        touch(t);
        return getForAdmin(id);
    }

    @Transactional
    public Map<String, Object> setStatus(Long id, String status, String resolution) {
        String admin = requirePlatformAdmin();
        SaaSSupportTicket t = find(id);
        applyAdminStatus(t, upper(status, ""), admin, resolution);
        touch(t);
        return getForAdmin(id);
    }

    @Transactional
    public Map<String, Object> setPriority(Long id, String priority) {
        String admin = requirePlatformAdmin();
        SaaSSupportTicket t = find(id);
        String p = upper(priority, "");
        if (!PRIORITIES.contains(p)) throw invalid("SUPPORT_PRIORITY", "Priority must be Low, Medium, High or Critical.");
        if (!p.equals(t.getPriority())) {
            event(t.getId(), admin, "PRIORITY", t.getPriority(), p, false);
            t.setPriority(p);
            t.setResolveDue(t.getCreatedDate().plusHours(resolveHours(p)));
            if (t.getFirstRespondedAt() == null) t.setFirstResponseDue(t.getCreatedDate().plusHours(firstResponseHours(p)));
            touch(t);
            auditService.log(admin, "SUPPORT_TICKET_PRIORITY", "saas_support_tickets", t.getId(), null, t.getTicketNumber() + " → " + p);
        }
        return getForAdmin(id);
    }

    /** Assign to a platform user (or unassign with a blank name). Internal: the client does not see who is assigned. */
    @Transactional
    public Map<String, Object> assign(Long id, String username) {
        String admin = requirePlatformAdmin();
        SaaSSupportTicket t = find(id);
        String to = username == null || username.isBlank() ? null : username.trim();
        if (to != null && assignees().stream().noneMatch(a -> to.equals(a.get("username")))) {
            throw invalid("SUPPORT_ASSIGNEE", "Assign the ticket to a platform admin user.");
        }
        if (!Objects.equals(to, t.getAssignedTo())) {
            event(t.getId(), admin, "ASSIGNED", t.getAssignedTo(), to, true);
            if (to != null && !to.equals(admin)) {
                jdbc.update("DELETE FROM saas_support_ticket_reads WHERE ticket_id = :id AND username = :u",
                        new MapSqlParameterSource("id", t.getId()).addValue("u", to));
            }
            t.setAssignedTo(to);
            touch(t);
            auditService.log(admin, "SUPPORT_TICKET_ASSIGNED", "saas_support_tickets", t.getId(), null, t.getTicketNumber() + " → " + to);
        }
        return getForAdmin(id);
    }

    /** Platform admin users a ticket can be assigned to. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> assignees() {
        return jdbc.queryForList("""
                SELECT DISTINCT u.username, COALESCE(NULLIF(u.name, ''), u.username) AS name
                  FROM app_users u JOIN user_roles ur ON ur.user_id = u.id JOIN app_roles r ON r.id = ur.role_id
                 WHERE r.code = 'SUPER_ADMIN' AND u.is_deleted = false AND COALESCE(u.status, 'ACTIVE') = 'ACTIVE'
                 ORDER BY 2""", new MapSqlParameterSource());
    }

    /** Support dashboard figures. */
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        requirePlatformAdmin();
        MapSqlParameterSource p = new MapSqlParameterSource("now", LocalDateTime.now());
        Map<String, Object> m = new LinkedHashMap<>(jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE status NOT IN ('RESOLVED','CLOSED')) AS open_total,
                       COUNT(*) FILTER (WHERE status = 'OPEN' AND assigned_to IS NULL) AS new_unassigned,
                       COUNT(*) FILTER (WHERE status NOT IN ('RESOLVED','CLOSED') AND priority IN ('HIGH','CRITICAL')) AS high_critical,
                       COUNT(*) FILTER (WHERE status NOT IN ('RESOLVED','CLOSED') AND priority = 'CRITICAL') AS critical,
                       COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') AS in_progress,
                       COUNT(*) FILTER (WHERE status = 'WAITING_FOR_CLIENT') AS waiting_for_client,
                       COUNT(*) FILTER (WHERE status = 'RESOLVED') AS resolved,
                       COUNT(*) FILTER (WHERE status = 'CLOSED') AS closed,
                       COUNT(*) FILTER (WHERE status IN ('OPEN','IN_PROGRESS') AND resolve_due < :now) AS overdue
                  FROM saas_support_tickets""", p));
        m.put("byClient", jdbc.queryForList("""
                SELECT c.name AS client, COUNT(*) AS open_count
                  FROM saas_support_tickets t JOIN companies c ON c.id = t.company_id
                 WHERE t.status NOT IN ('RESOLVED','CLOSED') GROUP BY c.name ORDER BY 2 DESC, 1 LIMIT 10""", p));
        m.put("byModule", jdbc.queryForList("""
                SELECT COALESCE(NULLIF(module, ''), 'Other') AS module, COUNT(*) AS open_count
                  FROM saas_support_tickets WHERE status NOT IN ('RESOLVED','CLOSED') GROUP BY 1 ORDER BY 2 DESC, 1 LIMIT 12""", p));
        return m;
    }

    // ================================================================ attachments (stored in the database)

    /** Client adds a file (screenshot / image / PDF) to their ticket; always visible to both sides. */
    @Transactional
    public Map<String, Object> clientAttach(Long id, org.springframework.web.multipart.MultipartFile file) {
        SaaSSupportTicket t = ownTicket(id);
        if ("CLOSED".equals(t.getStatus())) throw invalid("SUPPORT_CLOSED", "This ticket is closed. Please report a new issue.");
        String user = tenantAccess.requireCurrentUser().getUsername();
        storeFile(t, file, false, false, user);
        t.setLastClientActivity(LocalDateTime.now());
        touch(t);
        return getForClient(id);
    }

    /** Support adds a file; internal=true keeps it from the client (e.g. logs, internal screenshots). */
    @Transactional
    public Map<String, Object> adminAttach(Long id, org.springframework.web.multipart.MultipartFile file, boolean internal) {
        String admin = requirePlatformAdmin();
        SaaSSupportTicket t = find(id);
        storeFile(t, file, internal, true, admin);
        if (!internal) t.setLastAdminActivity(LocalDateTime.now());
        touch(t);
        return getForAdmin(id);
    }

    /** A file of a ticket the user may open; clients never get internal files. */
    @Transactional(readOnly = true)
    public SaaSSupportAttachment download(Long ticketId, Long attachmentId, boolean asPlatformAdmin) {
        if (asPlatformAdmin) requirePlatformAdmin(); else ownTicket(ticketId);
        SaaSSupportAttachment a = attachmentRepository.findById(attachmentId)
                .filter(x -> x.getTicketId().equals(ticketId))
                .orElseThrow(() -> new IllegalArgumentException("File not found."));
        if (!asPlatformAdmin && Boolean.TRUE.equals(a.getIsInternal())) throw new AccessDeniedException("File not available");
        a.getData();   // load the bytes inside the transaction
        return a;
    }

    private List<Map<String, Object>> attachments(Long ticketId, boolean admin) {
        List<Map<String, Object>> rows = attachmentRepository.listMeta(ticketId);
        if (admin) return rows;
        return rows.stream().filter(r -> !Boolean.TRUE.equals(r.get("isInternal"))).toList();
    }

    private void storeFile(SaaSSupportTicket t, org.springframework.web.multipart.MultipartFile file, boolean internal,
                           boolean fromSupport, String user) {
        if (file == null || file.isEmpty()) throw invalid("SUPPORT_FILE_EMPTY", "Choose a file to attach.");
        if (attachmentRepository.countByTicketId(t.getId()) >= MAX_FILES_PER_TICKET) {
            throw invalid("SUPPORT_FILE_LIMIT", "A ticket can have at most " + MAX_FILES_PER_TICKET + " files.");
        }
        byte[] bytes;
        try { bytes = file.getBytes(); } catch (java.io.IOException e) { throw invalid("SUPPORT_FILE_READ", "The file could not be read."); }
        String type = detectType(bytes);
        if (type == null) throw invalid("SUPPORT_FILE_TYPE", "Only screenshots / images (PNG, JPG, WEBP) and PDF files can be attached.");
        long max = "application/pdf".equals(type) ? 10L * 1024 * 1024 : 5L * 1024 * 1024;
        if (bytes.length > max) throw invalid("SUPPORT_FILE_SIZE", "Files can be up to " + (max / 1024 / 1024) + " MB (" + (bytes.length / 1024) + " KB given).");
        String name = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        if (name.isBlank()) name = "file";
        if (name.length() > 200) name = name.substring(name.length() - 200);
        SaaSSupportAttachment a = new SaaSSupportAttachment();
        a.setTicketId(t.getId());
        a.setFileName(name);
        a.setContentType(type);
        a.setSizeBytes((long) bytes.length);
        a.setData(bytes);
        a.setIsInternal(internal);
        a.setFromSupport(fromSupport);
        a.setUploadedBy(user);
        a.setCreatedDate(LocalDateTime.now());
        attachmentRepository.save(a);
        auditService.log(user, internal ? "SUPPORT_TICKET_FILE_INTERNAL" : "SUPPORT_TICKET_FILE", "saas_support_tickets", t.getId(), null,
                t.getTicketNumber() + " " + name + " (" + bytes.length / 1024 + " KB)");
    }

    /** Real file type from the first bytes (the browser-sent type / extension can lie). */
    static String detectType(byte[] b) {
        if (b == null || b.length < 12) return null;
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        if (b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return "application/pdf";
        return null;
    }

    // ================================================================ auto-close

    /**
     * Resolved tickets the client has not answered for {@value #AUTO_CLOSE_DAYS} days are closed (by SYSTEM).
     * Runs hourly (SupportTicketJobs); platform admins can also run it now.
     */
    @Transactional
    public int autoCloseResolved() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(AUTO_CLOSE_DAYS);
        List<Long> ids = jdbc.queryForList("""
                SELECT id FROM saas_support_tickets
                 WHERE status = 'RESOLVED' AND resolved_at < :cutoff
                   AND (last_client_activity IS NULL OR last_client_activity < resolved_at)""",
                new MapSqlParameterSource("cutoff", cutoff), Long.class);
        for (Long id : ids) {
            SaaSSupportTicket t = find(id);
            changeStatus(t, "CLOSED", "SYSTEM", false);
            event(t.getId(), "SYSTEM", "AUTO_CLOSED", "RESOLVED", "CLOSED", false);
            t.setClosedAt(LocalDateTime.now());
            touch(t);
            auditService.log("SYSTEM", "SUPPORT_TICKET_AUTO_CLOSED", "saas_support_tickets", t.getId(), null, t.getTicketNumber());
        }
        return ids.size();
    }

    @Transactional
    public int runAutoCloseNow() {
        requirePlatformAdmin();
        return autoCloseResolved();
    }

    // ================================================================ notifications (bell)

    /** Client bell: tickets with a support reply / status change the user has not opened yet. */
    @Transactional(readOnly = true)
    public Map<String, Object> clientNotifications() {
        AppUser user = clientUser();
        String scope = isClientAdmin(user) ? "" : " AND t.username = :u";
        String where = """
                 FROM saas_support_tickets t
                 LEFT JOIN saas_support_ticket_reads r ON r.ticket_id = t.id AND r.username = :u
                WHERE t.company_id = :cid""" + scope + """
                  AND t.last_admin_activity IS NOT NULL
                  AND t.last_admin_activity > COALESCE(r.last_read_at, t.created_date)""";
        MapSqlParameterSource p = new MapSqlParameterSource("u", user.getUsername()).addValue("cid", user.getCompanyId());
        Long count = jdbc.queryForObject("SELECT COUNT(*)" + where, p, Long.class);
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT t.id, t.ticket_number AS "ticketNumber", t.subject, t.status, t.priority,
                       t.last_admin_activity AS at,
                       CASE WHEN t.status = 'RESOLVED' THEN 'RESOLVED'
                            WHEN t.status = 'WAITING_FOR_CLIENT' THEN 'WAITING_FOR_YOU' ELSE 'SUPPORT_REPLIED' END AS reason""" + where
                + " ORDER BY t.last_admin_activity DESC LIMIT 10", p);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("unread", count == null ? 0 : count);
        m.put("items", items);
        return m;
    }

    /**
     * Platform admin bell: new tickets not opened yet, client replies since last opened, tickets assigned to me.
     * Tickets assigned to another admin notify that admin only. Critical first. Plus the open critical count (banner).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> adminNotifications() {
        String admin = requirePlatformAdmin();
        String where = """
                 FROM saas_support_tickets t
                 JOIN companies c ON c.id = t.company_id
                 LEFT JOIN saas_support_ticket_reads r ON r.ticket_id = t.id AND r.username = :u
                WHERE t.status <> 'CLOSED'
                  AND (t.assigned_to IS NULL OR t.assigned_to = :u)
                  AND (r.last_read_at IS NULL
                       OR (t.last_client_activity IS NOT NULL AND t.last_client_activity > r.last_read_at))""";
        MapSqlParameterSource p = new MapSqlParameterSource("u", admin);
        Long count = jdbc.queryForObject("SELECT COUNT(*)" + where, p, Long.class);
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT t.id, t.ticket_number AS "ticketNumber", t.subject, t.status, t.priority, c.name AS client,
                       GREATEST(t.created_date, COALESCE(t.last_client_activity, t.created_date)) AS at,
                       CASE WHEN r.last_read_at IS NULL AND t.assigned_to = :u THEN 'ASSIGNED_TO_YOU'
                            WHEN r.last_read_at IS NULL THEN 'NEW_TICKET' ELSE 'CLIENT_REPLIED' END AS reason""" + where + """
                 ORDER BY CASE t.priority WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 ELSE 2 END, 7 DESC LIMIT 10""", p);
        Long critical = jdbc.queryForObject(
                "SELECT COUNT(*) FROM saas_support_tickets WHERE priority = 'CRITICAL' AND status NOT IN ('RESOLVED','CLOSED')",
                new MapSqlParameterSource(), Long.class);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("unread", count == null ? 0 : count);
        m.put("items", items);
        m.put("criticalOpen", critical == null ? 0 : critical);
        return m;
    }

    private void markRead(Long ticketId, String username) {
        jdbc.update("""
                INSERT INTO saas_support_ticket_reads (ticket_id, username, last_read_at) VALUES (:id, :u, :now)
                ON CONFLICT (ticket_id, username) DO UPDATE SET last_read_at = EXCLUDED.last_read_at""",
                new MapSqlParameterSource("id", ticketId).addValue("u", username).addValue("now", LocalDateTime.now()));
    }

    // ================================================================ rules

    private void applyAdminStatus(SaaSSupportTicket t, String next, String admin, String resolution) {
        if (!STATUSES.contains(next)) throw invalid("SUPPORT_STATUS", "Unknown status " + next + ".");
        String cur = t.getStatus();
        if (next.equals(cur)) return;
        if ("CLOSED".equals(cur)) throw invalid("SUPPORT_CLOSED", "A closed ticket cannot be changed. The client can report a new issue.");
        LocalDateTime now = LocalDateTime.now();
        if ("RESOLVED".equals(next)) {
            String res = text(resolution, 20000);
            if (res == null) throw invalid("SUPPORT_RESOLUTION", "Describe how the issue was resolved.");
            t.setResolution(res);
            t.setResolvedAt(now);
            t.setResolvedBy(admin);
        }
        if ("CLOSED".equals(next)) t.setClosedAt(now);
        if (t.getFirstRespondedAt() == null && !"OPEN".equals(next)) t.setFirstRespondedAt(now);
        changeStatus(t, next, admin, false);
        t.setLastAdminActivity(now);
        auditService.log(admin, "SUPPORT_TICKET_STATUS", "saas_support_tickets", t.getId(), null, t.getTicketNumber() + " " + cur + " → " + next);
    }

    private void changeStatus(SaaSSupportTicket t, String next, String user, boolean internal) {
        String cur = t.getStatus();
        if (Objects.equals(cur, next)) return;
        t.setStatus(next);
        event(t.getId(), user, "STATUS", cur, next, internal);
    }

    private void addReply(SaaSSupportTicket t, String user, String message, boolean admin, boolean internal) {
        SaaSSupportReply r = new SaaSSupportReply();
        r.setTicket(t);
        r.setUsername(user);
        r.setMessage(message);
        r.setIsAdminReply(admin);
        r.setIsInternal(internal);
        r.setCreatedDate(LocalDateTime.now());
        replyRepository.save(r);
        auditService.log(user, internal ? "SUPPORT_TICKET_NOTE" : "SUPPORT_TICKET_REPLY", "saas_support_tickets", t.getId(), null, t.getTicketNumber());
    }

    private void event(Long ticketId, String user, String action, String oldValue, String newValue, boolean internal) {
        SaaSSupportTicketEvent e = new SaaSSupportTicketEvent();
        e.setTicketId(ticketId);
        e.setUsername(user);
        e.setAction(action);
        e.setOldValue(oldValue);
        e.setNewValue(newValue);
        e.setIsInternal(internal);
        e.setCreatedDate(LocalDateTime.now());
        eventRepository.save(e);
    }

    private void touch(SaaSSupportTicket t) {
        t.setUpdatedDate(LocalDateTime.now());
        ticketRepository.save(t);
    }

    /** Replies + events in time order; for clients, internal notes and internal events are removed here. */
    private List<Map<String, Object>> timeline(SaaSSupportTicket t, boolean admin) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SaaSSupportReply r : replyRepository.findByTicketIdOrderByCreatedDateAsc(t.getId())) {
            boolean internal = Boolean.TRUE.equals(r.getIsInternal());
            if (internal && !admin) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kind", internal ? "NOTE" : "MESSAGE");
            m.put("by", r.getUsername());
            m.put("fromSupport", Boolean.TRUE.equals(r.getIsAdminReply()));
            m.put("internal", internal);
            m.put("message", r.getMessage());
            m.put("at", r.getCreatedDate());
            out.add(m);
        }
        for (SaaSSupportTicketEvent e : eventRepository.findByTicketIdOrderByCreatedDateAsc(t.getId())) {
            boolean internal = Boolean.TRUE.equals(e.getIsInternal());
            if (internal && !admin) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kind", "EVENT");
            m.put("action", e.getAction());
            m.put("by", e.getUsername());
            m.put("from", e.getOldValue());
            m.put("to", e.getNewValue());
            m.put("internal", internal);
            m.put("at", e.getCreatedDate());
            out.add(m);
        }
        out.sort(Comparator.comparing(m -> (LocalDateTime) m.get("at")));
        return out;
    }

    private Map<String, Object> summary(SaaSSupportTicket t, boolean admin) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("ticketNumber", t.getTicketNumber());
        m.put("subject", t.getSubject());
        m.put("module", t.getModule());
        m.put("screen", t.getScreen());
        m.put("priority", t.getPriority());
        m.put("status", t.getStatus());
        m.put("reportedBy", t.getUsername());
        m.put("createdDate", t.getCreatedDate());
        m.put("updatedDate", t.getUpdatedDate());
        m.put("recordType", t.getRecordType());
        m.put("recordId", t.getRecordId());
        m.put("recordLabel", t.getRecordLabel());
        m.put("resolvedAt", t.getResolvedAt());
        m.put("closedAt", t.getClosedAt());
        m.put("reopenCount", t.getReopenCount());
        boolean supportNewer = t.getLastAdminActivity() != null
                && (t.getLastClientActivity() == null || t.getLastAdminActivity().isAfter(t.getLastClientActivity()));
        m.put("supportReplied", supportNewer);
        if (admin) {
            m.put("companyId", t.getCompanyId());
            m.put("clientName", companyRepository.findById(t.getCompanyId()).map(Company::getName).orElse(null));
            m.put("assignedTo", t.getAssignedTo());
            m.put("pageUrl", t.getPageUrl());
            m.put("userAgent", t.getUserAgent());
            m.put("appVersion", t.getAppVersion());
            m.put("firstResponseDue", t.getFirstResponseDue());
            m.put("firstRespondedAt", t.getFirstRespondedAt());
            m.put("resolveDue", t.getResolveDue());
            m.put("resolvedBy", t.getResolvedBy());
            m.put("overdue", ("OPEN".equals(t.getStatus()) || "IN_PROGRESS".equals(t.getStatus()))
                    && t.getResolveDue() != null && t.getResolveDue().isBefore(LocalDateTime.now()));
            boolean clientNewer = t.getLastClientActivity() != null
                    && (t.getLastAdminActivity() == null || t.getLastClientActivity().isAfter(t.getLastAdminActivity()));
            m.put("clientReplied", clientNewer);
        }
        return m;
    }

    /**
     * Next number for a client: prefix from the company code (PKC001 → PKC), fixed at the first ticket and unique
     * across clients (if another client already uses PKC, the full code is used). Row lock keeps numbers unique.
     */
    private String nextNumber(Long companyId) {
        MapSqlParameterSource p = new MapSqlParameterSource("cid", companyId);
        List<String> existing = jdbc.queryForList("SELECT prefix FROM saas_support_ticket_counters WHERE company_id = :cid", p, String.class);
        if (existing.isEmpty()) {
            Company c = companyRepository.findById(companyId).orElseThrow(() -> invalid("SUPPORT_COMPANY", "Company not found."));
            String code = (c.getCode() == null ? "" : c.getCode()).toUpperCase().replaceAll("[^A-Z0-9]", "");
            if (code.isEmpty()) code = "C" + companyId;
            String letters = code.replaceAll("\\d+$", "");
            List<String> candidates = new ArrayList<>();
            if (letters.length() >= 2) candidates.add(letters);
            candidates.add(code);
            candidates.add(code + companyId);
            for (String prefix : candidates) {
                int added = jdbc.update("""
                        INSERT INTO saas_support_ticket_counters (company_id, prefix, last_number)
                        SELECT :cid, :prefix, 0
                         WHERE NOT EXISTS (SELECT 1 FROM saas_support_ticket_counters WHERE prefix = :prefix OR company_id = :cid)""",
                        new MapSqlParameterSource("cid", companyId).addValue("prefix", prefix));
                if (added == 1 || !jdbc.queryForList("SELECT prefix FROM saas_support_ticket_counters WHERE company_id = :cid", p, String.class).isEmpty()) break;
            }
        }
        Map<String, Object> row = jdbc.queryForMap("""
                UPDATE saas_support_ticket_counters SET last_number = last_number + 1
                 WHERE company_id = :cid RETURNING prefix, last_number""", p);
        return row.get("prefix") + "-TKT-" + String.format("%05d", ((Number) row.get("last_number")).longValue());
    }

    private long countRecent(String username) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM saas_support_tickets WHERE username = :u AND created_date > :since",
                new MapSqlParameterSource("u", username).addValue("since", LocalDateTime.now().minusHours(1)), Long.class);
        return n == null ? 0 : n;
    }

    /** Target response / resolution hours by priority (Critical 1h / 4h, High 4h / 1 day, Medium 8h / 3 days, Low 1 / 5 days). */
    static long firstResponseHours(String p) {
        return switch (p == null ? "" : p) { case "CRITICAL" -> 1; case "HIGH" -> 4; case "LOW" -> 24; default -> 8; };
    }
    static long resolveHours(String p) {
        return switch (p == null ? "" : p) { case "CRITICAL" -> 4; case "HIGH" -> 24; case "LOW" -> 120; default -> 72; };
    }

    // ================================================================ access

    /** A company user (not a driver-only login, not the platform admin acting as a client). */
    private AppUser clientUser() {
        AppUser user = tenantAccess.requireCurrentUser();
        if (user.getCompanyId() == null) throw new AccessDeniedException("No company for this user");
        Set<String> roles = roles(user);
        if (roles.isEmpty() || roles.stream().allMatch("DRIVER"::equals)) {
            throw new AccessDeniedException("Driver logins report issues through their manager");
        }
        return user;
    }

    /** The ticket, if it belongs to the user's company (and to the user, unless they are a company admin). */
    private SaaSSupportTicket ownTicket(Long id) {
        AppUser user = clientUser();
        SaaSSupportTicket t = find(id);
        if (!user.getCompanyId().equals(t.getCompanyId())) throw new AccessDeniedException("Access denied to another company's ticket");
        if (!isClientAdmin(user) && !user.getUsername().equals(t.getUsername())) {
            throw new AccessDeniedException("Only your own tickets (or a company admin) can open this ticket");
        }
        return t;
    }

    private boolean isClientAdmin(AppUser user) {
        return roles(user).stream().anyMatch(CLIENT_ADMIN_ROLES::contains) || tenantAccess.isSuperAdmin(user);
    }

    private String requirePlatformAdmin() {
        AppUser user = tenantAccess.requireCurrentUser();
        if (!tenantAccess.isSuperAdmin(user)) throw new AccessDeniedException("Platform admin only");
        return user.getUsername();
    }

    private static Set<String> roles(AppUser user) {
        Set<String> out = new HashSet<>();
        if (user.getRoles() != null) user.getRoles().forEach(r -> out.add(r.getCode()));
        return out;
    }

    private SaaSSupportTicket find(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Ticket not found."));
    }

    // ================================================================ small helpers

    private static BusinessValidationException invalid(String code, String message) {
        return new BusinessValidationException("Support Ticket", code, message, "Check the details and try again.");
    }
    private static String text(Object v, int max) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
    private static String upper(Object v, String dflt) {
        String s = v == null ? null : String.valueOf(v).trim().toUpperCase();
        return s == null || s.isEmpty() ? dflt : s;
    }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }
    private static String like(String s) { return s == null || s.isBlank() ? "" : s.trim().toLowerCase(); }
    private static int clamp(int size) { return Math.max(1, Math.min(size <= 0 ? 20 : size, 100)); }
    private static Long longOrNull(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        try { return Long.valueOf(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
    }
}
