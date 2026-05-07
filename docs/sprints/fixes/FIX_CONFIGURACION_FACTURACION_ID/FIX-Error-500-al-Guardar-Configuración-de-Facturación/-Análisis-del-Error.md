## 🔍 Análisis del Error

### Causa Raíz

**Inconsistencia en el nombre del campo ID:**

El frontend estaba enviando el campo como `idConfiguracion`, pero el backend esperaba `id`:

**Frontend (configuracion-facturacion.js - INCORRECTO):**
```javascript
if (idConfiguracion) {
    // Actualizar
    datos.idConfiguracion = parseInt(idConfiguracion);  // ❌ INCORRECTO
    response = await Configuracion.put(this.API_URL, datos);
}
```

**Backend (ConfiguracionFacturacion.java):**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id")
private Integer id;  // ✅ Espera "id"
```

**Backend (ConfiguracionFacturacionRestController.java - línea 80):**
```java
if (configuracion.getId() != null && configuracion.getId() > 0) {
    guardada = configuracionFacturacionService.update(configuracion);
} else {
    guardada = configuracionFacturacionService.save(configuracion);
}
```

### Consecuencia

1. El frontend enviaba `idConfiguracion` en el JSON
2. El backend no reconocía el campo y `configuracion.getId()` retornaba `null`
3. Siempre intentaba hacer `save()` en lugar de `update()`
4. El método `save()` validaba si ya existe una configuración activa
5. Como ya existía, lanzaba `IllegalStateException: "Ya existe una configuración de facturación activa"`
6. Esto se traducía en HTTP 500

---

