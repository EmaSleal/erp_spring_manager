package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.shared.dto.PaginacionDTO;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import api.astro.whats_orders_manager.modules.seguridad.enums.Permiso;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.shared.service.EmailService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.LineaFacturaService;
import api.astro.whats_orders_manager.modules.producto.service.ProductoService;
import api.astro.whats_orders_manager.modules.facturacion.service.ConfiguracionFacturacionService;
import api.astro.whats_orders_manager.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Controlador para la gestión de Facturas
 * Maneja las operaciones CRUD y funcionalidades relacionadas con facturas
 * 
 * @version 3.1 - Refactorizado con DTOs y Utils
 * @since 26/10/2025
 */
@Controller
@RequestMapping("/facturas")
@RequiredArgsConstructor
@Slf4j
public class FacturaController {
    
    private final FacturaService facturaService;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final LineaFacturaService lineaFacturaService;
    private final EmailService emailService;
    private final ConfiguracionFacturacionService configuracionFacturacionService;

    /**
     * Lista todas las facturas con paginación y ordenamiento
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    public String listarFacturas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idFactura") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model, 
            Authentication authentication
    ) {
        log.info("Listando facturas - Página: {}, Tamaño: {}, Ordenar por: {} {}", 
                page, size, sortBy, sortDir);
        
        try {
            // Crear objeto Sort
            Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
            
            // Crear Pageable
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Obtener página de facturas
            Page<Factura> facturasPage = facturaService.findAll(pageable);
            
            // Convertir a DTO y agregar atributos de paginación usando PaginacionUtil
            PaginacionDTO<Factura> paginacion = PaginacionUtil.fromPage(facturasPage);
            PaginacionUtil.agregarAtributosConOrdenamiento(model, paginacion, "facturas", sortBy, sortDir);
            
            // Agregar datos adicionales para la vista
            model.addAttribute("clientes", clienteService.findAll());
            model.addAttribute("tiposFactura", InvoiceType.values());
            model.addAttribute("simboloMoneda", obtenerSimboloMoneda());
            
            // Agregar rol del usuario para controlar permisos en la vista
            agregarRolUsuario(model, authentication);
            
            log.info("Facturas cargadas: {} de {} total", 
                    facturasPage.getContent().size(), facturasPage.getTotalElements());
            
            return "facturas/facturas";
            
        } catch (Exception e) {
            log.error("Error al listar facturas: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar las facturas");
            return "error/error";
        }
    }

    /**
     * Obtiene el detalle de una factura (API REST)
     */
    @GetMapping("/detalle/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    @ResponseBody
    public ResponseEntity<Factura> obtenerDetalleFactura(@PathVariable Integer id, Authentication authentication) {
        log.info("Obteniendo detalle de factura ID: {}", id);
        
        try {
            Optional<Factura> factura = facturaService.findById(id);
            return factura.map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        log.warn("Factura no encontrada con ID: {}", id);
                        return ResponseEntity.notFound().build();
                    });
                    
        } catch (Exception e) {
            log.error("Error al obtener factura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Muestra el formulario para crear una nueva factura
     */
    @GetMapping("/nuevo")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_CREAR')")
    public String nuevaFactura(Model model, Authentication authentication) {
        log.info("Mostrando formulario de nueva factura");
        model.addAttribute("factura", new Factura());
        return "facturas/form";
    }

    /**
     * Proporciona los tipos de factura disponibles para el formulario
     */
    @ModelAttribute("tiposFactura")
    public InvoiceType[] getTiposFactura() {
        return InvoiceType.values();
    }

    /**
     * Guarda una nueva factura (API REST)
     * Nota: Las fechas se manejan automáticamente por @EntityListeners en la entidad
     */
    @PostMapping("/guardar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_CREAR')")
    @ResponseBody
    public ResponseEntity<Factura> guardarFactura(@RequestBody Factura factura, Authentication authentication) {
        log.info("Guardando nueva factura");
        
        try {
            Factura nuevaFactura = facturaService.save(factura);
            log.info("Factura guardada exitosamente con ID: {}", nuevaFactura.getIdFactura());
            return ResponseEntity.ok(nuevaFactura);
            
        } catch (Exception e) {
            log.error("Error al guardar factura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Elimina una factura por su ID
     */
    @GetMapping("/eliminar/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_ELIMINAR')")
    public String eliminarFactura(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        log.info("Intentando eliminar factura ID: {}", id);
        
        try {
            // Validar que la factura existe
            if (!facturaService.existsById(id)) {
                log.warn("La factura con ID {} no existe", id);
                redirectAttributes.addFlashAttribute("error", 
                    "La factura no existe");
                return "redirect:/facturas";
            }
            
            // Validar que no tenga líneas asociadas
            if (lineaFacturaService.existsByFacturaId(id)) {
                log.warn("La factura con ID {} tiene líneas asociadas", id);
                redirectAttributes.addFlashAttribute("error", 
                    "No se puede eliminar la factura porque tiene líneas asociadas");
                return "redirect:/facturas";
            }

            facturaService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", 
                "Factura eliminada exitosamente");
            log.info("Factura eliminada exitosamente ID: {}", id);
            
        } catch (Exception e) {
            log.error("Error al eliminar factura: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al eliminar la factura");
        }
        
        return "redirect:/facturas";
    }

    /**
     * Muestra el formulario para editar una factura existente
     */
    @GetMapping("/editar/{idFactura}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_EDITAR')")
    public String editarFactura(
            @PathVariable Integer idFactura,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        log.info("Editando factura ID: {}", idFactura);
        
        try {
            Optional<Factura> facturaOptional = facturaService.findById(idFactura);

            if (facturaOptional.isEmpty()) {
                log.warn("Factura no encontrada con ID: {}", idFactura);
                redirectAttributes.addFlashAttribute("error", 
                    "Factura no encontrada");
                return "redirect:/facturas";
            }
            
            model.addAttribute("factura", facturaOptional.get());
            return "facturas/form";
            
        } catch (Exception e) {
            log.error("Error al cargar factura para edición: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                "Error al cargar la factura");
            return "redirect:/facturas";
        }
    }

    /**
     * Muestra el formulario para crear una nueva factura (vista completa)
     */
    @GetMapping("/nueva")
    public String mostrarFormularioNuevaFactura(Model model) {
        log.info("Mostrando formulario completo para nueva factura");
        
        try {
            Factura nuevaFactura = new Factura();
            List<Cliente> clientes = clienteService.findAll();
            List<Producto> productos = productoService.findAll();

            model.addAttribute("factura", nuevaFactura);
            model.addAttribute("clientes", clientes);
            model.addAttribute("productos", productos);
            
            return "facturas/add-form";
            
        } catch (Exception e) {
            log.error("Error al cargar formulario de nueva factura: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el formulario");
            return "error/error";
        }
    }

    /**
     * Actualiza el estado de entrega de una factura (API REST)
     */
    @PutMapping("/actualizar-estado/{id}")
    @ResponseBody
    public ResponseEntity<String> actualizarEstadoFactura(
            @PathVariable Integer id,
            @RequestParam Boolean entregado
    ) {
        log.info("Actualizando estado de factura ID: {} a entregado={}", id, entregado);
        
        try {
            Optional<Factura> facturaOpt = facturaService.findById(id);
            
            if (facturaOpt.isEmpty()) {
                log.warn("Factura no encontrada con ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Factura factura = facturaOpt.get();
            factura.setEntregado(entregado);
            // Las fechas de actualización se manejan automáticamente por @EntityListeners
            facturaService.save(factura);
            
            log.info("Estado de factura {} actualizado exitosamente", id);
            return ResponseEntity.ok("Estado actualizado correctamente");
            
        } catch (Exception e) {
            log.error("Error al actualizar estado de factura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error al actualizar el estado");
        }
    }

    /**
     * Envía una factura por email al cliente (API REST)
     * Punto 5.3.1 - Envío de facturas por email
     */
    @PostMapping("/{id}/enviar-email")
    @ResponseBody
    public ResponseEntity<?> enviarFacturaPorEmail(@PathVariable Integer id) {
        log.info("Intentando enviar factura ID: {} por email", id);
        
        try {
            // Buscar la factura
            Optional<Factura> facturaOpt = facturaService.findById(id);
            
            if (facturaOpt.isEmpty()) {
                log.warn("Factura con ID {} no encontrada", id);
                return ResponseEntity.notFound().build();
            }
            
            Factura factura = facturaOpt.get();
            
            // Validar que el cliente tenga email
            if (factura.getCliente() == null || 
                factura.getCliente().getEmail() == null || 
                factura.getCliente().getEmail().isBlank()) {
                log.warn("El cliente de la factura {} no tiene email configurado", id);
                return ResponseEntity.badRequest()
                    .body("{\"error\": \"El cliente no tiene email configurado\"}");
            }
            
            // Enviar el email
            emailService.enviarFacturaPorEmail(factura);
            
            log.info("✅ Factura {} enviada por email a {}", 
                factura.getNumeroFactura(), 
                factura.getCliente().getEmail());
            
            return ResponseEntity.ok()
                .body("{\"message\": \"Factura enviada exitosamente a " + 
                    factura.getCliente().getEmail() + "\"}");
            
        } catch (Exception e) {
            log.error("Error al enviar factura por email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("{\"error\": \"Error al enviar el email: " + e.getMessage() + "\"}");
        }
    }

    // ==================== MÉTODOS PRIVADOS AUXILIARES ====================


    /**
     * Agrega el rol del usuario al modelo para control de permisos en la vista
     */
    private void agregarRolUsuario(Model model, Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            String userRole = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .orElse("USER");
            model.addAttribute("userRole", userRole);
            log.debug("Rol de usuario agregado al modelo: {}", userRole);
        }
    }

    /**
     * Obtiene el símbolo de moneda desde la configuración de facturación
     * @return Símbolo de moneda (₡, $, €, etc.)
     */
    private String obtenerSimboloMoneda() {
        try {
            var config = configuracionFacturacionService.getConfiguracionActiva();
            if (config.isPresent()) {
                String moneda = config.get().getMoneda();
                if (moneda != null) {
                    return switch (moneda.toUpperCase()) {
                        case "CRC" -> "₡";
                        case "USD" -> "$";
                        case "MXN" -> "$";
                        case "EUR" -> "€";
                        default -> moneda + " ";
                    };
                }
            }
        } catch (Exception e) {
            log.warn("Error al obtener símbolo de moneda, usando predeterminado: {}", e.getMessage());
        }
        return "₡"; // Colones por defecto
    }
}
