package api.astro.whats_orders_manager.modules.seguridad.controller;

import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * PERMISO ADMIN CONTROLLER
 * WhatsApp Orders Manager
 * ============================================================================
 * Controlador para la gestión administrativa de permisos individuales.
 * Permite listar, editar, activar/desactivar y marcar como críticos los
 * 48 permisos del sistema almacenados en base de datos.
 * Solo accesible por administradores.
 * 
 * @version 1.0 - Sprint 4
 * @since 23/12/2025
 * ============================================================================
 */
@Slf4j
@Controller
@RequestMapping("/admin/permisos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PermisoAdminController {
    
    private final PermisoService permisoService;
    
    /**
     * Listado de todos los permisos con filtros y paginación
     */
    @GetMapping("/gestionar")
    public String listarPermisos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "categoria") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean critico,
            @RequestParam(required = false) String buscar,
            Model model
    ) {
        log.info("Listando permisos - Página: {}, Tamaño: {}, Orden: {} {}", 
                page, size, sortBy, sortDir);
        
        try {
            // Obtener todos los permisos (sin paginación por ahora para mantenerlo simple)
            List<Permiso> todosPermisos = permisoService.obtenerTodos();
            
            // Aplicar filtros manualmente
            List<Permiso> permisosFiltrados = todosPermisos.stream()
                .filter(p -> categoria == null || categoria.isEmpty() || p.getCategoria().equals(categoria))
                .filter(p -> activo == null || p.getActivo().equals(activo))
                .filter(p -> critico == null || p.getEsCritico().equals(critico))
                .filter(p -> buscar == null || buscar.isEmpty() || 
                        p.getNombre().toLowerCase().contains(buscar.toLowerCase()) ||
                        p.getCodigo().toLowerCase().contains(buscar.toLowerCase()) ||
                        p.getDescripcion().toLowerCase().contains(buscar.toLowerCase()))
                .toList();
            
            // Obtener categorías únicas para filtro
            List<String> categorias = todosPermisos.stream()
                .map(Permiso::getCategoria)
                .distinct()
                .sorted()
                .toList();
            
            // Estadísticas
            long totalPermisos = todosPermisos.size();
            long permisosActivos = todosPermisos.stream().filter(Permiso::getActivo).count();
            long permisosCriticos = todosPermisos.stream().filter(Permiso::getEsCritico).count();
            
            model.addAttribute("permisos", permisosFiltrados);
            model.addAttribute("categorias", categorias);
            model.addAttribute("totalPermisos", totalPermisos);
            model.addAttribute("permisosActivos", permisosActivos);
            model.addAttribute("permisosCriticos", permisosCriticos);
            model.addAttribute("permisosFiltrados", permisosFiltrados.size());
            
            // Mantener valores de filtros
            model.addAttribute("categoriaSeleccionada", categoria);
            model.addAttribute("activoSeleccionado", activo);
            model.addAttribute("criticoSeleccionado", critico);
            model.addAttribute("busqueda", buscar);
            
            log.info("Permisos cargados exitosamente: {} de {} totales", 
                    permisosFiltrados.size(), totalPermisos);
            
            return "modules/seguridad/admin/permisos/gestionar";
            
        } catch (Exception e) {
            log.error("Error al listar permisos: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar la lista de permisos");
            return "shared/error/error";
        }
    }
    
    /**
     * Mostrar formulario de edición de un permiso
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Mostrando formulario de edición para permiso ID: {}", id);
        
        try {
            Optional<Permiso> permisoOpt = permisoService.buscarPorId(id);
            
            if (permisoOpt.isEmpty()) {
                log.warn("Permiso no encontrado con ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Permiso no encontrado");
                return "redirect:/admin/permisos/gestionar";
            }
            
            model.addAttribute("permiso", permisoOpt.get());
            model.addAttribute("esEdicion", true);
            
            return "modules/seguridad/admin/permisos/editar";
            
        } catch (Exception e) {
            log.error("Error al cargar formulario de edición: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar el permiso");
            return "redirect:/admin/permisos/gestionar";
        }
    }
    
    /**
     * Actualizar un permiso existente
     */
    @PostMapping("/actualizar/{id}")
    public String actualizarPermiso(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam String descripcion,
            @RequestParam String categoria,
            @RequestParam(required = false) Boolean esCritico,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Actualizando permiso ID: {}", id);
        
        try {
            Optional<Permiso> permisoOpt = permisoService.buscarPorId(id);
            
            if (permisoOpt.isEmpty()) {
                log.warn("Permiso no encontrado con ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Permiso no encontrado");
                return "redirect:/admin/permisos/gestionar";
            }
            
            Permiso permiso = permisoOpt.get();
            
            // Actualizar campos editables (no el código, es inmutable)
            permiso.setNombre(nombre);
            permiso.setDescripcion(descripcion);
            permiso.setCategoria(categoria);
            permiso.setEsCritico(esCritico != null && esCritico);
            
            permisoService.guardar(permiso);
            
            log.info("Permiso actualizado exitosamente: {}", permiso.getCodigo());
            redirectAttributes.addFlashAttribute("success", 
                    "Permiso '" + permiso.getNombre() + "' actualizado correctamente");
            
            return "redirect:/admin/permisos/gestionar";
            
        } catch (Exception e) {
            log.error("Error al actualizar permiso: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                    "Error al actualizar el permiso: " + e.getMessage());
            return "redirect:/admin/permisos/editar/" + id;
        }
    }
    
    /**
     * Cambiar estado activo/inactivo de un permiso
     */
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Cambiando estado del permiso ID: {}", id);
        
        try {
            Optional<Permiso> permisoOpt = permisoService.buscarPorId(id);
            
            if (permisoOpt.isEmpty()) {
                log.warn("Permiso no encontrado con ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Permiso no encontrado");
                return "redirect:/admin/permisos/gestionar";
            }
            
            Permiso permiso = permisoOpt.get();
            boolean nuevoEstado = !permiso.getActivo();
            permiso.setActivo(nuevoEstado);
            
            permisoService.guardar(permiso);
            
            String mensaje = nuevoEstado ? "activado" : "desactivado";
            log.info("Permiso {} {}", permiso.getCodigo(), mensaje);
            redirectAttributes.addFlashAttribute("success", 
                    "Permiso '" + permiso.getNombre() + "' " + mensaje + " correctamente");
            
            return "redirect:/admin/permisos/gestionar";
            
        } catch (Exception e) {
            log.error("Error al cambiar estado del permiso: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                    "Error al cambiar el estado del permiso");
            return "redirect:/admin/permisos/gestionar";
        }
    }
    
    /**
     * Marcar/desmarcar un permiso como crítico
     */
    @PostMapping("/toggle-critico/{id}")
    public String toggleCritico(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        log.info("Cambiando criticidad del permiso ID: {}", id);
        
        try {
            Optional<Permiso> permisoOpt = permisoService.buscarPorId(id);
            
            if (permisoOpt.isEmpty()) {
                log.warn("Permiso no encontrado con ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Permiso no encontrado");
                return "redirect:/admin/permisos/gestionar";
            }
            
            Permiso permiso = permisoOpt.get();
            boolean nuevaCriticidad = !permiso.getEsCritico();
            permiso.setEsCritico(nuevaCriticidad);
            
            permisoService.guardar(permiso);
            
            String mensaje = nuevaCriticidad ? "marcado como CRÍTICO" : "desmarcado como crítico";
            log.info("Permiso {} {}", permiso.getCodigo(), mensaje);
            redirectAttributes.addFlashAttribute("success", 
                    "Permiso '" + permiso.getNombre() + "' " + mensaje);
            
            return "redirect:/admin/permisos/gestionar";
            
        } catch (Exception e) {
            log.error("Error al cambiar criticidad del permiso: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", 
                    "Error al cambiar la criticidad del permiso");
            return "redirect:/admin/permisos/gestionar";
        }
    }
    
    /**
     * Ver detalle de un permiso (muestra roles que lo tienen)
     */
    @GetMapping("/detalle/{id}")
    @ResponseBody
    public String verDetalle(@PathVariable Long id) {
        log.info("Consultando detalle del permiso ID: {}", id);
        
        try {
            Optional<Permiso> permisoOpt = permisoService.buscarPorId(id);
            
            if (permisoOpt.isEmpty()) {
                return "Permiso no encontrado";
            }
            
            Permiso permiso = permisoOpt.get();
            
            StringBuilder detalle = new StringBuilder();
            detalle.append("Código: ").append(permiso.getCodigo()).append("\n");
            detalle.append("Nombre: ").append(permiso.getNombre()).append("\n");
            detalle.append("Categoría: ").append(permiso.getCategoria()).append("\n");
            detalle.append("Crítico: ").append(permiso.getEsCritico() ? "SÍ" : "NO").append("\n");
            detalle.append("Activo: ").append(permiso.getActivo() ? "SÍ" : "NO").append("\n");
            detalle.append("\nRoles con este permiso:\n");
            
            if (permiso.getRoles() != null && !permiso.getRoles().isEmpty()) {
                permiso.getRoles().forEach(rol -> 
                    detalle.append("- ").append(rol.getNombre()).append("\n")
                );
            } else {
                detalle.append("Ningún rol tiene este permiso asignado\n");
            }
            
            return detalle.toString();
            
        } catch (Exception e) {
            log.error("Error al obtener detalle del permiso: {}", e.getMessage(), e);
            return "Error al obtener detalles";
        }
    }
}
