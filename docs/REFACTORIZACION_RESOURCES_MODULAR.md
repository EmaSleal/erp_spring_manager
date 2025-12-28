# Refactorización Modular de Resources

## 📋 Análisis de Situación Actual

### Estructura Actual de `src/main/resources/`
```
resources/
├── application.yml (configuración global)
├── static/
│   ├── css/ (13 archivos - todos en un nivel)
│   ├── js/ (23 archivos - todos en un nivel)
│   ├── images/
│   └── uploads/
└── templates/
    ├── admin/
    ├── auth/
    ├── clientes/
    ├── components/
    ├── configuracion/
    ├── dashboard/
    ├── email/
    ├── error/
    ├── facturas/
    ├── notificaciones/
    ├── perfil/
    ├── permisos/
    ├── productos/
    ├── reportes/
    ├── usuarios/
    ├── whatsapp/
    ├── index.html
    └── layout.html
```

### Problemas Identificados

1. **CSS/JS Sin Organización Modular**
   - 23 archivos JS mezclados en un solo directorio
   - 13 archivos CSS sin estructura modular
   - Difícil encontrar archivos relacionados

2. **Templates Bien Organizados (pero mejorables)**
   - Templates ya están organizados por módulo
   - Falta separación clara entre compartidos y específicos
   - `components/` debería ser más clara como "shared"

3. **Falta Separación Config vs Compartido vs Módulos**
   - No hay distinción clara entre recursos de configuración, compartidos y modulares

---

## 🎯 Propuesta de Reorganización

### Estructura Propuesta
```
resources/
├── config/
│   ├── application.yml
│   └── [futuros archivos de configuración]
│
├── shared/
│   ├── static/
│   │   ├── css/
│   │   │   ├── common.css
│   │   │   ├── forms.css
│   │   │   ├── navbar.css
│   │   │   ├── responsive.css
│   │   │   ├── sidebar.css
│   │   │   ├── styles.css (estilos base)
│   │   │   └── tables.css
│   │   │
│   │   ├── js/
│   │   │   ├── common.js
│   │   │   ├── navbar.js
│   │   │   ├── scripts.js
│   │   │   ├── sidebar.js
│   │   │   └── websocket-notificaciones.js
│   │   │
│   │   └── images/
│   │       └── [imágenes compartidas: logos, iconos, etc.]
│   │
│   └── templates/
│       ├── components/
│       │   ├── navbar.html
│       │   └── sidebar.html
│       ├── error/
│       │   ├── 403.html
│       │   ├── 404.html
│       │   ├── 500.html
│       │   └── error.html
│       ├── index.html
│       └── layout.html
│
└── modules/
    ├── cliente/
    │   ├── static/
    │   │   ├── css/
    │   │   │   └── [estilos específicos si los hay]
    │   │   └── js/
    │   │       └── clientes.js
    │   └── templates/
    │       ├── clientes.html
    │       └── form.html
    │
    ├── producto/
    │   ├── static/
    │   │   └── js/
    │   │       └── productos.js
    │   └── templates/
    │       ├── productos.html
    │       └── form.html
    │
    ├── facturacion/
    │   ├── static/
    │   │   ├── css/
    │   │   │   └── facturas.css
    │   │   └── js/
    │   │       ├── facturas.js
    │   │       └── editar-factura.js
    │   └── templates/
    │       ├── facturas.html
    │       ├── form.html
    │       └── add-form.html
    │
    ├── reportes/
    │   ├── static/
    │   │   ├── css/
    │   │   │   └── reportes.css
    │   │   └── js/
    │   │       └── reportes.js
    │   └── templates/
    │       ├── index.html
    │       ├── ventas.html
    │       ├── productos.html
    │       └── clientes.html
    │
    ├── configuracion/
    │   ├── static/
    │   │   ├── css/
    │   │   │   └── configuracion.css
    │   │   └── js/
    │   │       ├── configuration.js
    │   │       ├── configuracion-email.js
    │   │       ├── configuracion-empresa.js
    │   │       ├── configuracion-facturacion.js
    │   │       └── configuracion-parametros.js
    │   └── templates/
    │       ├── index.html
    │       ├── empresa.html
    │       ├── facturacion.html
    │       ├── notificaciones.html
    │       ├── ayuda.html
    │       └── fragments/
    │           ├── tab-email.html
    │           └── tab-parametros.html
    │
    ├── whatsapp/
    │   ├── static/
    │   │   ├── css/
    │   │   │   └── whatsapp.css
    │   │   └── js/
    │   │       ├── whatsapp-conversaciones.js
    │   │       ├── whatsapp-mensajes.js
    │   │       └── whatsapp-plantillas.js
    │   └── templates/
    │       ├── plantillas.html
    │       ├── mensajes.html
    │       ├── mensajes-old.html
    │       └── conversacion-detalle.html
    │
    ├── notificacion/
    │   ├── static/
    │   │   └── js/
    │   │       ├── notificaciones.js
    │   │       └── preferencias-notificaciones.js
    │   └── templates/
    │       ├── lista.html
    │       └── preferencias.html
    │
    ├── seguridad/
    │   ├── static/
    │   │   ├── css/
    │   │   │   └── usuarios.css
    │   │   └── js/
    │   │       ├── usuarios.js
    │   │       └── usuarios-admin.js
    │   └── templates/
    │       ├── auth/
    │       │   ├── login.html
    │       │   └── register.html
    │       ├── usuarios/
    │       │   ├── usuarios.html
    │       │   ├── form.html
    │       │   ├── lista-admin.html
    │       │   ├── form-admin.html
    │       │   └── detalle-admin.html
    │       ├── admin/
    │       │   ├── usuarios/
    │       │   │   └── permisos.html
    │       │   ├── roles/
    │       │   │   ├── roles.html
    │       │   │   └── formulario.html
    │       │   └── permisos/
    │       │       ├── gestionar.html
    │       │       └── editar.html
    │       ├── perfil/
    │       │   ├── ver.html
    │       │   └── editar.html
    │       └── permisos/
    │           └── matriz.html
    │
    ├── presentacion/
    │   ├── static/
    │   │   ├── css/
    │   │   │   └── dashboard.css
    │   │   └── js/
    │   │       └── dashboard.js
    │   └── templates/
    │       └── dashboard/
    │           └── dashboard.html
    │
    └── email/
        └── templates/
            ├── factura.html
            ├── recordatorio-pago.html
            └── credenciales-usuario.html
```

