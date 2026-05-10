## 🧪 CASOS DE PRUEBA VISUALES

### Test 1: ADMIN - Sin restricciones visuales
```
Login: admin@example.com (ADMIN)
Navegar a:
1. /clientes
   → ✅ Ve botón "Agregar Cliente"
   → ✅ Ve botones "Editar" y "Eliminar" en tabla
   → ❌ NO ve badge "Solo lectura"

2. /productos
   → ✅ Ve botón "Agregar Producto"
   → ✅ Ve botones "Editar" y "Eliminar" en tabla
   → ❌ NO ve badge "Solo lectura"

3. /facturas
   → ✅ Ve botón "Nueva Factura"
   → ✅ Ve botones "Ver Detalle" y "Eliminar"
   → ❌ NO ve badge "Solo lectura"

4. Sidebar
   → ❌ Sin badges en módulos
```

### Test 2: USER - Sin restricciones en módulos permitidos
```
Login: user@example.com (USER)
Resultado idéntico a ADMIN en módulos operativos
Diferencia: NO ve Configuración ni Usuarios en sidebar
```

### Test 3: VENDEDOR - Puede crear facturas
```
Login: vendedor@example.com (VENDEDOR)
Navegar a:
1. /clientes
   → ❌ NO ve botón "Agregar Cliente"
   → ❌ NO ve botones "Editar" ni "Eliminar"
   → ✅ Ve badge "Solo lectura" en cada fila

2. /productos
   → ❌ NO ve botón "Agregar Producto"
   → ❌ NO ve botones "Editar" ni "Eliminar"
   → ✅ Ve badge "Solo lectura" en cada fila

3. /facturas
   → ✅ VE botón "Nueva Factura" (puede crear)
   → ✅ Ve botón "Ver Detalle"
   → ❌ NO ve botón "Eliminar"
   → ✅ Ve badge "Solo lectura" (porque no puede eliminar)

4. Sidebar
   → ✅ Badge verde (plus) en Facturas
   → ✅ Badge gris (ojo) en Clientes y Productos
```

### Test 4: VISUALIZADOR - Solo lectura total
```
Login: visualizador@example.com (VISUALIZADOR)
Navegar a:
1. /clientes
   → ❌ NO ve botón "Agregar Cliente"
   → ❌ NO ve botones "Editar" ni "Eliminar"
   → ✅ Ve badge "Solo lectura" en cada fila

2. /productos
   → ❌ NO ve botón "Agregar Producto"
   → ❌ NO ve botones "Editar" ni "Eliminar"
   → ✅ Ve badge "Solo lectura" en cada fila

3. /facturas
   → ❌ NO ve botón "Nueva Factura"
   → ✅ Ve botón "Ver Detalle" (solo)
   → ❌ NO ve botón "Eliminar"
   → ✅ Ve badge "Solo lectura" en cada fila

4. Sidebar
   → ✅ Badge gris (ojo) en todos los módulos
```

---

