## 🏗️ Arquitectura Implementada

### Backend (Java Spring Boot)

```
api.astro.whats_orders_manager/
├── controllers/
│   ├── PerfilController.java         ✅ (400+ líneas, 6 endpoints)
│   ├── UsuarioController.java        ✅ SPRINT 2 (CRUD completo)
│   ├── ConfiguracionController.java  ✅ SPRINT 2 (notificaciones)
│   ├── ReporteController.java        ✅ SPRINT 2 (PDF, Excel, CSV)
│   ├── DashboardController.java      ✅ (233 líneas)
│   ├── AuthController.java           ✅
│   ├── ClienteController.java        ✅
│   ├── ProductoController.java       ✅
│   └── FacturaController.java        ✅
├── services/
│   ├── UsuarioService.java           ✅ (+ CRUD Sprint 2)
│   ├── UsuarioServiceImpl.java       ✅ (+ caché Sprint 2)
│   ├── EmailService.java             ✅ SPRINT 2 (JavaMailSender)
│   ├── EmailServiceImpl.java         ✅ SPRINT 2 (envío async)
│   ├── ReporteService.java           ✅ SPRINT 2 (generación reportes)
│   ├── ReporteServiceImpl.java       ✅ SPRINT 2 (caché)
│   ├── ClienteService.java           ✅ (+ caché Sprint 2)
│   ├── ProductoService.java          ✅
│   └── FacturaService.java           ✅
├── repositories/
│   ├── UsuarioRepository.java        ✅ (+ findByEmail)
│   ├── ConfigNotificacionRepository  ✅ SPRINT 2
│   ├── ClienteRepository.java        ✅
│   ├── ProductoRepository.java       ✅
│   └── FacturaRepository.java        ✅
├── models/
│   ├── Usuario.java                  ✅ (+ 4 campos Sprint 1, + rol Sprint 2)
│   ├── ConfiguracionNotificacion.java ✅ SPRINT 2
│   ├── Cliente.java                  ✅
│   ├── Producto.java                 ✅
│   └── Factura.java                  ✅ (+ fechaPago Sprint 2)
├── dto/
│   ├── ModuloDTO.java                ✅
│   └── ReporteDTO.java               ✅ SPRINT 2
└── config/
    ├── SecurityConfig.java           ✅ (+ permisos granulares Sprint 2)
    ├── EmailConfig.java              ✅ SPRINT 2 (SMTP Gmail)
    └── CacheConfig.java              ✅ SPRINT 2 (Spring Cache)
```

### Frontend (Thymeleaf + HTML/CSS/JS)

```
templates/
├── auth/
│   ├── login.html                    ✅
│   └── register.html                 ✅
├── components/
│   ├── navbar.html                   ✅ (+ avatar usuario Sprint 2)
│   └── sidebar.html                  ✅
├── dashboard/
│   └── dashboard.html                ✅ (con fix de seguridad Thymeleaf)
├── perfil/
│   ├── ver.html                      ✅ (350+ líneas)
│   └── editar.html                   ✅ (700+ líneas)
├── usuarios/                         ✅ SPRINT 2 (Gestión completa)
│   ├── usuarios.html                 ✅ (lista con paginación)
│   └── form.html                     ✅ (crear/editar)
├── configuracion/                    ✅ SPRINT 2 (Sistema)
│   └── notificaciones.html           ✅ (config email)
├── reportes/                         ✅ SPRINT 2 (Generación)
│   ├── reportes.html                 ✅ (selección de reporte)
│   ├── facturas.html                 ✅ (filtros y descarga)
│   ├── clientes.html                 ✅ (filtros y descarga)
│   └── productos.html                ✅ (filtros y descarga)
├── clientes/
│   ├── clientes.html                 ✅ (+ paginación Sprint 2)
│   └── form.html                     ✅
├── productos/
│   ├── productos.html                ✅ (+ paginación Sprint 2)
│   └── form.html                     ✅
└── facturas/
    ├── facturas.html                 ✅ (+ paginación Sprint 2)
    ├── form.html                     ✅
    └── add-form.html                 ✅

static/
├── css/
│   ├── common.css                    ✅
│   ├── navbar.css                    ✅
│   ├── sidebar.css                   ✅
│   ├── dashboard.css                 ✅ (300+ líneas)
│   ├── usuarios.css                  ✅ SPRINT 2
│   ├── reportes.css                  ✅ SPRINT 2
│   ├── forms.css                     ✅
│   ├── tables.css                    ✅ (+ paginación Sprint 2)
│   └── facturas.css                  ✅
├── js/
│   ├── navbar.js                     ✅
│   ├── sidebar.js                    ✅
│   ├── dashboard.js                  ✅ (+ handleModuleClick)
│   ├── usuarios.js                   ✅ SPRINT 2 (CRUD + validaciones)
│   ├── reportes.js                   ✅ SPRINT 2 (generación)
│   ├── paginacion.js                 ✅ SPRINT 2 (componente reutilizable)
│   ├── common.js                     ✅
│   ├── clientes.js                   ✅
│   ├── productos.js                  ✅
│   └── facturas.js                   ✅
└── images/
    └── avatars/                      ✅ (directorio creado)
```