---

## 📊 Mapeo de Archivos

### Static - CSS

| Archivo Actual | Ubicación Propuesta | Categoría |
|----------------|---------------------|-----------|
| common.css | shared/static/css/ | Compartido |
| forms.css | shared/static/css/ | Compartido |
| navbar.css | shared/static/css/ | Compartido |
| responsive.css | shared/static/css/ | Compartido |
| sidebar.css | shared/static/css/ | Compartido |
| styles.css | shared/static/css/ | Compartido |
| tables.css | shared/static/css/ | Compartido |
| configuracion.css | modules/configuracion/static/css/ | Módulo |
| dashboard.css | modules/presentacion/static/css/ | Módulo |
| facturas.css | modules/facturacion/static/css/ | Módulo |
| reportes.css | modules/reportes/static/css/ | Módulo |
| usuarios.css | modules/seguridad/static/css/ | Módulo |
| whatsapp.css | modules/whatsapp/static/css/ | Módulo |

### Static - JS

| Archivo Actual | Ubicación Propuesta | Categoría |
|----------------|---------------------|-----------|
| common.js | shared/static/js/ | Compartido |
| navbar.js | shared/static/js/ | Compartido |
| scripts.js | shared/static/js/ | Compartido |
| sidebar.js | shared/static/js/ | Compartido |
| websocket-notificaciones.js | shared/static/js/ | Compartido |
| clientes.js | modules/cliente/static/js/ | Módulo |
| productos.js | modules/producto/static/js/ | Módulo |
| facturas.js | modules/facturacion/static/js/ | Módulo |
| editar-factura.js | modules/facturacion/static/js/ | Módulo |
| reportes.js | modules/reportes/static/js/ | Módulo |
| configuration.js | modules/configuracion/static/js/ | Módulo |
| configuracion-email.js | modules/configuracion/static/js/ | Módulo |
| configuracion-empresa.js | modules/configuracion/static/js/ | Módulo |
| configuracion-facturacion.js | modules/configuracion/static/js/ | Módulo |
| configuracion-parametros.js | modules/configuracion/static/js/ | Módulo |
| whatsapp-conversaciones.js | modules/whatsapp/static/js/ | Módulo |
| whatsapp-mensajes.js | modules/whatsapp/static/js/ | Módulo |
| whatsapp-plantillas.js | modules/whatsapp/static/js/ | Módulo |
| notificaciones.js | modules/notificacion/static/js/ | Módulo |
| preferencias-notificaciones.js | modules/notificacion/static/js/ | Módulo |
| usuarios.js | modules/seguridad/static/js/ | Módulo |
| usuarios-admin.js | modules/seguridad/static/js/ | Módulo |
| dashboard.js | modules/presentacion/static/js/ | Módulo |

### Templates

| Directorio Actual | Ubicación Propuesta | Categoría |
|-------------------|---------------------|-----------|
| components/ | shared/templates/components/ | Compartido |
| error/ | shared/templates/error/ | Compartido |
| index.html | shared/templates/ | Compartido |
| layout.html | shared/templates/ | Compartido |
| clientes/ | modules/cliente/templates/ | Módulo |
| productos/ | modules/producto/templates/ | Módulo |
| facturas/ | modules/facturacion/templates/ | Módulo |
| reportes/ | modules/reportes/templates/ | Módulo |
| configuracion/ | modules/configuracion/templates/ | Módulo |
| whatsapp/ | modules/whatsapp/templates/ | Módulo |
| notificaciones/ | modules/notificacion/templates/ | Módulo |
| auth/ | modules/seguridad/templates/auth/ | Módulo |
| usuarios/ | modules/seguridad/templates/usuarios/ | Módulo |
| admin/ | modules/seguridad/templates/admin/ | Módulo |
| perfil/ | modules/seguridad/templates/perfil/ | Módulo |
| permisos/ | modules/seguridad/templates/permisos/ | Módulo |
| dashboard/ | modules/presentacion/templates/dashboard/ | Módulo |
| email/ | modules/email/templates/ | Módulo |

---

## ⚠️ Consideraciones Importantes

### 1. **Rutas en Spring Boot**

Spring Boot busca recursos estáticos en estas ubicaciones por defecto:
- `/static/`
- `/public/`
- `/resources/`
- `/META-INF/resources/`

**Problema:** Si movemos archivos a `modules/`, Spring Boot NO los encontrará automáticamente.

**Soluciones:**

