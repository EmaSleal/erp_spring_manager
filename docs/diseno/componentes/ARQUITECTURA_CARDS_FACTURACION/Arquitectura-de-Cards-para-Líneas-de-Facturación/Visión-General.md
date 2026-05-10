## Visión General

La facturación maneja una lista de líneas de productos en **dos formatos simultáneamente**:
- **Tabla HTML** (desktop): Vista completa con todas las columnas
- **Cards Bootstrap** (móvil): Vista responsiva con diseño vertical

### Principio Fundamental: **Tabla = Fuente de Verdad**

La tabla (`#lineas-body`) es el **único almacén de datos** durante la sesión. Los cards son solo una **representación visual** de los datos de la tabla.

```
┌─────────────────────────────────────┐
│  Tabla (#lineas-body)               │
│  ← Fuente de verdad durante sesión  │
│  ← Datos se persisten aquí al POST  │
└────────────┬────────────────────────┘
             │
      ┌──────┴──────────────┐
      │                     │
      ▼                     ▼
 [Desktop View]        [Mobile View]
   (d-none d-md-block)  (d-md-none)
   Mostrar tabla        Mostrar cards
```

---

