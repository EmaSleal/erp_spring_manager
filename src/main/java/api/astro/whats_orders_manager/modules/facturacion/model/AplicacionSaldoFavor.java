package api.astro.whats_orders_manager.modules.facturacion.model;

import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoAplicacionSaldoFavor;
import api.astro.whats_orders_manager.modules.facturacion.enums.OrigenTipoCambio;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aplicación manual de un saldo a favor a otra factura del mismo cliente y moneda (ADR-057, T-048). Crea un Pago
 * NOTA_CREDITO sobre la factura destino y un asiento propio; guarda el soporte del diferencial cambiario realizado
 * (ADR-055: tramos contra el tipo de cambio del día, ADR-056 §3: solo de {@code tipos_cambio} de la fecha exacta). Una
 * reversión no borra nada: marca la aplicación REVERSADA con su razón, usuario, fecha y asiento de reversión.
 *
 * <p>Los diferenciales están en CRC. Convención de signo del tramo: {@code monto × (TC día − TC del documento)}; el neto
 * es {@code diferencialCxc − diferencialSaldo} (positivo = ganancia, crédito a DIFERENCIAL_CAMBIARIO_INGRESO; negativo =
 * pérdida, débito a DIFERENCIAL_CAMBIARIO_GASTO) y coincide con la diferencia del asiento.
 */
@Entity
@Table(name = "aplicaciones_saldo_favor", indexes = {
    @Index(name = "idx_aplicacion_saldo", columnList = "saldo_favor_id"),
    @Index(name = "idx_aplicacion_factura", columnList = "factura_id"),
    @Index(name = "idx_aplicacion_pago", columnList = "pago_id")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class AplicacionSaldoFavor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saldo_favor_id", nullable = false)
    private SaldoFavorCliente saldo;

    /** Factura destino. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    /** Pago NOTA_CREDITO creado sobre la factura destino. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    /** Monto aplicado, en la moneda original del saldo. */
    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    @ToString.Include
    private BigDecimal monto;

    @Column(name = "tipo_cambio_origen", nullable = false, precision = 18, scale = 5)
    private BigDecimal tipoCambioOrigen;

    @Column(name = "tipo_cambio_factura", nullable = false, precision = 18, scale = 5)
    private BigDecimal tipoCambioFactura;

    /** Tipo de cambio del día (solo USD): fecha exacta de la aplicación. */
    @Column(name = "tc_dia_fecha")
    private LocalDate tcDiaFecha;

    @Column(name = "tc_dia_valor", precision = 18, scale = 5)
    private BigDecimal tcDiaValor;

    /** Tipo de la tasa del día: siempre VENTA. */
    @Column(name = "tc_dia_tipo", length = 10)
    private String tcDiaTipo;

    /** Fuente de la fila de tipos_cambio (BCCR, API_EXTERNA, MANUAL). */
    @Column(name = "tc_dia_fuente", length = 20)
    private String tcDiaFuente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tc_dia_origen", length = 10)
    private OrigenTipoCambio tcDiaOrigen;

    /** Tramo del saldo: {@code monto × (TC día − TC origen)}, en CRC. */
    @Column(name = "diferencial_saldo", nullable = false, precision = 14, scale = 2)
    private BigDecimal diferencialSaldo = BigDecimal.ZERO;

    /** Tramo de la CxC: {@code monto × (TC día − TC de la factura destino)}, en CRC. */
    @Column(name = "diferencial_cxc", nullable = false, precision = 14, scale = 2)
    private BigDecimal diferencialCxc = BigDecimal.ZERO;

    /** Neto (positivo = ganancia, negativo = pérdida): lo que lleva el asiento a DIFERENCIAL_CAMBIARIO_*. */
    @Column(name = "diferencial_neto", nullable = false, precision = 14, scale = 2)
    private BigDecimal diferencialNeto = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asiento_id", nullable = false)
    private AsientoContable asiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoAplicacionSaldoFavor estado = EstadoAplicacionSaldoFavor.VIGENTE;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDateTime fechaAplicacion;

    @Column(name = "razon_reversion", length = 180)
    private String razonReversion;

    @Column(name = "reversado_por")
    private Integer reversadoPor;

    @Column(name = "fecha_reversion")
    private LocalDateTime fechaReversion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asiento_reversion_id")
    private AsientoContable asientoReversion;
}
