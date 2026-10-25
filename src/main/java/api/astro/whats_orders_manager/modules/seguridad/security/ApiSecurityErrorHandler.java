package api.astro.whats_orders_manager.modules.seguridad.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rechazos de seguridad en JSON para la API y las peticiones AJAX (ADR-031 §2): 403 por CSRF inválido o ausente o
 * por falta de permisos, y 401 sin sesión, con el cuerpo {status, error, message}. Las páginas conservan su
 * comportamiento (SecurityConfig delega en los manejadores de siempre cuando {@link #PETICION_API} no aplica).
 */
@Slf4j
@Component
public class ApiSecurityErrorHandler implements AccessDeniedHandler, AuthenticationEntryPoint {

    /** Mensaje del 403 por token CSRF inválido o ausente. */
    public static final String MENSAJE_CSRF = "Sesión vencida o token de seguridad inválido; recargue la página";
    static final String MENSAJE_SIN_PERMISO = "No tiene permisos para realizar esta acción";
    static final String MENSAJE_SIN_SESION = "Sesión no iniciada o vencida; inicie sesión de nuevo";

    /** /api/**, Accept con application/json o X-Requested-With: XMLHttpRequest. */
    public static final RequestMatcher PETICION_API = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
            ApiSecurityErrorHandler::aceptaJson,
            new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"));

    private final ObjectMapper objectMapper;

    public ApiSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        boolean csrf = accessDeniedException instanceof CsrfException;
        log.warn("403 en {} {}: {}", request.getMethod(), request.getRequestURI(),
                csrf ? "token CSRF inválido o ausente" : accessDeniedException.getMessage());
        escribir(response, HttpStatus.FORBIDDEN, csrf ? MENSAJE_CSRF : MENSAJE_SIN_PERMISO);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED, MENSAJE_SIN_SESION);
    }

    private void escribir(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", status.value());
        cuerpo.put("error", status.getReasonPhrase());
        cuerpo.put("message", message);
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), cuerpo);
    }

    private static boolean aceptaJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE);
    }
}
