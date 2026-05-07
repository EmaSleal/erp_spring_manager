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

