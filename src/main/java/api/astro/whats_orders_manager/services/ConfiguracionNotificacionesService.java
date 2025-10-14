package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.ConfiguracionNotificaciones;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * ============================================================================
 * CONFIGURACIÓN NOTIFICACIONES SERVICE
 * WhatsApp Orders Manager
 * ============================================================================
 * Servicio para gestionar la configuración de notificaciones del sistema.
 * ============================================================================
 */
@Service
public interface ConfiguracionNotificacionesService {

    /**
     * Obtiene la configuración de notificaciones activa
     * 
     * @return Optional con la configuración activa
     */
    Optional<ConfiguracionNotificaciones> getConfiguracionActiva();

    /**
     * Obtiene la configuración activa, o crea una nueva si no existe
     * 
     * @return ConfiguracionNotificaciones activa
     */
    ConfiguracionNotificaciones getOrCreateConfiguracion();

    /**
     * Guarda o actualiza la configuración de notificaciones
     * 
     * @param configuracion Configuración a guardar
     * @return Configuración guardada
     */
    ConfiguracionNotificaciones save(ConfiguracionNotificaciones configuracion);

    /**
     * Actualiza la configuración existente
     * Si hay otra configuración activa, la desactiva primero
     * 
     * @param configuracion Configuración con los nuevos valores
     * @return Configuración actualizada
     */
    ConfiguracionNotificaciones update(ConfiguracionNotificaciones configuracion);

    /**
     * Verifica si las notificaciones por email están habilitadas
     * 
     * @return true si están habilitadas
     */
    boolean notificacionesHabilitadas();

    /**
     * Verifica si se debe enviar la factura automáticamente al crearla
     * 
     * @return true si se debe enviar automáticamente
     */
    boolean debeEnviarFacturaAutomatica();

    /**
     * Verifica si se deben enviar recordatorios de pago
     * 
     * @return true si se deben enviar recordatorios
     */
    boolean debeEnviarRecordatorios();

    /**
     * Obtiene los días de recordatorio de pago configurados
     * 
     * @return días después del vencimiento para enviar recordatorio
     */
    int getDiasRecordatorioPago();

    /**
     * Obtiene los días de recordatorio preventivo configurados
     * 
     * @return días antes del vencimiento para enviar recordatorio
     */
    int getDiasRecordatorioPreventivo();

    /**
     * Obtiene la frecuencia de recordatorios configurada
     * 
     * @return cada cuántos días enviar recordatorios
     */
    int getFrecuenciaRecordatorios();
}
