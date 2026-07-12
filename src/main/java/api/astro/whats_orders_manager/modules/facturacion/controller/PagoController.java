package api.astro.whats_orders_manager.modules.facturacion.controller;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.ReporteCajaDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.shared.dto.PaginacionDTO;
import api.astro.whats_orders_manager.shared.dto.ResponseDTO;
import api.astro.whats_orders_manager.shared.util.PaginacionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Controlador para la gestión de Pagos de Facturas.
 * Maneja operaciones CRUD, conciliación bancaria y reportes de caja.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 1
 */
@Controller
@RequestMapping("/pagos")
@RequiredArgsConstructor
@Slf4j
public class PagoController {
    
    private final PagoService pagoService;
    private final FacturaService facturaService;
    private final ClienteService clienteService;
    private final PagoMapper pagoMapper;
    private final UsuarioService usuarioService;
    
    // ==================== VISTAS PRINCIPALES ====================
    
    /**
     * Lista todos los pagos con paginación y ordenamiento.
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_VER')")
    public String listarPagos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaPago") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) MetodoPago metodoPago,
            Model model,
            Authentication authentication
    ) {
        log.info("Listando pagos - Página: {}, Tamaño: {}, Ordenar: {} {}", 
            page, size, sortBy, sortDir);
        
        try {
            Pageable pageable = PaginacionUtil.buildPageable(page, size, sortBy, sortDir,
                    Set.of("idPago", "fechaPago", "monto", "estado", "metodoPago"), "fechaPago");

            // Obtener directamente Page<PagoDTO> sin conversión manual
            Page<PagoDTO> pagosDTOsPage = pagoService.findAllAsDTO(pageable);
            
            // Aplicar filtros si existen (TODO: implementar en query)
            if (estado != null || metodoPago != null) {
                log.warn("Filtros estado={} y metodoPago={} recibidos pero no aplicados aún", estado, metodoPago);
            }
            
            // Crear paginación directamente desde el Page<PagoDTO>
            PaginacionDTO<PagoDTO> paginacion = PaginacionUtil.fromPage(pagosDTOsPage);
            
            PaginacionUtil.agregarAtributosConOrdenamiento(model, paginacion, "pagos", sortBy, sortDir);
            
            // Agregar datos para filtros
            model.addAttribute("metodosPago", MetodoPago.values());
            model.addAttribute("estadosPago", EstadoPago.values());
            model.addAttribute("estadoSeleccionado", estado);
            model.addAttribute("metodoPagoSeleccionado", metodoPago);
            
            // Estadísticas del día
            model.addAttribute("ingresosDia", pagoService.sumIngresosDiaActual());
            model.addAttribute("cantidadPagosDia", pagoService.countPagosDelDia());
            
            log.info("Pagos cargados: {} de {} total",
                pagosDTOsPage.getNumberOfElements(), pagosDTOsPage.getTotalElements());
            
            return "modules/facturacion/pagos/listar";
            
        } catch (Exception e) {
            log.error("Error al listar pagos: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar los pagos");
            return "shared/error/error";
        }
    }
    
    /**
     * Muestra el formulario para registrar un nuevo pago.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_CREAR')")
    public String nuevoPago(
            @RequestParam(required = false) Integer idCliente,
            @RequestParam(required = false) Integer idFactura,
            Model model,
            Authentication authentication
    ) {
        log.info("Mostrando formulario de nuevo pago - Cliente: {}, Factura: {}", idCliente, idFactura);
        
        PagoDTO pagoDTO = new PagoDTO();
        pagoDTO.setFechaPago(LocalDateTime.now());
        pagoDTO.setEstado(EstadoPago.CONFIRMADO);
        pagoDTO.setTipoPago(TipoPago.TOTAL);  // Por defecto
        
        // Cargar cliente si se especifica
        if (idCliente != null) {
            Optional<Cliente> clienteOpt = clienteService.findById(idCliente);
            if (clienteOpt.isPresent()) {
                Cliente cliente = clienteOpt.get();
                pagoDTO.setIdCliente(idCliente);
                pagoDTO.setNombreCliente(cliente.getNombre());
            }
        }
        
        // Cargar factura si se especifica
        if (idFactura != null) {
            Optional<Factura> facturaOpt = facturaService.findById(idFactura);
            if (facturaOpt.isPresent()) {
                Factura factura = facturaOpt.get();
                pagoDTO.setIdFactura(idFactura);
                pagoDTO.setNumeroFactura(factura.getNumeroFactura());
                pagoDTO.setTotalFactura(factura.getTotal());
                pagoDTO.setSaldoPendiente(factura.calcularSaldoPendiente());
                
                // Asignar cliente desde factura si no se especificó
                if (idCliente == null && factura.getCliente() != null) {
                    pagoDTO.setIdCliente(factura.getCliente().getIdCliente());
                    pagoDTO.setNombreCliente(factura.getCliente().getNombre());
                }
            }
        }
        
        // Cargar clientes para el selector
        model.addAttribute("clientes", clienteService.findAll());
        model.addAttribute("pago", pagoDTO);
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("estadosPago", EstadoPago.values());
        model.addAttribute("tiposPago", TipoPago.values());
        
        return "modules/facturacion/pagos/form";
    }
    
    /**
     * Muestra el detalle de un pago.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_VER')")
    public String verDetallePago(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        log.info("Ver detalle de pago ID: {}", id);
        
        try {
            Optional<Pago> pagoOpt = pagoService.findById(id);
            
            if (pagoOpt.isEmpty()) {
                log.warn("Pago no encontrado con ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Pago no encontrado");
                return "redirect:/pagos";
            }
            
            PagoDTO pagoDTO = pagoMapper.toDTO(pagoOpt.get());
            model.addAttribute("pago", pagoDTO);

            return "modules/facturacion/pagos/detalle";
            
        } catch (Exception e) {
            log.error("Error al cargar detalle de pago: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar el pago");
            return "redirect:/pagos";
        }
    }
    
    // ==================== API REST ====================
    
    /**
     * Registra un nuevo pago (API REST).
     */
    @PostMapping("/registrar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_CREAR')")
    @ResponseBody
    public ResponseEntity<ResponseDTO> registrarPago(
            @Valid @RequestBody PagoDTO pagoDTO,
            BindingResult result,
            Authentication authentication
    ) {
        log.info("Registrando nuevo pago - Cliente: {}, Factura: {}, Tipo: {}, Monto: {}",
            pagoDTO.getIdCliente(), pagoDTO.getIdFactura(), pagoDTO.getTipoPago(), pagoDTO.getMonto());
        
        try {
            if (result.hasErrors()) {
                String errores = result.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Errores de validación");
                return ResponseEntity.badRequest()
                    .body(ResponseDTO.error(errores));
            }
            
            // Delegate entity resolution and registration to the service
            Pago pagoGuardado = pagoService.registrarPago(pagoDTO);
            PagoDTO responseDTO = pagoMapper.toDTO(pagoGuardado);
            
            log.info("Pago {} registrado exitosamente con ID: {}", 
                pagoGuardado.getNumeroPago(), pagoGuardado.getIdPago());
            return ResponseEntity.ok(
                ResponseDTO.success("Pago registrado exitosamente", responseDTO));
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al registrar pago: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ResponseDTO.error(e.getMessage()));
                
        } catch (Exception e) {
            log.error("Error al registrar pago: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ResponseDTO.error("Error al registrar el pago"));
        }
    }
    
