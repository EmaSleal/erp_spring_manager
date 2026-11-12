package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.exception.FacturaBloqueadaException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Traduce {@link FacturaBloqueadaException} (ADR-030) a 409 Conflict con el motivo accionable:
 * <ul>
 *   <li>navegación del navegador (Accept text/html, p. ej. eliminar o anular desde el listado): página de error;</li>
 *   <li>llamadas de la UI por fetch y clientes de API: JSON {success:false, message, estadoComprobante}, que
 *       common.js muestra como mensaje del servidor.</li>
 * </ul>
 */
@Slf4j
@ControllerAdvice
public class FacturaBloqueadaAdvice {

    @ExceptionHandler(FacturaBloqueadaException.class)
    public ModelAndView facturaBloqueada(FacturaBloqueadaException e, HttpServletRequest request) {
        log.warn("409 en {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        boolean navegador = accept != null && accept.contains("text/html");

        ModelAndView mav;
        if (navegador) {
            mav = new ModelAndView("shared/error/error");
            mav.addObject("status", HttpStatus.CONFLICT.value());
            mav.addObject("error", "Factura bloqueada por su comprobante electrónico");
            mav.addObject("message", e.getMessage());
            mav.addObject("path", request.getRequestURI());
        } else {
            MappingJackson2JsonView json = new MappingJackson2JsonView();
            json.setModelKeys(Set.of("success", "message", "estadoComprobante"));
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("success", false);
            cuerpo.put("message", e.getMessage());
            cuerpo.put("estadoComprobante", e.getDecision() != null && e.getDecision().estado() != null
                    ? e.getDecision().estado().name() : null);
            mav = new ModelAndView(json, cuerpo);
        }
        mav.setStatus(HttpStatus.CONFLICT);
        return mav;
    }
}
