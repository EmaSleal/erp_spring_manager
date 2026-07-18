package api.astro.whats_orders_manager.modules.seguridad.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitFilter.
 * Tests are in the same package as the filter to access the protected doFilterInternal.
 * Verifies per-IP rate limiting on /auth/login and /api/auth/login;
 * non-auth paths pass through untouched.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter — per-IP rate limiting on auth endpoints")
class RateLimitFilterTest {

    private RateLimitFilter filter;

    @Mock
    private FilterChain filterChain;

    private static final String FIXED_IP = "192.168.1.100";

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    // ── /auth/login ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("First request to /auth/login from a new IP passes through filter chain")
    void authLogin_firstRequest_passesThroughFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr(FIXED_IP);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("First 20 requests to /auth/login from same IP all pass through")
    void authLogin_requestsWithinCapacity_allPassThrough() throws Exception {
        String ip = "10.1.0.1";

        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr(ip);
            MockHttpServletResponse res = new MockHttpServletResponse();

            filter.doFilterInternal(req, res, filterChain);

            assertThat(res.getStatus())
                    .as("Request #%d should not be rate-limited", i + 1)
                    .isNotEqualTo(429);
        }

        verify(filterChain, times(20)).doFilter(any(), any());
    }

    @Test
    @DisplayName("21st request to /auth/login from same IP returns HTTP 429")
    void authLogin_requestBeyondCapacity_returns429() throws Exception {
        String ip = "10.0.0.1";

        // Exhaust the bucket (20 tokens)
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr(ip);
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        // 21st request — must be rejected
        MockHttpServletRequest req21 = new MockHttpServletRequest("POST", "/auth/login");
        req21.setRemoteAddr(ip);
        MockHttpServletResponse res21 = new MockHttpServletResponse();

        filter.doFilterInternal(req21, res21, filterChain);

        assertThat(res21.getStatus()).isEqualTo(429);
        // Filter chain was called only for the 20 allowed requests
        verify(filterChain, times(20)).doFilter(any(), any());
    }

    @Test
    @DisplayName("429 response for /auth/login has JSON Content-Type and error body")
    void authLogin_429Response_hasJsonBody() throws Exception {
        String ip = "10.0.0.2";

        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr(ip);
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest req21 = new MockHttpServletRequest("POST", "/auth/login");
        req21.setRemoteAddr(ip);
        MockHttpServletResponse res21 = new MockHttpServletResponse();

        filter.doFilterInternal(req21, res21, filterChain);

        assertThat(res21.getStatus()).isEqualTo(429);
        assertThat(res21.getContentType()).contains("application/json");
        assertThat(res21.getContentAsString()).contains("Too many requests");
    }

    // ── /api/auth/login ───────────────────────────────────────────────────────

    @Test
    @DisplayName("First request to /api/auth/login from a new IP passes through")
    void apiAuthLogin_firstRequest_passesThroughFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(FIXED_IP);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("21st request to /api/auth/login from same IP returns HTTP 429")
    void apiAuthLogin_requestBeyondCapacity_returns429() throws Exception {
        String ip = "10.0.0.3";

        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr(ip);
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest req21 = new MockHttpServletRequest("POST", "/api/auth/login");
        req21.setRemoteAddr(ip);
        MockHttpServletResponse res21 = new MockHttpServletResponse();

        filter.doFilterInternal(req21, res21, filterChain);

        assertThat(res21.getStatus()).isEqualTo(429);
    }

    // ── Non-auth paths ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Requests to non-auth paths are not intercepted — filter chain called through")
    void nonAuthPath_notIntercepted_filterChainCalled() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        request.setRemoteAddr(FIXED_IP);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    // ── IP isolation ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Rate limit is per-IP — exhausting one IP does not affect another")
    void rateLimitIsPerIp_differentIpsHaveIndependentBuckets() throws Exception {
        String ipA = "172.16.0.1";
        String ipB = "172.16.0.2";

        // Exhaust ipA
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr(ipA);
            filter.doFilterInternal(req, new MockHttpServletResponse(), filterChain);
        }

        // ipB should still be allowed
        MockHttpServletRequest reqB = new MockHttpServletRequest("POST", "/auth/login");
        reqB.setRemoteAddr(ipB);
        MockHttpServletResponse resB = new MockHttpServletResponse();

        filter.doFilterInternal(reqB, resB, filterChain);

        assertThat(resB.getStatus()).isNotEqualTo(429);
    }
}
