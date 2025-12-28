# FIX: Error 500 al Guardar Configuración de Facturación

**Fecha:** 1 de diciembre de 2025  
**Sprint:** Sprint 4 - Fase 1  
**Severidad:** 🟡 MEDIA  
**Estado:** ✅ RESUELTO

---

## 📋 Descripción del Problema

Al intentar guardar la configuración de **facturación** y **empresa** en `/configuracion`, se producía un error 500:

```
POST http://localhost:8080/api/configuracion/facturacion 500 (Internal Server Error)
POST http://localhost:8080/api/configuracion/empresa 500 (Internal Server Error)
```

**Console Error:**
```javascript
Error en POST: Error: HTTP error! status: 500
❌ Error guardando configuración: Error: HTTP error! status: 500
```

**Afecta a:**
- ❌ Configuración de Facturación (`configuracion-facturacion.js`)
- ❌ Configuración de Empresa (`configuracion-empresa.js`)

---

## 🔍 Análisis del Error

### Causa Raíz

**Inconsistencia en el nombre del campo ID:**

El frontend estaba enviando el campo como `idConfiguracion`, pero el backend esperaba `id`:

**Frontend (configuracion-facturacion.js - INCORRECTO):**
```javascript
if (idConfiguracion) {
    // Actualizar
    datos.idConfiguracion = parseInt(idConfiguracion);  // ❌ INCORRECTO
    response = await Configuracion.put(this.API_URL, datos);
}
```

