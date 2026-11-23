package api.astro.whats_orders_manager.modules.contabilidad.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Fila del desglose de IVA que envía el cliente al crear una cuenta por pagar (T-052). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CuentaPorPagarIvaDTO {
    /** CodigoTarifaIVA de Hacienda v4.4 (01..11). */
    private String codigoTarifa;
    private BigDecimal base;
    private BigDecimal iva;
}
