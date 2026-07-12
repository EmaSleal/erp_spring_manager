package api.astro.whats_orders_manager.modules.configuracion.model;

import api.astro.whats_orders_manager.modules.configuracion.converter.SmtpPasswordConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidad que almacena la configuración del servidor de correo electrónico (SMTP).
 * Permite configurar el envío de emails para notificaciones del sistema.
 * 
 * Solo debe existir UN registro activo en esta tabla.
 */
@Entity
@Table(name = "configuracion_email")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ConfiguracionEmail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Integer idConfiguracion;

    // ==================== SERVIDOR SMTP ====================
    
    @Column(name = "smtp_host", nullable = false, length = 255)
    private String smtpHost;

    @Builder.Default
    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort = 587;

    @Column(name = "smtp_usuario", nullable = false, length = 255)
    private String smtpUsuario;

    @Convert(converter = SmtpPasswordConverter.class)
    @Column(name = "smtp_password", nullable = false, length = 255)
    private String smtpPassword;

    // ==================== CONFIGURACIÓN DE SEGURIDAD ====================
    
    @Builder.Default
    @Column(name = "smtp_ssl")
    private Boolean smtpSsl = false;

    @Builder.Default
    @Column(name = "smtp_tls")
    private Boolean smtpTls = true;

    @Builder.Default
    @Column(name = "smtp_auth")
    private Boolean smtpAuth = true;

    // ==================== REMITENTE ====================
    
    @Column(name = "email_remitente", nullable = false, length = 255)
    private String emailRemitente;

    @Column(name = "nombre_remitente", length = 255)
    private String nombreRemitente;

    // ==================== CONFIGURACIÓN DE ENVÍO ====================
    
    @Column(name = "email_copia", length = 255)
    private String emailCopia;

    @Column(name = "email_copia_oculta", length = 255)
    private String emailCopiaOculta;

    // ==================== ESTADO ====================
    
    @Builder.Default
    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "ultimo_test")
    private LocalDateTime ultimoTest;

    @Column(name = "estado_ultimo_test", length = 50)
    private String estadoUltimoTest;

    // ==================== AUDITORÍA ====================
    
    @CreatedBy
    @Column(name = "create_by", updatable = false)
    private Integer createBy;

    @CreatedDate
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @LastModifiedBy
    @Column(name = "update_by")
    private Integer updateBy;

    @LastModifiedDate
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Verifica si la configuración está completa y lista para usar.
     * 
     * @return true si todos los campos obligatorios están configurados
     */
    public boolean isConfiguracionCompleta() {
        return smtpHost != null && !smtpHost.isEmpty() &&
               smtpPort != null &&
               smtpUsuario != null && !smtpUsuario.isEmpty() &&
               smtpPassword != null && !smtpPassword.isEmpty() &&
               emailRemitente != null && !emailRemitente.isEmpty();
    }

    /**
     * Verifica si el último test fue exitoso.
     * 
     * @return true si el último test fue exitoso
     */
    public boolean isUltimoTestExitoso() {
        return "EXITOSO".equalsIgnoreCase(estadoUltimoTest);
    }

    /**
     * Marca el resultado del último test de envío.
     * 
     * @param exitoso true si el test fue exitoso, false si falló
     * @param mensaje Mensaje descriptivo del resultado
     */
    public void registrarTest(boolean exitoso, String mensaje) {
        this.ultimoTest = LocalDateTime.now();
        this.estadoUltimoTest = exitoso ? "EXITOSO" : "FALLIDO";
    }

    /**
     * Obtiene el nombre del remitente para mostrar en el email.
     * Si no hay nombre configurado, usa el email.
     * 
     * @return Nombre del remitente
     */
    public String getRemitenteDisplay() {
        if (nombreRemitente != null && !nombreRemitente.isEmpty()) {
            return nombreRemitente + " <" + emailRemitente + ">";
        }
        return emailRemitente;
    }

    /**
     * Obtiene la configuración del protocolo de seguridad.
     * 
     * @return "SSL", "TLS" o "NINGUNO"
     */
    public String getProtocoloSeguridad() {
        if (Boolean.TRUE.equals(smtpSsl)) {
            return "SSL";
        } else if (Boolean.TRUE.equals(smtpTls)) {
            return "TLS";
        }
        return "NINGUNO";
    }

    /**
     * Verifica si usa algún protocolo de seguridad.
     * 
     * @return true si usa SSL o TLS
     */
    public boolean usaSeguridad() {
        return Boolean.TRUE.equals(smtpSsl) || Boolean.TRUE.equals(smtpTls);
    }

    @Override
    public String toString() {
        return "ConfiguracionEmail{" +
                "idConfiguracion=" + idConfiguracion +
                ", smtpHost='" + smtpHost + '\'' +
                ", smtpPort=" + smtpPort +
                ", emailRemitente='" + emailRemitente + '\'' +
                ", activo=" + activo +
                ", protocolo=" + getProtocoloSeguridad() +
                '}';
    }
}
