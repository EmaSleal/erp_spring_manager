package api.astro.whats_orders_manager.modules.inventario.controller;

import api.astro.whats_orders_manager.modules.inventario.dto.ResumenInventarioDTO;
import api.astro.whats_orders_manager.modules.inventario.service.ReporteInventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventario/stock")
@RequiredArgsConstructor
public class StockRestController {

    private final ReporteInventarioService reporteService;

    @GetMapping("/resumen")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResumenInventarioDTO> getResumen() {
        return ResponseEntity.ok(reporteService.getResumen());
    }
}
