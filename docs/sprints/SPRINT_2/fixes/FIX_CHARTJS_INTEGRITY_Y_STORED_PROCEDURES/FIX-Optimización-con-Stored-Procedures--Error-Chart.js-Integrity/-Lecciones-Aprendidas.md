## 📚 Lecciones Aprendidas

1. **Integrity Hash de CDNs:**
   - Los hashes pueden quedar desactualizados
   - Solo usar cuando sea crítico para seguridad
   - Verificar antes de agregar en producción

2. **Optimización de Queries:**
   - **SIEMPRE** procesar datos en la base de datos
   - Evitar `findAll()` + Stream API
   - Usar Stored Procedures para lógica compleja

3. **Balance Java vs SQL:**
   - Java: Lógica de negocio, validaciones
   - SQL: Agregaciones, filtrados, ordenamientos

---

