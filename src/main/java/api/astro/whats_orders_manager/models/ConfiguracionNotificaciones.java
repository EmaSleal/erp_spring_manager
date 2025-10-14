package api.astro.whats_orders_manager.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
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
 * CONFIGURACIÓN DE NOTIFICACIONES
 * WhatsApp Orders Manager
 * ============================================================================
 * Modelo de entidad para almacenar la configuración de notificaciones
 * del sistema.
 * 
 * Funcionalidades configurables:
 * - Activar/desactivar notificaciones por email
 * - Envío automático de facturas por email
 * - Días de recordatorio antes del vencimiento
 * - Días de recordatorio después del vencimiento
 * - Notificar al crear nuevo cliente
 * - Notificar al crear nuevo usuario
 * 
 * Tabla: configuracion_notificaciones
 * ============================================================================
 */
@Entity
@Table(name = "configuracion_notificaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ConfiguracionNotificaciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Integer idConfiguracion;

    /**
     * Activa o desactiva el sistema de notificaciones por email
     * Si está desactivado, no se enviarán emails automáticos
     */
    @NotNull(message = "El campo activar email es requerido")
    @Column(name = "activar_email", nullable = false)
    private Boolean activarEmail = true;

    /**
     * Enviar automáticamente la factura por email al crearla
     * Requiere que el cliente tenga email configurado
     */
    @NotNull(message = "El campo enviar factura automática es requerido")
    @Column(name = "enviar_factura_automatica", nullable = false)
    private Boolean enviarFacturaAutomatica = false;

    /**
     * Días antes del vencimiento para enviar recordatorio preventivo
     * Ej: 3 días antes de la fecha de pago
     */
    @Min(value = 0, message = "Los días de recordatorio preventivo no pueden ser negativos")
    @Column(name = "dias_recordatorio_preventivo")
    private Integer diasRecordatorioPreventivo = 3;

    /**
     * Días después del vencimiento para enviar recordatorios
     * Ej: Si está vencida por más de 1 día
     * El scheduler revisará facturas donde: fechaPago < (HOY - diasRecordatorioPago)
     */
    @Min(value = 0, message = "Los días de recordatorio de pago no pueden ser negativos")
    @Column(name = "dias_recordatorio_pago")
    private Integer diasRecordatorioPago = 0;

    /**
     * Frecuencia de recordatorios (cada cuántos días enviar recordatorio)
     * Ej: cada 7 días después del primer recordatorio
     */
    @Min(value = 1, message = "La frecuencia debe ser al menos 1 día")
    @Column(name = "frecuencia_recordatorios")
    private Integer frecuenciaRecordatorios = 7;

    /**
     * Notificar al administrador cuando se crea un nuevo cliente
     */
    @NotNull(message = "El campo notificar nuevo cliente es requerido")
    @Column(name = "notificar_nuevo_cliente", nullable = false)
    private Boolean notificarNuevoCliente = false;

    /**
     * Notificar al administrador cuando se crea un nuevo usuario
     */
    @NotNull(message = "El campo notificar nuevo usuario es requerido")
    @Column(name = "notificar_nuevo_usuario", nullable = false)
    private Boolean notificarNuevoUsuario = false;

    /**
     * Email del administrador para recibir notificaciones
     * Si está vacío, no se envían notificaciones administrativas
     */
    @Column(name = "email_admin", length = 100)
    private String emailAdmin;

    /**
     * Enviar copia de todas las facturas a este email (BCC)
     * Útil para contabilidad o respaldo
     */
    @Column(name = "email_copia_facturas", length = 100)
    private String emailCopiaFacturas;

    /**
     * Indica si esta es la configuración activa del sistema
     * Solo debe existir un registro con activo = true
     */
    @NotNull(message = "El campo activo es requerido")
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // ==================== CAMPOS DE AUDITORÍA ====================

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
     * Verifica si las notificaciones por email están habilitadas
     */
    public boolean notificacionesHabilitadas() {
        return Boolean.TRUE.equals(activarEmail);
    }

    /**
     * Verifica si se debe enviar la factura automáticamente
     */
    public boolean debeEnviarFacturaAutomatica() {
        return Boolean.TRUE.equals(activarEmail) && Boolean.TRUE.equals(enviarFacturaAutomatica);
    }

    /**
     * Verifica si se deben enviar recordatorios de pago
     */
    public boolean debeEnviarRecordatorios() {
        return Boolean.TRUE.equals(activarEmail) && diasRecordatorioPago != null && diasRecordatorioPago >= 0;
    }

    /**
     * Verifica si se debe enviar recordatorio preventivo
     */
    public boolean debeEnviarRecordatorioPreventivo() {
        return Boolean.TRUE.equals(activarEmail) && diasRecordatorioPreventivo != null && diasRecordatorioPreventivo > 0;
    }

    /**
     * Verifica si hay email de administrador configurado
     */
    public boolean tieneEmailAdmin() {
        return emailAdmin != null && !emailAdmin.trim().isEmpty();
    }

    /**
     * Verifica si se debe enviar notificación de nuevo cliente
     */
    public boolean debeNotificarNuevoCliente() {
        return Boolean.TRUE.equals(activarEmail) 
            && Boolean.TRUE.equals(notificarNuevoCliente) 
            && tieneEmailAdmin();
    }

    /**
     * Verifica si se debe enviar notificación de nuevo usuario
     */
    public boolean debeNotificarNuevoUsuario() {
        return Boolean.TRUE.equals(activarEmail) 
            && Boolean.TRUE.equals(notificarNuevoUsuario) 
            && tieneEmailAdmin();
    }

    /**
     * Obtiene el email para copia de facturas
     */
    public String getEmailCopiaFacturasOrNull() {
        return (emailCopiaFacturas != null && !emailCopiaFacturas.trim().isEmpty()) 
            ? emailCopiaFacturas 
            : null;
    }

    // ==================== CONSTRUCTOR CON VALORES POR DEFECTO ====================

    /**
     * Constructor con configuración por defecto
     */
    public static ConfiguracionNotificaciones conValoresPorDefecto() {
        ConfiguracionNotificaciones config = new ConfiguracionNotificaciones();
        config.setActivarEmail(true);
        config.setEnviarFacturaAutomatica(false);
        config.setDiasRecordatorioPreventivo(3);
        config.setDiasRecordatorioPago(0);
        config.setFrecuenciaRecordatorios(7);
        config.setNotificarNuevoCliente(false);
        config.setNotificarNuevoUsuario(false);
        config.setActivo(true);
        return config;
    }

    @Override
    public String toString() {
        return "ConfiguracionNotificaciones{" +
                "id=" + idConfiguracion +
                ", activarEmail=" + activarEmail +
                ", enviarFacturaAutomatica=" + enviarFacturaAutomatica +
                ", diasRecordatorioPago=" + diasRecordatorioPago +
                ", diasRecordatorioPreventivo=" + diasRecordatorioPreventivo +
                ", frecuenciaRecordatorios=" + frecuenciaRecordatorios +
                ", activo=" + activo +
                '}';
    }
}
