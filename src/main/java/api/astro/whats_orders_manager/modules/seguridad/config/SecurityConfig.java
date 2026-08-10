package api.astro.whats_orders_manager.modules.seguridad.config;

import api.astro.whats_orders_manager.modules.seguridad.security.AppLogoutSuccessHandler;
import api.astro.whats_orders_manager.modules.seguridad.security.LoginFailureHandler;
import api.astro.whats_orders_manager.modules.seguridad.security.LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.sql.DataSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Habilita @PreAuthorize, @PostAuthorize, @Secured
public class SecurityConfig {

    @Autowired
    private DataSource dataSource;

    @Value("${app.cors.allowed-origins:http://localhost:15808,http://localhost:8080}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LoginFailureHandler loginFailureHandler,
            LoginSuccessHandler loginSuccessHandler,
            AppLogoutSuccessHandler appLogoutSuccessHandler
    ) throws Exception {
        http
                // Configuración de autorización de requests
                .authorizeHttpRequests(auth -> auth
                        // Recursos públicos (CSS, JS, imágenes, auth)
                        .requestMatchers("/auth/**", "/api/auth/login", "/shared/css/**", "/shared/js/**", "/images/**").permitAll()

                        // Healthcheck para Docker Compose
                        .requestMatchers("/actuator/health").permitAll()
                        
                        // ========================================
                        // WEBHOOK DE WHATSAPP - Público (Meta verifica)
                        // ========================================
                        .requestMatchers("/api/whatsapp/webhook").permitAll()
                        
                        // Dashboard y perfil - requiere autenticación (todos los roles)
                        .requestMatchers("/dashboard", "/perfil/**").authenticated()
                        
                        // ========================================
                        // MÓDULOS OPERATIVOS
                        // ========================================
                        
                        // Clientes - Visualización para todos, edición solo ADMIN y USER
                        .requestMatchers("/clientes", "/clientes/").hasAnyRole("ADMIN", "USER", "VENDEDOR", "VISUALIZADOR")
                        .requestMatchers("/clientes/form", "/clientes/form/**", "/clientes/save", "/clientes/delete/**")
                            .hasAnyRole("ADMIN", "USER")
                        
                        // Productos - Visualización para todos, edición solo ADMIN y USER
                        .requestMatchers("/productos", "/productos/").hasAnyRole("ADMIN", "USER", "VENDEDOR", "VISUALIZADOR")
                        .requestMatchers("/productos/form", "/productos/form/**", "/productos/save", "/productos/delete/**")
                            .hasAnyRole("ADMIN", "USER")
                        
                        // Facturas - Todos pueden ver lista, solo ADMIN/USER/VENDEDOR pueden crear/editar
                        .requestMatchers("/facturas", "/facturas/", "/facturas/ver/**").hasAnyRole("ADMIN", "USER", "VENDEDOR", "VISUALIZADOR")
                        .requestMatchers("/facturas/form", "/facturas/form/**", "/facturas/editar/**", "/facturas/save")
                            .hasAnyRole("ADMIN", "USER", "VENDEDOR")
                        .requestMatchers("/facturas/delete/**", "/facturas/anular/**")
                            .hasAnyRole("ADMIN", "USER")
                        
                        // Líneas de factura - VISUALIZADOR puede VER, solo ADMIN/USER/VENDEDOR pueden modificar
                        .requestMatchers("/lineas-factura/detalle/**").hasAnyRole("ADMIN", "USER", "VENDEDOR", "VISUALIZADOR")
                        .requestMatchers("/lineas-factura/**").hasAnyRole("ADMIN", "USER", "VENDEDOR")
                        
                        // ========================================
                        // MÓDULOS ADMINISTRATIVOS - Solo ADMIN
                        // ========================================
                        .requestMatchers("/configuracion/**", "/usuarios/**", "/admin/**").hasRole("ADMIN")
                        
                        // ========================================
                        // REPORTES - ADMIN y USER
                        // ========================================
                        .requestMatchers("/reportes/**").hasAnyRole("ADMIN", "USER")
                        
                        // ========================================
                        // APIs y AJAX - Según funcionalidad
                        // ========================================
                        .requestMatchers("/api/clientes/**").hasAnyRole("ADMIN", "USER", "VENDEDOR")
                        .requestMatchers("/api/productos/**").hasAnyRole("ADMIN", "USER", "VENDEDOR")
                        .requestMatchers("/api/facturas/**").hasAnyRole("ADMIN", "USER", "VENDEDOR")
                        .requestMatchers("/api/configuracion/**").hasRole("ADMIN")

                        // Cualquier otro request requiere autenticación
                        .anyRequest().authenticated()
                )
                
                // Deshabilitar CSRF para webhook de WhatsApp y API de auth (clientes externos sin sesión)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/whatsapp/webhook", "/api/auth/login")
                )
                
                // Configuración de login
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )

                // Configuración de logout
                .logout(logout -> logout
                        .logoutUrl("/logout")                       // URL para hacer logout (POST request)
                        .logoutSuccessHandler(appLogoutSuccessHandler)
                        .invalidateHttpSession(true)                // Invalidar sesión HTTP
                        .deleteCookies("JSESSIONID")                // Eliminar cookie de sesión
                        .permitAll()
                )
                
                // Configuración de Remember Me — tokens persistentes en la tabla persistent_logins
                .rememberMe(r -> r
                        .tokenRepository(jdbcTokenRepository())
                        .tokenValiditySeconds(2592000)    // 30 días
                        .rememberMeParameter("remember-me")
                        .key("erp-remember-me-key")
                )

                // Configuración de sesiones
                .sessionManagement(session -> session
                        .maximumSessions(1)                     // Máximo 1 sesión por usuario
                        .maxSessionsPreventsLogin(false)        // Permitir nuevo login (cierra sesión anterior)
                )
                
                // Manejo de excepciones - Página 403 personalizada
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/error/403")         // Redirigir a página 403 personalizada
                )
                
                // Configuración de seguridad adicional — cabeceras HTTP de seguridad
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())           // Permitir iframes del mismo origen
                        .xssProtection(xss -> xss.disable())                 // XSS protection (ya manejado por navegadores modernos)
                        .contentSecurityPolicy(csp -> csp.policyDirectives(  // CSP: restringe fuentes de contenido
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://code.jquery.com; " +
                                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                                "font-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                                "img-src 'self' data:; " +
                                "connect-src 'self' https://cdn.jsdelivr.net;"
                        ))
                        .httpStrictTransportSecurity(hsts -> hsts             // HSTS: forzar HTTPS por 1 año
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                        )
                        .referrerPolicy(referrer -> referrer                  // Referrer-Policy: no leak cross-origin
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                )

                // CORS: restringir orígenes permitidos — sin wildcard
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    /**
     * Configuración del AuthenticationManager usando AuthenticationManagerBuilder
     * Este es el enfoque recomendado en Spring Security 6.x
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) throws Exception {
        AuthenticationManagerBuilder authManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
        
        return authManagerBuilder.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public PersistentTokenRepository jdbcTokenRepository() {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

    /**
     * Explicit CORS configuration source — replaces any wildcard @CrossOrigin.
     * Allowed origins are configurable via {@code app.cors.allowed-origins}
     * (default: localhost:9090 and localhost:8080).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}