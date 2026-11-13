package api.astro.whats_orders_manager.modules.facturacion.model;

import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nota de crédito (o, en el futuro, de débito) sobre una factura (ADR-037 §2). Sus líneas son una foto de las líneas
 * de la factura al emitirla: editar después la factura o el producto no cambia la nota.
 *
 * El comprobante electrónico de la nota (tipo 03) apunta a la nota, no a la factura. La nota guarda la clave y la
 * fecha del comprobante ACEPTADO de la factura que referencia (InformacionReferencia), y la moneda y el tipo de cambio
 * de la factura.
 */
@Entity
@Table(name = "nota_factura", indexes = {
    @Index(name = "idx_nota_factura_factura", columnList = "factura_id"),
    @Index(name = "idx_nota_factura_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"factura", "comprobanteReferencia", "lineas"})
public class NotaFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoNotaFactura tipo;

    /** Código de la nota 9 (InformacionReferencia/Codigo). */
    @Column(name = "codigo_referencia", nullable = false, length = 2)
    private String codigoReferencia;

    /** InformacionReferencia/Razon: de 3 a 180 caracteres (apartado d del Anexo). */
    @Column(name = "razon", nullable = false, length = 180)
    private String razon;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoNotaFactura estado = EstadoNotaFactura.EN_TRAMITE;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_aplicacion")
    private LocalDateTime fechaAplicacion;

    /** Comprobante ACEPTADO de la factura que la nota referencia. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprobante_referencia_id", nullable = false)
    private ComprobanteElectronico comprobanteReferencia;

    /** InformacionReferencia/Numero: clave de 50 dígitos del comprobante referenciado. */
    @Column(name = "clave_referencia", nullable = false, length = 50)
    private String claveReferencia;

    /** InformacionReferencia/FechaEmisionIR: fecha de emisión del comprobante referenciado. */
    @Column(name = "fecha_emision_referencia", nullable = false)
    private LocalDateTime fechaEmisionReferencia;

    /** Código ISO de la moneda de la factura. */
    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "tipo_cambio", nullable = false, precision = 18, scale = 5)
    private BigDecimal tipoCambio;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(name = "impuesto", nullable = false, precision = 12, scale = 2)
    private BigDecimal impuesto;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    /** Usuario que emitió la nota. */
    @Column(name = "usuario_creador", nullable = false, length = 100)
    private String usuarioCreador;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @OneToMany(mappedBy = "nota", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroLinea ASC")
    @Builder.Default
    private List<LineaNotaFactura> lineas = new ArrayList<>();

    public CodigoReferenciaNota codigo() {
        return CodigoReferenciaNota.deCodigo(codigoReferencia)
            .orElseThrow(() -> new IllegalStateException("Código de referencia desconocido: " + codigoReferencia));
    }

    public void agregarLinea(LineaNotaFactura linea) {
        linea.setNota(this);
        lineas.add(linea);
    }
}
