## 🔗 INTEGRACIÓN CON BACKEND

### Endpoints REST disponibles

**Configuración Email:**
```http
GET    /api/configuracion/email              → Obtener configuración
POST   /api/configuracion/email              → Crear configuración
PUT    /api/configuracion/email              → Actualizar configuración
POST   /api/configuracion/email/probar       → Enviar email de prueba
PATCH  /api/configuracion/email/estado       → Cambiar estado activo
GET    /api/configuracion/email/validar      → Validar configuración
```

**Parámetros del Sistema:**
```http
GET    /api/configuracion/parametros                     → Obtener todos
GET    /api/configuracion/parametros/categoria/{cat}     → Filtrar por categoría
GET    /api/configuracion/parametros/{clave}             → Obtener por clave
GET    /api/configuracion/parametros/editables/lista    → Solo editables
POST   /api/configuracion/parametros                     → Crear parámetro
PUT    /api/configuracion/parametros/{clave}             → Actualizar completo
PATCH  /api/configuracion/parametros/{clave}             → Actualizar valor
DELETE /api/configuracion/parametros/{clave}             → Eliminar parámetro
POST   /api/configuracion/parametros/inicializar         → Inicializar 17 defaults
```

**Seguridad:**
- ✅ Todos los endpoints requieren rol `ADMIN` (`@PreAuthorize`)
- ✅ Password de SMTP enmascarado en respuestas ("********")
- ✅ Validación de datos en backend

---

