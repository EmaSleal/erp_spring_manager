package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.models.Factura;
import api.astro.whats_orders_manager.models.Notificacion;
import api.astro.whats_orders_manager.models.PlantillaNotificacion;
import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.models.dto.NotificacionDTO;
import api.astro.whats_orders_manager.models.enums.CanalNotificacion;
import api.astro.whats_orders_manager.models.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.repositories.FacturaRepository;
import api.astro.whats_orders_manager.repositories.NotificacionRepository;
import api.astro.whats_orders_manager.repositories.UsuarioRepository;
import api.astro.whats_orders_manager.services.EmailService;
import api.astro.whats_orders_manager.services.NotificacionService;
import api.astro.whats_orders_manager.services.PlantillaNotificacionService;
import api.astro.whats_orders_manager.services.PreferenciaNotificacionService;
import api.astro.whats_orders_manager.services.WhatsAppService;
import api.astro.whats_orders_manager.controllers.NotificacionWebSocketController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * NOTIFICACIÓN SERVICE IMPLEMENTATION
 * WhatsApp Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Implementación del servicio de gestión de notificaciones.
 * 
 * Responsabilidades:
 * - Enviar notificaciones por diferentes canales (WEB, EMAIL, WHATSAPP)
 * - Validar preferencias de usuario antes de enviar
 * - Gestionar historial de notificaciones
 * - Reintentar envíos fallidos con lógica exponencial
 * - Integración con WebSocket para notificaciones en tiempo real
 * - Conversión de entidades a DTOs para API REST
 * ============================================================================
 */
