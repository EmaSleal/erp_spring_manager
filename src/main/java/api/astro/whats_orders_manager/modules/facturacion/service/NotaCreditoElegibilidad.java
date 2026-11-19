package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

/**
 * Regla única de «¿se ofrece "Emitir nota de crédito" en el modal de esta factura?» (ADR-047 §2, T-042). El servidor
 * decide; el JavaScript solo pinta la bandera. Es la misma regla de {@link NotaFacturaService#emitirNotaCreditoTotal}
 * (comprobante vigente ACEPTADO, factura no anulada, sin nota en trámite ni aplicada, sin pagos aplicados) más el
 * permiso FACTURA_ANULAR del usuario. La emisión sigue validando todo con su 409.
 */
@Component
@RequiredArgsConstructor
public class NotaCreditoElegibilidad {

    public static final String PERMISO_EMITIR = "FACTURA_ANULAR";
    public static final String MOTIVO_CON_PAGOS =
        "La nota de crédito sobre facturas con pagos llega en una próxima versión";

    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final NotaFacturaRepository notaRepository;
    private final PagoService pagoService;
    private final PermisoService permisoService;

    /**
     * @param puedeEmitir habilita el botón
     * @param motivoNoDisponible texto del botón deshabilitado cuando solo lo impiden los pagos; si no, null
     */
    public record Resultado(boolean puedeEmitir, String motivoNoDisponible) {
        static final Resultado NO = new Resultado(false, null);
    }

    public Resultado evaluar(Factura factura, String usuario) {
        if (factura == null || factura.getIdFactura() == null || Boolean.TRUE.equals(factura.getAnulada())
            || !permisoService.tienePermisoPorCodigo(usuario, PERMISO_EMITIR)) {
            return Resultado.NO;
        }
        Integer id = factura.getIdFactura();
        boolean aceptado = comprobanteRepository.findFirstByFactura_IdFacturaAndVigenteTrue(id)
            .map(c -> c.getEstado() == EstadoComprobante.ACEPTADO).orElse(false);
        if (!aceptado || notaRepository.existsByFactura_IdFacturaAndEstadoIn(id,
            EnumSet.of(EstadoNotaFactura.EN_TRAMITE, EstadoNotaFactura.APLICADA))) {
            return Resultado.NO;
        }
        if (!pagoService.findPagosValidosByFacturaId(id).isEmpty()) {
            return new Resultado(false, MOTIVO_CON_PAGOS);
        }
        return new Resultado(true, null);
    }
}
