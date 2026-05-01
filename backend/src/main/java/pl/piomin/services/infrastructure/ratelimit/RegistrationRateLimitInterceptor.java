package pl.piomin.services.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RegistrationRateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L; // 15 minutes

    private final ConcurrentHashMap<String, Deque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {

        String ip = resolveClientIp(request);
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MILLIS;

        Deque<Long> timestamps = requestTimestamps.computeIfAbsent(ip, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Evict timestamps outside the current window
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_REQUESTS) {
                response.setStatus(429);
                response.setHeader("Retry-After", "900");
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many registration attempts. Try again later.\"}");
                return false;
            }

            timestamps.addLast(now);
        }

        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain a comma-separated list; take the first entry
            int commaIndex = forwarded.indexOf(',');
            return commaIndex >= 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
