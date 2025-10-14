package api.astro.whats_orders_manager.config;

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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Habilita @PreAuthorize, @PostAuthorize, @Secured
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Configuración de autorización de requests
                .authorizeHttpRequests(auth -> auth
                        // Recursos públicos (CSS, JS, imágenes, auth)
                        .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/images/**").permitAll()
                        
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
                        
                        // Cualquier otro request requiere autenticación
                        .anyRequest().authenticated()
                )
                
                // Configuración de login
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .defaultSuccessUrl("/dashboard", true)  // Redirigir a dashboard después del login
                        .failureUrl("/auth/login?error=true")   // Redirigir al login con error
                        .permitAll()
                )
                
                // Configuración de logout
                .logout(logout -> logout
                        .logoutUrl("/logout")                   // URL para hacer logout (POST request)
                        .logoutSuccessUrl("/auth/login?logout") // Redirigir después del logout
                        .invalidateHttpSession(true)            // Invalidar sesión HTTP
                        .deleteCookies("JSESSIONID")            // Eliminar cookie de sesión
                        .permitAll()
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
                
                // Configuración de seguridad adicional
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()) // Permitir iframes del mismo origen
                        .xssProtection(xss -> xss.disable())       // XSS protection (ya manejado por navegadores modernos)
                );

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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}