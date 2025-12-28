package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.dto.whatsapp.*;
import api.astro.whats_orders_manager.models.MensajeWhatsApp;
import api.astro.whats_orders_manager.models.PlantillaWhatsApp;
import api.astro.whats_orders_manager.repositories.MensajeWhatsAppRepository;
import api.astro.whats_orders_manager.repositories.PlantillaWhatsAppRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio principal para la integración con Meta WhatsApp Business API
 * Maneja el envío de mensajes, plantillas y documentos
 * 
 * @author EmaSleal
 * @version 1.0
 * @since Sprint 3 - Fase 1.3
 */
@Service
@Slf4j
public class WhatsAppService {
    
    private final RestTemplate restTemplate;
    private final MensajeWhatsAppRepository mensajeRepository;
    private final PlantillaWhatsAppRepository plantillaRepository;
    
    @Value("${whatsapp.api.url}")
    private String apiUrl;
    
    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;
    
    @Value("${whatsapp.access-token}")
    private String accessToken;
    
    @Value("${whatsapp.api.version}")
    private String apiVersion;
    
    public WhatsAppService(
            RestTemplate restTemplate,
            MensajeWhatsAppRepository mensajeRepository,
            PlantillaWhatsAppRepository plantillaRepository) {
        this.restTemplate = restTemplate;
        this.mensajeRepository = mensajeRepository;
        this.plantillaRepository = plantillaRepository;
    }
    
