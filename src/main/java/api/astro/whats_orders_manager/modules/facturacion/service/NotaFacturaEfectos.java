package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.event.ComprobanteNotaResueltoEvent;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.PeriodoEfectoNota;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaEfectosException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Efectos de una nota de crédito cuando Hacienda resuelve su comprobante (ADR-037 §3), en la misma transacción que
 * guarda el estado del comprobante y una sola vez: solo una nota EN_TRAMITE cambia.
 *
 * <ul>
 *   <li>ACEPTADO: reversión espejo del asiento de venta vigente (fecha según el código de referencia), factura anulada
 *       por la ruta interna y nota APLICADA. Sin asiento de venta vigente (facturas históricas), la reversión se omite
 *       y se registra en el log.</li>
 *   <li>Si la factura tiene pagos aplicados, o cualquier paso falla, se lanza: nada se persiste (tampoco el
 *       ACEPTADO del comprobante) y la nota sigue EN_TRAMITE para el siguiente sondeo.</li>
 *   <li>RECHAZADO: nota RECHAZADA, sin efectos; la factura admite una nota nueva.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotaFacturaEfectos {

    private final NotaFacturaRepository notaRepository;
    private final AsientoContableService asientoContableService;
    private final FacturaService facturaService;
    private final PagoService pagoService;
    private final Clock clock;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void alResolverseComprobante(ComprobanteNotaResueltoEvent evento) {
        ComprobanteElectronico comprobante = evento.comprobante();
        NotaFactura nota = comprobante.getNotaFactura();
        if (nota == null || !Boolean.TRUE.equals(comprobante.getVigente())) {
            return;
        }
        if (nota.getEstado() != EstadoNotaFactura.EN_TRAMITE) {
            log.info("Nota {} ya está {}: el comprobante {} ({}) no vuelve a aplicar efectos", nota.getId(),
                nota.getEstado(), comprobante.getClaveNumerica(), comprobante.getEstado());
            return;
        }
        if (comprobante.getEstado() == EstadoComprobante.ACEPTADO) {
            try {
                aplicar(nota, comprobante);
            } catch (RuntimeException e) {
                log.error("Efectos de la nota {} (comprobante {}) no aplicados; la nota sigue EN_TRAMITE y el sondeo "
                    + "volverá a intentarlo: {}", nota.getId(), comprobante.getClaveNumerica(), e.getMessage(), e);
                throw e;
            }
        } else if (comprobante.getEstado() == EstadoComprobante.RECHAZADO) {
            nota.setEstado(EstadoNotaFactura.RECHAZADA);
            notaRepository.save(nota);
            log.info("Nota {} RECHAZADA por Hacienda (comprobante {}): sin efectos", nota.getId(),
                comprobante.getClaveNumerica());
        }
    }

    private void aplicar(NotaFactura nota, ComprobanteElectronico comprobante) {
        Factura factura = nota.getFactura();
        // Defensa (criterio 31): la nota total no se aplica sobre una factura con pagos; los pagos quedan bloqueados
        // mientras la nota está en trámite (NotaFacturaPrecondiciones), así que esto solo pasa por datos previos
        if (!pagoService.findPagosValidosByFacturaId(factura.getIdFactura()).isEmpty()) {
            throw new NotaFacturaEfectosException("La factura " + factura.getNumeroFactura() + " tiene pagos aplicados: "
                + "la nota de crédito total " + nota.getId() + " no se aplica hasta que se anulen");
        }
        String motivo = "Nota de crédito " + nota.getId() + " (clave " + comprobante.getClaveNumerica()
            + ") aceptada por Hacienda, código " + nota.getCodigoReferencia() + ": " + nota.getRazon();

        List<AsientoContable> ventas = asientoContableService.obtenerAsientosVentaVigentes(factura);
        if (ventas.isEmpty()) {
            log.warn("Factura {} sin asiento de venta vigente: la nota {} se aplica sin asiento de reversión",
                factura.getNumeroFactura(), nota.getId());
        }
        for (AsientoContable venta : ventas) {
            asientoContableService.generarAsientoReversion(venta, motivo, fechaDelAsiento(nota, venta), nota);
        }

        facturaService.anularPorNotaCredito(factura, motivo);

        nota.setEstado(EstadoNotaFactura.APLICADA);
        nota.setFechaAplicacion(LocalDateTime.now(clock));
        notaRepository.save(nota);
        log.info("Nota {} APLICADA: factura {} anulada, {} asiento(s) revertido(s)", nota.getId(),
            factura.getNumeroFactura(), ventas.size());
    }

    /**
     * Fecha del asiento de la nota según el periodo del efecto de su código (nota 9, ADR-037 §4): periodo de la nota
     * (01) → fecha de emisión de la nota; periodo del documento original (13) → fecha del asiento de venta original,
     * que es la fecha económica de la factura (ADR-014).
     */
    public static LocalDate fechaDelAsiento(NotaFactura nota, AsientoContable asientoVenta) {
        PeriodoEfectoNota periodo = nota.codigo().getPeriodoEfecto();
        if (periodo == PeriodoEfectoNota.DOCUMENTO_ORIGINAL) {
            return asientoVenta.getFecha();
        }
        if (periodo == PeriodoEfectoNota.NOTA) {
            return nota.getFechaEmision().toLocalDate();
        }
        throw new IllegalStateException("El código de referencia " + nota.getCodigoReferencia()
            + " no define el periodo de su efecto contable");
    }
}
