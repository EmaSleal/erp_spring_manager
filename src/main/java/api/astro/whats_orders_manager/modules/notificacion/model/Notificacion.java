package api.astro.whats_orders_manager.modules.notificacion.model;

import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;

/**
 * ============================================================================
 * NOTIFICACIÓN
 * ERP Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Modelo de entidad para almacenar el historial de notificaciones enviadas.
 * 
 * Funcionalidades:
 * - Registro de cada notificación enviada
 * - Seguimiento del estado (leída/no leída, enviada/error)
 * - Vínculo con el destinatario (usuario)
 * - Vínculo opcional con entidad relacionada (factura, cliente, etc.)
 * - Historial completo para auditoría
 * 
 * Tabla: notificacion
 * ============================================================================
 */
@Entity
@Table(name = "notificacion", indexes = {
    @Index(name = "idx_usuario_leida", columnList = "id_usuario, leida"),
    @Index(name = "idx_tipo_fecha", columnList = "tipo, fecha_envio"),
    @Index(name = "idx_canal_estado", columnList = "canal, enviada"),
    @Index(name = "idx_fecha_envio", columnList = "fecha_envio")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    // ==================== TIPO Y CANAL ====================

    /**
     * Tipo de notificación (factura creada, pago recibido, etc.)
     */
    @NotNull(message = "El tipo de notificación es requerido")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoNotificacion tipo;

    /**
     * Canal por el cual se envió la notificación (web, email, whatsapp)
     */
    @NotNull(message = "El canal de notificación es requerido")
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 30)
    private CanalNotificacion canal;

    // ==================== DESTINATARIO ====================

    /**
     * Usuario destinatario de la notificación
     * Si es NULL, la notificación es para todos los usuarios (broadcast)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", foreignKey = @ForeignKey(name = "fk_notificacion_usuario"))
    private Usuario usuario;

    /**
     * Email del destinatario (si no es usuario del sistema)
     * Usado para notificaciones a clientes externos
     */
    @Column(name = "email_destinatario", length = 100)
    private String emailDestinatario;

    /**
     * Teléfono del destinatario (para WhatsApp/SMS)
     */
    @Column(name = "telefono_destinatario", length = 20)
    private String telefonoDestinatario;

    // ==================== CONTENIDO ====================

    /**
     * Título de la notificación
     */
    @NotBlank(message = "El título es requerido")
    @Size(max = 200, message = "El título no puede exceder 200 caracteres")
    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    /**
     * Mensaje/contenido de la notificación
     */
    @NotBlank(message = "El mensaje es requerido")
    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    /**
     * URL de acción (redirección al hacer clic)
     * Ejemplo: /facturas/detalle/123
     */
    @Column(name = "url_accion", length = 255)
    private String urlAccion;

    /**
     * Texto del botón de acción
     * Ejemplo: "Ver Factura", "Ver Detalle"
     */
    @Column(name = "texto_boton", length = 50)
    private String textoBoton;

    // ==================== RELACIONES OPCIONALES ====================

    /**
     * ID de la factura relacionada (si aplica)
     */
    @Column(name = "id_factura_relacionada")
    private Integer idFacturaRelacionada;

    /**
     * ID del cliente relacionado (si aplica)
     */
    @Column(name = "id_cliente_relacionado")
    private Integer idClienteRelacionado;

    /**
     * ID del producto relacionado (si aplica)
     */
    @Column(name = "id_producto_relacionado")
    private Integer idProductoRelacionado;

    /**
     * ID de cualquier entidad relacionada (genérico)
     */
    @Column(name = "entidad_relacionada_id")
    private Integer entidadRelacionadaId;

    /**
     * Tipo de entidad relacionada (Factura, Cliente, Producto, etc.)
     */
    @Column(name = "entidad_relacionada_tipo", length = 50)
    private String entidadRelacionadaTipo;

    // ==================== ESTADO ====================

    /**
     * Indica si la notificación fue enviada exitosamente
     */
    @NotNull(message = "El estado de envío es requerido")
    @Column(name = "enviada", nullable = false)
    @Builder.Default
    private Boolean enviada = false;

    /**
     * Indica si la notificación fue leída por el usuario
     * Solo aplica para canal WEB
     */
    @NotNull(message = "El estado de lectura es requerido")
    @Column(name = "leida", nullable = false)
    @Builder.Default
    private Boolean leida = false;

    /**
     * Fecha y hora de envío de la notificación
     */
    @CreatedDate
    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private Timestamp fechaEnvio;

    /**
     * Fecha y hora en que fue leída
     */
    @Column(name = "fecha_lectura")
    private Timestamp fechaLectura;

    /**
     * Mensaje de error si el envío falló
     */
    @Column(name = "error_mensaje", columnDefinition = "TEXT")
    private String errorMensaje;

    /**
     * Número de intentos de envío
     */
    @Column(name = "intentos_envio")
    @Builder.Default
    private Integer intentosEnvio = 1;

    // ==================== PLANTILLA ====================

    /**
     * ID de la plantilla utilizada (si aplica)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plantilla", foreignKey = @ForeignKey(name = "fk_notificacion_plantilla"))
    private PlantillaNotificacion plantilla;

    // ==================== AUDITORÍA ====================

    /**
     * Usuario que generó la notificación (sistema o admin)
     */
    @Column(name = "generada_por")
    private Integer generadaPor;

    // ==================== MÉTODOS DE NEGOCIO ====================

    /**
     * Marca la notificación como leída
     */
    public void marcarComoLeida() {
        this.leida = true;
        this.fechaLectura = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Marca la notificación como enviada exitosamente
     */
    public void marcarComoEnviada() {
        this.enviada = true;
        this.errorMensaje = null;
    }

    /**
     * Registra un error en el envío
     */
    public void registrarError(String error) {
        this.enviada = false;
        this.errorMensaje = error;
        this.intentosEnvio++;
    }

    /**
     * Verifica si la notificación es para un usuario del sistema
     */
    public boolean esParaUsuario() {
        return usuario != null;
    }

    /**
     * Verifica si la notificación es para un destinatario externo
     */
    public boolean esParaExterno() {
        return usuario == null && (emailDestinatario != null || telefonoDestinatario != null);
    }

    /**
     * Verifica si la notificación está pendiente de lectura
     */
    public boolean estaPendiente() {
        return enviada && !leida && canal == CanalNotificacion.WEB;
    }

    /**
     * Verifica si el envío falló
     */
    public boolean tieneFallo() {
        return !enviada && errorMensaje != null;
    }

    /**
     * Obtiene el nombre del destinatario
     */
    public String getNombreDestinatario() {
        if (usuario != null) {
            return usuario.getNombre();
        }
        if (emailDestinatario != null) {
            return emailDestinatario;
        }
        if (telefonoDestinatario != null) {
            return telefonoDestinatario;
        }
        return "Destinatario desconocido";
    }

    @Override
    public String toString() {
        return "Notificacion{" +
                "id=" + idNotificacion +
                ", tipo=" + tipo +
                ", canal=" + canal +
                ", titulo='" + titulo + '\'' +
                ", enviada=" + enviada +
                ", leida=" + leida +
                ", fechaEnvio=" + fechaEnvio +
                '}';
    }
}
