package api.astro.whats_orders_manager.modules.configuracion.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación del servicio de configuración de empresa
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ConfiguracionEmpresaServiceImpl implements ConfiguracionEmpresaService {

    private final ConfiguracionEmpresaRepository configuracionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionEmpresa> obtenerConfiguracion() {
        log.debug("Obteniendo configuración de empresa");
        return configuracionRepository.findFirstByOrderByIdConfiguracionAsc();
    }

    @Override
    public ConfiguracionEmpresa obtenerOCrearConfiguracion() {
        log.debug("Obteniendo o creando configuración de empresa");
        
        Optional<ConfiguracionEmpresa> configuracionOpt = obtenerConfiguracion();
        
        if (configuracionOpt.isPresent()) {
            return configuracionOpt.get();
        }
        
        // Crear configuración por defecto
        ConfiguracionEmpresa nuevaConfiguracion = ConfiguracionEmpresa.builder()
                .razonSocial("Mi Empresa")
                .nombreComercial("Mi Empresa")
                .direccionPais("México")
                .colorPrimario("#007bff")
                .colorSecundario("#6c757d")
                .build();
        
        ConfiguracionEmpresa guardada = configuracionRepository.save(nuevaConfiguracion);
        log.info("Configuración de empresa creada con ID: {}", guardada.getIdConfiguracion());
        
        return guardada;
    }

    @Override
    public ConfiguracionEmpresa guardarConfiguracion(ConfiguracionEmpresa configuracion) {
        log.debug("Guardando configuración de empresa");
        
        if (configuracion == null) {
            throw new IllegalArgumentException("La configuración no puede ser null");
        }
        
        // Validar que la razón social no esté vacía
        if (configuracion.getRazonSocial() == null || configuracion.getRazonSocial().trim().isEmpty()) {
            throw new IllegalArgumentException("La razón social es obligatoria");
        }
        
        ConfiguracionEmpresa guardada = configuracionRepository.save(configuracion);
        log.info("Configuración de empresa guardada correctamente con ID: {}", guardada.getIdConfiguracion());
        
        return guardada;
    }

    @Override
    public ConfiguracionEmpresa actualizarConfiguracion(ConfiguracionEmpresa configuracion) {
        log.debug("Actualizando configuración de empresa");
        
        if (configuracion == null) {
            throw new IllegalArgumentException("La configuración no puede ser null");
        }
        
        // Obtener la configuración existente
        ConfiguracionEmpresa configuracionExistente = obtenerOCrearConfiguracion();
        
        // Actualizar solo los campos no nulos
        if (configuracion.getRazonSocial() != null) {
            configuracionExistente.setRazonSocial(configuracion.getRazonSocial());
        }
        if (configuracion.getNombreComercial() != null) {
            configuracionExistente.setNombreComercial(configuracion.getNombreComercial());
        }
        if (configuracion.getRfc() != null) {
            configuracionExistente.setRfc(configuracion.getRfc());
        }
        if (configuracion.getRegimenFiscal() != null) {
            configuracionExistente.setRegimenFiscal(configuracion.getRegimenFiscal());
        }
        if (configuracion.getDireccionCalle() != null) {
            configuracionExistente.setDireccionCalle(configuracion.getDireccionCalle());
        }
        if (configuracion.getDireccionNumero() != null) {
            configuracionExistente.setDireccionNumero(configuracion.getDireccionNumero());
        }
        if (configuracion.getDireccionColonia() != null) {
            configuracionExistente.setDireccionColonia(configuracion.getDireccionColonia());
        }
        if (configuracion.getDireccionCiudad() != null) {
            configuracionExistente.setDireccionCiudad(configuracion.getDireccionCiudad());
        }
        if (configuracion.getDireccionEstado() != null) {
            configuracionExistente.setDireccionEstado(configuracion.getDireccionEstado());
        }
        if (configuracion.getDireccionCodigoPostal() != null) {
            configuracionExistente.setDireccionCodigoPostal(configuracion.getDireccionCodigoPostal());
        }
        if (configuracion.getDireccionPais() != null) {
            configuracionExistente.setDireccionPais(configuracion.getDireccionPais());
        }
        if (configuracion.getTelefono() != null) {
            configuracionExistente.setTelefono(configuracion.getTelefono());
        }
        if (configuracion.getEmail() != null) {
            configuracionExistente.setEmail(configuracion.getEmail());
        }
        if (configuracion.getSitioWeb() != null) {
            configuracionExistente.setSitioWeb(configuracion.getSitioWeb());
        }
        if (configuracion.getLogoUrl() != null) {
            configuracionExistente.setLogoUrl(configuracion.getLogoUrl());
        }
        if (configuracion.getColorPrimario() != null) {
            configuracionExistente.setColorPrimario(configuracion.getColorPrimario());
        }
        if (configuracion.getColorSecundario() != null) {
            configuracionExistente.setColorSecundario(configuracion.getColorSecundario());
        }

        // Electronic invoicing — Costa Rica
        if (configuracion.getNumeroIdentificacion() != null) {
            configuracionExistente.setNumeroIdentificacion(configuracion.getNumeroIdentificacion());
        }
        if (configuracion.getTipoIdentificacion() != null) {
            configuracionExistente.setTipoIdentificacion(configuracion.getTipoIdentificacion());
        }
        if (configuracion.getCodigoProvincia() != null) {
            configuracionExistente.setCodigoProvincia(configuracion.getCodigoProvincia());
        }
        if (configuracion.getCanton() != null) {
            configuracionExistente.setCanton(configuracion.getCanton());
        }
        if (configuracion.getDistrito() != null) {
            configuracionExistente.setDistrito(configuracion.getDistrito());
        }
        if (configuracion.getBarrio() != null) {
            configuracionExistente.setBarrio(configuracion.getBarrio());
        }
        if (configuracion.getOtrasSenas() != null) {
            configuracionExistente.setOtrasSenas(configuracion.getOtrasSenas());
        }
        if (configuracion.getCodigoActividad() != null) {
            configuracionExistente.setCodigoActividad(configuracion.getCodigoActividad());
        }
        if (configuracion.getNombreComercialFe() != null) {
            configuracionExistente.setNombreComercialFe(configuracion.getNombreComercialFe());
        }

        ConfiguracionEmpresa actualizada = configuracionRepository.save(configuracionExistente);
        log.info("Configuración de empresa actualizada correctamente");
        
        return actualizada;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeConfiguracion() {
        boolean existe = configuracionRepository.existsByIdConfiguracionIsNotNull();
        log.debug("¿Existe configuración de empresa?: {}", existe);
        return existe;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validarDatosFiscales() {
        log.debug("Validando datos fiscales de la empresa");

        Optional<ConfiguracionEmpresa> configuracionOpt = obtenerConfiguracion();

        if (configuracionOpt.isEmpty()) {
            return false;
        }

        boolean valido = configuracionOpt.get().tieneDatosFiscalesCompletos();
        log.debug("Datos fiscales válidos: {}", valido);

        return valido;
    }

    @Override
    public ConfiguracionEmpresa saveOrUpdate(ConfiguracionEmpresa configuracion) {
        return actualizarConfiguracion(configuracion);
    }
}
