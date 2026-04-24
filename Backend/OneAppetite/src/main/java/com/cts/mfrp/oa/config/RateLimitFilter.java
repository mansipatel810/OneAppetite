package com.cts.mfrp.oa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final long windowMillis;

    private static final class Window {
        volatile long startMillis;
        final AtomicInteger count = new AtomicInteger(0);
    }

    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.auth.rate-limit.max-requests:5}") int maxRequests,
            @Value("${app.auth.rate-limit.window-seconds:60}") int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = clientKey(request);
        long now = System.currentTimeMillis();

        Window win = buckets.computeIfAbsent(key, k -> {
            Window w = new Window();
            w.startMillis = now;
            return w;
        });

        synchronized (win) {
            if (now - win.startMillis >= windowMillis) {
                win.startMillis = now;
                win.count.set(0);
            }
            int used = win.count.incrementAndGet();
            if (used > maxRequests) {
                long retryAfterSec = Math.max(1, (win.startMillis + windowMillis - now + 999) / 1000);
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(retryAfterSec));
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Try again in " + retryAfterSec + "s.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
