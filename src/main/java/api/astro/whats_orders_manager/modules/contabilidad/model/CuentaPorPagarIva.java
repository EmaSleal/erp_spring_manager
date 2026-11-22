package api.astro.whats_orders_manager.modules.contabilidad.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Fila del desglose de IVA de una cuenta por pagar (T-052, ADR-066): base e IVA de una tarifa del comprobante del
 * proveedor, en la moneda de la CxP. {@link #codigoTarifa} es el CodigoTarifaIVA de Hacienda v4.4; null = «sin tarifa»
 * (CxP anteriores a V53 cuya tarifa no se puede deducir, ADR-024).
 */
@Entity
@Table(name = "cuenta_pagar_iva", indexes = {
    @Index(name = "idx_cpi_cuenta", columnList = "cuenta_por_pagar_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorPagarIva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_por_pagar_id", nullable = false)
    private CuentaPorPagar cuentaPorPagar;

    @Column(name = "codigo_tarifa", length = 2)
    private String codigoTarifa;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal base;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal iva;
}
