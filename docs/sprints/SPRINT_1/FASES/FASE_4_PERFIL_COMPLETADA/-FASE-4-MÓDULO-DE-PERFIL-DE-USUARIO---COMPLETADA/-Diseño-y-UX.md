## 🎨 Diseño y UX

### Paleta de Colores
```css
/* Gradiente principal */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

/* Gradiente de avatar (iniciales) */
background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);

/* Estados */
.badge-activo: #d4edda (verde claro)
.badge-inactivo: #f8d7da (rojo claro)

/* Fortaleza de contraseña */
.strength-weak: #dc3545 (rojo)
.strength-medium: #ffc107 (amarillo)
.strength-strong: #28a745 (verde)
```

### Efectos Hover
```css
.btn-save:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.nav-link:hover {
    color: #667eea;
}
```

### Responsive Breakpoints
- **Desktop:** > 768px - Layout normal
- **Tablet:** ≤ 768px - Columnas apiladas
- **Mobile:** ≤ 576px - Full width buttons

---

