package api.astro.whats_orders_manager.modules.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Antigüedad de los saldos a favor vivos por cliente y moneda (T-049, ADR-058 §6). Los montos están en la moneda original
 * del saldo (no se mezclan monedas). Es solo informativo: ni este reporte ni el sistema revierten saldos a ingresos.
 *
 * <p>La antigüedad es {@code fechaCorte − fechaOrigen} en días; un saldo originado después del corte no existía y no
 * aparece. Los montos son el saldo vivo actual (no se reconstruye el de una fecha pasada).
 */
public record AntiguedadSaldosFavorDTO(LocalDate fechaCorte, List<Fila> filas, List<Fila> totales) {

    /**
     * Una fila por cliente y moneda; en {@code totales} hay una fila por moneda con {@code idCliente} null y
     * {@code cliente} «Total». {@code reservado} es lo reservado por devoluciones en trámite y {@code disponible} es
     * {@code total − reservado}.
     */
    public record Fila(Integer idCliente, String cliente, String moneda, BigDecimal tramo0a7, BigDecimal tramo8a30,
                       BigDecimal tramo31a90, BigDecimal tramo91a180, BigDecimal tramoMas180, BigDecimal total,
                       BigDecimal reservado, BigDecimal disponible) {
    }
}
