package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConsecutivoInvalidoException;
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
    private final ConfiguracionEmpresaRepository configuracionEmpresaRepository;
    private final ConfiguracionHaciendaMapper mapper;
    private final HaciendaApiService haciendaApiService;
    
    @Override
    @Transactional
    public ConfiguracionHaciendaDTO crear(ConfiguracionHaciendaDTO dto) {
        log.info("Creando configuración de Hacienda para empresa ID: {}", dto.getEmpresaId());
        
        ConfiguracionEmpresa empresa = configuracionEmpresaRepository.findById(dto.getEmpresaId().intValue())
            .orElseThrow(() -> new IllegalArgumentException("ConfiguracionEmpresa no encontrada"));

        // ADR-021: una configuración nueva marcada como activa desactiva las demás en la misma transacción
        boolean activa = Boolean.TRUE.equals(dto.getActiva());
        if (activa) {
            desactivarOtras(null);
        }
        
        ConfiguracionHacienda configuracion = ConfiguracionHacienda.builder()
            .empresa(empresa)
            .ambiente(dto.getAmbiente())
            .usuarioHacienda(dto.getUsuarioHacienda())
            .passwordHacienda(dto.getPasswordHacienda()) // cifrado en reposo por CredencialCifradaConverter
            .rutaCertificado(dto.getRutaCertificado())
            .pinCertificado(dto.getPinCertificado())
            .activa(activa)
            .consecutivoFactura(consecutivoValido(dto.getConsecutivoFactura(), 1L, "factura"))
            .consecutivoTiquete(consecutivoValido(dto.getConsecutivoTiquete(), 1L, "tiquete"))
            .consecutivoNotaCredito(consecutivoValido(dto.getConsecutivoNotaCredito(), 1L, "nota de crédito"))
            .consecutivoNotaDebito(consecutivoValido(dto.getConsecutivoNotaDebito(), 1L, "nota de débito"))
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
        
        // "********" (la máscara que devuelve el mapper) conserva la credencial guardada
        if (!ConfiguracionHaciendaMapper.MASK.equals(dto.getPasswordHacienda())) {
            configuracion.setPasswordHacienda(dto.getPasswordHacienda());
        }
        
        configuracion.setRutaCertificado(dto.getRutaCertificado());
        
        if (dto.getPinCertificado() != null && !ConfiguracionHaciendaMapper.MASK.equals(dto.getPinCertificado())) {
            configuracion.setPinCertificado(dto.getPinCertificado());
        }

        // T-033: el siguiente consecutivo de cada tipo se puede fijar (p. ej. por encima del mayor ya usado en el
        // ambiente, rechazo -99 de Hacienda). Sin valor, se conserva.
        configuracion.setConsecutivoFactura(
            fijarConsecutivo(configuracion.getConsecutivoFactura(), dto.getConsecutivoFactura(), "factura", id));
        configuracion.setConsecutivoTiquete(
            fijarConsecutivo(configuracion.getConsecutivoTiquete(), dto.getConsecutivoTiquete(), "tiquete", id));
        configuracion.setConsecutivoNotaCredito(fijarConsecutivo(configuracion.getConsecutivoNotaCredito(),
            dto.getConsecutivoNotaCredito(), "nota de crédito", id));
        configuracion.setConsecutivoNotaDebito(fijarConsecutivo(configuracion.getConsecutivoNotaDebito(),
            dto.getConsecutivoNotaDebito(), "nota de débito", id));
        
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
    public ConfiguracionHacienda obtenerConfiguracionActiva() {
        List<ConfiguracionHacienda> activas = repository.findByActivaTrueOrderByIdAsc();
        if (activas.isEmpty()) {
            throw new IllegalStateException("No hay ninguna configuración de Hacienda activa; active una en "
                    + "Facturación → Configuración de Hacienda.");
        }
        if (activas.size() > 1) {
            throw new IllegalStateException("Debe haber exactamente una configuración de Hacienda activa y hay "
                    + activas.size() + " (ids " + idsDe(activas) + ").");
        }
        return activas.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionHacienda> obtenerPorEmpresaYAmbiente(Integer empresaId, AmbienteHacienda ambiente) {
        return repository.findByEmpresaIdAndAmbiente(empresaId, ambiente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionHaciendaDTO> listarPorEmpresa(Integer empresaId) {
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
        
        // ADR-021: exactamente una activa; se desactivan las demás en la misma transacción
        desactivarOtras(id);
        
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
    public Optional<ConfiguracionHaciendaDTO> obtenerConfiguracionActiva(Integer empresaId) {
        return repository.findActivaByEmpresaId(empresaId).map(mapper::toDTO);
    }

    /**
     * Desactiva todas las configuraciones activas salvo la indicada (null: todas).
     */
    private void desactivarOtras(Long idQueQuedaActiva) {
        repository.findByActivaTrueOrderByIdAsc().forEach(config -> {
            if (!config.getId().equals(idQueQuedaActiva)) {
                config.setActiva(false);
                repository.save(config);
                log.info("Configuración de Hacienda {} desactivada", config.getId());
            }
        });
    }

    private static String idsDe(List<ConfiguracionHacienda> configuraciones) {
        return configuraciones.stream().map(c -> String.valueOf(c.getId())).collect(Collectors.joining(", "));
    }

    /** Mayor consecutivo que cabe en la clave: 10 dígitos (ClaveNumericaGenerator). */
    static final long CONSECUTIVO_MAXIMO = 9_999_999_999L;

    /**
     * Siguiente consecutivo pedido para un tipo de comprobante, o el valor por defecto si no se indica.
     *
     * @throws ConsecutivoInvalidoException si no está entre 1 y 9 999 999 999
     */
    static Long consecutivoValido(Long pedido, Long porDefecto, String tipo) {
        if (pedido == null) {
            return porDefecto;
        }
        if (pedido < 1 || pedido > CONSECUTIVO_MAXIMO) {
            throw new ConsecutivoInvalidoException("El consecutivo de " + tipo + " debe estar entre 1 y " + CONSECUTIVO_MAXIMO);
        }
        return pedido;
    }

    /** Fija el siguiente consecutivo; bajarlo se permite (cambio de ambiente) pero queda en el log. */
    private static Long fijarConsecutivo(Long actual, Long pedido, String tipo, Long idConfiguracion) {
        Long nuevo = consecutivoValido(pedido, actual, tipo);
        if (actual != null && nuevo < actual) {
            log.warn("Configuración {}: el consecutivo de {} baja de {} a {}; los números ya usados en el ambiente "
                + "serán rechazados por Hacienda", idConfiguracion, tipo, actual, nuevo);
        } else if (!nuevo.equals(actual)) {
            log.info("Configuración {}: consecutivo de {} fijado en {} (era {})", idConfiguracion, tipo, nuevo, actual);
        }
        return nuevo;
    }
}
