# 📊 Manual de Usuario - Reportes y Exportación

**Versión:** 1.0  
**Fecha:** 4 de enero de 2026  
**Audiencia:** Administradores y Usuarios del sistema  
**Nivel de acceso requerido:** ROL_ADMIN o ROL_USER

---

## 📑 Tabla de Contenidos

1. [Introducción](#introducción)
2. [Acceso al Módulo de Reportes](#acceso-al-módulo-de-reportes)
3. [Tipos de Reportes](#tipos-de-reportes)
4. [Reporte de Ventas](#reporte-de-ventas)
5. [Reporte de Clientes](#reporte-de-clientes)
6. [Reporte de Productos](#reporte-de-productos)
7. [Gráficas y Visualizaciones](#gráficas-y-visualizaciones)
8. [Exportación de Datos](#exportación-de-datos)
9. [Filtros y Búsqueda](#filtros-y-búsqueda)
10. [Solución de Problemas](#solución-de-problemas)
11. [Preguntas Frecuentes](#preguntas-frecuentes)

---

## 📖 Introducción

El **Módulo de Reportes** le permite generar análisis detallados de su negocio, con opciones de filtrado avanzado y exportación en múltiples formatos.

### Funcionalidades Principales

- ✅ **Reportes de Ventas** - Análisis de facturas y transacciones
- ✅ **Reportes de Clientes** - Estadísticas de clientes y deudas
- ✅ **Reportes de Productos** - Productos más vendidos y stock
- ✅ **Gráficas Interactivas** - Visualización con Chart.js
- ✅ **Exportación** - PDF, Excel y CSV
- ✅ **Filtros Avanzados** - Por fecha, cliente, estado, etc.

### ⚠️ Requisitos de Acceso

- **Roles permitidos:** ADMIN, USER
- **Permisos necesarios:** 
  - `REPORTES_VER`
  - `REPORTES_EXPORTAR` (para exportación)

---

## 🔐 Acceso al Módulo de Reportes

### Paso 1: Navegar al Módulo

**Opción 1: Desde el Menú Principal**
1. En la barra lateral izquierda, localice **"Reportes"**
2. Haga clic en el ícono de gráfica 📊
3. Será redirigido al dashboard de reportes

**Opción 2: URL Directa**
```
https://tu-dominio.com/reportes
```

### Dashboard de Reportes

Al acceder, verá el dashboard principal con accesos rápidos:

```
┌────────────────────────────────────────────────────────┐
│           DASHBOARD DE REPORTES                        │
├────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │ 📈 Ventas│  │ 👥 Cli.  │  │ 📦 Prod. │            │
│  │  Ver →   │  │  Ver →   │  │  Ver →   │            │
│  └──────────┘  └──────────┘  └──────────┘            │
│                                                         │
│  [Gráficas en tiempo real]                            │
│                                                         │
└────────────────────────────────────────────────────────┘
```

---

## 📊 Tipos de Reportes

### 1. Reporte de Ventas 📈

**¿Qué muestra?**
- Lista de todas las facturas
- Total de ventas por período
- IGV recaudado
- Facturas pendientes vs pagadas
- Gráfica de ventas mensuales

**¿Cuándo usarlo?**
- Análisis de ingresos
- Revisión de facturación
- Cálculo de impuestos
- Seguimiento de cobros

### 2. Reporte de Clientes 👥

**¿Qué muestra?**
- Lista de clientes
- Total de compras por cliente
- Deuda pendiente
- Clientes frecuentes
- Estado (activo/inactivo)

**¿Cuándo usarlo?**
- Identificar mejores clientes
- Gestión de cobranza
- Análisis de cartera
- Segmentación de clientes

### 3. Reporte de Productos 📦

**¿Qué muestra?**
- Productos más vendidos
- Stock disponible
- Productos con stock bajo
- Productos sin movimiento
- Valor del inventario

**¿Cuándo usarlo?**
- Control de inventario
- Planificación de compras
- Análisis de rotación
- Identificar productos estrella

---

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

## 📊 Gráficas y Visualizaciones

### Tipos de Gráficas

#### 1. Gráfica de Ventas Mensuales (Líneas)

**Ubicación:** Reporte de Ventas

**Características:**
- Muestra tendencia de ventas a lo largo del tiempo
- Eje X: Meses del año
- Eje Y: Total en soles
- Interactiva (hover para detalles)

**Interpretación:**
```
Alta tendencia ascendente → Negocio en crecimiento
Línea horizontal → Ventas estables
Tendencia descendente → Requiere análisis
```

#### 2. Gráfica de Estado de Facturas (Torta/Pie)

**Ubicación:** Dashboard de Reportes

**Muestra:**
- % de facturas pagadas
- % de facturas pendientes
- % de facturas vencidas

**Colores:**
- 🟢 Verde: Pagadas
- 🟡 Amarillo: Pendientes
- 🔴 Rojo: Vencidas

#### 3. Gráfica de Top Productos (Barras)

**Ubicación:** Reporte de Productos

**Muestra:**
- Los 10 productos más vendidos
- Cantidad de unidades vendidas por producto
- Permite identificar productos estrella

#### 4. Gráfica de Top Clientes (Barras Horizontales)

**Ubicación:** Reporte de Clientes

**Muestra:**
- Los 10 mejores clientes
- Total de compras en soles
- Útil para segmentación

### Interactividad de Gráficas

**Funciones disponibles:**

1. **Hover (pasar el mouse):** Ver valores exactos
2. **Click en leyenda:** Ocultar/mostrar serie
3. **Responsive:** Se adapta al tamaño de pantalla
4. **Tooltips:** Información detallada al pasar el mouse

---

## 📥 Exportación de Datos

### Formatos de Exportación

#### 1. PDF (Portable Document Format)

**Características:**
- Documento formateado profesionalmente
- Incluye logo de la empresa
- Con encabezados y pie de página
- Listo para imprimir o enviar

**Cuándo usar:**
- Presentaciones a clientes
- Reportes ejecutivos
- Documentación oficial
- Archivo para contabilidad

**Ejemplo de estructura PDF:**
```
┌──────────────────────────────────────────┐
│  [Logo Empresa]     REPORTE DE VENTAS   │
│                     Período: Dic 2025    │
├──────────────────────────────────────────┤
│                                           │
│  [Tabla de datos]                        │
│                                           │
│  [Resumen de estadísticas]               │
│                                           │
├──────────────────────────────────────────┤
│  Generado: 04/01/2026 15:30             │
│  Usuario: admin@empresa.com              │
└──────────────────────────────────────────┘
```

#### 2. Excel (XLSX)

**Características:**
- Formato de hoja de cálculo
- Múltiples hojas (datos + resumen)
- Con estilos y formato
- Editable y calculable

**Cuándo usar:**
- Análisis adicionales
- Cálculos personalizados
- Importación a otros sistemas
- Procesamiento de datos

**Estructura del archivo:**
```
Archivo: reporte-ventas.xlsx
├── Hoja 1: Datos
│   └── Tabla con todas las facturas
├── Hoja 2: Resumen
│   └── Estadísticas calculadas
└── Hoja 3: Gráficas (opcional)
```

#### 3. CSV (Comma Separated Values)

**Características:**
- Formato de texto plano
- Separado por comas
- Universal y ligero
- Compatible con todo software

**Cuándo usar:**
- Importación a software contable
- Procesamiento masivo de datos
- Integración con otros sistemas
- Análisis con herramientas externas

**Ejemplo de contenido:**
```csv
Numero,Cliente,Fecha,Subtotal,IGV,Total,Estado
F001-00125,Cliente ABC,01/12/2025,1000.00,180.00,1180.00,PAGADO
F001-00126,Cliente XYZ,02/12/2025,2500.00,450.00,2950.00,PENDIENTE
...
```

### Procedimiento de Exportación

#### Paso a Paso

1. **Generar el reporte**
   - Aplique los filtros deseados
   - Verifique que los datos sean correctos

2. **Seleccionar formato**
   - Identifique cuál formato necesita
   - PDF: Presentación formal
   - Excel: Análisis adicional
   - CSV: Integración con otros sistemas

3. **Hacer clic en el botón**
   - Botón "Exportar PDF" 📄
   - Botón "Exportar Excel" 📗
   - Botón "Exportar CSV" 📊

4. **Esperar descarga**
   - El navegador descargará automáticamente
   - Por defecto va a la carpeta "Descargas"

5. **Verificar archivo**
   - Abra el archivo descargado
   - Verifique que contenga los datos esperados

### Nombres de Archivos Exportados

| Reporte | PDF | Excel | CSV |
|---------|-----|-------|-----|
| Ventas | `reporte-ventas.pdf` | `reporte-ventas.xlsx` | `reporte-ventas.csv` |
| Clientes | `reporte-clientes.pdf` | `reporte-clientes.xlsx` | `reporte-clientes.csv` |
| Productos | `reporte-productos.pdf` | `reporte-productos.xlsx` | `reporte-productos.csv` |

### Tamaño de Archivos

Tamaños aproximados (pueden variar según cantidad de datos):

| Formato | Tamaño para 100 registros | Tamaño para 1,000 registros |
|---------|---------------------------|-----------------------------|
| **PDF** | 50-100 KB | 200-500 KB |
| **Excel** | 30-60 KB | 150-300 KB |
| **CSV** | 10-20 KB | 50-100 KB |

---

## 🔍 Filtros y Búsqueda

### Tipos de Filtros

#### 1. Filtros por Fecha

**Disponible en:** Reporte de Ventas

**Opciones:**
- **Rango personalizado:** Fecha inicio + Fecha fin
- **Mes actual:** Automático al abrir
- **Mes anterior:** Un clic para seleccionar
- **Último año:** Análisis anual

**Ejemplo de uso:**
```
Ver ventas del primer trimestre 2025:
  Fecha Inicio: 01/01/2025
  Fecha Fin:    31/03/2025
```

#### 2. Filtros por Cliente

**Disponible en:** Reporte de Ventas

**Cómo funciona:**
1. Dropdown con lista de todos los clientes
2. Seleccione uno para ver solo sus facturas
3. O deje en blanco para ver todos

**Uso práctico:**
- Analizar compras de un cliente específico
- Verificar deuda de un cliente
- Generar estado de cuenta

#### 3. Filtros por Estado

**Disponible en:** Reporte de Clientes

**Opciones:**
- **Activos:** Clientes con compras recientes
- **Inactivos:** Sin compras en período largo
- **Todos:** Todos los clientes

**Criterio de "activo":**
- Cliente con al menos 1 compra en los últimos 6 meses

#### 4. Filtros por Deuda

**Disponible en:** Reporte de Clientes

**Opciones:**
- **Con deuda:** Clientes con facturas pendientes
- **Sin deuda:** Clientes al día
- **Todos:** Todos los clientes

**Uso práctico:**
- Gestión de cobranza
- Identificar cuentas por cobrar
- Seguimiento de pagos

#### 5. Filtros de Stock

**Disponible en:** Reporte de Productos

**Opciones:**
- **Stock bajo:** Productos debajo del mínimo
- **Sin ventas:** Productos sin movimiento
- **Todos:** Todo el inventario

**Alertas generadas:**
```
Stock Bajo detectado:
  - Producto A: 5 unidades (Mínimo: 20)
  - Producto B: 2 unidades (Mínimo: 10)
  
Requiere reabastecimiento urgente
```

### Combinación de Filtros

Puede combinar múltiples filtros para análisis específicos:

**Ejemplo 1:** Ventas de un cliente en diciembre
```
Tipo: Ventas
Fecha Inicio: 01/12/2025
Fecha Fin: 31/12/2025
Cliente: Cliente ABC
```

**Ejemplo 2:** Clientes activos con deuda
```
Tipo: Clientes
Estado: Activo
Con Deuda: Sí
```

**Ejemplo 3:** Productos con stock crítico
```
Tipo: Productos
Stock Bajo: Sí
Sin Ventas: No
```

### Limpiar Filtros

Para resetear todos los filtros:
1. Haga clic en el botón **"Limpiar Filtros"** 🔄
2. O recargue la página (F5)

---

## 🔧 Solución de Problemas

### Problema: El reporte no muestra datos

**Causas posibles:**
- Filtros muy restrictivos
- No hay datos en el período seleccionado
- Error de conexión a base de datos

**Solución:**
1. Verifique los filtros aplicados
2. Amplíe el rango de fechas
3. Limpie todos los filtros
4. Si persiste, contacte al administrador

### Problema: La exportación falla

**Mensaje:** "Error al generar el archivo"

**Causas:**
- Demasiados registros (>10,000)
- Problema de permisos
- Error del servidor

**Solución:**
1. Aplique filtros para reducir datos
2. Intente con un rango de fechas menor
3. Pruebe otro formato de exportación
4. Verifique que tiene permiso `REPORTES_EXPORTAR`

### Problema: Las gráficas no se muestran

**Causas:**
- JavaScript deshabilitado
- Bloqueador de contenido activo
- Error en Chart.js

**Solución:**
1. Habilite JavaScript en su navegador
2. Desactive bloqueadores temporalmente
3. Limpie caché del navegador (Ctrl+F5)
4. Pruebe en modo incógnito

### Problema: PDF se descarga vacío

**Causas:**
- No hay datos para exportar
- Error en generación de PDF
- Problema con iText library

**Solución:**
1. Verifique que el reporte tiene datos
2. Pruebe con menos registros
3. Intente exportar a Excel primero
4. Contacte a soporte técnico

### Problema: Excel no abre correctamente

**Mensaje:** "El archivo está corrupto"

**Causas:**
- Caracteres especiales en los datos
- Versión incompatible de Excel
- Descarga interrumpida

**Solución:**
1. Descargue nuevamente el archivo
2. Abra con Excel 2016 o superior
3. Pruebe abrir con Google Sheets
4. Exporte a CSV como alternativa

---

## ❓ Preguntas Frecuentes

### ¿Con qué frecuencia se actualizan los reportes?

Los reportes se generan **en tiempo real** cada vez que los solicita. Siempre verá los datos más actualizados de la base de datos.

### ¿Puedo programar reportes automáticos?

Actualmente no. Los reportes deben generarse manualmente. Esta funcionalidad está planificada para futuras versiones.

### ¿Cuál es el límite de registros que puedo exportar?

**Recomendaciones:**
- **PDF:** Hasta 1,000 registros (óptimo: 500)
- **Excel:** Hasta 10,000 registros (óptimo: 5,000)
- **CSV:** Sin límite práctico (pero puede tardar)

### ¿Los reportes incluyen datos eliminados?

No. Los reportes solo muestran registros activos en la base de datos. Los registros eliminados no aparecen.

### ¿Puedo compartir el reporte con mi contador?

Sí. Exporte el reporte a **PDF** o **Excel** y envíelo por email. El PDF es ideal para compartir ya que no puede ser modificado.

### ¿El reporte de ventas incluye notas de crédito?

Actualmente solo incluye facturas. Las notas de crédito estarán disponibles en futuras versiones.

### ¿Cómo interpreto la gráfica de ventas mensuales?

**Línea ascendente:** Crecimiento en ventas  
**Línea descendente:** Disminución en ventas  
**Línea plana:** Ventas estables  
**Picos:** Meses con ventas extraordinarias (ej: campaña)

### ¿Puedo ver reportes de años anteriores?

Sí. En el filtro de fechas, seleccione el rango del año que desee consultar. Los datos se conservan indefinidamente.

### ¿Qué significa "Stock Bajo"?

Un producto tiene "Stock Bajo" cuando su cantidad disponible es **menor** al **Stock Mínimo** configurado en el producto.

### ¿Los reportes consideran descuentos?

Sí. Los montos mostrados incluyen todos los descuentos aplicados a las facturas.

### ¿Puedo ver cuánto me deben en total?

Sí. En el **Reporte de Clientes**, active el filtro "Con Deuda". El resumen mostrará la "Deuda Total" de todos los clientes.

### ¿Las gráficas se incluyen en el PDF?

Actualmente no. Los PDF incluyen tablas y resúmenes, pero no las gráficas. Para compartir gráficas, use capturas de pantalla.

---

## 📚 Casos de Uso Prácticos

### Caso 1: Cierre Mensual de Ventas

**Objetivo:** Generar reporte para contabilidad

1. Acceder a Reportes > Ventas
2. Filtrar por mes completo (01/12/2025 - 31/12/2025)
3. Revisar estadísticas (subtotal, IGV, total)
4. Exportar a **Excel** para análisis
5. Exportar a **PDF** para archivo

### Caso 2: Seguimiento de Cobranza

**Objetivo:** Identificar clientes con deuda

1. Acceder a Reportes > Clientes
2. Filtrar por "Con Deuda: Sí"
3. Revisar lista de clientes morosos
4. Anotar montos pendientes
5. Exportar a Excel para gestión de cobranza

### Caso 3: Análisis de Inventario

**Objetivo:** Productos que necesitan reabastecimiento

1. Acceder a Reportes > Productos
2. Filtrar por "Stock Bajo: Sí"
3. Revisar productos críticos
4. Exportar lista para orden de compra
5. Actualizar stock

### Caso 4: Top 10 Clientes del Año

**Objetivo:** Identificar mejores clientes para programa de fidelización

1. Acceder a Reportes > Clientes
2. No aplicar filtros (ver todos)
3. Revisar sección "Top 5 Mejores Clientes"
4. Exportar reporte completo
5. Ordenar en Excel por "Total Compras" descendente

### Caso 5: Análisis de Productos Sin Rotación

**Objetivo:** Liquidar productos que no se venden

1. Acceder a Reportes > Productos
2. Filtrar por "Sin Ventas: Sí"
3. Revisar productos sin movimiento
4. Evaluar descontinuar o promocionar
5. Exportar para análisis

---

## 🔗 Atajos de Teclado

| Acción | Atajo |
|--------|-------|
| Recargar reporte | `F5` |
| Limpiar filtros | `Ctrl + L` |
| Exportar PDF | `Ctrl + P` |
| Exportar Excel | `Ctrl + E` |
| Búsqueda rápida | `Ctrl + F` |

> **Nota:** Algunos atajos pueden variar según el navegador.

---

## 📞 Soporte Técnico

Si necesita ayuda adicional:

1. **Revise este manual** primero
2. **Consulte la sección de Solución de Problemas**
3. **Contacte al administrador** del sistema
4. **Envíe un ticket** a soporte técnico con:
   - Descripción del problema
   - Pasos que realizó
   - Capturas de pantalla
   - Mensaje de error (si aplica)

---

## 📚 Documentación Relacionada

- [Manual de Configuración del Sistema](MANUAL_CONFIGURACION_SISTEMA.md)
- [Manual de Gestión de Usuarios](../docs/guias/MANUAL_USUARIO_PERMISOS.md)
- [Guía de Logging](../docs/guias/GUIA_LOGGING.md)
- [Configuración de Email](../configuracion/CONFIGURACION_EMAIL.md)

---

## 📝 Registro de Cambios

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 04/01/2026 | Versión inicial del manual |

---

**Documento actualizado:** 4 de enero de 2026  
**Versión del sistema:** 4.0 - Sprint 4  
**Autor:** Equipo de Desarrollo ERP Spring Manager  

---

*Este manual está sujeto a cambios conforme el sistema evoluciona. Consulte siempre la versión más reciente en la documentación oficial.*
