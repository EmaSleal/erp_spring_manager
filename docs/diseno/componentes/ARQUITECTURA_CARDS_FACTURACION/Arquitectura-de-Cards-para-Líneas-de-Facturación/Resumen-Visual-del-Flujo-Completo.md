## Resumen Visual del Flujo Completo

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  1. Usuario abre modal "Nueva Factura"                 │
│     → resetForm() limpia todo                          │
│     → temporalLineaId = -1                             │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  2. Usuario presiona "Siguiente"                        │
│     → POST /facturas/guardar                            │
│     → Guarda Promise en facturaCreadaPromise            │
│     → Enable botón "Guardar Líneas"                     │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  3. Usuario agrega líneas (botón "Agregar línea")      │
│     → addLinea()                                        │
│     → Inyecta fila en tabla                             │
│     → Inyecta card en móvil                             │
│     → Ambos usan ID -1, -2, -3...                      │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  4. Usuario edita campos (cantidad, producto, etc)     │
│     → actualizarProductoSeleccionado()                  │
│     → Actualiza TABLA (fuente de verdad)                │
│     → sincronizarCardLinea() ← Actualiza SOLO ese card  │
│     → Recalcula totales                                 │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  5. Usuario elimina línea (botón delete)               │
│     → removeLinea()                                     │
│     → Elimina fila de tabla                             │
│     → obtenerCardLineaPorId() busca card                │
│     → Elimina SOLO ese card                             │
│     → (NO llama actualizarVistaLineas() ← importante!) │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  6. Usuario presiona "Guardar Líneas"                  │
│     → guardarLineas()                                   │
│     → await facturaCreadaPromise ← Espera factura OK   │
│     → facturaId ahora > 0                               │
│     → Recolecta datos de TABLA                          │
│     → Reemplaza -1, -2, -3 con facturaId               │
│     → PUT /lineas-factura/actualizar                    │
│     → Success ✓                                         │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

