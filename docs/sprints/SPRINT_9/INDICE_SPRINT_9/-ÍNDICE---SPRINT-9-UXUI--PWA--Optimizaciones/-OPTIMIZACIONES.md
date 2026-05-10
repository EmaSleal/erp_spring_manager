## ⚡ OPTIMIZACIONES

### Lighthouse Metrics

**Objetivos:**
```
Performance:       > 90
Accessibility:     > 95
Best Practices:    > 95
SEO:               > 90
PWA:               ✓ (todos los checks)
```

**Optimizaciones clave:**
- ✅ Lazy loading de imágenes: `<img loading="lazy">`
- ✅ Preload de recursos críticos: `<link rel="preload">`
- ✅ Defer de JavaScript no crítico: `<script defer>`
- ✅ Minificación CSS/JS
- ✅ Compresión Gzip/Brotli (nginx/Apache)
- ✅ Cache-Control headers optimizados

---

### Lazy Loading

**Imágenes:**
```html
<img src="placeholder.jpg" 
     data-src="imagen-real.jpg" 
     loading="lazy" 
     alt="Descripción">
```

**JavaScript modules:**
```javascript
// Code splitting con import dinámico
const loadChartModule = () => import('./modules/charts.js');

document.getElementById('show-chart').addEventListener('click', async () => {
  const chartModule = await loadChartModule();
  chartModule.renderChart();
});
```

---

### Optimización SQL

**Índices recomendados:**
```sql
-- Índices para mejora de rendimiento
CREATE INDEX idx_factura_cliente ON factura(cliente_id);
CREATE INDEX idx_factura_fecha ON factura(fecha_creacion);
CREATE INDEX idx_factura_estado ON factura(estado);
CREATE INDEX idx_producto_nombre ON producto(nombre);
CREATE INDEX idx_cliente_email ON cliente(email);
CREATE INDEX idx_usuario_username ON usuario(username);

-- Índice compuesto para reportes
CREATE INDEX idx_factura_fecha_estado ON factura(fecha_creacion, estado);
```

**N+1 Queries - Solución:**
```java
// ❌ MAL - N+1 query
List<Factura> facturas = facturaRepository.findAll();
for (Factura f : facturas) {
    f.getCliente().getNombre(); // Query por cada factura
}

// ✅ BIEN - Fetch join
@Query("SELECT f FROM Factura f JOIN FETCH f.cliente WHERE f.estado = :estado")
List<Factura> findAllWithCliente(@Param("estado") String estado);
```

---

