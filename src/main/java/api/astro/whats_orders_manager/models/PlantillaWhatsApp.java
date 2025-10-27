package api.astro.whats_orders_manager.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Entidad que representa una plantilla de WhatsApp aprobada por Meta.
 * Las plantillas deben ser pre-aprobadas antes de poder usarse.
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - 26 octubre 2025
 */
@Entity
@Table(name = "plantilla_whatsapp",
       indexes = {
           @Index(name = "idx_nombre", columnList = "nombre"),
           @Index(name = "idx_codigo_meta", columnList = "codigoMeta"),
           @Index(name = "idx_estado", columnList = "estadoMeta"),
           @Index(name = "idx_activo", columnList = "activo")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PlantillaWhatsApp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plantilla")
    private Integer idPlantilla;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede tener más de 50 caracteres")
    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;
    
    @NotBlank(message = "El código Meta es obligatorio")
    @Size(max = 50, message = "El código Meta no puede tener más de 50 caracteres")
    @Column(name = "codigo_meta", nullable = false, length = 50)
    private String codigoMeta;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    @Builder.Default
    private CategoriaPlantilla categoria = CategoriaPlantilla.UTILITY;
    
    @Column(name = "idioma", length = 10, nullable = false)
    @Builder.Default
    private String idioma = "es_MX";
    
    @NotBlank(message = "El contenido es obligatorio")
    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;
    
    @Column(name = "parametros", columnDefinition = "JSON")
    private String parametros;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_meta", nullable = false)
    @Builder.Default
    private EstadoMeta estadoMeta = EstadoMeta.PENDING;
    
    @Column(name = "template_id", length = 50)
    private String templateId;
    
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;
    
    
    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;
    
    // ========================================
    // CAMPOS DE AUDITORÍA
    // ========================================
    
    @CreatedDate
    @Column(name = "createDate", updatable = false)
    private Timestamp createDate;

    @LastModifiedDate
    @Column(name = "updateDate")
    private Timestamp updateDate;

    @CreatedBy
    @Column(name = "createBy", updatable = false)
    private Integer createBy;

    @LastModifiedBy
    @Column(name = "updateBy")
    private Integer updateBy;
    
    // ========================================
    // LIFECYCLE HOOKS
    // ========================================
    
    @PreUpdate
    protected void onUpdate() {
        updateDate = new Timestamp(System.currentTimeMillis());
    }
    
    // ========================================
    // ENUMS
    // ========================================
    
    /**
     * Enum para la categoría de la plantilla según Meta
     */
    public enum CategoriaPlantilla {
        /** Mensajes transaccionales (facturas, notificaciones) */
        UTILITY,
        /** Mensajes promocionales (ofertas, marketing) */
        MARKETING,
        /** Mensajes de autenticación (códigos OTP) */
        AUTHENTICATION
    }
    
    /**
     * Enum para el estado de aprobación en Meta
     */
    public enum EstadoMeta {
        /** Pendiente de aprobación */
        PENDING,
        /** Aprobada por Meta */
        APPROVED,
        /** Rechazada por Meta */
        REJECTED
    }
    
    /**
     * Verifica si la plantilla está lista para usarse
     */
    public boolean estaListaParaUsar() {
        return activo && estadoMeta == EstadoMeta.APPROVED;
    }
    
    /**
     * Verifica si la plantilla está aprobada
     */
    public boolean estaAprobada() {
        return estadoMeta == EstadoMeta.APPROVED;
    }
    
    /**
     * Marca la plantilla como aprobada
     */
    public void marcarComoAprobada() {
        this.estadoMeta = EstadoMeta.APPROVED;
        this.fechaAprobacion = LocalDateTime.now();
    }
    
    /**
     * Activa la plantilla
     */
    public void activar() {
        this.activo = true;
    }
    
    /**
     * Desactiva la plantilla
     */
    public void desactivar() {
        this.activo = false;
    }
}
