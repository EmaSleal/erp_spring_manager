package api.astro.whats_orders_manager.modules.seguridad.controller;

import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.model.Rol;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.RolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * PERMISOS CONTROLLER
 * ERP Orders Manager
 * ============================================================================
 * Controlador para la gestión y visualización de permisos del sistema.
 * Muestra la matriz de permisos por rol y estadísticas.
 * Ahora usa la base de datos en lugar del enum.
 * Solo accesible por administradores.
 * 
 * @version 2.0 - Sprint 4 (Migrado a Base de Datos)
 * @since 23/12/2025
 * ============================================================================
 */
@Slf4j
@Controller
@RequestMapping("/admin/permisos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PermisosController {
    
    private final PermisoService permisoService;
    private final RolService rolService;
    
    /**
     * Vista principal de la matriz de permisos
     */
    @GetMapping
    public String mostrarMatrizPermisos(Model model, Authentication authentication) {
        log.info("Mostrando matriz de permisos desde BD - Usuario: {}", authentication.getName());
        
        try {
            // Obtener todos los roles desde la BD
            List<Rol> roles = rolService.obtenerTodos();
            model.addAttribute("roles", roles);
            log.debug("Roles cargados desde BD: {}", roles.size());
            
            // Crear mapa de roles por código para fácil acceso en Thymeleaf
            Map<String, Rol> rolesPorCodigo = roles.stream()
                .collect(Collectors.toMap(Rol::getCodigo, rol -> rol, (a, b) -> a, LinkedHashMap::new));
            model.addAttribute("rolesPorCodigo", rolesPorCodigo);
            
            // Obtener todos los permisos disponibles agrupados por categoría
            Map<String, List<Permiso>> permisosPorCategoria = obtenerPermisosPorCategoria();
            model.addAttribute("permisosPorCategoria", permisosPorCategoria);
            log.debug("Categorías cargadas: {}", permisosPorCategoria.size());
            
            // Crear matriz de permisos [Permiso][Rol] -> boolean
            Map<Long, Map<Long, Boolean>> matrizPermisos = crearMatrizPermisos(roles);
            model.addAttribute("matrizPermisos", matrizPermisos);
            log.debug("Matriz creada con {} permisos", matrizPermisos.size());
            
            // Estadísticas
            Map<String, Object> estadisticas = calcularEstadisticas(roles);
            model.addAttribute("estadisticas", estadisticas);
            
            log.info("Matriz de permisos cargada exitosamente desde BD - {} roles", roles.size());
            
            return "modules/seguridad/permisos/matriz";
            
        } catch (Exception e) {
            log.error("Error al cargar matriz de permisos: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar la matriz de permisos");
            return "shared/error/error";
        }
    }
    
    /**
     * API REST - Obtener matriz completa de permisos desde BD
     */
    @GetMapping("/api/matriz")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerMatrizAPI() {
        log.info("Obteniendo matriz de permisos desde BD vía API");
        
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            
            List<Rol> roles = rolService.obtenerTodos();
            Map<Long, Map<Long, Boolean>> matriz = crearMatrizPermisos(roles);
            Map<String, List<Permiso>> permisosPorCategoria = obtenerPermisosPorCategoria();
            
            long totalPermisos = permisosPorCategoria.values().stream()
                .mapToLong(List::size)
                .sum();
            
            response.put("roles", roles.stream()
                .map(r -> {
                    Map<String, Object> rolData = new HashMap<>();
                    rolData.put("id", r.getIdRol());
                    rolData.put("codigo", r.getCodigo());
                    rolData.put("nombre", r.getNombre());
                    return rolData;
                })
                .collect(Collectors.toList()));
            response.put("matriz", matriz);
            response.put("permisosPorCategoria", permisosPorCategoria);
            response.put("totalPermisos", totalPermisos);
            response.put("totalRoles", roles.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener matriz de permisos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * API REST - Obtener permisos de un rol específico desde BD
     */
    @GetMapping("/api/rol/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerPermisosRol(@PathVariable Long id) {
        log.info("Obteniendo permisos del rol ID: {}", id);
        
        try {
            Set<Permiso> permisos = rolService.obtenerPermisos(id);
            
            Optional<Rol> rolOpt = rolService.buscarPorId(id);
            if (rolOpt.isEmpty()) {
                log.warn("Rol no encontrado con ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Rol rol = rolOpt.get();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", rol.getIdRol());
            response.put("codigo", rol.getCodigo());
            response.put("nombre", rol.getNombre());
            response.put("totalPermisos", permisos.size());
            response.put("permisos", permisos.stream()
                .map(p -> {
                    Map<String, Object> permisoData = new HashMap<>();
                    permisoData.put("id", p.getIdPermiso());
                    permisoData.put("codigo", p.getCodigo());
                    permisoData.put("nombre", p.getNombre());
                    permisoData.put("descripcion", p.getDescripcion());
                    permisoData.put("categoria", p.getCategoria());
                    permisoData.put("critico", p.getEsCritico());
                    return permisoData;
                })
                .collect(Collectors.toList()));
            
            // Permisos agrupados por categoría
            Map<String, List<Permiso>> porCategoria = permisos.stream()
                .collect(Collectors.groupingBy(
                    Permiso::getCategoria,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));
            response.put("permisosPorCategoria", porCategoria);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener permisos del rol: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * API REST - Verificar si un rol tiene un permiso específico (por códigos)
     */
    @GetMapping("/api/verificar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarPermiso(
            @RequestParam String codigoRol,
            @RequestParam String codigoPermiso
    ) {
        log.info("Verificando permiso {} para rol {}", codigoPermiso, codigoRol);
        
        try {
            boolean tiene = rolService.tienePermiso(codigoRol, codigoPermiso);
            
            Optional<Rol> rolOpt = rolService.buscarPorCodigo(codigoRol);
            Optional<Permiso> permisoOpt = permisoService.buscarPorCodigo(codigoPermiso);
            
            if (rolOpt.isEmpty() || permisoOpt.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            Permiso permiso = permisoOpt.get();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rol", codigoRol);
            response.put("permiso", codigoPermiso);
            response.put("tiene", tiene);
            response.put("categoria", permiso.getCategoria());
            response.put("critico", permiso.getEsCritico());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al verificar permiso: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * API REST - Obtener estadísticas del sistema de permisos desde BD
     */
    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        log.info("Obteniendo estadísticas del sistema de permisos desde BD");
        
        try {
            List<Rol> roles = rolService.obtenerTodos();
            Map<String, Object> estadisticas = calcularEstadisticas(roles);
            return ResponseEntity.ok(estadisticas);
            
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * API REST - Comparar permisos entre dos roles (por ID)
     */
    @GetMapping("/api/comparar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> compararRoles(
            @RequestParam Long idRol1,
            @RequestParam Long idRol2
    ) {
        log.info("Comparando permisos entre roles {} y {}", idRol1, idRol2);
        
        try {
            Optional<Rol> rol1Opt = rolService.buscarPorId(idRol1);
            Optional<Rol> rol2Opt = rolService.buscarPorId(idRol2);
            
            if (rol1Opt.isEmpty() || rol2Opt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Set<Permiso> permisos1 = rolService.obtenerPermisos(idRol1);
            Set<Permiso> permisos2 = rolService.obtenerPermisos(idRol2);
            
            // Permisos en común
            Set<Permiso> comunes = new HashSet<>(permisos1);
            comunes.retainAll(permisos2);
            
            // Solo en rol1
            Set<Permiso> soloRol1 = new HashSet<>(permisos1);
            soloRol1.removeAll(permisos2);
            
            // Solo en rol2
            Set<Permiso> soloRol2 = new HashSet<>(permisos2);
            soloRol2.removeAll(permisos1);
            
            Rol rol1 = rol1Opt.get();
            Rol rol2 = rol2Opt.get();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("rol1", Map.of("id", rol1.getIdRol(), "codigo", rol1.getCodigo(), "nombre", rol1.getNombre()));
            response.put("rol2", Map.of("id", rol2.getIdRol(), "codigo", rol2.getCodigo(), "nombre", rol2.getNombre()));
            response.put("totalPermisos1", permisos1.size());
            response.put("totalPermisos2", permisos2.size());
            response.put("permisosEnComun", comunes.size());
            response.put("soloEnRol1", soloRol1.size());
            response.put("soloEnRol2", soloRol2.size());
            response.put("comunes", comunes.stream()
                .map(Permiso::getNombre)
                .collect(Collectors.toList()));
            response.put("soloEn" + rol1.getCodigo(), soloRol1.stream()
                .map(Permiso::getNombre)
                .collect(Collectors.toList()));
            response.put("soloEn" + rol2.getCodigo(), soloRol2.stream()
                .map(Permiso::getNombre)
                .collect(Collectors.toList()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al comparar roles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtiene todos los permisos agrupados por categoría desde BD
     */
    private Map<String, List<Permiso>> obtenerPermisosPorCategoria() {
        List<Permiso> todosPermisos = permisoService.obtenerTodosActivos();
        
        Map<String, List<Permiso>> permisosPorCategoria = todosPermisos.stream()
            .collect(Collectors.groupingBy(
                Permiso::getCategoria,
                LinkedHashMap::new,
                Collectors.toList()
            ));
        
        // Ordenar permisos dentro de cada categoría por nombre
        permisosPorCategoria.values().forEach(lista -> 
            lista.sort(Comparator.comparing(Permiso::getNombre))
        );
        
        return permisosPorCategoria;
    }
    
    /**
     * Crea la matriz de permisos [IdPermiso][IdRol] -> boolean
     */
    private Map<Long, Map<Long, Boolean>> crearMatrizPermisos(List<Rol> roles) {
        Map<Long, Map<Long, Boolean>> matriz = new LinkedHashMap<>();
        
        List<Permiso> todosPermisos = permisoService.obtenerTodosActivos();
        
        for (Permiso permiso : todosPermisos) {
            Map<Long, Boolean> permisosPorRol = new LinkedHashMap<>();
            
            for (Rol rol : roles) {
                boolean tiene = rol.getPermisos().contains(permiso);
                permisosPorRol.put(rol.getIdRol(), tiene);
            }
            
            matriz.put(permiso.getIdPermiso(), permisosPorRol);
        }
        
        return matriz;
    }
    
    /**
     * Calcula estadísticas del sistema de permisos
     */
    private Map<String, Object> calcularEstadisticas(List<Rol> roles) {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        List<Permiso> todosPermisos = permisoService.obtenerTodosActivos();
        long permisosCriticos = todosPermisos.stream()
            .filter(Permiso::getEsCritico)
            .count();
        
        Map<String, Long> permisosPorCategoria = todosPermisos.stream()
            .collect(Collectors.groupingBy(
                Permiso::getCategoria,
                Collectors.counting()
            ));
        
        // Calcular permisos por rol (usando el código del rol como clave)
        Map<String, Long> permisosPorRol = new LinkedHashMap<>();
        for (Rol rol : roles) {
            long cantidadPermisos = rol.getPermisos() != null ? rol.getPermisos().size() : 0;
            permisosPorRol.put(rol.getCodigo(), cantidadPermisos);
        }
        
        stats.put("totalPermisos", todosPermisos.size());
        stats.put("permisosCriticos", permisosCriticos);
        stats.put("totalRoles", roles.size());
        stats.put("rolesActivos", roles.stream().filter(Rol::getActivo).count());
        stats.put("permisosPorCategoria", permisosPorCategoria);
        stats.put("categorias", permisosPorCategoria.keySet());
        stats.put("permisosPorRol", permisosPorRol);
        stats.put("totalPermisosEnSistema", todosPermisos.size());
        
        return stats;
    }
}