## 🏗️ ARQUITECTURA ACTUAL

### Componentes del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                     │
├─────────────────────────────────────────────────────────────┤
│  Templates (Thymeleaf)                                      │
│  - sidebar.html                 [10 usos sec:authorize]     │
│  - clientes.html                [3 usos sec:authorize]      │
│  - productos.html               [1 uso sec:authorize]       │
│  - facturas.html                [5 usos sec:authorize]      │
│  - usuarios.html                [7 usos sec:authorize]      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE CONTROLLERS                      │
├─────────────────────────────────────────────────────────────┤
│  @PreAuthorize en Métodos                                   │
│  - ClienteController            [3 métodos protegidos]      │
│  - ProductoController           [4 métodos protegidos]      │
│  - FacturaController            [6 métodos protegidos]      │
│  - ConfiguracionController      [2 métodos protegidos]      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE SERVICIOS                        │
├─────────────────────────────────────────────────────────────┤
│  PermisoService                                             │
│  - tienePermisoByUsername()                                 │
│  - tieneAlgunPermisoByUsername()                            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE DATOS (HARDCODED)                │
├─────────────────────────────────────────────────────────────┤
│  Permiso.java (Enum)            [48 constantes]            │
│  MatrizPermisos.java (Static)   [Asignación por rol]       │
└─────────────────────────────────────────────────────────────┘
```

---

