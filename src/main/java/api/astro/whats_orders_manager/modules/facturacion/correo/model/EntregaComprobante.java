package api.astro.whats_orders_manager.modules.facturacion.correo.model;

import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * T-055 (ADR-070): cola de entregas de comprobantes por correo (outbox). Una fila AUTOMATICO nace en la misma
 * transacción en que el comprobante pasa a ACEPTADO; el job la envía. {@code claveAutomatica} vale el id del
 * comprobante si el origen es AUTOMATICO y null si es MANUAL: su índice único impide más de una entrega automática por
 * comprobante (MySQL admite varios null).
 */
@Entity
@Table(name = "entrega_comprobante",
    uniqueConstraints = @UniqueConstraint(name = "uk_entrega_clave_automatica", columnNames = "clave_automatica"),
    indexes = {
        @Index(name = "idx_entrega_estado_proximo", columnList = "estado, proximo_intento"),
        @Index(name = "idx_entrega_comprobante", columnList = "comprobante_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class EntregaComprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprobante_id", nullable = false)
    private ComprobanteElectronico comprobante;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumentoEntrega tipoDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 12)
    private OrigenEntrega origen;

    /** Usuario que pidió el reenvío (solo MANUAL). */
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "destinatario", length = 320)
    private String destinatario;

    @Column(name = "cco", length = 1000)
    private String cco;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @ToString.Include
    private EstadoEntrega estado;

    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private int intentos = 0;

    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(name = "id_proveedor", length = 100)
    private String idProveedor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "clave_automatica")
    private Long claveAutomatica;
}
