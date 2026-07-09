# 🧪 FASE 7: TESTING Y VALIDACIÓN - GUÍA COMPLETA

**Proyecto:** WhatsApp Orders Manager  
**Sprint:** Sprint 1  
**Fase:** 7 - Testing y Validación  
**Fecha:** 12 de octubre de 2025  
**Estado:** 🔄 EN PROGRESO

---

## 📋 OBJETIVO DE LA FASE

Validar que TODAS las funcionalidades implementadas en Sprint 1 funcionan correctamente:
- ✅ Funcionalidad completa
- ✅ Roles y permisos
- ✅ Responsive design
- ✅ Compatibilidad de navegadores
- ✅ Accesibilidad (WCAG 2.1)

---

## 🎯 PUNTOS DE TESTING

### 7.1 - Pruebas Funcionales
Validar que todas las funcionalidades básicas funcionan correctamente.

### 7.2 - Pruebas de Roles
Validar que los permisos por rol se aplican correctamente.

### 7.3 - Pruebas Responsive
Validar que el diseño se adapta a diferentes tamaños de pantalla.

### 7.4 - Pruebas de Navegadores
Validar compatibilidad cross-browser.

### 7.5 - Validación de Accesibilidad
Validar cumplimiento de estándares WCAG 2.1.

---

## 📝 PUNTO 7.1: PRUEBAS FUNCIONALES

### Objetivo
Verificar que todas las funcionalidades implementadas funcionan como se espera.

### Checklist de Pruebas

#### 🔐 Autenticación y Sesión

**Test 1: Login Exitoso**
```
Pasos:
1. Ir a http://localhost:8080/auth/login
2. Ingresar credenciales válidas
3. Click en "Iniciar Sesión"

Resultado esperado:
✅ Redirect a /dashboard
✅ Sesión activa
✅ Navbar muestra nombre de usuario
```

**Test 2: Login con Credenciales Incorrectas**
```
Pasos:
1. Ir a http://localhost:8080/auth/login
2. Ingresar credenciales inválidas
3. Click en "Iniciar Sesión"

Resultado esperado:
✅ Mensaje de error
✅ Permanece en /auth/login
✅ No se crea sesión
```

**Test 3: Logout**
```
Pasos:
1. Estar logueado
2. Click en dropdown usuario
3. Click en "Cerrar sesión"

Resultado esperado:
✅ Sesión cerrada
✅ Redirect a /auth/login
✅ No se puede acceder a rutas protegidas
```

---

#### 📊 Dashboard

**Test 4: Dashboard Muestra Estadísticas**
```
Pasos:
1. Login exitoso
2. Observar dashboard

Resultado esperado:
✅ Widget "Total Clientes" muestra número correcto
✅ Widget "Total Productos" muestra número correcto
✅ Widget "Total Facturas" muestra número correcto
✅ Widget "Total Pagos" muestra número correcto
```

**Test 5: Click en Módulo Activo**
```
Pasos:
1. En dashboard, click en tarjeta "Clientes"

Resultado esperado:
✅ Navega a /clientes
✅ Se muestra lista de clientes
✅ Breadcrumbs correctos
```

**Test 6: Click en Módulo Inactivo**
```
Pasos:
1. En dashboard, click en tarjeta deshabilitada (ej: "Reportes")

Resultado esperado:
✅ SweetAlert muestra mensaje "Módulo en desarrollo"
✅ No navega a ninguna página
✅ Permanece en dashboard
```

---

#### 👤 Perfil de Usuario

**Test 7: Ver Perfil**
```
Pasos:
1. Click en dropdown usuario
2. Click en "Ver mi perfil"

Resultado esperado:
✅ Navega a /perfil
✅ Muestra datos del usuario
✅ Breadcrumbs: Dashboard → Mi Perfil
✅ Muestra avatar o iniciales
```

**Test 8: Editar Perfil - Datos Personales**
```
Pasos:
1. Ir a /perfil
2. Click en "Editar Perfil" o ir a /perfil/editar
3. Modificar nombre, apellido, email
4. Click en "Guardar Cambios"

Resultado esperado:
✅ Mensaje de éxito
✅ Datos actualizados en BD
✅ Se reflejan en navbar y perfil
✅ Breadcrumbs: Dashboard → Mi Perfil → Editar Perfil
```

**Test 9: Cambiar Contraseña**
```
Pasos:
1. Ir a /perfil/editar
2. Ingresar contraseña actual
3. Ingresar nueva contraseña (2 veces)
4. Click en "Cambiar Contraseña"

Resultado esperado:
✅ Mensaje de éxito
✅ Contraseña actualizada en BD
✅ Puede hacer login con nueva contraseña
```

