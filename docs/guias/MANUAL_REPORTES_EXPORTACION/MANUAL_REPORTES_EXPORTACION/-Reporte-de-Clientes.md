## 👥 Reporte de Clientes

### Acceso al Reporte

1. Dashboard de reportes → **"Reporte de Clientes"**
2. URL: `/reportes/clientes`

### Filtros Disponibles

| Filtro | Opciones | Descripción |
|--------|----------|-------------|
| **Estado** | Activo / Inactivo / Todos | Filtrar por estado del cliente |
| **Con Deuda** | Sí / No / Todos | Clientes con facturas pendientes |

### Datos Mostrados

#### Tabla de Clientes

| Columna | Descripción |
|---------|-------------|
| **Nombre** | Razón social o nombre completo |
| **RUC/DNI** | Documento de identidad |
| **Email** | Correo electrónico |
| **Teléfono** | Número de contacto |
| **Total Compras** | Suma de todas las facturas |
| **Deuda Pendiente** | Facturas sin pagar |
| **Última Compra** | Fecha de última factura |
| **Estado** | Activo / Inactivo |

#### Estadísticas de Clientes

```
┌─────────────────────────────────────────────────┐
│  RESUMEN DE CLIENTES                           │
├─────────────────────────────────────────────────┤
│  Total Clientes:            245                │
│  Clientes Activos:          198  (80.8%)       │
│  Clientes Inactivos:         47  (19.2%)       │
│                                                 │
│  Clientes con Deuda:         58  (23.7%)       │
│  Deuda Total:          S/ 45,230.00            │
│                                                 │
│  Top 5 Mejores Clientes:                       │
│  1. Cliente ABC       S/ 25,400.00             │
│  2. Cliente XYZ       S/ 18,750.00             │
│  3. Cliente 123       S/ 15,320.00             │
│  4. Cliente QWE       S/ 12,890.00             │
│  5. Cliente RTY       S/ 11,240.00             │
└─────────────────────────────────────────────────┘
```

### Gráfica de Top Clientes

Gráfico de barras horizontal mostrando los 10 clientes con mayor volumen de compras.

### Exportación del Reporte de Clientes

Formatos disponibles:
- 📄 **PDF** - Documento formateado con estadísticas
- 📗 **Excel** - Datos estructurados en tablas
- 📊 **CSV** - Para importación a otros sistemas

Funcionan igual que en el reporte de ventas.

---

