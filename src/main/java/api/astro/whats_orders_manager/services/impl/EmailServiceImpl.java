package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.ConfiguracionFacturacion;
import api.astro.whats_orders_manager.services.EmailService;
import api.astro.whats_orders_manager.services.EmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.service.LineaFacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.ConfiguracionFacturacionService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ============================================================================
 * EMAIL SERVICE IMPLEMENTATION
 * WhatsApp Orders Manager
 * ============================================================================
 * Implementación del servicio de correos electrónicos usando JavaMailSender.
 * 
 * Características:
 * - Envío de emails simples y HTML
 * - Soporte para archivos adjuntos
 * - Logging detallado de envíos
 * - Manejo robusto de excepciones
 * - Email de prueba para verificación
 * ============================================================================
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private EmpresaService empresaService;

    @Autowired
    private LineaFacturaService lineaFacturaService;

    @Autowired
    private ConfiguracionFacturacionService configuracionFacturacionService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String CHARSET = "UTF-8";

    /**
     * Envía un email simple con texto plano
     */
    @Override
    public void enviarEmail(String to, String subject, String body) throws MessagingException {
        log.info("Enviando email simple a: {}", to);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            
            log.info("✅ Email enviado exitosamente a: {}", to);
        } catch (Exception e) {
            log.error("❌ Error al enviar email a {}: {}", to, e.getMessage());
            throw new MessagingException("Error al enviar email: " + e.getMessage(), e);
        }
    }

    /**
     * Envía un email HTML
     */
    @Override
    public void enviarEmailHtml(String to, String subject, String htmlContent) throws MessagingException {
        log.info("Enviando email HTML a: {}", to);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, CHARSET);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            
            mailSender.send(message);
            
            log.info("✅ Email HTML enviado exitosamente a: {}", to);
        } catch (Exception e) {
            log.error("❌ Error al enviar email HTML a {}: {}", to, e.getMessage());
            throw new MessagingException("Error al enviar email HTML: " + e.getMessage(), e);
        }
    }

    /**
     * Envía un email con archivo adjunto
     */
    @Override
    public void enviarEmailConAdjunto(String to, String subject, String body, byte[] archivo, String nombreArchivo) 
            throws MessagingException {
        log.info("Enviando email con adjunto '{}' a: {}", nombreArchivo, to);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, CHARSET); // true = multipart
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            
            // Agregar archivo adjunto
            helper.addAttachment(nombreArchivo, new ByteArrayResource(archivo));
            
            mailSender.send(message);
            
            log.info("✅ Email con adjunto enviado exitosamente a: {}", to);
        } catch (Exception e) {
            log.error("❌ Error al enviar email con adjunto a {}: {}", to, e.getMessage());
            throw new MessagingException("Error al enviar email con adjunto: " + e.getMessage(), e);
        }
    }

    /**
     * Envía un email HTML con archivo adjunto
     */
    @Override
    public void enviarEmailHtmlConAdjunto(String to, String subject, String htmlContent, byte[] archivo, String nombreArchivo) 
            throws MessagingException {
        log.info("Enviando email HTML con adjunto '{}' a: {}", nombreArchivo, to);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, CHARSET); // true = multipart
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            
            // Agregar archivo adjunto
            helper.addAttachment(nombreArchivo, new ByteArrayResource(archivo));
            
            mailSender.send(message);
            
            log.info("✅ Email HTML con adjunto enviado exitosamente a: {}", to);
        } catch (Exception e) {
            log.error("❌ Error al enviar email HTML con adjunto a {}: {}", to, e.getMessage());
            throw new MessagingException("Error al enviar email HTML con adjunto: " + e.getMessage(), e);
        }
    }

    /**
     * Envía un email de prueba
     */
    @Override
    public boolean enviarEmailPrueba(String to) {
        log.info("Enviando email de prueba a: {}", to);
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            
            String htmlContent = generarHtmlEmailPrueba(timestamp);
            
            enviarEmailHtml(to, "✅ Prueba de Configuración de Email - WhatsApp Orders Manager", htmlContent);
            
            return true;
        } catch (Exception e) {
            log.error("❌ Error al enviar email de prueba: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Genera el HTML para el email de prueba
     */
    private String generarHtmlEmailPrueba(String timestamp) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Email de Prueba</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 30px;
                            text-align: center;
                            border-radius: 10px 10px 0 0;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 24px;
                        }
                        .content {
                            background: #f8f9fa;
                            padding: 30px;
                            border-radius: 0 0 10px 10px;
                        }
                        .success-icon {
                            text-align: center;
                            font-size: 60px;
                            margin: 20px 0;
                        }
                        .info-box {
                            background: white;
                            padding: 20px;
                            border-radius: 8px;
                            margin: 20px 0;
                            border-left: 4px solid #28a745;
                        }
                        .info-box h3 {
                            margin-top: 0;
                            color: #28a745;
                        }
                        .info-item {
                            margin: 10px 0;
                            padding: 10px;
                            background: #f8f9fa;
                            border-radius: 4px;
                        }
                        .info-item strong {
                            color: #667eea;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            padding-top: 20px;
                            border-top: 2px solid #dee2e6;
                            color: #6c757d;
                            font-size: 14px;
                        }
                        .button {
                            display: inline-block;
                            padding: 12px 30px;
                            background: #667eea;
                            color: white;
                            text-decoration: none;
                            border-radius: 6px;
                            margin: 20px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>✅ Configuración de Email Exitosa</h1>
                        <p style="margin: 10px 0 0 0;">WhatsApp Orders Manager</p>
                    </div>
                    
                    <div class="content">
                        <div class="success-icon">🎉</div>
                        
                        <h2 style="text-align: center; color: #28a745;">¡Email de Prueba Recibido!</h2>
                        
                        <p style="text-align: center; font-size: 16px;">
                            Si estás leyendo este mensaje, significa que la configuración de email
                            de tu aplicación está funcionando correctamente.
                        </p>
                        
                        <div class="info-box">
                            <h3>📋 Detalles de la Prueba</h3>
                            <div class="info-item">
                                <strong>Fecha y Hora:</strong> """ + timestamp + """
                            </div>
                            <div class="info-item">
                                <strong>Sistema:</strong> WhatsApp Orders Manager v1.0
                            </div>
                            <div class="info-item">
                                <strong>Módulo:</strong> Servicio de Notificaciones por Email
                            </div>
                            <div class="info-item">
                                <strong>Estado:</strong> <span style="color: #28a745;">✅ Operativo</span>
                            </div>
                        </div>
                        
                        <div class="info-box" style="border-left-color: #007bff;">
                            <h3 style="color: #007bff;">📧 Funcionalidades Disponibles</h3>
                            <ul>
                                <li>✉️ Envío de facturas por email</li>
                                <li>🔑 Envío de credenciales a usuarios</li>
                                <li>⏰ Recordatorios de pago automáticos</li>
                                <li>📊 Notificaciones de sistema</li>
                            </ul>
                        </div>
                        
                        <div style="text-align: center;">
                            <p style="color: #6c757d; font-style: italic;">
                                Este es un email generado automáticamente. No es necesario responder.
                            </p>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>
                            <strong>WhatsApp Orders Manager</strong><br>
                            Sistema de Gestión de Pedidos y Facturación<br>
                            © 2025 - Todos los derechos reservados
                        </p>
                    </div>
                </body>
                </html>
                """;
    }

    /**
     * Envía una factura por email al cliente usando el template HTML
     * Requiere que el cliente tenga email configurado
     */
    @Override
    public void enviarFacturaPorEmail(Factura factura) throws MessagingException {
        log.info("Preparando envío de factura #{} por email", factura.getNumeroFactura());
        
        try {
            Cliente cliente = factura.getCliente();
            
            // Validar que el cliente tenga email
            if (cliente == null || cliente.getEmail() == null || cliente.getEmail().isBlank()) {
                throw new IllegalArgumentException("El cliente no tiene email configurado");
            }
            
            // Cargar las líneas de la factura
            var lineasR = lineaFacturaService.findLineasByFacturaId(factura.getIdFactura());
            
            // Convertir LineaFacturaR a LineaFactura y asignarlas a la factura
            var lineas = lineasR.stream()
                .map(lr -> {
                    LineaFactura lf = new LineaFactura();
                    lf.setIdLineaFactura(lr.id_linea_factura());
                    lf.setCantidad(lr.cantidad());
                    lf.setPrecioUnitario(lr.precioUnitario());
                    lf.setSubtotal(lr.subtotal());
                    
                    // Crear un producto simplificado con el ID y descripción
                    var producto = new api.astro.whats_orders_manager.modules.producto.model.Producto();
                    producto.setIdProducto(lr.id_producto());
                    producto.setDescripcion(lr.descripcion());
                    lf.setProducto(producto);
                    
                    return lf;
                })
                .toList();
            
            factura.setLineas(lineas);
            
            // Obtener información de la empresa
            var empresa = empresaService.getEmpresaPrincipal();
            
            // Obtener símbolo de moneda
            String simboloMoneda = obtenerSimboloMoneda();
            
            // Crear contexto de Thymeleaf
            Context context = new Context();
            context.setVariable("factura", factura);
            context.setVariable("empresa", empresa);
            context.setVariable("simboloMoneda", simboloMoneda);
            
            // Procesar el template
            String htmlContent = templateEngine.process("email/factura", context);
            
            // Enviar el email
            enviarEmailHtml(
                cliente.getEmail(),
                "Factura #" + factura.getNumeroFactura() + " - " + empresa.getNombreEmpresa(),
                htmlContent
            );
            
            log.info("✅ Factura #{} enviada exitosamente por email a: {}", 
                factura.getNumeroFactura(), cliente.getEmail());
        } catch (Exception e) {
            log.error("❌ Error al enviar factura #{} por email: {}", 
                factura.getNumeroFactura(), e.getMessage());
            throw new MessagingException("Error al enviar factura por email: " + e.getMessage(), e);
        }
    }

    /**
     * Envía las credenciales de acceso a un nuevo usuario por email
     * Usa el template HTML credenciales-usuario.html
     */
    @Override
    public void enviarCredencialesUsuario(api.astro.whats_orders_manager.modules.seguridad.model.Usuario usuario, 
                                          String contrasenaPlana, 
                                          String urlLogin) throws MessagingException {
        log.info("📧 Preparando envío de credenciales para usuario: {} ({})", 
            usuario.getNombre(), usuario.getEmail());
        
        try {
            // Validar que el usuario tenga email
            if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
                throw new IllegalArgumentException("El usuario no tiene email configurado");
            }
            
            // Obtener información de la empresa
            var empresa = empresaService.getEmpresaPrincipal();
            
            // Crear contexto de Thymeleaf
            Context context = new Context();
            context.setVariable("usuario", usuario);
            context.setVariable("contrasena", contrasenaPlana);
            context.setVariable("urlLogin", urlLogin);
            context.setVariable("empresaEmail", empresa.getEmail());
            context.setVariable("empresaTelefono", empresa.getTelefono());
            
            // Procesar el template
            String htmlContent = templateEngine.process("email/credenciales-usuario", context);
            
            // Enviar el email
            enviarEmailHtml(
                usuario.getEmail(),
                "🔑 Credenciales de Acceso - " + empresa.getNombreEmpresa(),
                htmlContent
            );
            
            log.info("✅ Credenciales enviadas exitosamente por email a: {} ({})", 
                usuario.getNombre(), usuario.getEmail());
        } catch (Exception e) {
            log.error("❌ Error al enviar credenciales para usuario {}: {}", 
                usuario.getNombre(), e.getMessage());
            throw new MessagingException("Error al enviar credenciales por email: " + e.getMessage(), e);
        }
    }

    /**
     * Envía un recordatorio de pago al cliente de una factura vencida
     */
    @Override
    public void enviarRecordatorioPago(Factura factura) throws MessagingException {
        try {
            log.info("📧 Preparando recordatorio de pago para factura {} - Cliente: {}", 
                factura.getNumeroFactura(), factura.getCliente().getNombre());

            // Validaciones
            if (factura.getCliente() == null || factura.getCliente().getEmail() == null) {
                throw new IllegalArgumentException("El cliente no tiene email configurado");
            }

            if (factura.getFechaPago() == null) {
                throw new IllegalArgumentException("La factura no tiene fecha de pago configurada");
            }

            // Cargar datos de empresa
            var empresa = empresaService.findEmpresaActiva()
                .orElseThrow(() -> new IllegalStateException("No se encontró la empresa principal"));

            // Cargar líneas de factura
            var lineas = lineaFacturaService.findLineasByFacturaId(factura.getIdFactura());

            // Calcular días de retraso
            long diasRetraso = java.time.temporal.ChronoUnit.DAYS.between(
                factura.getFechaPago().toLocalDate(),
                java.time.LocalDate.now()
            );

            // Obtener símbolo de moneda
            String simboloMoneda = obtenerSimboloMoneda();

            // Preparar contexto para el template
            Context context = new Context();
            context.setVariable("empresa", empresa);
            context.setVariable("factura", factura);
            context.setVariable("cliente", factura.getCliente());
            context.setVariable("lineas", lineas);
            context.setVariable("diasRetraso", diasRetraso);
            context.setVariable("fechaActual", java.time.LocalDate.now());
            context.setVariable("simboloMoneda", simboloMoneda);

            // Procesar template
            String htmlContent = templateEngine.process("email/recordatorio-pago", context);

            // Enviar email
            enviarEmailHtml(
                factura.getCliente().getEmail(),
                "⏰ Recordatorio de Pago - Factura " + factura.getNumeroFactura(),
                htmlContent
            );

            log.info("✅ Recordatorio de pago enviado exitosamente a: {} - Factura: {} ({} días de retraso)", 
                factura.getCliente().getEmail(), factura.getNumeroFactura(), diasRetraso);

        } catch (Exception e) {
            log.error("❌ Error al enviar recordatorio de pago para factura {}: {}", 
                factura.getNumeroFactura(), e.getMessage());
            throw new MessagingException("Error al enviar recordatorio de pago: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el símbolo de moneda desde la configuración de facturación
     * @return Símbolo de moneda (₡, $, €, etc.)
     */
    private String obtenerSimboloMoneda() {
        try {
            var config = configuracionFacturacionService.getConfiguracionActiva();
            if (config.isPresent()) {
                String moneda = config.get().getMoneda();
                if (moneda != null) {
                    return switch (moneda.toUpperCase()) {
                        case "CRC" -> "₡";
                        case "USD" -> "$";
                        case "MXN" -> "$";
                        case "EUR" -> "€";
                        default -> moneda + " ";
                    };
                }
            }
        } catch (Exception e) {
            log.warn("Error al obtener símbolo de moneda, usando predeterminado: {}", e.getMessage());
        }
        return "₡"; // Colones por defecto
    }
}
