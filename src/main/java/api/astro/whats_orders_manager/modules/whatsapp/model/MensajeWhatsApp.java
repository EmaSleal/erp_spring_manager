package api.astro.whats_orders_manager.modules.whatsapp.model;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
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

import java.time.LocalDateTime;

/**
 * Entidad que representa un mensaje de WhatsApp en el sistema.
 * Almacena tanto mensajes enviados como recibidos.
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - 26 octubre 2025
 */
@Entity
@Table(name = "mensaje_whatsapp", 
       indexes = {
           @Index(name = "idx_telefono_fecha", columnList = "telefono, fechaEnvio"),
           @Index(name = "idx_usuario", columnList = "idUsuario"),
           @Index(name = "idx_estado", columnList = "estado"),
           @Index(name = "idx_tipo", columnList = "tipo"),
           @Index(name = "idx_mensaje_whatsapp", columnList = "idMensajeWhatsapp"),
           @Index(name = "idx_fecha_envio", columnList = "fechaEnvio")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MensajeWhatsApp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long idMensaje;
    
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no puede tener más de 20 caracteres")
    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;
    
    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoMensaje tipo;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoMensaje estado = EstadoMensaje.PENDIENTE;
    
    // Relación con Usuario (el chat está ligado al usuario, no a una factura específica)
    @Column(name = "id_usuario")
    private Integer idUsuario;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuario usuario;
    
    @Column(name = "id_mensaje_whatsapp", length = 255)
    private String idMensajeWhatsapp;
    
    @Column(name = "nombre_plantilla", length = 50)
    private String nombrePlantilla;
    
    @Column(name = "fecha_envio", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEnvio = LocalDateTime.now();
    
    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;
    
    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;
    
    @Column(name = "error", columnDefinition = "TEXT")
    private String error;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    // ========================================
    // CAMPOS DE AUDITORÍA
    // ========================================
    
    @CreatedDate
    @Column(name = "createDate", updatable = false)
    private LocalDateTime createDate;

    @LastModifiedDate
    @Column(name = "updateDate")
    private LocalDateTime updateDate;

    @CreatedBy
    @Column(name = "createBy", updatable = false)
    private Integer createBy;

    @LastModifiedBy
    @Column(name = "updateBy")
    private Integer updateBy;
    
    // ========================================
    // ENUMS
    // ========================================
    
    /**
     * Enum para el tipo de mensaje
     */
    public enum TipoMensaje {
        /** Mensaje enviado por el sistema */
        ENVIADO,
        /** Mensaje recibido del cliente */
        RECIBIDO
    }
    
    /**
     * Enum para el estado del mensaje (ciclo de vida)
     */
    public enum EstadoMensaje {
        /** Mensaje en cola, aún no enviado */
        PENDIENTE,
        /** Mensaje enviado a Meta API */
        ENVIADO,
        /** Mensaje entregado al destinatario (confirmado por webhook) */
        ENTREGADO,
        /** Mensaje leído por el destinatario (confirmado por webhook) */
        LEIDO,
        /** Mensaje falló al enviar */
        FALLIDO
    }
    
    /**
     * Verifica si el mensaje fue exitoso
     */
    public boolean esExitoso() {
        return estado == EstadoMensaje.ENTREGADO || estado == EstadoMensaje.LEIDO;
    }
    
    /**
     * Verifica si el mensaje está relacionado con un usuario
     */
    public boolean tieneUsuario() {
        return usuario != null;
    }
    
    /**
     * Obtiene el nombre del usuario asociado al mensaje
     */
    public String getNombreUsuario() {
        if (usuario != null) {
            return usuario.getNombre();
        }
        return null;
    }
}
