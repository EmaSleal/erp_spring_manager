package api.astro.whats_orders_manager.modules.facturacion.model;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Línea de una nota sobre una factura: foto de la línea de factura al emitir la nota (producto, descripción, CAByS,
 * cantidad, precio e IVA). La referencia a la línea de origen admite NULL si esa línea se borra.
 */
@Entity
@Table(name = "linea_nota_factura", indexes = {
    @Index(name = "idx_linea_nota_nota", columnList = "nota_factura_id"),
    @Index(name = "idx_linea_nota_linea_factura", columnList = "linea_factura_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"nota", "lineaFactura", "producto"})
public class LineaNotaFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nota_factura_id", nullable = false)
    private NotaFactura nota;

    /** Línea de factura de origen; NULL si se borró. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linea_factura_id")
    private LineaFactura lineaFactura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;

    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;

    @Column(name = "codigo_cabys", nullable = false, length = 13)
    private String codigoCabys;

    @Column(name = "unidad_medida", nullable = false, length = 15)
    private String unidadMedida;

    @Column(name = "cantidad", nullable = false, precision = 16, scale = 3)
    private BigDecimal cantidad;

    /** Precio unitario sin IVA. */
    @Column(name = "precio_unitario", nullable = false, precision = 18, scale = 5)
    private BigDecimal precioUnitario;

    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    /** Tarifa de IVA en porcentaje (13.00, 0.00...). */
    @Column(name = "tarifa_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal tarifaIva;

    @Column(name = "monto_iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoIva;

    /** Base imponible (sin IVA, después del descuento). */
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    public boolean gravada() {
        return tarifaIva != null && tarifaIva.signum() > 0;
    }
}