#### Opción A: Configurar ResourceHandlers (RECOMENDADA)
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Recursos compartidos
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/shared/static/");
        
        // Recursos por módulo
        registry.addResourceHandler("/modules/cliente/static/**")
                .addResourceLocations("classpath:/modules/cliente/static/");
        
        registry.addResourceHandler("/modules/producto/static/**")
                .addResourceLocations("classpath:/modules/producto/static/");
        
        // ... etc para cada módulo
    }
}
```

#### Opción B: Mantener estructura plana pero organizada
```
static/
├── shared/
│   ├── css/
│   ├── js/
│   └── images/
└── modules/
    ├── cliente/
    ├── producto/
    └── ...
```

Esta opción mantiene todo bajo `/static/` pero organizado internamente.

### 2. **Thymeleaf Template Resolution**

Thymeleaf busca templates en `classpath:/templates/` por defecto.

**Problema:** Similar a los archivos estáticos, si movemos templates a `modules/`, necesitamos configuración adicional.

**Soluciones:**

#### Opción A: Configurar múltiples template resolvers
```java
@Configuration
public class ThymeleafConfig {
    
    @Bean
    public SpringResourceTemplateResolver sharedTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/shared/templates/");
        resolver.setSuffix(".html");
        resolver.setOrder(1);
        resolver.setCheckExistence(true);
        return resolver;
    }
    
    @Bean
    public SpringResourceTemplateResolver modulesTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/modules/");
        resolver.setSuffix(".html");
        resolver.setOrder(2);
        resolver.setCheckExistence(true);
        return resolver;
    }
}
```

#### Opción B: Usar prefijos en los nombres de vistas
Mantener templates en `classpath:/templates/` pero organizados:
```
templates/
├── shared/
└── modules/
    ├── cliente/
    └── ...
```

Y en los controladores usar:
```java
return "modules/cliente/templates/clientes";
```

### 3. **Compatibilidad con Referencias Actuales**

Todos los templates y archivos HTML tienen referencias a CSS/JS que deberán actualizarse:

```html
<!-- ANTES -->
<link th:href="@{/css/clientes.css}" rel="stylesheet">
<script th:src="@{/js/clientes.js}"></script>

<!-- DESPUÉS (Opción A - con resource handlers) -->
<link th:href="@{/modules/cliente/static/css/clientes.css}" rel="stylesheet">
<script th:src="@{/modules/cliente/static/js/clientes.js}"></script>

<!-- DESPUÉS (Opción B - estructura plana organizada) -->
<link th:href="@{/static/modules/cliente/css/clientes.css}" rel="stylesheet">
<script th:src="@{/static/modules/cliente/js/clientes.js}"></script>
```

---

## 🎯 Recomendación Final

### **Opción Híbrida Recomendada**

Mantener la compatibilidad con Spring Boot pero con organización modular:

```
resources/
├── application.yml (config global)
│
├── static/
│   ├── shared/
│   │   ├── css/ (estilos compartidos)
│   │   ├── js/ (scripts compartidos)
│   │   └── images/ (imágenes compartidas)
│   │
│   ├── modules/
│   │   ├── cliente/
│   │   │   ├── css/
│   │   │   └── js/
│   │   ├── producto/
│   │   │   └── js/
│   │   ├── facturacion/
│   │   │   ├── css/
│   │   │   └── js/
│   │   └── ...
│   │
│   └── uploads/ (archivos subidos por usuarios)
│
└── templates/
    ├── shared/
    │   ├── components/
    │   ├── error/
    │   ├── layout.html
    │   └── index.html
    │
    └── modules/
        ├── cliente/
        ├── producto/
        ├── facturacion/
        └── ...
```

### Ventajas de esta Opción:

1. ✅ **Sin configuración adicional** - Spring Boot encuentra los recursos automáticamente
2. ✅ **Organización modular** - Archivos agrupados por módulo
3. ✅ **Separación clara** - `shared/` vs `modules/`
4. ✅ **Fácil migración** - Solo requiere mover archivos y actualizar referencias
5. ✅ **Mantenibilidad** - Estructura clara y coherente con el código Java

### Rutas después de reorganización:

```html
<!-- Recursos compartidos -->
<link th:href="@{/shared/css/common.css}" rel="stylesheet">
<script th:src="@{/shared/js/common.js}"></script>

<!-- Recursos de módulos -->
<link th:href="@{/modules/cliente/css/clientes.css}" rel="stylesheet">
<script th:src="@{/modules/facturacion/js/facturas.js}"></script>
```

```java
// Templates compartidos
return "shared/layout";
return "shared/error/404";

