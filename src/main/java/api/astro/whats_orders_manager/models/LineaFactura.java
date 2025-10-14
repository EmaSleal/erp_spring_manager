package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "linea_factura")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LineaFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idLineaFactura")
    private Integer idLineaFactura;

    //numero de la linea EN la factura, se autogenera
    @Column(name = "numeroLinea")
    private Integer numeroLinea;

    @ManyToOne
    @JoinColumn(name = "idFactura", nullable = false)
    private Factura factura;

    @ManyToOne
    @JoinColumn(name = "idProducto", nullable = false)
    private Producto producto;

    private Integer cantidad;
    @Column(name = "precioUnitario")
    private BigDecimal precioUnitario;
    @Column(name = "subtotal")
    private BigDecimal subtotal;

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
