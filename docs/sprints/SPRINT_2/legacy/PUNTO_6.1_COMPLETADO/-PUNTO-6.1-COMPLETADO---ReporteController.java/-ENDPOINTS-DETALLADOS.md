## 📊 ENDPOINTS DETALLADOS

### 1️⃣ Dashboard de Reportes

```java
@GetMapping
public String index(Model model, Authentication authentication)
```

**Ruta:** `GET /reportes`  
**Descripción:** Muestra la página principal de reportes con cards de acceso rápido  
**Retorna:** Vista `reportes/index.html`

**Datos cargados:**
- `totalClientes`: Total de clientes registrados
- `totalProductos`: Total de productos en el catálogo
- `totalFacturas`: Total de facturas emitidas
- `totalUsuarios`: Total de usuarios del sistema

**Uso:**
```
http://localhost:9090/reportes
```

---

### 2️⃣ Reporte de Ventas

```java
@GetMapping("/ventas")
public String reporteVentas(
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
    @RequestParam(required = false) Integer clienteId,
    Model model,
    Authentication authentication)
```

**Ruta:** `GET /reportes/ventas`  
**Descripción:** Genera reporte de ventas con filtros opcionales  
**Retorna:** Vista `reportes/ventas.html`

**Parámetros opcionales:**
- `fechaInicio`: Fecha de inicio del período (formato ISO: YYYY-MM-DD)
- `fechaFin`: Fecha de fin del período
- `clienteId`: ID del cliente para filtrar

**Datos cargados:**
- `facturas`: Lista de facturas (filtradas o todas)
- `clientes`: Lista de clientes para el filtro
- `fechaInicio` y `fechaFin`: Fechas del filtro
- `totalVentas`: Total de ventas en el período
- `cantidadFacturas`: Cantidad de facturas
- `ticketPromedio`: Promedio por factura

**Valores por defecto:**
- Si no se especifica `fechaInicio`: Primer día del mes actual
- Si no se especifica `fechaFin`: Día actual

**Uso:**
```
http://localhost:9090/reportes/ventas
http://localhost:9090/reportes/ventas?fechaInicio=2025-10-01&fechaFin=2025-10-18
http://localhost:9090/reportes/ventas?clienteId=5
```

---

### 3️⃣ Reporte de Clientes

```java
@GetMapping("/clientes")
public String reporteClientes(
    @RequestParam(required = false) Boolean activo,
    @RequestParam(required = false) Boolean conDeuda,
    Model model,
    Authentication authentication)
```

**Ruta:** `GET /reportes/clientes`  
**Descripción:** Genera reporte de clientes con estadísticas  
**Retorna:** Vista `reportes/clientes.html`

**Parámetros opcionales:**
- `activo`: Filtrar por estado activo/inactivo (true/false)
- `conDeuda`: Filtrar clientes con deuda pendiente (true/false)

**Datos cargados:**
- `clientes`: Lista de clientes (filtrados o todos)
- `totalClientes`: Total de clientes
- `clientesActivos`: Cantidad de clientes activos
- `clientesConDeuda`: Cantidad con deuda pendiente
- `clientesNuevosEsteMes`: Clientes nuevos en el mes

**Uso:**
```
http://localhost:9090/reportes/clientes
http://localhost:9090/reportes/clientes?activo=true
http://localhost:9090/reportes/clientes?conDeuda=true
```

---

### 4️⃣ Reporte de Productos

```java
@GetMapping("/productos")
public String reporteProductos(
    @RequestParam(required = false) Boolean stockBajo,
    @RequestParam(required = false) Boolean sinVentas,
    Model model,
    Authentication authentication)
```

**Ruta:** `GET /reportes/productos`  
**Descripción:** Genera reporte de productos más vendidos y stock  
**Retorna:** Vista `reportes/productos.html`

**Parámetros opcionales:**
- `stockBajo`: Filtrar productos con stock bajo (true/false)
- `sinVentas`: Filtrar productos sin ventas (true/false)

**Datos cargados:**
- `productos`: Lista de productos (filtrados o todos)
- `totalProductos`: Total de productos
- `productosActivos`: Productos activos
- `productosStockBajo`: Productos con stock bajo
- `productosSinStock`: Productos sin stock

**Uso:**
```
http://localhost:9090/reportes/productos
http://localhost:9090/reportes/productos?stockBajo=true
http://localhost:9090/reportes/productos?sinVentas=true
```

---

### 5️⃣ Exportar a PDF

```java
@GetMapping("/export/pdf")
@ResponseBody
public ResponseEntity<byte[]> exportarPDF(
    @RequestParam String tipo,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin)
```

**Ruta:** `GET /reportes/export/pdf`  
**Descripción:** Exporta un reporte a formato PDF  
**Retorna:** Archivo PDF (`application/pdf`)

**Parámetros:**
- `tipo` (requerido): Tipo de reporte (ventas, clientes, productos)
- `fechaInicio` (opcional): Fecha inicio para reporte de ventas
- `fechaFin` (opcional): Fecha fin para reporte de ventas

**Headers de respuesta:**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="reporte_ventas.pdf"
```

**Uso:**
```
http://localhost:9090/reportes/export/pdf?tipo=ventas&fechaInicio=2025-10-01&fechaFin=2025-10-18
http://localhost:9090/reportes/export/pdf?tipo=clientes
http://localhost:9090/reportes/export/pdf?tipo=productos
```

**Estado actual:** ⏳ Endpoint creado, implementación de PDF pendiente (punto 6.4.1)

---

### 6️⃣ Exportar a Excel

```java
@GetMapping("/export/excel")
@ResponseBody
public ResponseEntity<byte[]> exportarExcel(
    @RequestParam String tipo,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin)
```

**Ruta:** `GET /reportes/export/excel`  
**Descripción:** Exporta un reporte a formato Excel  
**Retorna:** Archivo Excel (`.xlsx`)

**Parámetros:**
- `tipo` (requerido): Tipo de reporte (ventas, clientes, productos)
- `fechaInicio` (opcional): Fecha inicio para reporte de ventas
- `fechaFin` (opcional): Fecha fin para reporte de ventas

**Headers de respuesta:**
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="reporte_ventas.xlsx"
```

**Uso:**
```
http://localhost:9090/reportes/export/excel?tipo=ventas&fechaInicio=2025-10-01&fechaFin=2025-10-18
http://localhost:9090/reportes/export/excel?tipo=clientes
http://localhost:9090/reportes/export/excel?tipo=productos
```

**Estado actual:** ⏳ Endpoint creado, implementación de Excel pendiente (punto 6.4.2)

---

