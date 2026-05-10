package api.astro.whats_orders_manager.modules.producto.model;

import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
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
    @Column(name = "descripcion")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "idPresentacion", referencedColumnName = "idPresentacion")
    private Presentacion presentacion;

    @Column(name = "precioInstitucional")
    private BigDecimal precioInstitucional;
    @Column(name = "precioMayorista")
    private BigDecimal precioMayorista;
    @Column(name = "active")
    private Boolean active;

    // ========== CAMPOS PARA FACTURACIÓN ELECTRÓNICA COSTA RICA ==========
    
    /**
     * Código CABYS de 13 dígitos (obligatorio para FE Costa Rica)
     * Ejemplo: "2132100000100"
     */
    @Column(name = "codigo_cabys", length = 13)
    private String codigoCabys;
    
    /**
     * Descripción oficial del CABYS según Hacienda
     * Esta descripción se usará en la facturación electrónica
     * Ejemplo: "Jugo de tomate concentrado"
     */
    @Column(name = "descripcion_cabys", length = 200)
    private String descripcionCabys;
    
    /**
     * Indica si el producto está gravado con impuestos (IVA)
     * true = Gravado, false = Exento
     */
    @Column(name = "gravado")
    private Boolean gravado;
    
    /**
     * Porcentaje de impuesto aplicable (IVA)
     * Ejemplo: 13 (13%)
     */
    @Column(name = "porcentaje_impuesto")
    private BigDecimal porcentajeImpuesto;
    
    /**
     * Indica si aplica otro impuesto adicional
     * Ejemplo: impuesto selectivo de consumo
     */
    @Column(name = "aplica_otro_impuesto")
    private Boolean aplicaOtroImpuesto;
    
    // ========== FIN CAMPOS FACTURACIÓN ELECTRÓNICA ==========

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