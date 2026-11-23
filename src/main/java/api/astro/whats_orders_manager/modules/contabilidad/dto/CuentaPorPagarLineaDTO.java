package api.astro.whats_orders_manager.modules.contabilidad.dto;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Línea que envía el cliente al crear una cuenta por pagar manual (T-054, ADR-067 §3). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CuentaPorPagarLineaDTO {
    /** Descripción de la línea; si viene vacía se usa la de la CxP. */
    private String descripcion;
    /** Cuenta contable (hoja que acepta movimientos) a la que va el costo o gasto de la línea. */
    private Long cuentaContableId;
    /** CodigoTarifaIVA de Hacienda v4.4 (01..11). */
    private String codigoTarifa;
    private BigDecimal base;
    private BigDecimal iva;
    private TratamientoIvaCompra tratamiento;
    /** Porcentaje límite (p. ej. 1 = 1 %); obligatorio solo con LIMITADO y se ignora en los demás. */
    private BigDecimal tarifaLimite;
}
