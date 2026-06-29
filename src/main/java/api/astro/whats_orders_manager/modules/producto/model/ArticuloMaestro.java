package api.astro.whats_orders_manager.modules.producto.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * Master article entity.
 * Owns all common/FE fields shared across Producto variants:
 * descripcion, active, tax/CABYS fields.
 *
 * One ArticuloMaestro HAS MANY Producto variants (variant-only fields
 * such as codigo, presentacion, precioInstitucional, precioMayorista
 * remain on Producto).
 */
@Entity
@Table(name = "articulo_maestro")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ArticuloMaestro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idArticuloMaestro")
    private Integer idArticuloMaestro;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "active")
    private Boolean active;

    // ========== ELECTRONIC INVOICING FIELDS (Costa Rica FE) ==========

    /**
     * Whether the article is subject to VAT.
     */
    @Column(name = "gravado")
    private Boolean gravado;

    /**
     * VAT percentage (e.g. 13 for 13%).
     */
    @Column(name = "porcentaje_impuesto")
    private BigDecimal porcentajeImpuesto;

    /**
     * Whether an additional selective-consumption tax applies.
     */
    @Column(name = "aplica_otro_impuesto")
    private Boolean aplicaOtroImpuesto;

    /**
     * 13-digit CABYS code (Hacienda catalogue).
     */
    @Column(name = "codigo_cabys", length = 13)
    private String codigoCabys;

    /**
     * Official CABYS description from Hacienda.
     */
    @Column(name = "descripcion_cabys", length = 200)
    private String descripcionCabys;

    // ========== END ELECTRONIC INVOICING FIELDS ==========

    @CreatedDate
    @Column(name = "createDate", updatable = false)
    private Timestamp createDate;

    @CreatedDate
    @Column(name = "updateDate")
    private Timestamp updateDate;

    @CreatedBy
    @Column(name = "createBy", updatable = false)
    private Integer createBy;

    @CreatedBy
    @Column(name = "updateBy")
    private Integer updateBy;

    /**
     * Producto variants belonging to this master.
     * No cascade-delete: service layer guards deletion when variants exist.
     * Excluded from toString to prevent circular reference and LazyInitializationException.
     */
    @JsonIgnore
    @ToString.Exclude
    @OneToMany(mappedBy = "articuloMaestro", fetch = FetchType.LAZY)
    private List<Producto> variantes;
}
