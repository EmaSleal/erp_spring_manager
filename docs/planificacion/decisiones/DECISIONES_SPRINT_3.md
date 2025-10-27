# 🔧 DECISIONES TÉCNICAS - SPRINT 3

**Proyecto:** WhatsApp Orders Manager  
**Sprint:** 3  
**Fecha:** 20 de octubre de 2025  
**Estado:** 📋 EN DELIBERACIÓN

---

## 📋 ÍNDICE

1. [Decisión 1: WhatsApp API - Twilio vs. Meta](#decisión-1-whatsapp-api)
2. [Decisión 2: Tipos de Cambio - Manual vs. API](#decisión-2-tipos-de-cambio)
3. [Decisión 3: Chart.js vs. Alternativas](#decisión-3-librería-de-gráficas)
4. [Decisión 4: Almacenamiento de Mensajes WhatsApp](#decisión-4-almacenamiento-mensajes)
5. [Decisión 5: Arquitectura de Webhooks](#decisión-5-arquitectura-webhooks)
6. [Decisión 6: Divisa Maestra - Validación](#decisión-6-divisa-maestra)
7. [Decisión 7: Caché del Dashboard](#decisión-7-caché-dashboard)

---

## DECISIÓN 1: WhatsApp API

### ❓ Problema
Necesitamos integrar WhatsApp para enviar mensajes automáticos a clientes. Existen dos opciones principales:
1. **Twilio WhatsApp API**
2. **Meta WhatsApp Business API**

### 🎯 Criterios de Decisión
- Costo mensual y por mensaje
- Facilidad de implementación
- Documentación y soporte
- Funcionalidades disponibles
- Tiempo de aprobación
- Escalabilidad

### 📊 Comparativa

| Aspecto | Twilio WhatsApp API | Meta WhatsApp Business API |
|---------|---------------------|----------------------------|
| **Costo Setup** | $0 (gratis) | $0 (gratis) |
| **Costo por mensaje** | $0.005 - $0.01 USD | $0.0042 - $0.0097 USD (depende volumen) |
| **Mensajes incluidos** | Ninguno | 1,000/mes gratis (conversaciones) |
| **Facilidad de implementación** | ⭐⭐⭐⭐⭐ Muy fácil | ⭐⭐⭐ Moderada |
| **Tiempo de aprobación** | Inmediato | 1-7 días |
| **Documentación** | Excelente | Buena |
| **SDK Java** | Sí (oficial) | Sí (oficial) |
| **Webhooks** | Sí | Sí |
| **Plantillas** | Sí (precargadas) | Sí (requiere aprobación) |
| **Límites de envío** | 1,000 msg/s | 80 msg/s (puede aumentar) |
| **Soporte** | Excelente (24/7) | Moderado |
| **Dependencia** | Tercero (Twilio) | Directo con Meta |
| **Extras** | SMS, Voice, Video, Email | Solo WhatsApp |

### 💰 Análisis de Costos

#### Escenario 1: 1,000 mensajes/mes
- **Twilio:** $5 - $10 USD/mes
- **Meta:** $0 (dentro de 1,000 gratis)
- **Ganador:** Meta 🏆

#### Escenario 2: 5,000 mensajes/mes
- **Twilio:** $25 - $50 USD/mes
- **Meta:** ~$17 - $39 USD/mes
- **Ganador:** Meta 🏆

#### Escenario 3: 50,000 mensajes/mes
- **Twilio:** $250 - $500 USD/mes
- **Meta:** ~$170 - $390 USD/mes (con descuento por volumen)
- **Ganador:** Meta 🏆

### ⚖️ Ventajas y Desventajas

#### Twilio WhatsApp API

**Ventajas:**
- ✅ Implementación muy rápida (< 1 día)
- ✅ Documentación excelente
- ✅ No requiere aprobación de Meta
- ✅ Sandbox para pruebas inmediato
- ✅ Soporte técnico premium
- ✅ Integración con otros canales (SMS, Email)

**Desventajas:**
- ❌ Costo por mensaje más alto
- ❌ Dependencia de tercero (Twilio)
- ❌ No tiene mensajes gratis
- ❌ Plantillas deben ser enviadas a aprobación vía Twilio

#### Meta WhatsApp Business API

**Ventajas:**
- ✅ Costo por mensaje más bajo
- ✅ 1,000 conversaciones gratis/mes
- ✅ Directo con Meta (sin intermediarios)
- ✅ Escalabilidad mejor
- ✅ Marca verificada de WhatsApp

**Desventajas:**
- ❌ Proceso de aprobación (1-7 días)
- ❌ Configuración más compleja
- ❌ Requiere verificar negocio
- ❌ Plantillas requieren aprobación
- ❌ Curva de aprendizaje mayor

### 🎲 DECISIÓN FINAL

**OPCIÓN ELEGIDA:** ⭐ **Meta WhatsApp Business API**

**Justificación:**
1. **Costo:** 1,000 conversaciones gratis/mes vs. $0 en Twilio. Ahorro de $5-10/mes inicialmente.
2. **Escalabilidad:** Costo por mensaje más bajo (40% ahorro en promedio).
3. **Sin intermediarios:** Integración directa con Meta, más control.
4. **Profesionalismo:** Marca verificada de WhatsApp Business.
5. **Largo plazo:** Mejor opción para crecimiento futuro.

**Plan de implementación:**
1. **Día 1-2:** Aplicar para cuenta de WhatsApp Business con Meta
2. **Día 3-5:** Configurar cuenta, verificar negocio, obtener credenciales
3. **Día 6-8:** Implementar integración, crear plantillas
4. **Día 9-10:** Pruebas y aprobación de plantillas
5. **Contingencia:** Si aprobación demora > 7 días, extender Sprint 3 en 2-3 días

**Mitigación de riesgos:**
- Iniciar proceso de aprobación INMEDIATAMENTE (hoy mismo)
- Preparar documentación del negocio con anticipación
- Desarrollo en paralelo mientras esperamos aprobación
- Crear plantillas genéricas para aprobación rápida

### 📝 Implementación

```java
// Interfaz abstracta
public interface WhatsAppService {
    String enviarMensaje(String telefono, String mensaje);
    String enviarMensajeConPlantilla(String telefono, String plantillaId, Map<String, String> params);
    String enviarDocumento(String telefono, String urlDocumento);
    String obtenerEstadoMensaje(String idMensaje);
}

// Implementación con Meta WhatsApp Business API
@Service
@Slf4j
public class MetaWhatsAppServiceImpl implements WhatsAppService {
    
    @Value("${whatsapp.meta.phone-number-id}")
    private String phoneNumberId;
    
    @Value("${whatsapp.meta.access-token}")
    private String accessToken;
    
    @Value("${whatsapp.meta.api-version:v18.0}")
    private String apiVersion;
    
    private final RestTemplate restTemplate;
    private final MensajeWhatsAppRepository mensajeRepository;
    
    private static final String META_API_URL = "https://graph.facebook.com";
    
    @Override
    public String enviarMensaje(String telefono, String mensaje) {
        String url = String.format("%s/%s/%s/messages", META_API_URL, apiVersion, phoneNumberId);
        
        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "recipient_type", "individual",
            "to", limpiarTelefono(telefono),
            "type", "text",
            "text", Map.of("body", mensaje)
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        try {
            ResponseEntity<MetaWhatsAppResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                MetaWhatsAppResponse.class
            );
            
            String messageId = response.getBody().getMessages().get(0).getId();
            log.info("Mensaje WhatsApp enviado: {} a {}", messageId, telefono);
            
            // Guardar en BD
            guardarMensajeEnviado(telefono, mensaje, messageId);
            
            return messageId;
            
        } catch (Exception e) {
            log.error("Error enviando mensaje WhatsApp: {}", e.getMessage());
            throw new WhatsAppException("Error al enviar mensaje", e);
        }
    }
    
    @Override
    public String enviarMensajeConPlantilla(String telefono, String plantillaId, Map<String, String> params) {
        String url = String.format("%s/%s/%s/messages", META_API_URL, apiVersion, phoneNumberId);
        
        // Construir componentes de la plantilla
        List<Map<String, Object>> components = new ArrayList<>();
        if (!params.isEmpty()) {
            List<Map<String, String>> parameters = params.entrySet().stream()
                .map(e -> Map.of("type", "text", "text", e.getValue()))
                .collect(Collectors.toList());
            
            components.add(Map.of(
                "type", "body",
                "parameters", parameters
            ));
        }
        
        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "to", limpiarTelefono(telefono),
            "type", "template",
            "template", Map.of(
                "name", plantillaId,
                "language", Map.of("code", "es_MX"),
                "components", components
            )
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        try {
            ResponseEntity<MetaWhatsAppResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                MetaWhatsAppResponse.class
            );
            
            String messageId = response.getBody().getMessages().get(0).getId();
            log.info("Plantilla WhatsApp enviada: {} ({})", messageId, plantillaId);
            
            return messageId;
            
        } catch (Exception e) {
            log.error("Error enviando plantilla WhatsApp: {}", e.getMessage());
            throw new WhatsAppException("Error al enviar plantilla", e);
        }
    }
    
    @Override
    public String enviarDocumento(String telefono, String urlDocumento) {
        String url = String.format("%s/%s/%s/messages", META_API_URL, apiVersion, phoneNumberId);
        
        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "to", limpiarTelefono(telefono),
            "type", "document",
            "document", Map.of(
                "link", urlDocumento,
                "filename", extraerNombreArchivo(urlDocumento)
            )
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        try {
            ResponseEntity<MetaWhatsAppResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                MetaWhatsAppResponse.class
            );
            
            return response.getBody().getMessages().get(0).getId();
            
        } catch (Exception e) {
            log.error("Error enviando documento WhatsApp: {}", e.getMessage());
            throw new WhatsAppException("Error al enviar documento", e);
        }
    }
    
    private String limpiarTelefono(String telefono) {
        // Remover espacios, guiones, paréntesis
        String limpio = telefono.replaceAll("[^0-9+]", "");
        // Asegurar que tenga código de país
        if (!limpio.startsWith("+")) {
            limpio = "+52" + limpio; // México por defecto
        }
        return limpio;
    }
}
```

```yaml
# application.yml
whatsapp:
  provider: meta
  meta:
    phone-number-id: ${META_WHATSAPP_PHONE_NUMBER_ID}
    access-token: ${META_WHATSAPP_ACCESS_TOKEN}
    api-version: v18.0
    webhook-verify-token: ${META_WEBHOOK_VERIFY_TOKEN}
```

### 📋 Checklist de Configuración Meta

**Requisitos previos:**
- [ ] Cuenta de Facebook Business
- [ ] Aplicación en Meta for Developers
- [ ] Número de teléfono verificado
- [ ] Documentación del negocio

**Pasos de configuración:**
1. [ ] Crear app en https://developers.facebook.com
2. [ ] Agregar producto "WhatsApp Business"
3. [ ] Obtener Phone Number ID
4. [ ] Generar Access Token permanente
5. [ ] Configurar webhook URL
6. [ ] Verificar webhook con token
7. [ ] Crear plantillas de mensajes
8. [ ] Enviar plantillas a aprobación
9. [ ] Probar en sandbox
10. [ ] Solicitar revisión de cuenta Business

**Plantillas a crear:**
- `bienvenida_cliente`: Mensaje de bienvenida
- `factura_creada`: Notificación de factura generada
- `factura_vencida`: Recordatorio de pago
- `pago_recibido`: Confirmación de pago
- `recordatorio_pago`: Recordatorio antes de vencimiento

---

## DECISIÓN 2: Tipos de Cambio

### ❓ Problema
Para el sistema multi-divisa, necesitamos tipos de cambio diarios. ¿Cómo obtenerlos?
1. **Entrada manual** por administrador
2. **API externa automática**
3. **Híbrido** (API con fallback manual)

### 📊 Comparativa

| Aspecto | Manual | API Automática | Híbrido |
|---------|--------|----------------|---------|
| **Costo** | $0 | $10-50/mes | $10-50/mes |
| **Precisión** | Depende usuario | Alta | Alta |
| **Automatización** | Baja | Alta | Alta |
| **Riesgo de error** | Alto | Bajo | Bajo |
| **Dependencia externa** | No | Sí | Sí (opcional) |
| **Complejidad** | Baja | Media | Media-Alta |

### 🌐 APIs de Tipos de Cambio

| API | Costo | Requests/mes | Divisas | Actualización |
|-----|-------|--------------|---------|---------------|
| **ExchangeRate-API** | Gratis | 1,500 | 160+ | Diario |
| **Fixer.io** | $10/mes | 1,000 | 170+ | Cada hora |
| **CurrencyLayer** | $10/mes | 1,000 | 170+ | Cada hora |
| **Open Exchange Rates** | $12/mes | 1,000 | 200+ | Cada hora |
| **Banxico API** | Gratis | Ilimitado | Solo MXN | Diario |

### 🎲 DECISIÓN FINAL

**OPCIÓN ELEGIDA:** ⭐ **HÍBRIDO (API + Manual)**

**Justificación:**
1. **Flexibilidad:** Permite entrada manual si API falla
2. **Confiabilidad:** No dependemos 100% de terceros
3. **Costo-beneficio:** Usamos API gratuita con fallback
4. **Escalabilidad:** Podemos mejorar a API de pago en futuro

**Implementación en Sprint 3:**
- Entrada manual obligatoria (interfaz web)
- Botón "Obtener TC actual" que llama API (opcional)
- Programar job para actualizar automáticamente (Fase 2)

**Proveedores seleccionados:**
1. **ExchangeRate-API** (gratis, 1,500 req/mes) - Para USD, EUR
2. **Banxico API** (gratis, ilimitado) - Para MXN oficial
3. **Entrada manual** - Backup y para divisas no soportadas

### 📝 Implementación

```java
@Service
public class TipoCambioServiceImpl implements TipoCambioService {
    
    @Value("${app.tipo-cambio.api.enabled:false}")
    private boolean apiEnabled;
    
    @Autowired
    private ExchangeRateApiClient exchangeRateClient;
    
    @Override
    public TipoCambio obtenerTipoCambioActual(String divisaOrigen, String divisaDestino) {
        if (apiEnabled) {
            try {
                // Intentar obtener de API
                BigDecimal tasa = exchangeRateClient.getTasa(divisaOrigen, divisaDestino);
                return crearTipoCambio(divisaOrigen, divisaDestino, tasa, "ExchangeRate-API");
            } catch (Exception e) {
                log.warn("Error obteniendo TC de API: {}", e.getMessage());
                // Fallback a manual
            }
        }
        
        // Buscar último TC registrado manualmente
        return tipoCambioRepository.findUltimoRegistrado(divisaOrigen, divisaDestino)
            .orElseThrow(() -> new RuntimeException("No hay TC registrado"));
    }
    
    @Scheduled(cron = "0 0 9 * * MON-FRI") // 9 AM días laborales
    public void actualizarTiposCambioAutomatico() {
        if (apiEnabled) {
            // Actualizar automáticamente principales divisas
            actualizarTasas("USD", "MXN");
            actualizarTasas("EUR", "MXN");
            // ...
        }
    }
}
```

---

## DECISIÓN 3: Librería de Gráficas

### ❓ Problema
Dashboard necesita gráficas interactivas. ¿Qué librería usar?

### 📊 Comparativa

| Librería | Licencia | Tamaño | Tipos de gráficas | Responsive | Documentación |
|----------|----------|--------|-------------------|------------|---------------|
| **Chart.js** | MIT | 60 KB | 8 tipos | ✅ | Excelente |
| **ApexCharts** | MIT | 150 KB | 14 tipos | ✅ | Excelente |
| **D3.js** | BSD | 250 KB | Ilimitado | ✅ | Compleja |
| **ECharts** | Apache 2.0 | 300 KB | 20+ tipos | ✅ | Buena |
| **Highcharts** | Comercial | 80 KB | 20+ tipos | ✅ | Excelente |

### 🎲 DECISIÓN FINAL

**OPCIÓN ELEGIDA:** ⭐ **Chart.js 4.x**

**Justificación:**
1. **Ligereza:** Solo 60 KB (el más ligero)
2. **Simplicidad:** API muy fácil de usar
3. **Licencia:** MIT (gratis comercialmente)
4. **Comunidad:** Muy popular, muchos ejemplos
5. **Suficiente:** Cubre todas nuestras necesidades

**Gráficas a implementar:**
- Line Chart (ventas mensuales)
- Pie Chart (estado facturas)
- Bar Chart (top productos)
- Doughnut Chart (clientes)
- Mixed Chart (ingresos vs gastos)

**Alternativa futura:** Si necesitamos más complejidad, migrar a ApexCharts

---

## DECISIÓN 4: Almacenamiento Mensajes WhatsApp

### ❓ Problema
¿Cómo almacenar el historial de mensajes enviados/recibidos?

### 📊 Opciones

| Opción | Pros | Contras |
|--------|------|---------|
| **Base de Datos MySQL** | Simple, consistente, queries SQL | Puede crecer mucho |
| **MongoDB** | Mejor para mensajes, escalable | Requiere nueva tecnología |
| **Archivos de log** | Muy simple | Difícil de consultar |
| **No almacenar** | Más simple | Sin historial |

### 🎲 DECISIÓN FINAL

**OPCIÓN ELEGIDA:** ⭐ **Base de Datos MySQL**

**Justificación:**
1. Ya usamos MySQL
2. Permite queries y reportes
3. Consistencia transaccional
4. No hay tanto volumen (< 10,000 msg/mes)

**Optimización:**
- Índice en fecha y telefono
- Particionamiento si crece mucho (futuro)
- Purga automática de mensajes > 1 año

```sql
CREATE TABLE mensaje_whatsapp (
    id_mensaje BIGINT PRIMARY KEY AUTO_INCREMENT,
    telefono VARCHAR(20) NOT NULL,
    mensaje TEXT,
    tipo ENUM('ENVIADO', 'RECIBIDO'),
    estado ENUM('ENVIADO', 'ENTREGADO', 'LEIDO', 'FALLIDO'),
    id_factura BIGINT,
    fecha_envio TIMESTAMP,
    fecha_entrega TIMESTAMP,
    fecha_lectura TIMESTAMP,
    id_mensaje_whatsapp VARCHAR(255),  -- ID de Twilio/Meta
    error TEXT,
    
    INDEX idx_telefono_fecha (telefono, fecha_envio),
    INDEX idx_factura (id_factura),
    INDEX idx_fecha (fecha_envio DESC),
    
    FOREIGN KEY (id_factura) REFERENCES factura(id_factura)
) ENGINE=InnoDB PARTITION BY RANGE (YEAR(fecha_envio)) (
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

---

## DECISIÓN 5: Arquitectura de Webhooks

### ❓ Problema
WhatsApp envía webhooks cuando hay mensajes entrantes. ¿Cómo manejarlos?

### 📊 Opciones

| Opción | Pros | Contras |
|--------|------|---------|
| **Síncrono (responder en 200ms)** | Simple | Puede fallar si lento |
| **Asíncrono (queue)** | Confiable | Más complejo |
| **Híbrido (guardar + procesar)** | Mejor de ambos | Medio complejo |

### 🎲 DECISIÓN FINAL

**OPCIÓN ELEGIDA:** ⭐ **HÍBRIDO**

**Implementación:**
1. Webhook recibe mensaje
2. Guardar en BD inmediatamente
3. Responder 200 OK (< 200ms)
4. Procesar mensaje en background (@Async)

```java
@RestController
@RequestMapping("/api/whatsapp/webhook")
@Slf4j
public class WhatsAppWebhookController {
    
    @Autowired
    private MensajeWhatsAppService mensajeService;
    
    @Value("${whatsapp.meta.webhook-verify-token}")
    private String verifyToken;
    
    // Verificación del webhook (GET)
    @GetMapping
    public ResponseEntity<?> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("Webhook verificado exitosamente");
            return ResponseEntity.ok(challenge);
        }
        
        log.warn("Verificación de webhook fallida");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    
    // Recepción de mensajes (POST)
    @PostMapping
    public ResponseEntity<Void> webhook(@RequestBody MetaWebhookRequest request) {
        log.info("Webhook recibido de Meta WhatsApp");
        
        // 1. Guardar mensaje inmediatamente (< 50ms)
        MensajeWhatsApp mensaje = mensajeService.guardarMensajeEntrante(request);
        
        // 2. Responder rápido a Meta (< 200ms)
        ResponseEntity<Void> response = ResponseEntity.ok().build();
        
        // 3. Procesar mensaje en background (@Async)
        mensajeService.procesarMensajeAsync(mensaje.getId());
        
        return response;
    }
}

@Service
@Slf4j
public class MensajeWhatsAppServiceImpl implements MensajeWhatsAppService {
    
    @Autowired
    private MensajeWhatsAppRepository mensajeRepository;
    
    @Override
    @Transactional
    public MensajeWhatsApp guardarMensajeEntrante(MetaWebhookRequest request) {
        // Extraer datos del webhook de Meta
        var entry = request.getEntry().get(0);
        var change = entry.getChanges().get(0);
        var value = change.getValue();
        var message = value.getMessages().get(0);
        
        MensajeWhatsApp mensaje = new MensajeWhatsApp();
        mensaje.setTelefono(message.getFrom());
        mensaje.setMensaje(message.getText().getBody());
        mensaje.setTipo(TipoMensaje.RECIBIDO);
        mensaje.setEstado(EstadoMensaje.RECIBIDO);
        mensaje.setIdMensajeWhatsapp(message.getId());
        mensaje.setFechaEnvio(LocalDateTime.now());
        
        return mensajeRepository.save(mensaje);
    }
    
    @Override
    @Async
    public void procesarMensajeAsync(Long idMensaje) {
        log.info("Procesando mensaje {} en background", idMensaje);
        
        MensajeWhatsApp mensaje = mensajeRepository.findById(idMensaje)
            .orElseThrow(() -> new EntityNotFoundException("Mensaje no encontrado"));
        
        try {
            // Procesar lógica de negocio
            if (mensaje.getMensaje().toLowerCase().contains("consultar factura")) {
                procesarConsultaFactura(mensaje);
            } else if (mensaje.getMensaje().toLowerCase().contains("estado pedido")) {
                procesarEstadoPedido(mensaje);
            } else {
                // Respuesta automática genérica
                enviarRespuestaAutomatica(mensaje);
            }
            
            mensaje.setEstado(EstadoMensaje.PROCESADO);
            mensajeRepository.save(mensaje);
            
        } catch (Exception e) {
            log.error("Error procesando mensaje {}: {}", idMensaje, e.getMessage());
            mensaje.setEstado(EstadoMensaje.ERROR);
            mensaje.setError(e.getMessage());
            mensajeRepository.save(mensaje);
        }
    }
}
```

**DTO para webhook de Meta:**

```java
@Data
public class MetaWebhookRequest {
    private String object;
    private List<Entry> entry;
    
    @Data
    public static class Entry {
        private String id;
        private List<Change> changes;
    }
    
    @Data
    public static class Change {
        private Value value;
        private String field;
    }
    
    @Data
    public static class Value {
        private String messagingProduct;
        private Metadata metadata;
        private List<Contact> contacts;
        private List<Message> messages;
        private List<Status> statuses;
    }
    
    @Data
    public static class Message {
        private String from;
        private String id;
        private String timestamp;
        private Text text;
        private String type;
    }
    
    @Data
    public static class Text {
        private String body;
    }
    
    @Data
    public static class Status {
        private String id;
        private String status; // sent, delivered, read
        private String timestamp;
        private String recipientId;
    }
}
        
        // 3. Procesar en background (no bloqueante)
        mensajeService.procesarMensajeAsync(mensaje.getId());
        
        return response;
    }
}
```

---

## DECISIÓN 6: Divisa Maestra

### ❓ Problema
¿Cómo garantizar que solo haya UNA divisa maestra?

### 📊 Opciones

| Opción | Pros | Contras |
|--------|------|---------|
| **Constraint de BD** | Garantizado | SQL complejo |
| **Validación en Service** | Simple | Puede fallar |
| **Ambos** | Doble protección | Redundante |

### 🎲 DECISIÓN FINAL

**OPCIÓN ELEGIDA:** ⭐ **AMBOS (BD + Service)**

**Implementación:**

```sql
-- Trigger que valida solo una maestra
DELIMITER $$
CREATE TRIGGER tr_divisa_maestra_unica
BEFORE INSERT ON divisa
FOR EACH ROW
BEGIN
    IF NEW.es_maestra = TRUE THEN
        IF EXISTS(SELECT 1 FROM divisa WHERE es_maestra = TRUE AND activo = TRUE) THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ya existe una divisa maestra activa';
        END IF;
    END IF;
END$$
DELIMITER ;
```

```java
@Service
public class DivisaServiceImpl implements DivisaService {
    
    @Transactional
    public void establecerDivisaMaestra(Integer idDivisa) {
        // 1. Verificar que no haya otra maestra
        Optional<Divisa> maestraActual = divisaRepository.findDivisaMaestra();
        
        if (maestraActual.isPresent() && !maestraActual.get().getIdDivisa().equals(idDivisa)) {
            // 2. Quitar bandera de maestra actual
            Divisa antigua = maestraActual.get();
            antigua.setEsMaestra(false);
            divisaRepository.save(antigua);
        }
        
        // 3. Establecer nueva maestra
        Divisa nueva = divisaRepository.findById(idDivisa)
            .orElseThrow(() -> new EntityNotFoundException("Divisa no encontrada"));
        nueva.setEsMaestra(true);
        divisaRepository.save(nueva);
        
        log.info("Divisa maestra cambiada a: {}", nueva.getCodigo());
    }
}
```

---

## DECISIÓN 7: Caché del Dashboard

### ❓ Problema
Dashboard calcula estadísticas complejas. ¿Cachear resultados?

### 📊 Opciones

| Opción | Pros | Contras |
|--------|------|---------|
| **Sin caché** | Datos siempre actualizados | Lento |
| **Spring Cache (5 min)** | Simple, rápido | Puede estar desactualizado |
| **Cache con invalidación** | Rápido + actualizado | Complejo |

### 🎲 DECISIÓN FINAL

**OPCIÓN ELEGIDA:** ⭐ **Spring Cache (5 minutos) + Invalidación manual**

**Justificación:**
- Dashboard no necesita datos en tiempo real absoluto
- 5 minutos es aceptable
- Invalidar al crear factura, producto, cliente

```java
@Service
public class DashboardServiceImpl implements DashboardService {
    
    @Cacheable(value = "dashboardKPIs", key = "'kpis'")
    public DashboardKPIsDTO getKPIs() {
        // Cálculos pesados
        return kpis;
    }
    
    @Cacheable(value = "dashboardVentas", key = "'ventas-' + #meses")
    public List<VentasMensualesDTO> getVentasMensuales(int meses) {
        // Query complejo
        return ventas;
    }
}

@Service
public class FacturaServiceImpl implements FacturaService {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Override
    @CacheEvict(value = {"dashboardKPIs", "dashboardVentas"}, allEntries = true)
    public Factura save(Factura factura) {
        // Guardar factura
        // Cache se invalida automáticamente
    }
}
```

```yaml
# application.yml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=5m
```

---

## 📊 RESUMEN DE DECISIONES

| # | Decisión | Opción Elegida | Justificación Principal |
|---|----------|---------------|------------------------|
| 1 | WhatsApp API | **Meta WhatsApp Business API** | Ahorro de costos (40% menor) + 1,000 msg gratis/mes |
| 2 | Tipos de Cambio | **Híbrido (API + Manual)** | Flexibilidad y confiabilidad |
| 3 | Librería Gráficas | **Chart.js 4.x** | Ligero, simple, suficiente |
| 4 | Almacenamiento Mensajes | **MySQL con particiones** | Consistencia, queries SQL |
| 5 | Arquitectura Webhooks | **Híbrido (guardar + async)** | Rápido y confiable |
| 6 | Divisa Maestra | **BD + Service (ambos)** | Doble protección |
| 7 | Caché Dashboard | **Spring Cache 5min** | Balance velocidad/actualización |

### 💰 Impacto Económico de Decisiones

**Decisión 1 (WhatsApp - Meta vs Twilio):**
- Ahorro proyectado: **$5-10 USD/mes** (1,000 msg)
- Ahorro a 12 meses: **$60-120 USD/año**
- Ahorro a 5,000 msg/mes: **$8-11 USD/mes** ($96-132/año)

**Decisión 2 (Tipo Cambio - API Gratis):**
- Ahorro vs API de pago: **$10-50 USD/mes**
- Ahorro a 12 meses: **$120-600 USD/año**

**Total ahorro anual estimado: $180-720 USD**

---

## ⏱️ CRONOGRAMA AJUSTADO SPRINT 3

### Fase 0: Preparación Meta WhatsApp (Paralela) - 3-7 días
**Inicio:** 21 octubre 2025 (HOY)  
**Objetivo:** Obtener aprobación de Meta antes de empezar desarrollo

| Día | Actividad | Responsable | Duración |
|-----|-----------|-------------|----------|
| 1 | Crear cuenta Facebook Business | EmaSleal | 1h |
| 1 | Crear app en Meta for Developers | EmaSleal | 1h |
| 1-2 | Verificar número de teléfono | Meta | 24h |
| 2 | Configurar WhatsApp Business API | EmaSleal | 2h |
| 2-3 | Crear plantillas de mensajes (5) | EmaSleal | 3h |
| 3-5 | Esperar aprobación de plantillas | Meta | 48-96h |
| 5-7 | Verificar cuenta Business (si necesario) | Meta | 48h |

**Entregables Fase 0:**
- ✅ Cuenta Meta WhatsApp Business aprobada
- ✅ 5 plantillas aprobadas
- ✅ Access Token y Phone Number ID
- ✅ Webhook configurado y verificado

### Fase 1: Integración WhatsApp API - 5-7 días (40-50h)
**Inicio:** 28 octubre 2025 (después de aprobación)

| Tarea | Estimación | Prioridad |
|-------|------------|-----------|
| Entidades BD (mensaje_whatsapp) | 2h | CRÍTICA |
| Repository + Service WhatsApp | 4h | CRÍTICA |
| MetaWhatsAppServiceImpl | 6h | CRÍTICA |
| Webhook Controller (GET/POST) | 4h | CRÍTICA |
| DTOs y respuestas Meta | 2h | CRÍTICA |
| Plantillas predefinidas | 3h | ALTA |
| Procesamiento async mensajes | 4h | ALTA |
| Envío factura por WhatsApp | 5h | ALTA |
| Recordatorios automáticos | 4h | ALTA |
| Tests unitarios WhatsApp | 4h | MEDIA |
| Tests integración webhook | 3h | MEDIA |
| Manejo de errores y reintentos | 3h | MEDIA |

**Total:** 44h (5.5 días a 8h/día)

### Fase 2: Dashboard Avanzado - 2.5 días (16-20h)
**Inicio:** 6 noviembre 2025

| Tarea | Estimación | Prioridad |
|-------|------------|-----------|
| Integrar Chart.js 4.x | 2h | CRÍTICA |
| KPIs principales (ventas, clientes) | 3h | CRÍTICA |
| Gráfica ventas mensuales (line) | 2h | CRÍTICA |
| Gráfica estado facturas (pie) | 2h | ALTA |
| Top productos (bar chart) | 2h | ALTA |
| Dashboard responsive | 3h | ALTA |
| Service de estadísticas | 4h | ALTA |
| Caché con Spring Cache | 2h | MEDIA |

**Total:** 20h (2.5 días)

### Fase 3: Sistema Multi-Divisa - 6.5 días (38-52h)
**Inicio:** 9 noviembre 2025

| Tarea | Estimación | Prioridad |
|-------|------------|-----------|
| Entidad Divisa + Repository | 3h | CRÍTICA |
| Entidad TipoCambio + Repository | 3h | CRÍTICA |
| CRUD Divisas (backend) | 4h | CRÍTICA |
| CRUD Tipos de Cambio (backend) | 4h | CRÍTICA |
| Validación divisa maestra única | 2h | CRÍTICA |
| ExchangeRate-API Client | 4h | ALTA |
| Banxico API Client | 3h | ALTA |
| Scheduled job actualización TC | 3h | ALTA |
| Adaptar Factura para multi-divisa | 5h | ALTA |
| Adaptar Producto para precio/divisa | 4h | ALTA |
| Conversión automática divisas | 4h | ALTA |
| UI CRUD Divisas | 3h | MEDIA |
| UI CRUD Tipos de Cambio | 3h | MEDIA |
| Tests multi-divisa | 4h | MEDIA |
| Migración BD (divisas iniciales) | 2h | MEDIA |

**Total:** 51h (6.4 días)

### Fase 4: Mejoras Autenticación - 1.5 días (8-11h)
**Inicio:** 18 noviembre 2025

| Tarea | Estimación | Prioridad |
|-------|------------|-----------|
| Agregar username a Usuario | 1h | ALTA |
| Validación username único | 1h | ALTA |
| Login con username (además de phone) | 3h | ALTA |
| Actualizar formularios | 2h | MEDIA |
| Tests | 2h | MEDIA |

**Total:** 9h (1.1 días)

### Fase 5: Mejoras Reportes (OPCIONAL) - 1 día (6-8h)
**Inicio:** 20 noviembre 2025

| Tarea | Estimación | Prioridad |
|-------|------------|-----------|
| Filtros avanzados reportes | 2h | BAJA |
| Exportar a Excel | 2h | BAJA |
| Gráficas en PDF | 2h | BAJA |

**Total:** 6h (0.75 días)

---

## 📅 CALENDARIO SPRINT 3 (AJUSTADO CON META)

### Semana 1: Preparación + Inicio (21-27 octubre)
- **Lun 21:** Solicitar cuenta Meta WhatsApp ⚡
- **Mar 22:** Crear plantillas + verificaciones
- **Mié 23:** Esperar aprobaciones (desarrollo DB schema en paralelo)
- **Jue 24:** Continuar esperando (desarrollar DTOs)
- **Vie 25:** Probablemente aprobado ✅
- **Sáb 26:** Buffer
- **Dom 27:** Buffer

### Semana 2: Desarrollo Principal (28 oct - 3 nov)
- **Lun 28:** WhatsApp Service + Repository (8h)
- **Mar 29:** Webhook Controller + Tests (8h)
- **Mié 30:** Plantillas + Envío Factura (8h)
- **Jue 31:** Recordatorios + Async (8h)
- **Vie 1:** Pruebas WhatsApp finales (6h)
- **Sáb 2:** Buffer
- **Dom 3:** Buffer

### Semana 3: Dashboard + Multi-Divisa (4-10 nov)
- **Lun 4:** Chart.js + KPIs (8h)
- **Mar 5:** Gráficas Dashboard (8h)
- **Mié 6:** Entidades Divisa + TC (8h)
- **Jue 7:** CRUDs + APIs (8h)
- **Vie 8:** Conversión + Factura multi-divisa (8h)
- **Sáb 9:** Buffer
- **Dom 10:** Buffer

### Semana 4: Finalización (11-17 nov)
- **Lun 11:** UI Divisas + TC (8h)
- **Mar 12:** Tests multi-divisa (6h)
- **Mié 13:** Username autenticación (8h)
- **Jue 14:** Tests finales (6h)
- **Vie 15:** Documentación + Deploy (6h)
- **Sáb 16:** Buffer
- **Dom 17:** Buffer

### Semana 5: Contingencia (18-21 nov)
- **Lun 18:** Ajustes finales
- **Mar 19:** Reportes (opcional)
- **Mié 20:** Testing integral
- **Jue 21:** Presentación Sprint 3 ✅

**Fecha inicio:** 21 octubre 2025 (HOY - Solicitud Meta)  
**Fecha fin:** 21 noviembre 2025  
**Duración total:** 32 días (17 días laborables)

---

## 📝 PRÓXIMAS DECISIONES

### Para Sprint 4
- [ ] ¿Cómo manejar múltiples almacenes?
- [ ] ¿PEPS o Promedio para valorización?
- [ ] ¿Control de lotes obligatorio?

### Para Sprint 5
- [ ] ¿MRP simple o complejo?
- [ ] ¿Integrar con IoT de planta?

---

**Aprobado por:** EmaSleal  
**Fecha:** 20 de octubre de 2025  
**Versión:** 1.0  
**Estado:** 📋 APROBADO
