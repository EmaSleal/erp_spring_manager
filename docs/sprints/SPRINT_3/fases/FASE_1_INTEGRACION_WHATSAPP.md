# 🚀 FASE 1: INTEGRACIÓN WHATSAPP API - DETALLADO

**Sprint:** 3  
**Fase:** 1 de 5  
**Duración:** 5-7 días (28 oct - 5 nov 2025)  
**Prioridad:** ⭐ MÁXIMA  
**Estado:** ⏸️ ESPERANDO FASE 0  
**Tiempo estimado:** 40-50 horas

---

## ⚠️ CAMBIOS IMPORTANTES

### 🔄 Refactorización: Chats Ligados a Usuario (10 nov 2025)
**Decisión Técnica:** Los chats de WhatsApp están ligados a **Usuario** (no a Factura/Pedido)

**Razón:** Mejor experiencia de usuario - conversaciones continuas que pueden abarcar múltiples pedidos

**Impacto:**
- ✅ Modelo `MensajeWhatsApp` usa `idUsuario` en lugar de `idFactura`
- ✅ Repository actualizado con 4 métodos nuevos para Usuario
- ✅ Permite historial completo de conversaciones por cliente
- ⚠️ Requiere script de migración SQL (pendiente)

**Documentación completa:** `docs/sprints/SPRINT_3/decisiones/DECISION_CHATS_LIGADOS_USUARIO.md`

---

## 🎯 OBJETIVO DE LA FASE

Implementar la integración completa con Meta WhatsApp Business API para enviar y recibir mensajes, incluyendo:
- Envío de mensajes simples y con plantillas
- Envío de documentos PDF (facturas)
- Recepción de webhooks
- Integración con usuarios del sistema
- Gestión de plantillas desde el sistema

---

## 📊 PROGRESO GENERAL

