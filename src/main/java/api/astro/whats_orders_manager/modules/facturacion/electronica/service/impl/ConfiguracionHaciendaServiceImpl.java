package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.EmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de configuración de Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionHaciendaServiceImpl implements ConfiguracionHaciendaService {
    
    private final ConfiguracionHaciendaRepository repository;
    private final EmpresaRepository empresaRepository;
    private final ConfiguracionHaciendaMapper mapper;
    private final HaciendaApiService haciendaApiService;
    
    @Override
    @Transactional
    public ConfiguracionHaciendaDTO crear(ConfiguracionHaciendaDTO dto) {
        log.info("Creando configuración de Hacienda para empresa ID: {}", dto.getEmpresaId());
        
        Empresa empresa = empresaRepository.findById(dto.getEmpresaId().intValue())
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
        
        ConfiguracionHacienda configuracion = ConfiguracionHacienda.builder()
            .empresa(empresa)
            .ambiente(dto.getAmbiente())
            .usuarioHacienda(dto.getUsuarioHacienda())
            .passwordHacienda(dto.getPasswordHacienda()) // TODO: Encriptar
            .rutaCertificado(dto.getRutaCertificado())
            .pinCertificado(dto.getPinCertificado()) // TODO: Encriptar
            .activa(false) // Inicia desactivada
            .consecutivoFactura(1L)
            .consecutivoTiquete(1L)
            .consecutivoNotaCredito(1L)
            .consecutivoNotaDebito(1L)
            .build();
        
        configuracion = repository.save(configuracion);
        log.info("Configuración creada con ID: {}", configuracion.getId());
        
        return mapper.toDTO(configuracion);
    }
    
    @Override
    @Transactional
    public ConfiguracionHaciendaDTO actualizar(Long id, ConfiguracionHaciendaDTO dto) {
        log.info("Actualizando configuración ID: {}", id);
        
        ConfiguracionHacienda configuracion = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        configuracion.setAmbiente(dto.getAmbiente());
        configuracion.setUsuarioHacienda(dto.getUsuarioHacienda());
        
        if (!dto.getPasswordHacienda().equals("********")) {
            configuracion.setPasswordHacienda(dto.getPasswordHacienda()); // TODO: Encriptar
        }
        
        configuracion.setRutaCertificado(dto.getRutaCertificado());
        
        if (dto.getPinCertificado() != null && !dto.getPinCertificado().equals("********")) {
            configuracion.setPinCertificado(dto.getPinCertificado()); // TODO: Encriptar
        }
        
        configuracion = repository.save(configuracion);
        log.info("Configuración actualizada: {}", id);
        
        return mapper.toDTO(configuracion);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionHaciendaDTO> obtenerPorId(Long id) {
        return repository.findById(id).map(mapper::toDTO);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionHacienda> obtenerActivaPorEmpresa(Long empresaId) {
        return repository.findActivaByEmpresaId(empresaId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionHacienda> obtenerPorEmpresaYAmbiente(Long empresaId, AmbienteHacienda ambiente) {
        return repository.findByEmpresaIdAndAmbiente(empresaId, ambiente);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionHaciendaDTO> listarPorEmpresa(Long empresaId) {
        return repository.findAllByEmpresaId(empresaId)
            .stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ConfiguracionHaciendaDTO activar(Long id) {
        log.info("Activando configuración ID: {}", id);
        
        ConfiguracionHacienda configuracion = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        // Desactivar otras configuraciones de la misma empresa
        Long empresaId = configuracion.getEmpresa().getIdEmpresa().longValue();
        repository.findAllByEmpresaId(empresaId).forEach(config -> {
            if (!config.getId().equals(id)) {
                config.setActiva(false);
                repository.save(config);
            }
        });
        
        // Activar esta configuración
        configuracion.setActiva(true);
        
        // Intentar autenticar y obtener token
        try {
            HaciendaApiService.TokenResponse token = haciendaApiService.autenticar(id);
            configuracion.setAccessToken(token.accessToken());
            configuracion.setRefreshToken(token.refreshToken());
            configuracion.setTokenExpira(LocalDateTime.now().plusSeconds(token.expiresIn()));
            log.info("Token OAuth2 obtenido exitosamente");
        } catch (Exception e) {
            log.warn("No se pudo obtener token al activar: {}", e.getMessage());
        }
        
        configuracion = repository.save(configuracion);
        log.info("Configuración activada: {}", id);
        
        return mapper.toDTO(configuracion);
    }
    
    @Override
    @Transactional
    public ConfiguracionHaciendaDTO desactivar(Long id) {
        log.info("Desactivando configuración ID: {}", id);
        
        ConfiguracionHacienda configuracion = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        configuracion.setActiva(false);
        repository.save(configuracion);
        
        log.info("Configuración desactivada: {}", id);
        return mapper.toDTO(configuracion);
    }
    
    @Override
    @Transactional
    public boolean renovarToken(Long id) {
        log.info("Renovando token para configuración ID: {}", id);
        
        ConfiguracionHacienda configuracion = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        try {
            HaciendaApiService.TokenResponse token;
            
            if (configuracion.getRefreshToken() != null) {
                token = haciendaApiService.renovarToken(configuracion.getRefreshToken(), id);
            } else {
                token = haciendaApiService.autenticar(id);
            }
            
            configuracion.setAccessToken(token.accessToken());
            configuracion.setRefreshToken(token.refreshToken());
            configuracion.setTokenExpira(LocalDateTime.now().plusSeconds(token.expiresIn()));
            
            repository.save(configuracion);
            log.info("Token renovado exitosamente");
            
            return true;
        } catch (Exception e) {
            log.error("Error renovando token: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean verificarYRenovarToken(Long id) {
        ConfiguracionHacienda configuracion = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        if (!configuracion.isTokenValido()) {
            log.info("Token expirado, renovando automáticamente...");
            return renovarToken(id);
        }
        
        return true;
    }
    
    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando configuración ID: {}", id);
        
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Configuración no encontrada");
        }
        
        repository.deleteById(id);
        log.info("Configuración eliminada: {}", id);
    }

    @Override
    public Optional<ConfiguracionHaciendaDTO> obtenerConfiguracionActiva(Long empresaId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'obtenerConfiguracionActiva'");
    }
}