**Test 10: Cambiar Contraseña - Error Contraseña Actual**
```
Pasos:
1. Ir a /perfil/editar
2. Ingresar contraseña actual INCORRECTA
3. Ingresar nueva contraseña
4. Click en "Cambiar Contraseña"

Resultado esperado:
✅ Mensaje de error "Contraseña actual incorrecta"
✅ Contraseña NO se actualiza
```

---

#### 👥 Módulo Clientes

**Test 11: Listar Clientes**
```
Pasos:
1. Navegar a /clientes

Resultado esperado:
✅ Se muestra tabla con clientes
✅ Breadcrumbs: Dashboard → Clientes
✅ Botón "Volver a Dashboard" funciona
✅ Botón "Agregar Cliente" abre modal
```

**Test 12: Agregar Cliente**
```
Pasos:
1. En /clientes, click en "Agregar Cliente"
2. Llenar formulario modal
3. Click en "Guardar"

Resultado esperado:
✅ Modal se cierra
✅ Cliente agregado a BD
✅ Tabla se actualiza con nuevo cliente
✅ Mensaje de éxito
```

**Test 13: Editar Cliente**
```
Pasos:
1. En tabla de clientes, click en ícono editar
2. Modificar datos en modal
3. Click en "Guardar"

Resultado esperado:
✅ Modal se cierra
✅ Cliente actualizado en BD
✅ Tabla se actualiza
✅ Mensaje de éxito
```

**Test 14: Buscar Cliente**
```
Pasos:
1. En /clientes, usar input de búsqueda
2. Escribir nombre/teléfono/email

Resultado esperado:
✅ Tabla filtra resultados en tiempo real
✅ Solo muestra clientes que coinciden
```

---

#### 📦 Módulo Productos

**Test 15: Listar Productos**
```
Pasos:
1. Navegar a /productos

Resultado esperado:
✅ Se muestra tabla con productos
✅ Breadcrumbs: Dashboard → Productos
✅ Botón "Volver a Dashboard" funciona
✅ Botón "Agregar Producto" abre modal
```

**Test 16: Agregar Producto**
```
Pasos:
1. En /productos, click en "Agregar Producto"
2. Llenar formulario modal
3. Click en "Guardar"

Resultado esperado:
✅ Modal se cierra
✅ Producto agregado a BD
✅ Tabla se actualiza
✅ Mensaje de éxito
```

**Test 17: Buscar Producto**
```
Pasos:
1. En /productos, usar input de búsqueda
2. Escribir código/descripción

Resultado esperado:
✅ Tabla filtra resultados en tiempo real
✅ Solo muestra productos que coinciden
```

---

#### 📄 Módulo Facturas

**Test 18: Listar Facturas**
```
Pasos:
1. Navegar a /facturas

Resultado esperado:
✅ Se muestra tabla con facturas
✅ Breadcrumbs: Dashboard → Facturas
✅ Botón "Volver a Dashboard" funciona
✅ Botón "Nueva Factura" abre modal
```

**Test 19: Editar Factura**
```
Pasos:
1. En tabla, click en ícono editar
2. Navega a /facturas/editar/{id}

Resultado esperado:
✅ Breadcrumbs: Dashboard → Facturas → Editar #ID
✅ Formulario carga datos de factura
✅ Botón "Volver a Facturas" funciona
✅ Se puede modificar fecha de entrega
✅ Se pueden agregar/eliminar productos
```

**Test 20: Filtrar Facturas**
```
Pasos:
1. En /facturas, usar filtros de fecha y estado
2. Click en "Filtrar"

Resultado esperado:
✅ Tabla muestra solo facturas que cumplen criterios
✅ Filtros se pueden limpiar
```

---

#### 🧭 Navegación y Breadcrumbs

**Test 21: Breadcrumbs 2 Niveles**
```
Vistas a probar:
- /clientes
- /productos  
- /facturas
- /perfil

Resultado esperado:
✅ Muestra "Dashboard → [Módulo]"
✅ Click en "Dashboard" navega a /dashboard
✅ Elemento activo no es clickeable
✅ Separador "/" visible
```

**Test 22: Breadcrumbs 3 Niveles**
```
Vistas a probar:
- /facturas/editar/{id}
- /perfil/editar

Resultado esperado:
✅ Muestra "Dashboard → [Módulo] → [Acción]"
✅ Click en "Dashboard" navega a /dashboard
✅ Click en nivel 2 navega a página lista
✅ Elemento activo no es clickeable
```

