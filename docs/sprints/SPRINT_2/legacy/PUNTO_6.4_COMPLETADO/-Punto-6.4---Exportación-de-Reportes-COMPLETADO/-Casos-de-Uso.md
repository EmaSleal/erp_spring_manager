## 📚 Casos de Uso

### **Caso 1: Exportar Reporte de Ventas con Filtros**
```
1. Usuario navega a /reportes/ventas
2. Aplica filtros: fechaInicio=2025-01-01, fechaFin=2025-12-31, clienteId=3
3. Click en botón "Exportar PDF"
4. Sistema captura parámetros de URL
5. Genera: /reportes/ventas/exportar/pdf?fechaInicio=2025-01-01&fechaFin=2025-12-31&clienteId=3
6. ReporteController recibe parámetros
7. ReporteService filtra facturas según parámetros
8. ReporteService calcula estadísticas
9. ExportService genera PDF con iText
10. ResponseEntity devuelve archivo: reporte-ventas.pdf
11. Navegador descarga archivo automáticamente
```

### **Caso 2: Exportar Todos los Clientes a Excel**
```
1. Usuario navega a /reportes/clientes
2. No aplica filtros (obtener todos)
3. Click en botón "Exportar Excel"
4. Sistema genera: /reportes/clientes/exportar/excel
5. ReporteService obtiene todos los clientes
6. ExportService genera XLSX con Apache POI
7. Archivo descargado: reporte-clientes.xlsx
```

### **Caso 3: Exportar Productos a CSV**
```
1. Usuario navega a /reportes/productos
2. Aplica filtro: stockBajo=true
3. Click en botón "Exportar CSV"
4. Sistema genera: /reportes/productos/exportar/csv?stockBajo=true
5. ExportService genera CSV nativo
6. Archivo descargado: reporte-productos.csv
7. Compatible con Excel, Google Sheets, LibreOffice
```

---

