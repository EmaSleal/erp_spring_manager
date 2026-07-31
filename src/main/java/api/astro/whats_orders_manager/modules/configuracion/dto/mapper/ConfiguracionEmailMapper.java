package api.astro.whats_orders_manager.modules.configuracion.dto.mapper;

import api.astro.whats_orders_manager.modules.configuracion.dto.ConfiguracionEmailDTO;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between ConfiguracionEmail entity and DTO.
 * Masks smtpPassword in toDTO — the raw value is never exposed in API responses.
 */
@Component
public class ConfiguracionEmailMapper {

    /**
     * Converts a ConfiguracionEmail entity to its DTO representation.
     * smtpPassword is always masked with "********" regardless of actual value.
     *
     * @param entity source entity; returns null if entity is null
     * @return mapped DTO
     */
    public ConfiguracionEmailDTO toDTO(ConfiguracionEmail entity) {
        if (entity == null) {
            return null;
        }

        ConfiguracionEmailDTO dto = new ConfiguracionEmailDTO();

        dto.setIdConfiguracion(entity.getIdConfiguracion());
        dto.setSmtpHost(entity.getSmtpHost());
        dto.setSmtpPort(entity.getSmtpPort());
        dto.setSmtpUsuario(entity.getSmtpUsuario());
        dto.setSmtpPassword("********"); // Never expose real password
        dto.setSmtpSsl(entity.getSmtpSsl());
        dto.setSmtpTls(entity.getSmtpTls());
        dto.setSmtpAuth(entity.getSmtpAuth());
        dto.setTimeout(entity.getSmtpTimeout());
        dto.setCharset(entity.getCharset());
        dto.setEmailRemitente(entity.getEmailRemitente());
        dto.setNombreRemitente(entity.getNombreRemitente());
        dto.setEmailCopia(entity.getEmailCopia());
        dto.setEmailCopiaOculta(entity.getEmailCopiaOculta());
        dto.setActivo(entity.getActivo());
        dto.setUltimoTest(entity.getUltimoTest());
        dto.setEstadoUltimoTest(entity.getEstadoUltimoTest());

        // Computed fields
        dto.setProtocoloSeguridad(entity.getProtocoloSeguridad());
        dto.setConfiguracionCompleta(entity.isConfiguracionCompleta());
        dto.setUltimoTestExitoso(entity.isUltimoTestExitoso());

        return dto;
    }

    /**
     * Converts a ConfiguracionEmailDTO to its entity representation.
     * smtpPassword is passed through unchanged — the caller is responsible for
     * not forwarding the masked value ("********") into persistence.
     *
     * @param dto source DTO; returns null if dto is null
     * @return mapped entity (persisted fields only — no computed fields)
     */
    public ConfiguracionEmail toEntity(ConfiguracionEmailDTO dto) {
        if (dto == null) {
            return null;
        }

        return ConfiguracionEmail.builder()
                .idConfiguracion(dto.getIdConfiguracion())
                .smtpHost(dto.getSmtpHost())
                .smtpPort(dto.getSmtpPort())
                .smtpUsuario(dto.getSmtpUsuario())
                .smtpPassword(dto.getSmtpPassword())
                .smtpSsl(dto.getSmtpSsl())
                .smtpTls(dto.getSmtpTls())
                .smtpAuth(dto.getSmtpAuth())
                .smtpTimeout(dto.getTimeout())
                .charset(dto.getCharset())
                .emailRemitente(dto.getEmailRemitente())
                .nombreRemitente(dto.getNombreRemitente())
                .emailCopia(dto.getEmailCopia())
                .emailCopiaOculta(dto.getEmailCopiaOculta())
                .activo(dto.getActivo())
                .build();
    }
}
