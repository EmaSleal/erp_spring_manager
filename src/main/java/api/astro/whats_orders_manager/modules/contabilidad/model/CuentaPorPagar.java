package api.astro.whats_orders_manager.modules.contabilidad.model;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAceptacionComprobante;
import api.astro.whats_orders_manager.modules.proveedores.enums.RegimenHacienda;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cuentas_por_pagar", indexes = {
    @Index(name = "idx_cpp_numero", columnList = "numero"),
    @Index(name = "idx_cpp_proveedor", columnList = "proveedor_id"),
    @Index(name = "idx_cpp_estado", columnList = "estado"),
    @Index(name = "idx_cpp_vencimiento", columnList = "fecha_vencimiento")
}, uniqueConstraints = {
    // T-052 (ADR-066): un proveedor no repite el comprobante en CxP no anuladas. comprobante_normalizado es null en las
    // anuladas y en las sin comprobante, y los null no chocan entre sí en un índice único.
    @UniqueConstraint(name = "uk_cpp_proveedor_comprobante", columnNames = {"proveedor_id", "comprobante_normalizado"})
})
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CuentaPorPagar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    @ToString.Exclude
    private Proveedor proveedor;

    // Optional link — null when created manually without a purchase order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_compra_id")
    @ToString.Exclude
    private OrdenCompra ordenCompra;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_pendiente", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoPendiente;

    /**
     * Desglose de una CxP manual (ADR-016): monto antes de IVA, en {@link #moneda}.
     * Null en las CxP con orden de compra (el desglose sale de la OC) y en las anteriores a V28.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal;

    /** IVA (crédito fiscal) de una CxP manual (ADR-016), en {@link #moneda}; 0 si es exenta. */
    @Column(precision = 15, scale = 2)
    private BigDecimal impuesto;

    /**
     * Contrapartida única de la CxP manual anterior a T-054 (ADR-016).
     *
     * @deprecated En desuso desde T-054 (ADR-067): cada línea de la CxP lleva su propia cuenta
     *             ({@link CuentaPorPagarLinea#getCuentaContable()}). La columna se conserva, nullable, y las CxP nuevas no la
     *             llenan; no se usa en ningún asiento.
     */
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_gasto_id")
    @ToString.Exclude
    @JsonIgnore
    private CuentaContable cuentaGasto;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private MonedaFE moneda = MonedaFE.CRC;

    /**
     * Tipo de cambio de la cuenta (ADR-011): tasa de venta, en CRC por unidad, de {@link #moneda}.
     * Hereda el de la orden de compra; si se crea manualmente se toma de la tabla tipos_cambio
     * para la fecha de emisión (editable). CRC = 1.
     */
    @Column(name = "tipo_cambio", precision = 18, scale = 5)
    private BigDecimal tipoCambio;

    /**
     * Fecha de emisión de la CxP. En una CxP manual es la fecha del comprobante del proveedor (ADR-014, T-052) y de ella
     * sale la fecha del asiento de compra.
     */
    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    /** Número o consecutivo del comprobante del proveedor (T-052, ADR-066). Null en las CxP de OC y en las anteriores a V53. */
    @Column(name = "numero_comprobante_proveedor", length = 50)
    private String numeroComprobanteProveedor;

    /** Clave numérica de 50 dígitos del comprobante del proveedor (opcional). */
    @Column(name = "clave_comprobante", length = 50)
    private String claveComprobante;

    /** Fecha de emisión del comprobante del proveedor; igual a {@link #fechaEmision} en las CxP manuales nuevas. */
    @Column(name = "fecha_comprobante")
    private LocalDate fechaComprobante;

    /**
     * Número del comprobante sin espacios y en mayúsculas, para el índice único (proveedor, comprobante). Null cuando la
     * CxP está anulada o no tiene comprobante: así no bloquea volver a registrarlo. Lo mantiene el servicio.
     */
    @Column(name = "comprobante_normalizado", length = 50)
    @JsonIgnore
    private String comprobanteNormalizado;

    /** Líneas de la CxP manual, cada una con su cuenta, tarifa, base, IVA y tratamiento (T-054, reemplaza al desglose de T-052). */
    @OneToMany(mappedBy = "cuentaPorPagar", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroLinea ASC")
    @BatchSize(size = 50)
    @Builder.Default
    @ToString.Exclude
    @JsonIgnore
    private List<CuentaPorPagarLinea> lineas = new ArrayList<>();

    /** Aceptación del comprobante con mensaje receptor (T-054, ADR-067 §4); PENDIENTE hasta que se registre. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_aceptacion", nullable = false, length = 12)
    @Builder.Default
    private EstadoAceptacionComprobante estadoAceptacion = EstadoAceptacionComprobante.PENDIENTE;

    /**
     * Régimen del proveedor al registrar la CxP (T-058, ADR-074 §2): copia de {@code proveedores.regimen_hacienda} en ese
     * momento; no cambia si el proveedor se vuelve a verificar.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_proveedor", nullable = false, length = 20)
    @Builder.Default
    private RegimenHacienda regimenProveedor = RegimenHacienda.SIN_VERIFICAR;

    /** Fecha del mensaje receptor; null mientras esté PENDIENTE. */
    @Column(name = "fecha_aceptacion")
    private LocalDate fechaAceptacion;

    /** Número del mensaje receptor; null mientras esté PENDIENTE. */
    @Column(name = "numero_mensaje_receptor", length = 50)
    private String numeroMensajeReceptor;

    /** Usuario que anuló la CxP (T-052). */
    @Column(name = "anulada_por_id")
    private Integer anuladaPorId;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion", length = 180)
    private String motivoAnulacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoCuentaPorPagar estado = EstadoCuentaPorPagar.PENDIENTE;

    @Column(length = 1000)
    private String notas;

    @OneToMany(mappedBy = "cuentaPorPagar", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<PagoProveedor> pagos = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (fechaEmision == null) fechaEmision = LocalDate.now();
        if (saldoPendiente == null) saldoPendiente = monto;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @JsonProperty("cuentaGastoId")
    public Long getCuentaGastoId() {
        return cuentaGasto != null ? cuentaGasto.getIdCuenta() : null;
    }

    public boolean isVencida() {
        return fechaVencimiento != null
            && fechaVencimiento.isBefore(LocalDate.now())
            && (estado == EstadoCuentaPorPagar.PENDIENTE || estado == EstadoCuentaPorPagar.PARCIAL);
    }
}
