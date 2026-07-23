package api.astro.whats_orders_manager.modules.seguridad.controller;

import api.astro.whats_orders_manager.modules.rrhh.service.EmpleadoService;
import api.astro.whats_orders_manager.modules.seguridad.dto.UsuarioAdminDTO;
import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioPermiso;
import api.astro.whats_orders_manager.modules.seguridad.repository.PermisoRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.RolService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioPermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.shared.service.EmailService;
import api.astro.whats_orders_manager.shared.util.PasswordUtil;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import api.astro.whats_orders_manager.shared.util.PaginacionUtil;

/**
 * ============================================================================
 * USUARIO ADMIN CONTROLLER
 * ERP Orders Manager
 * ============================================================================
 * Controller para la gestión administrativa de usuarios del sistema.
 * 
 * Funcionalidades:
 * - Listar usuarios con paginación y filtros
 * - Crear y editar usuarios (solo admin)
 * - Bloquear/desbloquear usuarios
 * - Cambiar roles
 * - Ver actividades de usuario
 * - Gestionar sesiones activas
 * 
 * @version 1.0 - Sprint 4
 * @since 22/12/2025
 * ============================================================================
 */
@Slf4j
@Controller
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioAdminController {
    
    private final UsuarioService usuarioService;
    private final UsuarioActividadService actividadService;
    private final UsuarioPermisoService usuarioPermisoService;
    private final PermisoService permisoService;
    private final PermisoRepository permisoRepository;
    private final RolService rolService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * @Lazy breaks the circular dependency: EmpleadoService → UsuarioRepository
     * and UsuarioAdminController → EmpleadoService → EmpleadoRepository → ...
     * Using field injection with @Lazy because @RequiredArgsConstructor does not
     * support per-field @Lazy on constructor parameters.
     */
    @Autowired
    @Lazy
    private EmpleadoService empleadoService;

    // ==================== VISTAS ====================

    /**
     * Vista principal: Lista de usuarios con paginación
     */
    @GetMapping("")
    public String listarUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idUsuario") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean bloqueado,
            Model model) {
        
        log.info("Listando usuarios - Page: {}, Size: {}, Sort: {} {}", page, size, sortBy, sortDir);
        
        try {
            // Crear paginación
            Pageable pageable = PaginacionUtil.buildPageable(page, size, sortBy, sortDir,
                    Set.of("idUsuario", "nombre", "rol"), "idUsuario");
            
            // Obtener usuarios (SIN FILTROS - traer todos para paginar correctamente)
            Page<Usuario> usuariosPage;
            
            // Si no hay filtros, traer todo
            if ((rol == null || rol.isEmpty()) && activo == null && bloqueado == null) {
                usuariosPage = usuarioService.findAll(pageable);
            } else {
                // Si hay filtros, traer todos y filtrar (temporal - debería hacerse en BD)
                List<Usuario> todosUsuarios = usuarioService.findAll();

                log.info("usuarios: {}", todosUsuarios.size());
                
                // Aplicar filtros
                List<Usuario> usuariosFiltrados = todosUsuarios.stream()
                        .filter(u -> (rol == null || rol.isEmpty()) || 
                                   (u.getRolEntity() != null && rol.equals(u.getRolEntity().getNombre())) ||
                                   (u.getRolEntity() == null && rol.equals(u.getRol())))
                        .filter(u -> activo == null || activo.equals(u.getActivo()))
                        .filter(u -> bloqueado == null || bloqueado.equals(u.getBloqueado()))
                        .collect(Collectors.toList());
                
                // Aplicar paginación manual
                int start = page * size;
                int end = Math.min(start + size, usuariosFiltrados.size());
                List<Usuario> usuariosPaginados = start < usuariosFiltrados.size() 
                        ? usuariosFiltrados.subList(start, end)
                        : List.of();
                
                // Crear página manual
                usuariosPage = new org.springframework.data.domain.PageImpl<>(
                        usuariosPaginados, 
                        pageable, 
                        usuariosFiltrados.size()
                );
            }
            
            // Convertir a DTOs
            Page<UsuarioAdminDTO> usuariosDTO = usuariosPage.map(UsuarioAdminDTO::fromEntity);
            
            // Agregar al modelo
            model.addAttribute("usuarios", usuariosDTO.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", usuariosDTO.getTotalPages());
            model.addAttribute("totalItems", usuariosDTO.getTotalElements());
            model.addAttribute("size", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
            
            // Filtros actuales
            model.addAttribute("currentRol", rol);
            model.addAttribute("currentActivo", activo);
            model.addAttribute("currentBloqueado", bloqueado);
            
            // Estadísticas
            model.addAttribute("totalUsuarios", usuarioService.count());
            model.addAttribute("totalActivos", usuarioService.countByActivo(true));
            model.addAttribute("totalBloqueados", usuarioService.countByBloqueado(true));
            model.addAttribute("totalAdmins", usuarioService.countByRol("ADMIN"));
            
            return "modules/seguridad/usuarios/lista-admin";
            
        } catch (Exception e) {
            log.error("Error al listar usuarios: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar la lista de usuarios");
            return "modules/seguridad/usuarios/lista-admin";
        }
    }

    /**
     * Vista: Formulario para crear nuevo usuario
     */
    @GetMapping("/nuevo")
    public String nuevoUsuario(Model model) {
        log.info("Mostrando formulario de nuevo usuario");

        // Obtener todos los roles activos
        var rolesActivos = rolService.findByActivoTrue();

        model.addAttribute("usuario", new UsuarioAdminDTO());
        model.addAttribute("roles", rolesActivos);
        model.addAttribute("modoEdicion", false);
        model.addAttribute("empleadosDisponibles", empleadoService.findEmpleadosDisponibles(null));

        return "modules/seguridad/usuarios/form-admin";
    }

    /**
     * Vista: Formulario para editar usuario existente
     */
    @GetMapping("/editar/{id}")
    public String editarUsuario(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        log.info("Mostrando formulario de edición para usuario ID: {}", id);
        
        try {
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            
            if (usuarioOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/admin/usuarios";
            }
            
            UsuarioAdminDTO usuarioDTO = UsuarioAdminDTO.fromEntity(usuarioOpt.get());

            log.info("usuarioDTO: {}", usuarioDTO);

            // Obtain all active employees not linked to another user (+ current one if any)
            var empleadosDisponibles = empleadoService.findEmpleadosDisponibles(id);

            // Pre-select the currently linked employee on the DTO (if any)
            empleadosDisponibles.stream()
                    .filter(e -> e.getUsuario() != null && e.getUsuario().getIdUsuario().equals(id))
                    .findFirst()
                    .ifPresent(e -> usuarioDTO.setEmpleadoId(e.getId()));

            // Obtener todos los roles activos
            var rolesActivos = rolService.findByActivoTrue();

            model.addAttribute("usuario", usuarioDTO);
            model.addAttribute("roles", rolesActivos);
            model.addAttribute("modoEdicion", true);
            model.addAttribute("empleadosDisponibles", empleadosDisponibles);

            return "modules/seguridad/usuarios/form-admin";
            
        } catch (Exception e) {
            log.error("Error al cargar usuario para edición: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar usuario");
            return "redirect:/admin/usuarios";
        }
    }

    /**
     * Procesar: Crear nuevo usuario desde formulario HTML
     */
    @PostMapping("/guardar")
    public String guardarNuevoUsuario(
            @ModelAttribute("usuario") UsuarioAdminDTO usuarioDTO,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        
        log.info("Guardando nuevo usuario: {}", usuarioDTO.getNombre());
        
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Error en los datos del formulario");
            return "redirect:/admin/usuarios/nuevo";
        }
        
        try {
            // Verificar si ya existe
            if (usuarioDTO.getTelefono() != null) {
                Optional<Usuario> existente = usuarioService.findByTelefono(usuarioDTO.getTelefono());
                if (existente.isPresent()) {
                    redirectAttributes.addFlashAttribute("error", "Ya existe un usuario con ese teléfono");
                    return "redirect:/admin/usuarios/nuevo";
                }
            }
            
            // Procesar avatar si se subió
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    String avatarUrl = guardarAvatar(avatarFile);
                    usuarioDTO.setAvatar(avatarUrl);
                    log.info("Avatar guardado: {}", avatarUrl);
                } catch (IOException e) {
                    log.error("Error al guardar avatar: {}", e.getMessage());
                    redirectAttributes.addFlashAttribute("warning", "Usuario creado pero no se pudo guardar el avatar");
                }
            }
            
            // Convertir DTO a entidad
            Usuario usuario = usuarioDTO.toEntity();

            if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isBlank()) {
                usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
            }

            // Guardar
            Usuario usuarioGuardado = usuarioService.save(usuario);

            // Link to employee if specified
            if (usuarioDTO.getEmpleadoId() != null) {
                empleadoService.vincularUsuario(usuarioDTO.getEmpleadoId(), usuarioGuardado.getIdUsuario());
            }

            // Registrar actividad
            Usuario admin = obtenerUsuarioActual(authentication);
            if (admin != null) {
                actividadService.registrarActividad(
                        admin.getIdUsuario(),
                        "CREAR_USUARIO",
                        "Admin creó nuevo usuario: " + usuario.getNombre(),
                        "USUARIO",
                        usuarioGuardado.getIdUsuario()
                );
            }

            log.info("Usuario creado exitosamente: {}", usuarioGuardado.getIdUsuario());
            redirectAttributes.addFlashAttribute("success", "Usuario creado exitosamente");
            return "redirect:/admin/usuarios";

        } catch (Exception e) {
            log.error("Error al crear usuario: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al crear usuario: " + e.getMessage());
            return "redirect:/admin/usuarios/nuevo";
        }
    }

    /**
     * Procesar: Actualizar usuario existente desde formulario HTML
     */
    @PostMapping("/actualizar/{id}")
    public String actualizarUsuarioForm(
            @PathVariable Integer id,
            @ModelAttribute("usuario") UsuarioAdminDTO usuarioDTO,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        
        log.info("Actualizando usuario ID: {} desde formulario", id);
        
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Error en los datos del formulario");
            return "redirect:/admin/usuarios/editar/" + id;
        }
        
        try {
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            
            if (usuarioOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/admin/usuarios";
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Actualizar campos manualmente
            usuario.setNombre(usuarioDTO.getNombre());
            usuario.setTelefono(usuarioDTO.getTelefono());
            usuario.setEmail(usuarioDTO.getEmail());
            usuario.setRol(usuarioDTO.getRol());
            usuario.setActivo(usuarioDTO.getActivo());
            
            // Procesar nuevo avatar si se subió
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    String avatarUrl = guardarAvatar(avatarFile);
                    usuario.setAvatar(avatarUrl);
                    log.info("Nuevo avatar guardado: {}", avatarUrl);
                } catch (IOException e) {
                    log.error("Error al guardar avatar: {}", e.getMessage());
                    redirectAttributes.addFlashAttribute("warning", "Usuario actualizado pero no se pudo guardar el avatar");
                }
            } else if (usuarioDTO.getAvatar() != null && !usuarioDTO.getAvatar().isEmpty()) {
                // Mantener avatar actual si no se subió uno nuevo
                usuario.setAvatar(usuarioDTO.getAvatar());
            }

            if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isBlank()) {
                usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
            }

            // Guardar
            Usuario usuarioActualizado = usuarioService.save(usuario);

            // Sync employee link: vincular if provided, desvincular if cleared
            if (usuarioDTO.getEmpleadoId() != null) {
                empleadoService.vincularUsuario(usuarioDTO.getEmpleadoId(), id);
            } else {
                empleadoService.desvincularUsuario(id);
            }

            // Registrar actividad
            Usuario admin = obtenerUsuarioActual(authentication);
            if (admin != null) {
                actividadService.registrarActividad(
                        admin.getIdUsuario(),
                        "ACTUALIZAR_USUARIO",
                        "Admin actualizó usuario: " + usuario.getNombre(),
                        "USUARIO",
                        id
                );
            }

            log.info("Usuario actualizado exitosamente: {}", id);
            redirectAttributes.addFlashAttribute("success", "Usuario actualizado exitosamente");
            return "redirect:/admin/usuarios";

        } catch (Exception e) {
            log.error("Error al actualizar usuario: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar usuario: " + e.getMessage());
            return "redirect:/admin/usuarios/editar/" + id;
        }
    }

    /**
     * Vista: Detalle completo de un usuario
     */
    @GetMapping("/detalle/{id}")
    public String detalleUsuario(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        log.info("Mostrando detalle del usuario ID: {}", id);
        
        try {
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            
            if (usuarioOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/admin/usuarios";
            }
            
            Usuario usuario = usuarioOpt.get();
            UsuarioAdminDTO usuarioDTO = UsuarioAdminDTO.fromEntity(usuario);
            
            // Obtener actividades recientes del usuario (con manejo de errores)
            try {
                var actividades = actividadService.getUltimasActividades(id, 20);
                long totalActividades = actividadService.countByUsuario(id);
                long totalLogins = actividadService.countByUsuarioAndTipo(id, "LOGIN");
                
                model.addAttribute("actividades", actividades);
                model.addAttribute("totalActividades", totalActividades);
                model.addAttribute("totalLogins", totalLogins);
            } catch (Exception e) {
                log.warn("No se pudieron cargar actividades para usuario {}: {}", id, e.getMessage());
                model.addAttribute("actividades", List.of());
                model.addAttribute("totalActividades", 0L);
                model.addAttribute("totalLogins", 0L);
            }
            
            model.addAttribute("usuario", usuarioDTO);
            
            return "modules/seguridad/usuarios/detalle-admin";
            
        } catch (Exception e) {
            log.error("Error al cargar detalle de usuario: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar detalle");
            return "redirect:/admin/usuarios";
        }
    }

    /**
     * Vista: Gestionar permisos personalizados de un usuario
     */
    @GetMapping("/{id}/permisos/gestionar")
    public String gestionarPermisosUsuario(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        log.info("Accediendo a gestión de permisos del usuario ID: {}", id);
        
        try {
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            
            if (usuarioOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
                return "redirect:/admin/usuarios";
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Obtener resumen de permisos
            Map<String, Object> resumen = usuarioPermisoService.obtenerResumenPermisos(id);
            
            // Obtener permisos disponibles para conceder (los que NO tiene el rol y NO están concedidos)
            List<Permiso> todosLosPermisos = permisoRepository.findByActivoTrue();
            Set<String> permisosDelRol = new HashSet<>();
            Set<String> permisosConcedidos = new HashSet<>();
            Set<String> permisosDenegados = new HashSet<>();
            
            // Extraer códigos de permisos del resumen
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> permisosRolList = (List<Map<String, Object>>) resumen.get("permisosRol");
            if (permisosRolList != null) {
                permisosDelRol = permisosRolList.stream()
                    .map(p -> (String) p.get("codigo"))
                    .collect(Collectors.toSet());
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> concedidosList = (List<Map<String, Object>>) resumen.get("permisosConcedidos");
            if (concedidosList != null) {
                permisosConcedidos = concedidosList.stream()
                    .map(p -> (String) p.get("codigo"))
                    .collect(Collectors.toSet());
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> denegadosList = (List<Map<String, Object>>) resumen.get("permisosDenegados");
            if (denegadosList != null) {
                permisosDenegados = denegadosList.stream()
                    .map(p -> (String) p.get("codigo"))
                    .collect(Collectors.toSet());
            }
            
            // Permisos disponibles para CONCEDER: los que NO tiene el rol y NO están concedidos
            final Set<String> finalPermisosDelRol = permisosDelRol;
            final Set<String> finalPermisosConcedidos = permisosConcedidos;
            List<Permiso> permisosParaConceder = todosLosPermisos.stream()
                .filter(p -> p.getActivo())
                .filter(p -> !finalPermisosDelRol.contains(p.getCodigo()))
                .filter(p -> !finalPermisosConcedidos.contains(p.getCodigo()))
                .sorted(Comparator.comparing(Permiso::getCodigo))
                .collect(Collectors.toList());
            
            // Permisos disponibles para DENEGAR: los que SÍ tiene el rol y NO están denegados
            final Set<String> finalPermisosDenegados = permisosDenegados;
            List<Permiso> permisosParaDenegar = todosLosPermisos.stream()
                .filter(p -> p.getActivo())
                .filter(p -> finalPermisosDelRol.contains(p.getCodigo()))
                .filter(p -> !finalPermisosDenegados.contains(p.getCodigo()))
                .sorted(Comparator.comparing(Permiso::getCodigo))
                .collect(Collectors.toList());
            
            model.addAttribute("usuario", usuario);
            model.addAttribute("resumenPermisos", resumen);
            model.addAttribute("permisosParaConceder", permisosParaConceder);
            model.addAttribute("permisosParaDenegar", permisosParaDenegar);
            model.addAttribute("titulo", "Gestión de Permisos - " + usuario.getNombre());
            
            return "modules/seguridad/admin/usuarios/permisos";
            
        } catch (Exception e) {
            log.error("Error al cargar gestión de permisos: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al cargar permisos");
            return "redirect:/admin/usuarios";
        }
    }

    // ==================== API ENDPOINTS ====================

    /**
     * API: Obtener todos los usuarios (con filtros opcionales)
     */
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("idUsuario").descending());
            Page<Usuario> usuariosPage;
            
            if (rol != null && !rol.isEmpty()) {
                List<Usuario> usuarios = usuarioService.findByRol(rol);
                usuariosPage = new org.springframework.data.domain.PageImpl<>(usuarios, pageable, usuarios.size());
            } else if (activo != null) {
                List<Usuario> usuarios = usuarioService.findByActivo(activo);
                usuariosPage = new org.springframework.data.domain.PageImpl<>(usuarios, pageable, usuarios.size());
            } else {
                usuariosPage = usuarioService.findAll(pageable);
            }
            
            Page<UsuarioAdminDTO> usuariosDTO = usuariosPage.map(UsuarioAdminDTO::fromEntity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("usuarios", usuariosDTO.getContent());
            response.put("currentPage", usuariosDTO.getNumber());
            response.put("totalItems", usuariosDTO.getTotalElements());
            response.put("totalPages", usuariosDTO.getTotalPages());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener usuarios: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener usuarios"));
        }
    }

    /**
     * API: Obtener un usuario por ID
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerUsuario(@PathVariable Integer id) {
        try {
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }
            
            UsuarioAdminDTO usuarioDTO = UsuarioAdminDTO.fromEntity(usuarioOpt.get());
            return ResponseEntity.ok(usuarioDTO);
            
        } catch (Exception e) {
            log.error("Error al obtener usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener usuario"));
        }
    }

    /**
     * API: Crear nuevo usuario
     */
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> crearUsuario(
            @Valid @RequestBody UsuarioAdminDTO usuarioDTO,
            BindingResult result,
            Authentication authentication) {
        
        log.info("Creando nuevo usuario: {}", usuarioDTO.getNombre());
        
        if (result.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos inválidos", "detalles", result.getAllErrors()));
        }
        
        try {
            // Verificar si ya existe
            if (usuarioDTO.getTelefono() != null) {
                Optional<Usuario> existente = usuarioService.findByTelefono(usuarioDTO.getTelefono());
                if (existente.isPresent()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Ya existe un usuario con ese teléfono"));
                }
            }
            
            // Convertir DTO a entidad
            Usuario usuario = usuarioDTO.toEntity();
            
            // Guardar
            Usuario usuarioGuardado = usuarioService.save(usuario);
            
            // Registrar actividad
            Usuario admin = obtenerUsuarioActual(authentication);
            if (admin != null) {
                actividadService.registrarActividad(
                        admin.getIdUsuario(),
                        "CREAR_USUARIO",
                        "Admin creó nuevo usuario: " + usuario.getNombre(),
                        "USUARIO",
                        usuarioGuardado.getIdUsuario()
                );
            }
            
            log.info("Usuario creado exitosamente: {}", usuarioGuardado.getIdUsuario());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(UsuarioAdminDTO.fromEntity(usuarioGuardado));
            
        } catch (Exception e) {
            log.error("Error al crear usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear usuario: " + e.getMessage()));
        }
    }

    /**
     * API: Actualizar usuario existente
     */
    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioAdminDTO usuarioDTO,
            BindingResult result,
            Authentication authentication) {
        
        log.info("Actualizando usuario ID: {}", id);
        
        if (result.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos inválidos", "detalles", result.getAllErrors()));
        }
        
        try {
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Actualizar campos
            usuarioDTO.updateEntity(usuario);
            
            // Guardar
            Usuario usuarioActualizado = usuarioService.save(usuario);
            
            // Registrar actividad
            Usuario admin = obtenerUsuarioActual(authentication);
            if (admin != null) {
                actividadService.registrarActividad(
                        admin.getIdUsuario(),
                        "ACTUALIZAR_USUARIO",
                        "Admin actualizó usuario: " + usuario.getNombre(),
                        "USUARIO",
                        id
                );
            }
            
            log.info("Usuario actualizado exitosamente: {}", id);
            
            return ResponseEntity.ok(UsuarioAdminDTO.fromEntity(usuarioActualizado));
            
        } catch (Exception e) {
            log.error("Error al actualizar usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar usuario: " + e.getMessage()));
        }
    }

    /**
     * API: Bloquear usuario
     */
    @PutMapping("/api/{id}/bloquear")
    @ResponseBody
    public ResponseEntity<?> bloquearUsuario(
            @PathVariable Integer id,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        
        log.warn("Bloqueando usuario ID: {}", id);
        
        try {
            String razon = payload.getOrDefault("razon", "Bloqueado por administrador");
            Usuario admin = obtenerUsuarioActual(authentication);
            Integer adminId = admin != null ? admin.getIdUsuario() : null;
            
            Usuario usuarioBloqueado = usuarioService.bloquearUsuario(id, razon, adminId);
            
            // Registrar actividad
            if (admin != null) {
                actividadService.registrarActividadCompleta(
                        admin.getIdUsuario(),
                        "BLOQUEAR_USUARIO",
                        "Admin bloqueó usuario: " + usuarioBloqueado.getNombre(),
                        "USUARIO",
                        id,
                        "{\"razon\":\"" + razon + "\"}",
                        "WARNING"
                );
            }
            
            log.info("Usuario bloqueado exitosamente: {}", id);
            
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Usuario bloqueado exitosamente",
                    "usuario", UsuarioAdminDTO.fromEntity(usuarioBloqueado)
            ));
            
        } catch (Exception e) {
            log.error("Error al bloquear usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al bloquear usuario: " + e.getMessage()));
        }
    }

    /**
     * API: Desbloquear usuario
     */
    @PutMapping("/api/{id}/desbloquear")
    @ResponseBody
    public ResponseEntity<?> desbloquearUsuario(
            @PathVariable Integer id,
            Authentication authentication) {
        
        log.info("Desbloqueando usuario ID: {}", id);
        
        try {
            Usuario usuarioDesbloqueado = usuarioService.desbloquearUsuario(id);
            
            // Registrar actividad
            Usuario admin = obtenerUsuarioActual(authentication);
            if (admin != null) {
                actividadService.registrarActividad(
                        admin.getIdUsuario(),
                        "DESBLOQUEAR_USUARIO",
                        "Admin desbloqueó usuario: " + usuarioDesbloqueado.getNombre(),
                        "USUARIO",
                        id
                );
            }
            
            log.info("Usuario desbloqueado exitosamente: {}", id);
            
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Usuario desbloqueado exitosamente",
                    "usuario", UsuarioAdminDTO.fromEntity(usuarioDesbloqueado)
            ));
            
        } catch (Exception e) {
            log.error("Error al desbloquear usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al desbloquear usuario: " + e.getMessage()));
        }
    }

    /**
     * API: Resetear intentos fallidos
     */
    @PutMapping("/api/{id}/resetear-intentos")
    @ResponseBody
    public ResponseEntity<?> resetearIntentosFallidos(@PathVariable Integer id) {
        log.info("Reseteando intentos fallidos para usuario ID: {}", id);
        try {
            usuarioService.resetearIntentosFallidos(id);
            return ResponseEntity.ok(Map.of("mensaje", "Intentos fallidos reseteados exitosamente"));
        } catch (Exception e) {
            log.error("Error al resetear intentos fallidos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al resetear intentos: " + e.getMessage()));
        }
    }

    /**
     * API: Cambiar rol de usuario
     */
    @PutMapping("/api/{id}/rol")
    @ResponseBody
    public ResponseEntity<?> cambiarRol(
            @PathVariable Integer id,
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        
        log.info("Cambiando rol de usuario ID: {}", id);
        
        try {
            String nuevoRol = payload.get("rol");
            
            if (nuevoRol == null || nuevoRol.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El rol es requerido"));
            }
            
            // Validar rol
            if (!List.of("ADMIN", "GERENTE", "VENDEDOR").contains(nuevoRol)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Rol inválido. Valores permitidos: ADMIN, GERENTE, VENDEDOR"));
            }
            
            Usuario usuarioActualizado = usuarioService.cambiarRol(id, nuevoRol);
            
            // Registrar actividad
            Usuario admin = obtenerUsuarioActual(authentication);
            if (admin != null) {
                actividadService.registrarActividad(
                        admin.getIdUsuario(),
                        "CAMBIAR_ROL",
                        "Admin cambió rol de usuario: " + usuarioActualizado.getNombre() + " a " + nuevoRol,
                        "USUARIO",
                        id
                );
            }
            
            log.info("Rol cambiado exitosamente para usuario: {}", id);
            
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Rol actualizado exitosamente",
                    "usuario", UsuarioAdminDTO.fromEntity(usuarioActualizado)
            ));
            
        } catch (Exception e) {
            log.error("Error al cambiar rol: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cambiar rol: " + e.getMessage()));
        }
    }

    /**
     * API: Activar/desactivar usuario
     */
    @PutMapping("/api/{id}/estado")
    @ResponseBody
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> payload,
            Authentication authentication) {
        
        log.info("Cambiando estado de usuario ID: {}", id);
        
        try {
            Boolean activo = payload.get("activo");
            
            if (activo == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El estado activo es requerido"));
            }
            
            Usuario usuarioActualizado = usuarioService.cambiarEstadoActivo(id, activo);
            
            // Registrar actividad
            Usuario admin = obtenerUsuarioActual(authentication);
            if (admin != null) {
                String accion = activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO";
                actividadService.registrarActividad(
                        admin.getIdUsuario(),
                        accion,
                        "Admin " + (activo ? "activó" : "desactivó") + " usuario: " + usuarioActualizado.getNombre(),
                        "USUARIO",
                        id
                );
            }
            
            log.info("Estado cambiado exitosamente para usuario: {}", id);
            
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Estado actualizado exitosamente",
                    "usuario", UsuarioAdminDTO.fromEntity(usuarioActualizado)
            ));
            
        } catch (Exception e) {
            log.error("Error al cambiar estado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cambiar estado: " + e.getMessage()));
        }
    }

    /**
     * API: Eliminar usuario (soft delete - desactivar)
     */
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarUsuario(
            @PathVariable Integer id,
            Authentication authentication) {
        
        log.warn("Eliminando usuario ID: {}", id);
        
        try {
            // Verificar que el usuario existe
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Soft delete - desactivar en lugar de eliminar
            Usuario usuarioDesactivado = usuarioService.cambiarEstadoActivo(id, false);
            
            // Registrar actividad
            Usuario admin = obtenerUsuarioActual(authentication);
            if (admin != null) {
                actividadService.registrarActividadCompleta(
                        admin.getIdUsuario(),
                        "ELIMINAR_USUARIO",
                        "Admin eliminó (desactivó) usuario: " + usuario.getNombre(),
                        "USUARIO",
                        id,
                        "{\"tipo\":\"soft_delete\"}",
                        "WARNING"
                );
            }
            
            log.info("Usuario eliminado (desactivado) exitosamente: {}", id);
            
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Usuario eliminado exitosamente",
                    "usuario", UsuarioAdminDTO.fromEntity(usuarioDesactivado)
            ));
            
        } catch (Exception e) {
            log.error("Error al eliminar usuario: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar usuario: " + e.getMessage()));
        }
    }

    /**
     * API: Obtener estadísticas de usuarios
     */
    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticas() {
        try {
            Map<String, Object> estadisticas = new HashMap<>();
            
            estadisticas.put("total", usuarioService.count());
            estadisticas.put("activos", usuarioService.countByActivo(true));
            estadisticas.put("inactivos", usuarioService.countByActivo(false));
            estadisticas.put("bloqueados", usuarioService.countByBloqueado(true));
            estadisticas.put("admins", usuarioService.countByRol("ADMIN"));
            estadisticas.put("gerentes", usuarioService.countByRol("GERENTE"));
            estadisticas.put("vendedores", usuarioService.countByRol("VENDEDOR"));
            
            return ResponseEntity.ok(estadisticas);
            
        } catch (Exception e) {
            log.error("Error al obtener estadísticas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener estadísticas"));
        }
    }

    // ==================== GESTIÓN DE PERMISOS PERSONALIZADOS ====================

    /**
     * API: Obtener resumen de permisos de un usuario
     * Incluye: permisos del rol, concedidos adicionales y denegados
     */
    @GetMapping("/{id}/permisos")
    @ResponseBody
    public ResponseEntity<?> obtenerPermisosUsuario(@PathVariable Integer id) {
        try {
            log.info("Obteniendo permisos para usuario {}", id);
            
            Map<String, Object> resumen = usuarioPermisoService.obtenerResumenPermisos(id);
            
            return ResponseEntity.ok(resumen);
            
        } catch (RuntimeException e) {
            log.error("Error al obtener permisos del usuario {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado al obtener permisos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener permisos"));
        }
    }

    /**
     * API: Conceder un permiso adicional a un usuario
     */
    @PostMapping("/{id}/permisos/conceder")
    @ResponseBody
    public ResponseEntity<?> concederPermiso(
            @PathVariable Integer id,
            @RequestParam Long idPermiso,
            Authentication authentication) {
        try {
            log.info("Concediendo permiso {} a usuario {}", idPermiso, id);
            
            Usuario usuarioActual = obtenerUsuarioActual(authentication);
            if (usuarioActual == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }
            
            UsuarioPermiso permiso = usuarioPermisoService.concederPermiso(
                    id, idPermiso, usuarioActual.getIdUsuario());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Permiso concedido exitosamente",
                    "permiso", permiso
            ));
            
        } catch (RuntimeException e) {
            log.error("Error al conceder permiso: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado al conceder permiso: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al conceder permiso"));
        }
    }

    /**
     * API: Denegar un permiso a un usuario (aunque su rol lo tenga)
     */
    @PostMapping("/{id}/permisos/denegar")
    @ResponseBody
    public ResponseEntity<?> denegarPermiso(
            @PathVariable Integer id,
            @RequestParam Long idPermiso,
            Authentication authentication) {
        try {
            log.info("Denegando permiso {} a usuario {}", idPermiso, id);
            
            Usuario usuarioActual = obtenerUsuarioActual(authentication);
            if (usuarioActual == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }
            
            UsuarioPermiso permiso = usuarioPermisoService.denegarPermiso(
                    id, idPermiso, usuarioActual.getIdUsuario());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Permiso denegado exitosamente",
                    "permiso", permiso
            ));
            
        } catch (RuntimeException e) {
            log.error("Error al denegar permiso: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado al denegar permiso: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al denegar permiso"));
        }
    }

    /**
     * API: Remover un permiso personalizado (vuelve a usar el del rol)
     */
    @DeleteMapping("/{id}/permisos/{codigoPermiso}")
    @ResponseBody
    public ResponseEntity<?> removerPermisoPersonalizado(
            @PathVariable Integer id,
            @PathVariable String codigoPermiso) {
        try {
            log.info("Removiendo permiso personalizado {} del usuario {}", codigoPermiso, id);
            
            usuarioPermisoService.removerPermisoPersonalizado(id, codigoPermiso);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Permiso personalizado removido exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error al remover permiso personalizado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al remover permiso"));
        }
    }

    /**
     * API: Obtener permisos efectivos de un usuario (para uso en seguridad)
     */
    @GetMapping("/{id}/permisos/efectivos")
    @ResponseBody
    public ResponseEntity<?> obtenerPermisosEfectivos(@PathVariable Integer id) {
        try {
            log.debug("Obteniendo permisos efectivos para usuario {}", id);
            
            Map<String, Boolean> permisosEfectivos = usuarioPermisoService.obtenerPermisosEfectivos(id);
            
            return ResponseEntity.ok(Map.of(
                    "idUsuario", id,
                    "permisos", permisosEfectivos,
                    "total", permisosEfectivos.size()
            ));
            
        } catch (RuntimeException e) {
            log.error("Error al obtener permisos efectivos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener permisos efectivos"));
        }
    }

    /**
     * API: Resetea la contraseña de un usuario y genera una nueva aleatoria
     * 
     * POST /admin/usuarios/api/{id}/reset-password
     */
    @PostMapping("/api/{id}/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@PathVariable Integer id) {
        log.info("Reseteando contraseña del usuario ID: {}", id);
        
        try {
            Usuario usuario = usuarioService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            // Generar contraseña aleatoria
            String nuevaPassword = generarPasswordAleatoria();
            
            // Encriptar y guardar
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioService.save(usuario);
            
            log.info("✅ Contraseña reseteada exitosamente para usuario ID: {}", id);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "password", nuevaPassword,
                    "message", "Contraseña reseteada exitosamente"
            ));
            
        } catch (Exception e) {
            log.error("Error al resetear contraseña: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al resetear la contraseña. Por favor intente nuevamente."));
        }
    }

    /**
     * API: Reenvía las credenciales de un usuario por email
     * Genera una nueva contraseña temporal y la envía por email
     * 
     * POST /admin/usuarios/api/{id}/reenviar-credenciales
     */
    @PostMapping("/api/{id}/reenviar-credenciales")
    @ResponseBody
    public ResponseEntity<?> reenviarCredenciales(@PathVariable Integer id) {
        log.info("Reenviando credenciales del usuario ID: {}", id);
        
        try {
            Usuario usuario = usuarioService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            
            // Validar que el usuario tenga email
            if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
                log.warn("El usuario {} no tiene email configurado", usuario.getNombre());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El usuario no tiene email configurado"));
            }
            
            // Generar nueva contraseña temporal
            String nuevaPassword = generarPasswordAleatoria();
            
            // Encriptar y guardar
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuarioService.save(usuario);
            
            // Enviar credenciales por email
            String urlLogin = "http://localhost:9090/auth/login"; // TODO: Mover a configuración
            emailService.enviarCredencialesUsuario(usuario, nuevaPassword, urlLogin);
            
            log.info("✅ Credenciales reenviadas exitosamente a: {}", usuario.getEmail());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Credenciales enviadas exitosamente a " + usuario.getEmail()
            ));
            
        } catch (MessagingException e) {
            log.error("❌ Error al enviar credenciales por email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al enviar el email. Por favor intente nuevamente."));
        } catch (Exception e) {
            log.error("❌ Error al reenviar credenciales: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al reenviar credenciales. Por favor intente nuevamente."));
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Obtiene el usuario autenticado actualmente
     */
    private Usuario obtenerUsuarioActual(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            String telefono = authentication.getName();
            return usuarioService.findByTelefono(telefono).orElse(null);
        } catch (Exception e) {
            log.error("Error al obtener usuario actual: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Genera una contraseña aleatoria segura
     * Delegado a PasswordUtil para reutilización
     */
    private String generarPasswordAleatoria() {
        return PasswordUtil.generarPasswordAleatoria();
    }

    /**
     * Guarda un archivo de avatar y retorna la URL
     */
    private String guardarAvatar(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Crear directorio si no existe
        String uploadDir = "src/main/resources/static/images/avatars";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generar nombre único para el archivo
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;

        // Guardar archivo
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Retornar URL relativa
        return "/images/avatars/" + filename;
    }
    
    /**
     * API: Subir avatar de usuario
     */
    @PostMapping("/api/{id}/avatar")
    @ResponseBody
    public ResponseEntity<?> subirAvatar(
            @PathVariable Integer id,
            @RequestParam("avatarFile") MultipartFile file,
            Authentication authentication) {
        
        log.info("Subiendo avatar para usuario ID: {}", id);
        
        try {
            // Verificar que el usuario existe
            Optional<Usuario> usuarioOpt = usuarioService.findById(id);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Usuario no encontrado"));
            }

            // Validar archivo
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No se proporcionó ningún archivo"));
            }

            // Validar tamaño (2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El archivo es demasiado grande. Máximo 2MB"));
            }

            // Validar tipo
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El archivo debe ser una imagen"));
            }

            // Guardar avatar
            String avatarUrl = guardarAvatar(file);

            // Actualizar usuario
            Usuario usuario = usuarioOpt.get();
            usuario.setAvatar(avatarUrl);
            Usuario usuarioGuardado = usuarioService.save(usuario);

            // Registrar actividad
            actividadService.registrarActividad(
                    obtenerUsuarioActual(authentication).getIdUsuario(),
                    "ACTUALIZAR_AVATAR",
                    "Avatar actualizado para usuario: " + usuario.getNombre()
            );

            return ResponseEntity.ok()
                    .body(Map.of(
                            "success", true,
                            "message", "Avatar actualizado correctamente",
                            "avatarUrl", avatarUrl
                    ));

        } catch (IOException e) {
            log.error("Error al guardar archivo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar el archivo"));
        } catch (Exception e) {
            log.error("Error al subir avatar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al subir avatar"));
        }
    }
}
