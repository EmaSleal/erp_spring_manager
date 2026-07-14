package api.astro.whats_orders_manager.modules.inventario.model;

import api.astro.whats_orders_manager.modules.inventario.enums.TipoMovimientoInventario;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario", indexes = {
    @Index(name = "idx_mov_producto", columnList = "producto_id"),
    @Index(name = "idx_mov_fecha", columnList = "fecha"),
    @Index(name = "idx_mov_tipo", columnList = "tipo"),
    @Index(name = "idx_mov_documento", columnList = "documento_origen, documento_origen_id")
})
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    @ToString.Exclude
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimientoInventario tipo;

    /** Positive for entries, negative for exits */
    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "costo_unitario", precision = 19, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "costo_total", precision = 19, scale = 2)
    private BigDecimal costoTotal;

    @Column(name = "stock_anterior", nullable = false)
    private Integer stockAnterior;

    @Column(name = "stock_nuevo", nullable = false)
    private Integer stockNuevo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "documento_origen", length = 50)
    private String documentoOrigen;

    @Column(name = "documento_origen_id")
    private Long documentoOrigenId;

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    @ToString.Exclude
    private LoteProducto lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @ToString.Exclude
    private Usuario usuario;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        if (costoUnitario != null && cantidad != null) {
            costoTotal = costoUnitario.multiply(BigDecimal.valueOf(Math.abs(cantidad)));
        }
    }
}
