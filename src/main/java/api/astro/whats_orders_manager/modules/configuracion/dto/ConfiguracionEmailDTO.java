package api.astro.whats_orders_manager.modules.configuracion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * CONFIGURACION EMAIL DTO
 * ERP Orders Manager
 * ============================================================================
 * Data Transfer Object para la configuración de email/SMTP.
 * 
 * Contiene los parámetros necesarios para configurar el servidor SMTP
 * y enviar correos electrónicos desde el sistema.
 * ============================================================================
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfiguracionEmailDTO {

    /**
     * ID de la configuración
     */
    private Integer idConfiguracion;

    /**
     * Host del servidor SMTP (ej: smtp.gmail.com)
     */
    private String smtpHost;

    /**
     * Puerto del servidor SMTP (ej: 587, 465, 25)
     */
    private Integer smtpPort;

    /**
     * Usuario para autenticación SMTP
     */
    private String smtpUsuario;

    /**
     * Contraseña para autenticación SMTP (se envía encriptada)
     */
    private String smtpPassword;

    /**
     * Indica si usa SSL
     */
    private Boolean smtpSsl;

    /**
     * Indica si usa TLS
     */
    private Boolean smtpTls;

    /**
     * Indica si requiere autenticación
     */
    private Boolean smtpAuth;

    /**
     * Email del remitente
     */
    private String emailRemitente;

    /**
     * Nombre del remitente
     */
    private String nombreRemitente;

    /**
     * Email para copia (CC)
     */
    private String emailCopia;

    /**
     * Email para copia oculta (BCC)
     */
    private String emailCopiaOculta;

    /**
     * Indica si la configuración está activa
     */
    private Boolean activo;

    /**
     * Fecha del último test realizado
     */
    private LocalDateTime ultimoTest;

    /**
     * Estado del último test (EXITOSO, FALLIDO, etc.)
     */
    private String estadoUltimoTest;

    /**
     * Protocolo de seguridad usado (solo lectura)
     */
    private String protocoloSeguridad;

    /**
     * Indica si la configuración está completa (solo lectura)
     */
    private Boolean configuracionCompleta;

    /**
     * Indica si el último test fue exitoso (solo lectura)
     */
    private Boolean ultimoTestExitoso;
}
