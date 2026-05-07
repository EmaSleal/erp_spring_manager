## 🎓 LECCIONES APRENDIDAS

### **1. Patrón Singleton en BD**
- Una sola configuración activa garantiza consistencia
- Query `WHERE activo = true` simple y eficiente
- Validación en service antes de guardar

### **2. Thread-Safety con @Transactional**
- Incremento atómico del número
- Rollback automático si falla el guardado
- No se pierden números en la secuencia

### **3. Preview en Tiempo Real**
- JavaScript + oninput = UX mejorada
- String.replace() para placeholders
- String.padStart() para formato consistente

### **4. Validaciones en 3 Capas**
1. **HTML5:** required, pattern, min, max
2. **JavaScript:** Lógica compleja (formato, moneda)
3. **Backend:** Validación definitiva en service

### **5. Documentación Completa**
- JavaDoc en cada método
- Comentarios explicativos en lógica compleja
- Documentación técnica externa (MD)
- Ejemplos de uso claros

---

