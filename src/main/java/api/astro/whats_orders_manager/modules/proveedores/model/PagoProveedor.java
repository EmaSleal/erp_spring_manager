package api.astro.whats_orders_manager.modules.proveedores.model;

import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_proveedor", indexes = {
    @Index(name = "idx_pp_numero", columnList = "numero"),
    @Index(name = "idx_pp_cuenta", columnList = "cuenta_por_pagar_id"),
    @Index(name = "idx_pp_fecha", columnList = "fecha")
})
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PagoProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_por_pagar_id", nullable = false)
    @ToString.Exclude
    private CuentaPorPagar cuentaPorPagar;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    /**
     * Moneda en que se pagó (ADR-011). Por defecto la de la cuenta por pagar.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "moneda", length = 10)
    private MonedaFE moneda;

    /**
     * Tipo de cambio de la operación (ADR-011): tasa de venta, en CRC por unidad, de la moneda
     * extranjera involucrada en la fecha del pago (1 si pago y cuenta están en CRC). Un pago en CRC
     * de una cuenta en moneda extranjera guarda la tasa de la moneda de la cuenta (no 1), con la
     * que se calculan {@link #montoAplicado} y el diferencial cambiario.
     */
    @Column(name = "tipo_cambio", precision = 18, scale = 5)
    private BigDecimal tipoCambio;

    /**
     * Monto del pago expresado en la moneda de la cuenta por pagar (ADR-011).
     * Es lo que reduce el saldo pendiente de la cuenta.
     */
    @Column(name = "monto_aplicado", precision = 19, scale = 2)
    private BigDecimal montoAplicado;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private MetodoPagoProveedor metodoPago;

    // Check number, transfer confirmation, SINPE reference, etc.
    @Column(length = 100)
    private String referencia;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoPagoProveedor estado = EstadoPagoProveedor.CONFIRMADO;

    @Column(length = 1000)
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id")
    @ToString.Exclude
    private Usuario registradoPor;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (fecha == null) fecha = LocalDate.now();
        createdAt = LocalDateTime.now();
    }
}
