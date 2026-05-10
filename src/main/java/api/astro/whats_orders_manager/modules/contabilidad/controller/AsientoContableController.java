package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.dto.AsientoContableDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.mapper.AsientoContableMapper;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.shared.service.MonedaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controlador para la gestión de asientos contables.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Controller
@RequestMapping("/contabilidad/asientos")
@RequiredArgsConstructor
@Slf4j
public class AsientoContableController {
    
    private final AsientoContableService asientoService;
    private final AsientoContableMapper asientoMapper;
    private final MonedaService monedaService;
    
    /**
     * Vista principal de asientos contables.
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    public String listar(Authentication authentication, Model model) {
        log.info("Accediendo a la vista de asientos contables");
        return "modules/contabilidad/asientos/listar";
    }
    
    /**
     * Vista del formulario para crear asiento.
     */
    @GetMapping("/crear")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_CREAR')")
    public String mostrarFormularioCrear(Authentication authentication, Model model) {
        log.info("Mostrando formulario de creación de asiento");
        
        model.addAttribute("esEdicion", false);
        model.addAttribute("tiposAsiento", TipoAsiento.values());
        model.addAttribute("fechaActual", LocalDate.now());
        model.addAttribute("simboloMoneda", monedaService.obtenerSimboloMoneda());
        
        return "modules/contabilidad/asientos/form";
    }
    
