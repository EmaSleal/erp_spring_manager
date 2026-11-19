package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

/** Comprobante de una nota (vigente o del historial) con lo que muestra el detalle; sin el XML ni entidades. */
public record ComprobanteNotaDTO(Long id, String clave, String consecutivo, String estado, String estadoDescripcion,
                                 LocalDateTime fechaEnvio, LocalDateTime fechaRespuesta, String codigoRespuesta,
                                 String mensajeRespuesta, String ultimoError, boolean puedeReenviar,
                                 boolean consultableEnHacienda, String ambiente, boolean ambienteCoincide,
                                 String motivoAmbiente) {

    /** Sin regla de ambiente (no hay configuración activa que lo contradiga). */
    public static ComprobanteNotaDTO de(ComprobanteElectronico c) {
        return de(c, Optional.empty());
    }

    /** T-044: con el ambiente activo ya leído; «Reenviar» y «Consultar» se deshabilitan en la UI con motivoAmbiente. */
    public static ComprobanteNotaDTO de(ComprobanteElectronico c, Optional<AmbienteHacienda> activo) {
        if (c == null) {
            return null;
        }
        return new ComprobanteNotaDTO(c.getId(), c.getClaveNumerica(), c.getConsecutivo(),
            c.getEstado() != null ? c.getEstado().name() : null,
            c.getEstado() != null ? c.getEstado().getDescripcion() : null,
            c.getFechaEnvio(), c.getFechaRespuesta(), c.getCodigoRespuesta(), c.getMensajeRespuesta(),
            c.getUltimoError(), c.getEstado() != null && c.puedeReenviar(), consultable(c),
            c.getAmbiente() != null ? c.getAmbiente().name() : null,
            AmbienteComprobanteGuard.coincide(c.getAmbiente(), activo),
            AmbienteComprobanteGuard.motivo(c.getAmbiente(), activo));
    }

    /** Misma regla que «Consultar estado en Hacienda» de comprobantes (ADR-031, T-040). */
    public static boolean consultable(ComprobanteElectronico c) {
        return c != null && ComprobanteElectronicoServiceImpl.CONSULTABLES_A_MANO.contains(c.getEstado());
    }
}
