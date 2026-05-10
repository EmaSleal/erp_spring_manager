package api.astro.whats_orders_manager.shared.service;

import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import jakarta.mail.MessagingException;

/**
 * ============================================================================
 * EMAIL SERVICE
 * ERP Orders Manager
 * ============================================================================
 * Servicio para el env├¡o de correos electr├│nicos.
 * 
 * Funcionalidades:
 * - Enviar emails simples (texto plano)
 * - Enviar emails HTML con plantillas
 * - Enviar emails con archivos adjuntos
 * - Logging de env├¡os
 * ============================================================================
 */
public interface EmailService {

    /**
     * Env├¡a un email simple con texto plano
     * 
     * @param to Destinatario (email)
     * @param subject Asunto del email
     * @param body Cuerpo del email (texto plano)
     * @throws MessagingException Si hay error al enviar
     */
    void enviarEmail(String to, String subject, String body) throws MessagingException;

    /**
     * Env├¡a un email HTML
     * 
     * @param to Destinatario (email)
     * @param subject Asunto del email
     * @param htmlContent Contenido HTML del email
     * @throws MessagingException Si hay error al enviar
     */
    void enviarEmailHtml(String to, String subject, String htmlContent) throws MessagingException;

    /**
     * Env├¡a un email con archivo adjunto
     * 
     * @param to Destinatario (email)
     * @param subject Asunto del email
     * @param body Cuerpo del email
     * @param archivo Archivo adjunto (bytes)
     * @param nombreArchivo Nombre del archivo adjunto
     * @throws MessagingException Si hay error al enviar
     */
    void enviarEmailConAdjunto(String to, String subject, String body, byte[] archivo, String nombreArchivo) 
            throws MessagingException;

    /**
     * Env├¡a un email HTML con archivo adjunto
     * 
     * @param to Destinatario (email)
     * @param subject Asunto del email
     * @param htmlContent Contenido HTML del email
     * @param archivo Archivo adjunto (bytes)
     * @param nombreArchivo Nombre del archivo adjunto
     * @throws MessagingException Si hay error al enviar
     */
    void enviarEmailHtmlConAdjunto(String to, String subject, String htmlContent, byte[] archivo, String nombreArchivo) 
            throws MessagingException;

    /**
     * Env├¡a un email de prueba al destinatario especificado
     * 
     * @param to Destinatario (email)
     * @return true si se envi├│ correctamente, false en caso contrario
     */
    boolean enviarEmailPrueba(String to);

    /**
     * Env├¡a una factura por email al cliente usando el template HTML
     * 
     * @param factura Factura a enviar
     * @throws MessagingException Si hay error al enviar
     */
    void enviarFacturaPorEmail(Factura factura) throws MessagingException;

    /**
     * Env├¡a las credenciales de acceso a un nuevo usuario por email
     * 
     * @param usuario Usuario al que se enviar├ín las credenciales
     * @param contrasenaPlana Contrase├▒a en texto plano (antes de encriptar)
     * @param urlLogin URL del sistema para login
     * @throws MessagingException Si hay error al enviar
     */
    void enviarCredencialesUsuario(api.astro.whats_orders_manager.modules.seguridad.model.Usuario usuario, String contrasenaPlana, String urlLogin) throws MessagingException;

    /**
     * Env├¡a un recordatorio de pago al cliente de una factura vencida
     * 
     * @param factura Factura con pago vencido
     * @throws MessagingException Si hay error al enviar
     */
    void enviarRecordatorioPago(Factura factura) throws MessagingException;
}
