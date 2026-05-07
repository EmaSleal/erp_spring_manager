package api.astro.whats_orders_manager.modules.facturacion.model;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPagoFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
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

    /**
     * Relación con los pagos aplicados a esta factura.
     * Una factura puede tener múltiples pagos parciales.
     */
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Pago> pagos = new ArrayList<>();
    
    /**
     * Relación con el comprobante electrónico de Hacienda (CR).
     * Una factura puede tener un solo comprobante electrónico.
     */
    @OneToOne(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private ComprobanteElectronico comprobanteElectronico;
    
    /**
     * Estado de pago calculado basado en los pagos aplicados.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estadoPago", length = 20)
    private EstadoPagoFactura estadoPago = EstadoPagoFactura.PENDIENTE;
    
    // ========== CAMPOS PARA FACTURACIÓN ELECTRÓNICA COSTA RICA ==========
    
    /**
     * Condición de venta según catálogo de Hacienda
     * 01-Contado, 02-Crédito, 03-Consignación, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_venta_fe", length = 50)
    private api.astro.whats_orders_manager.modules.facturacion.electronica.enums.CondicionVentaFE condicionVentaFE;
    
    /**
     * Medio de pago según catálogo de Hacienda
     * 01-Efectivo, 02-Tarjeta, 03-Cheque, 04-Transferencia, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago_fe", length = 50)
    private api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MedioPagoFE medioPagoFE;
    
    /**
     * Moneda de la factura: CRC, USD, EUR
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "moneda_fe", length = 3)
    private api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE monedaFE;
    
    /**
     * Tipo de cambio si la moneda no es CRC
     * Obligatorio cuando moneda != CRC
     */
    @Column(name = "tipo_cambio", precision = 18, scale = 5)
    private BigDecimal tipoCambio;
    
    /**
     * Plazo de crédito en días
     * Obligatorio cuando condicionVenta = CREDITO
     */
    @Column(name = "plazo_credito")
    private Integer plazoCredito;
    
    // ========== FIN CAMPOS FACTURACIÓN ELECTRÓNICA ==========
    
    /**
     * Fecha de vencimiento de pago (opcional).
     */
    @Column(name = "fechaVencimiento")
    private Date fechaVencimiento;

    @Transient
    private List<LineaFactura> lineas;
    
    // ==================== MÉTODOS DE NEGOCIO - PAGOS ====================
    
    /**
     * Calcula el total de pagos válidos aplicados a la factura.
     * Solo considera pagos en estado CONFIRMADO o CONCILIADO.
     * 
     * @return Total pagado
     */
    @Transient
    public BigDecimal calcularTotalPagado() {
        if (pagos == null || pagos.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return pagos.stream()
            .filter(Pago::esValido)
            .map(Pago::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcula el saldo pendiente de pago.
     * 
     * @return Saldo pendiente (total - pagos válidos)
     */
    @Transient
    public BigDecimal calcularSaldoPendiente() {
        BigDecimal totalPagado = calcularTotalPagado();
        return total != null ? total.subtract(totalPagado) : BigDecimal.ZERO;
    }
    
    /**
     * Actualiza el estado de pago de la factura basado en los pagos aplicados.
     * Este método debe llamarse cada vez que se registra un nuevo pago.
     */
    public void actualizarEstadoPago() {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            this.estadoPago = EstadoPagoFactura.PENDIENTE;
            return;
        }
        
        BigDecimal totalPagado = calcularTotalPagado();
        BigDecimal saldoPendiente = total.subtract(totalPagado);
        
        // Verificar si está vencida
        if (fechaVencimiento != null) {
            LocalDate hoy = LocalDate.now();
            LocalDate vencimiento = fechaVencimiento.toLocalDate();
            
            if (vencimiento.isBefore(hoy) && saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
                this.estadoPago = EstadoPagoFactura.VENCIDO;
                return;
            }
        }
        
        // Determinar estado según monto pagado
        if (saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            this.estadoPago = EstadoPagoFactura.PAGADO_TOTAL;
        } else if (totalPagado.compareTo(BigDecimal.ZERO) > 0) {
            this.estadoPago = EstadoPagoFactura.PAGADO_PARCIAL;
        } else {
            this.estadoPago = EstadoPagoFactura.PENDIENTE;
        }
    }
    
    /**
     * Agrega un pago a la factura y actualiza el estado.
     * 
     * @param pago Pago a agregar
     */
    public void agregarPago(Pago pago) {
        if (pagos == null) {
            pagos = new ArrayList<>();
        }
        pagos.add(pago);
        pago.setFactura(this);
        actualizarEstadoPago();
    }
    
    /**
     * Elimina un pago de la factura y actualiza el estado.
     * 
     * @param pago Pago a eliminar
     */
    public void eliminarPago(Pago pago) {
        if (pagos != null) {
            pagos.remove(pago);
            pago.setFactura(null);
            actualizarEstadoPago();
        }
    }
    
    /**
     * Verifica si la factura está completamente pagada.
     * 
     * @return true si el estado es PAGADO_TOTAL
     */
    @Transient
    public boolean estaPagada() {
        return estadoPago != null && estadoPago.estaPagada();
    }
    
    /**
     * Verifica si la factura tiene saldo pendiente.
     * 
     * @return true si tiene saldo pendiente
     */
    @Transient
    public boolean tieneSaldoPendiente() {
        return estadoPago != null && estadoPago.tieneSaldoPendiente();
    }


    //metodo getFechaEmision
    public LocalDate getFechaEmision() {
        return this.createDate != null ? this.createDate.toLocalDateTime().toLocalDate() : null;
    }

    //metodo getImpuesto
    public BigDecimal getImpuesto() {
        return this.igv;
    }

    public void marcarComoPagada() {
        this.estadoPago = EstadoPagoFactura.PAGADO_TOTAL;
    }
}
