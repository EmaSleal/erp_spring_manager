package api.astro.whats_orders_manager.modules.proveedores.model;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_ordenes_compra", indexes = {
    @Index(name = "idx_doc_orden", columnList = "orden_compra_id"),
    @Index(name = "idx_doc_producto", columnList = "producto_id")
})
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DetalleOrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_compra_id", nullable = false)
    @ToString.Exclude
    private OrdenCompra ordenCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    @ToString.Exclude
    private Producto producto;

    // Free-text description for items not in the product catalog
    @Column(length = 300)
    private String descripcion;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "cantidad_recibida", nullable = false)
    @Builder.Default
    private Integer cantidadRecibida = 0;

    @Column(name = "precio_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @PrePersist
    @PreUpdate
    protected void calcularTotal() {
        if (cantidad != null && precioUnitario != null) {
            total = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    public boolean isRecibidoCompleto() {
        return cantidad != null && cantidadRecibida != null && cantidadRecibida >= cantidad;
    }
}
