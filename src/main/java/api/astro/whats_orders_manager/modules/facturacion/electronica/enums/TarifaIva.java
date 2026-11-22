package api.astro.whats_orders_manager.modules.facturacion.electronica.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Catálogo de tarifas del IVA (CodigoTarifaIVA de Hacienda v4.4), compartido por el XML de la factura de venta y el
 * desglose de IVA de las cuentas por pagar (T-052, ADR-066).
 */
@Getter
public enum TarifaIva {
    T01("01", "0", "Tarifa 0% (artículo 32, num 1, RLIVA)"),
    T02("02", "1", "Tarifa reducida 1%"),
    T03("03", "2", "Tarifa reducida 2%"),
    T04("04", "4", "Tarifa reducida 4%"),
    T05("05", "0", "Transitorio 0%"),
    T06("06", "4", "Transitorio 4%"),
    T07("07", "8", "Transitorio 8%"),
    T08("08", "13", "Tarifa general 13%"),
    T09("09", "0.5", "Tarifa reducida 0,5%"),
    T10("10", "0", "Tarifa exenta"),
    T11("11", "0", "Tarifa 0% sin derecho a crédito");

    private final String codigo;
    private final BigDecimal porcentaje;
    private final String descripcion;

    TarifaIva(String codigo, String porcentaje, String descripcion) {
        this.codigo = codigo;
        this.porcentaje = new BigDecimal(porcentaje);
        this.descripcion = descripcion;
    }

    /** La tarifa con ese código de dos dígitos, si está en el catálogo. */
    public static Optional<TarifaIva> deCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(t -> t.codigo.equals(codigo)).findFirst();
    }

    /**
     * Código de tarifa para el porcentaje de una línea de venta: 0 o nulo→10 (exenta), 0,5→09, 1→02, 2→03, 4→04,
     * 8→07 y cualquier otro→08 (tarifa general 13 %). Es la tabla que usaba el generador del XML.
     */
    public static String codigoPorPorcentaje(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) == 0) return T10.codigo;
        if (porcentaje.compareTo(new BigDecimal("0.5")) == 0) return T09.codigo;
        if (porcentaje.compareTo(BigDecimal.ONE) == 0) return T02.codigo;
        if (porcentaje.compareTo(new BigDecimal("2")) == 0) return T03.codigo;
        if (porcentaje.compareTo(new BigDecimal("4")) == 0) return T04.codigo;
        if (porcentaje.compareTo(new BigDecimal("8")) == 0) return T07.codigo;
        return T08.codigo;
    }
}
