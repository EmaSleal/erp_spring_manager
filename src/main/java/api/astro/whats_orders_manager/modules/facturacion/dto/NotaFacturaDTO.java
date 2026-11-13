package api.astro.whats_orders_manager.modules.facturacion.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Nota sobre una factura con el estado de su comprobante vigente (respuesta de la API de notas, T-032).
 */
public record NotaFacturaDTO(Long id, Integer idFactura, TipoNotaFactura tipo, String codigoReferencia, String razon,
                             EstadoNotaFactura estado, LocalDateTime fechaEmision, LocalDateTime fechaAplicacion,
                             String moneda, BigDecimal total, String claveReferencia, Long idComprobante,
                             String claveComprobante, EstadoComprobante estadoComprobante) {

    public static NotaFacturaDTO de(NotaFactura nota, ComprobanteElectronico comprobante) {
        return new NotaFacturaDTO(nota.getId(), nota.getFactura().getIdFactura(), nota.getTipo(),
            nota.getCodigoReferencia(), nota.getRazon(), nota.getEstado(), nota.getFechaEmision(),
            nota.getFechaAplicacion(), nota.getMoneda(), nota.getTotal(), nota.getClaveReferencia(),
            comprobante != null ? comprobante.getId() : null,
            comprobante != null ? comprobante.getClaveNumerica() : null,
            comprobante != null ? comprobante.getEstado() : null);
    }
}
