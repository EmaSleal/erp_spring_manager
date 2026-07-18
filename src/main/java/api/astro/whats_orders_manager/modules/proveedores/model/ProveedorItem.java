package api.astro.whats_orders_manager.modules.proveedores.model;

import api.astro.whats_orders_manager.modules.proveedores.enums.CategoriaProveedor;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proveedor_item", indexes = {
    @Index(name = "idx_prov_item_proveedor", columnList = "proveedor_id"),
    @Index(name = "idx_prov_item_tipo", columnList = "tipo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaProveedor tipo;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(length = 300)
    private String descripcion;
}
