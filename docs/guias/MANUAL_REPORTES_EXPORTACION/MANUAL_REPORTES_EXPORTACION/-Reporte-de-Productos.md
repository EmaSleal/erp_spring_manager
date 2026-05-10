## 📦 Reporte de Productos

### Acceso al Reporte

1. Dashboard de reportes → **"Reporte de Productos"**
2. URL: `/reportes/productos`

### Filtros Disponibles

| Filtro | Descripción |
|--------|-------------|
| **Stock Bajo** | Productos con cantidad menor al mínimo |
| **Sin Ventas** | Productos que no se han vendido |

### Datos Mostrados

#### Tabla de Productos

| Columna | Descripción |
|---------|-------------|
| **Código** | Código SKU del producto |
| **Nombre** | Descripción del producto |
| **Categoría** | Categoría o familia |
| **Precio** | Precio de venta unitario |
| **Stock Actual** | Cantidad disponible |
| **Cantidad Vendida** | Total unidades vendidas |
| **Total Vendido** | Monto total de ventas |
| **Estado Stock** | NORMAL / BAJO / CRÍTICO |

#### Estadísticas de Productos

```
┌─────────────────────────────────────────────────┐
│  RESUMEN DE PRODUCTOS                          │
├─────────────────────────────────────────────────┤
│  Total Productos:           350                │
│  En Stock:                  322  (92.0%)       │
│  Stock Bajo:                 23  ( 6.6%)       │
│  Sin Stock:                   5  ( 1.4%)       │
│                                                 │
│  Productos Activos:         330  (94.3%)       │
│  Productos Inactivos:        20  ( 5.7%)       │
│                                                 │
│  Valor Total Inventario: S/ 285,420.00         │
│                                                 │
│  Top 5 Más Vendidos:                           │
│  1. Producto A         1,250 unidades          │
│  2. Producto B           980 unidades          │
│  3. Producto C           875 unidades          │
│  4. Producto D           720 unidades          │
│  5. Producto E           650 unidades          │
└─────────────────────────────────────────────────┘
```

### Gráfica de Productos Más Vendidos

Gráfico de barras mostrando los 10 productos con mayor cantidad vendida.

### Alertas de Stock

El sistema resalta visualmente:

| Estado | Color | Condición |
|--------|-------|-----------|
| **CRÍTICO** | 🔴 Rojo | Stock = 0 |
| **BAJO** | 🟡 Amarillo | Stock < Stock Mínimo |
| **NORMAL** | 🟢 Verde | Stock >= Stock Mínimo |

---

