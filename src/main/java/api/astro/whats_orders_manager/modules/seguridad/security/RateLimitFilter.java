package api.astro.whats_orders_manager.modules.seguridad.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limits authentication endpoints by client IP using Bucket4j token-bucket algorithm.
 * Capacity: 20 requests per 60-second window (per IP).
 * Scoped exclusively to /auth/login and /api/auth/login — all other paths pass through untouched.
 * Returns HTTP 429 with a JSON error body when the bucket is exhausted.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 20;
    private static final Duration REFILL_PERIOD = Duration.ofSeconds(60);
    private static final String ERROR_BODY =
            "{\"error\": \"Too many requests. Please try again later.\"}";

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/auth/login",
            "/api/auth/login"
    );

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!RATE_LIMITED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(clientIp, ignored -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP {} on path {}", clientIp, path);
            rejectWithTooManyRequests(response);
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(CAPACITY)
                .refillGreedy(CAPACITY, REFILL_PERIOD)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private void rejectWithTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(ERROR_BODY);
    }
}
