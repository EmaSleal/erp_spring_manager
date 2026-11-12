package api.astro.whats_orders_manager.modules.notificacion.service.impl;

import api.astro.whats_orders_manager.modules.notificacion.dto.PreferenciaTipoDTO;
import api.astro.whats_orders_manager.modules.notificacion.model.PreferenciaNotificacion;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.repository.PreferenciaNotificacionRepository;
import api.astro.whats_orders_manager.modules.notificacion.service.PreferenciaNotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * ============================================================================
 * PREFERENCIA NOTIFICACIÓN SERVICE IMPLEMENTATION
 * ERP Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Implementación del servicio de gestión de preferencias de notificaciones.
 * 
 * Responsabilidades:
 * - CRUD completo de preferencias por usuario
 * - Validación compleja de permisos (específica > tipo > canal > global)
 * - Configuración de horarios y frecuencias
 * - Preferencias predeterminadas para nuevos usuarios
 * - Operaciones masivas de activación/desactivación
 * ============================================================================
 */
@Slf4j
@Service
public class PreferenciaNotificacionServiceImpl implements PreferenciaNotificacionService {

    @Autowired
    private PreferenciaNotificacionRepository preferenciaRepository;

    // ==================== CRUD ====================

    @Override
    public Optional<PreferenciaNotificacion> findById(Integer idPreferencia) {
        return preferenciaRepository.findById(idPreferencia);
    }

    @Override
    public List<PreferenciaNotificacion> findByUsuario(Usuario usuario) {
        return preferenciaRepository.findByUsuario(usuario);
    }

    @Override
    public Page<PreferenciaNotificacion> findByUsuario(Usuario usuario, Pageable pageable) {
        List<PreferenciaNotificacion> lista = findByUsuario(usuario);
        return convertirAPage(lista, pageable);
    }

    @Override
    @Transactional
    public PreferenciaNotificacion crear(PreferenciaNotificacion preferencia) {
        log.info("📝 Creando nueva preferencia para usuario {}", preferencia.getUsuario().getIdUsuario());

        // Validar que no exista duplicada
        Optional<PreferenciaNotificacion> existente = preferenciaRepository
            .findByUsuarioAndTipoNotificacionAndCanal(
                preferencia.getUsuario(),
                preferencia.getTipoNotificacion(),
                preferencia.getCanal()
            );

        if (existente.isPresent()) {
            throw new IllegalArgumentException(
                "Ya existe una preferencia para este usuario, tipo y canal"
            );
        }

        PreferenciaNotificacion guardada = preferenciaRepository.save(preferencia);
        log.info("✅ Preferencia creada con ID: {}", guardada.getIdPreferencia());

        return guardada;
    }

    @Override
    @Transactional
    public PreferenciaNotificacion actualizar(Integer idPreferencia, PreferenciaNotificacion preferencia) {
        log.info("📝 Actualizando preferencia ID: {}", idPreferencia);

        PreferenciaNotificacion existente = preferenciaRepository.findById(idPreferencia)
            .orElseThrow(() -> new IllegalArgumentException("Preferencia no encontrada: " + idPreferencia));

        // Actualizar campos
        existente.setActiva(preferencia.getActiva());
        existente.setNotificacionesDesactivadasGlobal(preferencia.getNotificacionesDesactivadasGlobal());
        existente.setFrecuencia(preferencia.getFrecuencia());
        existente.setHoraPreferida(preferencia.getHoraPreferida());
        existente.setSoloHorarioLaboral(preferencia.getSoloHorarioLaboral());

        PreferenciaNotificacion actualizada = preferenciaRepository.save(existente);
        log.info("✅ Preferencia actualizada exitosamente");

        return actualizada;
    }

    @Override
    @Transactional
    public boolean eliminar(Integer idPreferencia) {
        log.info("🗑️ Eliminando preferencia ID: {}", idPreferencia);

        if (!preferenciaRepository.existsById(idPreferencia)) {
            return false;
        }

        preferenciaRepository.deleteById(idPreferencia);
        log.info("✅ Preferencia eliminada exitosamente");

        return true;
    }

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================

    @Override
    public Optional<PreferenciaNotificacion> findByUsuarioTipoCanal(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal) {
        return preferenciaRepository.findByUsuarioAndTipoNotificacionAndCanal(usuario, tipo, canal);
    }

    @Override
    public List<PreferenciaNotificacion> findByUsuarioAndTipo(Usuario usuario, TipoNotificacion tipo) {
        return preferenciaRepository.findByUsuarioAndTipoNotificacion(usuario, tipo);
    }

