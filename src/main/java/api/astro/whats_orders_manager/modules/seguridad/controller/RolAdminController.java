package api.astro.whats_orders_manager.modules.seguridad.controller;

import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.model.Rol;
import api.astro.whats_orders_manager.modules.seguridad.repository.PermisoRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.RolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * CONTROLADOR DE GESTIÓN DE ROLES
 * WhatsApp Orders Manager
 * ============================================================================
 * Gestiona la administración de roles y sus permisos.
 * Solo accesible para usuarios con permisos de ADMIN.
 * 
 * @version 1.0 - Sprint 4 Fase 4.6
 * @since 23/12/2025
 * ============================================================================
 */
@Slf4j
@Controller
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RolAdminController {
    
    private final RolService rolService;
    private final PermisoRepository permisoRepository;
    
    /**
     * Muestra la lista de roles
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listarRoles(Model model) {
        log.info("Accediendo a gestión de roles");
        
        try {
            List<Rol> roles = rolService.obtenerTodos();
            
            // Calcular estadísticas manualmente para evitar problemas de lazy loading
            Map<String, Long> estadisticasMap = new HashMap<>();
            long rolesActivos = 0;
            
            for (Rol rol : roles) {
                try {
                    // Obtener el tamaño de usuarios sin cargar la colección completa
                    long cantidadUsuarios = rol.getUsuarios() != null ? rol.getUsuarios().size() : 0;
                    estadisticasMap.put(rol.getNombre(), cantidadUsuarios);
                    
                    // Contar roles activos
                    if (rol.getActivo()) {
                        rolesActivos++;
                    }
                } catch (Exception e) {
                    log.warn("Error al contar usuarios del rol {}: {}", rol.getNombre(), e.getMessage());
                    estadisticasMap.put(rol.getNombre(), 0L);
                }
            }
            
            model.addAttribute("roles", roles);
            model.addAttribute("estadisticas", estadisticasMap);
            model.addAttribute("totalRoles", roles.size());
            model.addAttribute("rolesActivos", rolesActivos);
            model.addAttribute("titulo", "Gestión de Roles");
            
            return "modules/seguridad/admin/roles/roles";
            
        } catch (Exception e) {
            log.error("Error al cargar roles: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar la lista de roles: " + e.getMessage());
            return "shared/error/error";
        }
    }
    
    /**
     * Muestra el formulario para crear un nuevo rol
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMIN')")
    public String mostrarFormularioNuevo(Model model) {
        log.info("Mostrando formulario de nuevo rol");
        
        List<Permiso> todosLosPermisos = permisoRepository.findByActivoTrue();
        Map<String, List<Permiso>> permisosPorCategoria = todosLosPermisos.stream()
            .collect(Collectors.groupingBy(Permiso::getCategoria));
        
        model.addAttribute("permisosPorCategoria", permisosPorCategoria);
        model.addAttribute("titulo", "Crear Nuevo Rol");
        model.addAttribute("accion", "crear");
        
        return "modules/seguridad/admin/roles/formulario";
    }
    
    /**
     * Muestra el formulario para editar un rol existente
     */
    @GetMapping("/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        log.info("Mostrando formulario de edición de rol ID: {}", id);
        
        Rol rol = rolService.buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));
        
        List<Permiso> todosLosPermisos = permisoRepository.findByActivoTrue();
        Map<String, List<Permiso>> permisosPorCategoria = todosLosPermisos.stream()
            .collect(Collectors.groupingBy(Permiso::getCategoria));
        
        Set<Long> idsPermisosAsignados = rol.getPermisos().stream()
            .map(Permiso::getIdPermiso)
            .collect(Collectors.toSet());
        
        model.addAttribute("rol", rol);
        model.addAttribute("permisosPorCategoria", permisosPorCategoria);
        model.addAttribute("idsPermisosAsignados", idsPermisosAsignados);
        model.addAttribute("titulo", "Editar Rol: " + rol.getNombre());
        model.addAttribute("accion", "editar");
        
        return "modules/seguridad/admin/roles/formulario";
    }
    
    /**
     * Crea un nuevo rol
     */
    @PostMapping("/crear")
    @PreAuthorize("hasRole('ADMIN')")
    public String crearRol(
            @RequestParam String codigo,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) List<Long> permisos) {
        
        log.info("Creando nuevo rol: {}", codigo);
        
        try {
            // Crear el rol
            Rol nuevoRol = rolService.crearRol(codigo.toUpperCase(), nombre, descripcion);
            
            // Asignar permisos si se seleccionaron
            if (permisos != null && !permisos.isEmpty()) {
                rolService.asignarPermisos(nuevoRol.getIdRol(), new HashSet<>(permisos));
            }
            
            log.info("Rol creado exitosamente: {}", codigo);
            return "redirect:/admin/roles?success=created";
            
        } catch (Exception e) {
            log.error("Error al crear rol: {}", e.getMessage());
            return "redirect:/admin/roles/nuevo?error=" + e.getMessage();
        }
    }
    
    /**
     * Actualiza un rol existente
     */
    @PostMapping("/actualizar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String actualizarRol(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) List<Long> permisos) {
        
        log.info("Actualizando rol ID: {}", id);
        
        try {
            // Actualizar información del rol
            rolService.actualizarRol(id, nombre, descripcion);
            
            // Actualizar permisos
            Set<Long> permisosSet = permisos != null ? new HashSet<>(permisos) : new HashSet<>();
            rolService.asignarPermisos(id, permisosSet);
            
            log.info("Rol actualizado exitosamente ID: {}", id);
            return "redirect:/admin/roles?success=updated";
            
        } catch (Exception e) {
            log.error("Error al actualizar rol: {}", e.getMessage());
            return "redirect:/admin/roles/editar/" + id + "?error=" + e.getMessage();
        }
    }
    
    /**
     * Cambia el estado activo/inactivo de un rol
     */
    @PostMapping("/cambiar-estado/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam boolean activo) {
        log.info("Cambiando estado de rol ID: {} a {}", id, activo);
        
        try {
            Rol rol = rolService.cambiarEstado(id, activo);
            
            Map<String, Object> rolData = new HashMap<>();
            rolData.put("id", rol.getIdRol());
            rolData.put("codigo", rol.getCodigo());
            rolData.put("activo", rol.getActivo());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estado del rol actualizado");
            response.put("rol", rolData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al cambiar estado del rol: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Obtiene los permisos de un rol (AJAX)
     */
    @GetMapping("/{id}/permisos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<?> obtenerPermisos(@PathVariable Long id) {
        log.debug("Obteniendo permisos del rol ID: {}", id);
        
        try {
            Set<Permiso> permisos = rolService.obtenerPermisos(id);
            
            List<Map<String, Object>> permisosDto = permisos.stream()
                .map(p -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", p.getIdPermiso());
                    map.put("codigo", p.getCodigo());
                    map.put("nombre", p.getNombre());
                    map.put("categoria", p.getCategoria());
                    map.put("critico", p.getEsCritico());
                    return map;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("permisos", permisosDto);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener permisos: {}", e.getMessage());
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
