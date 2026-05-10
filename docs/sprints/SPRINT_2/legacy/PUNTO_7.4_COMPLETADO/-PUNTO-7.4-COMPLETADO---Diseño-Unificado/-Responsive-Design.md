## 📱 Responsive Design

### Breakpoints Verificados

✅ **Móvil (< 576px):**
- Layout fluido con sidebar colapsable
- Tablas con scroll horizontal
- Cards apiladas verticalmente
- Botones full-width cuando necesario

✅ **Tablet (576px - 992px):**
- Grid de 2 columnas para stats cards
- Sidebar toggleable
- Tablas responsive con scroll

✅ **Desktop (> 992px):**
- Layout completo con sidebar fijo
- Grid de 4 columnas para stats cards
- Tablas completas sin scroll

### Clases Responsive Usadas

```html
<!-- Grid responsive -->
<div class="row">
    <div class="col-12 col-sm-6 col-md-4 col-lg-3">
        <!-- Content -->
    </div>
</div>

<!-- Tablas responsive -->
<div class="table-responsive">
    <table class="table">...</table>
</div>

<!-- Utilidades responsive -->
<div class="d-none d-md-block">Solo desktop</div>
<div class="d-block d-md-none">Solo móvil</div>
```

---

