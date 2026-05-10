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

