package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * USUARIO SESION - Gestión de Sesiones Activas
 * WhatsApp Orders Manager
 * ============================================================================
 * Registra y gestiona las sesiones activas de los usuarios.
 * 
 * Funcionalidades:
 * - Control de sesiones concurrentes
 * - Registro de información del dispositivo
 * - Detección de ubicación (IP)
 * - Cierre automático de sesiones expiradas
 * - Seguridad: detección de sesiones sospechosas
 * 
 * Casos de uso:
 * - Mostrar dispositivos activos al usuario
 * - Cerrar sesión remota
 * - Auditoría de accesos
 * - Detección de accesos no autorizados
 * 
 * @version 1.0
 * @since Sprint 4 - Fase 4
 * @date 22/12/2025
 * ============================================================================
 */
@Entity
@Table(name = "usuario_sesion", indexes = {
    @Index(name = "idx_usuario_sesion_usuario", columnList = "id_usuario"),
    @Index(name = "idx_usuario_sesion_token", columnList = "session_token"),
    @Index(name = "idx_usuario_sesion_activa", columnList = "activa"),
    @Index(name = "idx_usuario_sesion_expiracion", columnList = "fecha_expiracion")
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UsuarioSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long idSesion;

    /**
     * Usuario propietario de la sesión
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    /**
     * Token único de la sesión (puede ser JSESSIONID o JWT)
     */
    @Column(name = "session_token", nullable = false, unique = true, length = 500)
    private String sessionToken;

    /**
     * Dirección IP desde donde se inició la sesión
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User-Agent del navegador
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Tipo de dispositivo detectado
     * Valores: DESKTOP, MOBILE, TABLET, UNKNOWN
     */
    @Column(name = "tipo_dispositivo", length = 20)
    private String tipoDispositivo;

    /**
     * Sistema operativo detectado
     * Ejemplo: Windows 10, Android 12, iOS 15
     */
    @Column(name = "sistema_operativo", length = 100)
    private String sistemaOperativo;

    /**
     * Navegador detectado
     * Ejemplo: Chrome 120, Firefox 121, Safari 17
     */
    @Column(name = "navegador", length = 100)
    private String navegador;

    /**
     * País desde donde se conectó (opcional)
     */
    @Column(name = "pais", length = 50)
    private String pais;

    /**
     * Ciudad desde donde se conectó (opcional)
     */
    @Column(name = "ciudad", length = 100)
    private String ciudad;

    /**
     * Fecha y hora de inicio de sesión
     */
    @CreatedDate
    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    /**
     * Fecha y hora de la última actividad
     */
    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    /**
     * Fecha y hora de cierre de sesión (null si está activa)
     */
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /**
     * Fecha y hora de expiración de la sesión
     */
    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    /**
     * Indica si la sesión está actualmente activa
     */
    @Column(name = "activa", nullable = false)
    @Builder.Default
    private Boolean activa = true;

    /**
     * Razón de cierre de sesión
     * Valores: LOGOUT, TIMEOUT, ADMIN_FORCE, EXPIRED, SECURITY
     */
    @Column(name = "razon_cierre", length = 50)
    private String razonCierre;

    /**
     * Indica si la sesión fue cerrada forzosamente por un admin
     */
    @Column(name = "cerrada_por_admin")
    @Builder.Default
    private Boolean cerradaPorAdmin = false;

    /**
     * Marca la sesión como sospechosa (por ubicación inusual, etc.)
     */
    @Column(name = "sospechosa")
    @Builder.Default
    private Boolean sospechosa = false;

    /**
     * Notas sobre la sesión (para sesiones sospechosas)
     */
    @Column(name = "notas", length = 500)
    private String notas;

    // ==================== CONSTRUCTORS ====================

    /**
     * Constructor con campos básicos requeridos
     */
    public UsuarioSesion(Usuario usuario, String sessionToken, String ipAddress) {
        this.usuario = usuario;
        this.sessionToken = sessionToken;
        this.ipAddress = ipAddress;
        this.activa = true;
        this.sospechosa = false;
        this.cerradaPorAdmin = false;
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Verifica si la sesión sigue activa
     */
    public boolean estaActiva() {
        return this.activa && (this.fechaExpiracion == null || 
               LocalDateTime.now().isBefore(this.fechaExpiracion));
    }

    /**
     * Verifica si la sesión ha expirado
     */
    public boolean haExpirado() {
        return this.fechaExpiracion != null && 
               LocalDateTime.now().isAfter(this.fechaExpiracion);
    }

    /**
     * Cierra la sesión
     */
    public void cerrarSesion(String razon) {
        this.activa = false;
        this.fechaCierre = LocalDateTime.now();
        this.razonCierre = razon;
    }

    /**
     * Cierra la sesión forzosamente (por admin)
     */
    public void cerrarSesionForzada(String razon, String notasAdmin) {
        cerrarSesion(razon);
        this.cerradaPorAdmin = true;
        this.notas = notasAdmin;
    }

    /**
     * Actualiza la última actividad
     */
    public void actualizarActividad() {
        this.ultimaActividad = LocalDateTime.now();
    }

    /**
     * Marca la sesión como sospechosa
     */
    public void marcarComoSospechosa(String motivo) {
        this.sospechosa = true;
        this.notas = motivo;
    }

    /**
     * Obtiene el nombre del usuario de la sesión
     */
    public String getNombreUsuario() {
        return this.usuario != null ? this.usuario.getNombre() : "Desconocido";
    }

    /**
     * Calcula la duración de la sesión en minutos
     */
    public long getDuracionMinutos() {
        LocalDateTime fin = this.fechaCierre != null ? this.fechaCierre : LocalDateTime.now();
        return java.time.Duration.between(this.fechaInicio, fin).toMinutes();
    }

    /**
     * Obtiene una descripción amigable del dispositivo
     */
    public String getDescripcionDispositivo() {
        StringBuilder sb = new StringBuilder();
        if (navegador != null) sb.append(navegador);
        if (sistemaOperativo != null) {
            if (sb.length() > 0) sb.append(" en ");
            sb.append(sistemaOperativo);
        }
        if (tipoDispositivo != null) {
            if (sb.length() > 0) sb.append(" (");
            sb.append(tipoDispositivo);
            sb.append(")");
        }
        return sb.length() > 0 ? sb.toString() : "Dispositivo desconocido";
    }
}
