## 📊 FLUJO DE RESTRICCIONES POR ROL

### ADMIN (Sin restricciones)
```
Login como ADMIN
    ↓
Accede a cualquier módulo
    ↓
Ve todos los botones de acción
    ↓
Puede crear, editar, eliminar todo
    ↓
Sin badges de restricción en la UI
```

### USER (Restricciones mínimas)
```
Login como USER
    ↓
Accede a módulos operativos
    ↓
Ve todos los botones de acción (excepto Configuración/Usuarios)
    ↓
Puede crear, editar, eliminar en módulos permitidos
    ↓
Sin badges de restricción en la UI
```

### VENDEDOR (Puede crear facturas)
```
Login como VENDEDOR
    ↓
Accede a Clientes, Productos, Facturas
    ↓
Clientes/Productos:
    - NO ve botones Agregar/Editar/Eliminar
    - Ve badge "Solo lectura"
    ↓
Facturas:
    - VE botón "Nueva Factura" ✅
    - VE botón "Ver Detalle" ✅
    - NO ve botón "Eliminar" ❌
    - Ve badge "Solo lectura" (porque no puede eliminar)
    ↓
Sidebar:
    - Badge verde en Facturas (puede crear)
    - Badge gris en Clientes y Productos
```

### VISUALIZADOR (Solo lectura total)
```
Login como VISUALIZADOR
    ↓
Accede a Clientes, Productos, Facturas
    ↓
Todos los módulos:
    - NO ve botones Agregar/Editar/Eliminar
    - Solo ve botón "Ver Detalle" en Facturas
    - Ve badge "Solo lectura" en todos los módulos
    ↓
Sidebar:
    - Badge gris en todos los módulos
    - Sin opciones de creación
```

---

