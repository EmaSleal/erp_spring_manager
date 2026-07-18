package api.astro.whats_orders_manager.modules.notificacion.controller;

import api.astro.whats_orders_manager.modules.notificacion.model.Notificacion;
import api.astro.whats_orders_manager.modules.notificacion.model.PreferenciaNotificacion;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import api.astro.whats_orders_manager.modules.notificacion.service.PreferenciaNotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller para la gestión de notificaciones
 * Proporciona endpoints para operaciones CRUD y consultas de notificaciones
 */
@Slf4j
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionRestController {

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private PreferenciaNotificacionService preferenciaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Obtiene las notificaciones no leídas del usuario con paginación
     * GET /api/notificaciones/no-leidas?page=0&size=5
     */
    @GetMapping("/no-leidas")
    public ResponseEntity<?> obtenerNoLeidas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Authentication authentication) {
        
        try {
            Integer idUsuario = obtenerIdUsuario(authentication);
            
            if (idUsuario == null) {
                log.error("Usuario no encontrado: {}", authentication.getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("fechaEnvio").descending());
            Page<Notificacion> notificaciones = notificacionService.findNoLeidasByUsuarioId(idUsuario, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", notificaciones.getContent());
            response.put("totalElements", notificaciones.getTotalElements());
            response.put("totalPages", notificaciones.getTotalPages());
            response.put("currentPage", notificaciones.getNumber());
            response.put("pageSize", notificaciones.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al obtener notificaciones no leídas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al cargar notificaciones"));
        }
    }

    /**
     * Obtiene todas las notificaciones del usuario con filtros y paginación
     * GET /api/notificaciones?page=0&size=10&tipo=FACTURA_CREADA&leida=false
     */
    @GetMapping
    public ResponseEntity<?> obtenerNotificaciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Boolean leida,
            Authentication authentication) {
        
        try {
            Integer idUsuario = obtenerIdUsuario(authentication);
            
            if (idUsuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("fechaEnvio").descending());
            Page<Notificacion> notificaciones;

            // Aplicar filtros
            if (tipo != null && leida != null) {
                notificaciones = notificacionService.findByUsuarioIdAndTipoAndLeida(
                    idUsuario, tipo, leida, pageable);
            } else if (tipo != null) {
                notificaciones = notificacionService.findByUsuarioIdAndTipo(
                    idUsuario, tipo, pageable);
            } else if (leida != null) {
                if (leida) {
                    notificaciones = notificacionService.findLeidasByUsuarioId(idUsuario, pageable);
                } else {
                    notificaciones = notificacionService.findNoLeidasByUsuarioId(idUsuario, pageable);
                }
            } else {
                notificaciones = notificacionService.findByUsuarioId(idUsuario, pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("content", notificaciones.getContent());
            response.put("totalElements", notificaciones.getTotalElements());
            response.put("totalPages", notificaciones.getTotalPages());
            response.put("currentPage", notificaciones.getNumber());
            response.put("pageSize", notificaciones.getSize());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al obtener notificaciones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al cargar notificaciones"));
        }
    }

    /**
     * Obtiene el contador de notificaciones no leídas
     * GET /api/notificaciones/contador-no-leidas
     */
    @GetMapping("/contador-no-leidas")
    public ResponseEntity<?> obtenerContadorNoLeidas(Authentication authentication) {
        try {
            Integer idUsuario = obtenerIdUsuario(authentication);
            
            if (idUsuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
            }

            long contador = notificacionService.countNoLeidasByUsuarioId(idUsuario);
            
            return ResponseEntity.ok(Map.of("contador", contador));

        } catch (Exception e) {
            log.error("Error al obtener contador de notificaciones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al obtener contador"));
        }
    }

    /**
     * Marca una notificación como leída
     * PUT /api/notificaciones/{id}/marcar-leida
     */
    @PutMapping("/{id}/marcar-leida")
    public ResponseEntity<?> marcarComoLeida(
            @PathVariable Integer id,
            Authentication authentication) {
        
        try {
            Integer idUsuario = obtenerIdUsuario(authentication);
            
            if (idUsuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
            }

            Optional<Notificacion> notificacionOpt = notificacionService.findById(id);
            
            if (notificacionOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Notificación no encontrada"));
            }

            Notificacion notificacion = notificacionOpt.get();
            
            // Verificar que la notificación pertenece al usuario
            if (!notificacion.getUsuario().getIdUsuario().equals(idUsuario)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tiene permisos para acceder a esta notificación"));
            }

            notificacionService.marcarComoLeida(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notificación marcada como leída"
            ));

        } catch (Exception e) {
            log.error("Error al marcar notificación como leída", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al actualizar notificación"));
        }
    }

    /**
     * Marca todas las notificaciones del usuario como leídas
     * PUT /api/notificaciones/marcar-todas-leidas
     */
    @PutMapping("/marcar-todas-leidas")
    public ResponseEntity<?> marcarTodasComoLeidas(Authentication authentication) {
        try {
            Integer idUsuario = obtenerIdUsuario(authentication);
            
            if (idUsuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
            }

            notificacionService.marcarTodasComoLeidas(idUsuario);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Todas las notificaciones marcadas como leídas"
            ));

        } catch (Exception e) {
            log.error("Error al marcar todas las notificaciones como leídas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al actualizar notificaciones"));
        }
    }

    /**
     * Elimina una notificación
     * DELETE /api/notificaciones/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarNotificacion(
            @PathVariable Integer id,
            Authentication authentication) {
        
        try {
            Integer idUsuario = obtenerIdUsuario(authentication);
            
            if (idUsuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
            }

            Optional<Notificacion> notificacionOpt = notificacionService.findById(id);
            
            if (notificacionOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Notificación no encontrada"));
            }

            Notificacion notificacion = notificacionOpt.get();
            
            // Verificar que la notificación pertenece al usuario
            if (!notificacion.getUsuario().getIdUsuario().equals(idUsuario)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tiene permisos para eliminar esta notificación"));
            }

            notificacionService.eliminar(id);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notificación eliminada"
            ));

        } catch (Exception e) {
            log.error("Error al eliminar notificación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al eliminar notificación"));
        }
    }

    /**
     * Obtiene las preferencias de notificación del usuario
     * GET /api/notificaciones/preferencias
     */
    @GetMapping("/preferencias")
    public ResponseEntity<?> obtenerPreferencias(Authentication authentication) {
        try {
            Integer idUsuario = obtenerIdUsuario(authentication);
            
            if (idUsuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
            }

            Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
            }

            Usuario usuario = usuarioOpt.get();
            List<PreferenciaNotificacion> preferencias = 
                preferenciaService.findByUsuario(usuario);
            
            // Si no hay preferencias, devolver array vacío (el frontend creará las predeterminadas)
            return ResponseEntity.ok(preferencias);

        } catch (Exception e) {
            log.error("Error al obtener preferencias de notificación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al cargar preferencias"));
        }
    }

    /**
     * Actualiza las preferencias de notificación del usuario
     * PUT /api/notificaciones/preferencias
     */
    @PutMapping("/preferencias")
    public ResponseEntity<?> actualizarPreferencias(
            @RequestBody List<PreferenciaNotificacion> preferencias,
            Authentication authentication) {
        
        try {
            Integer idUsuario = obtenerIdUsuario(authentication);
            
            if (idUsuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
            }

            // Validar que todas las preferencias pertenezcan al usuario
            Optional<Usuario> usuarioOpt = usuarioRepository.findById(idUsuario);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
            }

            Usuario usuario = usuarioOpt.get();
            
            // Actualizar cada preferencia
            for (PreferenciaNotificacion pref : preferencias) {
                pref.setUsuario(usuario);
                if (pref.getIdPreferencia() != null) {
                    // Actualizar existente
                    preferenciaService.actualizar(pref.getIdPreferencia(), pref);
                } else {
                    // Crear nueva
                    preferenciaService.crear(pref);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Preferencias actualizadas correctamente",
                "total", preferencias.size()
            ));

        } catch (Exception e) {
            log.error("Error al actualizar preferencias", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al guardar preferencias"));
        }
    }

    /**
     * Inicializa preferencias predeterminadas para todos los usuarios que no las tienen
     * POST /api/notificaciones/admin/inicializar-preferencias
     * Solo accesible para ADMIN
     */
    @PostMapping("/admin/inicializar-preferencias")
    public ResponseEntity<?> inicializarPreferenciasGlobal(Authentication authentication) {
        try {
            // Verificar permisos de admin
            Usuario usuarioActual = obtenerUsuario(authentication);
            if (usuarioActual == null || !"ADMIN".equals(usuarioActual.getRol())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo administradores pueden ejecutar esta acción"));
            }

            log.info("🔧 Iniciando inicialización de preferencias para todos los usuarios");
            
            // Obtener todos los usuarios activos
            List<Usuario> usuarios = usuarioRepository.findByActivo(true);
            
            int usuariosConPreferencias = 0;
            int usuariosSinPreferencias = 0;
            int preferenciasCreadas = 0;
            
            for (Usuario usuario : usuarios) {
                // Verificar si ya tiene preferencias
                List<PreferenciaNotificacion> preferenciasExistentes = 
                    preferenciaService.findByUsuario(usuario);
                
                if (preferenciasExistentes.isEmpty()) {
                    // No tiene preferencias, crear predeterminadas
                    log.info("📝 Creando preferencias para usuario: {} (ID: {})", 
                            usuario.getNombre(), usuario.getIdUsuario());
                    
                    List<PreferenciaNotificacion> nuevasPreferencias = 
                        preferenciaService.crearPreferenciasPredeterminadas(usuario);
                    
                    preferenciasCreadas += nuevasPreferencias.size();
                    usuariosSinPreferencias++;
                } else {
                    usuariosConPreferencias++;
                    log.debug("Usuario {} ya tiene {} preferencias configuradas", 
                            usuario.getNombre(), preferenciasExistentes.size());
                }
            }
            
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("success", true);
            resultado.put("message", "Inicialización completada exitosamente");
            resultado.put("totalUsuarios", usuarios.size());
            resultado.put("usuariosConPreferenciasExistentes", usuariosConPreferencias);
            resultado.put("usuariosSinPreferencias", usuariosSinPreferencias);
            resultado.put("preferenciasCreadas", preferenciasCreadas);
            
            log.info("✅ Inicialización completada: {} preferencias creadas para {} usuarios", 
                    preferenciasCreadas, usuariosSinPreferencias);
            
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("❌ Error al inicializar preferencias globalmente", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al inicializar preferencias: " + e.getMessage()));
        }
    }

    /**
     * Inicializa preferencias predeterminadas para el usuario actual
     * POST /api/notificaciones/preferencias/inicializar
     */
    @PostMapping("/preferencias/inicializar")
    public ResponseEntity<?> inicializarPreferenciasUsuario(Authentication authentication) {
        try {
            // Obtener usuario completo
            Usuario usuario = obtenerUsuario(authentication);
            
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
            }

            // Verificar si ya tiene preferencias
            List<PreferenciaNotificacion> preferenciasExistentes = 
                preferenciaService.findByUsuario(usuario);
            
            if (!preferenciasExistentes.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "El usuario ya tiene preferencias configuradas",
                    "totalPreferencias", preferenciasExistentes.size()
                ));
            }
            
            // Crear preferencias predeterminadas
            log.info("📝 Creando preferencias predeterminadas para usuario: {}", usuario.getNombre());
            List<PreferenciaNotificacion> preferencias = 
                preferenciaService.crearPreferenciasPredeterminadas(usuario);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Preferencias inicializadas correctamente",
                "totalPreferencias", preferencias.size(),
                "preferencias", preferencias
            ));

        } catch (Exception e) {
            log.error("Error al inicializar preferencias del usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al inicializar preferencias"));
        }
    }

    /**
     * Obtiene el usuario autenticado completo
     */
    private Usuario obtenerUsuario(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        String username = authentication.getName();
        
        // Buscar por nombre o teléfono
        Optional<Usuario> usuarioOpt = usuarioRepository.findByNombre(username);
        if (!usuarioOpt.isPresent()) {
            usuarioOpt = usuarioRepository.findByEmail(username);
        }
        
        return usuarioOpt.orElse(null);
    }

    /**
     * Obtiene el ID del usuario autenticado
     */
    private Integer obtenerIdUsuario(Authentication authentication) {
        Usuario usuario = obtenerUsuario(authentication);
        return usuario != null ? usuario.getIdUsuario() : null;
    }
}
