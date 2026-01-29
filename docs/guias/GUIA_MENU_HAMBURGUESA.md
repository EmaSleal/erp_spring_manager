# Guía del Menú Hamburguesa (Responsive Sidebar)

## 📱 Descripción

El sidebar del sistema ahora es completamente responsive y se adapta automáticamente a dispositivos móviles mediante un botón hamburguesa flotante.

## 🎯 Características

### Desktop (> 768px)
- Sidebar visible por defecto en el lado izquierdo
- Botón de colapso para minimizar el sidebar
- Estado del sidebar se guarda en localStorage

### Móvil (≤ 767px)
- Sidebar oculto por defecto
- **Botón hamburguesa flotante** en la esquina inferior derecha
- Overlay oscuro al abrir el sidebar
- Cierra automáticamente al seleccionar un módulo

## 🔧 Componentes Implementados

### HTML (`sidebar.html`)
```html
<!-- Sidebar principal -->
<aside th:fragment="sidebar" class="sidebar" id="sidebar">
    <!-- ... contenido del menú ... -->
</aside>

<!-- Overlay para cerrar en móvil -->
<div class="sidebar-overlay"></div>

<!-- Botón hamburguesa flotante -->
<button class="sidebar-toggle" aria-label="Toggle sidebar">
    <i class="fas fa-bars"></i>
</button>
```

### CSS (`sidebar.css`)
- **Transición suave**: `transform 0.3s ease-in-out`
- **Botón flotante**: Posición fija en esquina inferior derecha
- **Gradiente**: Diseño moderno con sombras elevadas
- **Animación**: Icono rota 90° al abrir

### JavaScript (`sidebar.js`)
- **Toggle móvil**: Abre/cierra el sidebar
- **Overlay**: Permite cerrar tocando fuera del menú
- **Auto-cierre**: Cierra al navegar a un módulo
- **Responsive**: Detecta cambios de tamaño de pantalla

## 📐 Estilos del Botón Hamburguesa

```css
.sidebar-toggle {
    position: fixed;
    bottom: 2rem;
    right: 2rem;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: linear-gradient(135deg, #1976D2 0%, #0d47a1 100%);
    box-shadow: 0 4px 12px rgba(25, 118, 210, 0.4);
    z-index: 1001;
}
```

## 🎨 Interacciones

### Abrir el menú
1. Click en el botón hamburguesa
2. Sidebar se desliza desde la izquierda
3. Overlay oscurece el contenido detrás

### Cerrar el menú
- Click en el botón hamburguesa nuevamente
- Click en el overlay (fuera del sidebar)
- Seleccionar cualquier módulo del menú

## 🔄 Estados del Sidebar

| Estado | Clase CSS | Comportamiento |
|--------|-----------|----------------|
| Oculto (móvil) | `transform: translateX(-100%)` | Por defecto en móvil |
| Visible (móvil) | `.sidebar.show` | Al hacer click en hamburguesa |
| Normal (desktop) | Sin transformación | Visible por defecto |
| Colapsado (desktop) | `.sidebar.collapsed` | Al hacer click en botón collapse |

## 🎯 Breakpoint Responsive

```css
@media (max-width: 767px) {
    /* Comportamiento móvil */
    .sidebar-toggle { display: flex; }
}

@media (min-width: 768px) {
    /* Comportamiento desktop */
    .sidebar-toggle { display: none; }
}
```

## ✅ Uso en Páginas

Todas las páginas que usen el layout estándar incluyen automáticamente:

```html
<!-- En el body -->
<div th:replace="~{shared/components/sidebar :: sidebar}"></div>

<!-- Scripts necesarios -->
<script th:src="@{/shared/js/sidebar.js}"></script>
```

## 🧪 Testing en Dispositivos

### Chrome DevTools
1. Abrir DevTools (F12)
2. Click en "Toggle device toolbar" (Ctrl+Shift+M)
3. Seleccionar dispositivo móvil (ej: iPhone 12)
4. Verificar botón hamburguesa en esquina inferior derecha

### Responsive Breakpoints
- **Móvil**: < 768px (iPhone, Android)
- **Tablet**: 768px - 991px (iPad)
- **Desktop**: > 991px

## 🎨 Personalización

### Cambiar posición del botón
```css
.sidebar-toggle {
    bottom: 1rem;  /* Cambiar altura */
    right: 1rem;   /* Cambiar posición horizontal */
}
```

### Cambiar color del botón
```css
.sidebar-toggle {
    background: linear-gradient(135deg, #YOUR_COLOR1 0%, #YOUR_COLOR2 100%);
}
```

### Cambiar icono
```html
<button class="sidebar-toggle">
    <i class="fas fa-bars"></i> <!-- Cambiar por otro icono -->
</button>
```

## 📱 Compatibilidad

- ✅ iOS Safari (iPhone)
- ✅ Chrome Mobile (Android)
- ✅ Samsung Internet
- ✅ Firefox Mobile
- ✅ Edge Mobile
- ✅ Todos los navegadores desktop modernos

## 🐛 Troubleshooting

### El botón no aparece en móvil
- Verificar que el viewport meta tag esté presente: 
  ```html
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  ```

### El sidebar no se cierra
- Verificar que el script `sidebar.js` esté cargado
- Revisar la consola del navegador por errores JavaScript

### Transiciones no funcionan
- Verificar que las variables CSS estén definidas en `common.css`
- Asegurar que no haya conflictos con otros estilos

## 🎓 Recursos Adicionales

- Font Awesome Icons: https://fontawesome.com/icons
- CSS Transform: https://developer.mozilla.org/es/docs/Web/CSS/transform
- CSS Transitions: https://developer.mozilla.org/es/docs/Web/CSS/transition

---

**Última actualización**: 20 de enero de 2026  
**Versión**: 1.0.0
