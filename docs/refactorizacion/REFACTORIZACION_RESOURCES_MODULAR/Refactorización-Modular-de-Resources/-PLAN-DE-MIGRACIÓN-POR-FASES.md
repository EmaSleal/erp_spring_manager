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

