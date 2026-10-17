package api.astro.whats_orders_manager.modules.configuracion.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmail;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmailRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmailService;
import api.astro.whats_orders_manager.modules.configuracion.service.SmtpMailSenderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementación del servicio de configuración de email
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ConfiguracionEmailServiceImpl implements ConfiguracionEmailService {

    /** Valor con el que la pantalla y la API muestran la contraseña (ConfiguracionEmailMapper). */
    static final String MASCARA_PASSWORD = "********";

    private final ConfiguracionEmailRepository configuracionRepository;

    /**
     * ADR-027: único método de lectura de la configuración de email (la tabla tiene una sola fila). Lo usan el
     * envío de correos, la prueba, la validación, la edición y la pantalla. "activo" no influye en cuál fila se
     * lee: indica si el envío está habilitado.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionEmail> obtenerConfiguracion() {
        log.debug("Obteniendo configuración de email");
        return configuracionRepository.findFirstByOrderByIdConfiguracionAsc();
    }

    @Override
    public ConfiguracionEmail obtenerOCrearConfiguracion() {
        log.debug("Obteniendo o creando configuración de email");
        
        Optional<ConfiguracionEmail> configuracionOpt = obtenerConfiguracion();
        
        if (configuracionOpt.isPresent()) {
            return configuracionOpt.get();
        }
        
        // Tabla vacía (instalación nueva): se crea la única fila, inactiva. Nunca se crea una segunda (ADR-027).
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
        
        // ADR-027: una sola fila; no se crea una segunda
        Optional<ConfiguracionEmail> actual = obtenerConfiguracion();
        if (actual.isPresent() && !actual.get().getIdConfiguracion().equals(configuracion.getIdConfiguracion())) {
            throw new IllegalStateException("Ya existe una configuración de email; el sistema opera con una sola. "
                    + "Actualice la existente en lugar de crear otra.");
        }

        // La máscara (o vacío) nunca se guarda como contraseña: se conserva la actual
        if (sinContrasenaNueva(configuracion.getSmtpPassword())) {
            configuracion.setSmtpPassword(actual.map(ConfiguracionEmail::getSmtpPassword).orElse(""));
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
        // La máscara (o vacío) nunca se guarda como contraseña: se conserva la actual (ADR-027)
        if (!sinContrasenaNueva(configuracion.getSmtpPassword())) {
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
            // Mismo servidor y parámetros que el envío de correos; la prueba no exige "activo" (se prueba antes
            // de habilitar el envío)
            JavaMailSenderImpl mailSender = SmtpMailSenderFactory.crear(configuracion);

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

    private static boolean sinContrasenaNueva(String password) {
        return password == null || password.isBlank() || MASCARA_PASSWORD.equals(password);
    }
}
