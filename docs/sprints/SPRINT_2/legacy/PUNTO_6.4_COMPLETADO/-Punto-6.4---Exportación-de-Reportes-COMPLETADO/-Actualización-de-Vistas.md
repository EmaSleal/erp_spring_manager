## 🎨 Actualización de Vistas

### **reportes/ventas.html**

**Funciones JavaScript actualizadas:**
```javascript
// Exportar a PDF
function exportarPDF() {
    const urlParams = new URLSearchParams(window.location.search);
    const fechaInicio = urlParams.get('fechaInicio') || '';
    const fechaFin = urlParams.get('fechaFin') || '';
    const clienteId = urlParams.get('clienteId') || '';
    
    const exportUrl = /*[[@{/reportes/ventas/exportar/pdf}]]*/ '/reportes/ventas/exportar/pdf' + 
                     '?' +
                     (fechaInicio ? 'fechaInicio=' + fechaInicio + '&' : '') +
                     (fechaFin ? 'fechaFin=' + fechaFin + '&' : '') +
                     (clienteId ? 'clienteId=' + clienteId : '');
    
    window.location.href = exportUrl.replace(/&$/, '').replace(/\?$/, '');
}

// Exportar a Excel
function exportarExcel() {
    const urlParams = new URLSearchParams(window.location.search);
    const fechaInicio = urlParams.get('fechaInicio') || '';
    const fechaFin = urlParams.get('fechaFin') || '';
    const clienteId = urlParams.get('clienteId') || '';
    
    const exportUrl = /*[[@{/reportes/ventas/exportar/excel}]]*/ '/reportes/ventas/exportar/excel' + 
                     '?' +
                     (fechaInicio ? 'fechaInicio=' + fechaInicio + '&' : '') +
                     (fechaFin ? 'fechaFin=' + fechaFin + '&' : '') +
                     (clienteId ? 'clienteId=' + clienteId : '');
    
    window.location.href = exportUrl.replace(/&$/, '').replace(/\?$/, '');
}
```

### **reportes/clientes.html**

```javascript
function exportarPDF() {
    const urlParams = new URLSearchParams(window.location.search);
    const activo = urlParams.get('activo') || '';
    const conDeuda = urlParams.get('conDeuda') || '';
    
    const exportUrl = /*[[@{/reportes/clientes/exportar/pdf}]]*/ '/reportes/clientes/exportar/pdf' + 
                     '?' +
                     (activo ? 'activo=' + activo + '&' : '') +
                     (conDeuda ? 'conDeuda=' + conDeuda : '');
    
    window.location.href = exportUrl.replace(/&$/, '').replace(/\?$/, '');
}
```

### **reportes/productos.html**

```javascript
function exportarPDF() {
    const urlParams = new URLSearchParams(window.location.search);
    const stockBajo = urlParams.get('stockBajo') || '';
    const sinVentas = urlParams.get('sinVentas') || '';
    
    const exportUrl = /*[[@{/reportes/productos/exportar/pdf}]]*/ '/reportes/productos/exportar/pdf' + 
                     '?' +
                     (stockBajo ? 'stockBajo=' + stockBajo + '&' : '') +
                     (sinVentas ? 'sinVentas=' + sinVentas : '');
    
    window.location.href = exportUrl.replace(/&$/, '').replace(/\?$/, '');
}
```

---