```
Total subfases: 7
Completadas: 2/7 (28%)
En progreso: 0/7
Pendientes: 5/7 (72%)

Tiempo estimado: 40-50 horas
Tiempo invertido: 10h
Tiempo restante: 30-40h
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
- `src/main/java/api/astro/whats_orders_manager/models/MensajeWhatsApp.java` (140 líneas)
- `src/main/java/api/astro/whats_orders_manager/models/PlantillaWhatsApp.java` (160 líneas)

**Características implementadas:**
- Enums: TipoMensaje, EstadoMensaje, CategoriaPlantilla, EstadoMeta
- Métodos helper: esExitoso(), tieneUsuario(), getNombreUsuario(), estaListaParaUsar(), etc.
- Relaciones JPA correctas (ManyToOne con Usuario)
- Validaciones con Bean Validation
- Campos de auditoría (@CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy)

**⚠️ CAMBIO IMPORTANTE (10 nov 2025):**
- **Chats ligados a Usuario** (no a Factura/Pedido)
- Ver: `docs/sprints/SPRINT_3/decisiones/DECISION_CHATS_LIGADOS_USUARIO.md`
- Justificación: Mejor UX, conversaciones continuas, múltiples pedidos por chat

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
- `src/main/java/api/astro/whats_orders_manager/repositories/MensajeWhatsAppRepository.java` (16 métodos)
- `src/main/java/api/astro/whats_orders_manager/repositories/PlantillaWhatsAppRepository.java` (11 métodos)

**Métodos implementados:**
- Búsquedas por ID de WhatsApp
- Búsquedas por teléfono, usuario, estado
- Queries para reintentos
- Rate limiting
- Validaciones de existencia
- **Métodos por Usuario** (10 nov 2025):
  - `findByIdUsuarioOrderByFechaEnvioDesc()` - Historial completo
  - `findTop10ByIdUsuarioOrderByFechaEnvioDesc()` - Últimos mensajes
  - `countByIdUsuarioAndEstado()` - Estadísticas
  - `findByIdUsuarioAndEstadoOrderByFechaEnvioDesc()` - Filtrado

**Validaciones:**
- [x] 27 métodos de consulta implementados
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

### ✅ SUBFASE 1.2: Backend - DTOs (4h)

**Estado:** ✅ COMPLETADO  
**Prioridad:** ALTA  
**Tiempo:** 4 horas  
**Dependencias:** Subfase 1.1 completada  
**Fecha completado:** 10 noviembre 2025

#### Tareas Específicas:

##### ✅ 1.2.1 - Crear DTOs de Webhook (2h) - COMPLETADO
- [x] Crear `MetaWebhookRequest.java`
- [x] Crear clases internas para estructura anidada (12 clases)
- [x] Agregar validaciones (@NotNull, Bean Validation)
- [x] Documentar cada campo

**Archivo creado:** `src/main/java/api/astro/whats_orders_manager/dto/whatsapp/MetaWebhookRequest.java` (220 líneas)

**Características:**
- 12 clases internas anidadas (Entry, Change, Value, Metadata, Contact, etc.)
- Estructura completa según documentación Meta
- Soporte para mensajes entrantes y actualizaciones de estado
- Jackson annotations para serialización JSON

##### ✅ 1.2.2 - Crear DTOs de Solicitud (1.5h) - COMPLETADO
- [x] Crear `EnviarMensajeRequest.java`
- [x] Soporte para múltiples tipos de mensaje
- [x] Validaciones de formato
- [x] Clases internas para componentes

**Archivo creado:** `src/main/java/api/astro/whats_orders_manager/dto/whatsapp/EnviarMensajeRequest.java` (160 líneas)

**Características:**
- Soporte para mensajes de texto, plantillas y documentos
- 7 clases internas (TextContent, TemplateContent, Component, Parameter, etc.)
- Validación de formato de teléfono con regex
- Builder pattern para construcción fácil

##### ✅ 1.2.3 - Crear DTOs de Respuesta (1.5h) - COMPLETADO
- [x] Crear `EnviarMensajeResponse.java`
- [x] Crear `MetaApiErrorResponse.java`
- [x] Métodos helper para verificación de estado
- [x] Detección de tipos de error

**Archivos creados:**
- `EnviarMensajeResponse.java` (90 líneas)
- `MetaApiErrorResponse.java` (90 líneas)

**Características EnviarMensajeResponse:**
- Estructura según respuesta de Meta API
- Métodos: `isExitoso()`, `getMessageId()`, `getWaId()`
- 2 clases internas (Contact, Message)

**Características MetaApiErrorResponse:**
- Detección de rate limit
- Detección de número inválido
- Detección de plantilla inválida
- Método `getErrorMessage()` centralizado

##### ✅ 1.2.4 - Crear DTOs Internos (1h) - COMPLETADO
- [x] Crear `WhatsAppMensajeDTO.java`
- [x] Crear `PlantillaWhatsAppDTO.java`
- [x] Crear `WebhookValidationDTO.java`
- [x] Métodos helper útiles

**Archivos creados:**
- `WhatsAppMensajeDTO.java` (70 líneas)
- `PlantillaWhatsAppDTO.java` (80 líneas)
- `WebhookValidationDTO.java` (50 líneas)

**Características WhatsAppMensajeDTO:**
- DTO interno para transferir mensajes entre capas
- Incluye datos de usuario (idUsuario, nombreUsuario)
- Métodos: `esExitoso()`, `esEnviado()`, `esRecibido()`, `esFallido()`, `esPendiente()`

**Características PlantillaWhatsAppDTO:**
- DTO interno para plantillas
- Métodos: `estaListaParaUsar()`, `estaAprobada()`, `estaPendiente()`, `estaRechazada()`
- Gestión de lista de parámetros
- Método `getNumeroParametros()`

**Características WebhookValidationDTO:**
- Validación inicial de webhooks (challenge)
- Verificación de token: `isTokenValid()`
- Verificación de modo: `isSubscribeMode()`

**Validaciones:**
- [x] 7 DTOs creados (~760 líneas)
- [x] 22 clases internas
- [x] 20+ métodos helper
- [x] 0 errores de compilación
- [x] Todas las anotaciones correctas (Jackson, Bean Validation, Lombok)

**Entregables Subfase 1.2:**
- [x] DTOs de webhook (1 archivo, 12 clases internas)
- [x] DTOs de request (1 archivo, 7 clases internas)
- [x] DTOs de response (2 archivos, 3 clases internas)
- [x] DTOs internos (3 archivos)
- [x] Validaciones Bean Validation configuradas
- [x] Documentación JavaDoc completa
- [x] 0 errores de compilación

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
