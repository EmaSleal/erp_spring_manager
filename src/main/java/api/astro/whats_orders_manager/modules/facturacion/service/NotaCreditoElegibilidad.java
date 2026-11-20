package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Regla única de «¿se ofrece "Emitir nota de crédito" en el modal de esta factura?» (ADR-047 §2, T-042). El servidor
 * decide; el JavaScript solo pinta la bandera. Es la misma regla de {@link NotaFacturaService#emitirNotaCreditoTotal}
 * (comprobante vigente ACEPTADO, factura no anulada, sin nota en trámite ni aplicada, sin pagos aplicados) más el
 * permiso FACTURA_ANULAR del usuario. La emisión sigue validando todo con su 409.
 *
 * <p>T-044: la factura se referencia solo si su comprobante ACEPTADO es del ambiente de la configuración activa; si no,
 * la bandera es false con «La factura se emitió en el ambiente X; la configuración activa es Y». Orden de las
 * comprobaciones: permiso/anulada, ACEPTADO y notas, ambiente, pagos.
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
    private final AmbienteComprobanteGuard ambienteGuard;

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
        ComprobanteElectronico aceptado = comprobanteRepository.findFirstByFactura_IdFacturaAndVigenteTrue(id)
            .filter(c -> c.getEstado() == EstadoComprobante.ACEPTADO).orElse(null);
        if (aceptado == null || notaRepository.existsByFactura_IdFacturaAndEstadoIn(id,
            EnumSet.of(EstadoNotaFactura.EN_TRAMITE, EstadoNotaFactura.APLICADA))) {
            return Resultado.NO;
        }
        // T-044 (P-099): después de permiso, anulada, ACEPTADO y notas (así el motivo es el que el usuario puede
        // corregir) y antes de pagos: la factura de otro ambiente no se referencia, sea cual sea el resto
        Optional<String> motivoAmbiente = ambienteGuard.motivoReferencia(aceptado);
        if (motivoAmbiente.isPresent()) {
            return new Resultado(false, motivoAmbiente.get());
        }
        if (!pagoService.findPagosValidosByFacturaId(id).isEmpty()) {
            return new Resultado(false, MOTIVO_CON_PAGOS);
        }
        return new Resultado(true, null);
    }

    /**
     * Mes (AAAA-MM) de la factura original para el texto del modal del código 13 (ADR-050 §2): el de la fecha de
     * emisión de su comprobante vigente ACEPTADO, el mismo dato que usa {@link AtribucionFiscalService} al emitir.
     */
    public Optional<String> periodoFacturaOriginal(Factura factura) {
        if (factura == null || factura.getIdFactura() == null) {
            return Optional.empty();
        }
        return comprobanteRepository.findFirstByFactura_IdFacturaAndVigenteTrue(factura.getIdFactura())
            .filter(c -> c.getEstado() == EstadoComprobante.ACEPTADO && c.getFechaEmision() != null)
            .map(c -> AtribucionFiscalService.formatear(java.time.YearMonth.from(c.getFechaEmision())));
    }
}
