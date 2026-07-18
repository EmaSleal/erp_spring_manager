package api.astro.whats_orders_manager.modules.contabilidad.model;

import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import jakarta.persistence.*;
import lombok.*;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private MonedaFE moneda = MonedaFE.CRC;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

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

    public boolean isVencida() {
        return fechaVencimiento != null
            && fechaVencimiento.isBefore(LocalDate.now())
            && (estado == EstadoCuentaPorPagar.PENDIENTE || estado == EstadoCuentaPorPagar.PARCIAL);
    }
}
