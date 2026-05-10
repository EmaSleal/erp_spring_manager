package api.astro.whats_orders_manager.modules.notificacion.model;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;

/**
 * ============================================================================
 * PREFERENCIA DE NOTIFICACIÓN
 * ERP Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Modelo de entidad para almacenar las preferencias de notificaciones
 * de cada usuario.
 * 
 * Funcionalidades:
 * - Configuración individual por usuario
 * - Control de qué tipos de notificaciones recibir
 * - Control de por qué canales recibir notificaciones
 * - Activación/desactivación global de notificaciones
 * 
 * Lógica:
 * - Un usuario puede tener múltiples preferencias (una por tipo + canal)
 * - Si no existe preferencia, se asume que está activada (opt-out)
 * - El usuario puede desactivar todas las notificaciones
 * 
 * Tabla: preferencia_notificacion
 * ============================================================================
 */
@Entity
@Table(name = "preferencia_notificacion", 
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_usuario_tipo_canal", 
            columnNames = {"id_usuario", "tipo_notificacion", "canal"}
        )
    },
    indexes = {
        @Index(name = "idx_usuario_activa", columnList = "id_usuario, activa"),
        @Index(name = "idx_tipo_canal", columnList = "tipo_notificacion, canal")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PreferenciaNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_preferencia")
    private Integer idPreferencia;

    // ==================== USUARIO ====================

    /**
     * Usuario propietario de la preferencia
     */
    @NotNull(message = "El usuario es requerido")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_preferencia_usuario"))
    private Usuario usuario;

    // ==================== TIPO Y CANAL ====================

    /**
     * Tipo de notificación específico
     * Si es NULL, la preferencia aplica para TODOS los tipos
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_notificacion", length = 100, nullable = true)
    private TipoNotificacion tipoNotificacion;

    /**
     * Canal de notificación específico
     * Si es NULL, la preferencia aplica para TODOS los canales
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", length = 50, nullable = true)
    private CanalNotificacion canal;

    // ==================== ESTADO ====================

    /**
     * Indica si la notificación está activada
     * TRUE = usuario RECIBE notificaciones de este tipo/canal
     * FALSE = usuario NO recibe notificaciones de este tipo/canal
     */
    @NotNull(message = "El estado activo es requerido")
    @Column(name = "activa", nullable = false)
    @Builder.Default
    private Boolean activa = true;

    /**
     * Indica si las notificaciones están completamente desactivadas
     * Si es TRUE, el usuario NO recibe NINGUNA notificación
     * Tiene prioridad sobre las preferencias individuales
     */
    @NotNull(message = "El estado global es requerido")
    @Column(name = "notificaciones_desactivadas_global", nullable = false)
    @Builder.Default
    private Boolean notificacionesDesactivadasGlobal = false;

    // ==================== CONFIGURACIÓN ADICIONAL ====================

    /**
     * Frecuencia deseada para este tipo de notificación
     * INMEDIATA: enviar cada vez que ocurra el evento
     * DIARIA: agrupar y enviar resumen diario
     * SEMANAL: agrupar y enviar resumen semanal
     */
    @Column(name = "frecuencia", length = 20)
    @Builder.Default
    private String frecuencia = "INMEDIATA";

    /**
     * Hora preferida para recibir notificaciones (formato HH:mm)
     * Solo aplica si frecuencia es DIARIA o SEMANAL
     */
    @Column(name = "hora_preferida", length = 5)
    private String horaPreferida;

    /**
     * Indica si se debe enviar notificación solo en horario laboral
     */
    @NotNull(message = "El estado de horario laboral es requerido")
    @Column(name = "solo_horario_laboral", nullable = false)
    @Builder.Default
    private Boolean soloHorarioLaboral = false;

    // ==================== AUDITORÍA ====================

    @CreatedBy
    @Column(name = "create_by", updatable = false)
    private Integer createBy;

    @CreatedDate
    @Column(name = "create_date", updatable = false)
    private Timestamp createDate;

    @LastModifiedBy
    @Column(name = "update_by")
    private Integer updateBy;

    @LastModifiedDate
    @Column(name = "update_date")
    private Timestamp updateDate;

    // ==================== MÉTODOS DE NEGOCIO ====================

    /**
     * Verifica si esta preferencia aplica para un tipo y canal específicos
     */
    public boolean aplicaPara(TipoNotificacion tipo, CanalNotificacion canalParam) {
        // Si las notificaciones están desactivadas globalmente, no aplica
        if (Boolean.TRUE.equals(notificacionesDesactivadasGlobal)) {
            return false;
        }

        // Si la preferencia no está activa, no aplica
        if (!Boolean.TRUE.equals(activa)) {
            return false;
        }

        // Si tipo es NULL, aplica para todos los tipos
        boolean coincideTipo = tipoNotificacion == null || tipoNotificacion == tipo;
        
        // Si canal es NULL, aplica para todos los canales
        boolean coincideCanal = canal == null || canal == canalParam;

        return coincideTipo && coincideCanal;
    }

    /**
     * Activa esta preferencia
     */
    public void activar() {
        this.activa = true;
    }

    /**
     * Desactiva esta preferencia
     */
    public void desactivar() {
        this.activa = false;
    }

    /**
     * Desactiva todas las notificaciones para este usuario
     */
    public void desactivarTodasLasNotificaciones() {
        this.notificacionesDesactivadasGlobal = true;
    }

    /**
     * Activa todas las notificaciones para este usuario
     */
    public void activarTodasLasNotificaciones() {
        this.notificacionesDesactivadasGlobal = false;
    }

    /**
     * Verifica si esta preferencia es global (aplica a todos los tipos y canales)
     */
    public boolean esGlobal() {
        return tipoNotificacion == null && canal == null;
    }

    /**
     * Verifica si esta preferencia es para un tipo específico en todos los canales
     */
    public boolean esPorTipo() {
        return tipoNotificacion != null && canal == null;
    }

    /**
     * Verifica si esta preferencia es para un canal específico en todos los tipos
     */
    public boolean esPorCanal() {
        return tipoNotificacion == null && canal != null;
    }

    /**
     * Verifica si esta preferencia es para un tipo y canal específicos
     */
    public boolean esEspecifica() {
        return tipoNotificacion != null && canal != null;
    }

    /**
     * Verifica si se debe enviar la notificación ahora o agruparla
     */
    public boolean esInmediata() {
        return "INMEDIATA".equals(frecuencia);
    }

    /**
     * Verifica si está en horario laboral (lunes a viernes 8:00 - 18:00)
     */
    public static boolean estaEnHorarioLaboral() {
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        java.time.DayOfWeek dia = ahora.getDayOfWeek();
        int hora = ahora.getHour();
        
        boolean esDiaSemana = dia != java.time.DayOfWeek.SATURDAY 
                           && dia != java.time.DayOfWeek.SUNDAY;
        boolean esHoraLaboral = hora >= 8 && hora < 18;
        
        return esDiaSemana && esHoraLaboral;
    }

    /**
     * Verifica si se debe enviar la notificación según restricción de horario
     */
    public boolean sePuedeEnviarAhora() {
        if (Boolean.TRUE.equals(soloHorarioLaboral)) {
            return estaEnHorarioLaboral();
        }
        return true;
    }

    @Override
    public String toString() {
        return "PreferenciaNotificacion{" +
                "id=" + idPreferencia +
                ", usuario=" + (usuario != null ? usuario.getIdUsuario() : "null") +
                ", tipo=" + tipoNotificacion +
                ", canal=" + canal +
                ", activa=" + activa +
                ", global=" + notificacionesDesactivadasGlobal +
                ", frecuencia=" + frecuencia +
                '}';
    }
}
