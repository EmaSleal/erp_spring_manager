# 📁 Estructura del Proyecto - WhatsApp Orders Manager

**Fecha de actualización:** 30 de Noviembre de 2025  
**Versión:** 1.1  

---

## 🏗️ Estructura General

```
whats_orders_manager/
├── docs/                              # Documentación del proyecto
│   ├── base de datos/                 # Scripts SQL y migraciones
│   ├── diseno/                        # Mockups y diseños
│   ├── planificacion/                 # Planes y decisiones técnicas
│   ├── referencias/                   # Referencias y roadmaps
│   └── sprints/                       # Documentación de sprints
│       ├── SPRINT_1/
│       ├── SPRINT_2/
│       ├── SPRINT_3/
│       │   └── FASE_1_WHATSAPP_CONVERSACIONES.md
│       └── fixes/
│
├── src/main/
│   ├── java/api/astro/whats_orders_manager/
│   │   ├── config/                    # Configuraciones de Spring
│   │   ├── controllers/               # Controladores REST y MVC
│   │   ├── dto/                       # ⚠️ DEPRECATED - usar models/dto
│   │   ├── models/                    # 📦 MODELOS ORGANIZADOS
│   │   │   ├── dto/                   # Data Transfer Objects
│   │   │   ├── enums/                 # Enumeraciones
│   │   │   ├── class/                 # Clases auxiliares
│   │   │   ├── records/               # Java Records
│   │   │   └── *.java                 # Entidades JPA
│   │   ├── repositories/              # Repositorios Spring Data JPA
│   │   ├── services/                  # Lógica de negocio
│   │   └── WhatsOrdersManagerApplication.java
│   │
│   └── resources/
│       ├── static/                    # Recursos estáticos
│       │   ├── css/                   # Hojas de estilo
│       │   ├── js/                    # Scripts JavaScript
│       │   └── img/                   # Imágenes
│       ├── templates/                 # Plantillas Thymeleaf
│       │   ├── components/            # Componentes reutilizables
│       │   ├── whatsapp/              # Vistas WhatsApp
│       │   └── *.html                 # Vistas generales
│       └── application.yml            # Configuración principal
│
├── target/                            # Archivos compilados (generado)
├── pom.xml                            # Dependencias Maven
├── start.ps1                          # Script de inicio
└── .env.local                         # Variables de entorno
```

---

## 📦 Estructura Detallada de `/models`

### 🎯 Nueva Organización (Sprint 3 - Fase 1.5)

```
models/
├── dto/                               # Data Transfer Objects
│   ├── EstadisticasUsuariosDTO.java
│   ├── ModuloDTO.java
│   ├── PaginacionDTO.java
│   ├── PlantillaWhatsAppDTO.java
│   ├── ResponseDTO.java
│   ├── WebhookValidationDTO.java
│   └── WhatsAppMensajeDTO.java
│
├── enums/                             # Enumeraciones standalone
│   └── (vacío - enums están como inner classes)
│
├── class/                             # Clases auxiliares
│   └── (vacío - reservado para futuras clases)
│
├── records/                           # Java Records
│   ├── LineaFacturaR.java             # Record para líneas de factura
│   └── ProductoRecord.java            # Record para productos
│
└── Entidades JPA (raíz de models/)
    ├── Cliente.java
    ├── ConfiguracionFacturacion.java
    ├── ConfiguracionNotificaciones.java
    ├── Empresa.java
    ├── Factura.java
    ├── LineaFactura.java
    ├── MensajeWhatsApp.java           # Contiene TipoMensaje y EstadoMensaje como inner enums
    ├── PlantillaWhatsApp.java         # Contiene CategoriaPlantilla y EstadoMeta como inner enums
    ├── Presentacion.java
    ├── Producto.java
    ├── Usuario.java
    └── WebhookLog.java
```

---

## 📄 Inventario de Archivos por Categoría

### 🗂️ DTOs (Data Transfer Objects)

