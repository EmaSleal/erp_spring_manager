package api.astro.whats_orders_manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import api.astro.whats_orders_manager.models.MensajeWhatsApp;
import api.astro.whats_orders_manager.models.MensajeWhatsApp.EstadoMensaje;
import api.astro.whats_orders_manager.models.MensajeWhatsApp.TipoMensaje;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para la entidad MensajeWhatsApp.
 * Proporciona métodos para gestionar mensajes de WhatsApp en la base de datos.
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - 26 octubre 2025
 */
@Repository
public interface MensajeWhatsAppRepository extends JpaRepository<MensajeWhatsApp, Long> {
    
    /**
     * Busca un mensaje por su ID de WhatsApp (wamid.xxx)
     * Útil para actualizar estados mediante webhooks
     * 
     * @param idMensajeWhatsapp ID del mensaje en WhatsApp
     * @return Optional con el mensaje si existe
     */
    Optional<MensajeWhatsApp> findByIdMensajeWhatsapp(String idMensajeWhatsapp);
    
    /**
     * Busca mensajes por teléfono ordenados por fecha descendente
     * 
     * @param telefono Número de teléfono en formato +525512345678
     * @return Lista de mensajes del teléfono
     */
    List<MensajeWhatsApp> findByTelefonoOrderByFechaEnvioDesc(String telefono);
    
    /**
     * Busca mensajes relacionados con una factura
     * 
     * @param idFactura ID de la factura
     * @return Lista de mensajes de la factura
     */
    List<MensajeWhatsApp> findByIdFacturaOrderByFechaEnvioDesc(Long idFactura);
    
    /**
     * Busca mensajes por estado
     * 
     * @param estado Estado del mensaje
     * @return Lista de mensajes con ese estado
     */
    List<MensajeWhatsApp> findByEstadoOrderByFechaEnvioDesc(EstadoMensaje estado);
    
    /**
     * Busca mensajes por tipo y estado
     * Útil para obtener mensajes enviados pendientes, por ejemplo
     * 
     * @param tipo Tipo de mensaje (ENVIADO/RECIBIDO)
     * @param estado Estado del mensaje
     * @return Lista de mensajes que cumplen los criterios
     */
    List<MensajeWhatsApp> findByTipoAndEstado(TipoMensaje tipo, EstadoMensaje estado);
    
    /**
     * Busca mensajes pendientes que llevan mucho tiempo sin procesar
     * Útil para sistema de reintentos
     * 
     * @param fecha Fecha límite (ej: hace 5 minutos)
     * @return Lista de mensajes pendientes antiguos
     */
    @Query("SELECT m FROM MensajeWhatsApp m WHERE m.estado = 'PENDIENTE' AND m.fechaEnvio < :fecha")
    List<MensajeWhatsApp> findMensajesPendientes(@Param("fecha") LocalDateTime fecha);
    
    /**
     * Busca mensajes fallidos recientes para revisión
     * 
     * @param estado Estado del mensaje
     * @param fecha Fecha desde la cual buscar
     * @return Lista de mensajes fallidos desde esa fecha
     */
    List<MensajeWhatsApp> findByEstadoAndFechaEnvioAfter(EstadoMensaje estado, LocalDateTime fecha);
    
    /**
     * Cuenta mensajes recientes de un teléfono
     * Útil para rate limiting (no enviar más de X mensajes por hora)
     * 
     * @param telefono Número de teléfono
     * @param fecha Fecha desde la cual contar (ej: última hora)
     * @return Cantidad de mensajes enviados desde esa fecha
     */
    @Query("SELECT COUNT(m) FROM MensajeWhatsApp m WHERE m.telefono = :telefono AND m.fechaEnvio > :fecha")
    Long countMensajesRecientes(@Param("telefono") String telefono, @Param("fecha") LocalDateTime fecha);
    
    /**
     * Obtiene los últimos 10 mensajes de un teléfono
     * Útil para mostrar historial de conversación
     * 
     * @param telefono Número de teléfono
     * @return Lista de los últimos 10 mensajes
     */
    List<MensajeWhatsApp> findTop10ByTelefonoOrderByFechaEnvioDesc(String telefono);
    
    /**
     * Busca mensajes por tipo (ENVIADO o RECIBIDO)
     * 
     * @param tipo Tipo de mensaje
     * @return Lista de mensajes del tipo especificado
     */
    List<MensajeWhatsApp> findByTipoOrderByFechaEnvioDesc(TipoMensaje tipo);
    
    /**
     * Cuenta mensajes por estado
     * Útil para dashboard/estadísticas
     * 
     * @param estado Estado del mensaje
     * @return Cantidad de mensajes en ese estado
     */
    Long countByEstado(EstadoMensaje estado);
    
    /**
     * Busca mensajes de una factura específica
     * 
     * @param idFactura ID de la factura
     * @return Lista de mensajes relacionados
     */
    List<MensajeWhatsApp> findByIdFactura(Long idFactura);
}
