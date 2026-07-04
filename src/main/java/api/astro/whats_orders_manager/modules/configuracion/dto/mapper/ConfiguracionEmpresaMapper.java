package api.astro.whats_orders_manager.modules.configuracion.dto.mapper;

import api.astro.whats_orders_manager.modules.configuracion.dto.ConfiguracionEmpresaDTO;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between ConfiguracionEmpresa entity and DTO.
 * No password field exists on this entity — no masking required.
 */
@Component
public class ConfiguracionEmpresaMapper {

    /**
     * Converts a ConfiguracionEmpresa entity to its DTO representation.
     * Includes all persisted fields plus the 3 computed fields
     * (direccionCompleta, tieneLogoConfigurado, datosFiscalesCompletos).
     *
     * @param entity source entity; returns null if entity is null
     * @return mapped DTO
     */
    public ConfiguracionEmpresaDTO toDTO(ConfiguracionEmpresa entity) {
        if (entity == null) {
            return null;
        }

        ConfiguracionEmpresaDTO dto = new ConfiguracionEmpresaDTO();

        dto.setIdConfiguracion(entity.getIdConfiguracion());
        dto.setRazonSocial(entity.getRazonSocial());
        dto.setNombreComercial(entity.getNombreComercial());
        dto.setRfc(entity.getRfc());
        dto.setRegimenFiscal(entity.getRegimenFiscal());
        dto.setDireccionCalle(entity.getDireccionCalle());
        dto.setDireccionNumero(entity.getDireccionNumero());
        dto.setDireccionColonia(entity.getDireccionColonia());
        dto.setDireccionCiudad(entity.getDireccionCiudad());
        dto.setDireccionEstado(entity.getDireccionEstado());
        dto.setDireccionCodigoPostal(entity.getDireccionCodigoPostal());
        dto.setDireccionPais(entity.getDireccionPais());
        dto.setTelefono(entity.getTelefono());
        dto.setEmail(entity.getEmail());
        dto.setSitioWeb(entity.getSitioWeb());
        dto.setLogoUrl(entity.getLogoUrl());
        dto.setLogoPath(entity.getLogoPath());
        dto.setFaviconPath(entity.getFaviconPath());
        dto.setTieneLogoCargado(entity.tieneLogoCargado());
        dto.setTieneFaviconCargado(entity.tieneFaviconCargado());
        dto.setColorPrimario(entity.getColorPrimario());
        dto.setColorSecundario(entity.getColorSecundario());

        // Computed fields
        dto.setDireccionCompleta(entity.getDireccionCompleta());
        dto.setTieneLogoConfigurado(entity.tieneLogoConfigurado());
        dto.setDatosFiscalesCompletos(entity.tieneDatosFiscalesCompletos());

        // Electronic invoicing — Costa Rica
        dto.setNumeroIdentificacion(entity.getNumeroIdentificacion());
        dto.setTipoIdentificacion(entity.getTipoIdentificacion());
        dto.setCodigoProvincia(entity.getCodigoProvincia());
        dto.setCanton(entity.getCanton());
        dto.setDistrito(entity.getDistrito());
        dto.setBarrio(entity.getBarrio());
        dto.setOtrasSenas(entity.getOtrasSenas());
        dto.setCodigoActividad(entity.getCodigoActividad());
        dto.setDescripcionActividad(entity.getDescripcionActividad());
        dto.setNombreComercialFe(entity.getNombreComercialFe());

        return dto;
    }

    /**
     * Converts a ConfiguracionEmpresaDTO to its entity representation.
     * Maps all persisted fields only — computed fields (direccionCompleta, etc.)
     * are excluded because they are derived from entity logic, not stored columns.
     *
     * @param dto source DTO; returns null if dto is null
     * @return mapped entity
     */
    public ConfiguracionEmpresa toEntity(ConfiguracionEmpresaDTO dto) {
        if (dto == null) {
            return null;
        }

        return ConfiguracionEmpresa.builder()
                .idConfiguracion(dto.getIdConfiguracion())
                .razonSocial(dto.getRazonSocial())
                .nombreComercial(dto.getNombreComercial())
                .rfc(dto.getRfc())
                .regimenFiscal(dto.getRegimenFiscal())
                .direccionCalle(dto.getDireccionCalle())
                .direccionNumero(dto.getDireccionNumero())
                .direccionColonia(dto.getDireccionColonia())
                .direccionCiudad(dto.getDireccionCiudad())
                .direccionEstado(dto.getDireccionEstado())
                .direccionCodigoPostal(dto.getDireccionCodigoPostal())
                .direccionPais(dto.getDireccionPais())
                .telefono(dto.getTelefono())
                .email(dto.getEmail())
                .sitioWeb(dto.getSitioWeb())
                .logoUrl(dto.getLogoUrl())
                .logoPath(dto.getLogoPath())
                .faviconPath(dto.getFaviconPath())
                .colorPrimario(dto.getColorPrimario())
                .colorSecundario(dto.getColorSecundario())
                // Electronic invoicing — Costa Rica
                .numeroIdentificacion(dto.getNumeroIdentificacion())
                .tipoIdentificacion(dto.getTipoIdentificacion())
                .codigoProvincia(dto.getCodigoProvincia())
                .canton(dto.getCanton())
                .distrito(dto.getDistrito())
                .barrio(dto.getBarrio())
                .otrasSenas(dto.getOtrasSenas())
                .codigoActividad(dto.getCodigoActividad())
                .descripcionActividad(dto.getDescripcionActividad())
                .nombreComercialFe(dto.getNombreComercialFe())
                .build();
    }
}
