## 🎨 DISEÑO UX/UI

### Tema Oscuro (Dark Mode)

**Paleta de colores:**
```css
/* Tema Oscuro */
:root[data-theme="dark"] {
    --bg-primary: #1a1a2e;
    --bg-secondary: #16213e;
    --bg-tertiary: #0f3460;
    --text-primary: #e4e4e4;
    --text-secondary: #a8a8a8;
    --accent-primary: #4a90e2;
    --accent-secondary: #5bc0de;
    --border-color: #2d3748;
    --shadow: rgba(0, 0, 0, 0.5);
}

/* Tema Claro */
:root[data-theme="light"] {
    --bg-primary: #ffffff;
    --bg-secondary: #f8f9fa;
    --bg-tertiary: #e9ecef;
    --text-primary: #212529;
    --text-secondary: #6c757d;
    --accent-primary: #007bff;
    --accent-secondary: #17a2b8;
    --border-color: #dee2e6;
    --shadow: rgba(0, 0, 0, 0.15);
}
```

**Selector de tema:**
```html
<div class="theme-selector">
    <button data-theme="light">☀️ Claro</button>
    <button data-theme="dark">🌙 Oscuro</button>
    <button data-theme="auto">🔄 Auto</button>
</div>
```

---

### Accesibilidad

**WCAG 2.1 AA - Requisitos:**
- ✅ Contraste mínimo 4.5:1 (texto normal)
- ✅ Contraste mínimo 3:1 (texto grande)
- ✅ Navegación por teclado completa (Tab, Enter, Esc)
- ✅ ARIA labels en elementos interactivos
- ✅ Alt text en todas las imágenes
- ✅ Skip links para saltar navegación
- ✅ Foco visible en todos los elementos
- ✅ Tamaño de botones mínimo 44x44px

**Implementación:**
```html
<!-- ARIA labels -->
<button aria-label="Crear nueva factura" title="Crear nueva factura">
    <i class="fas fa-plus"></i>
</button>

<!-- Skip link -->
<a href="#main-content" class="skip-link">Saltar al contenido</a>

<!-- ARIA live region para notificaciones -->
<div role="status" aria-live="polite" aria-atomic="true">
    Factura creada exitosamente
</div>
```

---

