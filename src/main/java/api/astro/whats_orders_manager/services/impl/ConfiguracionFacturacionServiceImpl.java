package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.repositories.ConfiguracionFacturacionRepository;
import api.astro.whats_orders_manager.services.ConfiguracionFacturacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Implementación del servicio de configuración de facturación.
 * 
 * Gestiona la configuración única del sistema para la generación de facturas,
 * incluyendo numeración automática, cálculo de impuestos y formato.
 * 
 * @author ASTRO
 * @version 1.0
 * @since Sprint 2 - Fase 2
 */
@Slf4j
@Service
@Transactional
public class ConfiguracionFacturacionServiceImpl implements ConfiguracionFacturacionService {

    @Autowired
    private ConfiguracionFacturacionRepository configuracionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionFacturacion> findById(Integer id) {
        log.debug("Buscando configuración de facturación por ID: {}", id);
        return configuracionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "configuracionFacturacion", key = "'activa'")
    public Optional<ConfiguracionFacturacion> getConfiguracionActiva() {
        log.debug("Obteniendo configuración de facturación activa (sin caché)");
        return configuracionRepository.findConfiguracionActiva();
    }

    @Override
    @Transactional
    @Cacheable(value = "configuracionFacturacion", key = "'activa'")
    public ConfiguracionFacturacion getOrCreateConfiguracion() {
        log.debug("Obteniendo o creando configuración de facturación");
        
        Optional<ConfiguracionFacturacion> configuracionOpt = configuracionRepository.findConfiguracionActiva();
        
        if (configuracionOpt.isPresent()) {
            log.debug("Configuración activa encontrada con ID: {}", configuracionOpt.get().getId());
            return configuracionOpt.get();
        }
        
        // Crear nueva configuración con valores por defecto
        log.info("No existe configuración activa. Creando configuración por defecto");
        ConfiguracionFacturacion nuevaConfiguracion = new ConfiguracionFacturacion();
        // Los valores por defecto ya están definidos en el modelo
        // serieFactura="F001", numeroInicial=1, igv=18.00, etc.
        
        ConfiguracionFacturacion guardada = configuracionRepository.save(nuevaConfiguracion);
        log.info("Configuración de facturación creada con ID: {}", guardada.getId());
        
        return guardada;
    }

    @Override
    @Transactional
    @CacheEvict(value = "configuracionFacturacion", allEntries = true)
    public ConfiguracionFacturacion save(ConfiguracionFacturacion configuracion) {
        log.debug("Guardando nueva configuración de facturación (invalidando caché)");
        
        // Validar datos
        validarConfiguracion(configuracion);
        
        // Validar que no exista otra configuración activa si esta es activa
        if (configuracion.getActivo() && configuracionRepository.existeConfiguracionActiva()) {
            log.error("Ya existe una configuración de facturación activa");
            throw new IllegalStateException("Ya existe una configuración de facturación activa. " +
                    "Solo puede haber una configuración activa a la vez.");
        }
        
        // Validar que la serie no esté duplicada
        Optional<ConfiguracionFacturacion> existente = configuracionRepository
                .findBySerieFactura(configuracion.getSerieFactura());
        if (existente.isPresent()) {
            log.error("La serie de factura {} ya existe", configuracion.getSerieFactura());
            throw new IllegalStateException("La serie de factura '" + configuracion.getSerieFactura() + 
                    "' ya está siendo utilizada");
        }
        
        // Asegurar que numeroActual comience en numeroInicial
        if (configuracion.getNumeroActual() == null || 
            configuracion.getNumeroActual() < configuracion.getNumeroInicial()) {
            configuracion.setNumeroActual(configuracion.getNumeroInicial());
        }
        
        ConfiguracionFacturacion guardada = configuracionRepository.save(configuracion);
        log.info("Configuración de facturación guardada exitosamente con ID: {}", guardada.getId());
        
        return guardada;
    }

