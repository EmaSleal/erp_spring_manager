# 📐 Arquitectura del Proyecto - WhatsApp Orders Manager

> **Fecha de creación:** 26 de octubre de 2025  
> **Patrón arquitectónico:** MVC (Model-View-Controller) con Spring Boot

---

## 📋 Índice

1. [Estructura General](#estructura-general)
2. [Capa de Dominio (Models)](#capa-de-dominio-models)
3. [Capa de DTOs (Data Transfer Objects)](#capa-de-dtos-data-transfer-objects)
4. [Capa de Utilidades (Utils)](#capa-de-utilidades-utils)
5. [Capa de Datos (Repositories)](#capa-de-datos-repositories)
6. [Capa de Lógica de Negocio (Services)](#capa-de-lógica-de-negocio-services)
7. [Capa de Presentación (Controllers)](#capa-de-presentación-controllers)
8. [Configuración (Config)](#configuración-config)
9. [Recursos Estáticos y Plantillas](#recursos-estáticos-y-plantillas)
10. [Tareas Programadas (Schedulers)](#tareas-programadas-schedulers)
11. [Enumeraciones](#enumeraciones)
12. [Diagramas de Arquitectura](#diagramas-de-arquitectura)

---

## 🏗️ Estructura General

```
src/
├── main/
│   ├── java/
│   │   └── api/
│   │       └── astro/
│   │           └── whats_orders_manager/
│   │               ├── WhatsOrdersManagerApplication.java  # Clase principal
│   │               ├── config/                             # Configuraciones
│   │               ├── controller/                         # Controladores auxiliares
│   │               ├── controllers/                        # Controladores principales
│   │               ├── dto/                               # 🆕 Data Transfer Objects
│   │               ├── enums/                             # Enumeraciones
│   │               ├── models/                            # Modelos de dominio
│   │               ├── repositories/                      # Acceso a datos
│   │               ├── schedulers/                        # Tareas programadas
│   │               ├── services/                          # Lógica de negocio
│   │               └── util/                              # 🆕 Utilidades reutilizables
│   └── resources/
│       ├── application.yml                                # Configuración de Spring
│       ├── static/                                        # Recursos estáticos
│       │   ├── css/                                      # Hojas de estilo
│       │   ├── js/                                       # JavaScript
│       │   └── images/                                   # Imágenes
│       └── templates/                                     # Plantillas Thymeleaf
│           ├── auth/                                     # Autenticación
│           ├── clientes/                                 # Gestión de clientes
│           ├── configuracion/                            # Configuración
│           ├── dashboard/                                # Panel principal
│           ├── email/                                    # Plantillas de email
│           ├── error/                                    # Páginas de error
│           ├── facturas/                                 # Gestión de facturas
│           ├── perfil/                                   # Perfil de usuario
│           ├── productos/                                # Gestión de productos
│           ├── reportes/                                 # Reportes y gráficos
│           ├── usuarios/                                 # Gestión de usuarios
│           ├── components/                               # Componentes reutilizables
│           ├── index.html                                # Página de inicio
│           └── layout.html                               # Layout base
└── test/
    └── java/
        └── api/                                          # Tests unitarios
```

---

## 📦 Capa de Dominio (Models)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/models/`

### Entidades Principales

| Clase | Descripción | Anotaciones JPA |
|-------|-------------|-----------------|
| `Cliente.java` | Representa un cliente del sistema | `@Entity`, `@Table`, `@EntityListeners` |
| `Factura.java` | Representa una factura | `@Entity`, `@Table`, `@EntityListeners` |
| `LineaFactura.java` | Línea de detalle de factura | `@Entity`, `@Table`, `@EntityListeners` |
| `Producto.java` | Representa un producto | `@Entity`, `@Table`, `@EntityListeners` |
| `Usuario.java` | Usuario del sistema | `@Entity`, `@Table`, `@EntityListeners` |
| `Empresa.java` | Datos de la empresa | `@Entity`, `@Table`, `@EntityListeners` |
| `Presentacion.java` | Presentación de productos | `@Entity`, `@Table` |
| `ConfiguracionFacturacion.java` | Configuración de facturación | `@Entity`, `@Table` |
| `ConfiguracionNotificaciones.java` | Configuración de notificaciones | `@Entity`, `@Table`, `@EntityListeners` |
| `WebhookLog.java` | Registro de webhooks de WhatsApp | `@Entity`, `@Table` |

### DTOs y Records

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/models/dto/`

| Clase | Tipo | Descripción |
|-------|------|-------------|
| `ModuloDTO.java` | DTO | Transferencia de datos de módulos |
| `ProductoRecord.java` | Record | Record inmutable de producto |
| `LineaFacturaR.java` | Record | Record inmutable de línea de factura |

### Características de Auditoría

Todas las entidades principales implementan auditoría automática con:
- `createdBy` - Usuario que creó el registro
- `createdDate` - Fecha de creación
- `lastModifiedBy` - Último usuario que modificó
- `lastModifiedDate` - Fecha de última modificación

**Implementación:** `@EntityListeners(AuditingEntityListener.class)`

---

## � Capa de DTOs (Data Transfer Objects)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/dto/`

### DTOs Genéricos

| Clase | Tipo | Descripción | Uso |
|-------|------|-------------|-----|
| `PaginacionDTO.java` | Class | DTO genérico para resultados paginados | 3 controllers |
| `ResponseDTO.java` | Class | Respuestas API REST estandarizadas | 1 controller |
| `EstadisticasUsuariosDTO.java` | Record | Estadísticas de usuarios inmutables | UsuarioController |

### PaginacionDTO<T>

**Propiedades:**
- `List<T> contenido` - Elementos de la página actual
- `int paginaActual` - Número de página (0-indexed)
- `int tamanoPagina` - Elementos por página
- `long totalElementos` - Total de elementos
- `int totalPaginas` - Total de páginas

**Métodos útiles:**
- `tieneSiguiente()` - Verifica si hay página siguiente
- `tieneAnterior()` - Verifica si hay página anterior
- `esPrimera()` - Verifica si es la primera página
- `esUltima()` - Verifica si es la última página

### ResponseDTO

**Propiedades:**
- `boolean success` - Indica éxito/fallo
- `String message` - Mensaje descriptivo
- `Object data` - Datos opcionales

**Factory Methods:**
- `ResponseDTO.success(mensaje)` - Crea respuesta exitosa
- `ResponseDTO.success(mensaje, data)` - Crea respuesta exitosa con datos
- `ResponseDTO.error(mensaje)` - Crea respuesta de error
- `ResponseDTO.error(mensaje, data)` - Crea respuesta de error con datos
- `toMap()` - Convierte a Map para compatibilidad

### EstadisticasUsuariosDTO

**Record con campos:**
- `long total` - Total de usuarios
- `long activos` - Usuarios activos
- `long inactivos` - Usuarios inactivos
- `long administradores` - Total de administradores
- `long vendedores` - Total de vendedores

**Métodos calculados:**
- `porcentajeActivos()` - % de usuarios activos
- `porcentajeAdministradores()` - % de administradores

---

## 🔧 Capa de Utilidades (Utils)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/util/`

### Utilidades Estáticas

| Clase | Descripción | Métodos Principales | Uso |
|-------|-------------|---------------------|-----|
| `ResponseUtil.java` | Respuestas HTTP estandarizadas | `error()`, `success()`, `successData()` | 1 controller |
| `PasswordUtil.java` | Generación de contraseñas seguras | `generarPasswordAleatoria()`, `esPasswordValida()` | 1 controller |
| `PaginacionUtil.java` | Operaciones de paginación | `fromPage()`, `crear()`, `agregarAtributos()` | 3 controllers |

### ResponseUtil

**Métodos estáticos:**
```java
ResponseEntity<Map<String, Object>> error(String mensaje)
ResponseEntity<Map<String, Object>> error(String mensaje, Object data)
ResponseEntity<Map<String, Object>> success(String mensaje)
ResponseEntity<Map<String, Object>> success(String mensaje, Object data)
ResponseEntity<Map<String, Object>> successData(Object data)
```

### PasswordUtil

**Características:**
- Usa `SecureRandom` para generación segura
- Caracteres: A-Z, a-z, 0-9, !@#$%
- Longitud por defecto: 10 caracteres
- Longitud mínima: 6 caracteres

**Métodos estáticos:**
```java
String generarPasswordAleatoria()
String generarPasswordAleatoria(int longitud)
boolean esPasswordValida(String password)
int getLongitudMinima()
```

### PaginacionUtil

**Métodos estáticos:**
```java
PaginacionDTO<T> fromPage(Page<T> page)  // Spring Page → PaginacionDTO
PaginacionDTO<T> crear(List<T> contenido, int paginaActual, int tamanoPagina, long totalElementos)
void agregarAtributos(Model model, PaginacionDTO<T> paginacion, String nombreAtributo)
void agregarAtributosConOrdenamiento(Model model, PaginacionDTO<T> paginacion, String nombreAtributo, String sortBy, String sortDir)
```

---

## �🗄️ Capa de Datos (Repositories)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/repositories/`

### Repositorios JPA

Todos los repositorios extienden `JpaRepository<Entity, ID>` proporcionando operaciones CRUD básicas.

| Repositorio | Entidad | Métodos Personalizados |
|-------------|---------|------------------------|
| `ClienteRepository.java` | Cliente | `findByTelefono()`, `findByNombreContaining()` |
| `FacturaRepository.java` | Factura | `findByClienteId()`, `findByEstado()`, queries de reportes |
| `LineaFacturaRepository.java` | LineaFactura | `findByFacturaId()` |
| `ProductoRepository.java` | Producto | `findByNombreContaining()`, `findByCategoriaId()` |
| `UsuarioRepository.java` | Usuario | `findByNombreUsuario()`, `findByTelefono()` |
| `EmpresaRepository.java` | Empresa | - |
| `PresentacionRepository.java` | Presentacion | - |
| `ConfiguracionFacturacionRepository.java` | ConfiguracionFacturacion | - |
| `ConfiguracionNotificacionesRepository.java` | ConfiguracionNotificaciones | - |
| `WebhookLogRepository.java` | WebhookLog | `findByTelefono()`, `findByEstado()` |

### Queries Nativas

Algunos repositorios incluyen `@Query` con SQL nativo para:
- Reportes complejos
- Agregaciones
- Estadísticas del dashboard

---

## 💼 Capa de Lógica de Negocio (Services)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/services/`

### Patrón de Diseño

Se utiliza el patrón **Interface + Implementación**:
- **Interfaces:** Definen el contrato del servicio
- **Implementaciones:** `src/main/java/api/astro/whats_orders_manager/services/impl/`

### Servicios Principales

| Servicio | Responsabilidad |
|----------|----------------|
| `ClienteService` | Gestión de clientes |
| `FacturaService` | Gestión de facturas y líneas |
| `LineaFacturaService` | Operaciones sobre líneas de factura |
| `ProductoService` | Gestión de productos y presentaciones |
| `UsuarioService` | Gestión de usuarios |
| `EmpresaService` | Configuración de la empresa |
| `ConfiguracionFacturacionService` | Configuración de facturación |
| `ConfiguracionNotificacionesService` | Configuración de notificaciones |
| `EmailService` | Envío de emails (facturas, recordatorios) |
| `ReporteService` | Generación de reportes y estadísticas |
| `ExportService` | Exportación de datos (Excel, PDF) |
| `WebhookLogService` | Gestión de webhooks de WhatsApp |
| `PresentacionService` | Gestión de presentaciones de productos |
| `UserDetailsServiceImpl` | Autenticación de usuarios (Spring Security) |

### Transaccionalidad

Los servicios están anotados con `@Transactional` para garantizar la integridad de las operaciones.

---

## 🎮 Capa de Presentación (Controllers)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/controllers/`

### Controladores Web

| Controlador | Ruta Base | Descripción |
|-------------|-----------|-------------|
| `HomeController.java` | `/` | Página de inicio |
| `AuthController.java` | `/auth` | Autenticación y registro |
| `DashboardController.java` | `/dashboard` | Panel principal |
| `ClienteController.java` | `/clientes` | CRUD de clientes |
| `ProductoController.java` | `/productos` | CRUD de productos |
| `FacturaController.java` | `/facturas` | CRUD de facturas |
| `LineaFacturaController.java` | `/lineas-factura` | API REST para líneas |
| `UsuarioController.java` | `/usuarios` | Gestión de usuarios |
| `ReporteController.java` | `/reportes` | Reportes y gráficos |
| `ConfiguracionController.java` | `/configuracion` | Configuración del sistema |
| `PerfilController.java` | `/perfil` | Perfil de usuario |
| `WhatsAppWebhookController.java` | `/webhook/whatsapp` | Webhook de WhatsApp |
| `WebhookLogController.java` | `/webhook-logs` | Logs de webhooks |

### Controlador de Errores

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/controller/`

| Controlador | Función |
|-------------|---------|
| `CustomErrorController.java` | Manejo centralizado de errores HTTP |

### Tipos de Respuesta

- **Vistas:** Return `String` (nombre de plantilla Thymeleaf)
- **REST API:** `@ResponseBody` + `ResponseEntity<?>` para JSON

---

## ⚙️ Configuración (Config)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/config/`

| Clase de Configuración | Propósito |
|------------------------|-----------|
| `SecurityConfig.java` | Configuración de Spring Security (autenticación, autorización, rutas públicas) |
| `PersistenceConfig.java` | Configuración de JPA y auditoría (`@EnableJpaAuditing`) |
| `AuditorAwareImpl.java` | Proveedor del usuario actual para auditoría |
| `GlobalControllerAdvice.java` | Manejo global de excepciones y atributos comunes |

### Características de Seguridad

- Autenticación basada en formulario
- Codificación BCrypt para contraseñas
- Roles de usuario: `ADMIN`, `USER`
- CSRF habilitado
- Logout personalizado

---

## 🎨 Recursos Estáticos y Plantillas

### Recursos Estáticos

**Ubicación:** `src/main/resources/static/`

#### CSS
**Ubicación:** `src/main/resources/static/css/`

| Archivo | Propósito |
|---------|-----------|
| `styles.css` | Estilos globales base |
| `common.css` | Componentes comunes |
| `navbar.css` | Barra de navegación |
| `sidebar.css` | Menú lateral |
| `dashboard.css` | Dashboard principal |
| `forms.css` | Formularios |
| `tables.css` | Tablas de datos |
| `facturas.css` | Vista de facturas |
| `configuracion.css` | Configuración |
| `usuarios.css` | Gestión de usuarios |
| `reportes.css` | Reportes y gráficos |
| `responsive.css` | Diseño responsive |

#### JavaScript
**Ubicación:** `src/main/resources/static/js/`

| Archivo | Propósito |
|---------|-----------|
| `scripts.js` | Scripts globales |
| `common.js` | Funciones comunes |
| `navbar.js` | Lógica de navegación |
| `sidebar.js` | Lógica del menú lateral |
| `dashboard.js` | Dashboard interactivo |
| `clientes.js` | Gestión de clientes |
| `productos.js` | Gestión de productos |
| `facturas.js` | Gestión de facturas |
| `editar-factura.js` | Editor de facturas |
| `usuarios.js` | Gestión de usuarios |
| `reportes.js` | Gráficos y reportes |
| `configuracion.js` | Configuración |

#### Imágenes
**Ubicación:** `src/main/resources/static/images/avatars/`

Contiene avatares predeterminados para usuarios.

### Plantillas Thymeleaf

**Ubicación:** `src/main/resources/static/templates/`

#### Estructura de Módulos

| Carpeta | Plantillas | Descripción |
|---------|-----------|-------------|
| `auth/` | `login.html`, `register.html` | Autenticación |
| `dashboard/` | `dashboard.html` | Panel principal con KPIs |
| `clientes/` | `listar.html`, `nuevo.html`, `editar.html`, `detalle.html` | CRUD de clientes |
| `productos/` | `listar.html`, `nuevo.html`, `editar.html` | CRUD de productos |
| `facturas/` | `listar.html`, `nueva.html`, `editar.html`, `ver.html` | CRUD de facturas |
| `usuarios/` | `listar.html`, `nuevo.html`, `editar.html` | CRUD de usuarios |
| `reportes/` | `reportes.html` | Reportes y gráficos |
| `configuracion/` | `general.html`, `facturacion.html`, `notificaciones.html` | Configuración |
| `perfil/` | `perfil.html` | Perfil de usuario |
| `email/` | `factura-email.html`, `recordatorio-pago.html` | Plantillas de email |
| `error/` | `error.html`, `403.html`, `404.html`, `500.html` | Páginas de error |
| `components/` | Fragmentos reutilizables | Componentes comunes |

#### Layouts Base

| Archivo | Propósito |
|---------|-----------|
| `layout.html` | Layout principal con navbar y sidebar |
| `index.html` | Página de bienvenida |

---

## ⏰ Tareas Programadas (Schedulers)

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/schedulers/`

| Scheduler | Descripción | Frecuencia |
|-----------|-------------|------------|
| `RecordatorioPagoScheduler.java` | Envío automático de recordatorios de pago por email | Configurable (cron) |

**Configuración:** Habilitado con `@EnableScheduling` en la clase principal.

---

## 🔢 Enumeraciones

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/enums/`

| Enum | Valores | Uso |
|------|---------|-----|
| `InvoiceType.java` | `PEDIDO`, `FACTURA` | Tipo de documento |

---

## 📊 Diagramas de Arquitectura

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

## 🔧 Tecnologías Utilizadas

### Backend
- **Spring Boot 3.x** - Framework principal
- **Spring Data JPA** - Persistencia
- **Spring Security** - Autenticación y autorización
- **Thymeleaf** - Motor de plantillas
- **PostgreSQL** - Base de datos

### Frontend
- **Bootstrap 5** - Framework CSS
- **Chart.js** - Gráficos
- **SweetAlert2** - Alertas modales
- **Font Awesome** - Iconos
- **jQuery** - Manipulación DOM

### Herramientas
- **Maven** - Gestión de dependencias
- **Lombok** - Reducción de código boilerplate
- **SLF4J + Logback** - Logging
- **JavaMail** - Envío de emails

---

## 📝 Notas sobre la Arquitectura

### Buenas Prácticas Implementadas

1. **Separación de Responsabilidades:** Cada capa tiene una responsabilidad clara
2. **Inyección de Dependencias:** Uso de constructor injection
3. **Programación por Contratos:** Uso de interfaces en servicios
4. **Auditoría Automática:** Rastreo de creación y modificación
5. **Manejo de Errores:** GlobalControllerAdvice para manejo centralizado
6. **Validación:** Bean Validation en entidades
7. **Seguridad:** Spring Security con roles y permisos
8. **Transaccionalidad:** Uso de @Transactional en servicios
9. **Logging:** Logging estructurado con SLF4J
10. **Responsive Design:** Diseño adaptable a dispositivos móviles

### Convenciones de Código

- **Nombres de Clases:** PascalCase
- **Nombres de Métodos:** camelCase
- **Nombres de Paquetes:** lowercase
- **Constantes:** UPPER_SNAKE_CASE
- **Rutas REST:** kebab-case
- **Archivos Estáticos:** kebab-case

---

## 🚀 Puntos de Entrada

- **Aplicación:** `WhatsOrdersManagerApplication.java` (método `main`)
- **Página Inicial:** `/` → `HomeController`
- **Login:** `/auth/login` → `AuthController`
- **Dashboard:** `/dashboard` → `DashboardController`
- **Webhook WhatsApp:** `/webhook/whatsapp` → `WhatsAppWebhookController`

---

**Última actualización:** 26 de octubre de 2025
