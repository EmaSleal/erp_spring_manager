package api.astro.whats_orders_manager.modules.facturacion.electronica.mapper;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaSinCredenciales;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre ConfiguracionHacienda y DTO.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Component
public class ConfiguracionHaciendaMapper {

    /** Valor enmascarado que se devuelve en lugar de la contraseña y el PIN (T-021). */
    public static final String MASK = "********";
    
    /** T-041: aviso fijo de una configuración cuyas credenciales no se descifran. */
    public static final String AVISO_CREDENCIALES_ILEGIBLES = "Las credenciales guardadas no se pueden descifrar con "
        + "la clave actual del servidor; vuelva a ingresarlas";

    /**
     * T-041: DTO de una configuración cuyas credenciales no se descifran, a partir de las columnas no cifradas.
     * Marca el aviso fijo; no expone ninguna credencial.
     */
    public ConfiguracionHaciendaDTO toDTOIlegible(ConfiguracionHaciendaSinCredenciales fila) {
        return ConfiguracionHaciendaDTO.builder()
            .id(fila.getId())
            .empresaId(fila.getEmpresa().getIdEmpresa().longValue())
            .empresaNombre(fila.getEmpresa().getNombreDisplay())
            .ambiente(fila.getAmbiente())
            .usuarioHacienda(fila.getUsuarioHacienda())
            .passwordHacienda(MASK)
            .rutaCertificado(fila.getRutaCertificado())
            .activa(fila.getActiva())
            .tokenValido(false)
            .tieneToken(false)
            .credencialesIlegibles(true)
            .avisoCredenciales(AVISO_CREDENCIALES_ILEGIBLES)
            .consecutivoFactura(fila.getConsecutivoFactura())
            .consecutivoTiquete(fila.getConsecutivoTiquete())
            .consecutivoNotaCredito(fila.getConsecutivoNotaCredito())
            .consecutivoNotaDebito(fila.getConsecutivoNotaDebito())
            .createdAt(fila.getCreatedAt())
            .updatedAt(fila.getUpdatedAt())
            .build();
    }

    /**
     * Convierte entidad a DTO.
     */
    public ConfiguracionHaciendaDTO toDTO(ConfiguracionHacienda entity) {
        if (entity == null) {
            return null;
        }
        
        return ConfiguracionHaciendaDTO.builder()
            .id(entity.getId())
            .empresaId(entity.getEmpresa().getIdEmpresa().longValue())
            .empresaNombre(entity.getEmpresa().getNombreDisplay())
            .ambiente(entity.getAmbiente())
            .usuarioHacienda(entity.getUsuarioHacienda())
            .passwordHacienda(MASK) // No exponer contraseña
            .rutaCertificado(entity.getRutaCertificado())
            .pinCertificado(entity.getPinCertificado() != null ? MASK : null)
            .activa(entity.getActiva())
            .tokenValido(entity.isTokenValido())
            .tokenExpira(entity.getTokenExpira())
            .tieneToken(entity.getAccessToken() != null)
            .consecutivoFactura(entity.getConsecutivoFactura())
            .consecutivoTiquete(entity.getConsecutivoTiquete())
            .consecutivoNotaCredito(entity.getConsecutivoNotaCredito())
            .consecutivoNotaDebito(entity.getConsecutivoNotaDebito())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
