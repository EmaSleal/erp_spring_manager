## 🔮 Mejoras Futuras

### 1. Validación de Número Único

**Descripción:** Validar en backend que numeroFactura no se repita.

```java
@Override
public Factura save(Factura factura) {
    // Validar unicidad
    if (factura.getNumeroFactura() != null) {
        Optional<Factura> existente = facturaRepository
            .findByNumeroFactura(factura.getNumeroFactura());
        if (existente.isPresent() && !existente.get().getIdFactura().equals(factura.getIdFactura())) {
            throw new IllegalArgumentException("El número de factura ya existe");
        }
    }
    // Continuar...
}
```

---

### 2. Configurar Días para Fecha de Pago

**Descripción:** Permitir configurar los días en `configuracion_facturacion`.

```java
// Agregar campo a ConfiguracionFacturacion
private Integer diasPago = 7; // Default 7 días

// En JavaScript
function calcularFechaPago() {
    // Obtener días configurados desde backend
    const diasPago = await fetch('/configuracion/dias-pago').then(r => r.json());
    entrega.setDate(entrega.getDate() + diasPago);
}
```

---

### 3. IGV Configurable

**Descripción:** Aplicar IGV basado en configuración.

```javascript
function actualizarResumenTotales() {
    const subtotal = calcularSubtotal();
    const igvPorcentaje = await fetch('/configuracion/igv').then(r => r.json()); // 18%
    const igv = subtotal * (igvPorcentaje / 100);
    const total = subtotal + igv;
    // Actualizar UI...
}
```

---

### 4. Búsqueda por Número de Factura

**Descripción:** Agregar filtro en tabla.

```html
<input type="text" placeholder="Buscar por N° Factura..." id="filterNumero">
```

```javascript
function filtrarPorNumero() {
    const filtro = document.getElementById('filterNumero').value;
    fetch(`/facturas?numero=${filtro}`)...
}
```

---

### 5. Exportar Factura con Número

**Descripción:** Incluir número en PDF generado.

```java
// En método de generación de PDF
document.add(new Paragraph("Factura N°: " + factura.getNumeroFactura()));
document.add(new Paragraph("Serie: " + factura.getSerie()));
document.add(new Paragraph("Fecha Límite Pago: " + factura.getFechaPago()));
```

---

