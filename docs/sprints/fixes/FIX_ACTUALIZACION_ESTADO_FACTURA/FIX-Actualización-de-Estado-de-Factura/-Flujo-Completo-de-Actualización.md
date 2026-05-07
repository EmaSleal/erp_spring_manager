## 🔄 Flujo Completo de Actualización

```
┌─────────────────────────────────────────────┐
│  Usuario cambia estado en el formulario     │
│  Select: Pendiente → Entregado              │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│  1. Click en "Guardar Factura"              │
│  → función guardarLineas()                  │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│  2. PUT /lineas-factura/actualizar          │
│  → Actualiza cantidad, precio, subtotal     │
│  → SP: sp_actualizar_linea_factura          │
│  → SP: ActualizarTotalFactura               │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│  3. PUT /facturas/actualizar-estado/{id}    │
│  ?entregado=true                            │
│  → FacturaController.actualizarEstadoFactura│
│  → facturaService.save(factura)             │
│  → UPDATE factura SET entregado=true        │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│  4. SweetAlert de éxito                     │
│  → location.reload()                        │
│  → Vista actualizada con nuevo estado       │
└─────────────────────────────────────────────┘
```

---

