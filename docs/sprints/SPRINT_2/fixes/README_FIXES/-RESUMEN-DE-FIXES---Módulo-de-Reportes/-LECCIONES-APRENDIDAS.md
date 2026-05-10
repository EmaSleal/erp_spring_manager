## 🔍 LECCIONES APRENDIDAS

### 1. Consistencia en Nomenclatura de Carpetas
**Problema:** Confusión entre `fragments/` y `components/`  
**Lección:** Establecer convención clara desde el inicio del proyecto  
**Recomendación:** Documentar estructura de carpetas en `COMPONENTES.md`

### 2. Protección contra Nulls en Streams
**Problema:** Method references no manejan nulls automáticamente  
**Lección:** Siempre verificar nulls explícitamente con Wrapper types  
**Patrón recomendado:**
```java
// ❌ Incorrecto
.filter(Objeto::getBoolean)

// ✅ Correcto
.filter(obj -> obj.getBoolean() != null && obj.getBoolean())
```

### 3. Validación de Campos del Modelo
**Problema:** Uso de campos inexistentes en templates  
**Lección:** Siempre verificar el modelo antes de escribir vistas  
**Recomendación:** Crear documento de referencia con campos disponibles por modelo

### 4. Testing Temprano
**Problema:** Errores detectados en testing manual, no en compilación  
**Lección:** Templates Thymeleaf no se validan en tiempo de compilación  
**Recomendación:** Testing manual inmediato después de crear cada vista

---

