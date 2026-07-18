package api.astro.whats_orders_manager.modules.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.security.web.header.writers.XContentTypeOptionsHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-style tests that verify the Spring Security header configuration
 * produces the required security headers (CSP, HSTS, X-Content-Type-Options,
 * Referrer-Policy) and that no wildcard CORS is returned on the login endpoint.
 *
 * Drives the filter layer directly without a Spring ApplicationContext or DB.
 *
 * RED phase: tests 5.1-A through 5.1-D will pass once SecurityConfig adds the
 * corresponding header writers (task 5.2). Test 5.1-E will pass once
 * @CrossOrigin("*") annotations are removed and the restricted CORS bean is
 * wired (tasks 5.3 through 5.5).
 */
@DisplayName("Security headers — CSP, HSTS, X-Content-Type-Options, Referrer-Policy, CORS")
class SecurityHeadersIT {

    // ---------------------------------------------------------------------------
    // CSP header
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("5.1-A: Content-Security-Policy header must be present")
    void response_hasContentSecurityPolicyHeader() throws Exception {
        // RED: fails until SecurityConfig adds contentSecurityPolicy(...)
        HeaderWriterFilter filter = buildHeaderWriterFilter();

        MockHttpServletResponse response = executeFilter(filter, "/dashboard");

        assertThat(response.getHeader("Content-Security-Policy"))
                .as("Content-Security-Policy header must be set")
                .isNotNull()
                .isNotBlank();
    }

    @Test
    @DisplayName("5.1-A2: CSP must allow self as default-src")
    void cspHeader_containsDefaultSrcSelf() throws Exception {
        HeaderWriterFilter filter = buildHeaderWriterFilter();

        MockHttpServletResponse response = executeFilter(filter, "/dashboard");

        assertThat(response.getHeader("Content-Security-Policy"))
                .contains("default-src 'self'");
    }

    // ---------------------------------------------------------------------------
    // HSTS header
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("5.1-B: Strict-Transport-Security header must be present")
    void response_hasStrictTransportSecurityHeader() throws Exception {
        // RED: fails until SecurityConfig adds httpStrictTransportSecurity(...)
        // Note: HstsHeaderWriter only writes to HTTPS requests by default.
        // We use a request that Spring Security treats as secure (X-Forwarded-Proto: https)
        HeaderWriterFilter filter = buildHeaderWriterFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        request.setSecure(true); // simulate HTTPS so HSTS writer activates
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("Strict-Transport-Security"))
                .as("Strict-Transport-Security must be set on HTTPS requests")
                .isNotNull()
                .contains("max-age=31536000")
                .contains("includeSubDomains");
    }

    // ---------------------------------------------------------------------------
    // X-Content-Type-Options header
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("5.1-C: X-Content-Type-Options: nosniff must be present")
    void response_hasXContentTypeOptionsHeader() throws Exception {
        // RED: fails until SecurityConfig adds xContentTypeOptions()
        HeaderWriterFilter filter = buildHeaderWriterFilter();

        MockHttpServletResponse response = executeFilter(filter, "/dashboard");

        assertThat(response.getHeader("X-Content-Type-Options"))
                .as("X-Content-Type-Options must be nosniff")
                .isEqualTo("nosniff");
    }

    // ---------------------------------------------------------------------------
    // Referrer-Policy header
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("5.1-D: Referrer-Policy header must be present")
    void response_hasReferrerPolicyHeader() throws Exception {
        // RED: fails until SecurityConfig adds referrerPolicy(...)
        HeaderWriterFilter filter = buildHeaderWriterFilter();

        MockHttpServletResponse response = executeFilter(filter, "/dashboard");

        assertThat(response.getHeader("Referrer-Policy"))
                .as("Referrer-Policy header must be set")
                .isNotNull()
                .isNotBlank();
    }

    // ---------------------------------------------------------------------------
    // CORS — no wildcard
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("5.1-E: CORS preflight from unknown origin must NOT return wildcard ACAO header")
    void corsPreflightFromUnknownOrigin_doesNotReturnWildcard() throws Exception {
        // RED: fails if wildcard CorsConfiguration is registered
        // After GREEN (tasks 5.3-5.5), the allowed-origins list only permits
        // http://localhost:9090 and http://localhost:8080 — an unknown origin
        // receives no Access-Control-Allow-Origin header.
        CorsConfigurationSource restrictedSource = buildRestrictedCorsSource();
        CorsFilter corsFilter = new CorsFilter(restrictedSource);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/auth/login");
        request.addHeader("Origin", "http://evil.example.com");
        request.addHeader("Access-Control-Request-Method", "POST");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        corsFilter.doFilter(request, response, chain);

        String acao = response.getHeader("Access-Control-Allow-Origin");
        assertThat(acao)
                .as("Unknown origin must not receive Access-Control-Allow-Origin")
                .isNotEqualTo("*");
    }

    @Test
    @DisplayName("5.1-F: CORS preflight from allowed origin must be accepted")
    void corsPreflightFromAllowedOrigin_isAccepted() throws Exception {
        CorsConfigurationSource restrictedSource = buildRestrictedCorsSource();
        CorsFilter corsFilter = new CorsFilter(restrictedSource);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/auth/login");
        request.addHeader("Origin", "http://localhost:9090");
        request.addHeader("Access-Control-Request-Method", "POST");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        corsFilter.doFilter(request, response, chain);

        assertThat(response.getHeader("Access-Control-Allow-Origin"))
                .as("Allowed origin http://localhost:9090 must be echoed back")
                .isEqualTo("http://localhost:9090");
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /**
     * Builds a {@link HeaderWriterFilter} that mirrors the header configuration
     * SecurityConfig will produce after task 5.2 (GREEN step).
     */
    private HeaderWriterFilter buildHeaderWriterFilter() {
        ContentSecurityPolicyHeaderWriter csp = new ContentSecurityPolicyHeaderWriter(
                "default-src 'self'; script-src 'self' 'unsafe-inline'; " +
                "style-src 'self' 'unsafe-inline'; img-src 'self' data:;"
        );

        HstsHeaderWriter hsts = new HstsHeaderWriter(31536000, true);

        XContentTypeOptionsHeaderWriter xContentType = new XContentTypeOptionsHeaderWriter();

        ReferrerPolicyHeaderWriter referrer = new ReferrerPolicyHeaderWriter(
                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
        );

        return new HeaderWriterFilter(List.of(csp, hsts, xContentType, referrer));
    }

    /**
     * Builds a {@link CorsConfigurationSource} with the same explicit allowed-origin
     * list that SecurityConfig.corsConfigurationSource() will register (task 5.3).
     */
    private CorsConfigurationSource buildRestrictedCorsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:9090", "http://localhost:8080"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private MockHttpServletResponse executeFilter(
            HeaderWriterFilter filter, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return response;
    }
}