    @Override
    public List<PreferenciaNotificacion> findByUsuarioAndCanal(Usuario usuario, CanalNotificacion canal) {
        return preferenciaRepository.findByUsuarioAndCanal(usuario, canal);
    }

    @Override
    public Optional<PreferenciaNotificacion> findPreferenciaGlobal(Usuario usuario) {
        return preferenciaRepository.findPreferenciaGlobal(usuario);
    }

    @Override
    public List<PreferenciaNotificacion> findActivas(Usuario usuario) {
        return preferenciaRepository.findByUsuarioAndActivaTrue(usuario);
    }

    @Override
    public List<PreferenciaNotificacion> findPreferenciasAplicables(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal) {
        return preferenciaRepository.findPreferenciasAplicables(usuario.getIdUsuario(), tipo, canal);
    }

    // ==================== VALIDACIÓN DE PERMISOS ====================

    @Override
    public boolean usuarioAceptaNotificacion(Usuario usuario, TipoNotificacion tipo, CanalNotificacion canal) {
        return usuarioAceptaNotificacionPorId(usuario.getIdUsuario(), tipo, canal);
    }

    @Override
    public boolean usuarioAceptaNotificacionPorId(Integer idUsuario, TipoNotificacion tipo, CanalNotificacion canal) {
        return aceptaSegunPreferencias(preferenciaRepository.findByUsuarioId(idUsuario), tipo, canal);
    }

