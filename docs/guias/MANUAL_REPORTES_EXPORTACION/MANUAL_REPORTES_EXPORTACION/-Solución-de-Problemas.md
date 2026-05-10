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

