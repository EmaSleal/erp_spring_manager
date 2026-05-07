## 📊 ARCHIVOS MODIFICADOS/CREADOS

### Nuevos
1. ✅ `src/main/java/api/astro/whats_orders_manager/config/SecurityConfig.java`

### Modificados
1. ✅ `src/main/java/api/astro/whats_orders_manager/models/Usuario.java`
   - Agregado campo `roles` (@ElementCollection)
   
2. ✅ `src/main/java/api/astro/whats_orders_manager/controllers/*.java` (8 controladores)
   - Agregadas anotaciones `@PreAuthorize`
   
3. ✅ `src/main/resources/templates/**/*.html` (todas las vistas)
   - Agregado namespace `xmlns:sec`
   - Agregadas directivas `sec:authorize`

4. ✅ `src/main/resources/schema.sql`
   - Agregada tabla `usuario_rol`

---

