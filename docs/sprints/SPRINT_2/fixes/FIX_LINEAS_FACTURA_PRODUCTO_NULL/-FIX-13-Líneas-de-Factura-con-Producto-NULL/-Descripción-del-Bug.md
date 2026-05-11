## 📋 Descripción del Bug

### Problema Reportado

Al intentar guardar una factura con líneas donde **no se seleccionó ningún producto**, el sistema lanzaba un error de constraint de base de datos:

```
Column 'id_producto' cannot be null
java.sql.SQLIntegrityConstraintViolationException: Column 'id_producto' cannot be null
```

### Contexto

El usuario agregaba una línea nueva usando el botón "Agregar línea", pero **no modificaba ningún campo** (no seleccionaba producto, cantidad, etc.). Al intentar guardar la factura, el sistema intentaba insertar esa línea vacía con `id_producto = null`, violando la constraint de la base de datos.

### Evidencia del Error

**Log del error:**

```
2025-10-20T12:08:47.310-06:00  INFO 17248 --- [nio-9090-exec-7] a.a.w.s.impl.LineaFacturaServiceImpl     : Actualizando líneas: [
  LineaFacturaR[
    id_linea_factura=null, 
    numero_linea=1, 
    id_producto=null,    <-- ❌ PROBLEMA AQUÍ
    id_factura=26, 
    descripcion=null, 
    cantidad=1, 
    precioUnitario=0, 
    subtotal=0, 
    ...
  ]
]

Hibernate: call sp_actualizar_linea_factura(?, ?, ?, ?, ?, ?, ?, ?)

2025-10-20T12:08:47.679-06:00  WARN 17248 --- [nio-9090-exec-7] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 1048, SQLState: 23000
2025-10-20T12:08:47.679-06:00 ERROR 17248 --- [nio-9090-exec-7] o.h.engine.jdbc.spi.SqlExceptionHelper   : Column 'id_producto' cannot be null
```

**Captura del comportamiento:**

La imagen muestra cómo el usuario agregó una línea (línea #1) pero no seleccionó ningún producto, dejando el campo vacío/nulo.

---

