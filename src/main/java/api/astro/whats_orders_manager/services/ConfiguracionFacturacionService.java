package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.ConfiguracionFacturacion;

import java.util.Optional;

/**
 * Service interface para gestionar la configuración de facturación del sistema.
 * 
 * Esta configuración controla aspectos clave como:
 * - Numeración automática de facturas (serie y secuencia)
 * - Configuración de impuestos (IGV/IVA)
 * - Formato de números y moneda
 * - Términos y condiciones para facturas
 * 
 * Solo puede existir una configuración activa a la vez (patrón singleton).
 * 
 * @author ASTRO
 * @version 1.0
 * @since Sprint 2 - Fase 2
 */
public interface ConfiguracionFacturacionService {
    
    /**
     * Busca una configuración de facturación por su ID.
     * 
     * @param id ID de la configuración
     * @return Optional con la configuración si existe
     */
    Optional<ConfiguracionFacturacion> findById(Integer id);
    
    /**
     * Obtiene la configuración de facturación activa del sistema.
     * Solo debe existir una configuración activa.
     * 
     * @return Optional con la configuración activa, vacío si no existe
     */
    Optional<ConfiguracionFacturacion> getConfiguracionActiva();
    
    /**
     * Obtiene la configuración activa o crea una nueva con valores por defecto
     * si no existe ninguna configuración.
     * 
     * Este método garantiza que siempre haya una configuración disponible
     * para el sistema de facturación.
     * 
     * @return Configuración activa existente o nueva con valores por defecto
     */
    ConfiguracionFacturacion getOrCreateConfiguracion();
    
    /**
     * Guarda una nueva configuración de facturación.
     * Valida que no exista otra configuración activa antes de guardar.
     * 
     * @param configuracion Configuración a guardar
     * @return Configuración guardada con ID asignado
     * @throws IllegalStateException si ya existe una configuración activa
     */
    ConfiguracionFacturacion save(ConfiguracionFacturacion configuracion);
    
    /**
     * Actualiza una configuración de facturación existente.
     * Valida que la configuración exista y mantiene la integridad
     * de los datos (ej: número actual no menor que número inicial).
     * 
     * @param configuracion Configuración con datos actualizados
     * @return Configuración actualizada
     * @throws IllegalArgumentException si la configuración no existe
     * @throws IllegalStateException si los datos son inválidos
     */
    ConfiguracionFacturacion update(ConfiguracionFacturacion configuracion);
    
    /**
     * Genera el siguiente número de factura basado en la configuración activa.
     * Formatea el número según el template configurado.
     * 
     * Ejemplo: Si serie="F001", numeroActual=5, formatoNumero="{serie}-{numero}"
     *          Resultado: "F001-00005"
     * 
     * @return Número de factura formateado
     * @throws IllegalStateException si no existe configuración activa
     */
    String generarSiguienteNumeroFactura();
    
    /**
     * Incrementa el contador de números de factura después de crear una factura.
     * Este método debe llamarse DESPUÉS de guardar exitosamente una factura
     * para evitar saltos en la numeración.
     * 
     * @return Configuración actualizada con nuevo número actual
     * @throws IllegalStateException si no existe configuración activa
     */
    ConfiguracionFacturacion incrementarNumeroFactura();
    
    /**
     * Verifica si existe una configuración de facturación activa.
     * 
     * @return true si existe al menos una configuración activa
     */
    boolean existeConfiguracionActiva();
    
    /**
     * Valida los datos de una configuración de facturación.
     * Verifica reglas de negocio como:
     * - Serie no puede estar vacía
     * - Número actual >= número inicial
     * - IGV debe estar entre 0 y 100
     * - Decimales entre 0 y 4
     * 
     * @param configuracion Configuración a validar
     * @throws IllegalArgumentException si alguna validación falla
     */
    void validarConfiguracion(ConfiguracionFacturacion configuracion);
    
    /**
     * Busca una configuración por su serie de facturación.
     * Útil para validar que no existan series duplicadas.
     * 
     * @param serie Serie a buscar (ej: "F001", "B001")
     * @return Optional con la configuración si existe
     */
    Optional<ConfiguracionFacturacion> findBySerieFactura(String serie);
}
