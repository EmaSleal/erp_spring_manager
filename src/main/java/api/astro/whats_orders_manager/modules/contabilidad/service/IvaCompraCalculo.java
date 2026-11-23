package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo del IVA acreditable y no acreditable de una línea de cuenta por pagar (T-054, ADR-067 §3). Redondeo a 2
 * decimales con HALF_UP.
 * <ul>
 *   <li>PLENO: el IVA.</li>
 *   <li>USO_COMUN: IVA × porcentaje de prorrata del año / 100.</li>
 *   <li>LIMITADO: base × tarifa límite / 100, sin pasar del IVA de la línea.</li>
 *   <li>NO_ACREDITABLE: 0.</li>
 * </ul>
 * El no acreditable es IVA − acreditable.
 */
public final class IvaCompraCalculo {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private IvaCompraCalculo() {
    }

    /** Acreditable y no acreditable de una línea, ambos con 2 decimales. */
    public record Resultado(BigDecimal acreditable, BigDecimal noAcreditable) {}

    /**
     * @param base            base imponible de la línea
     * @param iva             IVA de la línea
     * @param tratamiento     tratamiento del IVA
     * @param tarifaLimite    porcentaje límite, obligatorio solo con LIMITADO
     * @param porcentajeProrrata porcentaje de prorrata del año, obligatorio solo con USO_COMUN
     * @throws IllegalArgumentException si falta un dato que el tratamiento exige (los valida el servicio antes)
     */
    public static Resultado calcular(BigDecimal base, BigDecimal iva, TratamientoIvaCompra tratamiento,
                                     BigDecimal tarifaLimite, BigDecimal porcentajeProrrata) {
        BigDecimal ivaLinea = iva.setScale(2, RoundingMode.HALF_UP);
        BigDecimal acreditable;
        switch (tratamiento) {
            case PLENO -> acreditable = ivaLinea;
            case USO_COMUN -> {
                if (porcentajeProrrata == null) {
                    throw new IllegalArgumentException("El tratamiento USO_COMUN requiere el porcentaje de prorrata");
                }
                acreditable = ivaLinea.multiply(porcentajeProrrata).divide(CIEN, 2, RoundingMode.HALF_UP);
            }
            case LIMITADO -> {
                if (tarifaLimite == null) {
                    throw new IllegalArgumentException("El tratamiento LIMITADO requiere la tarifa límite");
                }
                acreditable = base.multiply(tarifaLimite).divide(CIEN, 2, RoundingMode.HALF_UP).min(ivaLinea);
            }
            case NO_ACREDITABLE -> acreditable = BigDecimal.ZERO.setScale(2);
            default -> throw new IllegalArgumentException("Tratamiento de IVA desconocido: " + tratamiento);
        }
        return new Resultado(acreditable, ivaLinea.subtract(acreditable));
    }
}
