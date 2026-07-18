package api.astro.whats_orders_manager.modules.seguridad.config;

import api.astro.whats_orders_manager.modules.seguridad.security.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers RateLimitFilter for the two authentication endpoints only.
 * Using FilterRegistrationBean keeps SecurityConfig untouched (PR5 constraint).
 * The filter runs before the security filter chain via Ordered.HIGHEST_PRECEDENCE.
 */
@Configuration
public class RateLimitFilterConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitFilter rateLimitFilter) {

        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(rateLimitFilter);
        registration.addUrlPatterns("/auth/login", "/api/auth/login");
        registration.setName("rateLimitFilter");
        registration.setOrder(1); // run early — before security filter chain
        return registration;
    }
}
