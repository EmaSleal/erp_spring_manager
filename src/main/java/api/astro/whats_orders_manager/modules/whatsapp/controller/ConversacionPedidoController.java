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

    @GetMapping("/{telefonoCliente}")
    public ResponseEntity<?> get(@PathVariable String telefonoCliente) {
        return service.findByTelefonoCliente(telefonoCliente)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No hay conversación activa para: " + telefonoCliente)));
    }

    @PutMapping("/{telefonoCliente}")
    public ResponseEntity<?> upsert(
            @PathVariable String telefonoCliente,
            @RequestBody ConversacionPedido body
    ) {
        try {
            ConversacionPedido resultado = service.upsert(telefonoCliente, body);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error en upsert conversacion [{}]: {}", telefonoCliente, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la conversación"));
        }
    }

    @DeleteMapping("/{telefonoCliente}")
    public ResponseEntity<?> delete(@PathVariable String telefonoCliente) {
        boolean eliminado = service.deleteByTelefonoCliente(telefonoCliente);
        if (!eliminado) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No existe conversación para: " + telefonoCliente));
        }
        return ResponseEntity.ok(Map.of("message", "Conversación eliminada: " + telefonoCliente));
    }
}
