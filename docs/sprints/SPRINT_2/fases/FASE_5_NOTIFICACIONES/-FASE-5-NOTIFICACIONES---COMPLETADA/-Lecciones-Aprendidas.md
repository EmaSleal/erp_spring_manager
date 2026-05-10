## 🎓 Lecciones Aprendidas

### 1. Validación Exhaustiva de Tipos
- Enum comparisons en JPQL deben usar valores exactos del enum
- Hibernate valida queries en startup
- Importante revisar tipos en toda la cadena (Java, JPA, SQL)

### 2. Carga de Model Attributes
- Thymeleaf fragments requieren todos los objetos en modelo
- Fragments se cargan simultáneamente
- Mejor cargar todo de una vez que hacer múltiples cargas parciales

### 3. Post-Redirect-Get Pattern
- Redirect debe ir a endpoint que carga todas las dependencias
- Evitar redirects a endpoints específicos
- Usar parámetros para controlar la vista activa

### 4. Consistencia en Auditoría
- `AuditorAware<T>` define el tipo para TODAS las entidades
- Campos de auditoría deben coincidir con el tipo del AuditorAware
- Validar nueva entidades contra el estándar del proyecto

### 5. Testing Incremental
- Probar cada funcionalidad inmediatamente después de implementarla
- No acumular múltiples features sin testing
- Documentar errores y fixes inmediatamente

---

