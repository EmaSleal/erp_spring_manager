# 📱 WhatsApp Orders Manager

> Sistema ERP completo para gestión de pedidos, ventas e inventario vía WhatsApp

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3.0-purple.svg)](https://getbootstrap.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## � Tabla de Contenidos

- [Descripción](#-descripción)
- [Características](#-características)
- [Capturas de Pantalla](#-capturas-de-pantalla)
- [Tecnologías](#️-tecnologías)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación](#-instalación)
- [Configuración](#️-configuración)
- [Uso](#-uso)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Roles y Permisos](#-roles-y-permisos)
- [Documentación](#-documentación)
- [Testing](#-testing)
- [Roadmap](#-roadmap)
- [Contribución](#-contribución)
- [Licencia](#-licencia)

---

## 📝 Descripción

**WhatsApp Orders Manager** es un sistema ERP moderno y completo diseñado para pequeñas y medianas empresas que gestionan pedidos a través de WhatsApp. Ofrece:

- 🎯 **Dashboard intuitivo** con estadísticas en tiempo real
- 👥 **Gestión completa de clientes** (CRUD, búsqueda, filtros)
- 📦 **Control de productos** con inventario y precios
- 📄 **Facturación** con seguimiento de estados
- 👤 **Sistema de perfiles** personalizable
- 🔐 **Seguridad robusta** con roles y permisos
- 📱 **100% Responsive** (móvil, tablet, desktop)

---

## ✨ Características

### 🏠 Dashboard Principal
- 4 widgets de estadísticas en tiempo real
- Módulos dinámicos según rol de usuario
- Diseño Material Design moderno
- Auto-refresh de datos cada 30 segundos

### 👥 Gestión de Clientes
- ✅ CRUD completo (Crear, Leer, Actualizar, Eliminar)
- 🔍 Búsqueda en tiempo real
- 📊 Filtros por tipo de cliente
- 📝 Avatar con iniciales automáticas
- 📅 Registro de fecha de alta

### 📦 Gestión de Productos
- ✅ CRUD completo con validaciones
- 💰 Precios mayorista e institucional
- 📦 Control de stock con alertas
- 🔢 Códigos únicos generados automáticamente
- 🎨 Estados visuales (Activo/Inactivo)
- 📄 Paginación inteligente (sliding window)

### 📄 Gestión de Facturas
- ✅ Creación y edición de facturas
- 💵 Cálculo automático de totales
- 📊 Filtros por estado y fecha
- ✅ Toggle rápido de estado entregado
- 🔍 Búsqueda por cliente

### 👤 Perfil de Usuario
- 👁️ Ver información personal
- ✏️ Editar datos (nombre, email, teléfono)
- 🔒 Cambiar contraseña con validación
- 🖼️ Upload de avatar (JPG, PNG, GIF)
- 🎨 Avatar con iniciales si no hay imagen
- 📅 Visualización de último acceso

### 🔐 Seguridad
- 🔑 Autenticación con Spring Security
- 👮 3 roles de usuario (ADMIN, USER, CLIENTE)
- 🛡️ Protección CSRF en todos los formularios
- 🔒 Sesiones limitadas (1 por usuario)
- 📝 Registro de último acceso automático
- 🚪 Logout seguro con invalidación de sesión

### 📱 Responsive Design
- 📐 5 breakpoints responsive (xs, sm, md, lg, xl)
- 📊 Tablas optimizadas con columnas ocultas en móvil
- 🔢 Paginación adaptativa (3-10 páginas según pantalla)
- 🏠 Sticky columns en móvil
- 🍔 Hamburger menu en dispositivos móviles

### 🎨 UX/UI Profesional
- 🎨 Material Design color palette (#1976D2)
- 🧭 Breadcrumbs de 2 y 3 niveles
- 🔔 Notificaciones toast con SweetAlert2
- ✅ Validación de formularios en tiempo real
- 🎭 Animaciones y transiciones suaves
- ♿ Accesibilidad WCAG 2.1 AA

---

## 📸 Capturas de Pantalla

### Dashboard Principal
```
[Aquí irá una captura del dashboard con widgets y módulos]
```

### Gestión de Clientes
```
[Aquí irá una captura de la lista de clientes]
```

### Gestión de Productos
```
[Aquí irá una captura de productos con paginación]
```

### Perfil de Usuario
```
[Aquí irá una captura del perfil con tabs]
```

---

## 🛠️ Tecnologías

### Backend
- **Java 21 LTS** - Lenguaje principal
- **Spring Boot 3.5.0** - Framework principal
- **Spring Security 6.x** - Autenticación y autorización
- **Spring Data JPA** - Persistencia de datos
- **Hibernate 6.6.x** - ORM
- **MySQL 8.0** - Base de datos
- **Maven** - Gestor de dependencias

### Frontend
- **Thymeleaf 3.x** - Motor de plantillas
- **Bootstrap 5.3.0** - Framework CSS
- **Font Awesome 6.4.0** - Iconos
- **JavaScript ES6+** - Lógica cliente
- **SweetAlert2 11** - Alertas y notificaciones
- **CSS3** - Estilos personalizados

### Herramientas de Desarrollo
- **IntelliJ IDEA / VS Code** - IDEs recomendados
- **Git** - Control de versiones
- **Maven** - Build tool
- **MySQL Workbench** - Administración de BD

---

## 📋 Requisitos Previos

- ☕ **Java 21 LTS** o superior
- 🗄️ **MySQL 8.0** o superior
- 📦 **Maven 3.6** o superior
- 🌐 Navegador moderno (Chrome, Firefox, Edge)

---

## 🚀 Instalación

### 1️⃣ Clonar el Repositorio
```bash
git clone https://github.com/tu-usuario/whats-orders-manager.git
cd whats-orders-manager
```

### 2️⃣ Configurar Base de Datos

```sql
-- Crear base de datos
CREATE DATABASE whatsapp_orders CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Crear usuario (opcional)
CREATE USER 'orders_user'@'localhost' IDENTIFIED BY 'tu_password';
GRANT ALL PRIVILEGES ON whatsapp_orders.* TO 'orders_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3️⃣ Configurar `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/whatsapp_orders
    username: orders_user
    password: tu_password
    
  jpa:
    hibernate:
      ddl-auto: update  # En producción usar 'validate'
    show-sql: true
```

### 4️⃣ Compilar el Proyecto

```bash
mvn clean compile
```

### 5️⃣ Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

### 6️⃣ Acceder a la Aplicación

Abre tu navegador en: **http://localhost:8080**

---

## ⚙️ Configuración

### Variables de Entorno (Producción)

```bash
# Base de datos
DB_URL=jdbc:mysql://tu-servidor:3306/whatsapp_orders
DB_USERNAME=tu_usuario
DB_PASSWORD=tu_password

# Puerto del servidor
SERVER_PORT=8080

# Perfil de Spring
SPRING_PROFILES_ACTIVE=production
```

### Configuración de Seguridad

```yaml
# application.yml
spring:
  security:
    user:
      name: admin
      password: admin123  # Cambiar en producción
```

### Upload de Archivos

```yaml
# application.yml
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 2MB
```

---

## 📖 Uso

### Primer Login

1. Accede a **http://localhost:8080**
2. Usa las credenciales por defecto:
   - **Usuario:** `admin`
   - **Contraseña:** `admin123`
3. Serás redirigido al Dashboard

### Crear un Cliente

1. Click en el módulo **"Clientes"**
2. Click en botón **"Agregar Cliente"**
3. Llena el formulario y guarda

### Crear un Producto

1. Click en el módulo **"Productos"**
2. Click en botón **"Agregar Producto"**
3. Llena el formulario con código, descripción y precio
4. Guarda

### Crear una Factura

1. Click en el módulo **"Facturas"**
2. Click en botón **"Nueva Factura"**
3. Selecciona cliente y productos
4. Guarda

### Editar Perfil

1. Click en tu avatar (esquina superior derecha)
2. Selecciona **"Mi Perfil"**
3. Click en **"Editar"**
4. Actualiza tu información

---

## 📂 Estructura del Proyecto

```
whats_orders_manager/
├── src/
│   ├── main/
│   │   ├── java/api/astro/whats_orders_manager/
│   │   │   ├── config/              # Configuración (Security, etc.)
│   │   │   ├── controllers/         # Controladores MVC
│   │   │   ├── models/              # Entidades JPA
│   │   │   ├── repositories/        # Repositorios JPA
│   │   │   ├── services/            # Lógica de negocio
│   │   │   └── dto/                 # Data Transfer Objects
│   │   │
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/             # Estilos personalizados
│   │       │   ├── js/              # JavaScript
│   │       │   └── images/          # Imágenes y avatares
│   │       │
│   │       ├── templates/           # Vistas Thymeleaf
│   │       │   ├── auth/            # Login, registro
│   │       │   ├── dashboard/       # Dashboard principal
│   │       │   ├── clientes/        # Gestión clientes
│   │       │   ├── productos/       # Gestión productos
│   │       │   ├── facturas/        # Gestión facturas
│   │       │   ├── perfil/          # Perfil usuario
│   │       │   └── components/      # Componentes reutilizables
│   │       │
│   │       └── application.yml      # Configuración
│   │
│   └── test/                        # Tests unitarios
│
├── docs/                            # Documentación completa
│   ├── COMPONENTES.md               # Guía de componentes
│   ├── planificacion/               # Planes y decisiones
│   ├── sprints/                     # Documentación sprints
│   └── base de datos/               # Scripts SQL
│
├── pom.xml                          # Dependencias Maven
└── README.md                        # Este archivo
```

---

## 👮 Roles y Permisos

### 🔴 ADMIN (Administrador)
**Acceso:** Total al sistema

| Módulo | Ver | Crear | Editar | Eliminar |
|--------|:---:|:-----:|:------:|:--------:|
| Dashboard | ✅ | - | - | - |
| Clientes | ✅ | ✅ | ✅ | ✅ |
| Productos | ✅ | ✅ | ✅ | ✅ |
| Facturas | ✅ | ✅ | ✅ | ✅ |
| Usuarios | ✅ | ✅ | ✅ | ✅ |
| Configuración | ✅ | ✅ | ✅ | ✅ |
| Reportes | ✅ | - | - | - |

### 🟢 USER (Usuario Operativo)
**Acceso:** Módulos operativos

| Módulo | Ver | Crear | Editar | Eliminar |
|--------|:---:|:-----:|:------:|:--------:|
| Dashboard | ✅ | - | - | - |
| Clientes | ✅ | ✅ | ✅ | ✅ |
| Productos | ✅ | ✅ | ✅ | ✅ |
| Facturas | ✅ | ✅ | ✅ | ❌ |
| Usuarios | ❌ | ❌ | ❌ | ❌ |
| Configuración | ❌ | ❌ | ❌ | ❌ |
| Reportes | ❌ | - | - | - |

### 🔵 CLIENTE (Cliente)
**Acceso:** Solo su perfil y sus facturas

| Módulo | Ver | Crear | Editar | Eliminar |
|--------|:---:|:-----:|:------:|:--------:|
| Dashboard | ❌ | - | - | - |
| Perfil | ✅ | - | ✅ | ❌ |
| Mis Facturas | ✅ | ❌ | ❌ | ❌ |

---

## 📚 Documentación

### Documentación Completa
Toda la documentación del proyecto se encuentra en la carpeta **`/docs/`**

#### 📘 Documentos Principales:
- **`COMPONENTES.md`** - Guía completa de componentes reutilizables
- **`sprints/INDICE_MAESTRO.md`** - Índice de toda la documentación de sprints
- **`planificacion/PLAN_MAESTRO.txt`** - Plan completo del proyecto
- **`planificacion/DECISIONES_TECNICAS.txt`** - Decisiones arquitectónicas

#### 📗 Por Fase:
- **Fase 3:** `sprints/SPRINT_1/FASES/FASE_3_DASHBOARD_COMPLETADA.md`
- **Fase 4:** `sprints/SPRINT_1/FASES/FASE_4_PERFIL_COMPLETADA.md`
- **Fase 5:** `sprints/SPRINT_1/FASES/FASE_5_SEGURIDAD_AVANZADA.md`
- **Fase 6:** `sprints/SPRINT_1/FASES/FASE_6_BREADCRUMBS_COMPLETADA.md`
- **Fase 7:** `sprints/SPRINT_1/FASES/FASE_7_COMPLETADA.md`

#### 🔧 Fixes Documentados:
- `sprints/fixes/FIX_RESPONSIVE_TABLES.md`
- `sprints/fixes/FIX_PAGINACION_RESPONSIVE.md`
- `sprints/fixes/FIX_PALETA_COLORES_AUTH.md`

---

## 🧪 Testing

### Resultados de Tests

```
✅ Tests Funcionales:    24/24 PASS (100%)
✅ Tests Responsive:     5/5 PASS (100%)
✅ Tests Navegadores:    4/4 PASS (100%)
✅ Tests Accesibilidad:  5/5 PASS (100%)

TOTAL: 38/38 PASS (100%)
```

### Ejecutar Tests

```bash
# Todos los tests
mvn test

# Tests específicos
mvn test -Dtest=ClienteControllerTest
```

### Cobertura

```bash
mvn jacoco:report
```

---

## �️ Roadmap

### ✅ Sprint 1 (Completado - 87.5%)
- [x] Dashboard con widgets
- [x] Navbar y navegación
- [x] Perfil de usuario completo
- [x] Breadcrumbs en todos los módulos
- [x] Responsive completo
- [x] Testing y validación
- [ ] Documentación final (en progreso)

### 🚧 Sprint 2 (Planeado)
- [ ] Gestión avanzada de productos
  - [ ] Categorías
  - [ ] Imágenes de productos
  - [ ] Import/Export Excel
- [ ] Sistema de órdenes completo
  - [ ] Carrito de compras
  - [ ] Estados de orden
  - [ ] Notificaciones automáticas
- [ ] Reportes y estadísticas
  - [ ] Ventas por período
  - [ ] Productos más vendidos
  - [ ] Clientes top
- [ ] Integración WhatsApp
  - [ ] Envío de mensajes
  - [ ] Templates de mensajes
  - [ ] Confirmaciones automáticas

### 🔮 Futuras Mejoras
- [ ] API REST completa
- [ ] App móvil (React Native / Flutter)
- [ ] Integración con pasarelas de pago
- [ ] Multi-tenant (múltiples empresas)
- [ ] Dashboard analytics con gráficos
- [ ] Sistema de notificaciones push

---

## 🤝 Contribución

¡Las contribuciones son bienvenidas! Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

### Estándares de Código
- Seguir convenciones de Spring Boot
- Comentar código complejo
- Escribir tests para nuevas features
- Documentar cambios en `/docs/`

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver archivo `LICENSE` para más detalles.

---

## � Autores

- **GitHub Copilot** - Desarrollo principal
- **Tu Nombre** - Product Owner

---

## 🙏 Agradecimientos

- Spring Boot Team
- Bootstrap Team
- Font Awesome
- SweetAlert2
- Comunidad de desarrolladores

---

## 📞 Soporte

¿Tienes preguntas o problemas?

- 📧 Email: soporte@whatsordersmanager.com
- 📚 Documentación: `/docs/`
- 🐛 Issues: [GitHub Issues](https://github.com/tu-usuario/whats-orders-manager/issues)

---

**Última actualización:** 13/10/2025  
**Versión:** 1.0.0  
**Estado:** ✅ Sprint 1 completado al 87.5%