| Archivo | Ubicación | Propósito |
|---------|-----------|-----------|
| `EstadisticasUsuariosDTO.java` | `models/dto/` | Estadísticas de usuarios |
| `ModuloDTO.java` | `models/dto/` | Información de módulos del sistema |
| `PaginacionDTO.java` | `models/dto/` | Datos de paginación |
| `PlantillaWhatsAppDTO.java` | `models/dto/` | DTO para plantillas WhatsApp |
| `ResponseDTO.java` | `models/dto/` | Respuesta genérica de API |
| `WebhookValidationDTO.java` | `models/dto/` | Validación de webhooks |
| `WhatsAppMensajeDTO.java` | `models/dto/` | DTO para mensajes WhatsApp |

**Total:** 7 DTOs

---

### 🎲 Enums (Enumeraciones)

**Nota:** Los enums están definidos como inner classes dentro de las entidades que los utilizan.

| Enum | Ubicación | Valores |
|------|-----------|---------|
| `TipoMensaje` | `MensajeWhatsApp.java` | ENVIADO, RECIBIDO |
| `EstadoMensaje` | `MensajeWhatsApp.java` | PENDIENTE, ENVIADO, ENTREGADO, LEIDO, FALLIDO |
| `CategoriaPlantilla` | `PlantillaWhatsApp.java` | MARKETING, UTILITY, AUTHENTICATION |
| `EstadoMeta` | `PlantillaWhatsApp.java` | PENDIENTE, APROBADO, RECHAZADO |

**Total:** 4 Enums (como inner classes)

**Recomendación futura:** Extraer enums a archivos separados en `models/enums/` si se reutilizan en múltiples contextos.

---

### 📝 Records (Java 17+)

| Record | Ubicación | Campos | Propósito |
|--------|-----------|--------|-----------|
| `LineaFacturaR` | `models/records/` | id, idProducto, cantidad, precioUnitario, subtotal, nombreProducto, codigoProducto | Representación inmutable de línea de factura |
| `ProductoRecord` | `models/records/` | id, codigo, nombre, descripcion, precio, stock, categoria, activo | Representación inmutable de producto |

**Total:** 2 Records

**Ventajas de Records:**
- ✅ Inmutabilidad garantizada
- ✅ Menos código boilerplate
- ✅ Equals, hashCode y toString automáticos
- ✅ Compatibilidad con pattern matching (Java 21+)

---

### 🗄️ Entidades JPA

| Entidad | Tabla | Relaciones | Descripción |
|---------|-------|------------|-------------|
| `Cliente` | `clientes` | - | Clientes del sistema |
| `ConfiguracionFacturacion` | `configuracion_facturacion` | - | Configuración de facturación |
| `ConfiguracionNotificaciones` | `configuracion_notificaciones` | - | Configuración de notificaciones |
| `Empresa` | `empresa` | - | Datos de la empresa |
| `Factura` | `facturas` | @OneToMany → LineaFactura<br>@ManyToOne → Cliente | Facturas emitidas |
| `LineaFactura` | `lineas_factura` | @ManyToOne → Factura<br>@ManyToOne → Producto | Líneas de factura |
| `MensajeWhatsApp` | `mensaje_whatsapp` | - | Mensajes enviados/recibidos |
| `PlantillaWhatsApp` | `plantilla_whatsapp` | - | Plantillas aprobadas |
| `Presentacion` | `presentaciones` | @ManyToOne → Producto | Presentaciones de productos |
| `Producto` | `productos` | @OneToMany → Presentacion | Productos del catálogo |
| `Usuario` | `usuarios` | - | Usuarios del sistema |
| `WebhookLog` | `webhook_log` | - | Logs de webhooks |

**Total:** 12 Entidades JPA

---

## 🎨 Estructura de `/templates`

