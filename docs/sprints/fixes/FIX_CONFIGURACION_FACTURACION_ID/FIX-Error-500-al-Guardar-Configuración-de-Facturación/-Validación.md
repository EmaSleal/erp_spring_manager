## 🧪 Validación

### Pruebas Funcionales

1. ✅ **Crear nueva configuración:** Funciona correctamente
   - Envía JSON sin campo `id`
   - Backend usa `save()`
   
2. ✅ **Actualizar configuración existente:** Funciona correctamente
   - Envía JSON con campo `id`
   - Backend usa `update()`
   - No lanza error de "configuración activa duplicada"

3. ✅ **Validaciones de backend:** Funcionan correctamente
   - Serie de factura requerida
   - IGV entre 0 y 100
   - Número actual >= número inicial
   - Formato de número con placeholder `{numero}`

### Payload Correcto (Ejemplo)

**Crear (sin id):**
```json
{
  "serieFactura": "F001",
  "prefijoFactura": "FAC",
  "numeroInicial": 1,
  "numeroActual": 1,
  "formatoNumero": "{numero}",
  "igv": 18,
  "incluirIgvEnPrecio": false,
  "moneda": "PEN",
  "simboloMoneda": "S/",
  "decimales": 2,
  "activo": true
}
```

**Actualizar (con id):**
```json
{
  "id": 1,  // ✅ Correcto
  "serieFactura": "F001",
  "prefijoFactura": "FAC",
  "numeroInicial": 1,
  "numeroActual": 5,
  "formatoNumero": "{numero}",
  "igv": 18,
  "incluirIgvEnPrecio": false,
  "moneda": "PEN",
  "simboloMoneda": "S/",
  "decimales": 2,
  "activo": true
}
```

---