### Base de Datos (MySQL)

```sql
-- Tablas Principales

-- Usuario (Sprint 1 + Sprint 2)
usuario
├── id_usuario         INT PRIMARY KEY AUTO_INCREMENT
├── nombre             VARCHAR(100)
├── telefono           VARCHAR(20) UNIQUE
├── email              VARCHAR(100) UNIQUE        ✅ Sprint 1
├── password           VARCHAR(255)
├── rol                VARCHAR(50)                ✅ Sprint 2 (expandido)
├── avatar             VARCHAR(255)               ✅ Sprint 1
├── activo             BOOLEAN DEFAULT TRUE       ✅ Sprint 1
├── ultimo_acceso      TIMESTAMP NULL             ✅ Sprint 1
├── createDate         TIMESTAMP
└── updateDate         TIMESTAMP

-- Usuario_Rol (Sprint 2 - Roles múltiples)
usuario_rol
├── id                 INT PRIMARY KEY AUTO_INCREMENT
├── usuario_id         INT FOREIGN KEY → usuario
├── rol                VARCHAR(50) (ADMIN, AGENTE, CONTADOR, VIEWER)
├── asignado_por       INT FOREIGN KEY → usuario
├── fecha_asignacion   TIMESTAMP
└── activo             BOOLEAN DEFAULT TRUE

-- Configuración de Notificaciones (Sprint 2)
configuracion_notificacion
├── id                 INT PRIMARY KEY AUTO_INCREMENT
├── tipo_notificacion  VARCHAR(50) (FACTURA_VENCIDA, etc.)
├── dias_antelacion    INT
├── hora_envio         TIME
├── activo             BOOLEAN DEFAULT TRUE
├── created_at         TIMESTAMP
└── updated_at         TIMESTAMP

-- Factura (Sprint 1 + Sprint 2)
factura
├── id_factura         INT PRIMARY KEY AUTO_INCREMENT
├── numero_factura     VARCHAR(50) UNIQUE
├── id_cliente         INT FOREIGN KEY → cliente
├── fecha_emision      DATE
├── fecha_vencimiento  DATE
├── fecha_pago         DATE                       ✅ Sprint 2 (NUEVO)
├── subtotal           DECIMAL(10,2)
├── total              DECIMAL(10,2)
├── estado_pago        VARCHAR(20)
├── entregado          BOOLEAN
├── notas              TEXT
├── createDate         TIMESTAMP
└── updateDate         TIMESTAMP

-- Índices de Optimización (Sprint 2)
✅ idx_usuario_email           ON usuario(email)
✅ idx_usuario_telefono        ON usuario(telefono)
✅ idx_usuario_activo          ON usuario(activo)
✅ idx_cliente_telefono        ON cliente(telefono)
✅ idx_cliente_activo          ON cliente(activo)
✅ idx_factura_cliente         ON factura(id_cliente)
✅ idx_factura_estado          ON factura(estado_pago)
✅ idx_factura_fecha_venc      ON factura(fecha_vencimiento)
✅ idx_factura_fecha_emision   ON factura(fecha_emision)
✅ idx_factura_numero          ON factura(numero_factura)

-- Stored Procedures (Sprint 2 - 24 SPs)
✅ SP_GetUsuariosConPaginacion
✅ SP_GetUsuarioById
✅ SP_GetUsuarioByEmail
✅ SP_CreateUsuario
✅ SP_UpdateUsuario
✅ SP_DeactivateUsuario
✅ SP_ReactivateUsuario
✅ SP_GetClientesConPaginacion
✅ SP_GetClienteById
✅ SP_GetClienteActivos
✅ SP_CreateCliente
✅ SP_UpdateCliente
✅ SP_GetProductosConPaginacion
✅ SP_GetProductoById
✅ SP_GetProductosActivos
✅ SP_GetProductosBajoStock
✅ SP_CreateProducto
✅ SP_UpdateProducto
✅ SP_GetFacturasConPaginacion
✅ SP_GetFacturaById
✅ SP_GetFacturasPendientes
✅ SP_GetFacturasVencidas
✅ SP_CreateFactura
✅ SP_UpdateFactura
```

---

