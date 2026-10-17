package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * T-022 (ADR-027) — Arma el JavaMailSender a partir de la configuración de email (fila única). Lo usan la
 * prueba de envío y el envío de correos, así ambos se conectan al mismo servidor con los mismos parámetros.
 * Crear el sender no abre ninguna conexión.
 */
public final class SmtpMailSenderFactory {

    private SmtpMailSenderFactory() {
    }

    public static JavaMailSenderImpl crear(ConfiguracionEmail configuracion) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(configuracion.getSmtpHost());
        mailSender.setPort(configuracion.getSmtpPort());
        mailSender.setUsername(configuracion.getSmtpUsuario());
        mailSender.setPassword(configuracion.getSmtpPassword());
        mailSender.setDefaultEncoding(configuracion.getCharset() != null ? configuracion.getCharset() : "UTF-8");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", Boolean.TRUE.equals(configuracion.getSmtpAuth()) ? "true" : "false");
        props.put("mail.smtp.starttls.enable", Boolean.TRUE.equals(configuracion.getSmtpTls()) ? "true" : "false");
        props.put("mail.smtp.ssl.enable", Boolean.TRUE.equals(configuracion.getSmtpSsl()) ? "true" : "false");
        props.put("mail.debug", "false");

        String timeoutMs = String.valueOf(configuracion.getSmtpTimeout() != null ? configuracion.getSmtpTimeout() : 5000);
        props.put("mail.smtp.connectiontimeout", timeoutMs);
        props.put("mail.smtp.timeout", timeoutMs);
        props.put("mail.smtp.writetimeout", timeoutMs);
        return mailSender;
    }
}
