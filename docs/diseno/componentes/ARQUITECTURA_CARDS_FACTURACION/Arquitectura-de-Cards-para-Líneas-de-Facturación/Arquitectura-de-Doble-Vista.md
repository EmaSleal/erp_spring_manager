## Arquitectura de Doble Vista

### Estructura HTML en `add-form.html`

```html
<!-- DESKTOP: Tabla (visible solo en pantallas ≥768px) -->
<div class="table-responsive d-none d-md-block">
    <table class="table table-bordered">
        <thead class="table-light">
            <tr>
                <th>#</th>
                <th>Producto</th>
                <th>Cantidad</th>
                <th>Precio</th>
                <th>Subtotal</th>
                <th>Acción</th>
            </tr>
        </thead>
        <tbody id="lineas-body"></tbody>  <!-- ← Aquí se inyectan las filas -->
    </table>
</div>

<!-- MOBILE: Cards (visible solo en pantallas <768px) -->
<div class="d-md-none" id="lineas-cards-container"></div>  <!-- ← Aquí se inyectan los cards -->

<!-- Botón Agregar (visible en ambas vistas) -->
<button class="btn btn-success" onclick="addLinea()">
    <i class="fas fa-plus me-2"></i>Agregar línea
</button>

<!-- Resumen de totales (visible en ambas vistas) -->
<div id="resumen-subtotal">$0.00</div>
<div id="resumen-igv">$0.00</div>
<div id="resumen-total">$0.00</div>
```

### Clases Bootstrap Utilizadas

| Clase | Comportamiento |
|-------|---|
| `d-none d-md-block` | Oculta en móvil (<768px), visible en desktop |
| `d-md-none` | Visible en móvil (<768px), oculta en desktop |
| `card mb-3` | Diseño de tarjeta con margen inferior |
| `form-select` / `form-control` | Inputs estilizados |

---

