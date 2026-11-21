package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Regla única de «¿se ofrece "Emitir nota de crédito" en el modal de esta factura?» (ADR-047 §2, T-042). El servidor
 * decide; el JavaScript solo pinta la bandera. Es la misma regla de {@link NotaFacturaService#emitirNotaCreditoTotal}
 * (comprobante vigente ACEPTADO, factura no anulada, sin nota en trámite ni aplicada; con pagos sí, T-048) más el
 * permiso FACTURA_ANULAR del usuario. La emisión sigue validando todo con su 409.
 *
 * <p>T-044: la factura se referencia solo si su comprobante ACEPTADO es del ambiente de la configuración activa; si no,
 * la bandera es false con «La factura se emitió en el ambiente X; la configuración activa es Y». Orden de las
 * comprobaciones: permiso/anulada, ACEPTADO y notas, ambiente.
 */
@Component
@RequiredArgsConstructor
public class NotaCreditoElegibilidad {

    public static final String PERMISO_EMITIR = "FACTURA_ANULAR";

    /** 02 y 14: las notas parciales. */
    private static final Set<String> CODIGOS_PARCIALES = Set.of(CodigoReferenciaNota.CORRIGE_MONTO.getCodigo(),
        CodigoReferenciaNota.CORRIGE_MONTO_ERROR_MATERIAL.getCodigo());

    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final NotaFacturaRepository notaRepository;
    private final PermisoService permisoService;
    private final AmbienteComprobanteGuard ambienteGuard;
    private final NotaCreditoTopes topes;

    /**
     * @param puedeEmitir        habilita el botón: se puede emitir al menos una modalidad (total o parcial)
     * @param motivoNoDisponible texto del botón deshabilitado cuando solo lo impiden los pagos o el ambiente; si no, null
     * @param puedeTotal         la nota total (01 o 13) procede; con notas parciales aplicadas o en trámite, no
     * @param motivoTotal        por qué no procede la total cuando la parcial sí; si no, null
     * @param puedeParcial       la nota parcial (02 o 14) procede
     */
    public record Resultado(boolean puedeEmitir, String motivoNoDisponible, boolean puedeTotal, String motivoTotal,
                            boolean puedeParcial) {
        static final Resultado NO = new Resultado(false, null, false, null, false);

        public Resultado(boolean puedeEmitir, String motivoNoDisponible) {
            this(puedeEmitir, motivoNoDisponible, puedeEmitir, null, puedeEmitir);
        }
    }

    public Resultado evaluar(Factura factura, String usuario) {
        if (factura == null || factura.getIdFactura() == null || Boolean.TRUE.equals(factura.getAnulada())
            || factura.estaAcreditadaTotalmente()
            || !permisoService.tienePermisoPorCodigo(usuario, PERMISO_EMITIR)) {
            return Resultado.NO;
        }
        Integer id = factura.getIdFactura();
        ComprobanteElectronico aceptado = comprobanteRepository.findFirstByFactura_IdFacturaAndVigenteTrue(id)
            .filter(c -> c.getEstado() == EstadoComprobante.ACEPTADO).orElse(null);
        // Una sola nota en trámite; una nota aplicada que no es parcial (total) anula la factura. Las parciales
        // aplicadas dejan emitir otra parcial (T-047, ADR-051)
        if (aceptado == null
            || notaRepository.existsByFactura_IdFacturaAndEstadoIn(id, EnumSet.of(EstadoNotaFactura.EN_TRAMITE))
            || notaRepository.existsByFactura_IdFacturaAndEstadoInAndCodigoReferenciaNotIn(id,
                EnumSet.of(EstadoNotaFactura.APLICADA), CODIGOS_PARCIALES)) {
            return Resultado.NO;
        }
        // T-044 (P-099): después de permiso, anulada, ACEPTADO y notas (así el motivo es el que el usuario puede
        // corregir) y antes de pagos: la factura de otro ambiente no se referencia, sea cual sea el resto
        Optional<String> motivoAmbiente = ambienteGuard.motivoReferencia(aceptado);
        if (motivoAmbiente.isPresent()) {
            return new Resultado(false, motivoAmbiente.get());
        }
        // T-048 (ADR-053, ADR-057): una factura con pagos admite la nota; el excedente sobre su saldo pendiente se
        // acredita a un saldo a favor del cliente al aplicarse
        boolean hayParciales = notaRepository.existsByFactura_IdFacturaAndEstadoInAndCodigoReferenciaIn(id,
            NotaCreditoTopes.CUENTAN, CODIGOS_PARCIALES);
        if (hayParciales) {
            return new Resultado(true, null, false, NotaFacturaService.MENSAJE_TOTAL_CON_PARCIALES, true);
        }
        return new Resultado(true, null);
    }

    /**
     * Total de la factura, lo acreditado y lo disponible por línea y por factura (ADR-051 §3) para el modal de la
     * nota parcial; vacío si la factura no tiene comprobante vigente ACEPTADO.
     */
    public Optional<NotaCreditoTopes.Disponible> disponible(Factura factura) {
        if (factura == null || factura.getIdFactura() == null) {
            return Optional.empty();
        }
        return comprobanteRepository.findFirstByFactura_IdFacturaAndVigenteTrue(factura.getIdFactura())
            .filter(c -> c.getEstado() == EstadoComprobante.ACEPTADO)
            .flatMap(c -> {
                try {
                    return Optional.of(topes.calcular(factura, c));
                } catch (api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException e) {
                    return Optional.empty(); // comprobante sin TotalComprobante: la emisión lo rechaza con su 409
                }
            });
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
