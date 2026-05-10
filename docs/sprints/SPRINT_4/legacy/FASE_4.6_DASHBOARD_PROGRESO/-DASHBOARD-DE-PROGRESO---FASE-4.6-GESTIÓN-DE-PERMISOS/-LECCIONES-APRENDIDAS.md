## 💡 LECCIONES APRENDIDAS

1. **Lombok y Colecciones EAGER:**
   - Problema: `@Data` incluye colecciones en hashCode
   - Solución: Usar `@EqualsAndHashCode(exclude = {...})`
   - Aplicar en todas las entidades con relaciones bidireccionales

2. **Filtrado Manual vs Specification:**
   - Decisión: Filtrado en memoria para <100 registros
   - Beneficio: Código más simple
   - Considerar: Migrar a Specification si dataset crece

3. **Inmutabilidad de Códigos:**
   - Razón: Usados en anotaciones @PreAuthorize
   - Implementación: Campo de solo lectura en UI
   - Validar: Antes de permitir edición

---