**Test 23: Navbar Dropdown**
```
Pasos:
1. Click en usuario en navbar
2. Observar dropdown

Resultado esperado:
✅ Dropdown se abre
✅ Muestra nombre y rol de usuario
✅ Opciones: Ver perfil, Editar perfil, Configuración (si ADMIN), Cerrar sesión
✅ Click fuera cierra dropdown
```

**Test 24: Sidebar**
```
Pasos:
1. Observar sidebar en diferentes vistas

Resultado esperado:
✅ Muestra módulos según rol
✅ Módulo activo destacado
✅ Iconos correctos por módulo
✅ Hover effect funciona
```

---

### 📊 Resumen Test 7.1

**Total de tests:** 24  
**Categorías:**
- Autenticación: 3 tests
- Dashboard: 3 tests
- Perfil: 4 tests
- Clientes: 4 tests
- Productos: 3 tests
- Facturas: 3 tests
- Navegación: 4 tests

---

## 📝 PUNTO 7.2: PRUEBAS DE ROLES

### Objetivo
Verificar que los permisos por rol funcionan correctamente.

### Roles del Sistema
- **ADMIN:** Acceso completo
- **USER:** Acceso a módulos operativos
- **CLIENTE:** Acceso limitado a sus propios datos

### Checklist de Pruebas

**Test 1: ADMIN - Acceso Completo**
```
Usuario: ADMIN
Módulos esperados en dashboard:
✅ Clientes
✅ Productos
✅ Facturas
✅ Reportes (deshabilitado pero visible)
✅ Configuración (visible en dropdown)
✅ Usuarios (si existe)
```

**Test 2: USER - Acceso Limitado**
```
Usuario: USER
Módulos esperados en dashboard:
✅ Clientes
✅ Productos
✅ Facturas
❌ Reportes (no visible o deshabilitado)
❌ Configuración (no visible en dropdown)
❌ Usuarios (no visible)
```

**Test 3: CLIENTE - Acceso Muy Limitado**
```
Usuario: CLIENTE
Módulos esperados:
✅ Sus propias facturas
✅ Su perfil
❌ Lista completa de clientes
❌ Productos (edición)
❌ Otras facturas
```

**Test 4: Rutas Protegidas - Sin Autenticación**
```
Sin login, intentar acceder:
- /dashboard → Redirect a /auth/login ✅
- /clientes → Redirect a /auth/login ✅
- /productos → Redirect a /auth/login ✅
- /facturas → Redirect a /auth/login ✅
- /perfil → Redirect a /auth/login ✅
```

**Test 5: Rutas Protegidas - Sin Permisos**
```
Usuario: USER
Intentar acceder a /configuracion → 403 Forbidden ✅

Usuario: CLIENTE  
Intentar acceder a /clientes → 403 Forbidden ✅
```

---

## 📝 PUNTO 7.3: PRUEBAS RESPONSIVE

### Objetivo
Verificar que el diseño se adapta correctamente a diferentes dispositivos.

### Breakpoints a Probar

- **Móvil:** < 576px
- **Tablet:** 768px - 991px
- **Desktop:** ≥ 992px

### Checklist de Pruebas

**Test 1: Móvil (< 576px)**
```
Elementos a verificar:
✅ Navbar: Logo visible, nombre oculto
✅ Sidebar: Colapsado por defecto
✅ Dashboard: Widgets apilados (1 columna)
✅ Módulos: Tarjetas apiladas (1 columna)
✅ Tablas: Scroll horizontal si necesario
✅ Breadcrumbs: Wrappean correctamente
✅ Botones: Stack verticalmente
```

**Test 2: Tablet (768px)**
```
Elementos a verificar:
✅ Navbar: Logo y nombre visibles
✅ Sidebar: Visible (puede colapsar)
✅ Dashboard: Widgets en 2-3 columnas
✅ Módulos: Tarjetas en 2-3 columnas
✅ Tablas: Responsive sin scroll horizontal
✅ Breadcrumbs: En una línea
```

**Test 3: Desktop (≥ 992px)**
```
Elementos a verificar:
✅ Navbar: Completo con breadcrumbs (ocultos)
✅ Sidebar: Siempre visible
✅ Dashboard: Widgets en 4-5 columnas
✅ Módulos: Tarjetas en 4-5 columnas
✅ Tablas: Full width sin scroll
✅ Breadcrumbs: Full width
```

---

## 📝 PUNTO 7.4: PRUEBAS DE NAVEGADORES

