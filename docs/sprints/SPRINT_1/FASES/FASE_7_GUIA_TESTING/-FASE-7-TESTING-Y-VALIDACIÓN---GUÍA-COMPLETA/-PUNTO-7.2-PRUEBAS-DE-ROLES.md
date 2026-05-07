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

