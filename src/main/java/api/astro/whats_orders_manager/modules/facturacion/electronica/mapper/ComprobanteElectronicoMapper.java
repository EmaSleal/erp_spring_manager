package api.astro.whats_orders_manager.modules.facturacion.electronica.mapper;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Mapper para convertir entre ComprobanteElectronico y DTO.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Component
public class ComprobanteElectronicoMapper {

    @Value("${facturacion.hacienda.max-consultas-error:"
        + ComprobanteElectronicoServiceImpl.MAX_CONSULTAS_ERROR_HACIENDA_DEFECTO + "}")
    private int maxConsultasErrorHacienda = ComprobanteElectronicoServiceImpl.MAX_CONSULTAS_ERROR_HACIENDA_DEFECTO;
    
    /**
     * T-044: regla de ambiente para las banderas del DTO. Por campo; sin ella (pruebas unitarias) no hay regla y el
     * comprobante «coincide».
     */
    @Autowired(required = false)
    private AmbienteComprobanteGuard ambienteGuard;

    /**
     * Convierte entidad a DTO. Lee el ambiente activo una vez (lectura aislada); un listado usa
     * {@link #toDTO(ComprobanteElectronico, Optional)} con una sola lectura para todas las filas.
     */
    public ComprobanteElectronicoDTO toDTO(ComprobanteElectronico entity) {
        if (entity == null) {
            return null;
        }
        return toDTO(entity, ambienteGuard != null ? ambienteGuard.ambienteActivo() : Optional.empty());
    }

    /** Convierte entidad a DTO con el ambiente activo ya leído (vacío = sin regla de ambiente). */
    public ComprobanteElectronicoDTO toDTO(ComprobanteElectronico entity, Optional<AmbienteHacienda> activo) {
        if (entity == null) {
            return null;
        }

        return ComprobanteElectronicoDTO.builder()
            .id(entity.getId())
            .facturaId(entity.getFactura() != null ? entity.getFactura().getIdFactura().longValue() : null)
            .facturaNumero(entity.getFactura() != null ? entity.getFactura().getNumeroFactura() : null)
            .empresaId(entity.getEmpresa().getIdEmpresa().longValue())
            .empresaNombre(entity.getEmpresa().getNombreDisplay())
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
            .vigente(entity.getVigente())
            .verificableDesde(entity.getEstado() == EstadoComprobante.ENVIO_INCIERTO
                ? ComprobanteElectronicoServiceImpl.disponibleParaVerificar(entity) : null)
            .consultableEnHacienda(ComprobanteElectronicoServiceImpl.CONSULTABLES_A_MANO.contains(entity.getEstado()))
            .consultasErrorHacienda(entity.getConsultasErrorHacienda())
            .revisarEnAtv(entity.requiereRevisionEnAtv(maxConsultasErrorHacienda))
            .ambiente(entity.getAmbiente() != null ? entity.getAmbiente().name() : null)
            .ambienteCoincide(AmbienteComprobanteGuard.coincide(entity.getAmbiente(), activo))
            .motivoAmbiente(AmbienteComprobanteGuard.motivo(entity.getAmbiente(), activo))
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .xmlComprobante(entity.getXmlComprobante())
            .xmlRespuesta(entity.getXmlRespuesta())
            .build();
    }
}
