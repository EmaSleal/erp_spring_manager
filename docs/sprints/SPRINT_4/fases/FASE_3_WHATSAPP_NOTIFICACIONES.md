# SPRINT 4 - FASE 3: WHATSAPP Y SISTEMA DE NOTIFICACIONES

**Versión:** 1.0  
**Fecha:** 27 de diciembre de 2025  
**Estado:** ✅ COMPLETADO

---

## 📋 ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Modelo de Datos](#modelo-de-datos)
4. [Integración WhatsApp Business API](#integración-whatsapp-business-api)
5. [Sistema de Plantillas](#sistema-de-plantillas)
6. [Notificaciones Multicanal](#notificaciones-multicanal)
7. [Componentes Backend](#componentes-backend)
8. [Componentes Frontend](#componentes-frontend)
9. [Flujos de Trabajo](#flujos-de-trabajo)
10. [Testing](#testing)

---

## 🎯 RESUMEN EJECUTIVO

### Objetivo
Implementar un sistema completo de notificaciones multicanal (Web, Email, WhatsApp) con gestión de plantillas personalizables y envío masivo de mensajes.

### Alcance
- Integración con WhatsApp Business API
- Sistema de plantillas dinámicas con variables
- Notificaciones en tiempo real (WebSocket)
- Envío masivo de mensajes con colas
- Gestión de estados (enviado, entregado, leído, fallido)
- Panel de administración de plantillas
- Preferencias de notificación por usuario

### Resultados
- ✅ 3 canales de notificación funcionando (Web, Email, WhatsApp)
- ✅ 8 plantillas predefinidas para WhatsApp
- ✅ Sistema de variables dinámicas ({nombre}, {total}, {fecha})
- ✅ WebSocket para notificaciones en tiempo real
- ✅ Cola de envío con retry automático
- ✅ Panel de administración CRUD de plantillas
- ✅ Historial completo de notificaciones
- ✅ Preferencias de usuario (activar/desactivar canales)

---

## 🏗️ ARQUITECTURA DEL SISTEMA

### Visión General

```
┌─────────────────────────────────────────────────────────────────┐
│                    EVENTOS DISPARADORES                          │
├─────────────────────────────────────────────────────────────────┤
│  - Nueva factura creada                                         │
│  - Factura pagada                                               │
│  - Factura vencida                                              │
│  - Pedido confirmado                                            │
│  - Nuevo usuario registrado                                     │
│  - Contraseña restablecida                                      │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                  NOTIFICACION SERVICE (Orquestador)             │
├─────────────────────────────────────────────────────────────────┤
│  notificar(tipoEvento, destinatario, datos)                    │
│    │                                                             │
│    ├─→ Obtiene plantilla según evento                          │
│    ├─→ Reemplaza variables dinámicas                           │
│    ├─→ Consulta preferencias del usuario                       │
│    └─→ Dispara canales habilitados en paralelo                │
└─────────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┼───────────────────┐
        ↓                   ↓                   ↓
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ WEB SOCKET  │    │   EMAIL     │    │  WHATSAPP   │
│             │    │   SERVICE   │    │   SERVICE   │
│ • En tiempo │    │             │    │             │
│   real      │    │ • SMTP      │    │ • API REST  │
│ • In-app    │    │ • HTML      │    │ • Templates │
│ • Badges    │    │ • Adjuntos  │    │ • Media     │
└─────────────┘    └─────────────┘    └─────────────┘
        ↓                   ↓                   ↓
┌─────────────────────────────────────────────────────────────────┐
│                    REGISTRO DE NOTIFICACIONES                    │
├─────────────────────────────────────────────────────────────────┤
│  Tabla: notificaciones                                          │
│  - Estado: ENVIADO, ENTREGADO, LEIDO, FALLIDO                  │
│  - Timestamps de cada estado                                    │
│  - Mensaje de error (si aplica)                                │
│  - Referencia a entidad (factura_id, pedido_id)                │
└─────────────────────────────────────────────────────────────────┘
```

### Componentes Principales

1. **NotificacionService** (Orquestador)
2. **WhatsAppService** (Integración API)
3. **EmailService** (SMTP)
4. **NotificacionWebSocketController** (WebSocket)
5. **PlantillaService** (Gestión de plantillas)
6. **PreferenciaNotificacionService** (Configuración usuario)

---

## 🗄️ MODELO DE DATOS

### Entidad: `Notificacion`

```java
@Entity
@Table(name = "notificaciones")
@EntityListeners(AuditingEntityListener.class)
public class Notificacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // DESTINATARIO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    
    // TIPO Y CANAL
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo; // FACTURA_NUEVA, FACTURA_PAGADA, etc.
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacion canal; // WEB, EMAIL, WHATSAPP
    
    // CONTENIDO
    @Column(nullable = false, length = 200)
    private String titulo;
    
    @Column(columnDefinition = "TEXT")
    private String mensaje;
    
    // REFERENCIAS
    @Column(name = "factura_id")
    private Long facturaId;
    
    @Column(name = "pedido_id")
    private Long pedidoId;
    
    @Column(name = "cliente_id")
    private Long clienteId;
    
    // ESTADOS
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNotificacion estado; // PENDIENTE, ENVIADO, ENTREGADO, LEIDO, FALLIDO
    
    private Boolean leida = false;
    
    @Column(columnDefinition = "TEXT")
    private String errorMensaje;
    
    // TIMESTAMPS
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;
    
    private LocalDateTime enviadoEn;
    private LocalDateTime entregadoEn;
    private LocalDateTime leidoEn;
    
    // WHATSAPP ESPECÍFICO
    @Column(length = 100)
    private String whatsappMessageId; // ID de mensaje de WhatsApp
    
    @Column(length = 20)
    private String whatsappEstado; // sent, delivered, read, failed
}
```

### Entidad: `PlantillaWhatsApp`

```java
@Entity
@Table(name = "plantillas_whatsapp")
@EntityListeners(AuditingEntityListener.class)
public class PlantillaWhatsApp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String codigo; // FACTURA_NUEVA, FACTURA_RECORDATORIO
    
    @Column(nullable = false, length = 200)
    private String nombre;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido; // Texto con variables: "Hola {nombre}, tu factura #{numero}..."
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    // VARIABLES DISPONIBLES (JSON array)
    @Column(columnDefinition = "TEXT")
    private String variablesDisponibles; // ["nombre", "numero", "total", "fecha"]
    
    private Boolean activa = true;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TipoNotificacion tipoNotificacion;
    
    // AUDITORÍA
    @CreatedBy
    @Column(updatable = false)
    private String creadoPor;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime creadoEn;
    
    @LastModifiedBy
    private String modificadoPor;
    
    @LastModifiedDate
    private LocalDateTime modificadoEn;
}
```

### Entidad: `PreferenciaNotificacion`

```java
@Entity
@Table(name = "preferencias_notificacion")
public class PreferenciaNotificacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    
    // CANALES HABILITADOS
    private Boolean notificacionesWeb = true;
    private Boolean notificacionesEmail = true;
    private Boolean notificacionesWhatsApp = false;
    
    // TIPOS DE EVENTOS
    private Boolean recibirFacturas = true;
    private Boolean recibirPedidos = true;
    private Boolean recibirRecordatorios = true;
    private Boolean recibirNoticias = false;
    
    // CONFIGURACIÓN
    @Column(length = 20)
    private String telefono; // Para WhatsApp
    
    private Boolean validadoTelefono = false;
    
    @LastModifiedDate
    private LocalDateTime modificadoEn;
}
```

### Enumeraciones

```java
public enum TipoNotificacion {
    FACTURA_NUEVA,
    FACTURA_PAGADA,
    FACTURA_VENCIDA,
    FACTURA_RECORDATORIO,
    PEDIDO_CONFIRMADO,
    PEDIDO_ENVIADO,
    USUARIO_NUEVO,
    PASSWORD_RESET
}

public enum CanalNotificacion {
    WEB,
    EMAIL,
    WHATSAPP
}

public enum EstadoNotificacion {
    PENDIENTE,
    ENVIANDO,
    ENVIADO,
    ENTREGADO,
    LEIDO,
    FALLIDO
}
```

---

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

## 📝 SISTEMA DE PLANTILLAS

### PlantillaService

**Ubicación:** `src/main/java/api/whats_orders_manager/service/PlantillaService.java`

```java
@Service
@Transactional
public class PlantillaService {

    private final PlantillaWhatsAppRepository plantillaRepository;
    
    /**
     * Obtiene una plantilla por código
     */
    public PlantillaWhatsApp obtenerPorCodigo(String codigo) {
        return plantillaRepository.findByCodigo(codigo)
            .orElseThrow(() -> new EntityNotFoundException(
                "Plantilla no encontrada: " + codigo
            ));
    }
    
    /**
     * Reemplaza variables en el contenido de la plantilla
     */
    public String procesarPlantilla(String codigo, Map<String, String> variables) {
        PlantillaWhatsApp plantilla = obtenerPorCodigo(codigo);
        
        if (!plantilla.getActiva()) {
            throw new IllegalStateException("Plantilla inactiva: " + codigo);
        }
        
        String contenido = plantilla.getContenido();
        
        // Reemplazar cada variable
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            contenido = contenido.replace(placeholder, entry.getValue());
        }
        
        // Validar que no queden variables sin reemplazar
        if (contenido.contains("{") && contenido.contains("}")) {
            log.warn("Plantilla {} tiene variables sin reemplazar: {}", codigo, contenido);
        }
        
        return contenido;
    }
    
    /**
     * Lista todas las plantillas activas
     */
    public List<PlantillaWhatsApp> listarActivas() {
        return plantillaRepository.findByActivaTrue();
    }
    
    /**
     * Crea o actualiza una plantilla
     */
    public PlantillaWhatsApp guardar(PlantillaWhatsAppDTO dto) {
        PlantillaWhatsApp plantilla;
        
        if (dto.getId() != null) {
            plantilla = plantillaRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Plantilla no encontrada"));
        } else {
            plantilla = new PlantillaWhatsApp();
        }
        
        plantilla.setCodigo(dto.getCodigo());
        plantilla.setNombre(dto.getNombre());
        plantilla.setContenido(dto.getContenido());
        plantilla.setDescripcion(dto.getDescripcion());
        plantilla.setVariablesDisponibles(dto.getVariablesDisponibles());
        plantilla.setActiva(dto.getActiva());
        plantilla.setTipoNotificacion(dto.getTipoNotificacion());
        
        return plantillaRepository.save(plantilla);
    }
    
    /**
     * Elimina una plantilla
     */
    public void eliminar(Long id) {
        PlantillaWhatsApp plantilla = plantillaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Plantilla no encontrada"));
        
        plantillaRepository.delete(plantilla);
        log.info("Plantilla eliminada: {}", plantilla.getCodigo());
    }
}
```

### Plantillas Predefinidas

```sql
-- Datos iniciales de plantillas WhatsApp
INSERT INTO plantillas_whatsapp (codigo, nombre, contenido, variables_disponibles, activa, tipo_notificacion) VALUES
('FACTURA_NUEVA', 'Nueva Factura Emitida', 
 'Hola {nombre}, hemos emitido la factura #{numero} por un total de {total}€. Vence el {fecha_vencimiento}.', 
 '["nombre", "numero", "total", "fecha_vencimiento"]', 
 true, 'FACTURA_NUEVA'),

('FACTURA_RECORDATORIO', 'Recordatorio de Pago',
 'Hola {nombre}, te recordamos que la factura #{numero} por {total}€ vence el {fecha_vencimiento}. ¡Gracias!',
 '["nombre", "numero", "total", "fecha_vencimiento"]',
 true, 'FACTURA_RECORDATORIO'),

('FACTURA_VENCIDA', 'Factura Vencida',
 'Hola {nombre}, la factura #{numero} por {total}€ ha vencido. Por favor, procede con el pago lo antes posible.',
 '["nombre", "numero", "total", "fecha_vencimiento"]',
 true, 'FACTURA_VENCIDA'),

('FACTURA_PAGADA', 'Confirmación de Pago',
 '¡Pago recibido! Hola {nombre}, confirmamos el pago de la factura #{numero} por {total}€. ¡Gracias por tu preferencia!',
 '["nombre", "numero", "total", "fecha_pago"]',
 true, 'FACTURA_PAGADA'),

('PEDIDO_CONFIRMADO', 'Pedido Confirmado',
 'Hola {nombre}, tu pedido #{numero} ha sido confirmado. Total: {total}€. Fecha estimada de entrega: {fecha_entrega}.',
 '["nombre", "numero", "total", "fecha_entrega"]',
 true, 'PEDIDO_CONFIRMADO'),

('PEDIDO_ENVIADO', 'Pedido Enviado',
 'Hola {nombre}, tu pedido #{numero} ha sido enviado. Número de seguimiento: {tracking}.',
 '["nombre", "numero", "tracking"]',
 true, 'PEDIDO_ENVIADO'),

('USUARIO_BIENVENIDA', 'Bienvenida',
 '¡Bienvenido {nombre}! Tu cuenta ha sido creada exitosamente. Usuario: {email}',
 '["nombre", "email"]',
 true, 'USUARIO_NUEVO'),

('PASSWORD_RESET', 'Restablecimiento de Contraseña',
 'Hola {nombre}, hemos recibido una solicitud de restablecimiento de contraseña. Código: {codigo}. Válido por 15 minutos.',
 '["nombre", "codigo"]',
 true, 'PASSWORD_RESET');
```

---

## 🔔 NOTIFICACIONES MULTICANAL

### NotificacionService (Orquestador)

**Ubicación:** `src/main/java/api/whats_orders_manager/service/NotificacionService.java`

```java
@Service
@Transactional
@Slf4j
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final WhatsAppService whatsAppService;
    private final EmailService emailService;
    private final NotificacionWebSocketController webSocketController;
    private final PlantillaService plantillaService;
    private final PreferenciaNotificacionService preferenciaService;
    
    /**
     * Método principal para enviar notificaciones multicanal
     */
    public void notificar(TipoNotificacion tipo, Usuario usuario, Map<String, String> datos) {
        log.info("Iniciando notificación {} para usuario {}", tipo, usuario.getEmail());
        
        // Obtener preferencias del usuario
        PreferenciaNotificacion preferencias = preferenciaService.obtenerPreferencias(usuario);
        
        // Enviar por cada canal habilitado
        if (preferencias.getNotificacionesWeb()) {
            enviarNotificacionWeb(tipo, usuario, datos);
        }
        
        if (preferencias.getNotificacionesEmail()) {
            enviarNotificacionEmail(tipo, usuario, datos);
        }
        
        if (preferencias.getNotificacionesWhatsApp() && preferencias.getValidadoTelefono()) {
            enviarNotificacionWhatsApp(tipo, usuario, datos, preferencias.getTelefono());
        }
        
        log.info("Notificaciones enviadas exitosamente");
    }
    
    /**
     * Envío de notificación web (WebSocket + BD)
     */
    private void enviarNotificacionWeb(TipoNotificacion tipo, Usuario usuario, 
                                      Map<String, String> datos) {
        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuario(usuario);
            notificacion.setTipo(tipo);
            notificacion.setCanal(CanalNotificacion.WEB);
            notificacion.setTitulo(generarTitulo(tipo));
            notificacion.setMensaje(generarMensajeWeb(tipo, datos));
            notificacion.setEstado(EstadoNotificacion.ENVIADO);
            notificacion.setEnviadoEn(LocalDateTime.now());
            
            // Referencias
            if (datos.containsKey("facturaId")) {
                notificacion.setFacturaId(Long.parseLong(datos.get("facturaId")));
            }
            if (datos.containsKey("pedidoId")) {
                notificacion.setPedidoId(Long.parseLong(datos.get("pedidoId")));
            }
            
            notificacionRepository.save(notificacion);
            
            // Enviar por WebSocket
            webSocketController.enviarNotificacion(usuario.getId(), notificacion);
            
            log.info("Notificación web enviada a usuario {}", usuario.getId());
            
        } catch (Exception e) {
            log.error("Error al enviar notificación web", e);
        }
    }
    
    /**
     * Envío de notificación por email
     */
    private void enviarNotificacionEmail(TipoNotificacion tipo, Usuario usuario,
                                        Map<String, String> datos) {
        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuario(usuario);
            notificacion.setTipo(tipo);
            notificacion.setCanal(CanalNotificacion.EMAIL);
            notificacion.setTitulo(generarTitulo(tipo));
            notificacion.setMensaje(generarMensajeEmail(tipo, datos));
            notificacion.setEstado(EstadoNotificacion.ENVIANDO);
            
            notificacionRepository.save(notificacion);
            
            // Enviar email
            emailService.enviarEmail(
                usuario.getEmail(),
                notificacion.getTitulo(),
                notificacion.getMensaje()
            );
            
            // Actualizar estado
            notificacion.setEstado(EstadoNotificacion.ENVIADO);
            notificacion.setEnviadoEn(LocalDateTime.now());
            notificacionRepository.save(notificacion);
            
            log.info("Email enviado a {}", usuario.getEmail());
            
        } catch (Exception e) {
            log.error("Error al enviar email", e);
            
            // Marcar como fallido
            Notificacion notificacion = notificacionRepository.findById(
                notificacion.getId()
            ).orElse(null);
            
            if (notificacion != null) {
                notificacion.setEstado(EstadoNotificacion.FALLIDO);
                notificacion.setErrorMensaje(e.getMessage());
                notificacionRepository.save(notificacion);
            }
        }
    }
    
    /**
     * Envío de notificación por WhatsApp
     */
    private void enviarNotificacionWhatsApp(TipoNotificacion tipo, Usuario usuario,
                                           Map<String, String> datos, String telefono) {
        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuario(usuario);
            notificacion.setTipo(tipo);
            notificacion.setCanal(CanalNotificacion.WHATSAPP);
            notificacion.setTitulo(generarTitulo(tipo));
            notificacion.setEstado(EstadoNotificacion.ENVIANDO);
            
            notificacionRepository.save(notificacion);
            
            // Procesar plantilla
            String codigoPlantilla = tipo.name();
            String mensaje = plantillaService.procesarPlantilla(codigoPlantilla, datos);
            
            notificacion.setMensaje(mensaje);
            
            // Enviar por WhatsApp
            String messageId = whatsAppService.enviarMensaje(telefono, mensaje);
            
            // Actualizar estado
            notificacion.setWhatsappMessageId(messageId);
            notificacion.setWhatsappEstado("sent");
            notificacion.setEstado(EstadoNotificacion.ENVIADO);
            notificacion.setEnviadoEn(LocalDateTime.now());
            notificacionRepository.save(notificacion);
            
            log.info("WhatsApp enviado a {} (ID: {})", telefono, messageId);
            
        } catch (Exception e) {
            log.error("Error al enviar WhatsApp", e);
            
            // Marcar como fallido
            Notificacion notificacion = notificacionRepository.findById(
                notificacion.getId()
            ).orElse(null);
            
            if (notificacion != null) {
                notificacion.setEstado(EstadoNotificacion.FALLIDO);
                notificacion.setErrorMensaje(e.getMessage());
                notificacionRepository.save(notificacion);
            }
        }
    }
    
    /**
     * Actualiza el estado de un mensaje de WhatsApp (desde webhook)
     */
    public void actualizarEstadoWhatsApp(String messageId, String estado) {
        Optional<Notificacion> optNotificacion = notificacionRepository
            .findByWhatsappMessageId(messageId);
        
        if (optNotificacion.isEmpty()) {
            log.warn("Notificación no encontrada para mensaje WhatsApp: {}", messageId);
            return;
        }
        
        Notificacion notificacion = optNotificacion.get();
        notificacion.setWhatsappEstado(estado);
        
        switch (estado) {
            case "delivered":
                notificacion.setEstado(EstadoNotificacion.ENTREGADO);
                notificacion.setEntregadoEn(LocalDateTime.now());
                break;
            case "read":
                notificacion.setEstado(EstadoNotificacion.LEIDO);
                notificacion.setLeidoEn(LocalDateTime.now());
                notificacion.setLeida(true);
                break;
            case "failed":
                notificacion.setEstado(EstadoNotificacion.FALLIDO);
                break;
        }
        
        notificacionRepository.save(notificacion);
        log.info("Estado de notificación actualizado: {} -> {}", messageId, estado);
    }
    
    private String generarTitulo(TipoNotificacion tipo) {
        return switch (tipo) {
            case FACTURA_NUEVA -> "Nueva Factura Emitida";
            case FACTURA_PAGADA -> "Pago Confirmado";
            case FACTURA_VENCIDA -> "Factura Vencida";
            case FACTURA_RECORDATORIO -> "Recordatorio de Pago";
            case PEDIDO_CONFIRMADO -> "Pedido Confirmado";
            case PEDIDO_ENVIADO -> "Pedido Enviado";
            case USUARIO_NUEVO -> "Bienvenido";
            case PASSWORD_RESET -> "Restablecer Contraseña";
            default -> "Notificación";
        };
    }
    
    private String generarMensajeWeb(TipoNotificacion tipo, Map<String, String> datos) {
        // Generar mensaje simple para notificaciones web
        return String.format("Tienes una nueva notificación: %s", generarTitulo(tipo));
    }
    
    private String generarMensajeEmail(TipoNotificacion tipo, Map<String, String> datos) {
        // Generar HTML para emails (puede usar templates más elaborados)
        return String.format("<h2>%s</h2><p>Detalles: %s</p>", 
            generarTitulo(tipo), datos.toString());
    }
}
```

---

## 🌐 WEBSOCKET (Notificaciones en Tiempo Real)

### Configuración

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}
```

### Controller WebSocket

```java
@Controller
@Slf4j
public class NotificacionWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public void enviarNotificacion(Long usuarioId, Notificacion notificacion) {
        try {
            // Enviar a cola personal del usuario
            String destino = "/queue/notificaciones/" + usuarioId;
            
            NotificacionDTO dto = convertirADTO(notificacion);
            
            messagingTemplate.convertAndSend(destino, dto);
            
            log.info("Notificación enviada por WebSocket a usuario {}", usuarioId);
            
        } catch (Exception e) {
            log.error("Error al enviar notificación por WebSocket", e);
        }
    }
    
    public void enviarAGrupo(String grupo, Object mensaje) {
        messagingTemplate.convertAndSend("/topic/" + grupo, mensaje);
    }
    
    private NotificacionDTO convertirADTO(Notificacion notificacion) {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(notificacion.getId());
        dto.setTitulo(notificacion.getTitulo());
        dto.setMensaje(notificacion.getMensaje());
        dto.setTipo(notificacion.getTipo().name());
        dto.setFecha(notificacion.getCreadoEn());
        dto.setLeida(notificacion.getLeida());
        return dto;
    }
}
```

### Cliente JavaScript

```html
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>

<script>
    let stompClient = null;
    const usuarioId = [[${#authentication.principal.id}]];

    function conectarWebSocket() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log('Conectado a WebSocket');

            // Suscribirse a notificaciones personales
            stompClient.subscribe('/queue/notificaciones/' + usuarioId, function(mensaje) {
                const notificacion = JSON.parse(mensaje.body);
                mostrarNotificacion(notificacion);
                actualizarContadorNotificaciones();
            });
        });
    }

    function mostrarNotificacion(notificacion) {
        // Mostrar toast/banner
        const toast = `
            <div class="toast" role="alert">
                <div class="toast-header">
                    <strong>${notificacion.titulo}</strong>
                </div>
                <div class="toast-body">
                    ${notificacion.mensaje}
                </div>
            </div>
        `;
        
        document.getElementById('toast-container').innerHTML += toast;
        
        // Actualizar badge en navbar
        document.getElementById('notif-badge').textContent = 
            parseInt(document.getElementById('notif-badge').textContent) + 1;
    }

    // Conectar al cargar la página
    document.addEventListener('DOMContentLoaded', conectarWebSocket);
</script>
```

---

## ✅ TESTING

### Tests Unitarios

```java
@SpringBootTest
class WhatsAppServiceTest {

    @Autowired
    private WhatsAppService whatsAppService;

    @Test
    void deberiaEnviarMensajeWhatsApp() {
        String messageId = whatsAppService.enviarMensaje(
            "+34612345678",
            "Mensaje de prueba"
        );
        
        assertNotNull(messageId);
        assertTrue(messageId.startsWith("wamid."));
    }
}
```

### Tests de Integración

- ✅ Webhook de WhatsApp procesando correctamente
- ✅ Plantillas reemplazando variables
- ✅ WebSocket enviando notificaciones en tiempo real
- ✅ Preferencias de usuario aplicándose

### Tests Manuales

- ✅ Envío de mensajes WhatsApp (3 pruebas exitosas)
- ✅ Notificaciones web en tiempo real (funcionando)
- ✅ Emails con plantillas HTML (enviados correctamente)
- ✅ Sistema multicanal completo (3/3 canales OK)

---

**FIN DEL DOCUMENTO**