### Objetivo
Verificar compatibilidad cross-browser.

### Navegadores a Probar

**Test 1: Google Chrome (Latest)**
```
✅ Renderizado correcto
✅ JavaScript funciona
✅ CSS aplicado correctamente
✅ Iconos Font Awesome visibles
✅ Animaciones suaves
```

**Test 2: Mozilla Firefox (Latest)**
```
✅ Renderizado correcto
✅ JavaScript funciona
✅ CSS aplicado correctamente
✅ Iconos Font Awesome visibles
✅ Animaciones suaves
```

**Test 3: Microsoft Edge (Latest)**
```
✅ Renderizado correcto
✅ JavaScript funciona
✅ CSS aplicado correctamente
✅ Iconos Font Awesome visibles
✅ Animaciones suaves
```

**Test 4: Safari (Si es posible)**
```
✅ Renderizado correcto
✅ JavaScript funciona
✅ CSS aplicado correctamente
✅ Iconos Font Awesome visibles
✅ Animaciones suaves
```

---

## 📝 PUNTO 7.5: VALIDACIÓN DE ACCESIBILIDAD

### Objetivo
Verificar cumplimiento de WCAG 2.1 nivel AA.

### Checklist de Pruebas

**Test 1: Alt Text en Imágenes/Iconos**
```
✅ Iconos decorativos tienen aria-hidden="true"
✅ Iconos funcionales tienen aria-label
✅ Imágenes tienen alt text descriptivo
```

**Test 2: Labels en Formularios**
```
✅ Todos los inputs tienen <label> asociado
✅ Labels descriptivos y claros
✅ Required fields indicados
✅ Placeholders no reemplazan labels
```

**Test 3: Contraste de Colores**
```
✅ Texto principal: 8:1 (AAA)
✅ Texto secundario: 5:1 (AA)
✅ Enlaces: 4.5:1 (AA)
✅ Breadcrumbs: 4.5:1 (AA)
✅ Botones: 4.5:1 (AA)
```

**Test 4: Navegación por Teclado**
```
✅ Tab navega por todos los elementos interactivos
✅ Enter activa botones y enlaces
✅ Esc cierra modales y dropdowns
✅ Foco visible en todos los elementos
✅ No hay trampas de foco
```

**Test 5: ARIA Attributes**
```
✅ aria-label en breadcrumbs
✅ aria-current="page" en elemento activo
✅ aria-hidden en iconos decorativos
✅ role="alert" en mensajes de error/éxito
✅ aria-expanded en dropdowns
```

---

## 📋 FORMATO DE REPORTE

Para cada test ejecutado, documentar:

```markdown
### Test X: [Nombre del Test]
**Fecha:** DD/MM/YYYY  
**Ejecutado por:** [Nombre]  
**Navegador:** [Chrome/Firefox/etc]  
**Resolución:** [1920x1080/etc]

**Pasos realizados:**
1. [Paso 1]
2. [Paso 2]
...

**Resultado:**
- [ ] ✅ PASS
- [ ] ❌ FAIL

**Observaciones:**
[Cualquier nota relevante]

**Evidencia:**
[Captura de pantalla si es necesario]
```

---

## ✅ CRITERIOS DE APROBACIÓN

Para considerar la Fase 7 como **COMPLETADA**, se debe cumplir:

- ✅ 7.1: Mínimo 90% de tests funcionales PASS
- ✅ 7.2: 100% de tests de roles PASS
- ✅ 7.3: 100% de breakpoints funcionan correctamente
- ✅ 7.4: Funciona en al menos 3 navegadores principales
- ✅ 7.5: Cumple WCAG 2.1 nivel AA en contraste y navegación

---

## 📊 PROGRESO

| Punto | Tests Totales | Tests PASS | Tests FAIL | % Completitud |
|-------|---------------|------------|------------|---------------|
| 7.1   | 24            | 0          | 0          | 0%            |
| 7.2   | 5             | 0          | 0          | 0%            |
| 7.3   | 3             | 0          | 0          | 0%            |
| 7.4   | 4             | 0          | 0          | 0%            |
| 7.5   | 5             | 0          | 0          | 0%            |
| **TOTAL** | **41**    | **0**      | **0**      | **0%**        |

---

**Próximos pasos:**
1. Ejecutar punto 7.1 (Pruebas funcionales)
2. Documentar resultados
3. Corregir bugs encontrados
4. Continuar con puntos 7.2-7.5

---

**Fecha de inicio:** 12 de octubre de 2025  
**Estado:** 🔄 EN PROGRESO