    /**
     * Confirma un pago pendiente.
     */
    @PutMapping("/{id}/confirmar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_EDITAR')")
    @ResponseBody
    public ResponseEntity<ResponseDTO> confirmarPago(
            @PathVariable Long id,
            Authentication authentication
    ) {
        log.info("Confirmando pago ID: {}", id);
        
        try {
            Pago pagoConfirmado = pagoService.confirmarPago(id);
            PagoDTO responseDTO = pagoMapper.toDTO(pagoConfirmado);
            
            return ResponseEntity.ok(
                ResponseDTO.success("Pago confirmado exitosamente", responseDTO));
                
        } catch (IllegalArgumentException e) {
            log.warn("Error al confirmar pago: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ResponseDTO.error(e.getMessage()));
                
        } catch (Exception e) {
            log.error("Error al confirmar pago: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ResponseDTO.error("Error al confirmar el pago"));
        }
    }
    
    /**
     * Concilia un pago con el banco.
     */
    @PutMapping("/{id}/conciliar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_CONCILIAR')")
    @ResponseBody
    public ResponseEntity<ResponseDTO> conciliarPago(
            @PathVariable Long id,
            Authentication authentication
    ) {
        log.info("Conciliando pago ID: {}", id);
        
        try {
            Pago pagoConciliado = pagoService.conciliarPago(id);
            PagoDTO responseDTO = pagoMapper.toDTO(pagoConciliado);
            
            return ResponseEntity.ok(
                ResponseDTO.success("Pago conciliado exitosamente", responseDTO));
                
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al conciliar pago: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ResponseDTO.error(e.getMessage()));
                
        } catch (Exception e) {
            log.error("Error al conciliar pago: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ResponseDTO.error("Error al conciliar el pago"));
        }
    }
    
    /**
     * Rechaza un pago.
     */
    @PutMapping("/{id}/rechazar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_EDITAR')")
    @ResponseBody
    public ResponseEntity<ResponseDTO> rechazarPago(
            @PathVariable Long id,
            @RequestParam String motivo,
            Authentication authentication
    ) {
        log.info("Rechazando pago ID: {} - Motivo: {}", id, motivo);
        
        try {
            Pago pagoRechazado = pagoService.rechazarPago(id, motivo);
            PagoDTO responseDTO = pagoMapper.toDTO(pagoRechazado);
            
            return ResponseEntity.ok(
                ResponseDTO.success("Pago rechazado", responseDTO));
                
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al rechazar pago: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ResponseDTO.error(e.getMessage()));
                
        } catch (Exception e) {
            log.error("Error al rechazar pago: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ResponseDTO.error("Error al rechazar el pago"));
        }
    }
    
    /**
     * Anula un pago con trazabilidad completa.
     */
    @PutMapping("/{id}/anular")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_ANULAR')")
    @ResponseBody
    public ResponseEntity<ResponseDTO> anularPago(
            @PathVariable Long id,
            @RequestParam String motivo,
            Authentication authentication
    ) {
        log.info("Anulando pago ID: {} - Motivo: {} - Usuario: {}", id, motivo, authentication.getName());
        
        try {
            Integer usuarioId = usuarioService.findByTelefono(authentication.getName())
                .map(u -> u.getIdUsuario())
                .orElse(null);
            
            Pago pagoAnulado = pagoService.anularPago(id, motivo, usuarioId);
            PagoDTO responseDTO = pagoMapper.toDTO(pagoAnulado);
            
            return ResponseEntity.ok(
                ResponseDTO.success("Pago anulado exitosamente", responseDTO));
                
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al anular pago: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ResponseDTO.error(e.getMessage()));
                
        } catch (Exception e) {
            log.error("Error al anular pago: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ResponseDTO.error("Error al anular el pago"));
        }
    }
    
    /**
     * Elimina un pago.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_ELIMINAR')")
    @ResponseBody
    public ResponseEntity<ResponseDTO> eliminarPago(
            @PathVariable Long id,
            Authentication authentication
    ) {
        log.info("Eliminando pago ID: {}", id);
        
        try {
            pagoService.deleteById(id);
            
            return ResponseEntity.ok(
                ResponseDTO.success("Pago eliminado exitosamente"));
                
        } catch (Exception e) {
            log.error("Error al eliminar pago: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ResponseDTO.error("Error al eliminar el pago"));
        }
    }
    
    // ==================== CONSULTAS Y REPORTES ====================
    
    /**
     * Obtiene los pagos de una factura específica.
     */
    @GetMapping("/factura/{idFactura}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_VER')")
    @ResponseBody
    public ResponseEntity<List<PagoDTO>> obtenerPagosPorFactura(
            @PathVariable Integer idFactura,
            Authentication authentication
    ) {
        log.info("Obteniendo pagos de la factura: {}", idFactura);
        
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
     * Obtiene pagos pendientes de conciliación.
     */
    @GetMapping("/pendientes-conciliacion")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PAGO_CONCILIAR')")
    public String pagosPendientesConciliacion(Model model, Authentication authentication) {
        log.info("Obteniendo pagos pendientes de conciliación");
        
        try {
            List<Pago> pagosPendientes = pagoService.findPagosPendientesConciliacion();
            List<PagoDTO> pagosDTOs = pagoMapper.toDTOList(pagosPendientes);
            
            model.addAttribute("pagos", pagosDTOs);
            model.addAttribute("totalPendiente", 
                pagosPendientes.stream()
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            
            return "modules/facturacion/pagos/conciliacion";
            
        } catch (Exception e) {
            log.error("Error al obtener pagos pendientes: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar pagos pendientes");
            return "shared/error/error";
        }
    }
    
    /**
     * Genera reporte de caja del día actual.
     */
    @GetMapping("/reporte-caja")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'REPORTE_CAJA')")
    public String reporteCajaDiaria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model,
            Authentication authentication
    ) {
        LocalDate fechaReporte = fecha != null ? fecha : LocalDate.now();
        log.info("Generando reporte de caja para: {}", fechaReporte);
        
        try {
            ReporteCajaDTO reporte = pagoService.buildReporteCaja(fechaReporte);

            model.addAttribute("reporte", reporte);
            model.addAttribute("fechaReporte", fechaReporte);

            return "modules/facturacion/pagos/reporte-caja";
            
        } catch (Exception e) {
            log.error("Error al generar reporte de caja: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al generar el reporte");
            return "shared/error/error";
        }
    }
    
}

