package api.astro.whats_orders_manager.modules.facturacion.model;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSaldoFavor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Auxiliar de un saldo a favor del cliente (ADR-053, T-048): el excedente de una nota de crédito sobre el saldo pendiente
 * de su factura, en la moneda de la factura. Se acredita a «Saldos a favor de clientes» (2.1.05.00x) y se aplica
 * manualmente, en montos parciales, a otra factura del mismo cliente y moneda. El saldo vivo nunca es negativo.
 */
@Entity
@Table(name = "saldos_favor_cliente", indexes = {
    @Index(name = "idx_saldo_favor_cliente_moneda_estado", columnList = "cliente_id, moneda, estado")
}, uniqueConstraints = @UniqueConstraint(name = "uk_saldo_favor_nota", columnNames = "nota_factura_id"))
@Check(name = "ck_saldo_favor_saldo_vivo", constraints = "saldo_vivo >= 0")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class SaldoFavorCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** Código ISO de la moneda de la factura (CRC, USD). */
    @Column(name = "moneda", nullable = false, length = 3)
    private String moneda;

    @Column(name = "monto_original", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoOriginal;

    /** Tipo de cambio de origen: el de la nota (el de la factura). 1 en CRC. */
    @Column(name = "tipo_cambio_origen", nullable = false, precision = 18, scale = 5)
    private BigDecimal tipoCambioOrigen;

    /** Fecha contable de la nota que originó el saldo. */
    @Column(name = "fecha_origen", nullable = false)
    private LocalDate fechaOrigen;

    @Column(name = "saldo_vivo", nullable = false, precision = 12, scale = 2)
    @ToString.Include
    private BigDecimal saldoVivo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nota_factura_id", nullable = false)
    private NotaFactura nota;

    /** Clave de 50 dígitos del comprobante de la nota. */
    @Column(name = "clave_nota", nullable = false, length = 50)
    private String claveNota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_origen_id", nullable = false)
    private Factura facturaOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSaldoFavor estado = EstadoSaldoFavor.VIGENTE;

    @CreatedBy
    @Column(name = "creado_por", updatable = false)
    private Integer creadoPor;

    @CreatedDate
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @LastModifiedBy
    @Column(name = "modificado_por")
    private Integer modificadoPor;

    @LastModifiedDate
    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    /**
     * Baja el saldo vivo en {@code monto} (AGOTADO al llegar a 0).
     *
     * @throws IllegalStateException si el monto no es positivo o excede el saldo vivo (el saldo nunca queda negativo)
     */
    public void consumir(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalStateException("El monto a consumir debe ser mayor a 0");
        }
        if (monto.compareTo(saldoVivo) > 0) {
            throw new IllegalStateException("El monto excede el saldo vivo del saldo a favor");
        }
        saldoVivo = saldoVivo.subtract(monto);
        estado = saldoVivo.signum() == 0 ? EstadoSaldoFavor.AGOTADO : EstadoSaldoFavor.VIGENTE;
    }

    /**
     * Devuelve {@code monto} al saldo vivo (reversión de una aplicación); vuelve a VIGENTE.
     *
     * @throws IllegalStateException si el saldo vivo superaría el monto original
     */
    public void restituir(BigDecimal monto) {
        BigDecimal nuevo = saldoVivo.add(monto);
        if (nuevo.compareTo(montoOriginal) > 0) {
            throw new IllegalStateException("El saldo vivo no puede superar el monto original del saldo a favor");
        }
        saldoVivo = nuevo;
        estado = saldoVivo.signum() > 0 ? EstadoSaldoFavor.VIGENTE : EstadoSaldoFavor.AGOTADO;
    }
}
