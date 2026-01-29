package api.astro.whats_orders_manager.modules.facturacion.electronica.model;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Representa un comprobante electrónico enviado a Hacienda.
 * Almacena el XML generado, firma digital y respuestas de Hacienda.
 * 
 * @author Sistema ERP
 * @version 1.0
 * @since Sprint 5 - Fase 3
 */
@Entity
@Table(name = "comprobante_electronico", indexes = {
    @Index(name = "idx_clave_numerica", columnList = "clave_numerica", unique = true),
    @Index(name = "idx_factura", columnList = "factura_id"),
    @Index(name = "idx_estado", columnList = "estado"),
    @Index(name = "idx_fecha_emision", columnList = "fecha_emision")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprobanteElectronico {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Factura asociada a este comprobante (opcional para tiquetes).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private Factura factura;
    
    /**
     * Empresa emisora del comprobante.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @NotNull(message = "La empresa es obligatoria")
    private Empresa empresa;
    
    /**
     * Tipo de comprobante electrónico (01-09).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 50)
    @NotNull(message = "El tipo de comprobante es obligatorio")
    private TipoComprobanteElectronico tipoComprobante;
    
    /**
     * Clave numérica única de 50 dígitos generada según especificación de Hacienda.
     * Formato: [País][Día][Mes][Año][Cédula][ConsecutivoComprobante][Situación][CódigoSeguridad]
     */
    @Column(name = "clave_numerica", nullable = false, unique = true, length = 50)
    @NotBlank(message = "La clave numérica es obligatoria")
    private String claveNumerica;
    
    /**
     * Consecutivo del comprobante (parte de la clave numérica).
     */
    @Column(nullable = false, length = 20)
    @NotBlank(message = "El consecutivo es obligatorio")
    private String consecutivo;
    
    /**
     * Fecha y hora de emisión del comprobante.
     */
    @Column(name = "fecha_emision", nullable = false)
    @NotNull(message = "La fecha de emisión es obligatoria")
    private LocalDateTime fechaEmision;
    
    /**
     * XML del comprobante generado y firmado.
     */
    @Column(name = "xml_comprobante", columnDefinition = "TEXT")
    @Lob
    private String xmlComprobante;
    
    /**
     * XML de respuesta de Hacienda.
     */
    @Column(name = "xml_respuesta", columnDefinition = "TEXT")
    @Lob
    private String xmlRespuesta;
    
    /**
     * Estado actual del comprobante.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "El estado es obligatorio")
    @Builder.Default
    private EstadoComprobante estado = EstadoComprobante.GENERADO;
    
    /**
     * Código de mensaje de respuesta de Hacienda.
     */
    @Column(name = "codigo_respuesta", length = 10)
    private String codigoRespuesta;
    
    /**
     * Mensaje de respuesta detallado de Hacienda.
     */
    @Column(name = "mensaje_respuesta", columnDefinition = "TEXT")
    private String mensajeRespuesta;
    
    /**
     * Fecha de envío a Hacienda.
     */
    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;
    
    /**
     * Fecha de respuesta de Hacienda.
     */
    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
    
    /**
     * Número de intentos de envío.
     */
    @Column(name = "intentos_envio")
    @Builder.Default
    private Integer intentosEnvio = 0;
    
    /**
     * Último error ocurrido durante el proceso.
     */
    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;
    
    /**
     * URL para descargar el PDF del comprobante.
     */
    @Column(name = "url_pdf", length = 500)
    private String urlPdf;
    
    /**
     * Ruta del archivo XML almacenado en filesystem.
     * Formato: /comprobantes/{año}/{mes}/{claveNumerica}.xml
     */
    @Column(name = "ruta_archivo_xml", length = 500)
    private String rutaArchivoXml;
    
    /**
     * Indica si el comprobante fue enviado por correo al cliente.
     */
    @Column(name = "enviado_email")
    @Builder.Default
    private Boolean enviadoEmail = false;
    
    /**
     * Fecha en que se envió por correo.
     */
    @Column(name = "fecha_envio_email")
    private LocalDateTime fechaEnvioEmail;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Verifica si el comprobante puede ser reenviado.
     */
    public boolean puedeReenviar() {
        return !estado.esTerminal() && intentosEnvio < 3;
    }
    
    /**
     * Verifica si el comprobante está listo para enviar a Hacienda.
     */
    public boolean listoParaEnviar() {
        return xmlComprobante != null && 
               (estado == EstadoComprobante.GENERADO || estado == EstadoComprobante.FIRMADO);
    }
    
    /**
     * Incrementa el contador de intentos de envío.
     */
    public void incrementarIntentos() {
        this.intentosEnvio++;
    }
}
