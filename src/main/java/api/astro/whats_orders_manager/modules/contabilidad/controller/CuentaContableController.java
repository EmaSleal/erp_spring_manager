package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaContableDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.mapper.CuentaContableMapper;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaContableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para la gestión del plan de cuentas contables.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 2
 */
@Controller
@RequestMapping("/contabilidad/cuentas")
@RequiredArgsConstructor
@Slf4j
public class CuentaContableController {
    
    private final CuentaContableService cuentaService;
    private final CuentaContableMapper cuentaMapper;
    
    /**
     * Vista principal del plan de cuentas.
     */
    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    public String listar(Authentication authentication, Model model) {
        log.info("Accediendo a la vista de plan de cuentas");
        return "modules/contabilidad/cuentas/listar";
    }
    
    /**
     * Vista del formulario para crear cuenta.
     */
    @GetMapping("/crear")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_CREAR')")
    public String mostrarFormularioCrear(Authentication authentication, @RequestParam(required = false) Long cuentaPadreId, Model model) {
        log.info("Mostrando formulario de creación de cuenta");
        
        if (cuentaPadreId != null) {
            cuentaService.obtenerPorId(cuentaPadreId).ifPresent(padre -> {
                model.addAttribute("cuentaPadre", cuentaMapper.toDTO(padre));
                model.addAttribute("siguienteCodigo", cuentaService.generarSiguienteCodigo(padre.getCodigo()));
            });
        } else {
            model.addAttribute("siguienteCodigo", cuentaService.generarSiguienteCodigo(null));
        }
        
        model.addAttribute("tiposCuenta", TipoCuenta.values());
        return "modules/contabilidad/cuentas/form";
    }
    
    /**
     * Vista del formulario para editar cuenta.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_EDITAR')")
    public String mostrarFormularioEditar(Authentication authentication, @PathVariable Long id, Model model) {
        log.info("Mostrando formulario de edición de cuenta ID: {}", id);
        
        CuentaContable cuenta = cuentaService.obtenerPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        
        model.addAttribute("cuenta", cuentaMapper.toDTOExtendido(cuenta, false));
        model.addAttribute("tiposCuenta", TipoCuenta.values());
        model.addAttribute("esEdicion", true);
        
        return "modules/contabilidad/cuentas/form";
    }
    
    /**
     * Vista de detalle de cuenta.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    public String verDetalle(Authentication authentication, @PathVariable Long id, Model model) {
        log.info("Mostrando detalle de cuenta ID: {}", id);
        
        CuentaContable cuenta = cuentaService.obtenerPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        
        model.addAttribute("cuenta", cuentaMapper.toDTOExtendido(cuenta, true));
        model.addAttribute("subcuentas", cuentaMapper.toDTOList(cuentaService.obtenerSubcuentas(id)));
        model.addAttribute("ruta", cuentaMapper.toDTOList(cuentaService.obtenerRutaCuenta(id)));
        model.addAttribute("tieneMovimientos", cuentaService.tieneMovimientos(id));
        
        return "modules/contabilidad/cuentas/detalle";
    }
    
    // ============= API REST =============
    
    /**
     * Obtiene todas las cuentas activas.
     */
    @GetMapping("/api/todas")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<CuentaContableDTO>> obtenerTodas(Authentication authentication) {
        log.debug("API: Obteniendo todas las cuentas");
        List<CuentaContable> cuentas = cuentaService.obtenerActivas();
        return ResponseEntity.ok(cuentaMapper.toDTOList(cuentas));
    }
    
    /**
     * Obtiene las cuentas raíz.
     */
    @GetMapping("/api/raiz")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<CuentaContableDTO>> obtenerCuentasRaiz(Authentication authentication) {
        log.debug("API: Obteniendo cuentas raíz");
        List<CuentaContable> cuentas = cuentaService.obtenerCuentasRaiz();
        return ResponseEntity.ok(cuentaMapper.toDTOList(cuentas));
    }
    
    /**
     * Obtiene las subcuentas de una cuenta.
     */
    @GetMapping("/api/{id}/subcuentas")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<CuentaContableDTO>> obtenerSubcuentas(Authentication authentication, @PathVariable Long id) {
        log.debug("API: Obteniendo subcuentas de cuenta ID: {}", id);
        List<CuentaContable> subcuentas = cuentaService.obtenerSubcuentas(id);
        return ResponseEntity.ok(cuentaMapper.toDTOList(subcuentas));
    }
    
    /**
     * Obtiene cuentas por tipo.
     */
    @GetMapping("/api/tipo/{tipo}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<CuentaContableDTO>> obtenerPorTipo(Authentication authentication, @PathVariable TipoCuenta tipo) {
        log.debug("API: Obteniendo cuentas de tipo: {}", tipo);
        List<CuentaContable> cuentas = cuentaService.obtenerPorTipo(tipo);
        return ResponseEntity.ok(cuentaMapper.toDTOList(cuentas));
    }
    