// Templates de módulos
return "modules/cliente/clientes";
return "modules/facturacion/form";
```

---

## 📋 Plan de Ejecución

### Fase 1: Preparación
1. ✅ Documentar estructura actual
2. ✅ Definir estructura objetivo
3. ⬜ Crear script de migración
4. ⬜ Hacer backup/commit de estado actual

### Fase 2: Reorganización de Static
1. ⬜ Crear estructura de directorios en `static/`
2. ⬜ Mover archivos CSS compartidos a `static/shared/css/`
3. ⬜ Mover archivos JS compartidos a `static/shared/js/`
4. ⬜ Mover archivos CSS de módulos a `static/modules/{modulo}/css/`
5. ⬜ Mover archivos JS de módulos a `static/modules/{modulo}/js/`
6. ⬜ Actualizar referencias en templates HTML

### Fase 3: Reorganización de Templates
1. ⬜ Crear estructura de directorios en `templates/`
2. ⬜ Mover templates compartidos a `templates/shared/`
3. ⬜ Mover templates de módulos a `templates/modules/{modulo}/`
4. ⬜ Actualizar referencias en controladores Java
5. ⬜ Actualizar referencias th:fragment y th:replace

### Fase 4: Verificación
1. ⬜ Compilar proyecto
2. ⬜ Probar rutas de recursos estáticos
3. ⬜ Probar rutas de templates
4. ⬜ Verificar que todas las vistas se renderizan correctamente
5. ⬜ Revisar console del navegador para errores 404

### Fase 5: Documentación
1. ⬜ Actualizar ESTRUCTURA_ARCHIVOS.md
2. ⬜ Documentar nuevas convenciones
3. ⬜ Crear guía para nuevos desarrolladores

---

## 🔍 Archivos que Requerirán Actualización

### Controladores Java (~15 archivos)
Buscar y reemplazar rutas de templates:
```bash
# Buscar
return "clientes/form";

# Reemplazar
return "modules/cliente/form";
```

### Templates HTML (~80 archivos)
Buscar y reemplazar referencias a CSS/JS:
```bash
# Buscar
th:href="@{/css/clientes.css}"

# Reemplazar
th:href="@{/modules/cliente/css/clientes.css}"
```

### Layout.html y templates base
Actualizar referencias a recursos compartidos:
```html
<!-- Compartidos -->
<link th:href="@{/shared/css/common.css}" rel="stylesheet">
<link th:href="@{/shared/css/navbar.css}" rel="stylesheet">
<link th:href="@{/shared/css/sidebar.css}" rel="stylesheet">
```

---

## 🎓 Lecciones del Código Java

De la refactorización de código Java aprendimos:

1. **Planificar antes de ejecutar** - Documentar estructura objetivo
2. **Migración incremental** - Hacer cambios por fases
3. **Verificación constante** - Compilar después de cada fase
4. **Usar git** - Commits frecuentes para poder revertir
5. **Actualizar imports/referencias** - No olvidar actualizar todas las referencias

Aplicaremos estos principios a la reorganización de resources.

---

## ❓ Preguntas para Decidir

1. **¿Prefieres la opción híbrida (mantener static/ y templates/) o reorganización completa con configuración adicional?**
   - Recomiendo: Opción híbrida por simplicidad

2. **¿Quieres que ejecutemos la migración ahora o prefieres revisarla primero?**
   - Sugerencia: Revisar y aprobar plan antes de ejecutar

3. **¿Hay archivos específicos de static/ que quieras mantener fuera de la reorganización?**
   - Ejemplo: uploads/ debería quedarse en /static/uploads/

4. **¿Prefieres hacer la reorganización completa de una vez o por módulos incrementalmente?**
   - Recomiendo: Por fases (primero static, luego templates)

---

## 📊 Impacto Estimado

- **Archivos a mover:** ~100+ (36 static + 80+ templates)
- **Archivos a actualizar:** ~80+ templates + ~15 controladores
- **Tiempo estimado:** 2-3 horas
- **Riesgo:** MEDIO (muchas referencias a actualizar)
- **Beneficio:** ALTO (organización coherente con código Java)

---

## 🎯 DECISIÓN FINAL Y RECOMENDACIÓN

### Enfoque Recomendado: **Opción Híbrida - Migración por Fases**

Después de analizar la estructura actual y considerando:
- Compatibilidad con Spring Boot
- Complejidad de configuración
- Riesgo de romper referencias
- Coherencia con la arquitectura Java

**RECOMENDACIÓN: Mantener `static/` y `templates/` pero reorganizar su contenido internamente**

### Estructura Final Recomendada

```
resources/
├── application.yml (mantener en raíz)
│
├── static/
│   ├── shared/          ← Recursos compartidos entre módulos
│   │   ├── css/
│   │   │   ├── common.css
│   │   │   ├── forms.css
│   │   │   ├── navbar.css
│   │   │   ├── responsive.css
│   │   │   ├── sidebar.css
│   │   │   ├── styles.css
│   │   │   └── tables.css
│   │   ├── js/
│   │   │   ├── common.js
│   │   │   ├── navbar.js
│   │   │   ├── scripts.js
│   │   │   ├── sidebar.js
│   │   │   └── websocket-notificaciones.js
│   │   └── images/
│   │       └── [logos, iconos compartidos]
│   │
│   ├── modules/         ← Recursos específicos por módulo
│   │   ├── cliente/
│   │   │   └── js/
│   │   │       └── clientes.js
│   │   ├── producto/
│   │   │   └── js/
│   │   │       └── productos.js
│   │   ├── facturacion/
│   │   │   ├── css/
│   │   │   │   └── facturas.css
│   │   │   └── js/
│   │   │       ├── facturas.js
│   │   │       └── editar-factura.js
│   │   ├── reportes/
│   │   │   ├── css/
│   │   │   │   └── reportes.css
│   │   │   └── js/
│   │   │       └── reportes.js
│   │   ├── configuracion/
│   │   │   ├── css/
│   │   │   │   └── configuracion.css
│   │   │   └── js/
│   │   │       ├── configuration.js
│   │   │       ├── configuracion-email.js
│   │   │       ├── configuracion-empresa.js
│   │   │       ├── configuracion-facturacion.js
│   │   │       └── configuracion-parametros.js
│   │   ├── whatsapp/
│   │   │   ├── css/
│   │   │   │   └── whatsapp.css
│   │   │   └── js/
│   │   │       ├── whatsapp-conversaciones.js
│   │   │       ├── whatsapp-mensajes.js
│   │   │       └── whatsapp-plantillas.js
│   │   ├── notificacion/
│   │   │   └── js/
│   │   │       ├── notificaciones.js
│   │   │       └── preferencias-notificaciones.js
│   │   ├── seguridad/
│   │   │   ├── css/
│   │   │   │   └── usuarios.css
│   │   │   └── js/
│   │   │       ├── usuarios.js
│   │   │       └── usuarios-admin.js
│   │   └── presentacion/
│   │       ├── css/
│   │       │   └── dashboard.css
│   │       └── js/
│   │           └── dashboard.js
│   │
│   └── uploads/         ← Mantener fuera (contenido dinámico)
│       └── avatars/
│
└── templates/
    ├── shared/          ← Templates compartidos
    │   ├── components/
    │   │   ├── navbar.html
    │   │   └── sidebar.html
    │   ├── error/
    │   │   ├── 403.html
    │   │   ├── 404.html
    │   │   ├── 500.html
    │   │   └── error.html
    │   ├── layout.html
    │   └── index.html
    │
    └── modules/         ← Templates por módulo
        ├── cliente/
        │   ├── clientes.html
        │   └── form.html
        ├── producto/
        │   ├── productos.html
        │   └── form.html
        ├── facturacion/
        │   ├── facturas.html
        │   ├── form.html
        │   └── add-form.html
        ├── reportes/
        │   ├── index.html
        │   ├── ventas.html
        │   ├── productos.html
        │   └── clientes.html
        ├── configuracion/
        │   ├── index.html
        │   ├── empresa.html
        │   ├── facturacion.html
        │   ├── notificaciones.html
        │   ├── ayuda.html
        │   └── fragments/
        │       ├── tab-email.html
        │       └── tab-parametros.html
        ├── whatsapp/
        │   ├── plantillas.html
        │   ├── mensajes.html
        │   ├── mensajes-old.html
        │   └── conversacion-detalle.html
        ├── notificacion/
        │   ├── lista.html
        │   └── preferencias.html
        ├── seguridad/
        │   ├── auth/
        │   │   ├── login.html
        │   │   └── register.html
        │   ├── usuarios/
        │   │   ├── usuarios.html
        │   │   ├── form.html
        │   │   ├── lista-admin.html
        │   │   ├── form-admin.html
        │   │   └── detalle-admin.html
        │   ├── admin/
        │   │   ├── usuarios/
        │   │   │   └── permisos.html
        │   │   ├── roles/
        │   │   │   ├── roles.html
        │   │   │   └── formulario.html
        │   │   └── permisos/
        │   │       ├── gestionar.html
        │   │       └── editar.html
        │   ├── perfil/
        │   │   ├── ver.html
        │   │   └── editar.html
        │   └── permisos/
        │       └── matriz.html
        ├── presentacion/
        │   └── dashboard/
        │       └── dashboard.html
        └── email/
            ├── factura.html
            ├── recordatorio-pago.html
            └── credenciales-usuario.html
