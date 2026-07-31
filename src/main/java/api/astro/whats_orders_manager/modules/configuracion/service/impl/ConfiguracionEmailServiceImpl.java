package api.astro.whats_orders_manager.modules.configuracion.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmailRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

/**
 * Implementación del servicio de configuración de email
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ConfiguracionEmailServiceImpl implements ConfiguracionEmailService {

    private final ConfiguracionEmailRepository configuracionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionEmail> obtenerConfiguracion() {
        log.debug("Obteniendo configuración de email");
        return configuracionRepository.findFirstByOrderByIdConfiguracionAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionEmail> obtenerConfiguracionActiva() {
        log.debug("Obteniendo configuración de email activa");
        return configuracionRepository.findFirstByActivoTrue();
    }

    @Override
    public ConfiguracionEmail obtenerOCrearConfiguracion() {
        log.debug("Obteniendo o creando configuración de email");
        
        Optional<ConfiguracionEmail> configuracionOpt = obtenerConfiguracion();
        
        if (configuracionOpt.isPresent()) {
            return configuracionOpt.get();
        }
        
        // Crear configuración por defecto
        ConfiguracionEmail nuevaConfiguracion = ConfiguracionEmail.builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .smtpUsuario("")
                .smtpPassword("")
                .smtpTls(true)
                .smtpAuth(true)
                .smtpTimeout(5000)
                .charset("UTF-8")
                .emailRemitente("")
                .nombreRemitente("Sistema de Facturación")
                .activo(false)
                .build();
        
        ConfiguracionEmail guardada = configuracionRepository.save(nuevaConfiguracion);
        log.info("Configuración de email creada con ID: {}", guardada.getIdConfiguracion());
        
        return guardada;
    }

    @Override
    public ConfiguracionEmail guardarConfiguracion(ConfiguracionEmail configuracion) {
        log.debug("Guardando configuración de email");
        
        if (configuracion == null) {
            throw new IllegalArgumentException("La configuración no puede ser null");
        }
        
        // Validar campos obligatorios
        if (configuracion.getSmtpHost() == null || configuracion.getSmtpHost().trim().isEmpty()) {
            throw new IllegalArgumentException("El host SMTP es obligatorio");
        }
        if (configuracion.getSmtpPort() == null) {
            throw new IllegalArgumentException("El puerto SMTP es obligatorio");
        }
        if (configuracion.getEmailRemitente() == null || configuracion.getEmailRemitente().trim().isEmpty()) {
            throw new IllegalArgumentException("El email remitente es obligatorio");
        }
        
        ConfiguracionEmail guardada = configuracionRepository.save(configuracion);
        log.info("Configuración de email guardada correctamente con ID: {}", guardada.getIdConfiguracion());
        
        return guardada;
    }

    @Override
    public ConfiguracionEmail actualizarConfiguracion(ConfiguracionEmail configuracion) {
        log.debug("Actualizando configuración de email");
        
        if (configuracion == null) {
            throw new IllegalArgumentException("La configuración no puede ser null");
        }
        
        // Obtener la configuración existente
        ConfiguracionEmail configuracionExistente = obtenerOCrearConfiguracion();
        
        // Actualizar solo los campos no nulos
        if (configuracion.getSmtpHost() != null) {
            configuracionExistente.setSmtpHost(configuracion.getSmtpHost());
        }
        if (configuracion.getSmtpPort() != null) {
            configuracionExistente.setSmtpPort(configuracion.getSmtpPort());
        }
        if (configuracion.getSmtpUsuario() != null) {
            configuracionExistente.setSmtpUsuario(configuracion.getSmtpUsuario());
        }
        if (configuracion.getSmtpPassword() != null && !configuracion.getSmtpPassword().equals("********")) {
            configuracionExistente.setSmtpPassword(configuracion.getSmtpPassword());
        }
        if (configuracion.getSmtpSsl() != null) {
            configuracionExistente.setSmtpSsl(configuracion.getSmtpSsl());
        }
        if (configuracion.getSmtpTls() != null) {
            configuracionExistente.setSmtpTls(configuracion.getSmtpTls());
        }
        if (configuracion.getSmtpAuth() != null) {
            configuracionExistente.setSmtpAuth(configuracion.getSmtpAuth());
        }
        if (configuracion.getSmtpTimeout() != null) {
            configuracionExistente.setSmtpTimeout(configuracion.getSmtpTimeout());
        }
        if (configuracion.getCharset() != null) {
            configuracionExistente.setCharset(configuracion.getCharset());
        }
        if (configuracion.getEmailRemitente() != null) {
            configuracionExistente.setEmailRemitente(configuracion.getEmailRemitente());
        }
        if (configuracion.getNombreRemitente() != null) {
            configuracionExistente.setNombreRemitente(configuracion.getNombreRemitente());
        }
        if (configuracion.getEmailCopia() != null) {
            configuracionExistente.setEmailCopia(configuracion.getEmailCopia());
        }
        if (configuracion.getEmailCopiaOculta() != null) {
            configuracionExistente.setEmailCopiaOculta(configuracion.getEmailCopiaOculta());
        }
        if (configuracion.getActivo() != null) {
            configuracionExistente.setActivo(configuracion.getActivo());
        }
        
        ConfiguracionEmail actualizada = configuracionRepository.save(configuracionExistente);
        log.info("Configuración de email actualizada correctamente");
        
        return actualizada;
    }

    @Override
    public boolean probarConfiguracion(String emailDestino) {
        log.debug("Probando configuración de email enviando a: {}", emailDestino);
        
        Optional<ConfiguracionEmail> configuracionOpt = obtenerConfiguracion();
        
        if (configuracionOpt.isEmpty()) {
            log.warn("No existe configuración de email para probar");
            return false;
        }
        
        ConfiguracionEmail configuracion = configuracionOpt.get();
        
        if (!configuracion.isConfiguracionCompleta()) {
            log.warn("La configuración de email no está completa");
            return false;
        }
        
        try {
            // Crear JavaMailSender con la configuración actual
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(configuracion.getSmtpHost());
            mailSender.setPort(configuracion.getSmtpPort());
            mailSender.setUsername(configuracion.getSmtpUsuario());
            mailSender.setPassword(configuracion.getSmtpPassword());
            mailSender.setDefaultEncoding(
                    configuracion.getCharset() != null ? configuracion.getCharset() : "UTF-8");

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", configuracion.getSmtpAuth() ? "true" : "false");
            props.put("mail.smtp.starttls.enable", configuracion.getSmtpTls() ? "true" : "false");
            props.put("mail.smtp.ssl.enable", configuracion.getSmtpSsl() ? "true" : "false");
            props.put("mail.debug", "false");

            String timeoutMs = String.valueOf(
                    configuracion.getSmtpTimeout() != null ? configuracion.getSmtpTimeout() : 5000);
            props.put("mail.smtp.connectiontimeout", timeoutMs);
            props.put("mail.smtp.timeout", timeoutMs);
            props.put("mail.smtp.writetimeout", timeoutMs);
            
            // Crear y enviar mensaje de prueba
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(configuracion.getEmailRemitente());
            message.setTo(emailDestino);
            message.setSubject("Prueba de Configuración de Email");
            message.setText("Este es un correo de prueba del sistema de facturación.\n\n" +
                    "Si recibiste este mensaje, la configuración de email está funcionando correctamente.\n\n" +
                    "Fecha: " + LocalDateTime.now());
            
            mailSender.send(message);
            
            // Registrar el resultado de la prueba
            configuracion.registrarTest(true, "Prueba exitosa");
            configuracionRepository.save(configuracion);
            
            log.info("Prueba de email exitosa enviada a: {}", emailDestino);
            return true;
            
        } catch (Exception e) {
            log.error("Error al probar configuración de email: {}", e.getMessage(), e);
            
            // Registrar el resultado fallido
            ConfiguracionEmail config = configuracionOpt.get();
            config.registrarTest(false, "Error: " + e.getMessage());
            configuracionRepository.save(config);
            
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validarConfiguracion() {
        log.debug("Validando configuración de email");
        
        Optional<ConfiguracionEmail> configuracionOpt = obtenerConfiguracion();
        
        if (configuracionOpt.isEmpty()) {
            return false;
        }
        
        boolean valida = configuracionOpt.get().isConfiguracionCompleta();
        log.debug("Configuración de email válida: {}", valida);
        
        return valida;
    }

    @Override
    public ConfiguracionEmail cambiarEstado(boolean activo) {
        log.debug("Cambiando estado de configuración de email a: {}", activo);

        ConfiguracionEmail configuracion = obtenerOCrearConfiguracion();
        configuracion.setActivo(activo);

        ConfiguracionEmail actualizada = configuracionRepository.save(configuracion);
        log.info("Estado de configuración de email cambiado a: {}", activo);

        return actualizada;
    }

    @Override
    public ConfiguracionEmail saveOrUpdate(ConfiguracionEmail configuracion) {
        return actualizarConfiguracion(configuracion);
    }
}
