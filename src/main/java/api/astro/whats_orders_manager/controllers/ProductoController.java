package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.enums.InvoiceType;
import api.astro.whats_orders_manager.models.Presentacion;
import api.astro.whats_orders_manager.models.Producto;
import api.astro.whats_orders_manager.models.ProductoRecord;
import api.astro.whats_orders_manager.services.PresentacionService;
import api.astro.whats_orders_manager.services.ProductoService;
import api.astro.whats_orders_manager.util.PaginacionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * PRODUCTO CONTROLLER
 * WhatsApp Orders Manager
 * ============================================================================
 * Controlador para la gestión del catálogo de productos.
 * 
 * @version 2.0 - Aplicado PaginacionUtil
 * @since 26/10/2025
 * ============================================================================
 */
@Slf4j
@Controller
@RequestMapping("/productos")
public class ProductoController {
    @Autowired
    private ProductoService productoService;

    @Autowired
    private PresentacionService presentacionService;

    @GetMapping
    public String listarProductos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idProducto") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model
    ) {
        log.info("Listando productos - Página: {}, Tamaño: {}, Ordenar por: {} {}", page, size, sortBy, sortDir);
        
        // Crear objeto Sort
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        // Crear Pageable y obtener página de productos
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Producto> productosPage = productoService.findAll(pageable);
        
        // Agregar presentaciones al modelo
        model.addAttribute("presentaciones", presentacionService.findAll());
        
        // Delegar paginación a PaginacionUtil
        PaginacionUtil.agregarAtributosConOrdenamiento(
            model, 
            PaginacionUtil.fromPage(productosPage),
            "productos",
            sortBy, 
            sortDir
        );
        
        log.info("Productos cargados: {} de {} total", productosPage.getContent().size(), productosPage.getTotalElements());
        
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