```

### Ventajas de Este Enfoque

1. ✅ **Sin configuración adicional en Spring Boot**
2. ✅ **Rutas simples y predecibles**
3. ✅ **Organización modular clara**
4. ✅ **Migración incremental posible**
5. ✅ **Fácil rollback si algo sale mal**

---

## 📋 PLAN DE MIGRACIÓN POR FASES

### **FASE 1: Preparación y Backup** ⏱️ 15 min

**Objetivos:**
- Crear backup del estado actual
- Verificar que todo funciona antes de empezar
- Crear estructura de directorios objetivo

**Tareas:**
```powershell
# 1. Commit del estado actual
git add .
git commit -m "checkpoint: Estado antes de reorganizar resources"

# 2. Verificar que la aplicación funciona
./mvnw clean compile
./mvnw spring-boot:run

# 3. Crear estructura de directorios
```

**Resultado esperado:** ✅ Commit de seguridad creado

---

### **FASE 2: Reorganizar Static - CSS** ⏱️ 30 min

**Objetivos:**
- Mover archivos CSS a estructura modular
- Actualizar referencias en templates
- Verificar que los estilos se cargan correctamente

**Archivos a mover:**

```powershell
# Crear estructura
New-Item -Path "src/main/resources/static/shared/css" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/facturacion/css" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/reportes/css" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/configuracion/css" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/whatsapp/css" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/seguridad/css" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/presentacion/css" -ItemType Directory -Force

# Mover archivos compartidos
Move-Item "src/main/resources/static/css/common.css" "src/main/resources/static/shared/css/"
Move-Item "src/main/resources/static/css/forms.css" "src/main/resources/static/shared/css/"
Move-Item "src/main/resources/static/css/navbar.css" "src/main/resources/static/shared/css/"
Move-Item "src/main/resources/static/css/responsive.css" "src/main/resources/static/shared/css/"
Move-Item "src/main/resources/static/css/sidebar.css" "src/main/resources/static/shared/css/"
Move-Item "src/main/resources/static/css/styles.css" "src/main/resources/static/shared/css/"
Move-Item "src/main/resources/static/css/tables.css" "src/main/resources/static/shared/css/"