**Backend (ConfiguracionFacturacion.java):**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id")
private Integer id;  // ✅ Espera "id"
```

**Backend (ConfiguracionFacturacionRestController.java - línea 80):**
```java
if (configuracion.getId() != null && configuracion.getId() > 0) {
    guardada = configuracionFacturacionService.update(configuracion);
} else {
    guardada = configuracionFacturacionService.save(configuracion);
}
```

### Consecuencia

1. El frontend enviaba `idConfiguracion` en el JSON
2. El backend no reconocía el campo y `configuracion.getId()` retornaba `null`
3. Siempre intentaba hacer `save()` en lugar de `update()`
4. El método `save()` validaba si ya existe una configuración activa
5. Como ya existía, lanzaba `IllegalStateException: "Ya existe una configuración de facturación activa"`
6. Esto se traducía en HTTP 500

---

## ✅ Solución Implementada

### Cambios en Frontend

#### 1. Configuración de Facturación

**Archivo:** `src/main/resources/static/js/configuracion-facturacion.js`

**Línea 51 - ANTES:**
```javascript
document.getElementById('idConfiguracionFacturacion').value = datos.idConfiguracion || '';  // ❌
```

**Línea 51 - DESPUÉS:**
```javascript
document.getElementById('idConfiguracionFacturacion').value = datos.id || '';  // ✅
```

**Línea 111 - ANTES:**
```javascript
if (idConfiguracion) {
    // Actualizar
    datos.idConfiguracion = parseInt(idConfiguracion);  // ❌
    response = await Configuracion.put(this.API_URL, datos);
}
```

**Línea 111 - DESPUÉS:**
```javascript
if (idConfiguracion) {
    // Actualizar - usar 'id' en lugar de 'idConfiguracion'
    datos.id = parseInt(idConfiguracion);  // ✅
    response = await Configuracion.put(this.API_URL, datos);
}
```

---

#### 2. Configuración de Empresa

**Archivo:** `src/main/resources/static/js/configuracion-empresa.js`

**Línea 51 - ANTES:**
```javascript
document.getElementById('idConfiguracionEmpresa').value = datos.idConfiguracion || '';  // ❌
```

**Línea 51 - DESPUÉS:**
```javascript
document.getElementById('idConfiguracionEmpresa').value = datos.id || '';  // ✅
```

**Línea 125 - ANTES:**
```javascript
if (idConfiguracion) {
    // Actualizar
    datos.idConfiguracion = parseInt(idConfiguracion);  // ❌
    response = await Configuracion.put(this.API_URL, datos);
}
```

**Línea 125 - DESPUÉS:**
```javascript
if (idConfiguracion) {
    // Actualizar - usar 'id' en lugar de 'idConfiguracion'
    datos.id = parseInt(idConfiguracion);  // ✅
    response = await Configuracion.put(this.API_URL, datos);
}
```

---

## 🧪 Validación

### Pruebas Funcionales

1. ✅ **Crear nueva configuración:** Funciona correctamente
   - Envía JSON sin campo `id`
   - Backend usa `save()`
   
2. ✅ **Actualizar configuración existente:** Funciona correctamente
   - Envía JSON con campo `id`
   - Backend usa `update()`
   - No lanza error de "configuración activa duplicada"

3. ✅ **Validaciones de backend:** Funcionan correctamente
   - Serie de factura requerida
   - IGV entre 0 y 100
   - Número actual >= número inicial
   - Formato de número con placeholder `{numero}`

### Payload Correcto (Ejemplo)

**Crear (sin id):**
```json
{
  "serieFactura": "F001",
  "prefijoFactura": "FAC",
  "numeroInicial": 1,
  "numeroActual": 1,
  "formatoNumero": "{numero}",
  "igv": 18,
  "incluirIgvEnPrecio": false,
  "moneda": "PEN",
  "simboloMoneda": "S/",
  "decimales": 2,
  "activo": true
}
```

**Actualizar (con id):**
```json
{
  "id": 1,  // ✅ Correcto
  "serieFactura": "F001",
  "prefijoFactura": "FAC",
  "numeroInicial": 1,
  "numeroActual": 5,
  "formatoNumero": "{numero}",
  "igv": 18,
  "incluirIgvEnPrecio": false,
  "moneda": "PEN",
  "simboloMoneda": "S/",
  "decimales": 2,
  "activo": true
}
```

---

## 📊 Flujo Corregido

```
┌─────────────────────────────────────────┐
│ Usuario edita configuración facturación │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ configuracion-facturacion.js            │
│ - Lee idConfiguracionFacturacion        │
│ - Si existe: datos.id = parseInt(id) ✅ │
│ - Envía PUT /api/configuracion/facturacion│
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ ConfiguracionFacturacionRestController  │
│ - Recibe ConfiguracionFacturacion       │
│ - if (config.getId() != null) ✅        │
│   → update() (correcto)                 │
│ - else → save() (nuevo)                 │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ ConfiguracionFacturacionServiceImpl     │
│ update():                               │
│ 1. Busca existente por ID ✅            │
│ 2. Valida datos                         │
│ 3. Valida serie (si cambió)             │
│ 4. Guarda con save() de JPA             │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ ✅ Configuración actualizada exitosamente│
│ - Caché invalidado                      │
│ - Frontend recarga datos                │
└─────────────────────────────────────────┘
```

---

## 🔄 Impacto

### Positivo
- ✅ Actualizaciones de configuración funcionan correctamente
- ✅ No más errores 500 al guardar
- ✅ Lógica de save/update funciona como se esperaba
- ✅ Validaciones de backend operativas

### Sin efectos secundarios
- ✅ No afecta creación de nuevas configuraciones
- ✅ No modifica lógica de backend
- ✅ Cambio mínimo (1 línea de código)

---

## 🎯 Lecciones Aprendidas

1. **Consistencia de nombres:** Los campos deben tener el mismo nombre en frontend y backend
2. **Logging mejorado:** El backend debería loggear el JSON recibido para debugging
3. **Validación:** El controller debería validar que los campos requeridos existen
4. **Manejo de errores:** El frontend debería mostrar el mensaje de error específico del backend

---

## 🔧 Mejoras Futuras Recomendadas

### 1. Agregar logging en el controller

```java
@PutMapping
public ResponseEntity<?> guardarConfiguracion(@RequestBody ConfiguracionFacturacion configuracion) {
    log.debug("JSON recibido: {}", configuracion);  // ⭐ AGREGAR
    
    if (configuracion.getId() != null) {
        log.debug("ID recibido: {}", configuracion.getId());  // ⭐ AGREGAR
    }
    
    // ... resto del código
}
```

### 2. Mejorar manejo de errores en frontend

```javascript
} catch (error) {
    console.error('❌ Error guardando configuración:', error);
    
    // ⭐ Intentar obtener mensaje específico del servidor
    let mensaje = 'Error al guardar. Por favor intenta nuevamente.';
    if (error.message) {
        mensaje = error.message;
    }
    
    Configuracion.mostrarAlertaEn('alert-facturacion-container', 'danger', mensaje);
}
```

### 3. Usar DTOs con validación

```java
@Data
public class ConfiguracionFacturacionDTO {
    @NotNull(message = "El ID es requerido para actualizar")
    private Integer id;
    
    @NotBlank(message = "La serie es requerida")
    private String serieFactura;
    
    // ... resto de campos con validaciones
}
```

---

## 📝 Archivos Modificados

```
src/main/resources/static/js/
├── configuracion-facturacion.js (+2 líneas modificadas)
│   ├── Línea 51: datos.idConfiguracion → datos.id (al cargar)
│   └── Línea 111: datos.idConfiguracion → datos.id (al guardar)
└── configuracion-empresa.js (+2 líneas modificadas)
    ├── Línea 51: datos.idConfiguracion → datos.id (al cargar)
    └── Línea 125: datos.idConfiguracion → datos.id (al guardar)
```

**Total:** 2 archivos, 4 líneas modificadas

---

## 📚 Referencias

- [Spring Data JPA - Save vs Update](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.entity-persistence)
- [Jackson ObjectMapper - Field Naming](https://github.com/FasterXML/jackson-databind/wiki/Jackson-Annotations)
- [Lombok @Data](https://projectlombok.org/features/Data)

---

**Autor:** Copilot AI Assistant  
**Revisado por:** Usuario  
**Estado final:** ✅ RESUELTO - Requiere Ctrl+F5 en navegador
