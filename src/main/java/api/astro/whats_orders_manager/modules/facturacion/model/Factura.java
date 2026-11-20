package api.astro.whats_orders_manager.modules.facturacion.model;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPagoFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private LocalDateTime createDate;

    @CreatedDate
    @Column(name = "updateDate")
    private LocalDateTime updateDate;

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
     * Factura anulada que esta factura sustituye (ADR-045 §1); null si no es una sustituta. Lo fija el servidor: no
     * se serializa ni se recibe del cliente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sustituye_factura_id")
    @JsonIgnore
    @ToString.Exclude
    private Factura facturaSustituida;

    /** Código de referencia de la sustituta (nota 9): 07 si la original se anuló con la nota 01, 15 si con la 13. */
    @Column(name = "codigo_sustitucion", length = 2)
    @JsonIgnore
    private String codigoSustitucion;

    /** Fecha del asiento de venta de la sustituta (ADR-050 §1); obligatoria cuando es sustituta, null en el resto. */
    @Column(name = "fecha_contable")
    @JsonIgnore
    private LocalDate fechaContable;

    /** Mes (AAAA-MM) al que se atribuye el IVA de la sustituta (ADR-050 §2); obligatorio cuando es sustituta. */
    @Column(name = "periodo_fiscal_atribucion", length = 7, columnDefinition = "CHAR(7)")
    @JsonIgnore
    private String periodoFiscalAtribucion;

    /** Razón de la InformacionReferencia 07/15 de la sustituta (3 a 180 caracteres). */
    @Column(name = "razon_sustitucion", length = 180)
    @JsonIgnore
    private String razonSustitucion;

    /**
     * Comprobantes electrónicos de la factura (ADR-030 §4): N comprobantes, uno solo vigente (un RECHAZADO queda
     * como historial al reemitir). Sin cascade: eliminar la factura nunca borra sus comprobantes (§5).
     */
    @OneToMany(mappedBy = "factura", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    @Setter(AccessLevel.NONE)
    private List<ComprobanteElectronico> comprobantes = new ArrayList<>();

    /**
     * Comprobante vigente de la factura, o null si no tiene (vistas Thymeleaf y PDF). No se serializa a JSON: la
     * entidad lleva relaciones perezosas (empresa) y la API usa DTOs para el detalle.
     */
    @JsonIgnore
    public ComprobanteElectronico getComprobanteElectronico() {
        return comprobantes.stream()
                .filter(c -> Boolean.TRUE.equals(c.getVigente()))
                .findFirst()
                .orElse(null);
    }
    
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

    // ========== ANULACIÓN (ADR-008) ==========

    /**
     * Indica si la factura fue anulada. Una factura con asiento contabilizado
     * no se borra: se anula y su asiento de venta se revierte.
     */
    @Column(name = "anulada", nullable = false)
    private Boolean anulada = false;

    /**
     * Fecha y hora de la anulación.
     */
    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo de la anulación.
     */
    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;

    // ========== NOTAS DE CRÉDITO PARCIALES (ADR-051, T-047) ==========

    /**
     * Suma de las notas de crédito parciales APLICADAS, en la moneda de la factura con IVA incluido. Lo actualiza
     * NotaFacturaEfectos en la misma transacción en que la nota pasa a APLICADA. Las notas totales no lo tocan: anulan
     * la factura.
     */
    @Column(name = "monto_acreditado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoAcreditado = BigDecimal.ZERO;

    /** La suma de las notas parciales APLICADAS alcanzó el TotalComprobante: saldo 0, sin anular, sin más notas ni pagos. */
    @Column(name = "acreditada_totalmente", nullable = false)
    private Boolean acreditadaTotalmente = false;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @ToString.Exclude
    private List<LineaFactura> lineas = new ArrayList<>();
    
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
        
        // ADR-011: cada pago se aplica en la moneda de la factura (montoAplicado)
        return pagos.stream()
            .filter(Pago::esValido)
            .map(Pago::getMontoAplicadoEfectivo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcula el saldo pendiente de pago.
     * 
     * @return Saldo pendiente (total - pagos válidos)
     */
    @Transient
    public BigDecimal calcularSaldoPendiente() {
        if (estaAcreditadaTotalmente()) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalPagado = calcularTotalPagado();
        return total != null ? total.subtract(calcularTotalAcreditado()).subtract(totalPagado) : BigDecimal.ZERO;
    }

    /** Total acreditado por notas parciales APLICADAS (0 si no hay). */
    @Transient
    public BigDecimal calcularTotalAcreditado() {
        return montoAcreditado != null ? montoAcreditado : BigDecimal.ZERO;
    }

    @Transient
    public boolean estaAcreditadaTotalmente() {
        return Boolean.TRUE.equals(acreditadaTotalmente);
    }

    /**
     * T-047: saldo pendiente convertido a CRC con el tipo de cambio de la factura (mismo criterio que
     * {@link #getTotalEnCRC()}). Una factura anulada no tiene saldo; una acreditada totalmente tampoco
     * ({@link #calcularSaldoPendiente()}). Es el cálculo que usan los reportes.
     */
    @Transient
    public BigDecimal getSaldoPendienteEnCRC() {
        if (estaAnulada()) {
            return BigDecimal.ZERO;
        }
        BigDecimal saldo = calcularSaldoPendiente();
        if (monedaFE == null || monedaFE == api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE.CRC) {
            return saldo;
        }
        BigDecimal tc = (tipoCambio != null && tipoCambio.compareTo(BigDecimal.ZERO) > 0) ? tipoCambio : BigDecimal.ONE;
        return saldo.multiply(tc).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * T-047: la factura admite recordatorio de cobro: no está anulada ni acreditada totalmente y su saldo pendiente
     * (total - acreditado - pagado) es mayor que cero.
     */
    @Transient
    public boolean admiteRecordatorioDePago() {
        return !estaAnulada() && !estaAcreditadaTotalmente() && calcularSaldoPendiente().signum() > 0;
    }

    // Returns the invoice total converted to CRC for consolidated report aggregation.
    // Uses tipoCambio stored at invoice creation time; falls back to 1 if absent.
    @Transient
    public BigDecimal getTotalEnCRC() {
        BigDecimal t = total != null ? total : BigDecimal.ZERO;
        if (monedaFE == null || monedaFE == api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE.CRC) {
            return t;
        }
        BigDecimal tc = (tipoCambio != null && tipoCambio.compareTo(BigDecimal.ZERO) > 0) ? tipoCambio : BigDecimal.ONE;
        return t.multiply(tc).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Actualiza el estado de pago de la factura basado en los pagos aplicados.
     * Este método debe llamarse cada vez que se registra un nuevo pago.
     */
    public void actualizarEstadoPago() {
        if (estaAcreditadaTotalmente()) {
            return; // sin saldo por notas de crédito: no hay estado de pago que calcular
        }
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            this.estadoPago = EstadoPagoFactura.PENDIENTE;
            return;
        }
        
        BigDecimal totalPagado = calcularTotalPagado();
        BigDecimal saldoPendiente = total.subtract(calcularTotalAcreditado()).subtract(totalPagado);
        
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
        return this.createDate != null ? this.createDate.toLocalDate() : null;
    }

    //metodo getImpuesto
    public BigDecimal getImpuesto() {
        return this.igv;
    }

    /**
     * Verifica si la factura está anulada.
     * @return true si fue anulada
     */
    @Transient
    public boolean estaAnulada() {
        return Boolean.TRUE.equals(anulada);
    }

    public void marcarComoPagada() {
        this.estadoPago = EstadoPagoFactura.PAGADO_TOTAL;
    }
}
