package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.event.ComprobanteNotaResueltoEvent;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Efectos de una nota de crédito cuando Hacienda resuelve su comprobante (ADR-037 §3), en la misma transacción que
 * guarda el estado del comprobante y una sola vez: solo una nota EN_TRAMITE cambia.
 *
 * <ul>
 *   <li>ACEPTADO: asiento propio de la nota (ADR-052, ADR-054: débito a la cuenta de la base según el código, IVA por
 *       pagar, crédito a cuentas por cobrar; fecha contable de la nota) y, según la modalidad, nota total (01, 13):
 *       factura anulada por la ruta interna; nota parcial (02, 14): sube lo acreditado de la factura y, al llegar al
 *       TotalComprobante, la marca «acreditada totalmente» sin anularla. La nota queda APLICADA. Sin asiento de venta
 *       vigente (facturas históricas), el asiento se omite y se registra en el log.</li>
 *   <li>T-048 (ADR-053): con la factura bloqueada FOR UPDATE, el total de la nota se reparte al aplicarla (no al emitirla)
 *       entre cuentas por cobrar ({@code min(total, saldo pendiente)}) y un saldo a favor del cliente (el excedente, con su
 *       línea 2.1.05.00x en el asiento). Los pagos de la factura no se tocan.</li>
 *   <li>Si cualquier paso falla (p. ej. falta un parámetro contable), se lanza: nada se persiste (tampoco el ACEPTADO del
 *       comprobante) y la nota sigue EN_TRAMITE para el siguiente sondeo.</li>
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
    private final Clock clock;
    private final FacturaRepository facturaRepository;
    private final SaldoFavorService saldoFavorService;
    private final EntityManager entityManager;

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
        // T-048: la factura se bloquea FOR UPDATE antes de leer sus pagos y su saldo; refresh la recarga de la fila
        // bloqueada (la instancia ya está en el contexto de persistencia y un @Lock por consulta no la refrescaría)
        Factura factura = Hibernate.unproxy(nota.getFactura(), Factura.class);
        entityManager.refresh(factura, LockModeType.PESSIMISTIC_WRITE);

        // Reparto (ADR-053): se calcula ahora, con el saldo de la factura antes de que la nota lo mueva
        BigDecimal saldoPendiente = factura.calcularSaldoPendiente();
        BigDecimal creditoCxc = nota.getTotal().min(saldoPendiente);
        BigDecimal excedente = nota.getTotal().subtract(creditoCxc);

        // ADR-052/054: asiento propio de la nota (débito a la base según el código, IVA, crédito a cuentas por cobrar y,
        // si hay excedente, a saldos a favor), total o parcial; ya no se revierte el asiento de venta. Los asientos de
        // reversión de notas anteriores no se tocan
        List<AsientoContable> ventas = asientoContableService.obtenerAsientosVentaVigentes(factura);
        if (ventas.isEmpty()) {
            log.warn("Factura {} sin asiento de venta vigente: la nota {} se aplica sin asiento contable",
                factura.getNumeroFactura(), nota.getId());
        } else {
            asientoContableService.generarAsientoNotaCredito(nota, ventas.get(0), fechaDelAsiento(nota), creditoCxc,
                excedente);
        }

        if (nota.esParcial()) {
            acreditar(factura, nota);
        } else {
            String motivo = "Nota de crédito " + nota.getId() + " (clave " + comprobante.getClaveNumerica()
                + ") aceptada por Hacienda, código " + nota.getCodigoReferencia() + ": " + nota.getRazon();
            facturaService.anularPorNotaCredito(factura, motivo);
        }

        nota.setMontoCreditoCxc(creditoCxc);
        nota.setMontoSaldoFavor(excedente);
        nota.setEstado(EstadoNotaFactura.APLICADA);
        nota.setFechaAplicacion(LocalDateTime.now(clock));
        notaRepository.save(nota);
        if (excedente.signum() > 0) {
            saldoFavorService.crearDesdeNota(nota, excedente, comprobante.getClaveNumerica());
        }
        log.info("Nota {} ({}) APLICADA sobre la factura {}: {} a cuentas por cobrar, {} a saldo a favor", nota.getId(),
            nota.esParcial() ? "parcial" : "total", factura.getNumeroFactura(), creditoCxc, excedente);
    }

    /**
     * Nota parcial aplicada (ADR-051 §5): sube lo acreditado de la factura y, si la suma de las notas APLICADAS alcanza el
     * TotalComprobante del comprobante ACEPTADO, la marca «acreditada totalmente». La factura no se anula; el saldo
     * ({@code Factura.calcularSaldoPendiente}) baja en lo acreditado. Misma transacción que la nota.
     */
    private void acreditar(Factura factura, NotaFactura nota) {
        BigDecimal acreditado = factura.calcularTotalAcreditado().add(nota.getTotal());
        factura.setMontoAcreditado(acreditado);
        BigDecimal totalComprobante = NotaCreditoTopes.totalComprobante(nota.getComprobanteReferencia());
        if (acreditado.compareTo(totalComprobante) >= 0) {
            factura.setAcreditadaTotalmente(true);
            log.info("Factura {} acreditada totalmente: {} de {}", factura.getNumeroFactura(), acreditado,
                totalComprobante);
        }
        if (factura.calcularTotalPagado().signum() > 0) {
            factura.actualizarEstadoPago(); // con pagos, lo acreditado puede dejar la factura pagada del todo (T-048)
        }
        facturaRepository.save(factura);
    }

    /**
     * Fecha del asiento de reversión de la nota (ADR-050 §1): su fecha contable, guardada al emitirla, que es la fecha
     * de emisión de la nota para todos los códigos (13 incluido). El período del IVA va aparte
     * ({@code periodoFiscalAtribucion}).
     */
    public static LocalDate fechaDelAsiento(NotaFactura nota) {
        if (nota.getFechaContable() == null) {
            throw new IllegalStateException("La nota " + nota.getId() + " no tiene fecha contable");
        }
        return nota.getFechaContable();
    }
}
