package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.DesgloseImpuesto;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.DatosFeProducto;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Topes de la nota de crédito parcial (ADR-051 §3, T-047): lo que cada línea de la factura y la factura completa aún
 * admiten acreditar. Cuentan las notas APLICADAS y EN_TRAMITE; una RECHAZADA no cuenta (criterio 11). Único punto del
 * cálculo: lo usan la emisión ({@link NotaFacturaService}) y los datos del modal ({@link NotaCreditoElegibilidad}).
 */
@Component
@RequiredArgsConstructor
public class NotaCreditoTopes {

    /** Estados de nota que consumen el tope. */
    public static final Set<EstadoNotaFactura> CUENTAN =
        EnumSet.of(EstadoNotaFactura.EN_TRAMITE, EstadoNotaFactura.APLICADA);

    private final NotaFacturaRepository notaRepository;
    private final CalculadoraImpuestoService calculadoraImpuestoService;

    /**
     * Línea de la factura con lo ya acreditado y lo disponible, con IVA incluido y en la moneda de la factura.
     *
     * @param total      total de la línea (el MontoTotalLinea de su XML: base más impuesto)
     * @param acreditado notas APLICADAS y EN_TRAMITE
     * @param disponible total menos acreditado (nunca negativo)
     */
    public record LineaDisponible(Integer idLineaFactura, Integer numeroLinea, String descripcion, BigDecimal total,
                                  BigDecimal acreditado, BigDecimal disponible) {
    }

    /**
     * @param totalComprobante TotalComprobante del comprobante ACEPTADO
     * @param totalAcreditado  suma de las notas APLICADAS y EN_TRAMITE
     * @param disponible       totalComprobante menos totalAcreditado (nunca negativo)
     */
    public record Disponible(BigDecimal totalComprobante, BigDecimal totalAcreditado, BigDecimal disponible,
                             List<LineaDisponible> lineas) {
    }

    /** Mismo importe de línea que el XML de la factura (XmlGeneratorServiceImpl). */
    public DesgloseImpuesto desglosar(LineaFactura linea) {
        BigDecimal importe = linea.getSubtotal();
        if (importe == null) {
            BigDecimal precio = linea.getPrecioUnitario() != null ? linea.getPrecioUnitario() : BigDecimal.ZERO;
            importe = precio.multiply(cantidad(linea));
        }
        return calculadoraImpuestoService.desglosarLinea(linea.getProducto(), importe);
    }

    public static BigDecimal cantidad(LineaFactura linea) {
        return linea.getCantidad() != null ? BigDecimal.valueOf(linea.getCantidad()) : BigDecimal.ONE;
    }

    /** Total de la línea de la factura: el que declara su XML (base más impuesto). */
    public BigDecimal totalDeLinea(LineaFactura linea) {
        return desglosar(linea).total();
    }

    /**
     * TotalComprobante del XML del comprobante ACEPTADO.
     *
     * @throws NotaFacturaConflictoException si el XML no lo trae
     */
    public static BigDecimal totalComprobante(ComprobanteElectronico aceptado) {
        String totalXml = aceptado.getXmlComprobante() != null
            ? XmlValidator.extraerValorNodo(aceptado.getXmlComprobante(), "TotalComprobante") : null;
        if (totalXml == null || totalXml.isBlank()) {
            throw new NotaFacturaConflictoException("El comprobante aceptado " + aceptado.getClaveNumerica()
                + " no tiene TotalComprobante en su XML: no se puede verificar el monto de la nota");
        }
        return new BigDecimal(totalXml.trim());
    }

    /** Total acreditado por línea de factura (id de línea → total) por las notas APLICADAS y EN_TRAMITE. */
    public Map<Integer, BigDecimal> acreditadoPorLinea(Integer idFactura) {
        Map<Integer, BigDecimal> mapa = new HashMap<>();
        for (Object[] fila : notaRepository.acreditadoPorLinea(idFactura, CUENTAN)) {
            mapa.put((Integer) fila[0], (BigDecimal) fila[1]);
        }
        return mapa;
    }

    /** Suma de las notas APLICADAS y EN_TRAMITE de la factura. */
    public BigDecimal acreditadoDeLaFactura(Integer idFactura) {
        BigDecimal suma = notaRepository.sumaTotalPorFactura(idFactura, CUENTAN);
        return suma != null ? suma : BigDecimal.ZERO;
    }

    /** Lo disponible por línea y por factura, con el servidor como única fuente (criterio 12). */
    public Disponible calcular(Factura factura, ComprobanteElectronico aceptado) {
        Map<Integer, BigDecimal> acreditadoLineas = acreditadoPorLinea(factura.getIdFactura());
        List<LineaDisponible> lineas = new ArrayList<>();
        for (LineaFactura linea : factura.getLineas()) {
            BigDecimal total = totalDeLinea(linea);
            BigDecimal acreditado = acreditadoLineas.getOrDefault(linea.getIdLineaFactura(), BigDecimal.ZERO);
            lineas.add(new LineaDisponible(linea.getIdLineaFactura(), linea.getNumeroLinea(),
                DatosFeProducto.detalle(linea.getProducto()), total, acreditado,
                total.subtract(acreditado).max(BigDecimal.ZERO)));
        }
        BigDecimal totalComprobante = totalComprobante(aceptado);
        BigDecimal acreditado = acreditadoDeLaFactura(factura.getIdFactura());
        return new Disponible(totalComprobante, acreditado, totalComprobante.subtract(acreditado).max(BigDecimal.ZERO),
            lineas);
    }
}
