package api.astro.whats_orders_manager.modules.contabilidad.enums;

/**
 * Tratamiento del IVA de una línea de cuenta por pagar (T-054, ADR-067 §3).
 * <ul>
 *   <li>{@link #PLENO}: todo el IVA es crédito fiscal acreditable.</li>
 *   <li>{@link #USO_COMUN}: acreditable = IVA × porcentaje provisional de prorrata del año del comprobante.</li>
 *   <li>{@link #LIMITADO}: acreditable = base × tarifa límite (compra gravada a una tarifa mayor cuyo crédito se limita a
 *       una tarifa reducida).</li>
 *   <li>{@link #NO_ACREDITABLE}: ningún crédito; todo el IVA es costo o gasto de la cuenta de la línea.</li>
 * </ul>
 * Lo no acreditable se suma a la cuenta de la línea en el asiento.
 */
public enum TratamientoIvaCompra {
    PLENO,
    USO_COMUN,
    LIMITADO,
    NO_ACREDITABLE
}