    /**
     * Vista del formulario para editar asiento.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_EDITAR')")
    public String mostrarFormularioEditar(Authentication authentication, @PathVariable Long id, Model model) {
        log.info("Mostrando formulario de edición de asiento ID: {}", id);
        
        AsientoContable asiento = asientoService.obtenerPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));
        
        if (!asiento.puedeModificarse()) {
            throw new IllegalArgumentException("El asiento no puede ser modificado en su estado actual");
        }
        
        model.addAttribute("asiento", asientoMapper.toDTO(asiento));
        model.addAttribute("tiposAsiento", TipoAsiento.values());
        model.addAttribute("esEdicion", true);
        model.addAttribute("simboloMoneda", monedaService.obtenerSimboloMoneda());
        
        return "modules/contabilidad/asientos/form";
    }
    
    /**
     * Vista de detalle de asiento.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    public String verDetalle(Authentication authentication, @PathVariable Long id, Model model) {
        log.info("Mostrando detalle de asiento ID: {}", id);
        
        AsientoContable asiento = asientoService.obtenerPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));
        
        model.addAttribute("asiento", asientoMapper.toDTO(asiento));
        
        return "modules/contabilidad/asientos/detalle";
    }
    
    // ============= API REST =============
    
    /**
     * Obtiene todos los asientos con paginación.
     */
    @GetMapping("/api")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<Page<AsientoContableDTO>> obtenerTodos(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("API: Obteniendo asientos - página: {}, tamaño: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        Page<AsientoContable> asientos = asientoService.obtenerTodos(pageable);
        Page<AsientoContableDTO> asientosDTO = asientos.map(asientoMapper::toDTOSimple);
        
        return ResponseEntity.ok(asientosDTO);
    }
    
    /**
     * Obtiene asientos por estado.
     */
    @GetMapping("/api/estado/{estado}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<Page<AsientoContableDTO>> obtenerPorEstado(
            Authentication authentication,
            @PathVariable EstadoAsiento estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("API: Obteniendo asientos con estado: {}", estado);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AsientoContable> asientos = asientoService.obtenerPorEstado(estado, pageable);
        Page<AsientoContableDTO> asientosDTO = asientos.map(asientoMapper::toDTOSimple);
        
        return ResponseEntity.ok(asientosDTO);
    }
    
    /**
     * Obtiene asientos en un rango de fechas.
     */
    @GetMapping("/api/periodo")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<AsientoContableDTO>> obtenerPorPeriodo(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.debug("API: Obteniendo asientos entre {} y {}", fechaInicio, fechaFin);
        
        List<AsientoContable> asientos = asientoService.obtenerPorPeriodo(fechaInicio, fechaFin);
        return ResponseEntity.ok(asientoMapper.toDTOSimpleList(asientos));
    }
    
    /**
     * Obtiene asientos relacionados con una factura.
     */
    @GetMapping("/api/factura/{facturaId}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<AsientoContableDTO>> obtenerPorFactura(
            Authentication authentication,
            @PathVariable Long facturaId) {
        log.debug("API: Obteniendo asientos de factura ID: {}", facturaId);
        
        List<AsientoContable> asientos = asientoService.obtenerPorFactura(facturaId);
        return ResponseEntity.ok(asientoMapper.toDTOList(asientos));
    }
    
    /**
     * Obtiene asientos relacionados con un pago.
     */
    @GetMapping("/api/pago/{pagoId}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<AsientoContableDTO>> obtenerPorPago(
            Authentication authentication,
            @PathVariable Long pagoId) {
        log.debug("API: Obteniendo asientos de pago ID: {}", pagoId);
        
        List<AsientoContable> asientos = asientoService.obtenerPorPago(pagoId);
        return ResponseEntity.ok(asientoMapper.toDTOList(asientos));
    }
    
    /**
     * Busca asientos por concepto.
     */
    @GetMapping("/api/buscar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<AsientoContableDTO>> buscar(
            Authentication authentication,
            @RequestParam String concepto) {
        log.debug("API: Buscando asientos con concepto: {}", concepto);
        
        List<AsientoContable> asientos = asientoService.buscarPorConcepto(concepto);
        return ResponseEntity.ok(asientoMapper.toDTOSimpleList(asientos));
    }
    
    /**
     * Obtiene un asiento por ID.
     */
    @GetMapping("/api/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<AsientoContableDTO> obtenerPorId(
            Authentication authentication,
            @PathVariable Long id) {
        log.debug("API: Obteniendo asiento ID: {}", id);
        
        AsientoContable asiento = asientoService.obtenerPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Asiento no encontrado"));
        
        return ResponseEntity.ok(asientoMapper.toDTO(asiento));
    }
    
    /**
     * Crea un nuevo asiento.
     */
    @PostMapping("/api")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_CREAR')")
    @ResponseBody
    public ResponseEntity<AsientoContableDTO> crear(
            Authentication authentication,
            @RequestBody AsientoContableDTO asientoDTO) {
        log.info("API: Creando nuevo asiento");
        
        AsientoContable asiento = asientoMapper.toEntity(asientoDTO);
        AsientoContable guardado = asientoService.crear(asiento);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(asientoMapper.toDTO(guardado));
    }
    
    /**
     * Actualiza un asiento existente.
     */
    @PutMapping("/api/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_EDITAR')")
    @ResponseBody
    public ResponseEntity<AsientoContableDTO> actualizar(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody AsientoContableDTO asientoDTO) {
        log.info("API: Actualizando asiento ID: {}", id);
        
        AsientoContable asiento = asientoMapper.toEntity(asientoDTO);
        AsientoContable actualizado = asientoService.actualizar(id, asiento);
        
        return ResponseEntity.ok(asientoMapper.toDTO(actualizado));
    }
    
    /**
     * Contabiliza un asiento (cambia a estado CONTABILIZADO).
     */
    @PostMapping("/api/{id}/contabilizar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_CONTABILIZAR')")
    @ResponseBody
    public ResponseEntity<AsientoContableDTO> contabilizar(
            Authentication authentication,
            @PathVariable Long id) {
        log.info("API: Contabilizando asiento ID: {}", id);
        
        AsientoContable asiento = asientoService.contabilizar(id);
        return ResponseEntity.ok(asientoMapper.toDTO(asiento));
    }
    
    /**
     * Anula un asiento contabilizado.
     */
    @PostMapping("/api/{id}/anular")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_ANULAR')")
    @ResponseBody
    public ResponseEntity<AsientoContableDTO> anular(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        log.info("API: Anulando asiento ID: {}", id);
        
        AsientoContable asiento = asientoService.anular(id, motivo);
        return ResponseEntity.ok(asientoMapper.toDTO(asiento));
    }
    
    /**
     * Elimina un asiento en borrador.
     */
    @DeleteMapping("/api/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_ELIMINAR')")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminar(
            Authentication authentication,
            @PathVariable Long id) {
        log.info("API: Eliminando asiento ID: {}", id);
        
        asientoService.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Asiento eliminado exitosamente"));
    }
    
    /**
     * Genera asiento automático desde una factura.
     */
    @PostMapping("/api/generar-venta")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_CREAR')")
    @ResponseBody
    public ResponseEntity<AsientoContableDTO> generarAsientoVenta(
            Authentication authentication,
            @RequestParam Long facturaId,
            @RequestParam Long cuentaClienteId,
            @RequestParam Long cuentaVentaId,
            @RequestParam Long cuentaIvaId) {
        log.info("API: Generando asiento de venta para factura ID: {}", facturaId);
        
        // Obtener factura del servicio correspondiente
        // AsientoContable asiento = asientoService.generarAsientoVenta(factura, cuentaClienteId, cuentaVentaId, cuentaIvaId);
        
        // Por ahora retornamos error indicando que se debe implementar
        return ResponseEntity.badRequest()
            .body(null);
    }
    
    /**
     * Genera asiento automático desde un pago.
     */
    @PostMapping("/api/generar-pago")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_CREAR')")
    @ResponseBody
    public ResponseEntity<AsientoContableDTO> generarAsientoPago(
            Authentication authentication,
            @RequestParam Long pagoId,
            @RequestParam Long cuentaClienteId,
            @RequestParam Long cuentaCajaId) {
        log.info("API: Generando asiento de pago para pago ID: {}", pagoId);
        
        // Obtener pago del servicio correspondiente
        // AsientoContable asiento = asientoService.generarAsientoPago(pago, cuentaClienteId, cuentaCajaId);
        
        // Por ahora retornamos error indicando que se debe implementar
        return ResponseEntity.badRequest()
            .body(null);
    }
    
    /**
     * Manejo de excepciones.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Error de validación: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(Map.of("error", e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("Error inesperado: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Error al procesar la solicitud: " + e.getMessage()));
    }
}

