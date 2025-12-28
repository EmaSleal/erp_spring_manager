package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.ConfiguracionEmail;

import java.util.Optional;

/**
 * Servicio para gestionar la configuración de email/SMTP
 */
public interface ConfiguracionEmailService {
    
    /**
     * Obtiene la configuración de email (única en el sistema)
     * @return Optional con la configuración si existe
     */
    Optional<ConfiguracionEmail> obtenerConfiguracion();
    
    /**
     * Obtiene la configuración de email activa
     * @return Optional con la configuración activa
     */
    Optional<ConfiguracionEmail> obtenerConfiguracionActiva();
    
    /**
     * Obtiene la configuración de email o crea una nueva si no existe
     * @return ConfiguracionEmail existente o nueva
     */
    ConfiguracionEmail obtenerOCrearConfiguracion();
    
    /**
     * Guarda o actualiza la configuración de email
     * @param configuracion Configuración a guardar
     * @return Configuración guardada
     */
    ConfiguracionEmail guardarConfiguracion(ConfiguracionEmail configuracion);
    
    /**
     * Actualiza parcialmente la configuración de email
     * @param configuracion Configuración con los campos a actualizar
     * @return Configuración actualizada
     */
    ConfiguracionEmail actualizarConfiguracion(ConfiguracionEmail configuracion);
    
    /**
     * Prueba la configuración de email enviando un correo de prueba
     * @param emailDestino Email destino para la prueba
     * @return true si el envío fue exitoso
     */
    boolean probarConfiguracion(String emailDestino);
    
    /**
     * Valida que la configuración SMTP esté completa
     * @return true si la configuración está completa
     */
    boolean validarConfiguracion();
    
    /**
     * Activa o desactiva la configuración de email
     * @param activo true para activar, false para desactivar
     * @return Configuración actualizada
     */
    ConfiguracionEmail cambiarEstado(boolean activo);
}
