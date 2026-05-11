## 📝 PUNTO 7.1: PRUEBAS FUNCIONALES

### Objetivo
Verificar que todas las funcionalidades implementadas funcionan como se espera.

### Checklist de Pruebas

#### 🔐 Autenticación y Sesión

**Test 1: Login Exitoso**
```
Pasos:
1. Ir a http://localhost:9090/auth/login
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
1. Ir a http://localhost:9090/auth/login
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

