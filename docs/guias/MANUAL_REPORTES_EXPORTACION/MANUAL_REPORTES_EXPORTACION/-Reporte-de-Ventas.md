## 📈 Reporte de Ventas

### Acceso al Reporte

1. En el dashboard de reportes, haga clic en **"Reporte de Ventas"**
2. O navegue a: `/reportes/ventas`

### Filtros Disponibles

#### 1. Filtro por Fecha

| Campo | Descripción | Formato |
|-------|-------------|---------|
| **Fecha Inicio** | Desde qué fecha buscar | dd/mm/yyyy |
| **Fecha Fin** | Hasta qué fecha buscar | dd/mm/yyyy |

**Ejemplos de uso:**
```
Ventas del mes actual:
  Inicio: 01/01/2026
  Fin:    31/01/2026

Ventas del último año:
  Inicio: 01/01/2025
  Fin:    31/12/2025
```

#### 2. Filtro por Cliente

- Seleccione un cliente específico del dropdown
- O deje en blanco para ver todos los clientes

### Datos Mostrados

#### Tabla de Facturas

| Columna | Descripción |
|---------|-------------|
| **N° Factura** | Número completo de la factura |
| **Cliente** | Nombre del cliente |
| **Fecha** | Fecha de emisión |
| **Subtotal** | Monto sin IGV |
| **IGV** | Impuesto (18%) |
| **Total** | Monto final |
| **Estado** | PAGADO / PENDIENTE / VENCIDO |
| **Acciones** | Ver detalles, Exportar |

#### Estadísticas Resumen

```
┌─────────────────────────────────────────────────┐
│  RESUMEN DE VENTAS                             │
├─────────────────────────────────────────────────┤
│  Total Facturas:        125                    │
│  Subtotal:         S/ 105,250.00               │
│  IGV (18%):        S/  18,945.00               │
│  Total Ventas:     S/ 124,195.00               │
│                                                 │
│  Facturas Pagadas:      98  (78.4%)           │
│  Facturas Pendientes:   22  (17.6%)           │
│  Facturas Vencidas:      5  ( 4.0%)           │
└─────────────────────────────────────────────────┘
```

### Gráfica de Ventas Mensuales

El sistema genera automáticamente un gráfico de líneas mostrando:
- Eje X: Meses del año
- Eje Y: Total de ventas en soles
- Línea: Tendencia de ventas

**Interactividad:**
- Pase el mouse sobre un punto para ver detalles
- La gráfica es responsive (se adapta al tamaño de pantalla)

### Exportación del Reporte de Ventas

#### Exportar a PDF

1. Haga clic en el botón **"Exportar PDF"** 📄
2. El archivo se descargará automáticamente
3. Nombre del archivo: `reporte-ventas.pdf`

**Contenido del PDF:**
- Encabezado con logo de la empresa
- Filtros aplicados
- Tabla de facturas
- Resumen de estadísticas
- Pie de página con fecha de generación

#### Exportar a Excel

1. Haga clic en el botón **"Exportar Excel"** 📗
2. Se descargará un archivo `.xlsx`
3. Nombre del archivo: `reporte-ventas.xlsx`

**Contenido del Excel:**
- Hoja 1: Datos de facturas (formato tabla)
- Hoja 2: Resumen de estadísticas
- Con formato y estilos corporativos

#### Exportar a CSV

1. Haga clic en el botón **"Exportar CSV"** 📊
2. Se descargará un archivo `.csv`
3. Nombre del archivo: `reporte-ventas.csv`

**Uso del CSV:**
- Importable a cualquier hoja de cálculo
- Compatible con software de contabilidad
- Formato estándar separado por comas

### Procedimiento Completo: Generar Reporte de Ventas

#### Ejemplo Práctico

**Objetivo:** Generar reporte de ventas del mes de diciembre 2025

1. **Acceder al módulo**
   - Clic en "Reportes" en el menú

2. **Seleccionar tipo**
   - Clic en "Reporte de Ventas"

3. **Aplicar filtros**
   ```
   Fecha Inicio: 01/12/2025
   Fecha Fin:    31/12/2025
   Cliente:      [Todos los clientes]
   ```

4. **Generar reporte**
   - Clic en botón "Buscar" o "Generar"

5. **Revisar resultados**
   - Verificar tabla de facturas
   - Analizar estadísticas
   - Revisar gráfica

6. **Exportar (opcional)**
   - Seleccionar formato deseado
   - Clic en botón de exportación
   - Guardar archivo descargado

---

