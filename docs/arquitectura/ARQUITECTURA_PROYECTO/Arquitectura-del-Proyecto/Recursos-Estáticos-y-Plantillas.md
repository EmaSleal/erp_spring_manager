##  Recursos Estáticos y Plantillas

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

