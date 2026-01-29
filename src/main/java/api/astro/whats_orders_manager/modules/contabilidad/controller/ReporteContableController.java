package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.service.CalculoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.CalculoContableService.MovimientoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.service.CalculoContableService.SaldoCuenta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controlador para reportes contables y estados financieros.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Controller
@RequestMapping("/contabilidad/reportes")
@RequiredArgsConstructor
@Slf4j
public class ReporteContableController {
    
    private final CalculoContableService calculoService;
    
    /**
     * Vista principal de reportes contables.
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_REPORTES')")
    public String index(Authentication authentication, Model model) {
        log.info("Accediendo a la vista de reportes contables");
        
        // Fechas por defecto (mes actual)
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        
        model.addAttribute("fechaInicio", inicioMes);
        model.addAttribute("fechaFin", hoy);
        
        return "modules/contabilidad/reportes/index";
    }
    
    /**
     * Vista del libro mayor.
     */
    @GetMapping("/libro-mayor")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_REPORTES')")
    public String libroMayor(Authentication authentication, @RequestParam Long cuentaId,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                            Model model) {
        log.info("Generando libro mayor para cuenta ID: {}", cuentaId);
        
        // Si no se especifican fechas, usar mes actual
        if (fechaInicio == null || fechaFin == null) {
            LocalDate hoy = LocalDate.now();
            fechaInicio = hoy.withDayOfMonth(1);
            fechaFin = hoy;
        }
        
        List<MovimientoCuenta> movimientos = calculoService.obtenerLibroMayor(cuentaId, fechaInicio, fechaFin);
        
        model.addAttribute("cuentaId", cuentaId);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("movimientos", movimientos);
        
        return "modules/contabilidad/reportes/libro-mayor";
    }
    
    /**
     * Vista del balance de comprobación.
     */
    @GetMapping("/balance-comprobacion")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_REPORTES')")
    public String balanceComprobacion(Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Model model) {
        log.info("Generando balance de comprobación");
        
        // Si no se especifican fechas, usar mes actual
        if (fechaInicio == null || fechaFin == null) {
            LocalDate hoy = LocalDate.now();
            fechaInicio = hoy.withDayOfMonth(1);
            fechaFin = hoy;
        }
        
        List<SaldoCuenta> saldos = calculoService.generarBalanceComprobacion(fechaInicio, fechaFin);
        
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("saldos", saldos);
        
        return "modules/contabilidad/reportes/balance-comprobacion";
    }
    
    /**
     * Vista del balance general.
     */
    @GetMapping("/balance-general")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_REPORTES')")
    public String balanceGeneral(Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {
        log.info("Generando balance general");
        
        // Si no se especifica fecha, usar hoy
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        
        Map<String, Object> balance = calculoService.generarBalanceGeneral(fecha);
        
        model.addAttribute("fecha", fecha);
        model.addAttribute("balance", balance);
        
        return "modules/contabilidad/reportes/balance-general";
    }
    
    /**
     * Vista del estado de resultados.
     */
    @GetMapping("/estado-resultados")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_REPORTES')")
    public String estadoResultados(Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Model model) {
        log.info("Generando estado de resultados");
        
        // Si no se especifican fechas, usar mes actual
        if (fechaInicio == null || fechaFin == null) {
            LocalDate hoy = LocalDate.now();
            fechaInicio = hoy.withDayOfMonth(1);
            fechaFin = hoy;
        }
        
        Map<String, Object> estado = calculoService.generarEstadoResultados(fechaInicio, fechaFin);
        
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("estado", estado);
        
        return "modules/contabilidad/reportes/estado-resultados";
    }
    
    // ============= API REST =============
    
    /**
     * Calcula el saldo de una cuenta.
     */
    @GetMapping("/api/saldo/{cuentaId}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, '')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calcularSaldo(
            @PathVariable Long cuentaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        log.debug("API: Calculando saldo de cuenta {} al {}", cuentaId, fecha);
        
        BigDecimal saldo = calculoService.calcularSaldoCuenta(cuentaId, fecha);
        
        return ResponseEntity.ok(Map.of(
            "cuentaId", cuentaId,
            "fecha", fecha,
            "saldo", saldo
        ));
    }
    
    /**
     * Obtiene el libro mayor de una cuenta.
     */
    @GetMapping("/api/libro-mayor/{cuentaId}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, '')")
    @ResponseBody
    public ResponseEntity<List<MovimientoCuenta>> obtenerLibroMayor(
            @PathVariable Long cuentaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.debug("API: Obteniendo libro mayor de cuenta {} entre {} y {}", cuentaId, fechaInicio, fechaFin);
        
        List<MovimientoCuenta> movimientos = calculoService.obtenerLibroMayor(cuentaId, fechaInicio, fechaFin);
        return ResponseEntity.ok(movimientos);
    }
    
    /**
     * Genera el balance de comprobación.
     */
    @GetMapping("/api/balance-comprobacion")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, '')")
    @ResponseBody
    public ResponseEntity<List<SaldoCuenta>> obtenerBalanceComprobacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.debug("API: Generando balance de comprobación entre {} y {}", fechaInicio, fechaFin);
        
        List<SaldoCuenta> saldos = calculoService.generarBalanceComprobacion(fechaInicio, fechaFin);
        return ResponseEntity.ok(saldos);
    }
    
    /**
     * Genera el balance general.
     */
    @GetMapping("/api/balance-general")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, '')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerBalanceGeneral(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        log.debug("API: Generando balance general al {}", fecha);
        
        Map<String, Object> balance = calculoService.generarBalanceGeneral(fecha);
        return ResponseEntity.ok(balance);
    }
    
    /**
     * Genera el estado de resultados.
     */
    @GetMapping("/api/estado-resultados")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, '')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEstadoResultados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.debug("API: Generando estado de resultados entre {} y {}", fechaInicio, fechaFin);
        
        Map<String, Object> estado = calculoService.generarEstadoResultados(fechaInicio, fechaFin);
        return ResponseEntity.ok(estado);
    }
    
    /**
     * Calcula la utilidad del ejercicio.
     */
    @GetMapping("/api/utilidad")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, '')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calcularUtilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        log.debug("API: Calculando utilidad entre {} y {}", fechaInicio, fechaFin);
        
        BigDecimal utilidad = calculoService.calcularUtilidadDelEjercicio(fechaInicio, fechaFin);
        
        return ResponseEntity.ok(Map.of(
            "fechaInicio", fechaInicio,
            "fechaFin", fechaFin,
            "utilidad", utilidad
        ));
    }
    
    /**
     * Obtiene estadísticas generales de contabilidad.
     */
    @GetMapping("/api/estadisticas")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, '')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        log.debug("API: Obteniendo estadísticas contables");
        
        Map<String, Object> stats = calculoService.obtenerEstadisticas();
        return ResponseEntity.ok(stats);
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

