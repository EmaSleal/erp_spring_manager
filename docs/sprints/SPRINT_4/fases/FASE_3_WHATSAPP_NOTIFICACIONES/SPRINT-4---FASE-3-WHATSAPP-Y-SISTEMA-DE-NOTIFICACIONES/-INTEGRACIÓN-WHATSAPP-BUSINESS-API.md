## 📱 INTEGRACIÓN WHATSAPP BUSINESS API

### Configuración

```yaml
# application.yml
whatsapp:
  api:
    url: https://graph.facebook.com/v18.0
    phone-number-id: ${WHATSAPP_PHONE_ID}
    access-token: ${WHATSAPP_TOKEN}
    verify-token: ${WHATSAPP_VERIFY_TOKEN}
  webhook:
    enabled: true
    path: /api/whatsapp/webhook
```

### WhatsAppService

**Ubicación:** `src/main/java/api/whats_orders_manager/service/WhatsAppService.java`

```java
@Service
@Slf4j
public class WhatsAppService {

    @Value("${whatsapp.api.url}")
    private String apiUrl;
    
    @Value("${whatsapp.api.phone-number-id}")
    private String phoneNumberId;
    
    @Value("${whatsapp.api.access-token}")
    private String accessToken;
    
    private final RestTemplate restTemplate;
    private final NotificacionRepository notificacionRepository;

    /**
     * Envía un mensaje de texto a WhatsApp
     */
    public String enviarMensaje(String destinatario, String mensaje) {
        String url = String.format("%s/%s/messages", apiUrl, phoneNumberId);
        
        // Limpiar número de teléfono (solo dígitos)
        String telefonoLimpio = destinatario.replaceAll("[^0-9]", "");
        
        // Construir payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", telefonoLimpio);
        payload.put("type", "text");
        payload.put("text", Map.of("body", mensaje));
        
        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
                
                if (messages != null && !messages.isEmpty()) {
                    String messageId = (String) messages.get(0).get("id");
                    log.info("Mensaje WhatsApp enviado exitosamente. ID: {}", messageId);
                    return messageId;
                }
            }
            
            throw new RuntimeException("Respuesta inesperada de WhatsApp API");
            
        } catch (Exception e) {
            log.error("Error al enviar mensaje de WhatsApp a {}", telefonoLimpio, e);
            throw new RuntimeException("Error al enviar WhatsApp: " + e.getMessage(), e);
        }
    }
    
    /**
     * Envía un mensaje usando plantilla de WhatsApp
     */
    public String enviarConPlantilla(String destinatario, String nombrePlantilla, 
                                    Map<String, String> parametros) {
        String url = String.format("%s/%s/messages", apiUrl, phoneNumberId);
        
        String telefonoLimpio = destinatario.replaceAll("[^0-9]", "");
        
        // Construir componentes de la plantilla
        List<Map<String, Object>> components = new ArrayList<>();
        
        if (parametros != null && !parametros.isEmpty()) {
            List<Map<String, String>> parameters = parametros.entrySet().stream()
                .map(entry -> Map.of("type", "text", "text", entry.getValue()))
                .collect(Collectors.toList());
            
            components.add(Map.of(
                "type", "body",
                "parameters", parameters
            ));
        }
        
        Map<String, Object> payload = Map.of(
            "messaging_product", "whatsapp",
            "to", telefonoLimpio,
            "type", "template",
            "template", Map.of(
                "name", nombrePlantilla,
                "language", Map.of("code", "es"),
                "components", components
            )
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
                
                if (messages != null && !messages.isEmpty()) {
                    return (String) messages.get(0).get("id");
                }
            }
            
            throw new RuntimeException("Error en respuesta de WhatsApp");
            
        } catch (Exception e) {
            log.error("Error al enviar plantilla WhatsApp", e);
            throw new RuntimeException("Error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Envía mensaje con archivo adjunto (imagen, PDF, etc.)
     */
    public String enviarConArchivo(String destinatario, String urlArchivo, 
                                   String tipoMime, String caption) {
        String url = String.format("%s/%s/messages", apiUrl, phoneNumberId);
        
        String telefonoLimpio = destinatario.replaceAll("[^0-9]", "");
        
        // Determinar tipo de medio
        String tipoMedio = determinarTipoMedio(tipoMime);
        
        Map<String, Object> mediaObject = new HashMap<>();
        mediaObject.put("link", urlArchivo);
        if (caption != null && !caption.isEmpty()) {
            mediaObject.put("caption", caption);
        }
        
        Map<String, Object> payload = Map.of(
            "messaging_product", "whatsapp",
            "to", telefonoLimpio,
            "type", tipoMedio,
            tipoMedio, mediaObject
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
                
                if (messages != null && !messages.isEmpty()) {
                    return (String) messages.get(0).get("id");
                }
            }
            
            throw new RuntimeException("Error en respuesta de WhatsApp");
            
        } catch (Exception e) {
            log.error("Error al enviar archivo por WhatsApp", e);
            throw new RuntimeException("Error: " + e.getMessage(), e);
        }
    }
    
    private String determinarTipoMedio(String tipoMime) {
        if (tipoMime.startsWith("image/")) return "image";
        if (tipoMime.equals("application/pdf")) return "document";
        if (tipoMime.startsWith("video/")) return "video";
        if (tipoMime.startsWith("audio/")) return "audio";
        return "document";
    }
    
    /**
     * Consulta el estado de un mensaje
     */
    public Map<String, String> consultarEstadoMensaje(String messageId) {
        String url = String.format("%s/%s", apiUrl, messageId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, request, Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                
                return Map.of(
                    "status", (String) body.getOrDefault("status", "unknown"),
                    "timestamp", String.valueOf(body.getOrDefault("timestamp", ""))
                );
            }
            
        } catch (Exception e) {
            log.error("Error al consultar estado de mensaje WhatsApp", e);
        }
        
        return Map.of("status", "unknown");
    }
}
```

