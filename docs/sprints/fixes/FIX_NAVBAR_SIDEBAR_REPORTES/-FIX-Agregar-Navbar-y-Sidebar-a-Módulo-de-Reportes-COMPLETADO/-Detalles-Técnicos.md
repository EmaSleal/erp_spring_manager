## 🔍 Detalles Técnicos

### **Fragment Layout (layout :: head)**
Incluye automáticamente:
- ✅ Bootstrap 5.3.0 CSS
- ✅ Font Awesome 6.4.0
- ✅ `/css/navbar.css`
- ✅ `/css/sidebar.css`
- ✅ `/css/dashboard.css`
- ✅ Meta tags responsivos

### **Fragment Navbar (components/navbar :: navbar)**
- Barra superior con:
  - Logo/Título de la aplicación
  - Menú de usuario
  - Notificaciones
  - Botón de logout

### **Fragment Sidebar (components/sidebar :: sidebar)**
- Menú lateral con:
  - Dashboard
  - Productos
  - Clientes
  - Facturación
  - **Reportes** (ahora accesible)
  - Configuración
  - Usuarios (admin)

### **Clase CSS: main-content**
```css
.main-content {
    margin-left: 250px; /* Ancho del sidebar */
    padding-top: 60px;  /* Altura del navbar */
    min-height: 100vh;
    transition: margin-left 0.3s;
}

/* Responsive */
@media (max-width: 768px) {
    .main-content {
        margin-left: 0;
    }
}
```

---

