package api.astro.whats_orders_manager.models;

import api.astro.whats_orders_manager.enums.InvoiceType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "factura")
@Getter
@Setter
@ToString

@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idFactura")
    private Integer idFactura;

    @ManyToOne
    @JoinColumn(name = "idCliente", nullable = false)
    private Cliente cliente;

    @Column(name = "numeroFactura", unique = true, length = 50)
    private String numeroFactura;

    @Column(name = "serie", length = 10)
    private String serie;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "igv", precision = 10, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "tipoFactura")
    private InvoiceType tipoFactura;

    @Column(name = "fechaPago")
    private Date fechaPago;

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
    @Column(name = "fechaEntrega")
    private Date fechaEntrega;
    @Column(name = "entregado")
    private Boolean entregado;
    @Column(name = "descripcion")
    private String descripcion;

    @Transient
    private List<LineaFactura> lineas;


}
