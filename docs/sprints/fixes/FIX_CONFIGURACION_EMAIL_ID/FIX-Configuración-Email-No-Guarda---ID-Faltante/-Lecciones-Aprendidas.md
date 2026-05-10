## 🎯 Lecciones Aprendidas

### 1. IDs son Críticos en REST

Para operaciones UPDATE, el backend **necesita el ID** para identificar el recurso:

```javascript
// CREATE (sin ID)
POST /api/configuracion/email
{ smtpHost: "smtp.gmail.com", ... }

// UPDATE (con ID)
PUT /api/configuracion/email
{ idConfiguracion: 1, smtpHost: "smtp.gmail.com", ... }
```

### 2. Frontend Debe Mantener Estado

El frontend debe **recordar** el ID entre operaciones:

```javascript
// Guardar en variable de módulo
this.idConfiguracion = datos.idConfiguracion;

// O usar localStorage
localStorage.setItem('emailConfigId', datos.idConfiguracion);

// O campo hidden en HTML
<input type="hidden" id="idConfiguracionEmail" value="">
```

### 3. Métodos HTTP Semánticos

```
POST   → Crear nuevo recurso (sin ID)
PUT    → Actualizar recurso completo (con ID)
PATCH  → Actualizar recurso parcial (con ID)
DELETE → Eliminar recurso (con ID)
```

### 4. Validar en Backend

El backend debe validar la presencia del ID:

```java
if (configuracion.getIdConfiguracion() == null) {
    // Crear nuevo
    return repository.save(configuracion);
} else {
    // Actualizar existente
    ConfiguracionEmail existente = repository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Configuración no existe"));
    // ... actualizar campos
    return repository.save(existente);
}
```

---

