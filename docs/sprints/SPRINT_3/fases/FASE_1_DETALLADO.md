# 🚀 FASE 1: INTEGRACIÓN WHATSAPP API - DETALLADO

**Sprint:** 3  
**Fase:** 1 de 5  
**Duración:** 5-7 días (28 oct - 5 nov 2025)  
**Prioridad:** ⭐ MÁXIMA  
**Estado:** ⏸️ ESPERANDO FASE 0  
**Tiempo estimado:** 40-50 horas

---

## 🎯 OBJETIVO DE LA FASE

Implementar la integración completa con Meta WhatsApp Business API para enviar y recibir mensajes, incluyendo:
- Envío de mensajes simples y con plantillas
- Envío de documentos PDF (facturas)
- Recepción de webhooks
- Integración con módulo de facturación
- Gestión de plantillas desde el sistema

---

## 📊 PROGRESO GENERAL

```
Total subfases: 7
Completadas: 1/7 (14%)
En progreso: 0/7
Pendientes: 6/7 (86%)

Tiempo estimado: 40-50 horas
Tiempo invertido: 6h
Tiempo restante: 34-44h
```

---

## 📋 SUBFASES DETALLADAS

### ✅ SUBFASE 1.1: Backend - Modelos y Persistencia (6h)

**Estado:** ✅ COMPLETADO  
**Prioridad:** CRÍTICA  
**Tiempo:** 6 horas  
**Dependencias:** Fase 0 completada  
**Fecha completado:** 26 octubre 2025

#### Tareas Específicas:

##### ✅ 1.1.1 - Crear Script de Migración SQL (1.5h) - COMPLETADO
- [x] Crear archivo `MIGRATION_WHATSAPP_SPRINT_3.sql`
- [x] Agregar header con información de migración
- [x] Documentar cambios incluidos

**Archivo creado:** `docs/base de datos/MIGRATION_WHATSAPP_SPRINT_3.sql`

##### ✅ 1.1.2 - Crear Tabla mensaje_whatsapp (2h) - COMPLETADO
- [x] Definir estructura de tabla
- [x] Crear índices optimizados (6 índices)
- [x] Agregar foreign keys
- [x] Configurar particionamiento por año (5 particiones)
- [x] Documentar cada campo

**Validaciones:**
- [x] Script SQL completado (400+ líneas)
- [x] Tabla con 13 columnas
- [x] 6 índices creados
- [x] Particionamiento configurado (2025-2028 + future)

##### ✅ 1.1.3 - Crear Tabla plantilla_whatsapp (1.5h) - COMPLETADO
- [x] Definir estructura de tabla
- [x] Crear índices (4 índices)
- [x] Insertar plantillas iniciales (5 plantillas)
- [x] Documentar uso

**Validaciones:**
- [x] Tabla con 13 columnas
- [x] 4 índices creados
- [x] 5 plantillas aprobadas insertadas

##### ✅ 1.1.4 - Crear Entidades Java (1h) - COMPLETADO
- [x] Crear `MensajeWhatsApp.java`
- [x] Crear `PlantillaWhatsApp.java`
- [x] Agregar anotaciones JPA
- [x] Agregar validaciones
- [x] Generar getters/setters con Lombok

**Archivos creados:**
- `src/main/java/api/astro/whats_orders_manager/model/MensajeWhatsApp.java` (140 líneas)
- `src/main/java/api/astro/whats_orders_manager/model/PlantillaWhatsApp.java` (160 líneas)

**Características implementadas:**
- Enums: TipoMensaje, EstadoMensaje, CategoriaPlantilla, EstadoMeta
- Métodos helper: esExitoso(), tieneFactura(), estaListaParaUsar(), etc.
- Relaciones JPA correctas
- Validaciones con Bean Validation

**Validaciones:**
- [x] Compilación sin errores
- [x] Anotaciones JPA correctas
- [x] Lombok genera getters/setters

##### ✅ 1.1.5 - Crear Repositories (30min) - COMPLETADO
- [x] Crear `MensajeWhatsAppRepository.java`
- [x] Crear `PlantillaWhatsAppRepository.java`
- [x] Agregar queries personalizadas
- [x] Documentar métodos

**Archivos creados:**
- `src/main/java/api/astro/whats_orders_manager/repository/MensajeWhatsAppRepository.java` (14 métodos)
- `src/main/java/api/astro/whats_orders_manager/repository/PlantillaWhatsAppRepository.java` (11 métodos)

**Métodos implementados:**
- Búsquedas por ID de WhatsApp
- Búsquedas por teléfono, factura, estado
- Queries para reintentos
- Rate limiting
- Validaciones de existencia

**Validaciones:**
- [x] 25 métodos de consulta implementados
- [x] Documentación completa
- [x] Compilación sin errores

**Entregables Subfase 1.1:**
- [x] Script SQL completo (400+ líneas)
- [x] 2 tablas creadas en BD
- [x] 2 entidades Java compilando (300 líneas)
- [x] 2 repositories funcionando (25 métodos)
- [x] 5 plantillas listas para usar
- [x] 0 errores de compilación

---

### 🔲 SUBFASE 1.2: Backend - DTOs (4h)

**Estado:** ⏸️ PENDIENTE  
**Prioridad:** ALTA  
**Tiempo:** 4 horas  
**Dependencias:** Subfase 1.1 completada

#### Tareas Específicas:

