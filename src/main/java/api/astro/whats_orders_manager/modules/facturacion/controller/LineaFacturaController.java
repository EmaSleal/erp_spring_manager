package api.astro.whats_orders_manager.modules.facturacion.controller;


import api.astro.whats_orders_manager.modules.facturacion.dto.TotalesFacturaDTO;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.service.LineaFacturaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * LINEA FACTURA CONTROLLER
 * ERP Orders Manager
 * ============================================================================
 * API REST para gestión de líneas de factura
 * 
 * @version 1.1 - Mejorado logging profesional
 * @since 26/10/2025
 * ============================================================================
 */
@Slf4j
@Controller
@RequestMapping("/lineas-factura")
public class LineaFacturaController {

    @Autowired
    private LineaFacturaService lineaFacturaService;

    @GetMapping("/detalle/{id}")
    @ResponseBody
    public List<LineaFacturaR> obtenerDetalleLineaFactura(@PathVariable Integer id) {
        log.info("Obteniendo líneas de factura ID: {}", id);
        List<LineaFacturaR> lineas = lineaFacturaService.findLineasByFacturaId(id);
        log.debug("Líneas encontradas: {}", lineas.size());
        return lineas;
    }


    /**
     * Guarda el conjunto completo de líneas de una factura (T-024): las que no vienen se eliminan y los montos
     * se calculan en el servidor. Devuelve los totales persistidos.
     */
    @PutMapping("/actualizar")
    @ResponseBody
    public TotalesFacturaDTO actualizarLineaFactura(@RequestBody List<LineaFacturaR> lineas) {
        log.info("Actualizando {} líneas de factura", lineas.size());
        TotalesFacturaDTO totales = lineaFacturaService.updateLineas(lineas);
        log.info("✅ Líneas de la factura {} guardadas: {} líneas, total {}",
                totales.idFactura(), totales.lineas(), totales.total());
        return totales;
    }

    /** Cuerpo inválido (factura inexistente, línea ajena, cantidad o precio inválidos): 400 y nada se persiste. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Líneas de factura rechazadas: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
    }
}