```
templates/
├── components/                        # Componentes reutilizables
│   ├── navbar.html                   # Barra de navegación
│   └── sidebar.html                  # Menú lateral
│
├── whatsapp/                         # Módulo WhatsApp
│   ├── conversacion-detalle.html     # Vista de chat individual
│   ├── mensajes.html                 # Lista de conversaciones
│   ├── mensajes-old.html             # Backup tabla antigua
│   └── plantillas.html               # Gestión de plantillas
│
├── usuarios/                         # Módulo Usuarios
│   └── usuarios.html
│
├── clientes/                         # Módulo Clientes
│   └── clientes.html
│
├── facturas/                         # Módulo Facturas
│   ├── facturas.html
│   └── factura-detalle.html
│
├── productos/                        # Módulo Productos
│   └── productos.html
│
├── auth/                             # Autenticación
│   ├── login.html
│   └── registro.html
│
├── errors/                           # Páginas de error
│   ├── 403.html
│   ├── 404.html
│   └── 500.html
│
├── dashboard.html                    # Dashboard principal
├── layout.html                       # Layout base
└── index.html                        # Landing page
```

---

## 📚 Estructura de `/static`

```
static/
├── css/
│   ├── auth.css                      # Estilos de autenticación
│   ├── clientes.css                  # Estilos de clientes
│   ├── dashboard.css                 # Estilos del dashboard
│   ├── facturas.css                  # Estilos de facturas
│   ├── layout.css                    # Estilos del layout
│   ├── productos.css                 # Estilos de productos
│   ├── responsive.css                # Media queries responsive
│   ├── usuarios.css                  # Estilos de usuarios
│   └── whatsapp.css                  # ⭐ Estilos WhatsApp (NUEVO)
│
├── js/
│   ├── clientes.js
│   ├── dashboard.js
│   ├── facturas.js
│   ├── productos.js
│   ├── usuarios.js
│   ├── whatsapp-conversaciones.js    # ⭐ Interacciones conversaciones (NUEVO)
│   └── whatsapp-plantillas.js        # CRUD plantillas
│
└── img/
    └── logo.png
```

---

## 🔧 Archivos de Configuración

### `application.yml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
  jpa:
    hibernate:
      ddl-auto: update                 # Auto-migraciones
  thymeleaf:
    cache: false                       # Desarrollo: sin cache
  security:
    enabled: true