    /**
     * Envía un mensaje de texto simple a un número de WhatsApp
     * 
     * @param telefono Número en formato internacional (+525512345678)
     * @param mensaje Contenido del mensaje
     * @param idUsuario ID del usuario relacionado con el mensaje
     * @return MensajeWhatsApp guardado
     */
    @Transactional
    public MensajeWhatsApp enviarMensajeTexto(String telefono, String mensaje, Integer idUsuario) {
        log.info("Enviando mensaje de texto a: {}", telefono);
        
        try {
            // Crear request
            EnviarMensajeRequest request = EnviarMensajeRequest.builder()
                    .messagingProduct("whatsapp")
                    .recipientType("individual")
                    .to(telefono)
                    .type("text")
                    .text(EnviarMensajeRequest.TextContent.builder()
                            .body(mensaje)
                            .previewUrl(false)
                            .build())
                    .build();
            
            // Guardar mensaje como PENDIENTE
            MensajeWhatsApp mensajeWhatsApp = guardarMensajePendiente(telefono, mensaje, idUsuario);
            
            // Enviar a Meta API
            EnviarMensajeResponse response = enviarAMetaApi(request);
            
            // Actualizar con respuesta
            if (response.isExitoso()) {
                mensajeWhatsApp.setIdMensajeWhatsapp(response.getMessageId());
                mensajeWhatsApp.setEstado(MensajeWhatsApp.EstadoMensaje.ENVIADO);
                log.info("Mensaje enviado exitosamente. ID: {}", response.getMessageId());
            } else {
                mensajeWhatsApp.setEstado(MensajeWhatsApp.EstadoMensaje.FALLIDO);
                mensajeWhatsApp.setError("No se recibió ID de mensaje");
                log.error("Error al enviar mensaje: No se recibió ID");
            }
            
            return mensajeRepository.save(mensajeWhatsApp);
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error HTTP al enviar mensaje: {}", e.getMessage());
            return guardarMensajeFallido(telefono, mensaje, idUsuario, e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al enviar mensaje", e);
            return guardarMensajeFallido(telefono, mensaje, idUsuario, e.getMessage());
        }
    }
    
    /**
     * Envía un mensaje usando una plantilla aprobada
     * 
     * @param telefono Número destino
     * @param nombrePlantilla Nombre de la plantilla
     * @param parametros Parámetros de la plantilla
     * @param idUsuario ID del usuario
     * @return MensajeWhatsApp guardado
     */
    @Transactional
    public MensajeWhatsApp enviarMensajePlantilla(
            String telefono, 
            String nombrePlantilla, 
            List<String> parametros,
            Integer idUsuario) {
        
        log.info("Enviando mensaje con plantilla '{}' a: {}", nombrePlantilla, telefono);
        
        try {
            // Buscar plantilla
            PlantillaWhatsApp plantilla = plantillaRepository.findByNombre(nombrePlantilla)
                    .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada: " + nombrePlantilla));
            
            if (!plantilla.estaListaParaUsar()) {
                throw new IllegalStateException("La plantilla no está lista para usar: " + nombrePlantilla);
            }
            
            // Construir request con plantilla
            EnviarMensajeRequest request = construirRequestPlantilla(telefono, plantilla, parametros);
            
            // Guardar mensaje como PENDIENTE
            String contenidoMensaje = String.format("Plantilla: %s", nombrePlantilla);
            MensajeWhatsApp mensajeWhatsApp = guardarMensajePendiente(telefono, contenidoMensaje, idUsuario);
            
            // Enviar a Meta API
            EnviarMensajeResponse response = enviarAMetaApi(request);
            
            // Actualizar estado
            if (response.isExitoso()) {
                mensajeWhatsApp.setIdMensajeWhatsapp(response.getMessageId());
                mensajeWhatsApp.setEstado(MensajeWhatsApp.EstadoMensaje.ENVIADO);
                log.info("Plantilla enviada exitosamente. ID: {}", response.getMessageId());
            } else {
                mensajeWhatsApp.setEstado(MensajeWhatsApp.EstadoMensaje.FALLIDO);
                mensajeWhatsApp.setError("No se recibió ID de mensaje");
            }
            
            return mensajeRepository.save(mensajeWhatsApp);
            
        } catch (Exception e) {
            log.error("Error al enviar plantilla", e);
            return guardarMensajeFallido(telefono, "Plantilla: " + nombrePlantilla, idUsuario, e.getMessage());
        }
    }
    
    /**
     * Envía un documento PDF por WhatsApp
     * 
     * @param telefono Número destino
     * @param urlDocumento URL pública del documento
     * @param nombreArchivo Nombre del archivo
     * @param caption Descripción opcional
     * @param idUsuario ID del usuario
     * @return MensajeWhatsApp guardado
     */
    @Transactional
    public MensajeWhatsApp enviarDocumento(
            String telefono,
            String urlDocumento,
            String nombreArchivo,
            String caption,
            Integer idUsuario) {
        
        log.info("Enviando documento '{}' a: {}", nombreArchivo, telefono);
        
        try {
            // Crear request para documento
            EnviarMensajeRequest request = EnviarMensajeRequest.builder()
                    .messagingProduct("whatsapp")
                    .recipientType("individual")
                    .to(telefono)
                    .type("document")
                    .document(EnviarMensajeRequest.DocumentContent.builder()
                            .link(urlDocumento)
                            .filename(nombreArchivo)
                            .caption(caption)
                            .build())
                    .build();
            
            // Guardar mensaje como PENDIENTE
            String contenidoMensaje = String.format("Documento: %s", nombreArchivo);
            MensajeWhatsApp mensajeWhatsApp = guardarMensajePendiente(telefono, contenidoMensaje, idUsuario);
            
            // Enviar a Meta API
            EnviarMensajeResponse response = enviarAMetaApi(request);
            
            // Actualizar estado
            if (response.isExitoso()) {
                mensajeWhatsApp.setIdMensajeWhatsapp(response.getMessageId());
                mensajeWhatsApp.setEstado(MensajeWhatsApp.EstadoMensaje.ENVIADO);
                log.info("Documento enviado exitosamente. ID: {}", response.getMessageId());
            } else {
                mensajeWhatsApp.setEstado(MensajeWhatsApp.EstadoMensaje.FALLIDO);
                mensajeWhatsApp.setError("No se recibió ID de mensaje");
            }
            
            return mensajeRepository.save(mensajeWhatsApp);
            
        } catch (Exception e) {
            log.error("Error al enviar documento", e);
            return guardarMensajeFallido(telefono, "Documento: " + nombreArchivo, idUsuario, e.getMessage());
        }
    }
    
    /**
     * Obtiene el historial de mensajes de un usuario
     * 
     * @param idUsuario ID del usuario
     * @return Lista de mensajes
     */
    public List<MensajeWhatsApp> obtenerHistorialUsuario(Integer idUsuario) {
        return mensajeRepository.findByIdUsuarioOrderByFechaEnvioDesc(idUsuario);
    }
    
    /**
     * Obtiene mensajes recientes de un teléfono
     * 
     * @param telefono Número de teléfono
     * @return Lista de últimos 10 mensajes
     */
    public List<MensajeWhatsApp> obtenerMensajesRecientes(String telefono) {
        return mensajeRepository.findTop10ByTelefonoOrderByFechaEnvioDesc(telefono);
    }
    
    /**
     * Reintenta enviar mensajes fallidos
     * 
     * @return Cantidad de mensajes reenviados
     */
    @Transactional
    public int reintentarMensajesFallidos() {
        log.info("Reintentando mensajes fallidos...");
        
        LocalDateTime hace5Minutos = LocalDateTime.now().minusMinutes(5);
        List<MensajeWhatsApp> mensajesPendientes = mensajeRepository.findMensajesPendientes(hace5Minutos);
        
        int exitosos = 0;
        for (MensajeWhatsApp mensaje : mensajesPendientes) {
            try {
                // Reenviar mensaje
                MensajeWhatsApp resultado = enviarMensajeTexto(
                        mensaje.getTelefono(),
                        mensaje.getMensaje(),
                        mensaje.getIdUsuario()
                );
                
                if (resultado.getEstado() == MensajeWhatsApp.EstadoMensaje.ENVIADO) {
                    exitosos++;
                }
            } catch (Exception e) {
                log.error("Error al reintentar mensaje {}: {}", mensaje.getIdMensaje(), e.getMessage());
            }
        }
        
        log.info("Mensajes reenviados exitosamente: {}/{}", exitosos, mensajesPendientes.size());
        return exitosos;
    }
    
    // ========================================
    // MÉTODOS PRIVADOS
    // ========================================
    
    /**
     * Envía el request a Meta WhatsApp API
     */
    private EnviarMensajeResponse enviarAMetaApi(EnviarMensajeRequest request) {
        String url = String.format("%s/%s/%s/messages", apiUrl, apiVersion, phoneNumberId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        
        HttpEntity<EnviarMensajeRequest> entity = new HttpEntity<>(request, headers);
        
        log.debug("Enviando request a Meta API: {}", url);
        
        ResponseEntity<EnviarMensajeResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                EnviarMensajeResponse.class
        );
        
        return response.getBody();
    }
    
    /**
     * Construye request para envío con plantilla
     */
    private EnviarMensajeRequest construirRequestPlantilla(
            String telefono,
            PlantillaWhatsApp plantilla,
            List<String> parametros) {
        
        // Construir componente con parámetros
        List<EnviarMensajeRequest.Parameter> params = parametros.stream()
                .map(param -> EnviarMensajeRequest.Parameter.builder()
                        .type("text")
                        .text(param)
                        .build())
                .toList();
        
        EnviarMensajeRequest.Component bodyComponent = EnviarMensajeRequest.Component.builder()
                .type("body")
                .parameters(params)
                .build();
        
        return EnviarMensajeRequest.builder()
                .messagingProduct("whatsapp")
                .recipientType("individual")
                .to(telefono)
                .type("template")
                .template(EnviarMensajeRequest.TemplateContent.builder()
                        .name(plantilla.getCodigoMeta())
                        .language(EnviarMensajeRequest.Language.builder()
                                .code(plantilla.getIdioma())
                                .build())
                        .components(List.of(bodyComponent))
                        .build())
                .build();
    }
    
    /**
     * Guarda un mensaje en estado PENDIENTE
     */
    private MensajeWhatsApp guardarMensajePendiente(String telefono, String mensaje, Integer idUsuario) {
        MensajeWhatsApp mensajeWhatsApp = MensajeWhatsApp.builder()
                .telefono(telefono)
                .mensaje(mensaje)
                .tipo(MensajeWhatsApp.TipoMensaje.ENVIADO)
                .estado(MensajeWhatsApp.EstadoMensaje.PENDIENTE)
                .idUsuario(idUsuario)
                .fechaEnvio(LocalDateTime.now())
                .build();
        
        return mensajeRepository.save(mensajeWhatsApp);
    }
    
    /**
     * Guarda un mensaje en estado FALLIDO
     */
    private MensajeWhatsApp guardarMensajeFallido(String telefono, String mensaje, Integer idUsuario, String error) {
        MensajeWhatsApp mensajeWhatsApp = MensajeWhatsApp.builder()
                .telefono(telefono)
                .mensaje(mensaje)
                .tipo(MensajeWhatsApp.TipoMensaje.ENVIADO)
                .estado(MensajeWhatsApp.EstadoMensaje.FALLIDO)
                .idUsuario(idUsuario)
                .fechaEnvio(LocalDateTime.now())
                .error(error)
                .build();
        
        return mensajeRepository.save(mensajeWhatsApp);
    }
}