    /**
     * Regla de aceptación (T-030, P-032). notificacionesDesactivadasGlobal = true en cualquier fila del usuario
     * bloquea siempre. Si no, decide la fila aplicable más específica, en el orden (tipo, canal) → (tipo, NULL) →
     * (NULL, canal) → (NULL, NULL), según su campo activa; entre filas del mismo nivel, la de menor id.
     * Sin ninguna fila aplicable no se acepta.
     */
    static boolean aceptaSegunPreferencias(
            List<PreferenciaNotificacion> preferencias,
            TipoNotificacion tipo,
            CanalNotificacion canal) {
        if (preferencias.stream().anyMatch(p -> Boolean.TRUE.equals(p.getNotificacionesDesactivadasGlobal()))) {
            return false;
        }
        return preferencias.stream()
            .filter(p -> (p.getTipoNotificacion() == null || p.getTipoNotificacion() == tipo)
                && (p.getCanal() == null || p.getCanal() == canal))
            .min(Comparator.comparingInt(PreferenciaNotificacionServiceImpl::nivelDeEspecificidad)
                .thenComparing(PreferenciaNotificacion::getIdPreferencia,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .map(p -> Boolean.TRUE.equals(p.getActiva()))
            .orElse(false);
    }

    private static int nivelDeEspecificidad(PreferenciaNotificacion p) {
        if (p.esEspecifica()) {
            return 1;
        }
        if (p.esPorTipo()) {
            return 2;
        }
        return p.esPorCanal() ? 3 : 4;
    }

    @Override
    public boolean tieneNotificacionesDesactivadas(Usuario usuario) {
        Optional<PreferenciaNotificacion> global = findPreferenciaGlobal(usuario);
        
        return global.isPresent() && 
               Boolean.TRUE.equals(global.get().getNotificacionesDesactivadasGlobal());
    }

    @Override
    public boolean sePuedeEnviarAhora(PreferenciaNotificacion preferencia) {
        if (preferencia == null) {
            return true;
        }

        return preferencia.sePuedeEnviarAhora();
    }

    // ==================== OPERACIONES MASIVAS ====================

    @Override
    @Transactional
    public boolean activar(Integer idPreferencia) {
        log.debug("Activando preferencia ID: {}", idPreferencia);
        int updated = preferenciaRepository.activarPreferencia(idPreferencia);
        return updated > 0;
    }

    @Override
    @Transactional
    public boolean desactivar(Integer idPreferencia) {
        log.debug("Desactivando preferencia ID: {}", idPreferencia);
        int updated = preferenciaRepository.desactivarPreferencia(idPreferencia);
        return updated > 0;
    }

    @Override
    @Transactional
    public int desactivarTodasLasNotificaciones(Usuario usuario) {
        log.info("🔕 Desactivando todas las notificaciones para usuario {}", usuario.getIdUsuario());
        
        int desactivadas = preferenciaRepository.desactivarTodasLasNotificaciones(usuario.getIdUsuario());
        
        // Crear/actualizar preferencia global desactivada
        Optional<PreferenciaNotificacion> global = findPreferenciaGlobal(usuario);
        
        if (global.isPresent()) {
            PreferenciaNotificacion pref = global.get();
            pref.setNotificacionesDesactivadasGlobal(true);
            pref.setActiva(false);
            preferenciaRepository.save(pref);
        } else {
            // Crear preferencia global desactivada
            PreferenciaNotificacion nueva = PreferenciaNotificacion.builder()
                .usuario(usuario)
                .tipoNotificacion(null)  // NULL = todos los tipos
                .canal(null)             // NULL = todos los canales
                .activa(false)
                .notificacionesDesactivadasGlobal(true)
                .build();
            preferenciaRepository.save(nueva);
        }
        
        log.info("✅ Desactivadas {} preferencias + preferencia global", desactivadas);
        
        return desactivadas + 1;
    }

    @Override
    @Transactional
    public int reactivarTodasLasNotificaciones(Usuario usuario) {
        log.info("🔔 Reactivando todas las notificaciones para usuario {}", usuario.getIdUsuario());
        
        // Activar todas las preferencias manualmente
        List<PreferenciaNotificacion> todas = findByUsuario(usuario);
        int reactivadas = 0;
        
        for (PreferenciaNotificacion pref : todas) {
            pref.setActiva(true);
            pref.setNotificacionesDesactivadasGlobal(false);
            preferenciaRepository.save(pref);
            reactivadas++;
        }
        
        // Actualizar preferencia global si existe
        Optional<PreferenciaNotificacion> global = findPreferenciaGlobal(usuario);
        
        if (global.isPresent()) {
            PreferenciaNotificacion pref = global.get();
            pref.setNotificacionesDesactivadasGlobal(false);
            pref.setActiva(true);
            preferenciaRepository.save(pref);
        }
        
        log.info("✅ Reactivadas {} preferencias", reactivadas);
        
        return reactivadas;
    }

    @Override
    @Transactional
    public int desactivarPorTipo(Usuario usuario, TipoNotificacion tipo) {
        log.info("🔕 Desactivando notificaciones tipo {} para usuario {}", tipo, usuario.getIdUsuario());
        
        List<PreferenciaNotificacion> porTipo = findByUsuarioAndTipo(usuario, tipo);
        int desactivadas = 0;
        
        for (PreferenciaNotificacion pref : porTipo) {
            pref.setActiva(false);
            preferenciaRepository.save(pref);
            desactivadas++;
        }
        
        log.info("✅ Desactivadas {} preferencias", desactivadas);
        
        return desactivadas;
    }

    @Override
    @Transactional
    public int desactivarPorCanal(Usuario usuario, CanalNotificacion canal) {
        log.info("🔕 Desactivando notificaciones canal {} para usuario {}", canal, usuario.getIdUsuario());
        
        List<PreferenciaNotificacion> porCanal = findByUsuarioAndCanal(usuario, canal);
        int desactivadas = 0;
        
        for (PreferenciaNotificacion pref : porCanal) {
            pref.setActiva(false);
            preferenciaRepository.save(pref);
            desactivadas++;
        }
        
        log.info("✅ Desactivadas {} preferencias", desactivadas);
        
        return desactivadas;
    }

    // ==================== CONFIGURACIÓN PREDETERMINADA ====================

    @Override
    @Transactional
    public List<PreferenciaNotificacion> crearPreferenciasPredeterminadas(Usuario usuario) {
        log.info("🆕 Creando preferencias predeterminadas para usuario {}", usuario.getIdUsuario());

        List<PreferenciaNotificacion> preferencias = new ArrayList<>();

        // Preferencia global: ACTIVADA por defecto
        PreferenciaNotificacion global = PreferenciaNotificacion.builder()
            .usuario(usuario)
            .tipoNotificacion(null)
            .canal(null)
            .activa(true)
            .notificacionesDesactivadasGlobal(false)
            .frecuencia("INMEDIATA")
            .soloHorarioLaboral(false)
            .build();
        
        preferencias.add(preferenciaRepository.save(global));

        // Preferencias específicas para tipos críticos
        // Facturas vencidas - Activadas para todos los canales
        for (CanalNotificacion canal : CanalNotificacion.getCanalesDisponibles()) {
            PreferenciaNotificacion pref = PreferenciaNotificacion.builder()
                .usuario(usuario)
                .tipoNotificacion(TipoNotificacion.FACTURA_VENCIDA)
                .canal(canal)
                .activa(true)
                .notificacionesDesactivadasGlobal(false)
                .frecuencia("INMEDIATA")
                .build();
            
            preferencias.add(preferenciaRepository.save(pref));
        }

        // Stock bajo - Solo WEB y EMAIL por defecto
        for (CanalNotificacion canal : new CanalNotificacion[]{CanalNotificacion.WEB, CanalNotificacion.EMAIL}) {
            PreferenciaNotificacion pref = PreferenciaNotificacion.builder()
                .usuario(usuario)
                .tipoNotificacion(TipoNotificacion.STOCK_BAJO)
                .canal(canal)
                .activa(true)
                .notificacionesDesactivadasGlobal(false)
                .frecuencia("DIARIA")
                .soloHorarioLaboral(true)
                .build();
            
            preferencias.add(preferenciaRepository.save(pref));
        }

        log.info("✅ Creadas {} preferencias predeterminadas", preferencias.size());

        return preferencias;
    }

    @Override
    public PreferenciaNotificacion obtenerConfiguracionPredeterminada(
            TipoNotificacion tipo,
            CanalNotificacion canal) {
        
        // Retornar configuración por defecto según tipo y canal
        PreferenciaNotificacion config = PreferenciaNotificacion.builder()
            .tipoNotificacion(tipo)
            .canal(canal)
            .activa(true)
            .notificacionesDesactivadasGlobal(false)
            .build();

        // Configuración específica según tipo
        if (tipo != null && tipo.esCritica()) {
            config.setFrecuencia("INMEDIATA");
            config.setSoloHorarioLaboral(false);
        } else {
            config.setFrecuencia("DIARIA");
            config.setSoloHorarioLaboral(true);
        }

        return config;
    }

    @Override
    @Transactional
    public PreferenciaNotificacion crearOActualizar(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            Boolean activa) {
        
        log.debug("Crear o actualizar preferencia: usuario={}, tipo={}, canal={}, activa={}", 
            usuario.getIdUsuario(), tipo, canal, activa);

        Optional<PreferenciaNotificacion> existente = findByUsuarioTipoCanal(usuario, tipo, canal);

        if (existente.isPresent()) {
            // Actualizar
            PreferenciaNotificacion pref = existente.get();
            pref.setActiva(activa);
            return preferenciaRepository.save(pref);
        } else {
            // Crear nueva
            PreferenciaNotificacion nueva = PreferenciaNotificacion.builder()
                .usuario(usuario)
                .tipoNotificacion(tipo)
                .canal(canal)
                .activa(activa)
                .notificacionesDesactivadasGlobal(false)
                .frecuencia("INMEDIATA")
                .build();
            
            return preferenciaRepository.save(nueva);
        }
    }

    @Override
    public List<PreferenciaTipoDTO> listarPorTipo(Usuario usuario) {
        // Valor efectivo por tipo y canal: la misma regla que aplica el envío
        List<PreferenciaNotificacion> preferencias = findByUsuario(usuario);

        List<PreferenciaTipoDTO> resultado = new ArrayList<>();
        for (TipoNotificacion tipo : TipoNotificacion.values()) {
            resultado.add(PreferenciaTipoDTO.builder()
                .tipo(tipo)
                .activoWeb(aceptaSegunPreferencias(preferencias, tipo, CanalNotificacion.WEB))
                .activoEmail(aceptaSegunPreferencias(preferencias, tipo, CanalNotificacion.EMAIL))
                .activoWhatsapp(aceptaSegunPreferencias(preferencias, tipo, CanalNotificacion.WHATSAPP))
                .build());
        }
        return resultado;
    }

    @Override
    @Transactional
    public List<PreferenciaTipoDTO> guardarPorTipo(Usuario usuario, List<PreferenciaTipoDTO> preferencias) {
        validarPreferenciasPorTipo(preferencias);

        for (PreferenciaTipoDTO pref : preferencias) {
            for (CanalNotificacion canal : PreferenciaTipoDTO.CANALES) {
                crearOActualizar(usuario, pref.getTipo(), canal, pref.activoPara(canal));
            }
        }
        log.info("✅ Guardadas preferencias de {} tipos para usuario {}", preferencias.size(), usuario.getIdUsuario());

        return listarPorTipo(usuario);
    }

    private static void validarPreferenciasPorTipo(List<PreferenciaTipoDTO> preferencias) {
        if (preferencias == null) {
            throw new IllegalArgumentException("Debe enviar la lista de preferencias");
        }
        Set<TipoNotificacion> tipos = EnumSet.noneOf(TipoNotificacion.class);
        for (PreferenciaTipoDTO pref : preferencias) {
            if (pref == null || pref.getTipo() == null) {
                throw new IllegalArgumentException("Cada preferencia debe indicar el tipo de notificación");
            }
            if (!tipos.add(pref.getTipo())) {
                throw new IllegalArgumentException("El tipo " + pref.getTipo() + " está repetido");
            }
            for (CanalNotificacion canal : PreferenciaTipoDTO.CANALES) {
                if (pref.activoPara(canal) == null) {
                    throw new IllegalArgumentException(
                        "Falta el estado del canal " + canal + " para el tipo " + pref.getTipo());
                }
            }
        }
    }

    // ==================== CONFIGURACIÓN DE HORARIOS ====================

    @Override
    @Transactional
    public boolean configurarHorario(Integer idPreferencia, String horaPreferida) {
        log.debug("Configurando horario preferido {} para preferencia {}", horaPreferida, idPreferencia);

        PreferenciaNotificacion preferencia = preferenciaRepository.findById(idPreferencia)
            .orElseThrow(() -> new IllegalArgumentException("Preferencia no encontrada: " + idPreferencia));

        preferencia.setHoraPreferida(horaPreferida);
        preferenciaRepository.save(preferencia);

        return true;
    }

    @Override
    @Transactional
    public boolean configurarHorarioLaboral(Integer idPreferencia, Boolean soloHorarioLaboral) {
        log.debug("Configurando horario laboral {} para preferencia {}", soloHorarioLaboral, idPreferencia);

        PreferenciaNotificacion preferencia = preferenciaRepository.findById(idPreferencia)
            .orElseThrow(() -> new IllegalArgumentException("Preferencia no encontrada: " + idPreferencia));

        preferencia.setSoloHorarioLaboral(soloHorarioLaboral);
        preferenciaRepository.save(preferencia);

        return true;
    }

    @Override
    @Transactional
    public boolean configurarFrecuencia(Integer idPreferencia, String frecuencia) {
        log.debug("Configurando frecuencia {} para preferencia {}", frecuencia, idPreferencia);

        PreferenciaNotificacion preferencia = preferenciaRepository.findById(idPreferencia)
            .orElseThrow(() -> new IllegalArgumentException("Preferencia no encontrada: " + idPreferencia));

        // Validar frecuencia
        if (!Arrays.asList("INMEDIATA", "DIARIA", "SEMANAL").contains(frecuencia)) {
            throw new IllegalArgumentException("Frecuencia inválida: " + frecuencia);
        }

        preferencia.setFrecuencia(frecuencia);
        preferenciaRepository.save(preferencia);

        return true;
    }

    // ==================== ESTADÍSTICAS ====================

    @Override
    public long contarUsuariosConNotificacionesActivas() {
        return preferenciaRepository.contarUsuariosConNotificacionesActivas();
    }

    @Override
    public long contarUsuariosConNotificacionesDesactivadas() {
        return preferenciaRepository.contarUsuariosConNotificacionesDesactivadas();
    }

    @Override
    public Map<TipoNotificacion, Long> obtenerEstadisticasPorTipo() {
        List<Object[]> resultados = preferenciaRepository.obtenerEstadisticasPorTipo();
        Map<TipoNotificacion, Long> estadisticas = new HashMap<>();
        
        for (Object[] resultado : resultados) {
            TipoNotificacion tipo = (TipoNotificacion) resultado[0];
            Long count = (Long) resultado[1];
            estadisticas.put(tipo, count);
        }
        
        return estadisticas;
    }

    @Override
    public Map<CanalNotificacion, Long> obtenerEstadisticasPorCanal() {
        List<Object[]> resultados = preferenciaRepository.obtenerEstadisticasPorCanal();
        Map<CanalNotificacion, Long> estadisticas = new HashMap<>();
        
        for (Object[] resultado : resultados) {
            CanalNotificacion canal = (CanalNotificacion) resultado[0];
            Long count = (Long) resultado[1];
            estadisticas.put(canal, count);
        }
        
        return estadisticas;
    }

    @Override
    public long contarActivas(Usuario usuario) {
        return findActivas(usuario).size();
    }

    @Override
    public long contarTotal(Usuario usuario) {
        return findByUsuario(usuario).size();
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Convierte una lista a Page aplicando paginación manual
     */
    private <T> Page<T> convertirAPage(List<T> lista, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), lista.size());
        
        if (start > lista.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, lista.size());
        }
        
        List<T> subLista = lista.subList(start, end);
        return new PageImpl<>(subLista, pageable, lista.size());
    }
}