@Slf4j
@Service
public class NotificacionServiceImpl implements NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PreferenciaNotificacionService preferenciaService;

    @Autowired
    private PlantillaNotificacionService plantillaService;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private WhatsAppService whatsAppService;

    @Autowired(required = false)
    @Lazy
    private NotificacionWebSocketController webSocketController;

    // ==================== ENVÍO DE NOTIFICACIONES ====================

    @Override
    @Transactional
    @Async
    public Notificacion enviarNotificacion(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            String titulo,
            String mensaje
    ) {
        log.info("📬 Enviando notificación {} por {} a usuario {}", tipo, canal, usuario.getIdUsuario());

        // Validar preferencias del usuario
        if (!preferenciaService.usuarioAceptaNotificacion(usuario, tipo, canal)) {
            log.info("⏭️ Usuario {} no acepta notificaciones {} por {}", 
                usuario.getIdUsuario(), tipo, canal);
            return null;
        }

        // Crear notificación
        Notificacion notificacion = Notificacion.builder()
                .tipo(tipo)
                .canal(canal)
                .usuario(usuario)
                .titulo(titulo)
                .mensaje(mensaje)
                .enviada(false)
                .leida(false)
                .intentosEnvio(0)
                .build();

        // Guardar en base de datos
        notificacion = notificacionRepository.save(notificacion);
        log.debug("✅ Notificación guardada con ID: {}", notificacion.getIdNotificacion());

        // Enviar según el canal
        try {
            enviarPorCanal(notificacion);
        } catch (Exception e) {
            log.error("❌ Error al enviar notificación ID {}: {}", 
                notificacion.getIdNotificacion(), e.getMessage(), e);
            notificacion.registrarError(e.getMessage());
            notificacionRepository.save(notificacion);
        }

        return notificacion;
    }

    @Override
    @Transactional
    @Async
    public Notificacion enviarNotificacionConAccion(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            String titulo,
            String mensaje,
            String urlAccion,
            String textoBoton
    ) {
        log.info("📬 Enviando notificación {} por {} con acción a usuario {}", 
            tipo, canal, usuario.getIdUsuario());

        // Validar preferencias
        if (!preferenciaService.usuarioAceptaNotificacion(usuario, tipo, canal)) {
            log.info("⏭️ Usuario {} no acepta notificaciones {} por {}", 
                usuario.getIdUsuario(), tipo, canal);
            return null;
        }

        // Crear notificación con acción
        Notificacion notificacion = Notificacion.builder()
                .tipo(tipo)
                .canal(canal)
                .usuario(usuario)
                .titulo(titulo)
                .mensaje(mensaje)
                .urlAccion(urlAccion)
                .textoBoton(textoBoton)
                .enviada(false)
                .leida(false)
                .intentosEnvio(0)
                .build();

        notificacion = notificacionRepository.save(notificacion);

        try {
            enviarPorCanal(notificacion);
        } catch (Exception e) {
            log.error("❌ Error al enviar notificación ID {}: {}", 
                notificacion.getIdNotificacion(), e.getMessage(), e);
            notificacion.registrarError(e.getMessage());
            notificacionRepository.save(notificacion);
        }

        return notificacion;
    }

    @Override
    @Transactional
    @Async
    public Notificacion enviarNotificacionConPlantilla(
            Usuario usuario,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            Integer idPlantilla,
            Map<String, Object> variables
    ) {
        log.info("📬 Enviando notificación {} por {} con plantilla {} a usuario {}", 
            tipo, canal, idPlantilla, usuario.getIdUsuario());

        // Validar preferencias
        if (!preferenciaService.usuarioAceptaNotificacion(usuario, tipo, canal)) {
            log.info("⏭️ Usuario {} no acepta notificaciones {} por {}", 
                usuario.getIdUsuario(), tipo, canal);
            return null;
        }

        // Procesar plantilla
        Map<String, String> contenidoProcesado = plantillaService.procesarPlantilla(idPlantilla, variables);

        // Crear notificación
        Notificacion notificacion = Notificacion.builder()
                .tipo(tipo)
                .canal(canal)
                .usuario(usuario)
                .titulo(contenidoProcesado.get("asunto"))
                .mensaje(contenidoProcesado.get("contenido"))
                .plantilla(plantillaService.findById(idPlantilla).orElse(null))
                .enviada(false)
                .leida(false)
                .intentosEnvio(0)
                .build();

        notificacion = notificacionRepository.save(notificacion);

        try {
            enviarPorCanal(notificacion);
        } catch (Exception e) {
            log.error("❌ Error al enviar notificación ID {}: {}", 
                notificacion.getIdNotificacion(), e.getMessage(), e);
            notificacion.registrarError(e.getMessage());
            notificacionRepository.save(notificacion);
        }

        return notificacion;
    }

    @Override
    @Transactional
    @Async
    public Notificacion enviarNotificacionExterna(
            String emailOTelefono,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            String titulo,
            String mensaje
    ) {
        log.info("📬 Enviando notificación externa {} por {} a {}", tipo, canal, emailOTelefono);

        // Crear notificación sin usuario
        Notificacion notificacion = Notificacion.builder()
                .tipo(tipo)
                .canal(canal)
                .titulo(titulo)
                .mensaje(mensaje)
                .enviada(false)
                .leida(false)
                .intentosEnvio(0)
                .build();

        // Asignar destinatario según canal
        if (canal == CanalNotificacion.EMAIL) {
            notificacion.setEmailDestinatario(emailOTelefono);
        } else if (canal == CanalNotificacion.WHATSAPP) {
            notificacion.setTelefonoDestinatario(emailOTelefono);
        }

        notificacion = notificacionRepository.save(notificacion);

        try {
            enviarPorCanal(notificacion);
        } catch (Exception e) {
            log.error("❌ Error al enviar notificación externa ID {}: {}", 
                notificacion.getIdNotificacion(), e.getMessage(), e);
            notificacion.registrarError(e.getMessage());
            notificacionRepository.save(notificacion);
        }

        return notificacion;
    }

    @Override
    public Notificacion enviarNotificacionWeb(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje) {
        return enviarNotificacion(usuario, tipo, CanalNotificacion.WEB, titulo, mensaje);
    }

    @Override
    public Notificacion enviarNotificacionEmail(Usuario usuario, TipoNotificacion tipo, String titulo, String mensaje) {
        return enviarNotificacion(usuario, tipo, CanalNotificacion.EMAIL, titulo, mensaje);
    }

    @Override
    public Notificacion enviarNotificacionWhatsApp(Usuario usuario, TipoNotificacion tipo, String mensaje) {
        return enviarNotificacion(usuario, tipo, CanalNotificacion.WHATSAPP, null, mensaje);
    }

    // ==================== CONSULTAS ====================

    @Override
    public Optional<Notificacion> findById(Integer idNotificacion) {
        return notificacionRepository.findById(idNotificacion);
    }

    @Override
    public Page<Notificacion> findByUsuario(Usuario usuario, Pageable pageable) {
        return notificacionRepository.findByUsuarioOrderByFechaEnvioDesc(usuario, pageable);
    }

    @Override
    public List<Notificacion> findNoLeidasByUsuario(Usuario usuario) {
        return notificacionRepository.findByUsuarioAndLeidaFalseOrderByFechaEnvioDesc(usuario);
    }

    @Override
    public List<Notificacion> findNoLeidasByUsuarioId(Integer idUsuario) {
        Optional<Usuario> usuario = usuarioRepository.findById(idUsuario);
        return usuario.map(this::findNoLeidasByUsuario).orElse(Collections.emptyList());
    }

    @Override
    public long countNoLeidasByUsuario(Usuario usuario) {
        return notificacionRepository.countByUsuarioAndLeidaFalse(usuario);
    }

    @Override
    public long countNoLeidasByUsuarioId(Integer idUsuario) {
        Optional<Usuario> usuario = usuarioRepository.findById(idUsuario);
        return usuario.map(this::countNoLeidasByUsuario).orElse(0L);
    }

    @Override
    public List<Notificacion> findUltimasNotificaciones(Integer idUsuario, Integer limite) {
        Pageable pageable = PageRequest.of(0, limite);
        return notificacionRepository.findUltimasNotificaciones(idUsuario, pageable);
    }

    @Override
    public Page<Notificacion> findByTipoAndCanal(TipoNotificacion tipo, CanalNotificacion canal, Pageable pageable) {
        return notificacionRepository.findByTipoAndCanalOrderByFechaEnvioDesc(tipo, canal, pageable);
    }

    @Override
    public List<Notificacion> findByFacturaRelacionada(Integer idFactura) {
        return notificacionRepository.findByIdFacturaRelacionadaOrderByFechaEnvioDesc(idFactura);
    }

    @Override
    public List<Notificacion> findByClienteRelacionado(Integer idCliente) {
        return notificacionRepository.findByIdClienteRelacionadoOrderByFechaEnvioDesc(idCliente);
    }

    // ==================== OPERACIONES ====================

    @Override
    @Transactional
    public boolean marcarComoLeida(Integer idNotificacion) {
        log.debug("Marcando notificación {} como leída", idNotificacion);
        
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        int updated = notificacionRepository.marcarComoLeida(idNotificacion, ahora);
        
        return updated > 0;
    }

    @Override
    @Transactional
    public int marcarTodasComoLeidas(Integer idUsuario) {
        log.info("Marcando todas las notificaciones de usuario {} como leídas", idUsuario);
        
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        return notificacionRepository.marcarTodasComoLeidas(idUsuario, ahora);
    }

    @Override
    @Transactional
    @Async
    public int reintentarNotificacionesFallidas(Integer maxIntentos) {
        log.info("🔄 Reintentando notificaciones fallidas (max {} intentos)", maxIntentos);
        
        List<Notificacion> fallidas = notificacionRepository.findFallidasParaReintentar(maxIntentos);
        log.info("Encontradas {} notificaciones para reintentar", fallidas.size());
        
        int exitosas = 0;
        
        for (Notificacion notificacion : fallidas) {
            try {
                log.debug("Reintentando notificación ID: {}", notificacion.getIdNotificacion());
                notificacion.setIntentosEnvio(notificacion.getIntentosEnvio() + 1);
                
                enviarPorCanal(notificacion);
                exitosas++;
                
            } catch (Exception e) {
                log.error("❌ Error al reintentar notificación ID {}: {}", 
                    notificacion.getIdNotificacion(), e.getMessage());
                notificacion.registrarError(e.getMessage());
                notificacionRepository.save(notificacion);
            }
        }
        
        log.info("✅ Reintento completado: {}/{} exitosas", exitosas, fallidas.size());
        return exitosas;
    }

    @Override
    @Transactional
    public int eliminarNotificacionesAntiguas(Integer diasAntiguedad) {
        log.info("🗑️ Eliminando notificaciones antiguas (>{} días)", diasAntiguedad);
        
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasAntiguedad);
        Timestamp timestamp = Timestamp.valueOf(fechaLimite);
        
        int eliminadas = notificacionRepository.eliminarNotificacionesAntiguas(timestamp);
        log.info("✅ Eliminadas {} notificaciones antiguas", eliminadas);
        
        return eliminadas;
    }

    // ==================== CONVERSIÓN ====================

    @Override
    public NotificacionDTO convertirADTO(Notificacion notificacion) {
        if (notificacion == null) {
            return null;
        }

        NotificacionDTO dto = NotificacionDTO.builder()
                .idNotificacion(notificacion.getIdNotificacion())
                .tipo(notificacion.getTipo())
                .canal(notificacion.getCanal())
                .titulo(notificacion.getTitulo())
                .mensaje(notificacion.getMensaje())
                .urlAccion(notificacion.getUrlAccion())
                .textoBoton(notificacion.getTextoBoton())
                .enviada(notificacion.getEnviada())
                .leida(notificacion.getLeida())
                .fechaEnvio(notificacion.getFechaEnvio())
                .fechaLectura(notificacion.getFechaLectura())
                .errorMensaje(notificacion.getErrorMensaje())
                .intentosEnvio(notificacion.getIntentosEnvio())
                .idFacturaRelacionada(notificacion.getIdFacturaRelacionada())
                .idClienteRelacionado(notificacion.getIdClienteRelacionado())
                .idProductoRelacionado(notificacion.getIdProductoRelacionado())
                .build();

        // Enriquecer con datos adicionales
        if (notificacion.getTipo() != null) {
            dto.setTipoNombre(notificacion.getTipo().name());
            dto.setTipoDescripcion(notificacion.getTipo().name());
            dto.setTipoIcono(notificacion.getTipo().getIcono());
            dto.setTipoColor(notificacion.getTipo().getColor());
        }

        if (notificacion.getCanal() != null) {
            dto.setCanalNombre(notificacion.getCanal().getNombre());
            dto.setCanalIcono(notificacion.getCanal().getIcono());
        }

        if (notificacion.getUsuario() != null) {
            dto.setNombreUsuario(notificacion.getUsuario().getNombre());
        }

        // Enriquecer con datos relacionados
        if (notificacion.getIdFacturaRelacionada() != null) {
            facturaRepository.findById(notificacion.getIdFacturaRelacionada())
                .ifPresent(factura -> dto.setNumeroFactura(factura.getNumeroFactura()));
        }

        if (notificacion.getIdClienteRelacionado() != null) {
            clienteRepository.findById(notificacion.getIdClienteRelacionado())
                .ifPresent(cliente -> dto.setNombreCliente(cliente.getNombre()));
        }

        return dto;
    }

    @Override
    public List<NotificacionDTO> convertirADTOs(List<Notificacion> notificaciones) {
        return notificaciones.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    // ==================== ESTADÍSTICAS ====================

    @Override
    public Map<TipoNotificacion, Long> obtenerEstadisticasPorTipo() {
        List<Object[]> resultados = notificacionRepository.obtenerEstadisticasPorTipo();
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
        List<Object[]> resultados = notificacionRepository.obtenerEstadisticasPorCanal();
        Map<CanalNotificacion, Long> estadisticas = new HashMap<>();
        
        for (Object[] resultado : resultados) {
            CanalNotificacion canal = (CanalNotificacion) resultado[0];
            Long count = (Long) resultado[1];
            estadisticas.put(canal, count);
        }
        
        return estadisticas;
    }

    @Override
    public long countEnviadas() {
        return notificacionRepository.countByEnviadaTrue();
    }

    @Override
    public long countFallidas() {
        return notificacionRepository.countByEnviadaFalse();
    }

    // ==================== MÉTODOS PRIVADOS ====================

    /**
     * Envía la notificación por el canal correspondiente
     * 
     * @param notificacion Notificación a enviar
     * @throws Exception Si hay error en el envío
     */
    private void enviarPorCanal(Notificacion notificacion) throws Exception {
        switch (notificacion.getCanal()) {
            case WEB:
                enviarPorWeb(notificacion);
                break;
            case EMAIL:
                enviarPorEmail(notificacion);
                break;
            case WHATSAPP:
                enviarPorWhatsApp(notificacion);
                break;
            case SMS:
                log.warn("⚠️ Canal SMS no implementado aún");
                throw new UnsupportedOperationException("Canal SMS no disponible");
            default:
                throw new IllegalArgumentException("Canal no soportado: " + notificacion.getCanal());
        }
    }

    /**
     * Envía notificación por WEB (WebSocket en tiempo real)
     * Sprint 4 Fase 3.5: Implementado con WebSocket
     */
    private void enviarPorWeb(Notificacion notificacion) {
        log.debug("📱 Enviando notificación WEB a usuario {}", 
            notificacion.getUsuario() != null ? notificacion.getUsuario().getIdUsuario() : "externo");
        
        // Guardar en BD primero
        notificacion.marcarComoEnviada();
        notificacionRepository.save(notificacion);
        
        // Enviar por WebSocket si está disponible y hay usuario
        if (webSocketController != null && notificacion.getUsuario() != null) {
            try {
                NotificacionDTO dto = convertirADTO(notificacion);
                
                // Enviar notificación por WebSocket
                webSocketController.enviarNotificacionAUsuario(
                    notificacion.getUsuario().getIdUsuario(),
                    dto
                );
                
                // Actualizar contador de no leídas
                long noLeidas = countNoLeidasByUsuarioId(notificacion.getUsuario().getIdUsuario());
                webSocketController.notificarContadorNoLeidas(
                    notificacion.getUsuario().getIdUsuario(),
                    noLeidas
                );
                
                log.info("✅ Notificación WEB enviada por WebSocket a usuario {}", 
                    notificacion.getUsuario().getIdUsuario());
                
            } catch (Exception e) {
                log.error("❌ Error al enviar por WebSocket (notificación guardada en BD): {}", 
                    e.getMessage());
                // No lanzar excepción, la notificación ya está guardada en BD
            }
        } else {
            log.warn("⚠️ WebSocket no disponible, notificación guardada solo en BD");
        }
    }

    /**
     * Envía notificación por EMAIL
     */
    private void enviarPorEmail(Notificacion notificacion) throws Exception {
        log.debug("📧 Enviando notificación EMAIL");
        
        if (emailService == null) {
            throw new UnsupportedOperationException("EmailService no configurado");
        }
        
        String destinatario = notificacion.getEmailDestinatario();
        if (destinatario == null && notificacion.getUsuario() != null) {
            destinatario = notificacion.getUsuario().getEmail();
        }
        
        if (destinatario == null || destinatario.trim().isEmpty()) {
            log.warn("⚠️ No se puede enviar email - Usuario {} no tiene email configurado", 
                    notificacion.getUsuario() != null ? notificacion.getUsuario().getIdUsuario() : "desconocido");
            
            // Marcar como fallida por falta de destinatario
            notificacion.setEnviada(false);
            notificacion.setErrorMensaje("Usuario sin email configurado");
            notificacion.setFechaEnvio(new Timestamp(System.currentTimeMillis()));
            notificacionRepository.save(notificacion);
            return; // Salir sin lanzar excepción
        }
        
        emailService.enviarEmail(destinatario, notificacion.getTitulo(), notificacion.getMensaje());
        
        notificacion.marcarComoEnviada();
        notificacionRepository.save(notificacion);
        
        log.info("✅ Notificación EMAIL enviada a {}", destinatario);
    }

    /**
     * Envía notificación por WHATSAPP
     */
    private void enviarPorWhatsApp(Notificacion notificacion) throws Exception {
        log.debug("💬 Enviando notificación WhatsApp");
        
        if (whatsAppService == null) {
            throw new UnsupportedOperationException("WhatsAppService no configurado");
        }
        
        String telefono = notificacion.getTelefonoDestinatario();
        if (telefono == null && notificacion.getUsuario() != null) {
            telefono = notificacion.getUsuario().getTelefono();
        }
        
        if (telefono == null) {
            throw new IllegalArgumentException("No se encontró teléfono destinatario");
        }
        
        // Enviar usando WhatsAppService (requiere idUsuario)
        Integer idUsuario = notificacion.getUsuario() != null ? 
            notificacion.getUsuario().getIdUsuario() : null;
        
        whatsAppService.enviarMensajeTexto(telefono, notificacion.getMensaje(), idUsuario);
        
        notificacion.marcarComoEnviada();
        notificacionRepository.save(notificacion);
        
        log.info("✅ Notificación WhatsApp enviada a {}", telefono);
    }

    @Override
    public Page<Notificacion> findNoLeidasByUsuarioId(Integer idUsuario, Pageable pageable) {
        log.debug("📋 Buscando notificaciones no leídas para usuario {} con paginación", idUsuario);
        return notificacionRepository.findByUsuarioIdAndLeida(
            idUsuario, false, pageable);
    }

    @Override
    public Page<Notificacion> findLeidasByUsuarioId(Integer idUsuario, Pageable pageable) {
        log.debug("📋 Buscando notificaciones leídas para usuario {} con paginación", idUsuario);
        return notificacionRepository.findByUsuarioIdAndLeida(
            idUsuario, true, pageable);
    }

    @Override
    public Page<Notificacion> findByUsuarioId(Integer idUsuario, Pageable pageable) {
        log.debug("📋 Buscando todas las notificaciones para usuario {} con paginación", idUsuario);
        return notificacionRepository.findByUsuarioId(idUsuario, pageable);
    }

    @Override
    public Page<Notificacion> findByUsuarioIdAndTipoAndLeida(Integer idUsuario, String tipo, Boolean leida,
            Pageable pageable) {
        log.debug("📋 Buscando notificaciones para usuario {} tipo {} leída {}", 
            idUsuario, tipo, leida);
        
        try {
            TipoNotificacion tipoEnum = TipoNotificacion.valueOf(tipo);
            return notificacionRepository.findByUsuarioIdAndTipoAndLeida(
                idUsuario, tipoEnum, leida, pageable);
        } catch (IllegalArgumentException e) {
            log.error("❌ Tipo de notificación inválido: {}", tipo);
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<Notificacion> findByUsuarioIdAndTipo(Integer idUsuario, String tipo, Pageable pageable) {
        log.debug("📋 Buscando notificaciones para usuario {} tipo {}", idUsuario, tipo);
        
        try {
            TipoNotificacion tipoEnum = TipoNotificacion.valueOf(tipo);
            return notificacionRepository.findByUsuarioIdAndTipo(
                idUsuario, tipoEnum, pageable);
        } catch (IllegalArgumentException e) {
            log.error("❌ Tipo de notificación inválido: {}", tipo);
            return Page.empty(pageable);
        }
    }

    @Override
    @Transactional
    public boolean eliminar(Integer idNotificacion) {
        log.debug("🗑️ Eliminando notificación {}", idNotificacion);
        
        try {
            Optional<Notificacion> notificacionOpt = notificacionRepository.findById(idNotificacion);
            
            if (notificacionOpt.isPresent()) {
                notificacionRepository.deleteById(idNotificacion);
                log.info("✅ Notificación {} eliminada correctamente", idNotificacion);
                return true;
            } else {
                log.warn("⚠️ Notificación {} no encontrada", idNotificacion);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Error al eliminar notificación {}: {}", idNotificacion, e.getMessage());
            return false;
        }
    }
}
