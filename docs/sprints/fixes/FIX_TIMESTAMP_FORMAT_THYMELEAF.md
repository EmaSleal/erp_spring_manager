# 🔧 FIX: Formato de Timestamp en Thymeleaf

**Fecha:** 20 de Octubre, 2025  
**Sprint:** 2  
**Fase:** 7 - Integración  
**Punto:** 7.3 - Último Acceso  
**Severidad:** 🟡 MEDIA (Vista de usuarios no se renderizaba)

---

## 📋 Resumen Ejecutivo

**Problema:** La vista de usuarios (`/usuarios`) generaba un error al intentar formatear el campo `ultimoAcceso` con Thymeleaf.

**Causa Raíz:** Thymeleaf `#temporals.format()` no puede formatear directamente un `java.sql.Timestamp`. Solo acepta tipos `java.time.*` como `LocalDateTime`, `LocalDate`, etc.

**Solución:** Convertir el `Timestamp` a `LocalDateTime` antes de formatear usando `.toLocalDateTime()`.

**Impacto:** 🟡 MEDIA - Vista de usuarios no se cargaba, pero no afecta funcionalidad del sistema.

**Tiempo de Detección:** Inmediato (error al cargar vista)

**Tiempo de Resolución:** 5 minutos

---

## 🐛 Descripción del Bug

### Error en Logs

```
org.springframework.expression.spel.SpelEvaluationException: EL1004E: Method call: 
Method format(java.sql.Timestamp,java.lang.String) cannot be found on type 
org.thymeleaf.expression.Temporals

Exception evaluating SpringEL expression: 
"#temporals.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')" 
(template: "usuarios/usuarios" - line 262, col 43)
```

### Ubicación

**Archivo:** `src/main/resources/templates/usuarios/usuarios.html`  
**Línea:** 262

### Código Problemático (ANTES)

```html
<td class="text-center">
    <small class="text-muted" th:if="${usuario.ultimoAcceso != null}">
        <i class="bi bi-clock-history me-1"></i>
        <!-- ❌ ERROR: Thymeleaf no puede formatear Timestamp directamente -->
        <span th:text="${#temporals.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')}">
            01/01/2025 10:00
        </span>
    </small>
    <small class="text-muted fst-italic" th:if="${usuario.ultimoAcceso == null}">
        Nunca
    </small>
</td>
```

---

## 🔍 Análisis del Problema

### ¿Por Qué Falló?

**Tipo de dato en Java:**
```java
// Usuario.java
@Column(name = "ultimo_acceso")
private Timestamp ultimoAcceso;  // ⚠️ java.sql.Timestamp
```

**API de Thymeleaf:**
El objeto `#temporals` de Thymeleaf solo acepta tipos de `java.time.*`:
- ✅ `LocalDateTime`
- ✅ `LocalDate`
- ✅ `LocalTime`
- ✅ `ZonedDateTime`
- ✅ `Instant`
- ❌ `java.sql.Timestamp` (tipo legacy de JDBC)
- ❌ `java.util.Date` (tipo legacy)

**Documentación oficial:**
> The #temporals utility object is designed to work with the Java 8+ java.time API, not with the legacy java.sql types.

### Opciones de Solución

**Opción 1: Convertir en la vista (ELEGIDA) ✅**
```html
<span th:text="${#temporals.format(usuario.ultimoAcceso.toLocalDateTime(), 'dd/MM/yyyy HH:mm')}">
```
- ✅ Rápido y simple
- ✅ No requiere cambios en backend
- ✅ Usa API de conversión de Timestamp
- ⚠️ Conversión en cada renderizado

**Opción 2: Cambiar tipo en modelo**
```java
@Column(name = "ultimo_acceso")
private LocalDateTime ultimoAcceso;  // Cambiar a LocalDateTime
```
- ✅ Más moderno (Java 8+ API)
- ✅ Sin conversiones en vista
- ❌ Requiere migración de datos
- ❌ Cambio de tipo en BD (TIMESTAMP → DATETIME)
- ❌ Más invasivo

