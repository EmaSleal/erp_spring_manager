## 📊 Arquitectura de Exportación

```
┌─────────────────────────────────────────────────┐
│           ReporteController                     │
│  ┌──────────────────────────────────────────┐  │
│  │  GET /reportes/{tipo}/exportar/{formato}  │  │
│  │  - Recibe parámetros de filtros           │  │
│  │  - Genera datos con ReporteService        │  │
│  │  - Calcula estadísticas                   │  │
│  │  - Llama a ExportService                  │  │
│  │  - Devuelve ResponseEntity<byte[]>        │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│            ReporteService                       │
│  ┌──────────────────────────────────────────┐  │
│  │  generarReporte{Tipo}()                   │  │
│  │  - Filtra datos según parámetros          │  │
│  │  - Retorna List<Entidad>                  │  │
│  │                                            │  │
│  │  calcularEstadisticas{Tipo}()             │  │
│  │  - Calcula totales, promedios, etc.       │  │
│  │  - Retorna Map<String, Object>            │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│            ExportService                        │
│  ┌──────────────────────────────────────────┐  │
│  │  exportar{Tipo}PDF()                      │  │
│  │  ├─ iText 7.2.5                           │  │
│  │  ├─ PdfDocument + Document                │  │
│  │  ├─ Tables con formato                    │  │
│  │  └─ ByteArrayOutputStream                 │  │
│  │                                            │  │
│  │  exportar{Tipo}Excel()                    │  │
│  │  ├─ Apache POI 5.2.3                      │  │
│  │  ├─ XSSFWorkbook + Sheet                  │  │
│  │  ├─ CellStyles personalizados             │  │
│  │  └─ ByteArrayOutputStream                 │  │
│  │                                            │  │
│  │  exportar{Tipo}CSV()                      │  │
│  │  ├─ StringBuilder nativo                  │  │
│  │  ├─ Escape de caracteres especiales       │  │
│  │  └─ ByteArrayOutputStream UTF-8           │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│     ResponseEntity<byte[]>                      │
│  ┌──────────────────────────────────────────┐  │
│  │  HttpHeaders                              │  │
│  │  ├─ Content-Type (application/pdf, xlsx)  │  │
│  │  ├─ Content-Disposition (attachment)      │  │
│  │  └─ Filename (reporte-{tipo}.{ext})       │  │
│  │                                            │  │
│  │  Body: byte[]                             │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
                     ↓
              Descarga en navegador
```

---

