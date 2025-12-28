package api.astro.whats_orders_manager.services;


import api.astro.whats_orders_manager.models.MensajeWhatsApp;
import api.astro.whats_orders_manager.models.MensajeWhatsApp.EstadoMensaje;
import api.astro.whats_orders_manager.models.MensajeWhatsApp.TipoMensaje;
import api.astro.whats_orders_manager.models.dto.WhatsAppMensajeDTO;
import api.astro.whats_orders_manager.repositories.MensajeWhatsAppRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de mensajes de WhatsApp
 * Proporciona operaciones CRUD y consultas especializadas
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.3
 */
@Service
@Slf4j
public class MensajeWhatsAppService {
    
    private final MensajeWhatsAppRepository mensajeRepository;
    
    public MensajeWhatsAppService(MensajeWhatsAppRepository mensajeRepository) {
        this.mensajeRepository = mensajeRepository;
    }
    
    /**
     * Obtiene un mensaje por su ID
     * 
     * @param idMensaje ID del mensaje
     * @return Mensaje encontrado
     */
    public Optional<MensajeWhatsApp> obtenerPorId(Long idMensaje) {
        return mensajeRepository.findById(idMensaje);
    }
    
    /**
     * Obtiene un mensaje por su ID como DTO
     * 
     * @param idMensaje ID del mensaje
     * @return DTO del mensaje encontrado
     */
    public WhatsAppMensajeDTO obtenerPorIdDTO(Long idMensaje) {
        return mensajeRepository.findById(idMensaje)
                .map(this::convertirADTO)
                .orElse(null);
    }
    
    /**
     * Obtiene un mensaje por su ID de WhatsApp (wamid.xxx)
     * 
     * @param idMensajeWhatsapp ID del mensaje en WhatsApp
     * @return Mensaje encontrado
     */
    public Optional<MensajeWhatsApp> obtenerPorIdWhatsApp(String idMensajeWhatsapp) {
        return mensajeRepository.findByIdMensajeWhatsapp(idMensajeWhatsapp);
    }
    
    /**
     * Obtiene el historial completo de mensajes de un usuario
     * 
     * @param idUsuario ID del usuario
     * @return Lista de mensajes del usuario
     */
    public List<WhatsAppMensajeDTO> obtenerHistorialUsuario(Integer idUsuario) {
        List<MensajeWhatsApp> mensajes = mensajeRepository.findByIdUsuarioOrderByFechaEnvioDesc(idUsuario);
        return convertirADTOs(mensajes);
    }
    
    /**
     * Obtiene mensajes recientes de un teléfono
     * 
     * @param telefono Número de teléfono
     * @return Lista de últimos 10 mensajes
     */
    /**
     * Obtiene los mensajes recientes de un teléfono
     * Ordenados del más antiguo al más reciente (para vista de conversación)
     * 
     * @param telefono Número de teléfono a buscar
     * @return Lista de mensajes ordenados cronológicamente
     */
    public List<WhatsAppMensajeDTO> obtenerMensajesRecientes(String telefono) {
        List<MensajeWhatsApp> mensajes = mensajeRepository.findByTelefonoOrderByFechaEnvioAsc(telefono);
        return convertirADTOs(mensajes);
    }
    
    /**
     * Obtiene todos los mensajes
     * 
     * @return Lista de todos los mensajes
     */
    public List<WhatsAppMensajeDTO> obtenerTodos() {
        List<MensajeWhatsApp> mensajes = mensajeRepository.findAllByOrderByFechaEnvioDesc();
        return convertirADTOs(mensajes);
    }
    
