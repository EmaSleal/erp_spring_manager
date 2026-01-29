package api.astro.whats_orders_manager.modules.facturacion.electronica.mapper;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre ComprobanteElectronico y DTO.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Component
public class ComprobanteElectronicoMapper {
    
    /**
     * Convierte entidad a DTO.
     */
    public ComprobanteElectronicoDTO toDTO(ComprobanteElectronico entity) {
        if (entity == null) {
            return null;
        }
        
        return ComprobanteElectronicoDTO.builder()
            .id(entity.getId())
            .facturaId(entity.getFactura() != null ? entity.getFactura().getIdFactura().longValue() : null)
            .facturaNumero(entity.getFactura() != null ? entity.getFactura().getNumeroFactura() : null)
            .empresaId(entity.getEmpresa().getIdEmpresa().longValue())
            .empresaNombre(entity.getEmpresa().getNombreParaMostrar())
            .tipoComprobante(entity.getTipoComprobante())
            .tipoComprobanteDescripcion(entity.getTipoComprobante().getDescripcion())
            .claveNumerica(entity.getClaveNumerica())
            .consecutivo(entity.getConsecutivo())
            .fechaEmision(entity.getFechaEmision())
            .estado(entity.getEstado())
            .estadoDescripcion(entity.getEstado().getDescripcion())
            .codigoRespuesta(entity.getCodigoRespuesta())
            .mensajeRespuesta(entity.getMensajeRespuesta())
            .fechaEnvio(entity.getFechaEnvio())
            .fechaRespuesta(entity.getFechaRespuesta())
            .intentosEnvio(entity.getIntentosEnvio())
            .ultimoError(entity.getUltimoError())
            .enviadoEmail(entity.getEnviadoEmail())
            .fechaEnvioEmail(entity.getFechaEnvioEmail())
            .urlPdf(entity.getUrlPdf())
            .puedeReenviar(entity.puedeReenviar())
            .listoParaEnviar(entity.listoParaEnviar())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .xmlComprobante(entity.getXmlComprobante())
            .xmlRespuesta(entity.getXmlRespuesta())
            .build();
    }
}