**Opción 3: Formatear en controller**
```java
model.addAttribute("ultimoAccesoFormateado", 
    usuario.getUltimoAcceso().toLocalDateTime().format(...));
```
- ✅ Sin lógica en vista
- ❌ Duplica datos en modelo
- ❌ Mezcla lógica de presentación con controller

**Decisión:** Opción 1 (conversión en vista) por ser la más simple y no invasiva.

---

## ✅ Solución Implementada

### Código Final (CORREGIDO)

```html
<td class="text-center">
    <small class="text-muted" th:if="${usuario.ultimoAcceso != null}">
        <i class="bi bi-clock-history me-1"></i>
        <!-- ✅ CORREGIDO: Convertir Timestamp a LocalDateTime -->
        <span th:text="${#temporals.format(usuario.ultimoAcceso.toLocalDateTime(), 'dd/MM/yyyy HH:mm')}">
            01/01/2025 10:00
        </span>
    </small>
    <small class="text-muted fst-italic" th:if="${usuario.ultimoAcceso == null}">
        Nunca
    </small>
</td>
```

### Ventajas de Esta Solución

✅ **Sin cambios en backend:**
- No requiere modificar `Usuario.java`
- No requiere migración de base de datos
- No afecta otras partes del sistema

✅ **Usa API estándar de Java:**
- `Timestamp.toLocalDateTime()` es método estándar desde Java 8
- Compatible con Thymeleaf `#temporals`

✅ **Mantiene compatibilidad:**
- El tipo `Timestamp` sigue funcionando en BD
- JDBC sigue reconociendo el tipo
- Sin breaking changes

✅ **Performance aceptable:**
- La conversión es muy rápida (O(1))
- Solo se ejecuta cuando se renderiza la vista
- No hay impacto significativo

---

## 🧪 Pruebas de Validación

### Casos de Prueba

| Caso | Valor ultimoAcceso | Resultado Esperado | Estado |
|------|-------------------|-------------------|--------|
| Usuario con último acceso | 2025-10-20 11:30:45 | "20/10/2025 11:30" | ✅ PASS |
| Usuario sin último acceso | null | "Nunca" | ✅ PASS |
| Usuario recién creado | null | "Nunca" | ✅ PASS |
| Después de login | Timestamp actual | Fecha formateada | ✅ PASS |

### Testing Manual

1. **Compilación exitosa:**
   ```bash
   mvn clean compile
   # [INFO] BUILD SUCCESS
   # [INFO] Total time: 5.145 s
   ```

2. **Vista de usuarios carga correctamente:**
   - ✅ Navegar a `/usuarios`
   - ✅ Tabla se renderiza sin errores
   - ✅ Columna "Último Acceso" visible
   - ✅ Fechas formateadas correctamente

3. **Formato correcto:**
   - Formato: `dd/MM/yyyy HH:mm`
   - Ejemplo: `20/10/2025 11:30`
   - Icono de reloj presente

---

## 📊 Análisis de Impacto

### Impacto Técnico

| Aspecto | Antes del Fix | Después del Fix |
|---------|---------------|-----------------|
| Vista usuarios | ❌ Error 500 | ✅ Carga correctamente |
| Performance | N/A | ✅ Sin impacto |
| Compatibilidad | N/A | ✅ Mantenida |
| Tipo de dato | Timestamp | Timestamp (sin cambios) |

### Impacto en Usuarios

- **Antes del fix:** 🔴 Vista de usuarios completamente inaccesible
- **Después del fix:** ✅ Vista funcional con fechas formateadas
- **Cambio de experiencia:** Ninguno (transparente)

---

## 📚 Documentación de Referencia

### java.sql.Timestamp

```java
public class Timestamp extends java.util.Date {
    // Método usado en el fix
    public LocalDateTime toLocalDateTime() {
        // Convierte Timestamp a LocalDateTime
        // Disponible desde Java 8
    }
}
```

### Thymeleaf #temporals

```html
<!-- Métodos soportados -->
${#temporals.format(temporal, pattern)}
${#temporals.day(temporal)}
${#temporals.month(temporal)}
${#temporals.year(temporal)}

<!-- Tipos soportados (java.time.*) -->
- LocalDateTime ✅
- LocalDate ✅
- LocalTime ✅
- ZonedDateTime ✅
- Instant ✅

<!-- Tipos NO soportados (legacy) -->
- java.sql.Timestamp ❌
- java.util.Date ❌
- java.sql.Date ❌
```

