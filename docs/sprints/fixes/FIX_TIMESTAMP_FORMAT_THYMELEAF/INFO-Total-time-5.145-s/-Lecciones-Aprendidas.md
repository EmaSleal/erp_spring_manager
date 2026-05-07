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

