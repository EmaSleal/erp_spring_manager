package api.astro.whats_orders_manager.modules.inventario.model;

import api.astro.whats_orders_manager.modules.inventario.enums.EstadoLote;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotes_productos",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_producto_codigo_lote",
        columnNames = {"producto_id", "codigo"}
    ),
    indexes = {
        @Index(name = "idx_lote_vencimiento", columnList = "fecha_vencimiento"),
        @Index(name = "idx_lote_estado", columnList = "estado")
    }
)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoteProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    @ToString.Exclude
    private Producto producto;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(name = "fecha_fabricacion")
    private LocalDate fechaFabricacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(nullable = false)
    @Builder.Default
    private Integer cantidad = 0;

    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;

    @Column(name = "costo_unitario", precision = 19, scale = 2)
    private BigDecimal costoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoLote estado = EstadoLote.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    @ToString.Exclude
    private Proveedor proveedor;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (cantidad != null && cantidad == 0) {
            estado = EstadoLote.AGOTADO;
        }
    }

    public boolean isPorVencer(int diasAnticipacion) {
        if (fechaVencimiento == null) return false;
        LocalDate limite = LocalDate.now().plusDays(diasAnticipacion);
        return !fechaVencimiento.isAfter(limite);
    }

    public boolean isVencido() {
        return fechaVencimiento != null && fechaVencimiento.isBefore(LocalDate.now());
    }
}