    /**
     * Obtiene cuentas operativas (que aceptan movimientos).
     */
    @GetMapping("/api/operativas")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<CuentaContableDTO>> obtenerOperativas(Authentication authentication) {
        log.debug("API: Obteniendo cuentas operativas");
        List<CuentaContable> cuentas = cuentaService.obtenerCuentasOperativas();
        return ResponseEntity.ok(cuentaMapper.toDTOList(cuentas));
    }
    
    /**
     * Obtiene cuentas activas que aceptan movimientos (alias de operativas).
     */
    @GetMapping("/api/activas-movimientos")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<CuentaContableDTO>> obtenerActivasMovimientos(Authentication authentication) {
        log.debug("API: Obteniendo cuentas activas que aceptan movimientos");
        List<CuentaContable> cuentas = cuentaService.obtenerCuentasOperativas();
        return ResponseEntity.ok(cuentaMapper.toDTOList(cuentas));
    }
    
    /**
     * Busca cuentas por código o nombre.
     */
    @GetMapping("/api/buscar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<List<CuentaContableDTO>> buscar(Authentication authentication, @RequestParam String termino) {
        log.debug("API: Buscando cuentas con término: {}", termino);
        List<CuentaContable> cuentas = cuentaService.buscarCuentas(termino);
        return ResponseEntity.ok(cuentaMapper.toDTOList(cuentas));
    }
    
    /**
     * Obtiene una cuenta por ID.
     */
    @GetMapping("/api/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<CuentaContableDTO> obtenerPorId(Authentication authentication, @PathVariable Long id) {
        log.debug("API: Obteniendo cuenta ID: {}", id);
        CuentaContable cuenta = cuentaService.obtenerPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        return ResponseEntity.ok(cuentaMapper.toDTOExtendido(cuenta, true));
    }
    
    /**
     * Crea una nueva cuenta.
     */
    @PostMapping("/api")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_CREAR')")
    @ResponseBody
    public ResponseEntity<CuentaContableDTO> crear(Authentication authentication, @RequestBody CuentaContableDTO cuentaDTO) {
        log.info("API: Creando nueva cuenta: {}", cuentaDTO.getCodigo());
        
        CuentaContable cuenta = cuentaMapper.toEntity(cuentaDTO);
        CuentaContable guardada = cuentaService.crear(cuenta);

        log.info("Cuenta creada con ID: {}", guardada.getIdCuenta());
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(cuentaMapper.toDTO(guardada));
    }
    
    /**
     * Actualiza una cuenta existente.
     */
    @PutMapping("/api/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_EDITAR')")
    @ResponseBody
    public ResponseEntity<CuentaContableDTO> actualizar(Authentication authentication, @PathVariable Long id, 
                                                        @RequestBody CuentaContableDTO cuentaDTO) {
        log.info("API: Actualizando cuenta ID: {}", id);
        
        CuentaContable cuenta = cuentaMapper.toEntity(cuentaDTO);
        CuentaContable actualizada = cuentaService.actualizar(id, cuenta);
        
        return ResponseEntity.ok(cuentaMapper.toDTO(actualizada));
    }
    
    /**
     * Cambia el estado de una cuenta (activa/inactiva).
     */
    @PatchMapping("/api/{id}/estado")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_EDITAR')")
    @ResponseBody
    public ResponseEntity<CuentaContableDTO> cambiarEstado(Authentication authentication, @PathVariable Long id, 
                                                           @RequestParam boolean activa) {
        log.info("API: Cambiando estado de cuenta ID: {} a {}", id, activa);
        
        CuentaContable actualizada = cuentaService.cambiarEstado(id, activa);
        return ResponseEntity.ok(cuentaMapper.toDTO(actualizada));
    }
    
    /**
     * Elimina una cuenta.
     */
    @DeleteMapping("/api/{id}")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_ELIMINAR')")
    @ResponseBody
    public ResponseEntity<Map<String, String>> eliminar(Authentication authentication, @PathVariable Long id) {
        log.info("API: Eliminando cuenta ID: {}", id);
        
        cuentaService.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta eliminada exitosamente"));
    }
    
    /**
     * Genera el siguiente código disponible.
     */
    @GetMapping("/api/siguiente-codigo")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generarSiguienteCodigo(Authentication authentication,
            @RequestParam(required = false) String codigoPadre) {
        log.debug("API: Generando siguiente código para padre: {}", codigoPadre);
        
        String codigo = cuentaService.generarSiguienteCodigo(codigoPadre);
        return ResponseEntity.ok(Map.of("codigo", codigo));
    }
    
    /**
     * Valida si un código es válido.
     */
    @GetMapping("/api/validar-codigo")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'CONTABILIDAD_VER')")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> validarCodigo(Authentication authentication, @RequestParam String codigo) {
        log.debug("API: Validando código: {}", codigo);
        
        boolean valido = cuentaService.esCodigoValido(codigo);
        return ResponseEntity.ok(Map.of("valido", valido));
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
