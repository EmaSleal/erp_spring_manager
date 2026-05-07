##  Diagramas de Arquitectura

### Flujo de Petición HTTP

```
Cliente (Browser)
    ↓
Controller (validación, recibe request)
    ↓
Service (lógica de negocio)
    ↓
Repository (acceso a datos)
    ↓
Base de Datos (PostgreSQL)
    ↓
Repository (mapeo a entidades)
    ↓
Service (procesamiento)
    ↓
Controller (preparar modelo)
    ↓
Thymeleaf Template (render HTML)
    ↓
Cliente (Browser)
```

### Arquitectura en Capas

```
┌─────────────────────────────────────────┐
│         Capa de Presentación            │
│  (Controllers + Thymeleaf Templates)    │
├─────────────────────────────────────────┤
│      Capa de Lógica de Negocio          │
│            (Services)                   │
├─────────────────────────────────────────┤
│         Capa de Acceso a Datos          │
│          (Repositories)                 │
├─────────────────────────────────────────┤
│         Capa de Dominio                 │
│       (Models/Entities)                 │
├─────────────────────────────────────────┤
│         Base de Datos                   │
│         (PostgreSQL)                    │
└─────────────────────────────────────────┘
```

### Módulos Funcionales

```
┌──────────────┬──────────────┬──────────────┐
│   Gestión    │   Gestión    │   Gestión    │
│   Clientes   │  Productos   │  Facturas    │
└──────────────┴──────────────┴──────────────┘
┌──────────────┬──────────────┬──────────────┐
│   Gestión    │   Reportes   │Configuración │
│   Usuarios   │  y Gráficos  │   Sistema    │
└──────────────┴──────────────┴──────────────┘
┌──────────────┬──────────────┬──────────────┐
│   Dashboard  │   WhatsApp   │    Email     │
│     KPIs     │   Webhook    │  Automático  │
└──────────────┴──────────────┴──────────────┘
```

---

