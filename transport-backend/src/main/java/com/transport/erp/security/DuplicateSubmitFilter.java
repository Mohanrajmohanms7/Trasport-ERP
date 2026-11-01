package com.transport.erp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Duplicate-submission guard (idempotency by content). The same user sending the identical POST (same URL, same JSON
 * body) again within {@link #WINDOW_MS} — a double click, a network retry — does not run it again: it waits for the
 * first request and receives the first request's response (header X-Duplicate-Submission: replayed). So a second
 * voucher, booking, receipt … is never created and document numbering is untouched. Only successful responses are
 * remembered; a failed request can be corrected and sent again at once. Login, uploads and exports are not touched.
 * A client that deliberately sends identical requests can opt out with header X-Idempotency: off.
 */
public class DuplicateSubmitFilter extends OncePerRequestFilter {

    static final long WINDOW_MS = 5_000;
    private static final int MAX_BODY = 1_000_000;

    private record Captured(int status, String contentType, byte[] body) {}
    private record Entry(long at, CompletableFuture<Captured> done) {}

    private final Map<String, Entry> recent = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        if (!"POST".equalsIgnoreCase(req.getMethod())) return true;
        String uri = req.getRequestURI();
        String ct = req.getContentType();
        return uri.startsWith("/api/v1/auth") || uri.contains("/export") || uri.contains("/upload") || uri.contains("/bulk-import")
                || uri.contains("/attachments") || uri.contains("/files") || ct == null || !ct.toLowerCase().contains("json")
                || "off".equalsIgnoreCase(req.getHeader("X-Idempotency"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) { chain.doFilter(req, res); return; }
        byte[] body = req.getInputStream().readNBytes(MAX_BODY + 1);
        if (body.length > MAX_BODY) { chain.doFilter(new CachedBody(req, body), res); return; }
        String key = hash(auth.getName() + "|" + req.getRequestURI() + "?" + (req.getQueryString() == null ? "" : req.getQueryString()), body);
        long now = System.currentTimeMillis();
        if (recent.size() > 5_000) recent.entrySet().removeIf(e -> now - e.getValue().at() > WINDOW_MS);

        Entry mine = new Entry(now, new CompletableFuture<>());
        Entry prev = recent.compute(key, (k, old) -> old != null && now - old.at() < WINDOW_MS ? old : mine);
        if (prev != mine) {
            Captured first;
            try { first = prev.done().get(30, TimeUnit.SECONDS); } catch (Exception e) { first = null; }
            if (first != null) {   // the identical request already succeeded: answer with its result, do not run it again
                res.setStatus(first.status());
                if (first.contentType() != null) res.setContentType(first.contentType());
                res.setHeader("X-Duplicate-Submission", "replayed");
                res.getOutputStream().write(first.body());
                return;
            }
            chain.doFilter(new CachedBody(req, body), res);   // the first one failed: this attempt runs normally
            return;
        }
        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(res);
        Captured result = null;
        try {
            chain.doFilter(new CachedBody(req, body), wrapped);
            byte[] out = wrapped.getContentAsByteArray();
            int st = wrapped.getStatus();
            boolean ok = st >= 200 && st < 300 && !new String(out, 0, Math.min(out.length, 200), StandardCharsets.UTF_8).contains("\"success\":false");
            if (ok) result = new Captured(st, wrapped.getContentType(), out);
        } finally {
            mine.done().complete(result);
            if (result == null) recent.remove(key, mine);
            wrapped.copyBodyToResponse();
        }
    }

    private static String hash(String prefix, byte[] body) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(prefix.getBytes(StandardCharsets.UTF_8));
            md.update(body);
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Request whose body was already read (so it can be hashed) and is served again to the controller. */
    private static final class CachedBody extends HttpServletRequestWrapper {
        private final byte[] body;
        CachedBody(HttpServletRequest r, byte[] body) { super(r); this.body = body; }
        @Override public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return in.read(); }
                @Override public boolean isFinished() { return in.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener l) { }
            };
        }
        @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)); }
        @Override public int getContentLength() { return body.length; }
        @Override public long getContentLengthLong() { return body.length; }
    }
}