# Mover archivos de módulos
Move-Item "src/main/resources/static/css/facturas.css" "src/main/resources/static/modules/facturacion/css/"
Move-Item "src/main/resources/static/css/reportes.css" "src/main/resources/static/modules/reportes/css/"
Move-Item "src/main/resources/static/css/configuracion.css" "src/main/resources/static/modules/configuracion/css/"
Move-Item "src/main/resources/static/css/whatsapp.css" "src/main/resources/static/modules/whatsapp/css/"
Move-Item "src/main/resources/static/css/usuarios.css" "src/main/resources/static/modules/seguridad/css/"
Move-Item "src/main/resources/static/css/dashboard.css" "src/main/resources/static/modules/presentacion/css/"

# Eliminar directorio viejo si está vacío
Remove-Item "src/main/resources/static/css" -Force
```

**Actualizar referencias:**

Buscar y reemplazar en TODOS los archivos `.html`:

```
BUSCAR: th:href="@{/css/common.css}"
REEMPLAZAR: th:href="@{/shared/css/common.css}"

BUSCAR: th:href="@{/css/facturas.css}"
REEMPLAZAR: th:href="@{/modules/facturacion/css/facturas.css}"

BUSCAR: th:href="@{/css/dashboard.css}"
REEMPLAZAR: th:href="@{/modules/presentacion/css/dashboard.css}"

# ... etc para cada archivo CSS
```

**Verificación:**
```powershell
# Compilar
./mvnw clean compile

# Ejecutar y verificar en navegador que los estilos se cargan
./mvnw spring-boot:run

# Abrir navegador y verificar console (F12) - No debe haber errores 404
```

**Resultado esperado:** ✅ CSS reorganizado y funcionando

**Commit:**
```powershell
git add .
git commit -m "refactor(resources): Reorganizar archivos CSS en estructura modular"
```

---

### **FASE 3: Reorganizar Static - JavaScript** ⏱️ 45 min

**Objetivos:**
- Mover archivos JS a estructura modular
- Actualizar referencias en templates
- Verificar funcionalidad JavaScript

**Archivos a mover:**

```powershell
# Crear estructura
New-Item -Path "src/main/resources/static/shared/js" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/cliente/js" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/producto/js" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/facturacion/js" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/reportes/js" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/configuracion/js" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/whatsapp/js" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/notificacion/js" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/seguridad/js" -ItemType Directory -Force
New-Item -Path "src/main/resources/static/modules/presentacion/js" -ItemType Directory -Force

# Mover archivos compartidos
Move-Item "src/main/resources/static/js/common.js" "src/main/resources/static/shared/js/"
Move-Item "src/main/resources/static/js/navbar.js" "src/main/resources/static/shared/js/"
Move-Item "src/main/resources/static/js/scripts.js" "src/main/resources/static/shared/js/"
Move-Item "src/main/resources/static/js/sidebar.js" "src/main/resources/static/shared/js/"
Move-Item "src/main/resources/static/js/websocket-notificaciones.js" "src/main/resources/static/shared/js/"

# Mover archivos de módulos
Move-Item "src/main/resources/static/js/clientes.js" "src/main/resources/static/modules/cliente/js/"
Move-Item "src/main/resources/static/js/productos.js" "src/main/resources/static/modules/producto/js/"
Move-Item "src/main/resources/static/js/facturas.js" "src/main/resources/static/modules/facturacion/js/"
Move-Item "src/main/resources/static/js/editar-factura.js" "src/main/resources/static/modules/facturacion/js/"
Move-Item "src/main/resources/static/js/reportes.js" "src/main/resources/static/modules/reportes/js/"
Move-Item "src/main/resources/static/js/configuration.js" "src/main/resources/static/modules/configuracion/js/"
Move-Item "src/main/resources/static/js/configuracion-email.js" "src/main/resources/static/modules/configuracion/js/"
Move-Item "src/main/resources/static/js/configuracion-empresa.js" "src/main/resources/static/modules/configuracion/js/"
Move-Item "src/main/resources/static/js/configuracion-facturacion.js" "src/main/resources/static/modules/configuracion/js/"
Move-Item "src/main/resources/static/js/configuracion-parametros.js" "src/main/resources/static/modules/configuracion/js/"
Move-Item "src/main/resources/static/js/whatsapp-conversaciones.js" "src/main/resources/static/modules/whatsapp/js/"
Move-Item "src/main/resources/static/js/whatsapp-mensajes.js" "src/main/resources/static/modules/whatsapp/js/"
Move-Item "src/main/resources/static/js/whatsapp-plantillas.js" "src/main/resources/static/modules/whatsapp/js/"
Move-Item "src/main/resources/static/js/notificaciones.js" "src/main/resources/static/modules/notificacion/js/"
Move-Item "src/main/resources/static/js/preferencias-notificaciones.js" "src/main/resources/static/modules/notificacion/js/"
Move-Item "src/main/resources/static/js/usuarios.js" "src/main/resources/static/modules/seguridad/js/"
Move-Item "src/main/resources/static/js/usuarios-admin.js" "src/main/resources/static/modules/seguridad/js/"
Move-Item "src/main/resources/static/js/dashboard.js" "src/main/resources/static/modules/presentacion/js/"

# Eliminar directorio viejo si está vacío
Remove-Item "src/main/resources/static/js" -Force
```

**Actualizar referencias:**

Buscar y reemplazar en TODOS los archivos `.html`:

```
BUSCAR: th:src="@{/js/common.js}"
REEMPLAZAR: th:src="@{/shared/js/common.js}"

