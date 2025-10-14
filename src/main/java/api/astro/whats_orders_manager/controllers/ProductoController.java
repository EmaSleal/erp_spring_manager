package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.enums.InvoiceType;
import api.astro.whats_orders_manager.models.Presentacion;
import api.astro.whats_orders_manager.models.Producto;
import api.astro.whats_orders_manager.models.ProductoRecord;
import api.astro.whats_orders_manager.services.PresentacionService;
import api.astro.whats_orders_manager.services.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/productos")
public class ProductoController {
    @Autowired
    private ProductoService productoService;

    @Autowired
    private PresentacionService presentacionService;

    @GetMapping
    public String listarProductos(Model model) {
        model.addAttribute("productos", productoService.findAll());
        model.addAttribute("presentaciones", presentacionService.findAll());
        return "productos/productos";
    }

//    @GetMapping("/nuevo")
//    public String nuevoProducto(Model model) {
//        model.addAttribute("producto", new Producto());
//        return "productos/form";
//    }

    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto) {
        productoService.save(producto);
        //log.info("Guardando producto: {}", producto);
        return "redirect:/productos";
    }

    @GetMapping("/editar/{id}")
    public String editarProducto(@PathVariable Integer id, Model model) {
        Producto producto = productoService.findById(id).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
        List<Presentacion> presentaciones = presentacionService.findAll();

        model.addAttribute("producto", producto);
        model.addAttribute("presentaciones", presentaciones);

        return "productos/form";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable Integer id) {
        productoService.deleteById(id);
        return "redirect:/productos";
    }

    @GetMapping("/records")
    public ResponseEntity<List<ProductoRecord>> obtenerRegistros() {
        List<ProductoRecord> productos = productoService.findAllRecords();
        return ResponseEntity.ok(productos);
    }

    @PostMapping("/actualizar")
    public String actualizarProducto(@ModelAttribute Producto producto) {
        productoService.save(producto);
        return "redirect:/productos";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoProducto(Model model) {
        List<Presentacion> presentaciones = presentacionService.findAll();
        //log.info("Presentaciones disponibles: {}", presentaciones);
        model.addAttribute("producto", new Producto());
        model.addAttribute("presentaciones", presentaciones);
        return "productos/form";
    }

    @ModelAttribute("tiposFactura")
    public InvoiceType[] getTiposFactura() {
        return InvoiceType.values();
    }

}
