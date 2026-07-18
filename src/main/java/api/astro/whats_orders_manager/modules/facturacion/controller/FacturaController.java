package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.shared.dto.PaginacionDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaDetalleDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.FacturaPendienteDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.FacturaMapper;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.shared.service.EmailService;
import api.astro.whats_orders_manager.shared.service.MonedaService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.shared.util.ResponseUtil;
import api.astro.whats_orders_manager.shared.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

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
    private final PagoService pagoService;
    private final EmailService emailService;
    private final MonedaService monedaService;
    private final FacturaMapper facturaMapper;
    private final FacturaPdfService facturaPdfService;
    private final PagoMapper pagoMapper;

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
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(required = false) String estadoFE,
            Model model,
            Authentication authentication
    ) {
        log.info("Listando facturas - Página: {}, Tamaño: {}, Ordenar por: {} {}",
                page, size, sortBy, sortDir);

        try {
            Pageable pageable = PaginacionUtil.buildPageable(page, size, sortBy, sortDir,
                    Set.of("idFactura", "fechaEmision", "total", "estado", "estadoPago", "fechaEntrega"), "idFactura");

            // Derivar el filtro de entrega a partir del valor plano del select
            Boolean entregado = "entregado".equals(status) ? Boolean.TRUE
                    : "pendiente".equals(status) ? Boolean.FALSE
                    : null;

            // sinFE y estadoFE son mutuamente excluyentes: "sin_fe" busca facturas sin
            // comprobante electrónico; cualquier otro valor es un nombre de EstadoComprobante
            Boolean sinFE = null;
            EstadoComprobante estadoFEFiltro = null;
            if (estadoFE != null && !estadoFE.isBlank()) {
                if ("sin_fe".equals(estadoFE)) {
                    sinFE = Boolean.TRUE;
                } else {
                    try {
                        estadoFEFiltro = EstadoComprobante.valueOf(estadoFE);
                    } catch (IllegalArgumentException e) {
                        log.warn("Valor de estadoFE inválido recibido: {}", estadoFE);
                    }
                }
            }

            boolean sinFiltros = startDate == null && endDate == null
                    && (status == null || status.isBlank())
                    && isPaid == null
                    && (estadoFE == null || estadoFE.isBlank());

            // Obtener página de facturas (con o sin filtros)
            Page<Factura> facturasPage = sinFiltros
                    ? facturaService.findAll(pageable)
                    : facturaService.buscarConFiltros(pageable, startDate, endDate, entregado, isPaid, sinFE, estadoFEFiltro);

            // Convertir a DTO y agregar atributos de paginación usando PaginacionUtil
            PaginacionDTO<Factura> paginacion = PaginacionUtil.fromPage(facturasPage);
            PaginacionUtil.agregarAtributosConOrdenamiento(model, paginacion, "facturas", sortBy, sortDir);

            // Preservar los filtros activos para el formulario y los enlaces de paginación
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("status", status);
            model.addAttribute("isPaid", isPaid);
            model.addAttribute("estadoFE", estadoFE);

            // Agregar datos adicionales para la vista
            model.addAttribute("clientes", clienteService.findAll());
            model.addAttribute("tiposFactura", InvoiceType.values());
            model.addAttribute("simboloMoneda", monedaService.obtenerSimboloMoneda());
            
            // Enums de Facturación Electrónica Costa Rica
            model.addAttribute("condicionesVenta", api.astro.whats_orders_manager.modules.facturacion.electronica.enums.CondicionVentaFE.values());
            model.addAttribute("mediosPago", api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MedioPagoFE.values());
            model.addAttribute("monedas", api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE.values());
            
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
            
            return ResponseEntity.ok(facturaMapper.toDetalleDTO(factura));
                    
        } catch (Exception e) {
            log.error("Error al obtener factura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
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
            facturaService.eliminarConValidacion(id);
            redirectAttributes.addFlashAttribute("success", "Factura eliminada exitosamente");
            log.info("Factura eliminada exitosamente ID: {}", id);
        } catch (NoSuchElementException | IllegalStateException e) {
            log.warn("No se pudo eliminar factura {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error al eliminar factura: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la factura");
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
     * Actualiza el estado de entrega de una factura (API REST)
     */
    @PutMapping("/actualizar-estado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> actualizarEstadoFactura(
            @PathVariable Integer id,
            @RequestParam Boolean entregado,
            @RequestParam(required = false) String descripcion,
            @RequestParam(name = "fechaEntrega", required = false) String fechaEntregaStr
    ) {
        log.info("Actualizando estado de factura ID: {} a entregado={}", id, entregado);
        try {
            facturaService.actualizarEstado(id, entregado, descripcion, fechaEntregaStr);
            log.info("Estado de factura {} actualizado exitosamente", id);
            return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado correctamente"));
        } catch (NoSuchElementException e) {
            log.warn("Factura no encontrada con ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al actualizar estado de factura: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("mensaje", "Error al actualizar el estado"));
        }
    }

    @GetMapping("/pdf/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'FACTURA_VER')")
    public ResponseEntity<byte[]> descargarPdfFactura(@PathVariable Integer id, Authentication authentication) {
        try {
            byte[] pdf = facturaPdfService.generarPdfFactura(id);
            return ResponseUtil.pdf(pdf, "factura-" + id + ".pdf");
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al generar PDF de factura {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
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
            List<PagoDTO> pagosDTOs = pagoMapper.toDTOList(pagos);
            
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
     * API REST: Returns pending invoices for a given client.
     *
     * @param idCliente client ID
     * @return list of invoices with a pending balance greater than zero
     */
    @GetMapping("/api/cliente/{idCliente}")
    @ResponseBody
    public ResponseEntity<List<FacturaPendienteDTO>> obtenerFacturasPorCliente(@PathVariable Integer idCliente) {
        try {
            log.info("Obteniendo facturas del cliente ID: {}", idCliente);
            List<FacturaPendienteDTO> result = facturaService.obtenerPendientesPorCliente(idCliente);
            log.info("Encontradas {} facturas con saldo pendiente para cliente {}", result.size(), idCliente);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error al obtener facturas del cliente {}: {}", idCliente, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