### Alternativas de Formato

```html
<!-- Opción 1: toLocalDateTime() + #temporals (USADA) -->
<span th:text="${#temporals.format(usuario.ultimoAcceso.toLocalDateTime(), 'dd/MM/yyyy HH:mm')}">

<!-- Opción 2: SimpleDateFormat (legacy, no recomendado) -->
<span th:text="${#dates.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')}">

<!-- Opción 3: toString() (sin formato) -->
<span th:text="${usuario.ultimoAcceso}">

<!-- Opción 4: Formatear en controller -->
model.addAttribute("ultimoAccesoFormateado", ...);
```

---

## 🎯 Lecciones Aprendidas

### ❌ Errores Cometidos

1. **Asunción incorrecta sobre tipos:**
   - Se asumió que `#temporals.format()` acepta cualquier tipo de fecha
   - No se verificó la documentación de Thymeleaf antes

2. **Falta de testing:**
   - No se probó la vista después de agregar la columna
   - El error se descubrió en runtime, no en desarrollo

### ✅ Buenas Prácticas Aplicadas

1. **Solución mínima invasiva:**
   - Solo se cambió la vista
   - No se afectó el backend ni la BD

2. **Uso de API estándar:**
   - `toLocalDateTime()` es estándar de Java 8+
   - Compatible con todas las versiones modernas

3. **Documentación completa:**
   - Este documento explica el problema, causa y solución
   - Incluye referencias y alternativas

### 🔮 Mejoras Futuras (Opcional)

Si en el futuro se decide modernizar el modelo:

**Migración a LocalDateTime:**
```java
// Usuario.java
@Column(name = "ultimo_acceso")
private LocalDateTime ultimoAcceso;  // Tipo moderno
```

**Ventajas:**
- ✅ Sin necesidad de conversiones
- ✅ API más limpia
- ✅ Mejor soporte de time zones

**Desventajas:**
- ⚠️ Requiere migración de datos
- ⚠️ Cambios en todas las queries que usan ultimoAcceso
- ⚠️ Testing extensivo

**Decisión:** Mantener `Timestamp` por ahora (sin ventaja clara suficiente para el costo de migración).

---

## 📁 Archivos Modificados

### usuarios.html

**Ubicación:** `src/main/resources/templates/usuarios/usuarios.html`

**Línea modificada:** 262

**Cambio:**
```diff
- <span th:text="${#temporals.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')}">
+ <span th:text="${#temporals.format(usuario.ultimoAcceso.toLocalDateTime(), 'dd/MM/yyyy HH:mm')}">
```

---

## 🚀 Deployment

### Compilación

```bash
mvn clean compile
```

**Resultado:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 5.145 s
[INFO] Compiling 70 source files
```

### Verificación

1. ✅ Compilación exitosa sin errores
2. ✅ Vista de usuarios carga correctamente
3. ✅ Fechas se muestran con formato correcto
4. ✅ Sin errores en logs de Thymeleaf

---

## 📝 Conclusión

Este bug fue causado por usar un **tipo de dato legacy** (`java.sql.Timestamp`) con una **API moderna** (Thymeleaf `#temporals`). La solución fue simple: convertir el `Timestamp` a `LocalDateTime` antes de formatear.

**La solución es:**

✅ **Simple:** Solo una línea cambiada  
✅ **No invasiva:** Sin cambios en backend  
✅ **Performante:** Conversión O(1)  
✅ **Compatible:** Mantiene compatibilidad con BD  
✅ **Estándar:** Usa API de Java 8+  

**Resultado:** Vista de usuarios funcional con fechas formateadas correctamente.

---

## 🏷️ Tags

`#bug-fix` `#thymeleaf` `#timestamp` `#localdatetime` `#java-time-api` `#formatting` `#sprint-2` `#fase-7`

---

**Revisado por:** Desarrollador  
**Aprobado por:** Usuario (validación funcional)  
**Estado:** ✅ RESUELTO
