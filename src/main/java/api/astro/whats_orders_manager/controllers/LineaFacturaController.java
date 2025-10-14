package api.astro.whats_orders_manager.controllers;


import api.astro.whats_orders_manager.models.LineaFacturaR;
import api.astro.whats_orders_manager.services.LineaFacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/lineas-factura")
public class LineaFacturaController {

    @Autowired
    private LineaFacturaService lineaFacturaService;

    @GetMapping("/detalle/{id}")
    @ResponseBody
    public List<LineaFacturaR> obtenerDetalleLineaFactura(@PathVariable Integer id) {
        return lineaFacturaService.findLineasByFacturaId(id);
    }


    @PutMapping("/actualizar")
    @ResponseBody
    public Boolean actualizarLineaFactura(@RequestBody List<LineaFacturaR> lineas) {
        return lineaFacturaService.updateLineas(lineas);
    }

    @DeleteMapping("/eliminar/{id}")
    @ResponseBody
    public Boolean eliminarLineaFactura(@PathVariable Integer id) {
        return lineaFacturaService.deleteById(id);
    }
}
