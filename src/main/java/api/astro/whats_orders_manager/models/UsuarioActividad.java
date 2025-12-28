package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * USUARIO ACTIVIDAD - Entidad de Auditoría
 * WhatsApp Orders Manager
 * ============================================================================
 * Registra todas las acciones importantes realizadas por los usuarios.
 * 
 * Funcionalidades:
 * - Registro automático de acciones (login, logout, CRUD, etc.)
 * - Auditoría completa de cambios
 * - Trazabilidad de operaciones críticas
 * - Información de IP y navegador
 * 
 * Casos de uso:
 * - Auditoría de seguridad
 * - Investigación de incidentes
 * - Reportes de actividad
 * - Cumplimiento normativo
 * 
 * @version 1.0
 * @since Sprint 4 - Fase 4
 * @date 22/12/2025
 * ============================================================================
 */
@Entity
@Table(name = "usuario_actividad", indexes = {
    @Index(name = "idx_usuario_actividad_usuario", columnList = "id_usuario"),
    @Index(name = "idx_usuario_actividad_fecha", columnList = "fecha_actividad"),
    @Index(name = "idx_usuario_actividad_tipo", columnList = "tipo_actividad")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UsuarioActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad")
    private Long idActividad;

    /**
     * Usuario que realizó la actividad
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    /**
     * Tipo de actividad realizada
     * Ejemplos: LOGIN, LOGOUT, CREAR_FACTURA, EDITAR_CLIENTE, etc.
     */
    @Column(name = "tipo_actividad", nullable = false, length = 50)
    private String tipoActividad;

    /**
     * Descripción detallada de la actividad
     * Ejemplo: "Usuario creó factura #F-0001 para cliente Juan Pérez"
     */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Módulo o entidad afectada
     * Ejemplos: FACTURA, CLIENTE, PRODUCTO, USUARIO, etc.
     */
    @Column(name = "entidad", length = 50)
    private String entidad;

    /**
     * ID del registro afectado
     * Ejemplo: ID de la factura creada, ID del cliente editado, etc.
     */
    @Column(name = "id_entidad")
    private Integer idEntidad;

    /**
     * Dirección IP desde donde se realizó la actividad
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User-Agent del navegador
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Información adicional en formato JSON
     * Ejemplo: {"cambios": {"precio_anterior": 100, "precio_nuevo": 150}}
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /**
     * Nivel de importancia de la actividad
     * Valores: INFO, WARNING, CRITICAL
     */
    @Column(name = "nivel", length = 20)
    @Builder.Default
    private String nivel = "INFO";

    /**
     * Fecha y hora de la actividad
     */
    @CreatedDate
    @Column(name = "fecha_actividad", nullable = false, updatable = false)
    private LocalDateTime fechaActividad;

    /**
     * Resultado de la actividad
     * Valores: SUCCESS, FAILURE, PARTIAL
     */
    @Column(name = "resultado", length = 20)
    @Builder.Default
    private String resultado = "SUCCESS";

    /**
     * Mensaje de error si la actividad falló
     */
    @Column(name = "error_mensaje", length = 500)
    private String errorMensaje;

    // ==================== CONSTRUCTORS ====================

    /**
     * Constructor con campos básicos requeridos
     */
    public UsuarioActividad(Usuario usuario, String tipoActividad, String descripcion) {
        this.usuario = usuario;
        this.tipoActividad = tipoActividad;
        this.descripcion = descripcion;
        this.nivel = "INFO";
        this.resultado = "SUCCESS";
    }

    /**
     * Constructor completo para registro de actividad
     */
    public UsuarioActividad(Usuario usuario, String tipoActividad, String descripcion, 
                           String entidad, Integer idEntidad, String ipAddress) {
        this.usuario = usuario;
        this.tipoActividad = tipoActividad;
        this.descripcion = descripcion;
        this.entidad = entidad;
        this.idEntidad = idEntidad;
        this.ipAddress = ipAddress;
        this.nivel = "INFO";
        this.resultado = "SUCCESS";
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Verifica si la actividad fue exitosa
     */
    public boolean fueExitosa() {
        return "SUCCESS".equals(this.resultado);
    }

    /**
     * Verifica si la actividad es crítica
     */
    public boolean esCritica() {
        return "CRITICAL".equals(this.nivel);
    }

    /**
     * Marca la actividad como fallida
     */
    public void marcarComoFallida(String mensajeError) {
        this.resultado = "FAILURE";
        this.errorMensaje = mensajeError;
    }

    /**
     * Obtiene el nombre del usuario que realizó la actividad
     */
    public String getNombreUsuario() {
        return this.usuario != null ? this.usuario.getNombre() : "Sistema";
    }
}
