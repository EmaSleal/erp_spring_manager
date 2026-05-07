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

