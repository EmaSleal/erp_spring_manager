package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;

import java.util.Optional;

/**
 * Servicio para gestionar la configuración de la empresa
 */
public interface ConfiguracionEmpresaService {
    
    /**
     * Obtiene la configuración de la empresa (única en el sistema)
     * @return Optional con la configuración si existe
     */
    Optional<ConfiguracionEmpresa> obtenerConfiguracion();
    
    /**
     * Obtiene la configuración de la empresa o crea una nueva si no existe
     * @return ConfiguracionEmpresa existente o nueva
     */
    ConfiguracionEmpresa obtenerOCrearConfiguracion();
    
    /**
     * Guarda o actualiza la configuración de la empresa
     * @param configuracion Configuración a guardar
     * @return Configuración guardada
     */
    ConfiguracionEmpresa guardarConfiguracion(ConfiguracionEmpresa configuracion);
    
    /**
     * Actualiza parcialmente la configuración de la empresa
     * @param configuracion Configuración con los campos a actualizar
     * @return Configuración actualizada
     */
    ConfiguracionEmpresa actualizarConfiguracion(ConfiguracionEmpresa configuracion);
    
    /**
     * Verifica si existe configuración de empresa
     * @return true si existe
     */
    boolean existeConfiguracion();
    
    /**
     * Valida que los datos fiscales de la empresa estén completos
     * @return true si los datos fiscales están completos
     */
    boolean validarDatosFiscales();

    /**
     * Creates or updates the empresa configuration.
     * Delegates to actualizarConfiguracion, which calls obtenerOCrearConfiguracion
     * internally to insert a default row if none exists, then applies non-null fields.
     *
     * @param configuracion configuration to save or update
     * @return persisted configuration
     */
    ConfiguracionEmpresa saveOrUpdate(ConfiguracionEmpresa configuracion);
}
