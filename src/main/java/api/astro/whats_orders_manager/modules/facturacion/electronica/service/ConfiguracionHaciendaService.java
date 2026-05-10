package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestión de configuraciones de Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
public interface ConfiguracionHaciendaService {
    
    /**
     * Crea una nueva configuración de Hacienda.
     */
    ConfiguracionHaciendaDTO crear(ConfiguracionHaciendaDTO dto);
    
    /**
     * Actualiza una configuración existente.
     */
    ConfiguracionHaciendaDTO actualizar(Long id, ConfiguracionHaciendaDTO dto);
    
    /**
     * Obtiene configuración por ID.
     */
    Optional<ConfiguracionHaciendaDTO> obtenerPorId(Long id);
    
    /**
     * Obtiene la primera configuración activa disponible (sin filtrar por empresa).
     */
    Optional<ConfiguracionHacienda> obtenerConfiguracionActivaGlobal();

    /**
     * Obtiene la configuración activa de una empresa.
     */
    Optional<ConfiguracionHacienda> obtenerActivaPorEmpresa(Integer empresaId);
    
    /**
     * Obtiene la configuración activa de una empresa como DTO.
     */
    Optional<ConfiguracionHaciendaDTO> obtenerConfiguracionActiva(Integer empresaId);
    
    /**
     * Obtiene configuración por empresa y ambiente.
     */
    Optional<ConfiguracionHacienda> obtenerPorEmpresaYAmbiente(Integer empresaId, AmbienteHacienda ambiente);
    
    /**
     * Lista todas las configuraciones de una empresa.
     */
    List<ConfiguracionHaciendaDTO> listarPorEmpresa(Integer empresaId);
    
    /**
     * Activa una configuración (desactiva las demás de la empresa).
     */
    ConfiguracionHaciendaDTO activar(Long id);
    
    /**
     * Desactiva una configuración.
     */
    ConfiguracionHaciendaDTO desactivar(Long id);
    
    /**
     * Renueva el token OAuth2.
     */
    boolean renovarToken(Long id);
    
    /**
     * Verifica si el token está vigente, si no lo renueva automáticamente.
     */
    boolean verificarYRenovarToken(Long id);
    
    /**
     * Elimina una configuración.
     */
    void eliminar(Long id);
}
