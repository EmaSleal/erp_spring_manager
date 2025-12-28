package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.dto.whatsapp.MetaWebhookRequest;
import api.astro.whats_orders_manager.models.MensajeWhatsApp;
import api.astro.whats_orders_manager.models.WebhookLog;
import api.astro.whats_orders_manager.repositories.MensajeWhatsAppRepository;
import api.astro.whats_orders_manager.repositories.WebhookLogRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para procesar webhooks de Meta WhatsApp Business API
 * Maneja mensajes entrantes, actualizaciones de estado y errores
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.3
 */
@Service
@Slf4j
public class WebhookWhatsAppService {
    
    private final MensajeWhatsAppRepository mensajeRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final ObjectMapper objectMapper;
    
    public WebhookWhatsAppService(
            MensajeWhatsAppRepository mensajeRepository,
            WebhookLogRepository webhookLogRepository,
            ObjectMapper objectMapper) {
        this.mensajeRepository = mensajeRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Procesa un webhook recibido de Meta
     * 
     * @param webhookRequest Request del webhook
     */
    @Transactional
    public void procesarWebhook(MetaWebhookRequest webhookRequest) {
        log.info("Procesando webhook de WhatsApp");
        
        try {
            // Registrar webhook
            registrarWebhook(webhookRequest);
            
            // Procesar cada entrada
            if (webhookRequest.getEntry() != null) {
                for (MetaWebhookRequest.Entry entry : webhookRequest.getEntry()) {
                    procesarEntry(entry);
                }
            }
            
            log.info("Webhook procesado exitosamente");
            
        } catch (Exception e) {
            log.error("Error al procesar webhook", e);
            throw e;
        }
    }
    
    /**
     * Procesa una entrada del webhook
     */
    private void procesarEntry(MetaWebhookRequest.Entry entry) {
        if (entry.getChanges() == null) {
            return;
        }
        
        for (MetaWebhookRequest.Change change : entry.getChanges()) {
            MetaWebhookRequest.Value value = change.getValue();
            
            if (value == null) {
                continue;
            }
            
            // Procesar mensajes entrantes
            if (value.getMessages() != null && !value.getMessages().isEmpty()) {
                procesarMensajesEntrantes(value);
            }
            
            // Procesar actualizaciones de estado
            if (value.getStatuses() != null && !value.getStatuses().isEmpty()) {
                procesarActualizacionesEstado(value);
            }
        }
    }
    
    /**
     * Procesa mensajes entrantes de clientes
     */
    private void procesarMensajesEntrantes(MetaWebhookRequest.Value value) {
        for (MetaWebhookRequest.Message message : value.getMessages()) {
            try {
                log.info("Procesando mensaje entrante - ID: {}, From: {}", 
                        message.getId(), message.getFrom());
                
                // Crear mensaje en BD
                MensajeWhatsApp mensajeWhatsApp = MensajeWhatsApp.builder()
                        .idMensajeWhatsapp(message.getId())
                        .telefono(message.getFrom())
                        .tipo(MensajeWhatsApp.TipoMensaje.RECIBIDO)
                        .estado(MensajeWhatsApp.EstadoMensaje.ENTREGADO)
                        .fechaEnvio(LocalDateTime.now())
                        .build();
                
                // Extraer contenido según tipo
                if (message.getType() != null) {
                    switch (message.getType()) {
                        case "text":
                            if (message.getText() != null) {
                                mensajeWhatsApp.setMensaje(message.getText().getBody());
                            }
                            break;
                        case "image":
                            mensajeWhatsApp.setMensaje("[Imagen recibida]");
                            break;
                        case "document":
                            mensajeWhatsApp.setMensaje("[Documento recibido]");
                            break;
                        case "audio":
                            mensajeWhatsApp.setMensaje("[Audio recibido]");
                            break;
                        case "video":
                            mensajeWhatsApp.setMensaje("[Video recibido]");
                            break;
                        default:
                            mensajeWhatsApp.setMensaje("[Mensaje de tipo: " + message.getType() + "]");
                    }
                }
                
                // Guardar metadata si existe
                if (value.getMetadata() != null) {
                    try {
                        String metadata = objectMapper.writeValueAsString(value.getMetadata());
                        mensajeWhatsApp.setMetadata(metadata);
                    } catch (Exception e) {
                        log.warn("No se pudo serializar metadata", e);
                    }
                }
                
                mensajeRepository.save(mensajeWhatsApp);
                log.info("Mensaje entrante guardado exitosamente");
                
            } catch (Exception e) {
                log.error("Error al procesar mensaje entrante: {}", message.getId(), e);
            }
        }
    }
    
    /**
     * Procesa actualizaciones de estado de mensajes enviados
     */
    private void procesarActualizacionesEstado(MetaWebhookRequest.Value value) {
        for (MetaWebhookRequest.Status status : value.getStatuses()) {
            try {
                String messageId = status.getId();
                String statusType = status.getStatus();
                
                log.info("Procesando actualización de estado - Message ID: {}, Status: {}", 
                        messageId, statusType);
                
                // Buscar mensaje en BD
                Optional<MensajeWhatsApp> mensajeOpt = mensajeRepository.findByIdMensajeWhatsapp(messageId);
                
                if (mensajeOpt.isEmpty()) {
                    log.warn("No se encontró mensaje con ID: {}", messageId);
                    continue;
                }
                
                MensajeWhatsApp mensaje = mensajeOpt.get();
                
                // Actualizar estado según tipo
                switch (statusType.toLowerCase()) {
                    case "sent":
                        mensaje.setEstado(MensajeWhatsApp.EstadoMensaje.ENVIADO);
                        break;
                    case "delivered":
                        mensaje.setEstado(MensajeWhatsApp.EstadoMensaje.ENTREGADO);
                        if (status.getTimestamp() != null) {
                            mensaje.setFechaEntrega(LocalDateTime.now());
                        }
                        break;
                    case "read":
                        mensaje.setEstado(MensajeWhatsApp.EstadoMensaje.LEIDO);
                        if (status.getTimestamp() != null) {
                            mensaje.setFechaLectura(LocalDateTime.now());
                        }
                        break;
                    case "failed":
                        mensaje.setEstado(MensajeWhatsApp.EstadoMensaje.FALLIDO);
                        if (status.getErrors() != null && !status.getErrors().isEmpty()) {
                            MetaWebhookRequest.Error error = status.getErrors().get(0);
                            mensaje.setError(String.format("Error %d: %s", 
                                    error.getCode(), error.getTitle()));
                        }
                        break;
                    default:
                        log.warn("Estado desconocido: {}", statusType);
                }
                
                mensajeRepository.save(mensaje);
                log.info("Estado actualizado exitosamente para mensaje: {}", messageId);
                
            } catch (Exception e) {
                log.error("Error al procesar actualización de estado", e);
            }
        }
    }
    
    /**
     * Registra el webhook en la base de datos para auditoría
     */
    private void registrarWebhook(MetaWebhookRequest webhookRequest) {
        try {
            String payload = objectMapper.writeValueAsString(webhookRequest);
            
            WebhookLog webhookLog = new WebhookLog();
            webhookLog.setWholeMessage(payload);
            webhookLog.setTimestamp(java.sql.Timestamp.valueOf(LocalDateTime.now()));
            
            // Extraer primer mensaje si existe para logging
            if (webhookRequest.getEntry() != null && !webhookRequest.getEntry().isEmpty()) {
                MetaWebhookRequest.Entry entry = webhookRequest.getEntry().get(0);
                if (entry.getChanges() != null && !entry.getChanges().isEmpty()) {
                    MetaWebhookRequest.Value value = entry.getChanges().get(0).getValue();
                    if (value != null && value.getMessages() != null && !value.getMessages().isEmpty()) {
                        MetaWebhookRequest.Message msg = value.getMessages().get(0);
                        webhookLog.setMessageId(msg.getId());
                        webhookLog.setPhoneNumber(msg.getFrom());
                        if (msg.getText() != null) {
                            webhookLog.setMessageBody(msg.getText().getBody());
                        }
                    }
                }
            }
            
            webhookLogRepository.save(webhookLog);
            
        } catch (Exception e) {
            log.error("Error al registrar webhook", e);
            // No lanzar excepción para no interrumpir el procesamiento
        }
    }
    
    /**
     * Obtiene todos los mensajes recibidos de un teléfono
     * 
     * @param telefono Número de teléfono
     * @return Lista de mensajes recibidos
     */
    public List<MensajeWhatsApp> obtenerMensajesRecibidos(String telefono) {
        return mensajeRepository.findByTipoAndEstado(
                MensajeWhatsApp.TipoMensaje.RECIBIDO,
                MensajeWhatsApp.EstadoMensaje.ENTREGADO
        ).stream()
                .filter(m -> m.getTelefono().equals(telefono))
                .toList();
    }
    
    /**
     * Obtiene estadísticas de webhooks procesados
     * 
     * @param fechaInicio Fecha de inicio
     * @param fechaFin Fecha de fin
     * @return Cantidad de webhooks procesados
     */
    public long obtenerEstadisticasWebhooks(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        java.sql.Timestamp inicio = java.sql.Timestamp.valueOf(fechaInicio);
        java.sql.Timestamp fin = java.sql.Timestamp.valueOf(fechaFin);
        return webhookLogRepository.countByTimestampBetween(inicio, fin);
    }
}
