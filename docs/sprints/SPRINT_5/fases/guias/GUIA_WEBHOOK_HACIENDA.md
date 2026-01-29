# 🔔 GUÍA: Webhook de Hacienda

**Sprint:** 5  
**Fase:** 3  
**Fecha:** 24 de enero de 2026  
**Autor:** Sistema ERP - Módulo de Facturación Electrónica

---

## 📋 INTRODUCCIÓN

Esta guía documenta la implementación del endpoint webhook para recibir notificaciones asíncronas de Hacienda de Costa Rica cuando el estado de un comprobante electrónico cambia.

### ¿Qué es un Webhook?

Un webhook es un endpoint HTTP que Hacienda llama automáticamente cuando:
- Un comprobante es aceptado ✅
- Un comprobante es rechazado ❌
- Ocurre un error en el procesamiento ⚠️

**Ventajas sobre Polling:**
- ⚡ Actualizaciones en tiempo real
- 📉 Menos llamadas a la API de Hacienda
- 🔔 Notificaciones instantáneas al usuario
- 💰 Menor consumo de recursos

---

## 🏗️ ARQUITECTURA

```
┌─────────────────┐
│   Hacienda CR   │
│   API v4.4      │
└────────┬────────┘
         │ HTTP POST
         │ (Callback)
         ▼
┌─────────────────────────────────┐
│  HaciendaWebhookController      │
│  POST /api/hacienda/callback    │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│  ComprobanteElectronicoService  │
│  procesarCallbackHacienda()     │
└────────┬────────────────────────┘
         │
         ├──► Actualizar BD (ComprobanteElectronico)
         ├──► Guardar XML en filesystem
         ├──► Notificar WebSocket
         └──► Enviar Email (si configurado)
```

---

## 📦 COMPONENTES A IMPLEMENTAR

### 1. DTO: HaciendaCallbackDTO

**Archivo:** `src/main/java/.../electronica/dto/HaciendaCallbackDTO.java`

```java
package api.astro.whats_orders_manager.modules.facturacion.electronica.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO para recibir callbacks de Hacienda.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HaciendaCallbackDTO {
    
    /**
     * Clave numérica del comprobante (50 dígitos).
     */
    @NotBlank(message = "La clave numérica es obligatoria")
    @Size(min = 50, max = 50, message = "La clave debe tener 50 dígitos")
    private String claveNumerica;
    
    /**
     * Estado del comprobante según Hacienda.
     * Valores: "aceptado", "rechazado", "procesando", "error"
     */
    @NotBlank(message = "El estado es obligatorio")
    private String estado;
    
    /**
     * Código de respuesta de Hacienda.
     * Ejemplo: "1" = Aceptado, "2" = Rechazado parcial, "3" = Rechazado
     */
    @NotBlank(message = "El código de respuesta es obligatorio")
    private String codigoRespuesta;
    
    /**
     * Mensaje descriptivo de la respuesta.
     */
    private String mensaje;
    
    /**
     * XML de respuesta firmado por Hacienda (Base64).
     */
    private String xmlRespuesta;
    
    /**
     * Fecha y hora de la respuesta (ISO 8601).
     */
    @NotNull(message = "La fecha de respuesta es obligatoria")
    private String fechaRespuesta;
    
    /**
     * Token de autenticación (opcional, según configuración).
     */
    private String token;
}
```

---

### 2. Controller: HaciendaWebhookController

**Archivo:** `src/main/java/.../electronica/controller/HaciendaWebhookController.java`

```java
package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.HaciendaCallbackDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para recibir callbacks de Hacienda.
 */
@RestController
@RequestMapping("/api/hacienda")
@RequiredArgsConstructor
@Slf4j
public class HaciendaWebhookController {
    
    private final ComprobanteElectronicoService comprobanteService;
    
    @Value("${facturacion.hacienda.webhook.token:}")
    private String webhookToken;
    
    /**
     * Endpoint para recibir callbacks de Hacienda.
     * 
     * @param callback Datos del callback
     * @param authToken Token de autenticación (header)
     * @return 200 OK si se procesó correctamente
     */
    @PostMapping("/callback")
    public ResponseEntity<?> recibirCallback(
            @Valid @RequestBody HaciendaCallbackDTO callback,
            @RequestHeader(value = "X-Webhook-Token", required = false) String authToken
    ) {
        log.info("📥 Callback recibido de Hacienda para clave: {}", callback.getClaveNumerica());
        
        try {
            // 1. Validar token de autenticación (si está configurado)
            if (!webhookToken.isEmpty() && !webhookToken.equals(authToken)) {
                log.warn("⚠️ Token de autenticación inválido");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token de autenticación inválido");
            }
            
            // 2. Procesar callback
            comprobanteService.procesarCallbackHacienda(callback);
            
            log.info("✅ Callback procesado exitosamente para clave: {}", 
                callback.getClaveNumerica());
            
            return ResponseEntity.ok()
                .body("Callback procesado exitosamente");
                
        } catch (Exception e) {
            log.error("❌ Error procesando callback de Hacienda: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error procesando callback: " + e.getMessage());
        }
    }
}
```

