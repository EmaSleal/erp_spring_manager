## Diferencias Entre Crear y Editar

| Aspecto | Crear (modal) | Editar (página) |
|--------|---|---|
| **Ubicación** | Modal `#nuevaFacturaModal` | Página `/facturas/editar/{id}` |
| **Template** | `add-form.html` | `form.html` |
| **Líneas IDs** | Negativos (-1, -2, -3) | Positivos (de BD) |
| **Factura creación** | POST `/facturas/guardar` | Factura ya existe |
| **Líneas guardado** | PUT `/lineas-factura/actualizar` | PUT `/lineas-factura/actualizar` |
| **JS utilizado** | `editar-factura.js` | `editar-factura.js` (mismo) |
| **Detección** | `facturaId = 0` inicialmente | `facturaId` extraído de URL |
| **Carga de líneas** | Usuario agrega manualmente | Cargadas automáticamente |

---

