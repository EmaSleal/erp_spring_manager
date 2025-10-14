package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.ConfiguracionNotificaciones;
import api.astro.whats_orders_manager.repositories.ConfiguracionNotificacionesRepository;
import api.astro.whats_orders_manager.services.ConfiguracionNotificacionesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * ============================================================================
 * CONFIGURACIÓN NOTIFICACIONES SERVICE IMPLEMENTATION
 * WhatsApp Orders Manager
 * ============================================================================
 * Implementación del servicio de configuración de notificaciones.
 * 
 * Responsabilidades:
 * - Gestionar la configuración única de notificaciones
 * - Proveer valores por defecto si no existe configuración
 * - Validar que solo exista una configuración activa
 * ============================================================================
 */
@Slf4j
@Service
public class ConfiguracionNotificacionesServiceImpl implements ConfiguracionNotificacionesService {

    @Autowired
    private ConfiguracionNotificacionesRepository repository;

    @Override
    public Optional<ConfiguracionNotificaciones> getConfiguracionActiva() {
        log.debug("Buscando configuración de notificaciones activa");
        return repository.findConfiguracionActiva();
    }

    @Override
    @Transactional
    public ConfiguracionNotificaciones getOrCreateConfiguracion() {
        log.debug("Obteniendo o creando configuración de notificaciones");
        
        Optional<ConfiguracionNotificaciones> configuracionOpt = repository.findConfiguracionActiva();
        
        if (configuracionOpt.isPresent()) {
            log.debug("Configuración existente encontrada: {}", configuracionOpt.get());
            return configuracionOpt.get();
        }
        
        // No existe configuración, crear una con valores por defecto
        log.info("No existe configuración de notificaciones, creando una nueva con valores por defecto");
        ConfiguracionNotificaciones nuevaConfig = ConfiguracionNotificaciones.conValoresPorDefecto();
        // createBy se dejará NULL ya que es creado por el sistema, no por un usuario específico
        
        ConfiguracionNotificaciones guardada = repository.save(nuevaConfig);
        log.info("✅ Configuración de notificaciones creada exitosamente: {}", guardada);
        
        return guardada;
    }

    @Override
    @Transactional
    public ConfiguracionNotificaciones save(ConfiguracionNotificaciones configuracion) {
        log.info("Guardando configuración de notificaciones: {}", configuracion);
        
        // Si esta configuración se marca como activa, desactivar las demás
        if (Boolean.TRUE.equals(configuracion.getActivo())) {
            desactivarConfiguracionesExistentes(configuracion.getIdConfiguracion());
        }
        
        ConfiguracionNotificaciones guardada = repository.save(configuracion);
        log.info("✅ Configuración guardada exitosamente");
        
        return guardada;
    }

    @Override
    @Transactional
    public ConfiguracionNotificaciones update(ConfiguracionNotificaciones configuracion) {
        log.info("Actualizando configuración de notificaciones ID: {}", configuracion.getIdConfiguracion());
        
        if (configuracion.getIdConfiguracion() == null) {
            throw new IllegalArgumentException("No se puede actualizar una configuración sin ID");
        }
        
        // Verificar que existe
        ConfiguracionNotificaciones existente = repository.findById(configuracion.getIdConfiguracion())
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró la configuración con ID: " + configuracion.getIdConfiguracion()
            ));
        
        // Si se marca como activa, desactivar las demás
        if (Boolean.TRUE.equals(configuracion.getActivo())) {
            desactivarConfiguracionesExistentes(configuracion.getIdConfiguracion());
        }
        
        // Actualizar campos
        existente.setActivarEmail(configuracion.getActivarEmail());
        existente.setEnviarFacturaAutomatica(configuracion.getEnviarFacturaAutomatica());
        existente.setDiasRecordatorioPreventivo(configuracion.getDiasRecordatorioPreventivo());
        existente.setDiasRecordatorioPago(configuracion.getDiasRecordatorioPago());
        existente.setFrecuenciaRecordatorios(configuracion.getFrecuenciaRecordatorios());
        existente.setNotificarNuevoCliente(configuracion.getNotificarNuevoCliente());
        existente.setNotificarNuevoUsuario(configuracion.getNotificarNuevoUsuario());
        existente.setEmailAdmin(configuracion.getEmailAdmin());
        existente.setEmailCopiaFacturas(configuracion.getEmailCopiaFacturas());
        existente.setActivo(configuracion.getActivo());
        
        ConfiguracionNotificaciones actualizada = repository.save(existente);
        log.info("✅ Configuración actualizada exitosamente");
        
        return actualizada;
    }

    @Override
    public boolean notificacionesHabilitadas() {
        ConfiguracionNotificaciones config = getOrCreateConfiguracion();
        return config.notificacionesHabilitadas();
    }

    @Override
    public boolean debeEnviarFacturaAutomatica() {
        ConfiguracionNotificaciones config = getOrCreateConfiguracion();
        return config.debeEnviarFacturaAutomatica();
    }

    @Override
    public boolean debeEnviarRecordatorios() {
        ConfiguracionNotificaciones config = getOrCreateConfiguracion();
        return config.debeEnviarRecordatorios();
    }

    @Override
    public int getDiasRecordatorioPago() {
        ConfiguracionNotificaciones config = getOrCreateConfiguracion();
        return config.getDiasRecordatorioPago() != null ? config.getDiasRecordatorioPago() : 0;
    }

    @Override
    public int getDiasRecordatorioPreventivo() {
        ConfiguracionNotificaciones config = getOrCreateConfiguracion();
        return config.getDiasRecordatorioPreventivo() != null ? config.getDiasRecordatorioPreventivo() : 3;
    }

    @Override
    public int getFrecuenciaRecordatorios() {
        ConfiguracionNotificaciones config = getOrCreateConfiguracion();
        return config.getFrecuenciaRecordatorios() != null ? config.getFrecuenciaRecordatorios() : 7;
    }

    /**
     * Desactiva todas las configuraciones excepto la especificada
     * 
     * @param idExcluir ID de la configuración a excluir (puede ser null)
     */
    @Transactional
    private void desactivarConfiguracionesExistentes(Integer idExcluir) {
        log.debug("Desactivando configuraciones existentes (excepto ID: {})", idExcluir);
        
        repository.findAll().forEach(config -> {
            if (!config.getIdConfiguracion().equals(idExcluir) && Boolean.TRUE.equals(config.getActivo())) {
                config.setActivo(false);
                repository.save(config);
                log.debug("Configuración ID {} desactivada", config.getIdConfiguracion());
            }
        });
    }
}
