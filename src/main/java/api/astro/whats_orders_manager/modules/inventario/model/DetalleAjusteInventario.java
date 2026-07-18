package api.astro.whats_orders_manager.modules.inventario.model;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalles_ajuste_inventario", indexes = {
    @Index(name = "idx_detalle_ajuste", columnList = "ajuste_id"),
    @Index(name = "idx_detalle_producto", columnList = "producto_id")
})
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetalleAjusteInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ajuste_id", nullable = false)
    @ToString.Exclude
    private AjusteInventario ajuste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    @ToString.Exclude
    private Producto producto;

    /** Physical count during the adjustment */
    @Column(name = "cantidad_fisica", nullable = false)
    private Integer cantidadFisica;

    /** System stock before the adjustment */
    @Column(name = "cantidad_sistema", nullable = false)
    private Integer cantidadSistema;

    /** cantidadFisica - cantidadSistema; positive = surplus, negative = deficit */
    @Column(nullable = false)
    private Integer diferencia;

    @Column(name = "costo_unitario", precision = 19, scale = 2)
    private BigDecimal costoUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    @ToString.Exclude
    private LoteProducto lote;

    @PrePersist
    @PreUpdate
    protected void calcularDiferencia() {
        if (cantidadFisica != null && cantidadSistema != null) {
            diferencia = cantidadFisica - cantidadSistema;
        }
    }
}
