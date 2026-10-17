package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;

import java.util.Optional;

/**
 * Servicio para gestionar la configuración de email/SMTP
 */
public interface ConfiguracionEmailService {
    
    /**
     * Único método de lectura de la configuración de email (fila única, ADR-027). Lo usan el envío de correos,
     * la prueba, la validación, la edición y la pantalla. "activo" indica si el envío está habilitado.
     * @return Optional con la configuración si existe
     */
    Optional<ConfiguracionEmail> obtenerConfiguracion();
    
    /**
     * Obtiene la configuración de email o, solo si la tabla está vacía, crea la única fila (inactiva).
     * Nunca crea una segunda fila.
     * @return ConfiguracionEmail existente o nueva
     */
    ConfiguracionEmail obtenerOCrearConfiguracion();
    
    /**
     * Guarda la configuración de email. Si la contraseña llega vacía o con la máscara "********", se conserva
     * la guardada.
     * @param configuracion Configuración a guardar
     * @return Configuración guardada
     * @throws IllegalStateException si es una fila nueva y ya existe una configuración (ADR-027)
     */
    ConfiguracionEmail guardarConfiguracion(ConfiguracionEmail configuracion);
    
    /**
     * Actualiza parcialmente la configuración de email (campos no nulos). Si la contraseña llega vacía o con la
     * máscara "********", se conserva la guardada.
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

    /**
     * Creates or updates the email configuration.
     * Delegates to actualizarConfiguracion, which calls obtenerOCrearConfiguracion
     * internally to insert a default row if none exists, then applies non-null fields.
     *
     * @param configuracion configuration to save or update
     * @return persisted configuration
     */
    ConfiguracionEmail saveOrUpdate(ConfiguracionEmail configuracion);
}