```

### `.env.local`
```properties
DB_HOST=192.168.100.8
DB_PORT=3306
DB_NAME=facturas_monrachem
DB_USER=admin_facturas
DB_PASSWORD=********
WHATSAPP_TOKEN=********
WHATSAPP_PHONE_ID=********
```

### `pom.xml` - Dependencias Principales
```xml
- Spring Boot 3.5.0
- Spring Data JPA
- Spring Security
- MySQL Connector 8.0
- Thymeleaf + Extras Security
- Lombok
- Validation
- DevTools
```

---

## 📊 Métricas del Proyecto

### Código Java
- **Entidades:** 12
- **DTOs:** 7
- **Records:** 2
- **Enums:** 4 (inner classes)
- **Repositorios:** 12
- **Servicios:** 10
- **Controladores:** 8

### Frontend
- **Templates Thymeleaf:** 25+
- **Archivos CSS:** 9
- **Archivos JavaScript:** 7
- **Componentes reutilizables:** 2

### Líneas de Código (aprox.)
- **Backend Java:** ~8,500 líneas
- **Frontend (HTML/CSS/JS):** ~4,200 líneas
- **Documentación:** ~2,800 líneas
- **Total:** ~15,500 líneas

---

## 🗺️ Roadmap de Organización

### ✅ Completado (Sprint 3 - Fase 1.5)
- [x] Crear carpeta `models/dto/`
- [x] Crear carpeta `models/enums/`
- [x] Crear carpeta `models/class/`
- [x] Crear carpeta `models/records/`
- [x] Mover DTOs a `models/dto/`
- [x] Mover Records a `models/records/`
- [x] Documentar estructura completa

### 🔜 Próximos Pasos

#### Fase 2: Extraer Enums
- [ ] Extraer `TipoMensaje` a `models/enums/TipoMensaje.java`
- [ ] Extraer `EstadoMensaje` a `models/enums/EstadoMensaje.java`
- [ ] Extraer `CategoriaPlantilla` a `models/enums/CategoriaPlantilla.java`
- [ ] Extraer `EstadoMeta` a `models/enums/EstadoMeta.java`
- [ ] Actualizar imports en todas las clases

#### Fase 3: Consolidar DTOs
- [ ] Eliminar carpeta `dto/` raíz
- [ ] Actualizar todos los imports
- [ ] Verificar compilación

#### Fase 4: Clases Auxiliares
- [ ] Identificar clases helper/utility
- [ ] Mover a `models/class/`
- [ ] Documentar responsabilidades

---

## 📝 Convenciones de Nomenclatura

### Entidades JPA
- **Patrón:** `NombreSingular.java`
- **Ejemplo:** `Cliente.java`, `Factura.java`
- **Tabla:** `nombre_plural` (snake_case)

### DTOs
- **Patrón:** `NombreDTO.java`
- **Ejemplo:** `WhatsAppMensajeDTO.java`
- **Ubicación:** `models/dto/`

### Records
- **Patrón:** `NombreRecord.java` o `NombreR.java`
- **Ejemplo:** `ProductoRecord.java`, `LineaFacturaR.java`
- **Ubicación:** `models/records/`

### Enums
- **Patrón:** `NombrePascalCase.java`
- **Ejemplo:** `TipoMensaje.java`, `EstadoMensaje.java`
- **Ubicación:** `models/enums/` (futuro)
- **Actual:** Inner classes en entidades

### Servicios
- **Patrón:** `NombreService.java`
- **Ejemplo:** `MensajeWhatsAppService.java`

### Repositorios
- **Patrón:** `NombreRepository.java`
- **Ejemplo:** `MensajeWhatsAppRepository.java`

### Controladores
- **Patrón:** `NombreController.java` o `NombreViewController.java`
- **Ejemplo:** `WhatsAppViewController.java`

---

## 🎯 Buenas Prácticas Aplicadas

### Backend
1. ✅ Separación de capas (Controller → Service → Repository → Entity)
2. ✅ DTOs para transferencia de datos
3. ✅ Records para datos inmutables
4. ✅ Enums para valores constantes
5. ✅ Lombok para reducir boilerplate
6. ✅ Validation con anotaciones
7. ✅ Logging con SLF4J

### Frontend
1. ✅ Templates reutilizables (layout, components)
2. ✅ CSS modular por funcionalidad
3. ✅ JavaScript separado por módulo
4. ✅ Event delegation para performance
5. ✅ Data attributes para binding
6. ✅ Responsive design (mobile-first)

### Base de Datos
1. ✅ Nombres en snake_case
2. ✅ Claves foráneas con índices
3. ✅ Auditoría con timestamps
4. ✅ Migraciones documentadas

---

## 🔍 Cómo Navegar el Proyecto

### Buscar Entidades
```
📁 models/*.java
```

### Buscar DTOs
```
📁 models/dto/*DTO.java
```

### Buscar Records
```
📁 models/records/*Record.java
```

### Buscar Lógica de Negocio
```
📁 services/*Service.java
```

### Buscar Queries SQL
```
📁 repositories/*Repository.java
```

### Buscar Vistas
```
📁 templates/**/*.html
```

### Buscar Estilos
```
📁 static/css/*.css
```

### Buscar Interacciones JS
```
📁 static/js/*.js
```

---

## 📚 Documentación Relacionada

- [FASE_1_WHATSAPP_CONVERSACIONES.md](sprints/SPRINT_3/FASE_1_WHATSAPP_CONVERSACIONES.md) - Implementación conversaciones
- [ESTADO_PROYECTO.md](ESTADO_PROYECTO.md) - Estado actual del proyecto
- [DECISIONES_APLICADAS.md](DECISIONES_APLICADAS.md) - Decisiones técnicas
- [PROXIMOS_PASOS.md](PROXIMOS_PASOS.md) - Roadmap de desarrollo

---

**Última actualización:** 30 de Noviembre de 2025  
**Mantenido por:** Equipo de Desarrollo  
**Versión:** 1.1
