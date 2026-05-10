package api.astro.whats_orders_manager.modules.facturacion.electronica.mapper;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
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
            .passwordHacienda("********") // No exponer contraseña
            .rutaCertificado(entity.getRutaCertificado())
            .pinCertificado(entity.getPinCertificado() != null ? "********" : null)
            .activa(entity.getActiva())
            .tokenValido(entity.isTokenValido())
            .tokenExpira(entity.getTokenExpira())
            .consecutivoFactura(entity.getConsecutivoFactura())
            .consecutivoTiquete(entity.getConsecutivoTiquete())
            .consecutivoNotaCredito(entity.getConsecutivoNotaCredito())
            .consecutivoNotaDebito(entity.getConsecutivoNotaDebito())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
