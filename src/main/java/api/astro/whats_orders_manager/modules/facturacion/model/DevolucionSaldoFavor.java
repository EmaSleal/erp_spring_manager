package api.astro.whats_orders_manager.modules.facturacion.model;

import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoDevolucionSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.enums.MedioSolicitudDevolucion;
import api.astro.whats_orders_manager.modules.facturacion.enums.OrigenTipoCambio;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Devolución en dinero, total o parcial, de un saldo a favor del cliente (T-049, ADR-058, fase B2 de ADR-053).
 *
 * <p>Ciclo: SOLICITADA (reserva el monto) → APROBADA → PAGADA (único estado con asiento), más RECHAZADA y ANULADA, que
 * liberan la reserva. Cada transición guarda su usuario y fecha; nada se borra. El comprobante bancario es solo número y
 * fecha: el sistema no tiene almacenamiento de adjuntos. Los diferenciales están en CRC (ver
 * {@code CalculoDevolucionSaldoFavor}): {@code diferencialSaldo = monto × (TC día − TC origen)} y
 * {@code diferencialNeto = débito − crédito} del asiento (positivo = ganancia). Solo hay TC del día en USD.
 */
@Entity
@Table(name = "devoluciones_saldo_favor", indexes = {
    @Index(name = "idx_devolucion_saldo", columnList = "saldo_favor_id, estado")
})
@Check(name = "ck_devolucion_monto", constraints = "monto > 0")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class DevolucionSaldoFavor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saldo_favor_id", nullable = false)
    private SaldoFavorCliente saldo;

    /** Monto a devolver, en la moneda original del saldo. */
    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    @ToString.Include
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @ToString.Include
    private EstadoDevolucionSaldoFavor estado = EstadoDevolucionSaldoFavor.SOLICITADA;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_solicitud", nullable = false, length = 20)
    private MedioSolicitudDevolucion medioSolicitud;

    /** Fecha en que el cliente hizo la solicitud (no futura). */
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate fechaSolicitud;

    /** IBAN del cliente, normalizado (sin espacios, mayúsculas). */
    @Column(name = "iban", nullable = false, length = 34)
    private String iban;

    @Column(name = "nombre_titular", nullable = false, length = 100)
    private String nombreTitular;

    /** Identificación del titular, normalizada (solo letras y dígitos); igual a la del cliente del saldo. */
    @Column(name = "identificacion_titular", nullable = false, length = 20)
    private String identificacionTitular;

    @Column(name = "solicitado_por", nullable = false)
    private Integer solicitadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "aprobado_por")
    private Integer aprobadoPor;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    /** Declaración del aprobador: «Verifiqué que la cuenta pertenece al cliente». */
    @Column(name = "verifico_cuenta", nullable = false)
    private boolean verificoCuenta;

    @Column(name = "fecha_verificacion")
    private LocalDateTime fechaVerificacion;

    @Column(name = "rechazado_por")
    private Integer rechazadoPor;

    @Column(name = "fecha_rechazo")
    private LocalDateTime fechaRechazo;

    @Column(name = "anulado_por")
    private Integer anuladoPor;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    /** Razón del rechazo o de la anulación (3 a 180 caracteres). */
    @Column(name = "razon", length = 180)
    private String razon;

    @Column(name = "pagado_por")
    private Integer pagadoPor;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    /** Cuenta bancaria de la empresa de la que sale el dinero (ADR-018). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_bancaria_id")
    private CuentaBancaria cuentaBancaria;

    @Column(name = "numero_comprobante", length = 50)
    private String numeroComprobante;

    @Column(name = "fecha_comprobante")
    private LocalDate fechaComprobante;

    @Column(name = "tipo_cambio_origen", precision = 18, scale = 5)
    private BigDecimal tipoCambioOrigen;

    /** TC del día del pago (solo USD): fecha exacta, tasa de VENTA (ADR-056 §3). */
    @Column(name = "tc_dia_fecha")
    private LocalDate tcDiaFecha;

    @Column(name = "tc_dia_valor", precision = 18, scale = 5)
    private BigDecimal tcDiaValor;

    @Column(name = "tc_dia_tipo", length = 10)
    private String tcDiaTipo;

    @Column(name = "tc_dia_fuente", length = 20)
    private String tcDiaFuente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tc_dia_origen", length = 10)
    private OrigenTipoCambio tcDiaOrigen;

    /** Tramo del saldo: {@code monto × (TC día − TC origen)}, en CRC. */
    @Column(name = "diferencial_saldo", precision = 14, scale = 2)
    private BigDecimal diferencialSaldo;

    /** Neto = débito − crédito del asiento (positivo = ganancia, negativo = pérdida). */
    @Column(name = "diferencial_neto", precision = 14, scale = 2)
    private BigDecimal diferencialNeto;

    /** Asiento del pago: la única fila contable de la devolución. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asiento_id")
    private AsientoContable asiento;
}