    /**
     * Obtiene conversaciones agrupadas por teléfono
     * Cada conversación incluye el último mensaje, contador de mensajes y no leídos
     * 
     * @return Lista de conversaciones
     */
    public List<Conversacion> obtenerConversaciones() {
        List<MensajeWhatsApp> todosMensajes = mensajeRepository.findAllByOrderByFechaEnvioDesc();
        
        // Agrupar mensajes por teléfono
        java.util.Map<String, List<MensajeWhatsApp>> mensajesPorTelefono = todosMensajes.stream()
                .collect(Collectors.groupingBy(MensajeWhatsApp::getTelefono));
        
        // Crear conversaciones
        return mensajesPorTelefono.entrySet().stream()
                .map(entry -> {
                    String telefono = entry.getKey();
                    List<MensajeWhatsApp> mensajes = entry.getValue();
                    
                    // Obtener último mensaje
                    MensajeWhatsApp ultimoMensaje = mensajes.stream()
                            .max(java.util.Comparator.comparing(MensajeWhatsApp::getFechaEnvio))
                            .orElse(mensajes.get(0));
                    
                    // Contar no leídos (mensajes recibidos que no están en estado LEIDO)
                    long noLeidos = mensajes.stream()
                            .filter(m -> m.getTipo() == TipoMensaje.RECIBIDO)
                            .filter(m -> m.getEstado() != EstadoMensaje.LEIDO)
                            .count();
                    
                    return new Conversacion(
                            telefono,
                            ultimoMensaje.getNombreUsuario(),
                            ultimoMensaje.getMensaje(),
                            ultimoMensaje.getFechaEnvio(),
                            mensajes.size(),
                            (int) noLeidos,
                            ultimoMensaje.getTipo(),
                            ultimoMensaje.getEstado()
                    );
                })
                .sorted(java.util.Comparator.comparing(Conversacion::getUltimaFecha).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene mensajes por estado
     * 
     * @param estado Estado del mensaje
     * @return Lista de mensajes con ese estado
     */
    public List<WhatsAppMensajeDTO> obtenerPorEstado(EstadoMensaje estado) {
        List<MensajeWhatsApp> mensajes = mensajeRepository.findByEstadoOrderByFechaEnvioDesc(estado);
        return convertirADTOs(mensajes);
    }
    
    /**
     * Obtiene mensajes por tipo
     * 
     * @param tipo Tipo de mensaje (ENVIADO/RECIBIDO)
     * @return Lista de mensajes del tipo especificado
     */
    public List<WhatsAppMensajeDTO> obtenerPorTipo(TipoMensaje tipo) {
        List<MensajeWhatsApp> mensajes = mensajeRepository.findByTipoOrderByFechaEnvioDesc(tipo);
        return convertirADTOs(mensajes);
    }
    
    /**
     * Obtiene mensajes pendientes antiguos
     * Útil para sistema de reintentos
     * 
     * @param minutos Mensajes pendientes desde hace X minutos
     * @return Lista de mensajes pendientes
     */
    public List<MensajeWhatsApp> obtenerMensajesPendientes(int minutos) {
        LocalDateTime fecha = LocalDateTime.now().minusMinutes(minutos);
        return mensajeRepository.findMensajesPendientes(fecha);
    }
    
    /**
     * Obtiene mensajes fallidos recientes
     * 
     * @param horas Mensajes fallidos en las últimas X horas
     * @return Lista de mensajes fallidos
     */
    public List<WhatsAppMensajeDTO> obtenerMensajesFallidos(int horas) {
        LocalDateTime fecha = LocalDateTime.now().minusHours(horas);
        List<MensajeWhatsApp> mensajes = mensajeRepository.findByEstadoAndFechaEnvioAfter(
                EstadoMensaje.FALLIDO, fecha);
        return convertirADTOs(mensajes);
    }
    
    /**
     * Actualiza el estado de un mensaje
     * 
     * @param idMensajeWhatsapp ID del mensaje en WhatsApp
     * @param nuevoEstado Nuevo estado
     * @return Mensaje actualizado
     */
    @Transactional
    public Optional<MensajeWhatsApp> actualizarEstado(String idMensajeWhatsapp, EstadoMensaje nuevoEstado) {
        Optional<MensajeWhatsApp> mensajeOpt = mensajeRepository.findByIdMensajeWhatsapp(idMensajeWhatsapp);
        
        if (mensajeOpt.isPresent()) {
            MensajeWhatsApp mensaje = mensajeOpt.get();
            mensaje.setEstado(nuevoEstado);
            
            // Actualizar fechas según estado
            switch (nuevoEstado) {
                case ENTREGADO:
                    mensaje.setFechaEntrega(LocalDateTime.now());
                    break;
                case LEIDO:
                    mensaje.setFechaLectura(LocalDateTime.now());
                    if (mensaje.getFechaEntrega() == null) {
                        mensaje.setFechaEntrega(LocalDateTime.now());
                    }
                    break;
                default:
                    // No actualizar fechas para otros estados
                    break;
            }
            
            mensajeRepository.save(mensaje);
            log.info("Estado actualizado para mensaje {}: {}", idMensajeWhatsapp, nuevoEstado);
        }
        
        return mensajeOpt;
    }
    
    /**
     * Marca un mensaje como fallido con error
     * 
     * @param idMensajeWhatsapp ID del mensaje en WhatsApp
     * @param mensajeError Descripción del error
     * @return Mensaje actualizado
     */
    @Transactional
    public Optional<MensajeWhatsApp> marcarComoFallido(String idMensajeWhatsapp, String mensajeError) {
        Optional<MensajeWhatsApp> mensajeOpt = mensajeRepository.findByIdMensajeWhatsapp(idMensajeWhatsapp);
        
        if (mensajeOpt.isPresent()) {
            MensajeWhatsApp mensaje = mensajeOpt.get();
            mensaje.setEstado(EstadoMensaje.FALLIDO);
            mensaje.setError(mensajeError);
            mensajeRepository.save(mensaje);
            log.warn("Mensaje {} marcado como fallido: {}", idMensajeWhatsapp, mensajeError);
        }
        
        return mensajeOpt;
    }
    
    /**
     * Obtiene estadísticas de mensajes por estado
     * 
     * @return Map con contadores por estado
     */
    public EstadisticasMensajes obtenerEstadisticas() {
        Long pendientes = mensajeRepository.countByEstado(EstadoMensaje.PENDIENTE);
        Long enviados = mensajeRepository.countByEstado(EstadoMensaje.ENVIADO);
        Long entregados = mensajeRepository.countByEstado(EstadoMensaje.ENTREGADO);
        Long leidos = mensajeRepository.countByEstado(EstadoMensaje.LEIDO);
        Long fallidos = mensajeRepository.countByEstado(EstadoMensaje.FALLIDO);
        
        return new EstadisticasMensajes(pendientes, enviados, entregados, leidos, fallidos);
    }
    
    /**
     * Obtiene estadísticas de mensajes de un usuario
     * 
     * @param idUsuario ID del usuario
     * @return Estadísticas del usuario
     */
    public EstadisticasMensajes obtenerEstadisticasUsuario(Integer idUsuario) {
        Long pendientes = mensajeRepository.countByIdUsuarioAndEstado(idUsuario, EstadoMensaje.PENDIENTE);
        Long enviados = mensajeRepository.countByIdUsuarioAndEstado(idUsuario, EstadoMensaje.ENVIADO);
        Long entregados = mensajeRepository.countByIdUsuarioAndEstado(idUsuario, EstadoMensaje.ENTREGADO);
        Long leidos = mensajeRepository.countByIdUsuarioAndEstado(idUsuario, EstadoMensaje.LEIDO);
        Long fallidos = mensajeRepository.countByIdUsuarioAndEstado(idUsuario, EstadoMensaje.FALLIDO);
        
        return new EstadisticasMensajes(pendientes, enviados, entregados, leidos, fallidos);
    }
    
    /**
     * Verifica si se puede enviar un mensaje (rate limiting)
     * 
     * @param telefono Número de teléfono
     * @param limitePorHora Límite de mensajes por hora
     * @return true si se puede enviar, false si se excede el límite
     */
    public boolean puedeEnviarMensaje(String telefono, int limitePorHora) {
        LocalDateTime hace1Hora = LocalDateTime.now().minusHours(1);
        Long cantidadReciente = mensajeRepository.countMensajesRecientes(telefono, hace1Hora);
        return cantidadReciente < limitePorHora;
    }
    
    /**
     * Elimina mensajes antiguos (limpieza de datos)
     * 
     * @param diasAntiguedad Eliminar mensajes más antiguos que X días
     * @return Cantidad de mensajes eliminados
     */
    @Transactional
    public int eliminarMensajesAntiguos(int diasAntiguedad) {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasAntiguedad);
        List<MensajeWhatsApp> mensajesAntiguos = mensajeRepository.findByEstadoAndFechaEnvioAfter(
                EstadoMensaje.ENTREGADO, fechaLimite);
        
        int cantidad = mensajesAntiguos.size();
        mensajeRepository.deleteAll(mensajesAntiguos);
        log.info("Eliminados {} mensajes antiguos (más de {} días)", cantidad, diasAntiguedad);
        
        return cantidad;
    }
    
    // ========================================
    // MÉTODOS PRIVADOS
    // ========================================
    
    /**
     * Convierte lista de entidades a DTOs
     */
    private List<WhatsAppMensajeDTO> convertirADTOs(List<MensajeWhatsApp> mensajes) {
        return mensajes.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Convierte una entidad a DTO
     */
    private WhatsAppMensajeDTO convertirADTO(MensajeWhatsApp mensaje) {
        return WhatsAppMensajeDTO.builder()
                .idMensaje(mensaje.getIdMensaje())
                .idMensajeWhatsapp(mensaje.getIdMensajeWhatsapp())
                .telefono(mensaje.getTelefono())
                .mensaje(mensaje.getMensaje())
                .tipo(mensaje.getTipo() != null ? mensaje.getTipo().name() : null)
                .estado(mensaje.getEstado() != null ? mensaje.getEstado().name() : null)
                .idUsuario(mensaje.getIdUsuario())
                .nombreUsuario(mensaje.getNombreUsuario())
                .nombrePlantilla(mensaje.getNombrePlantilla())
                .fechaEnvio(mensaje.getFechaEnvio())
                .fechaEntrega(mensaje.getFechaEntrega())
                .fechaLectura(mensaje.getFechaLectura())
                .error(mensaje.getError())
                .metadata(mensaje.getMetadata())
                .build();
    }
    
    // ========================================
    // CLASE INTERNA - ESTADÍSTICAS
    // ========================================
    
    /**
     * Clase para agrupar estadísticas de mensajes
     */
    public static class EstadisticasMensajes {
        private final Long pendientes;
        private final Long enviados;
        private final Long entregados;
        private final Long leidos;
        private final Long fallidos;
        
        public EstadisticasMensajes(Long pendientes, Long enviados, Long entregados, Long leidos, Long fallidos) {
            this.pendientes = pendientes;
            this.enviados = enviados;
            this.entregados = entregados;
            this.leidos = leidos;
            this.fallidos = fallidos;
        }
        
        public Long getPendientes() {
            return pendientes;
        }
        
        public Long getEnviados() {
            return enviados;
        }
        
        public Long getEntregados() {
            return entregados;
        }
        
        public Long getLeidos() {
            return leidos;
        }
        
        public Long getFallidos() {
            return fallidos;
        }
        
        public Long getTotal() {
            return pendientes + enviados + entregados + leidos + fallidos;
        }
        
        public double getTasaExito() {
            long total = getTotal();
            if (total == 0) return 0.0;
            return ((double) (enviados + entregados + leidos) / total) * 100.0;
        }
    }
    
    // ========================================
    // CLASE INTERNA - CONVERSACIÓN
    // ========================================
    
    /**
     * Clase para representar una conversación agrupada por teléfono
     */
    public static class Conversacion {
        private final String telefono;
        private final String nombreUsuario;
        private final String ultimoMensaje;
        private final LocalDateTime ultimaFecha;
        private final int totalMensajes;
        private final int noLeidos;
        private final TipoMensaje ultimoTipo;
        private final EstadoMensaje ultimoEstado;
        
        public Conversacion(String telefono, String nombreUsuario, String ultimoMensaje, 
                          LocalDateTime ultimaFecha, int totalMensajes, int noLeidos,
                          TipoMensaje ultimoTipo, EstadoMensaje ultimoEstado) {
            this.telefono = telefono;
            this.nombreUsuario = nombreUsuario;
            this.ultimoMensaje = ultimoMensaje;
            this.ultimaFecha = ultimaFecha;
            this.totalMensajes = totalMensajes;
            this.noLeidos = noLeidos;
            this.ultimoTipo = ultimoTipo;
            this.ultimoEstado = ultimoEstado;
        }
        
        public String getTelefono() {
            return telefono;
        }
        
        public String getNombreUsuario() {
            return nombreUsuario != null ? nombreUsuario : telefono;
        }
        
        public String getUltimoMensaje() {
            return ultimoMensaje;
        }
        
        public LocalDateTime getUltimaFecha() {
            return ultimaFecha;
        }
        
        public int getTotalMensajes() {
            return totalMensajes;
        }
        
        public int getNoLeidos() {
            return noLeidos;
        }
        
        public TipoMensaje getUltimoTipo() {
            return ultimoTipo;
        }
        
        public EstadoMensaje getUltimoEstado() {
            return ultimoEstado;
        }
        
        public boolean tieneNoLeidos() {
            return noLeidos > 0;
        }
    }
}
