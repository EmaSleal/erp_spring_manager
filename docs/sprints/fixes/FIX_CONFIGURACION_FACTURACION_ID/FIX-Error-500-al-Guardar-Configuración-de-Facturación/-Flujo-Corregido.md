## 📊 Flujo Corregido

```
┌─────────────────────────────────────────┐
│ Usuario edita configuración facturación │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ configuracion-facturacion.js            │
│ - Lee idConfiguracionFacturacion        │
│ - Si existe: datos.id = parseInt(id) ✅ │
│ - Envía PUT /api/configuracion/facturacion│
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ ConfiguracionFacturacionRestController  │
│ - Recibe ConfiguracionFacturacion       │
│ - if (config.getId() != null) ✅        │
│   → update() (correcto)                 │
│ - else → save() (nuevo)                 │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ ConfiguracionFacturacionServiceImpl     │
│ update():                               │
│ 1. Busca existente por ID ✅            │
│ 2. Valida datos                         │
│ 3. Valida serie (si cambió)             │
│ 4. Guarda con save() de JPA             │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│ ✅ Configuración actualizada exitosamente│
│ - Caché invalidado                      │
│ - Frontend recarga datos                │
└─────────────────────────────────────────┘
```

---