    @Override
    @Transactional
    @CacheEvict(value = "configuracionFacturacion", allEntries = true)
    public ConfiguracionFacturacion update(ConfiguracionFacturacion configuracion) {
        log.debug("Actualizando configuración de facturación con ID: {} (invalidando caché)", configuracion.getId());
        
        // Verificar que la configuración existe
        ConfiguracionFacturacion existente = configuracionRepository.findById(configuracion.getId())
                .orElseThrow(() -> {
                    log.error("Configuración de facturación no encontrada con ID: {}", configuracion.getId());
                    return new IllegalArgumentException("La configuración de facturación no existe");
                });
        
        // Validar datos
        validarConfiguracion(configuracion);
        
        // Si se está cambiando la serie, validar que no exista otra con la misma serie
        if (!existente.getSerieFactura().equals(configuracion.getSerieFactura())) {
            Optional<ConfiguracionFacturacion> otraSerie = configuracionRepository
                    .findBySerieFactura(configuracion.getSerieFactura());
            if (otraSerie.isPresent() && !otraSerie.get().getId().equals(configuracion.getId())) {
                log.error("La serie de factura {} ya existe en otra configuración", configuracion.getSerieFactura());
                throw new IllegalStateException("La serie de factura '" + configuracion.getSerieFactura() + 
                        "' ya está siendo utilizada en otra configuración");
            }
        }
        
        // Validar que el número actual no sea menor que el número inicial
        if (configuracion.getNumeroActual() < configuracion.getNumeroInicial()) {
            log.error("El número actual ({}) no puede ser menor que el número inicial ({})", 
                    configuracion.getNumeroActual(), configuracion.getNumeroInicial());
            throw new IllegalStateException("El número actual no puede ser menor que el número inicial");
        }
        
        // Si se cambia el número inicial y es mayor que el actual, ajustar el actual
        if (configuracion.getNumeroInicial() > existente.getNumeroInicial() && 
            configuracion.getNumeroInicial() > configuracion.getNumeroActual()) {
            log.warn("Ajustando número actual de {} a {} porque el número inicial aumentó", 
                    configuracion.getNumeroActual(), configuracion.getNumeroInicial());
            configuracion.setNumeroActual(configuracion.getNumeroInicial());
        }
        
        ConfiguracionFacturacion actualizada = configuracionRepository.save(configuracion);
        log.info("Configuración de facturación actualizada exitosamente con ID: {}", actualizada.getId());
        
        return actualizada;
    }

    @Override
    @Transactional
    public String generarSiguienteNumeroFactura() {
        log.debug("Generando siguiente número de factura");
        
        ConfiguracionFacturacion configuracion = getConfiguracionActiva()
                .orElseThrow(() -> {
                    log.error("No existe configuración de facturación activa");
                    return new IllegalStateException("No existe configuración de facturación activa. " +
                            "Configure el sistema antes de generar facturas.");
                });
        
        String numeroFactura = configuracion.generarNumeroFactura();
        log.debug("Número de factura generado: {}", numeroFactura);
        
        return numeroFactura;
    }

