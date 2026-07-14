package api.astro.whats_orders_manager.modules.inventario.controller;

import api.astro.whats_orders_manager.modules.inventario.dto.KardexItemDTO;
import api.astro.whats_orders_manager.modules.inventario.service.MovimientoInventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/kardex")
@RequiredArgsConstructor
@Slf4j
public class KardexRestController {

    private final MovimientoInventarioService movimientoService;

    @GetMapping("/{productoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KardexItemDTO>> getKardex(@PathVariable Integer productoId) {
        log.debug("Fetching kardex for productoId={}", productoId);
        List<KardexItemDTO> items = movimientoService.obtenerKardex(productoId)
            .stream()
            .map(KardexItemDTO::new)
            .toList();
        return ResponseEntity.ok(items);
    }
}