### Webhook de WhatsApp

**Ubicación:** `src/main/java/api/whats_orders_manager/controller/api/WhatsAppWebhookController.java`

```java
@RestController
@RequestMapping("/api/whatsapp/webhook")
@Slf4j
public class WhatsAppWebhookController {

    private final NotificacionService notificacionService;
    
    @Value("${whatsapp.api.verify-token}")
    private String verifyToken;

    /**
     * Verificación del webhook (GET)
     */
    @GetMapping
    public ResponseEntity<String> verificarWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("Webhook de WhatsApp verificado exitosamente");
            return ResponseEntity.ok(challenge);
        }
        
        log.warn("Intento de verificación de webhook fallido");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
    }
    
    /**
     * Recepción de actualizaciones de estado (POST)
     */
    @PostMapping
    public ResponseEntity<String> recibirActualizacion(@RequestBody Map<String, Object> payload) {
        log.info("Webhook recibido de WhatsApp: {}", payload);
        
        try {
            List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
            
            if (entries == null || entries.isEmpty()) {
                return ResponseEntity.ok("OK");
            }
            
            for (Map<String, Object> entry : entries) {
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                
                if (changes == null) continue;
                
                for (Map<String, Object> change : changes) {
                    Map<String, Object> value = (Map<String, Object>) change.get("value");
                    
                    if (value == null) continue;
                    
                    // Procesar estados de mensajes
                    List<Map<String, Object>> statuses = (List<Map<String, Object>>) value.get("statuses");
                    
                    if (statuses != null) {
                        for (Map<String, Object> status : statuses) {
                            procesarEstadoMensaje(status);
                        }
                    }
                    
                    // Procesar mensajes entrantes
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                    
                    if (messages != null) {
                        for (Map<String, Object> message : messages) {
                            procesarMensajeEntrante(message);
                        }
                    }
                }
            }
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error al procesar webhook de WhatsApp", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }
    
    private void procesarEstadoMensaje(Map<String, Object> status) {
        String messageId = (String) status.get("id");
        String estado = (String) status.get("status"); // sent, delivered, read, failed
        
        log.info("Estado de mensaje WhatsApp: {} -> {}", messageId, estado);
        
        // Actualizar notificación en BD
        notificacionService.actualizarEstadoWhatsApp(messageId, estado);
    }
    
    private void procesarMensajeEntrante(Map<String, Object> message) {
        String from = (String) message.get("from");
        String messageId = (String) message.get("id");
        
        Map<String, Object> text = (Map<String, Object>) message.get("text");
        String body = text != null ? (String) text.get("body") : "";
        
        log.info("Mensaje entrante de {}: {}", from, body);
        
        // Aquí se puede implementar lógica de respuestas automáticas
        // Por ejemplo: responder a comandos como "FACTURA", "ESTADO", etc.
    }
}
```

---

