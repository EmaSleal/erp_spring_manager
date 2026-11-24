package api.astro.whats_orders_manager.modules.facturacion.correo.controller;

import api.astro.whats_orders_manager.modules.facturacion.correo.dto.EntregaDTO;
import api.astro.whats_orders_manager.modules.facturacion.correo.service.EntregaComprobanteService;
import api.astro.whats_orders_manager.modules.facturacion.correo.service.EntregaCorreoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * T-055 (ADR-070): historial de entregas por correo de un comprobante y reenvío manual. Los errores responden el cuerpo
 * habitual del proyecto: {@code {"success": false, "message": "..."}}.
 */
@RestController
@RequestMapping("/api/facturas/electronica/comprobantes/{id}/entregas")
@RequiredArgsConstructor
@Slf4j
public class EntregaComprobanteRestController {

    private final EntregaComprobanteService servicio;

    /** Cuerpo del reenvío; el destinatario es opcional (por defecto, el correo del receptor). */
    public record SolicitudReenvio(String destinatario) {
    }

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    public List<EntregaDTO> historial(@PathVariable Long id, Authentication authentication) {
        return servicio.listar(id);
    }

    @PostMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'COMPROBANTE_REENVIAR_CORREO')")
    public ResponseEntity<?> reenviar(@PathVariable Long id, @RequestBody(required = false) SolicitudReenvio solicitud,
                                      Authentication authentication) {
        try {
            String destinatario = solicitud != null ? solicitud.destinatario() : null;
            EntregaDTO entrega = servicio.registrarManual(id, destinatario, authentication.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(entrega);
        } catch (EntregaCorreoException e) {
            return ResponseEntity.status(e.getEstado()).body(Map.of("success", false, "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error registrando el reenvío del comprobante {}: {}", id, e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "No se pudo registrar el reenvío por correo"));
        }
    }
}