BUSCAR: th:src="@{/js/clientes.js}"
REEMPLAZAR: th:src="@{/modules/cliente/js/clientes.js}"

# ... etc
```

**Verificación:**
```powershell
./mvnw clean compile
./mvnw spring-boot:run

# Probar funcionalidades JavaScript en el navegador
# Verificar console (F12) - No errores 404
```

**Resultado esperado:** ✅ JavaScript reorganizado y funcionando

**Commit:**
```powershell
git add .
git commit -m "refactor(resources): Reorganizar archivos JavaScript en estructura modular"
```

---

### **FASE 4: Reorganizar Templates** ⏱️ 60 min

**Objetivos:**
- Mover templates a estructura modular
- Actualizar referencias en controladores
- Actualizar fragmentos y referencias entre templates

**Paso 4.1: Crear estructura de directorios**

```powershell
# Shared
New-Item -Path "src/main/resources/templates/shared" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/shared/components" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/shared/error" -ItemType Directory -Force

# Modules
New-Item -Path "src/main/resources/templates/modules" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/cliente" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/producto" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/facturacion" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/reportes" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/configuracion" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/configuracion/fragments" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/whatsapp" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/notificacion" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/seguridad" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/seguridad/auth" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/seguridad/usuarios" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/seguridad/admin" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/seguridad/admin/usuarios" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/seguridad/admin/roles" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/seguridad/admin/permisos" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/seguridad/perfil" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/seguridad/permisos" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/presentacion" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/presentacion/dashboard" -ItemType Directory -Force
New-Item -Path "src/main/resources/templates/modules/email" -ItemType Directory -Force
```

**Paso 4.2: Mover templates compartidos**

```powershell
# Shared
Move-Item "src/main/resources/templates/components/*" "src/main/resources/templates/shared/components/"
Move-Item "src/main/resources/templates/error/*" "src/main/resources/templates/shared/error/"
Move-Item "src/main/resources/templates/layout.html" "src/main/resources/templates/shared/"
Move-Item "src/main/resources/templates/index.html" "src/main/resources/templates/shared/"
```

**Paso 4.3: Mover templates de módulos**

```powershell
# Cliente
Move-Item "src/main/resources/templates/clientes/*" "src/main/resources/templates/modules/cliente/"

# Producto
Move-Item "src/main/resources/templates/productos/*" "src/main/resources/templates/modules/producto/"

# Facturación
Move-Item "src/main/resources/templates/facturas/*" "src/main/resources/templates/modules/facturacion/"

# Reportes
Move-Item "src/main/resources/templates/reportes/*" "src/main/resources/templates/modules/reportes/"

# Configuración
Move-Item "src/main/resources/templates/configuracion/*" "src/main/resources/templates/modules/configuracion/"

# WhatsApp
Move-Item "src/main/resources/templates/whatsapp/*" "src/main/resources/templates/modules/whatsapp/"

# Notificación
Move-Item "src/main/resources/templates/notificaciones/*" "src/main/resources/templates/modules/notificacion/"

# Seguridad (varios subdirectorios)
Move-Item "src/main/resources/templates/auth/*" "src/main/resources/templates/modules/seguridad/auth/"
Move-Item "src/main/resources/templates/usuarios/*" "src/main/resources/templates/modules/seguridad/usuarios/"
Move-Item "src/main/resources/templates/admin/usuarios/*" "src/main/resources/templates/modules/seguridad/admin/usuarios/"
Move-Item "src/main/resources/templates/admin/roles/*" "src/main/resources/templates/modules/seguridad/admin/roles/"
Move-Item "src/main/resources/templates/admin/permisos/*" "src/main/resources/templates/modules/seguridad/admin/permisos/"
Move-Item "src/main/resources/templates/perfil/*" "src/main/resources/templates/modules/seguridad/perfil/"
Move-Item "src/main/resources/templates/permisos/*" "src/main/resources/templates/modules/seguridad/permisos/"

# Presentación
Move-Item "src/main/resources/templates/dashboard/*" "src/main/resources/templates/modules/presentacion/dashboard/"

# Email
Move-Item "src/main/resources/templates/email/*" "src/main/resources/templates/modules/email/"

# Eliminar directorios viejos
Remove-Item "src/main/resources/templates/components" -Force
Remove-Item "src/main/resources/templates/error" -Force
Remove-Item "src/main/resources/templates/clientes" -Force
Remove-Item "src/main/resources/templates/productos" -Force
Remove-Item "src/main/resources/templates/facturas" -Force
Remove-Item "src/main/resources/templates/reportes" -Force
Remove-Item "src/main/resources/templates/configuracion" -Force
Remove-Item "src/main/resources/templates/whatsapp" -Force
Remove-Item "src/main/resources/templates/notificaciones" -Force
Remove-Item "src/main/resources/templates/auth" -Force
Remove-Item "src/main/resources/templates/usuarios" -Force
Remove-Item "src/main/resources/templates/admin" -Recurse -Force
Remove-Item "src/main/resources/templates/perfil" -Force
Remove-Item "src/main/resources/templates/permisos" -Force
Remove-Item "src/main/resources/templates/dashboard" -Force
Remove-Item "src/main/resources/templates/email" -Force
```

**Paso 4.4: Actualizar referencias en Controladores Java**

Buscar en todos los controladores (`.java`) y actualizar:

```java
// ANTES
return "clientes/clientes";
return "productos/form";
return "facturas/facturas";