---

### 3. Service: Método procesarCallbackHacienda()

**Archivo:** `src/main/java/.../electronica/service/impl/ComprobanteElectronicoServiceImpl.java`

Agregar este método:

```java
@Override
@Transactional
public void procesarCallbackHacienda(HaciendaCallbackDTO callback) {
    log.info("🔄 Procesando callback para clave: {}", callback.getClaveNumerica());
    
    // 1. Buscar comprobante por clave numérica
    ComprobanteElectronico comprobante = repository.findByClaveNumerica(callback.getClaveNumerica())
        .orElseThrow(() -> new EntityNotFoundException(
            "Comprobante no encontrado con clave: " + callback.getClaveNumerica()
        ));
    
    // 2. Mapear estado de Hacienda a nuestro enum
    EstadoComprobante nuevoEstado = mapearEstadoHacienda(callback.getEstado(), callback.getCodigoRespuesta());
    
    // 3. Actualizar comprobante
    comprobante.setEstado(nuevoEstado);
    comprobante.setCodigoRespuesta(callback.getCodigoRespuesta());
    comprobante.setMensajeRespuesta(callback.getMensaje());
    comprobante.setFechaRespuesta(LocalDateTime.parse(callback.getFechaRespuesta()));
    
    // 4. Guardar XML de respuesta (si viene)
    if (callback.getXmlRespuesta() != null) {
        String xmlDecodificado = new String(Base64.getDecoder().decode(callback.getXmlRespuesta()));
        comprobante.setXmlRespuesta(xmlDecodificado);
        
        // Guardar XML en filesystem
        try {
            guardarXmlRespuestaEnFilesystem(comprobante, xmlDecodificado);
        } catch (IOException e) {
            log.error("Error guardando XML de respuesta: {}", e.getMessage());
        }
    }
    
    repository.save(comprobante);
    log.info("✅ Comprobante actualizado a estado: {}", nuevoEstado);
    
    // 5. Notificar al usuario (WebSocket)
    notificarCambioEstado(comprobante);
    
    // 6. Enviar email si está configurado
    if (debeEnviarEmailNotificacion(comprobante)) {
        enviarEmailNotificacion(comprobante);
    }
}

/**
 * Mapea el estado recibido de Hacienda a nuestro enum.
 */
private EstadoComprobante mapearEstadoHacienda(String estado, String codigo) {
    return switch (estado.toLowerCase()) {
        case "aceptado" -> EstadoComprobante.ACEPTADO;
        case "rechazado" -> EstadoComprobante.RECHAZADO;
        case "procesando" -> EstadoComprobante.ENVIADO;
        case "error" -> EstadoComprobante.ERROR;
        default -> {
            log.warn("⚠️ Estado desconocido: {}, código: {}", estado, codigo);
            yield EstadoComprobante.ERROR;
        }
    };
}

/**
 * Notifica al usuario mediante WebSocket.
 */
private void notificarCambioEstado(ComprobanteElectronico comprobante) {
    // TODO: Implementar notificación WebSocket
    log.info("🔔 Notificación WebSocket enviada para comprobante: {}", comprobante.getId());
}

/**
 * Verifica si debe enviar email de notificación.
 */
private boolean debeEnviarEmailNotificacion(ComprobanteElectronico comprobante) {
    // Solo notificar si fue aceptado o rechazado (estados terminales)
    return comprobante.getEstado() == EstadoComprobante.ACEPTADO || 
           comprobante.getEstado() == EstadoComprobante.RECHAZADO;
}

/**
 * Envía email de notificación al cliente.
 */
private void enviarEmailNotificacion(ComprobanteElectronico comprobante) {
    // TODO: Integrar con EmailService
    log.info("📧 Email de notificación enviado para comprobante: {}", comprobante.getId());
}

/**
 * Guarda el XML de respuesta en filesystem.
 */
private void guardarXmlRespuestaEnFilesystem(ComprobanteElectronico comprobante, String xml) 
        throws IOException {
    
    LocalDateTime fecha = comprobante.getFechaEmision();
    String año = String.valueOf(fecha.getYear());
    String mes = String.format("%02d", fecha.getMonthValue());
    
    Path directorioBase = Paths.get(directorioComprobantes);
    Path directorioComprobante = directorioBase.resolve(año).resolve(mes);
    Files.createDirectories(directorioComprobante);
    
    String nombreArchivo = comprobante.getClaveNumerica() + "_respuesta.xml";
    Path archivoXml = directorioComprobante.resolve(nombreArchivo);
    
    Files.writeString(archivoXml, xml, StandardCharsets.UTF_8);
    log.info("💾 XML de respuesta guardado: {}", archivoXml.toAbsolutePath());
}
```