    @Override
    @Transactional
    public ConfiguracionFacturacion incrementarNumeroFactura() {
        log.debug("Incrementando número de factura");
        
        ConfiguracionFacturacion configuracion = getConfiguracionActiva()
                .orElseThrow(() -> {
                    log.error("No existe configuración de facturación activa");
                    return new IllegalStateException("No existe configuración de facturación activa");
                });
        
        Integer numeroAnterior = configuracion.getNumeroActual();
        configuracion.incrementarNumero();
        ConfiguracionFacturacion actualizada = configuracionRepository.save(configuracion);
        
        log.info("Número de factura incrementado de {} a {}", numeroAnterior, actualizada.getNumeroActual());
        
        return actualizada;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeConfiguracionActiva() {
        log.debug("Verificando existencia de configuración activa");
        return configuracionRepository.existeConfiguracionActiva();
    }

    @Override
    public void validarConfiguracion(ConfiguracionFacturacion configuracion) {
        log.debug("Validando configuración de facturación");
        
        if (configuracion == null) {
            throw new IllegalArgumentException("La configuración no puede ser nula");
        }
        
        // Validar serie
        if (configuracion.getSerieFactura() == null || configuracion.getSerieFactura().trim().isEmpty()) {
            log.error("Serie de factura es requerida");
            throw new IllegalArgumentException("La serie de factura es requerida");
        }
        
        if (configuracion.getSerieFactura().length() > 10) {
            log.error("Serie de factura excede longitud máxima: {}", configuracion.getSerieFactura().length());
            throw new IllegalArgumentException("La serie de factura no puede exceder 10 caracteres");
        }
        
        // Validar números
        if (configuracion.getNumeroInicial() == null || configuracion.getNumeroInicial() < 1) {
            log.error("Número inicial inválido: {}", configuracion.getNumeroInicial());
            throw new IllegalArgumentException("El número inicial debe ser mayor o igual a 1");
        }
        
        if (configuracion.getNumeroActual() == null || configuracion.getNumeroActual() < 1) {
            log.error("Número actual inválido: {}", configuracion.getNumeroActual());
            throw new IllegalArgumentException("El número actual debe ser mayor o igual a 1");
        }
        
        if (configuracion.getNumeroActual() < configuracion.getNumeroInicial()) {
            log.error("Número actual ({}) menor que número inicial ({})", 
                    configuracion.getNumeroActual(), configuracion.getNumeroInicial());
            throw new IllegalArgumentException("El número actual no puede ser menor que el número inicial");
        }
        
        // Validar IGV
        if (configuracion.getIgv() == null) {
            log.error("IGV es requerido");
            throw new IllegalArgumentException("El porcentaje de IGV es requerido");
        }
        
        if (configuracion.getIgv().compareTo(BigDecimal.ZERO) < 0 || 
            configuracion.getIgv().compareTo(new BigDecimal("100")) > 0) {
            log.error("IGV fuera de rango: {}", configuracion.getIgv());
            throw new IllegalArgumentException("El IGV debe estar entre 0 y 100");
        }
        
        // Validar moneda
        if (configuracion.getMoneda() == null || configuracion.getMoneda().trim().isEmpty()) {
            log.error("Moneda es requerida");
            throw new IllegalArgumentException("La moneda es requerida");
        }
        
        if (configuracion.getMoneda().length() != 3) {
            log.error("Código de moneda inválido: {}", configuracion.getMoneda());
            throw new IllegalArgumentException("El código de moneda debe tener 3 caracteres (ISO 4217)");
        }
        
        if (configuracion.getSimboloMoneda() == null || configuracion.getSimboloMoneda().trim().isEmpty()) {
            log.error("Símbolo de moneda es requerido");
            throw new IllegalArgumentException("El símbolo de moneda es requerido");
        }
        
        // Validar decimales
        if (configuracion.getDecimales() == null || configuracion.getDecimales() < 0 || configuracion.getDecimales() > 4) {
            log.error("Decimales fuera de rango: {}", configuracion.getDecimales());
            throw new IllegalArgumentException("Los decimales deben estar entre 0 y 4");
        }
        
        // Validar formato de número
        if (configuracion.getFormatoNumero() == null || configuracion.getFormatoNumero().trim().isEmpty()) {
            log.error("Formato de número es requerido");
            throw new IllegalArgumentException("El formato de número es requerido");
        }
        
        if (!configuracion.getFormatoNumero().contains("{numero}")) {
            log.error("Formato de número no contiene placeholder {{numero}}");
            throw new IllegalArgumentException("El formato de número debe contener el placeholder {numero}");
        }
        
        // Validar longitud de términos y condiciones
        if (configuracion.getTerminosCondiciones() != null && 
            configuracion.getTerminosCondiciones().length() > 5000) {
            log.error("Términos y condiciones exceden longitud máxima");
            throw new IllegalArgumentException("Los términos y condiciones no pueden exceder 5000 caracteres");
        }
        
        // Validar longitud de nota de pie de página
        if (configuracion.getNotaPiePagina() != null && 
            configuracion.getNotaPiePagina().length() > 500) {
            log.error("Nota de pie de página excede longitud máxima");
            throw new IllegalArgumentException("La nota de pie de página no puede exceder 500 caracteres");
        }
        
        log.debug("Configuración validada exitosamente");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionFacturacion> findBySerieFactura(String serie) {
        log.debug("Buscando configuración por serie: {}", serie);
        return configuracionRepository.findBySerieFactura(serie);
    }
}
