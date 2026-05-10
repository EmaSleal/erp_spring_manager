## 🔄 Alternativas Consideradas

### Opción 1: Deshabilitar botón "Guardar" hasta que todas las líneas sean válidas ❌

**Pros:**
- Previene el error completamente
- Usuario no puede hacer guardado inválido

**Contras:**
- Más complejo de implementar
- Menos flexible (no permite omitir líneas vacías automáticamente)
- Peor UX (botón deshabilitado confunde)

**Decisión:** No implementar

---

### Opción 2: Validación en Backend ⚠️

**Pros:**
- Más seguro (última línea de defensa)
- Centraliza la lógica

**Contras:**
- Error se detecta tarde (después de enviar request)
- Desperdicia recursos de red/servidor
- Peor experiencia de usuario

**Decisión:** Mantener validación en frontend, pero considerar agregar backend también

---

### Opción 3: Filtrar líneas vacías automáticamente ✅ **(IMPLEMENTADA)**

**Pros:**
- Flexible
- Buena UX (no bloquea al usuario)
- Fácil de implementar
- Solución transparente

**Contras:**
- Usuario podría no darse cuenta de líneas omitidas

**Decisión:** **Implementar + agregar log en consola**

---

