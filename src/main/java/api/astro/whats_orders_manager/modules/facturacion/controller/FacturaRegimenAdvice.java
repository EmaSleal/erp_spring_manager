package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.facturacion.exception.FacturaRegimenConflictoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * T-057 (ADR-073): traduce a 409 con cuerpo {success:false, message} los dos errores de régimen al guardar una factura:
 * mover la fecha de una factura a un tramo de otro régimen ({@link FacturaRegimenConflictoException}) y no haber un tramo
 * para la fecha de la factura nueva ({@link RegimenNoDefinidoException}, con el mensaje del servicio).
 */
@Slf4j
@ControllerAdvice(assignableTypes = FacturaController.class)
public class FacturaRegimenAdvice {

    @ExceptionHandler({FacturaRegimenConflictoException.class, RegimenNoDefinidoException.class})
    public ResponseEntity<Map<String, Object>> conflicto(RuntimeException e) {
        log.warn("409 de régimen al guardar una factura: {}", e.getMessage());
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("success", false);
        cuerpo.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo);
    }
}