// DESPUÉS
return "modules/cliente/clientes";
return "modules/producto/form";
return "modules/facturacion/facturas";
```

**Paso 4.5: Actualizar referencias en Templates (th:fragment, th:replace)**

Buscar en todos los `.html` y actualizar:

```html
<!-- ANTES -->
<div th:replace="~{layout :: layout}"></div>
<div th:replace="~{components/navbar :: navbar}"></div>
<div th:replace="~{components/sidebar :: sidebar}"></div>

<!-- DESPUÉS -->
<div th:replace="~{shared/layout :: layout}"></div>
<div th:replace="~{shared/components/navbar :: navbar}"></div>
<div th:replace="~{shared/components/sidebar :: sidebar}"></div>
```

**Verificación:**
```powershell
./mvnw clean compile
./mvnw spring-boot:run

# Navegar por TODAS las páginas de la aplicación
# Verificar que se renderizan correctamente
```

**Resultado esperado:** ✅ Templates reorganizados y funcionando

**Commit:**
```powershell
git add .
git commit -m "refactor(resources): Reorganizar templates en estructura modular"
```

---

### **FASE 5: Verificación Final y Documentación** ⏱️ 30 min

**Tareas:**

1. **Compilación completa**
```powershell
./mvnw clean package -DskipTests
```

2. **Ejecutar aplicación y probar TODAS las funcionalidades**
```powershell
./mvnw spring-boot:run
```

Verificar:
- ✅ Login/Logout
- ✅ Dashboard
- ✅ Clientes (lista, crear, editar)
- ✅ Productos (lista, crear, editar)
- ✅ Facturas (lista, crear, editar, anular)
- ✅ Reportes (ventas, productos, clientes)
- ✅ Configuración (empresa, facturación, email)
- ✅ WhatsApp (plantillas, mensajes, conversaciones)
- ✅ Notificaciones
- ✅ Usuarios y permisos

3. **Verificar navegador (F12)**
- ✅ No errores 404 en console
- ✅ CSS se carga correctamente
- ✅ JavaScript funciona sin errores

4. **Actualizar documentación**
```powershell
# Actualizar ESTRUCTURA_ARCHIVOS.md en static/
# Crear/actualizar README en cada carpeta modules/
```

5. **Commit final**
```powershell
git add .
git commit -m "refactor(resources): Completar reorganización modular de resources - Actualizar documentación"
```

**Resultado esperado:** ✅ Reorganización completa y documentada

---

## 📊 Resumen de Cambios por Fase

| Fase | Archivos Afectados | Tiempo | Riesgo | Rollback |
|------|-------------------|---------|--------|----------|
| 1. Preparación | 0 | 15 min | Bajo | - |
| 2. CSS | 13 archivos + ~30 templates | 30 min | Bajo | git revert |
| 3. JavaScript | 23 archivos + ~30 templates | 45 min | Medio | git revert |
| 4. Templates | ~80 templates + ~15 controllers | 60 min | Alto | git revert |
| 5. Verificación | Todos | 30 min | - | - |
| **TOTAL** | **~150+ archivos** | **3h** | **Medio** | **git reset --hard** |

---

## 🔧 Scripts de Ayuda

### Script para buscar y reemplazar en templates

```powershell
# Actualizar referencias CSS compartidos
Get-ChildItem -Path "src/main/resources/templates" -Filter "*.html" -Recurse | ForEach-Object {
    (Get-Content $_.FullName) -replace '@\{/css/common\.css\}', '@{/shared/css/common.css}' | Set-Content $_.FullName
    (Get-Content $_.FullName) -replace '@\{/css/forms\.css\}', '@{/shared/css/forms.css}' | Set-Content $_.FullName
    (Get-Content $_.FullName) -replace '@\{/css/navbar\.css\}', '@{/shared/css/navbar.css}' | Set-Content $_.FullName
    # ... etc
}

# Actualizar referencias JS compartidos
Get-ChildItem -Path "src/main/resources/templates" -Filter "*.html" -Recurse | ForEach-Object {
    (Get-Content $_.FullName) -replace '@\{/js/common\.js\}', '@{/shared/js/common.js}' | Set-Content $_.FullName
    (Get-Content $_.FullName) -replace '@\{/js/sidebar\.js\}', '@{/shared/js/sidebar.js}' | Set-Content $_.FullName
    # ... etc
}
```

### Script para actualizar controladores

```powershell
# Actualizar returns en controladores
Get-ChildItem -Path "src/main/java" -Filter "*Controller.java" -Recurse | ForEach-Object {
    (Get-Content $_.FullName) -replace 'return "clientes/', 'return "modules/cliente/' | Set-Content $_.FullName
    (Get-Content $_.FullName) -replace 'return "productos/', 'return "modules/producto/' | Set-Content $_.FullName
    (Get-Content $_.FullName) -replace 'return "facturas/', 'return "modules/facturacion/' | Set-Content $_.FullName
    # ... etc
}
```

---

**¿Procedemos con la reorganización?** 🚀

**Opciones:**
1. ✅ **Ejecutar Fase 1** (Preparación) - 15 min
2. ✅ **Ejecutar Fases 1-2** (Preparación + CSS) - 45 min
3. ✅ **Ejecutar Fases 1-3** (Preparación + CSS + JS) - 1h 30min
4. ✅ **Ejecutar completo** (Todas las fases) - 3h
5. ⏸️ **Revisar más antes de empezar**
