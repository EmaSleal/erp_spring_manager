## Cambios Realizados

### 1. Template: `form.html`

**Ubicación**: `src/main/resources/templates/modules/facturacion/form.html`

**Cambio**: Se dividió el contenedor de la tabla en dos vistas responsivas:

```html
<!-- ANTES: Solo tabla (no responsiva) -->
<div class="card-body">
    <div class="table-responsive">
        <table id="lineas-table" class="table table-bordered table-hover">
            <!-- ... -->
        </table>
    </div>
    <button type="button" onclick="addLinea()" class="btn btn-success mt-3">
        <i class="fas fa-plus me-2"></i>Agregar Producto
    </button>
</div>

<!-- DESPUÉS: Tabla + Cards responsivos -->
<div class="card-body">
    <!-- DESKTOP: Tabla (visible solo en pantallas ≥768px) -->
    <div class="table-responsive d-none d-md-block">
        <table id="lineas-table" class="table table-bordered table-hover">
            <!-- ... -->
        </table>
    </div>

    <!-- MOBILE: Cards (visible solo en pantallas <768px) -->
    <div class="d-md-none" id="lineas-cards-container">
        <!-- Se llenará con JS -->
    </div>

    <button type="button" onclick="addLinea()" class="btn btn-success mt-3">
        <i class="fas fa-plus me-2"></i>Agregar Producto
    </button>
</div>
```

**Explicación**:
- `d-none d-md-block`: Tabla oculta en móvil, visible en desktop (≥768px)
- `d-md-none`: Cards visibles en móvil, ocultos en desktop
- `id="lineas-cards-container"`: Contenedor donde se inyectan los cards

---

### 2. JavaScript: `editar-factura.js`

**Ubicación**: `src/main/resources/static/modules/facturacion/js/editar-factura.js`

**Estado**: ✅ **No requiere cambios** — Ya tiene todas las funciones necesarias

El archivo ya contiene:
- ✅ Detección de modo edición (`path.includes('/editar/')`)
- ✅ Carga de líneas existentes (`cargarLineas()`)
- ✅ Creación de filas y cards (`createLineaRow()`, `createLineaCard()`)
- ✅ Sincronización entre vistas (`sincronizarCardLinea()`)
- ✅ Limpieza de cards en `resetForm()`
- ✅ Actualización de vista (`actualizarVistaLineas()`)

---

