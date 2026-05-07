## 📤 MEJORA 5: Exportación a Excel/PDF

### Implementación

#### 5.1 Botones de Exportación

```html
<!-- En comprobantes.html -->
<div class="card-header d-flex justify-content-between align-items-center">
    <h5 class="card-title mb-0">Comprobantes Electrónicos</h5>
    
    <div class="btn-group">
        <button class="btn btn-success btn-sm" onclick="exportarExcel()">
            <i class="bi bi-file-earmark-excel"></i> Excel
        </button>
        <button class="btn btn-danger btn-sm" onclick="exportarPDF()">
            <i class="bi bi-file-earmark-pdf"></i> PDF
        </button>
    </div>
</div>
```

#### 5.2 JavaScript para Exportación

```javascript
function exportarExcel() {
    const filtros = obtenerFiltrosActuales();
    window.location.href = `/api/facturas/electronica/comprobantes/exportar/excel?${filtros}`;
}

function exportarPDF() {
    const filtros = obtenerFiltrosActuales();
    window.open(`/api/facturas/electronica/comprobantes/exportar/pdf?${filtros}`, '_blank');
}

function obtenerFiltrosActuales() {
    const params = new URLSearchParams();
    const estado = document.getElementById('filtroEstado').value;
    const fechaDesde = document.getElementById('fechaDesde').value;
    const fechaHasta = document.getElementById('fechaHasta').value;
    
    if (estado) params.append('estado', estado);
    if (fechaDesde) params.append('fechaDesde', fechaDesde);
    if (fechaHasta) params.append('fechaHasta', fechaHasta);
    
    return params.toString();
}
```

---