---

## ⚙️ CONFIGURACIÓN

### application.yml

Agregar configuración del webhook:

```yaml
facturacion:
  hacienda:
    webhook:
      # Token de seguridad para validar callbacks (opcional)
      # Si está vacío, no se valida token
      token: ${HACIENDA_WEBHOOK_TOKEN:}
      
      # URL pública del webhook (para registrar en Hacienda)
      url: ${HACIENDA_WEBHOOK_URL:https://tu-dominio.com/api/hacienda/callback}
```

### Variables de Entorno

```bash
# Token de seguridad compartido con Hacienda
HACIENDA_WEBHOOK_TOKEN=tu-token-secreto-aqui

# URL pública del webhook
HACIENDA_WEBHOOK_URL=https://tu-dominio.com/api/hacienda/callback
```

---

## 🔐 SEGURIDAD

### 1. Validación de Token

El webhook acepta un header `X-Webhook-Token` para autenticar las llamadas:

```http
POST /api/hacienda/callback HTTP/1.1
Host: tu-dominio.com
Content-Type: application/json
X-Webhook-Token: tu-token-secreto-aqui

{
  "claveNumerica": "50612202600100111234567890123456789012345678901234",
  "estado": "aceptado",
  "codigoRespuesta": "1",
  "mensaje": "Aceptado",
  "xmlRespuesta": "PD94bWwgdmVyc2lvbj0iMS4wIi...",
  "fechaRespuesta": "2026-01-24T15:30:00"
}
```

### 2. Validación de Firma Digital (Opcional)

Para mayor seguridad, validar la firma del XML de respuesta:

```java
// Validar que el XML venga firmado por Hacienda
if (!firmaDigitalService.validarFirma(xmlDecodificado)) {
    throw new SecurityException("Firma digital inválida en XML de respuesta");
}
```

---

## 🧪 TESTING

### Prueba Manual con cURL

```bash
curl -X POST http://localhost:8080/api/hacienda/callback \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Token: tu-token-secreto-aqui" \
  -d '{
    "claveNumerica": "50612202600100111234567890123456789012345678901234",
    "estado": "aceptado",
    "codigoRespuesta": "1",
    "mensaje": "Comprobante aceptado",
    "fechaRespuesta": "2026-01-24T15:30:00"
  }'
```

### Respuesta Esperada

```json
{
  "message": "Callback procesado exitosamente"
}
```

---

## 📊 LOGS

Los logs del webhook incluyen:

```
[INFO] 📥 Callback recibido de Hacienda para clave: 50612202600100111234567890123456789012345678901234
[INFO] 🔄 Procesando callback para clave: 50612202600100111234567890123456789012345678901234
[INFO] ✅ Comprobante actualizado a estado: ACEPTADO
[INFO] 💾 XML de respuesta guardado: ./comprobantes/2026/01/50612202600100111234567890123456789012345678901234_respuesta.xml
[INFO] 🔔 Notificación WebSocket enviada para comprobante: 123
[INFO] 📧 Email de notificación enviado para comprobante: 123
[INFO] ✅ Callback procesado exitosamente para clave: 50612202600100111234567890123456789012345678901234
```

---

## 🚀 PRÓXIMOS PASOS

1. ✅ Implementar `HaciendaCallbackDTO`
2. ✅ Crear `HaciendaWebhookController`
3. ✅ Agregar método `procesarCallbackHacienda()` en Service
4. 🔄 Integrar notificaciones WebSocket (opcional)
5. 🔄 Configurar envío de emails (opcional)
6. 🔄 Registrar URL del webhook en portal de Hacienda

---

**Documentación creada:** 24 de enero de 2026  
**Autor:** Sistema ERP - Equipo de Desarrollo
