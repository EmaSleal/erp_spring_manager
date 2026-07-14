package api.astro.whats_orders_manager.modules.producto.model;

import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "producto")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProducto")
    private Integer idProducto;
    @Column(name = "codigo")
    private String codigo;

    /**
     * @deprecated Transition fallback. Source of truth is ArticuloMaestro.
     *             Kept for write-through safety during migration.
     *             Read from articuloMaestro.getDescripcion() instead.
     */
    @Deprecated
    @Column(name = "descripcion")
    private String descripcion;

    // Link to the master article that owns common/FE fields.
    @ToString.Exclude
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idArticuloMaestro")
    private ArticuloMaestro articuloMaestro;

    @ManyToOne
    @JoinColumn(name = "idPresentacion", referencedColumnName = "idPresentacion")
    private Presentacion presentacion;

    @Column(name = "precioInstitucional")
    private BigDecimal precioInstitucional;
    @Column(name = "precioMayorista")
    private BigDecimal precioMayorista;

    /**
     * @deprecated Transition fallback. Source of truth is ArticuloMaestro.
     *             Kept for write-through safety during migration.
     */
    @Deprecated
    @Column(name = "active")
    private Boolean active;

    // ========== CAMPOS PARA FACTURACIÓN ELECTRÓNICA COSTA RICA ==========

    /**
     * @deprecated Transition fallback. Source of truth is ArticuloMaestro.
     *             Kept for write-through safety during migration.
     *             Código CABYS de 13 dígitos (obligatorio para FE Costa Rica).
     */
    @Deprecated
    @Column(name = "codigo_cabys", length = 13)
    private String codigoCabys;

    /**
     * @deprecated Transition fallback. Source of truth is ArticuloMaestro.
     *             Kept for write-through safety during migration.
     *             Descripción oficial del CABYS según Hacienda.
     */
    @Deprecated
    @Column(name = "descripcion_cabys", length = 200)
    private String descripcionCabys;

    /**
     * @deprecated Transition fallback. Source of truth is ArticuloMaestro.
     *             Kept for write-through safety during migration.
     *             Indica si el producto está gravado con impuestos (IVA).
     */
    @Deprecated
    @Column(name = "gravado")
    private Boolean gravado;

    /**
     * @deprecated Transition fallback. Source of truth is ArticuloMaestro.
     *             Kept for write-through safety during migration.
     *             Porcentaje de impuesto aplicable (IVA).
     */
    @Deprecated
    @Column(name = "porcentaje_impuesto")
    private BigDecimal porcentajeImpuesto;

    /**
     * @deprecated Transition fallback. Source of truth is ArticuloMaestro.
     *             Kept for write-through safety during migration.
     *             Indica si aplica otro impuesto adicional.
     */
    @Deprecated
    @Column(name = "aplica_otro_impuesto")
    private Boolean aplicaOtroImpuesto;

    // ========== FIN CAMPOS FACTURACIÓN ELECTRÓNICA ==========

    // ========== INVENTORY CONTROL FIELDS ==========

    @Column(name = "stock")
    private Integer stock = 0;

    @Column(name = "costo", precision = 19, scale = 2)
    private BigDecimal costo;

    @Column(name = "stock_minimo")
    private Integer stockMinimo = 10;

    @Column(name = "stock_bajo")
    private Integer stockBajo = 20;

    @Column(name = "stock_maximo")
    private Integer stockMaximo = 1000;

    @Column(name = "punto_reorden")
    private Integer puntoReorden = 15;

    @Column(name = "maneja_lotes")
    private Boolean manejaLotes = false;

    @Column(name = "maneja_vencimiento")
    private Boolean manejaVencimiento = false;

    @Column(name = "dias_alerta_vencimiento")
    private Integer diasAlertaVencimiento = 30;

    public boolean isStockBajo() {
        return stock != null && stockBajo != null && stock <= stockBajo;
    }

    public boolean isStockCritico() {
        return stock != null && stockMinimo != null && stock <= stockMinimo;
    }

    public boolean alcanzoPuntoReorden() {
        return stock != null && puntoReorden != null && stock <= puntoReorden;
    }

    // ========== END INVENTORY CONTROL FIELDS ==========

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

    // Getters y Setters
}