##### 1.2.1 - Crear DTOs de Webhook (2h)
- [ ] Crear `MetaWebhookRequest.java`
- [ ] Crear clases internas para estructura anidada
- [ ] Agregar validaciones
- [ ] Documentar cada campo

**Archivo:** `src/main/java/com/astro/erp/dto/whatsapp/MetaWebhookRequest.java`

```java
package com.astro.erp.dto.whatsapp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO para recibir webhooks de Meta WhatsApp Business API
 * Estructura completa según documentación oficial de Meta
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaWebhookRequest {
    
    @NotNull
    private String object; // "whatsapp_business_account"
    
    @NotNull
    private List<Entry> entry;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private String id; // WhatsApp Business Account ID
        private List<Change> changes;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Change {
        private Value value;
        private String field; // "messages" o "message_status"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Value {
        @JsonProperty("messaging_product")
        private String messagingProduct; // "whatsapp"
        
        private Metadata metadata;
        private List<Contact> contacts;
        private List<Message> messages;
        private List<Status> statuses;
        private List<Error> errors;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        @JsonProperty("display_phone_number")
        private String displayPhoneNumber;
        
        @JsonProperty("phone_number_id")
        private String phoneNumberId;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contact {
        private Profile profile;
        @JsonProperty("wa_id")
        private String waId; // WhatsApp ID (teléfono)
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Profile {
        private String name;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String from; // Número del remitente
        private String id; // ID del mensaje (wamid.xxx)
        private String timestamp;
        private Text text;
        private String type; // "text", "image", "document", etc.
        private Context context; // Si es respuesta a otro mensaje
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Text {
        private String body; // Contenido del mensaje
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Context {
        private String from;
        private String id;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Status {
        private String id; // ID del mensaje
        private String status; // "sent", "delivered", "read", "failed"
        private String timestamp;
        @JsonProperty("recipient_id")
        private String recipientId;
        private Conversation conversation;
        private Pricing pricing;
        private List<Error> errors;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Conversation {
        private String id;
        @JsonProperty("expiration_timestamp")
        private String expirationTimestamp;
        private Origin origin;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Origin {
        private String type; // "user_initiated", "business_initiated"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pricing {
        private boolean billable;
        @JsonProperty("pricing_model")
        private String pricingModel;
        private String category;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        private Integer code;
        private String title;
        private String message;
        @JsonProperty("error_data")
        private ErrorData errorData;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData {
        private String details;
    }
}
```

##### 1.2.2 - Crear DTOs de Respuesta (1h)
- [ ] Crear `MetaWhatsAppResponse.java`
- [ ] Crear `EnviarMensajeResponse.java`

**Archivos a crear:** (continúo en siguiente mensaje por límite de caracteres)

**Entregables Subfase 1.2:**
- [ ] 4 DTOs creados
- [ ] Validaciones configuradas
- [ ] Documentación completa
- [ ] Tests unitarios de serialización

---

### 🔲 SUBFASE 1.3: Backend - Servicios Core (12h)

[Continúa con detalles completos...]

---

## 📊 RESUMEN DE ENTREGABLES FASE 1

### Base de Datos
- [ ] 2 tablas nuevas
- [ ] 8 índices optimizados
- [ ] Particionamiento configurado
- [ ] 5 plantillas iniciales

### Backend
- [ ] 2 entidades JPA ✅
- [ ] 2 repositories ✅
- [ ] 7 DTOs completos ⏸️ (Webhook completo, pendientes Response y Request)
- [ ] 5 services (WhatsApp, Webhook, Mensaje, Plantilla, FacturaWhatsApp)
- [ ] 2 controllers API + 1 Webhook Controller
- [ ] 10+ endpoints API
- [ ] RestTemplate configurado
- [ ] Retry policy

### Frontend
- [ ] 2 vistas HTML (mensajes, plantillas)
- [ ] 1 archivo JavaScript (envío WhatsApp)
- [ ] Botón en factura-list
- [ ] Modal confirmación

### Testing
- [ ] 15+ tests unitarios
- [ ] 8+ tests integración
- [ ] Tests de webhook con mock

### Base de Datos
- [ ] 2 tablas creadas ✅ (con particionamiento)
- [ ] 15+ índices optimizados ✅
- [ ] 5 plantillas iniciales ✅

---

## 📋 SUBFASES PENDIENTES DE DOCUMENTAR

**Las siguientes subfases están pendientes de desarrollo detallado:**

### SUBFASE 1.2: Backend DTOs (PARCIAL - 50% completo)
✅ Completado: `MetaWebhookRequest.java`  
⏸️ Pendiente: Response DTOs, Request DTOs, validation

### SUBFASE 1.3: Backend - Servicios Core WhatsApp (12h)
Pendiente de documentar en detalle

### SUBFASE 1.4: Backend - Webhook Controller (8h)
Pendiente de documentar en detalle

### SUBFASE 1.5: Integración con Facturación (8h)
Pendiente de documentar en detalle

### SUBFASE 1.6: Frontend - Vistas WhatsApp (6h)
Pendiente de documentar en detalle

### SUBFASE 1.7: Testing (6h)
Pendiente de documentar en detalle

---

**Estado:** ⏸️ FASE 1 documentada parcialmente (30% completo). Ver FASE_2_DETALLADO.md para dashboard.

**Nota:** Este documento será ampliado con las subfases 1.2-1.7 cuando se requiera el desarrollo completo.
