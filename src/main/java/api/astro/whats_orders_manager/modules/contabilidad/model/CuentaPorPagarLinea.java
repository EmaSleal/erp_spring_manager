package api.astro.whats_orders_manager.modules.contabilidad.model;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Línea de una cuenta por pagar manual (T-054, ADR-067 §3, P-020): la cuenta contable (hoja) a la que va el costo o gasto,
 * la tarifa, la base y el IVA del comprobante del proveedor (en la moneda de la CxP), el tratamiento del IVA y el IVA
 * acreditable y no acreditable, calculados y guardados. {@link #codigoTarifa} es el CodigoTarifaIVA de Hacienda v4.4; null
 * = «sin tarifa» (filas migradas de V53 cuya tarifa no se pudo deducir, ADR-024).
 *
 * {@link #tarifaLimite} solo aplica a LIMITADO; {@link #porcentajeProrrata} guarda el porcentaje del año usado al crear una
 * línea USO_COMUN, para que corregir después el porcentaje del año no cambie la historia.
 */
@Entity
@Table(name = "cuenta_pagar_linea", indexes = {
    @Index(name = "idx_cpl_cuenta_contable", columnList = "cuenta_contable_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_cpl_cuenta_pagar_numero", columnNames = {"cuenta_por_pagar_id", "numero_linea"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaPorPagarLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_por_pagar_id", nullable = false)
    private CuentaPorPagar cuentaPorPagar;

    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_contable_id", nullable = false)
    private CuentaContable cuentaContable;

    @Column(name = "codigo_tarifa", length = 2)
    private String codigoTarifa;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal base;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal iva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TratamientoIvaCompra tratamiento;

    /** Porcentaje (p. ej. 1 = 1 %) al que se limita el crédito; solo con LIMITADO. */
    @Column(name = "tarifa_limite", precision = 5, scale = 2)
    private BigDecimal tarifaLimite;

    /** Porcentaje de prorrata del año aplicado al crear la línea; solo con USO_COMUN. */
    @Column(name = "porcentaje_prorrata", precision = 5, scale = 2)
    private BigDecimal porcentajeProrrata;

    @Column(name = "iva_acreditable", nullable = false, precision = 15, scale = 2)
    private BigDecimal ivaAcreditable;

    @Column(name = "iva_no_acreditable", nullable = false, precision = 15, scale = 2)
    private BigDecimal ivaNoAcreditable;
}
