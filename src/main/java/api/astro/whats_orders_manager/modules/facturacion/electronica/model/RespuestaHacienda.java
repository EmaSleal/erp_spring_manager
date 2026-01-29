package api.astro.whats_orders_manager.modules.facturacion.electronica.model;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Registro de respuestas recibidas de Hacienda para un comprobante.
 * Permite trazabilidad completa de la comunicación con Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Entity
@Table(name = "respuesta_hacienda", indexes = {
    @Index(name = "idx_comprobante", columnList = "comprobante_id"),
    @Index(name = "idx_fecha", columnList = "fecha_respuesta")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaHacienda {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Comprobante al que pertenece esta respuesta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id", nullable = false)
    @NotNull(message = "El comprobante es obligatorio")
    private ComprobanteElectronico comprobante;
    
    /**
     * Fecha y hora de la respuesta.
     */
    @Column(name = "fecha_respuesta", nullable = false)
    @NotNull(message = "La fecha de respuesta es obligatoria")
    private LocalDateTime fechaRespuesta;
    
    /**
     * Código de mensaje de Hacienda.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "codigo_mensaje", nullable = false, length = 50)
    @NotNull(message = "El código de mensaje es obligatorio")
    private MensajeHacienda codigoMensaje;
    
    /**
     * Mensaje detallado de Hacienda.
     */
    @Column(name = "mensaje", columnDefinition = "TEXT")
    @NotBlank(message = "El mensaje es obligatorio")
    private String mensaje;
    
    /**
     * XML completo de la respuesta de Hacienda.
     */
    @Column(name = "xml_respuesta", columnDefinition = "TEXT")
    @Lob
    private String xmlRespuesta;
    
    /**
     * Indica si fue una respuesta exitosa.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean exitoso = false;
    
    /**
     * Detalles adicionales o errores.
     */
    @Column(columnDefinition = "TEXT")
    private String detalles;
    
    /**
     * Código de error HTTP si aplicable.
     */
    @Column(name = "codigo_http")
    private Integer codigoHttp;
    
    /**
     * Tiempo de respuesta en milisegundos.
     */
    @Column(name = "tiempo_respuesta_ms")
    private Long tiempoRespuestaMs;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Verifica si la respuesta indica que se debe reintentar.
     */
    public boolean debeReintentar() {
        return !exitoso && 
               (codigoMensaje.esErrorTecnico() || 
                codigoHttp != null && (codigoHttp >= 500 || codigoHttp == 408));
    }
}
