## 📦 FASE 6: REPORTES Y ESTADÍSTICAS

### Objetivo
Generar reportes básicos de ventas y estadísticas.

### Tareas

#### 6.1 Controller y Vistas
**Archivo:** `ReporteController.java`

**Endpoints:**
```java
GET /reportes                      → Página principal
GET /reportes/ventas               → Reporte de ventas
GET /reportes/clientes             → Reporte de clientes
GET /reportes/productos            → Reporte de productos
GET /reportes/export/pdf           → Exportar a PDF
GET /reportes/export/excel         → Exportar a Excel
```

#### 6.2 Tipos de Reportes

**1. Reporte de Ventas**
- Total de ventas por período (día, semana, mes, año)
- Facturas pagadas vs pendientes
- Gráfico de ventas en el tiempo
- Top 10 clientes
- Métodos de pago más usados

**2. Reporte de Clientes**
- Total de clientes registrados
- Clientes nuevos por período
- Clientes con deuda pendiente
- Clientes más frecuentes

**3. Reporte de Productos**
- Productos más vendidos
- Stock bajo (alertas)
- Productos sin ventas
- Valor total del inventario

#### 6.3 Filtros
- Rango de fechas (desde - hasta)
- Cliente específico
- Producto específico
- Estado de factura (pagado, pendiente)
- Método de pago

#### 6.4 Exportación
- **PDF**: Usando iText o similar
- **Excel**: Usando Apache POI
- **CSV**: Export simple

---

