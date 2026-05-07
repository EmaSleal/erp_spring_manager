package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.shared.dto.PaginacionDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaDetalleDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import api.astro.whats_orders_manager.modules.seguridad.enums.Permiso;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.shared.service.EmailService;
import api.astro.whats_orders_manager.shared.service.MonedaService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.LineaFacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.producto.service.ProductoService;
import api.astro.whats_orders_manager.shared.util.PaginacionUtil;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Controlador para la gestión de Facturas
 * Maneja las operaciones CRUD y funcionalidades relacionadas con facturas
 * 
 * @version 3.2 - Agregada integración con módulo de pagos
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
    private final PagoService pagoService;
    private final EmailService emailService;
    private final MonedaService monedaService;
    private final FacturaMapper facturaMapper;

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
            model.addAttribute("simboloMoneda", monedaService.obtenerSimboloMoneda());
            
            // Enums de Facturación Electrónica Costa Rica
            model.addAttribute("condicionesVenta", api.astro.whats_orders_manager.modules.facturacion.electronica.enums.CondicionVentaFE.values());
            model.addAttribute("mediosPago", api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MedioPagoFE.values());
            model.addAttribute("monedas", api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE.values());
            
            // Agregar rol del usuario para controlar permisos en la vista
            agregarRolUsuario(model, authentication);
            
            log.info("Facturas cargadas: {} de {} total", 
                    facturasPage.getContent().size(), facturasPage.getTotalElements());
            
            return "modules/facturacion/facturas";
            
        } catch (Exception e) {
            log.error("Error al listar facturas: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar las facturas");
            return "shared/error/error";
        }
    }

    /**
     * Obtiene el detalle de una factura (API REST)
     */
    @GetMapping("/detalle/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    @ResponseBody
    public ResponseEntity<FacturaDetalleDTO> obtenerDetalleFactura(@PathVariable Integer id, Authentication authentication) {
        log.info("Obteniendo detalle de factura ID: {}", id);
        
        try {
            Optional<Factura> facturaOpt = facturaService.findById(id);
            
            if (facturaOpt.isEmpty()) {
                log.warn("Factura no encontrada con ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Factura factura = facturaOpt.get();
            
            // Cargar las líneas de factura (están marcadas como @Transient)
            List<LineaFactura> lineas = lineaFacturaService.findLineasByFacturaId(id)
                    .stream()
                    .map(lineaR -> {
                        LineaFactura linea = new LineaFactura();
                        linea.setIdLineaFactura(lineaR.id_linea_factura());
                        linea.setNumeroLinea(lineaR.numero_linea());
                        linea.setCantidad(lineaR.cantidad());
                        linea.setPrecioUnitario(lineaR.precioUnitario());
                        linea.setSubtotal(lineaR.subtotal());
                        Producto producto = new Producto();
                        producto.setIdProducto(lineaR.id_producto());
                        producto.setDescripcion(lineaR.descripcion());
                        linea.setProducto(producto);
                        return linea;
                    })
                    .toList();
                    

            factura.setLineas(lineas);
            
            return ResponseEntity.ok(facturaMapper.toDetalleDTO(factura));
                    
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
        return "modules/facturacion/form";
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
            return "modules/facturacion/form";
            
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
            
            return "modules/facturacion/add-form";
            
        } catch (Exception e) {
            log.error("Error al cargar formulario de nueva factura: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar el formulario");
            return "shared/error/error";
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

    // ==================== ENDPOINTS DE PAGOS ====================
    
    /**
     * Obtiene los pagos de una factura específica (API REST).
     */
    @GetMapping("/{idFactura}/pagos")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_VER')")
    @ResponseBody
    public ResponseEntity<List<PagoDTO>> obtenerPagosPorFactura(
            @PathVariable Integer idFactura,
            Authentication authentication
    ) {
        log.info("Obteniendo pagos de la factura ID: {}", idFactura);
        
        try {
            List<Pago> pagos = pagoService.findByFacturaId(idFactura);
            List<PagoDTO> pagosDTOs = PagoMapper.toDTOList(pagos);
            
            return ResponseEntity.ok(pagosDTOs);
            
        } catch (Exception e) {
            log.error("Error al obtener pagos de factura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Calcula el total pagado de una factura (API REST).
     */
    @GetMapping("/{idFactura}/total-pagado")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_VER')")
    @ResponseBody
    public ResponseEntity<BigDecimal> obtenerTotalPagado(
            @PathVariable Integer idFactura,
            Authentication authentication
    ) {
        log.info("Calculando total pagado de la factura ID: {}", idFactura);
        
        try {
            BigDecimal totalPagado = pagoService.calcularTotalPagadoPorFactura(idFactura);
            return ResponseEntity.ok(totalPagado);
            
        } catch (Exception e) {
            log.error("Error al calcular total pagado: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Calcula el saldo pendiente de una factura (API REST).
     */
    @GetMapping("/{idFactura}/saldo-pendiente")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    @ResponseBody
    public ResponseEntity<BigDecimal> obtenerSaldoPendiente(
            @PathVariable Integer idFactura,
            Authentication authentication
    ) {
        log.info("Calculando saldo pendiente de la factura ID: {}", idFactura);
        
        try {
            Optional<Factura> facturaOpt = facturaService.findById(idFactura);
            
            if (facturaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Factura factura = facturaOpt.get();
            BigDecimal saldoPendiente = factura.calcularSaldoPendiente();
            
            return ResponseEntity.ok(saldoPendiente);
            
        } catch (Exception e) {
            log.error("Error al calcular saldo pendiente: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * API REST: Obtiene las facturas pendientes de un cliente
     * @param idCliente ID del cliente
     * @return Lista de facturas con saldo pendiente
     */
    @GetMapping("/api/cliente/{idCliente}")
    @ResponseBody
    public ResponseEntity<List<java.util.Map<String, Object>>> obtenerFacturasPorCliente(@PathVariable Integer idCliente) {
        try {
            log.info("Obteniendo facturas del cliente ID: {}", idCliente);
            
            // Obtener todas las facturas del cliente
            Optional<List<Factura>> facturasOpt = facturaService.findByClienteId(idCliente);
            
            // Si no hay facturas, devolver lista vacía
            if (facturasOpt.isEmpty()) {
                log.info("Cliente {} no tiene facturas", idCliente);
                return ResponseEntity.ok(List.of());
            }
            
            List<Factura> facturas = facturasOpt.get();
            
            // Filtrar solo las que tienen saldo pendiente
            List<java.util.Map<String, Object>> facturasDTO = facturas.stream()
                .filter(f -> f.calcularSaldoPendiente().compareTo(BigDecimal.ZERO) > 0)
                .map(f -> {
                    java.util.Map<String, Object> dto = new java.util.HashMap<>();
                    dto.put("idFactura", f.getIdFactura());
                    dto.put("numeroFactura", f.getNumeroFactura());
                    dto.put("totalFactura", f.getTotal());
                    dto.put("saldoPendiente", f.calcularSaldoPendiente());
                    return dto;
                })
                .toList();
            
            log.info("Encontradas {} facturas con saldo pendiente para cliente {}", facturasDTO.size(), idCliente);
            return ResponseEntity.ok(facturasDTO);
            
        } catch (Exception e) {
            log.error("Error al obtener facturas del cliente {}: {}", idCliente, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
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
  
}
