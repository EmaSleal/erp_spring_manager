package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConfiguracionHaciendaNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConsecutivoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaSinCredenciales;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.CertificadoHaciendaValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.CertificadoHaciendaValidator.InfoCertificado;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.CertificadoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final CertificadoHaciendaValidator certificadoValidator;

    /**
     * T-041: gestor de transacciones para probar la lectura de una configuración en una transacción propia. Por campo,
     * para no cambiar el constructor; sin él (pruebas unitarias) no se prueba y se asume legible.
     */
    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;
    
    @Override
    @Transactional
    public ConfiguracionHaciendaDTO crear(ConfiguracionHaciendaDTO dto) {
        log.info("Creando configuración de Hacienda para empresa ID: {}", dto.getEmpresaId());
        
        ConfiguracionEmpresa empresa = configuracionEmpresaRepository.findById(dto.getEmpresaId().intValue())
            .orElseThrow(() -> new IllegalArgumentException("ConfiguracionEmpresa no encontrada"));

        // T-035: se abre el keystore con el PIN antes de cambiar nada (sin ruta no hay certificado que validar)
        InfoCertificado certificado = validarCertificado(dto.getRutaCertificado(), dto.getPinCertificado());

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
        
        return conCertificado(mapper.toDTO(configuracion), certificado, empresa);
    }
    
    @Override
    @Transactional
    public ConfiguracionHaciendaDTO actualizar(Long id, ConfiguracionHaciendaDTO dto) {
        log.info("Actualizando configuración ID: {}", id);

        // T-041: con credenciales que no se descifran no se puede cargar la entidad; se vuelven a guardar sin cargarla
        if (!credencialesLegibles(id)) {
            return restablecerCredenciales(id, dto);
        }

        ConfiguracionHacienda configuracion = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        
        // T-035: si cambia la ruta o llega un PIN nuevo, se valida con la ruta y el PIN efectivos antes de modificar
        // nada. El PIN guardado ya viene descifrado por CredencialCifradaConverter.
        boolean pinNuevo = dto.getPinCertificado() != null
            && !ConfiguracionHaciendaMapper.MASK.equals(dto.getPinCertificado());
        boolean rutaCambia = !java.util.Objects.equals(dto.getRutaCertificado(), configuracion.getRutaCertificado());
        InfoCertificado certificado = null;
        if (rutaCambia || pinNuevo) {
            certificado = validarCertificado(dto.getRutaCertificado(),
                pinNuevo ? dto.getPinCertificado() : configuracion.getPinCertificado());
        }

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
        
        return conCertificado(mapper.toDTO(configuracion), certificado, configuracion.getEmpresa());
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
            throw new ConfiguracionHaciendaNoDisponibleException("No hay ninguna configuración de Hacienda activa; "
                    + "active una en Facturación → Configuración de Hacienda.", ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA);
        }
        if (activas.size() > 1) {
            throw new ConfiguracionHaciendaNoDisponibleException("Debe haber exactamente una configuración de Hacienda "
                    + "activa y hay " + activas.size() + " (ids " + idsDe(activas) + ").", ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA);
        }
        return activas.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionHacienda> obtenerPorEmpresaYAmbiente(Integer empresaId, AmbienteHacienda ambiente) {
        return repository.findByEmpresaIdAndAmbiente(empresaId, ambiente);
    }

    /**
     * Sin transacción propia (T-041): si una fila tiene credenciales que no se descifran, la carga lanza y marcaría
     * como rollback-only una transacción que envolviera todo el listado. Cada lectura del repositorio usa la suya; la
     * empresa viene cargada con la configuración (JOIN FETCH), así que mapear no necesita sesión.
     */
    @Override
    public List<ConfiguracionHaciendaDTO> listarPorEmpresa(Integer empresaId) {
        try {
            return repository.findAllByEmpresaId(empresaId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
        } catch (RuntimeException e) {
            log.warn("No se pudo leer el listado de configuraciones de Hacienda de la empresa {} ({}); se lista fila por "
                + "fila con las credenciales ilegibles marcadas", empresaId, causaRaiz(e));
            return repository.findSinCredencialesByEmpresaId(empresaId).stream()
                .map(this::dtoDeFila)
                .collect(Collectors.toList());
        }
    }

    /** Una fila del listado de reserva: completa si se descifra; si no, solo las columnas no cifradas y el aviso. */
    private ConfiguracionHaciendaDTO dtoDeFila(ConfiguracionHaciendaSinCredenciales fila) {
        try {
            return repository.findConEmpresaById(fila.getId()).map(mapper::toDTO)
                .orElseGet(() -> mapper.toDTOIlegible(fila));
        } catch (RuntimeException e) {
            log.warn("Configuración de Hacienda {}: las credenciales guardadas no se descifran con la clave actual "
                + "(HACIENDA_CRYPTO_KEY): {}", fila.getId(), causaRaiz(e));
            return mapper.toDTOIlegible(fila);
        }
    }

    /**
     * T-041: true si la fila se puede cargar (sus cuatro columnas cifradas se descifran). La prueba corre en una
     * transacción propia de solo lectura: si falla, solo se pierde esa y la del llamador sigue sana.
     */
    private boolean credencialesLegibles(Long id) {
        if (transactionManager == null) {
            return true;
        }
        TransactionTemplate prueba = new TransactionTemplate(transactionManager);
        prueba.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        prueba.setReadOnly(true);
        try {
            prueba.executeWithoutResult(st -> repository.findById(id));
            return true;
        } catch (RuntimeException e) {
            log.warn("Configuración de Hacienda {}: las credenciales guardadas no se descifran con la clave actual "
                + "(HACIENDA_CRYPTO_KEY): {}", id, causaRaiz(e));
            return false;
        }
    }

    /**
     * T-041: guarda una configuración con credenciales ilegibles sin cargar la entidad. Exige contraseña y PIN nuevos
     * (no la máscara): los guardados no se pueden recuperar. Valida el certificado con el PIN nuevo, como en una
     * actualización normal, y descarta los tokens guardados.
     */
    private ConfiguracionHaciendaDTO restablecerCredenciales(Long id, ConfiguracionHaciendaDTO dto) {
        ConfiguracionHaciendaSinCredenciales actual = repository.findSinCredencialesById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        if (esMascaraOVacio(dto.getPasswordHacienda()) || esMascaraOVacio(dto.getPinCertificado())) {
            throw new IllegalArgumentException(ConfiguracionHaciendaMapper.AVISO_CREDENCIALES_ILEGIBLES
                + " (contraseña y PIN).");
        }
        validarCertificado(dto.getRutaCertificado(), dto.getPinCertificado());

        repository.restablecerCredenciales(id, dto.getAmbiente(), dto.getUsuarioHacienda(), dto.getPasswordHacienda(),
            dto.getRutaCertificado(), dto.getPinCertificado(),
            fijarConsecutivo(actual.getConsecutivoFactura(), dto.getConsecutivoFactura(), "factura", id),
            fijarConsecutivo(actual.getConsecutivoTiquete(), dto.getConsecutivoTiquete(), "tiquete", id),
            fijarConsecutivo(actual.getConsecutivoNotaCredito(), dto.getConsecutivoNotaCredito(), "nota de crédito", id),
            fijarConsecutivo(actual.getConsecutivoNotaDebito(), dto.getConsecutivoNotaDebito(), "nota de débito", id),
            LocalDateTime.now());
        log.info("Configuración de Hacienda {} actualizada con credenciales nuevas (las anteriores no se descifraban)", id);

        ConfiguracionHacienda guardada = repository.findConEmpresaById(id)
            .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        return conCertificado(mapper.toDTO(guardada), infoMejorEsfuerzo(guardada), guardada.getEmpresa());
    }

    private static boolean esMascaraOVacio(String valor) {
        return valor == null || valor.isBlank() || ConfiguracionHaciendaMapper.MASK.equals(valor);
    }

    private static String causaRaiz(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
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
        return repository.findActivaByEmpresaId(empresaId).map(config -> conCertificado(mapper.toDTO(config),
            infoMejorEsfuerzo(config), config.getEmpresa()));
    }

    /** Valida solo si hay ruta; lanza {@link CertificadoInvalidoException} con mensaje fijo por causa. */
    private InfoCertificado validarCertificado(String ruta, String pin) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
        return certificadoValidator.validar(ruta, pin);
    }

    /** Para la pantalla: vencimiento y titular de la configuración guardada; cualquier fallo deja los campos nulos. */
    private InfoCertificado infoMejorEsfuerzo(ConfiguracionHacienda config) {
        try {
            return validarCertificado(config.getRutaCertificado(), config.getPinCertificado());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Agrega a la respuesta vencimiento, titular y el aviso (no bloqueante) de cédula distinta (ADR-019/021). */
    private static ConfiguracionHaciendaDTO conCertificado(ConfiguracionHaciendaDTO dto, InfoCertificado info,
                                                           ConfiguracionEmpresa empresa) {
        if (info == null) {
            return dto;
        }
        dto.setCertificadoVencimiento(info.fechaVencimiento());
        dto.setCertificadoTitular(info.titular());
        String cedula = empresa == null || empresa.getNumeroIdentificacion() == null ? ""
            : empresa.getNumeroIdentificacion().replaceAll("\\D", "");
        if (!cedula.isEmpty() && !info.identificacion().contains(cedula)) {
            dto.setCertificadoAviso("La identificación del certificado no coincide con la cédula de la empresa ("
                + cedula + "). Verifique que sea el certificado correcto.");
        }
        return dto;
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
