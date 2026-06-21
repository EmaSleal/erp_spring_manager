package api.astro.whats_orders_manager.modules.whatsapp.controller;

import api.astro.whats_orders_manager.modules.whatsapp.model.ConversacionPedido;
import api.astro.whats_orders_manager.modules.whatsapp.service.ConversacionPedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversaciones")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ConversacionPedidoController {

    private final ConversacionPedidoService service;

    @GetMapping("/{telefonoVendedor}")
    public ResponseEntity<?> get(@PathVariable String telefonoVendedor) {
        return service.findByTelefonoVendedor(telefonoVendedor)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No hay conversación activa para: " + telefonoVendedor)));
    }

    @PutMapping("/{telefonoVendedor}")
    public ResponseEntity<?> upsert(
            @PathVariable String telefonoVendedor,
            @RequestBody ConversacionPedido body
    ) {
        try {
            ConversacionPedido resultado = service.upsert(telefonoVendedor, body);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error en upsert conversacion [{}]: {}", telefonoVendedor, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la conversación"));
        }
    }

    @DeleteMapping("/{telefonoVendedor}")
    public ResponseEntity<?> delete(@PathVariable String telefonoVendedor) {
        boolean eliminado = service.deleteByTelefonoVendedor(telefonoVendedor);
        if (!eliminado) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No existe conversación para: " + telefonoVendedor));
        }
        return ResponseEntity.ok(Map.of("message", "Conversación eliminada: " + telefonoVendedor));
    }
}
