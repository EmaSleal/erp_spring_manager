package api.astro.whats_orders_manager.modules.seguridad.service.impl;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.model.UsuarioActividad;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioActividadRepository;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * USUARIO ACTIVIDAD SERVICE IMPLEMENTATION
 * ERP Orders Manager
 * ============================================================================
 * Implementación del servicio de actividades de usuarios.
 * Maneja el registro automático de auditoría.
 * 
 * @version 1.0 - Sprint 4
 * @since 22/12/2025
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioActividadServiceImpl implements UsuarioActividadService {
    
    private final UsuarioActividadRepository actividadRepository;
    private final UsuarioRepository usuarioRepository;

    // ==================== CRUD BÁSICO ====================

    @Override
    public List<UsuarioActividad> findAll() {
        log.debug("Buscando todas las actividades");
        return actividadRepository.findAll();
    }

    @Override
    public Page<UsuarioActividad> findAll(Pageable pageable) {
        log.debug("Buscando actividades con paginación: {}", pageable);
        return actividadRepository.findAll(pageable);
    }

    @Override
    public Optional<UsuarioActividad> findById(Long id) {
        log.debug("Buscando actividad por ID: {}", id);
        return actividadRepository.findById(id);
    }

    @Override
    @Transactional
    public UsuarioActividad save(UsuarioActividad actividad) {
        log.debug("Guardando actividad: {}", actividad.getTipoActividad());
        return actividadRepository.save(actividad);
    }

    // ==================== REGISTRO DE ACTIVIDADES ====================

    @Override
    @Transactional
    public void registrarActividad(Integer idUsuario, String tipo, String descripcion) {
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            
            UsuarioActividad actividad = UsuarioActividad.builder()
                    .usuario(usuario)
                    .tipoActividad(tipo)
                    .descripcion(descripcion)
                    .nivel("INFO")
                    .resultado("SUCCESS")
                    .ipAddress(obtenerIPActual())
                    .userAgent(obtenerUserAgentActual())
                    .build();
            
            actividadRepository.save(actividad);
            log.debug("Actividad registrada: {} - {}", tipo, descripcion);
        } catch (Exception e) {
            log.error("Error al registrar actividad: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void registrarActividad(Integer idUsuario, String tipo, String descripcion, 
                                   String entidad, Integer idEntidad) {
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            
            UsuarioActividad actividad = UsuarioActividad.builder()
                    .usuario(usuario)
                    .tipoActividad(tipo)
                    .descripcion(descripcion)
                    .entidad(entidad)
                    .idEntidad(idEntidad)
                    .nivel("INFO")
                    .resultado("SUCCESS")
                    .ipAddress(obtenerIPActual())
                    .userAgent(obtenerUserAgentActual())
                    .build();
            
            actividadRepository.save(actividad);
            log.debug("Actividad registrada: {} - {} [{}:{}]", tipo, descripcion, entidad, idEntidad);
        } catch (Exception e) {
            log.error("Error al registrar actividad: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void registrarActividadCompleta(Integer idUsuario, String tipo, String descripcion,
                                         String entidad, Integer idEntidad, String metadata, String nivel) {
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            
            UsuarioActividad actividad = UsuarioActividad.builder()
                    .usuario(usuario)
                    .tipoActividad(tipo)
                    .descripcion(descripcion)
                    .entidad(entidad)
                    .idEntidad(idEntidad)
                    .metadata(metadata)
                    .nivel(nivel)
                    .resultado("SUCCESS")
                    .ipAddress(obtenerIPActual())
                    .userAgent(obtenerUserAgentActual())
                    .build();
            
            actividadRepository.save(actividad);
            log.info("Actividad {} registrada: {}", nivel, descripcion);
        } catch (Exception e) {
            log.error("Error al registrar actividad completa: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void registrarActividadFallida(Integer idUsuario, String tipo, String descripcion, String errorMensaje) {
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            
            UsuarioActividad actividad = UsuarioActividad.builder()
                    .usuario(usuario)
                    .tipoActividad(tipo)
                    .descripcion(descripcion)
                    .nivel("WARNING")
                    .resultado("FAILURE")
                    .errorMensaje(errorMensaje)
                    .ipAddress(obtenerIPActual())
                    .userAgent(obtenerUserAgentActual())
                    .build();
            
            actividadRepository.save(actividad);
            log.warn("Actividad fallida registrada: {} - {}", tipo, descripcion);
        } catch (Exception e) {
            log.error("Error al registrar actividad fallida: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void registrarLogin(Integer idUsuario, String ipAddress, String userAgent) {
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            
            UsuarioActividad actividad = UsuarioActividad.builder()
                    .usuario(usuario)
                    .tipoActividad("LOGIN")
                    .descripcion("Inicio de sesión exitoso")
                    .nivel("INFO")
                    .resultado("SUCCESS")
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            
            actividadRepository.save(actividad);
            log.info("Login registrado para usuario ID: {}", idUsuario);
        } catch (Exception e) {
            log.error("Error al registrar login: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void registrarLoginFallido(String telefono, String ipAddress, String motivo) {
        try {
            // Intentar encontrar usuario por teléfono
            Usuario usuario = usuarioRepository.findByTelefono(telefono).orElse(null);
            
            UsuarioActividad actividad = UsuarioActividad.builder()
                    .usuario(usuario)
                    .tipoActividad("LOGIN")
                    .descripcion("Intento de inicio de sesión fallido: " + motivo)
                    .nivel("WARNING")
                    .resultado("FAILURE")
                    .errorMensaje(motivo)
                    .ipAddress(ipAddress)
                    .build();
            
            actividadRepository.save(actividad);
            log.warn("Login fallido registrado para teléfono: {}", telefono);
        } catch (Exception e) {
            log.error("Error al registrar login fallido: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void registrarLogout(Integer idUsuario) {
        try {
            Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
            
            UsuarioActividad actividad = UsuarioActividad.builder()
                    .usuario(usuario)
                    .tipoActividad("LOGOUT")
                    .descripcion("Cierre de sesión")
                    .nivel("INFO")
                    .resultado("SUCCESS")
                    .ipAddress(obtenerIPActual())
                    .userAgent(obtenerUserAgentActual())
                    .build();
            
            actividadRepository.save(actividad);
            log.info("Logout registrado para usuario ID: {}", idUsuario);
        } catch (Exception e) {
            log.error("Error al registrar logout: {}", e.getMessage());
        }
    }

    // ==================== BÚSQUEDAS ====================

    @Override
    public List<UsuarioActividad> findByUsuario(Integer idUsuario) {
        log.debug("Buscando actividades del usuario ID: {}", idUsuario);
        return actividadRepository.findByUsuario_IdUsuario(idUsuario);
    }

    @Override
    public Page<UsuarioActividad> findByUsuario(Integer idUsuario, Pageable pageable) {
        log.debug("Buscando actividades del usuario ID: {} con paginación", idUsuario);
        return actividadRepository.findByUsuario_IdUsuario(idUsuario, pageable);
    }

    @Override
    public List<UsuarioActividad> findByTipoActividad(String tipo) {
        log.debug("Buscando actividades por tipo: {}", tipo);
        return actividadRepository.findByTipoActividad(tipo);
    }

    @Override
    public List<UsuarioActividad> findByNivel(String nivel) {
        log.debug("Buscando actividades por nivel: {}", nivel);
        return actividadRepository.findByNivel(nivel);
    }

    @Override
    public List<UsuarioActividad> findByResultado(String resultado) {
        log.debug("Buscando actividades por resultado: {}", resultado);
        return actividadRepository.findByResultado(resultado);
    }

    @Override
    public List<UsuarioActividad> findByUsuarioAndFechas(Integer idUsuario, 
                                                         LocalDateTime fechaInicio, 
                                                         LocalDateTime fechaFin) {
        log.debug("Buscando actividades del usuario {} entre {} y {}", idUsuario, fechaInicio, fechaFin);
        return actividadRepository.findByUsuarioAndFechas(idUsuario, fechaInicio, fechaFin);
    }

    @Override
    public List<UsuarioActividad> findByEntidad(String entidad, Integer idEntidad) {
        log.debug("Buscando actividades de la entidad {}:{}", entidad, idEntidad);
        return actividadRepository.findByEntidadAndIdEntidad(entidad, idEntidad);
    }

    @Override
    public List<UsuarioActividad> findByUsuarioAndEntidad(Integer idUsuario, String entidad, Integer idEntidad) {
        log.debug("Buscando actividades del usuario {} sobre {}:{}", idUsuario, entidad, idEntidad);
        return actividadRepository.findByUsuarioAndEntidad(idUsuario, entidad, idEntidad);
    }

    // ==================== SEGURIDAD ====================

    @Override
    public List<UsuarioActividad> findActividadesCriticas() {
        log.debug("Buscando actividades críticas");
        return actividadRepository.findActividadesCriticas();
    }

    @Override
    public List<UsuarioActividad> findActividadesFallidas(Integer idUsuario) {
        log.debug("Buscando actividades fallidas del usuario ID: {}", idUsuario);
        return actividadRepository.findActividadesFallidasByUsuario(idUsuario);
    }

    @Override
    public List<UsuarioActividad> findActividadesSospechosas(int horasAtras, int intentosMinimos) {
        log.debug("Buscando actividades sospechosas (últimas {} horas, mínimo {} intentos)", 
                horasAtras, intentosMinimos);
        LocalDateTime fechaLimite = LocalDateTime.now().minusHours(horasAtras);
        return actividadRepository.findActividadesSospechosas(fechaLimite, (long) intentosMinimos);
    }

    @Override
    public List<UsuarioActividad> findByIpAddress(String ipAddress) {
        log.debug("Buscando actividades desde IP: {}", ipAddress);
        return actividadRepository.findByIpAddress(ipAddress);
    }

    // ==================== ESTADÍSTICAS ====================

    @Override
    public long countByUsuario(Integer idUsuario) {
        return actividadRepository.countByUsuario_IdUsuario(idUsuario);
    }

    @Override
    public long countByTipoActividad(String tipo) {
        return actividadRepository.countByTipoActividad(tipo);
    }

    @Override
    public long countByNivel(String nivel) {
        return actividadRepository.countByNivel(nivel);
    }

    @Override
    public long countByResultado(String resultado) {
        return actividadRepository.countByResultado(resultado);
    }

    @Override
    public long countByUsuarioAndTipo(Integer idUsuario, String tipo) {
        return actividadRepository.countByUsuarioAndTipo(idUsuario, tipo);
    }

    // ==================== REPORTES ====================

    @Override
    public List<UsuarioActividad> getUltimasActividades(Integer idUsuario, int limite) {
        log.debug("Obteniendo últimas {} actividades del usuario ID: {}", limite, idUsuario);
        return actividadRepository.findUltimasActividadesByUsuario(idUsuario, limite);
    }

    @Override
    public List<UsuarioActividad> getActividadesRecientes(int limite) {
        log.debug("Obteniendo últimas {} actividades del sistema", limite);
        return actividadRepository.findActividadesRecientes(limite);
    }

    @Override
    public List<UsuarioActividad> getActividadesHoy() {
        LocalDateTime inicioDia = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime finDia = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return actividadRepository.findByFechas(inicioDia, finDia, Pageable.unpaged()).getContent();
    }

    @Override
    public List<UsuarioActividad> getActividadesUltimaSemana() {
        LocalDateTime hace7Dias = LocalDateTime.now().minusDays(7);
        LocalDateTime ahora = LocalDateTime.now();
        return actividadRepository.findByFechas(hace7Dias, ahora, Pageable.unpaged()).getContent();
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Obtiene la IP del request actual
     */
    private String obtenerIPActual() {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener IP del request");
        }
        return "SYSTEM";
    }

    /**
     * Obtiene el User Agent del request actual
     */
    private String obtenerUserAgentActual() {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener User-Agent del request");
        }
        return "SYSTEM";
    }
}
