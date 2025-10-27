package api.astro.whats_orders_manager.controllers;


import api.astro.whats_orders_manager.models.LineaFacturaR;
import api.astro.whats_orders_manager.services.LineaFacturaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * LINEA FACTURA CONTROLLER
 * WhatsApp Orders Manager
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


    @PutMapping("/actualizar")
    @ResponseBody
    public Boolean actualizarLineaFactura(@RequestBody List<LineaFacturaR> lineas) {
        log.info("Actualizando {} líneas de factura", lineas.size());
        Boolean resultado = lineaFacturaService.updateLineas(lineas);
        log.info("✅ Líneas actualizadas: {}", resultado);
        return resultado;
    }

    @DeleteMapping("/eliminar/{id}")
    @ResponseBody
    public Boolean eliminarLineaFactura(@PathVariable Integer id) {
        log.info("Eliminando línea de factura ID: {}", id);
        Boolean resultado = lineaFacturaService.deleteById(id);
        log.info("✅ Línea eliminada: {}", resultado);
        return resultado;
    }
}
