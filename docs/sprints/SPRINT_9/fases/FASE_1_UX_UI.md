# 🎨 FASE 1: UX/UI Mejorado

**Sprint:** 9  
**Fase:** 1 de 5  
**Duración estimada:** 6-8 días  
**Prioridad:** ⭐⭐ ALTA  
**Estado:** 📋 PENDIENTE (0/36 tareas)

---

## 📋 OBJETIVO DE LA FASE

Mejorar significativamente la experiencia de usuario mediante:
- **Tema Oscuro (Dark Mode)** completo y elegante
- **Accesibilidad WCAG 2.1 AA** completa
- **Animaciones y transiciones** suaves
- **Responsive design** perfeccionado
- **Navegación mejorada** con breadcrumbs
- **Feedback visual** consistente

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/36] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Sistema de Temas (Claro/Oscuro)  [0/8]  ░░░░░░░░░░ 0%
├─ 2. Accesibilidad (WCAG 2.1 AA)      [0/7]  ░░░░░░░░░░ 0%
├─ 3. Animaciones y Transiciones       [0/5]  ░░░░░░░░░░ 0%
├─ 4. Componentes UI Mejorados         [0/8]  ░░░░░░░░░░ 0%
├─ 5. Navegación y Breadcrumbs         [0/4]  ░░░░░░░░░░ 0%
└─ 6. Responsive Design                [0/4]  ░░░░░░░░░░ 0%
```

---

## 📦 1. SISTEMA DE TEMAS (8 tareas)

### 1.1. Descripción

Implementar sistema completo de temas con:
- ✅ **Tema Claro** (mejorado)
- ✅ **Tema Oscuro** (nuevo)
- ✅ **Modo Automático** (según hora del día o preferencia del sistema)
- ✅ Persistencia de preferencias en localStorage
- ✅ Transición suave entre temas

#### Tareas:

- [ ] **1.1** Crear variables CSS para tema claro

```css
/* src/main/resources/static/css/themes/light-theme.css */

:root[data-theme="light"] {
    /* ==================== COLORES PRIMARIOS ==================== */
    --color-primary: #0d6efd;
    --color-primary-hover: #0b5ed7;
    --color-primary-active: #0a58ca;
    --color-primary-light: #e7f1ff;
    
    /* ==================== COLORES SECUNDARIOS ==================== */
    --color-secondary: #6c757d;
    --color-secondary-hover: #5c636a;
    --color-success: #198754;
    --color-success-light: #d1e7dd;
    --color-danger: #dc3545;
    --color-danger-light: #f8d7da;
    --color-warning: #ffc107;
    --color-warning-light: #fff3cd;
    --color-info: #0dcaf0;
    --color-info-light: #cff4fc;
    
    /* ==================== FONDOS ==================== */
    --bg-body: #ffffff;
    --bg-card: #ffffff;
    --bg-sidebar: #f8f9fa;
    --bg-navbar: #ffffff;
    --bg-footer: #f8f9fa;
    --bg-modal: #ffffff;
    --bg-input: #ffffff;
    --bg-input-disabled: #e9ecef;
    --bg-hover: #f8f9fa;
    --bg-active: #e9ecef;
    
    /* ==================== TEXTOS ==================== */
    --text-primary: #212529;
    --text-secondary: #6c757d;
    --text-muted: #adb5bd;
    --text-white: #ffffff;
    --text-link: #0d6efd;
    --text-link-hover: #0a58ca;
    
    /* ==================== BORDES ==================== */
    --border-color: #dee2e6;
    --border-color-dark: #ced4da;
    --border-radius: 0.375rem;
    --border-radius-lg: 0.5rem;
    --border-radius-sm: 0.25rem;
    
    /* ==================== SOMBRAS ==================== */
    --shadow-sm: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
    --shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);
    --shadow-lg: 0 1rem 3rem rgba(0, 0, 0, 0.175);
    
    /* ==================== TRANSICIONES ==================== */
    --transition-fast: 0.15s ease-in-out;
    --transition-normal: 0.3s ease-in-out;
    --transition-slow: 0.5s ease-in-out;
}
```

- [ ] **1.2** Crear variables CSS para tema oscuro

```css
/* src/main/resources/static/css/themes/dark-theme.css */

:root[data-theme="dark"] {
    /* ==================== COLORES PRIMARIOS ==================== */
    --color-primary: #4d96ff;
    --color-primary-hover: #6ba5ff;
    --color-primary-active: #3d86ef;
    --color-primary-light: #1a3a5c;
    
    /* ==================== COLORES SECUNDARIOS ==================== */
    --color-secondary: #adb5bd;
    --color-secondary-hover: #ced4da;
    --color-success: #20c997;
    --color-success-light: #1a4d3a;
    --color-danger: #ff6b6b;
    --color-danger-light: #5c2020;
    --color-warning: #ffd93d;
    --color-warning-light: #5c4d1a;
    --color-info: #6bcff6;
    --color-info-light: #1a4c5c;
    
    /* ==================== FONDOS ==================== */
    --bg-body: #0d1117;
    --bg-card: #161b22;
    --bg-sidebar: #0d1117;
    --bg-navbar: #161b22;
    --bg-footer: #0d1117;
    --bg-modal: #161b22;
    --bg-input: #0d1117;
    --bg-input-disabled: #1c2128;
    --bg-hover: #1c2128;
    --bg-active: #21262d;
    
    /* ==================== TEXTOS ==================== */
    --text-primary: #e6edf3;
    --text-secondary: #adb5bd;
    --text-muted: #6c757d;
    --text-white: #ffffff;
    --text-link: #4d96ff;
    --text-link-hover: #6ba5ff;
    
    /* ==================== BORDES ==================== */
    --border-color: #30363d;
    --border-color-dark: #21262d;
    --border-radius: 0.375rem;
    --border-radius-lg: 0.5rem;
    --border-radius-sm: 0.25rem;
    
    /* ==================== SOMBRAS ==================== */
    --shadow-sm: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.5);
    --shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.7);
    --shadow-lg: 0 1rem 3rem rgba(0, 0, 0, 0.9);
    
    /* ==================== SCROLLBAR OSCURO ==================== */
    scrollbar-color: #30363d #0d1117;
}

/* Scrollbar personalizado para tema oscuro */
:root[data-theme="dark"] ::-webkit-scrollbar {
    width: 12px;
}

:root[data-theme="dark"] ::-webkit-scrollbar-track {
    background: #0d1117;
}

:root[data-theme="dark"] ::-webkit-scrollbar-thumb {
    background: #30363d;
    border-radius: 6px;
}

:root[data-theme="dark"] ::-webkit-scrollbar-thumb:hover {
    background: #484f58;
}
```

- [ ] **1.3** Crear JavaScript para cambio de tema

```javascript
// src/main/resources/static/js/theme-switcher.js

/**
 * Gestor de Temas (Claro/Oscuro/Auto)
 */
class ThemeManager {
    
    constructor() {
        this.STORAGE_KEY = 'erp-theme-preference';
        this.currentTheme = this.loadTheme();
        this.applyTheme(this.currentTheme);
        this.setupListeners();
    }
    
    /**
     * Cargar tema guardado o detectar preferencia del sistema
     */
    loadTheme() {
        const saved = localStorage.getItem(this.STORAGE_KEY);
        
        if (saved) {
            return saved; // 'light', 'dark', 'auto'
        }
        
        // Default: auto (detectar preferencia del sistema)
        return 'auto';
    }
    
    /**
     * Aplicar tema
     */
    applyTheme(theme) {
        let actualTheme = theme;
        
        if (theme === 'auto') {
            // Detectar preferencia del sistema
            actualTheme = window.matchMedia('(prefers-color-scheme: dark)').matches 
                ? 'dark' 
                : 'light';
        }
        
        // Aplicar al HTML
        document.documentElement.setAttribute('data-theme', actualTheme);
        
        // Actualizar icono del botón
        this.updateThemeIcon(theme);
        
        // Actualizar meta theme-color para PWA
        this.updateMetaThemeColor(actualTheme);
        
        console.log(`✅ Tema aplicado: ${actualTheme} (Preferencia: ${theme})`);
    }
    
    /**
     * Cambiar tema
     */
    setTheme(theme) {
        this.currentTheme = theme;
        localStorage.setItem(this.STORAGE_KEY, theme);
        this.applyTheme(theme);
        
        // Dispatch evento personalizado
        window.dispatchEvent(new CustomEvent('themeChanged', { 
            detail: { theme } 
        }));
    }
    
    /**
     * Toggle entre claro y oscuro
     */
    toggleTheme() {
        const currentActual = document.documentElement.getAttribute('data-theme');
        const newTheme = currentActual === 'dark' ? 'light' : 'dark';
        this.setTheme(newTheme);
    }
    
    /**
     * Actualizar icono del botón de tema
     */
    updateThemeIcon(theme) {
        const icon = document.getElementById('theme-icon');
        if (!icon) return;
        
        const icons = {
            'light': 'bi-sun-fill',
            'dark': 'bi-moon-stars-fill',
            'auto': 'bi-circle-half'
        };
        
        // Remover clases anteriores
        icon.className = '';
        icon.classList.add('bi', icons[theme] || icons['auto']);
    }
    
    /**
     * Actualizar meta theme-color para PWA
     */
    updateMetaThemeColor(theme) {
        const metaTheme = document.querySelector('meta[name="theme-color"]');
        if (!metaTheme) return;
        
        const colors = {
            'light': '#ffffff',
            'dark': '#161b22'
        };
        
        metaTheme.setAttribute('content', colors[theme] || colors['light']);
    }
    
    /**
     * Setup event listeners
     */
    setupListeners() {
        // Listener para cambios en preferencia del sistema
        window.matchMedia('(prefers-color-scheme: dark)')
            .addEventListener('change', (e) => {
                if (this.currentTheme === 'auto') {
                    this.applyTheme('auto');
                }
            });
        
        // Botón de cambio de tema
        const themeBtn = document.getElementById('theme-toggle');
        if (themeBtn) {
            themeBtn.addEventListener('click', () => {
                this.toggleTheme();
            });
        }
    }
    
    /**
     * Obtener tema actual
     */
    getCurrentTheme() {
        return this.currentTheme;
    }
}

// Inicializar al cargar la página
const themeManager = new ThemeManager();

// Exponer globalmente
window.themeManager = themeManager;
```

- [ ] **1.4** Agregar botón de cambio de tema en navbar

```html
<!-- En navbar.html -->
<div class="navbar-nav ms-auto">
    <!-- Botón de tema -->
    <button id="theme-toggle" 
            class="btn btn-link nav-link" 
            title="Cambiar tema">
        <i id="theme-icon" class="bi bi-moon-stars-fill fs-5"></i>
    </button>
    
    <!-- Dropdown de usuario -->
    <div class="dropdown">
        <!-- ... -->
    </div>
</div>
```

- [ ] **1.5** Crear dropdown de selección de tema (Claro/Oscuro/Auto)

```html
<!-- Dropdown avanzado de tema -->
<div class="dropdown">
    <button class="btn btn-link nav-link dropdown-toggle" 
            type="button" 
            id="themeDropdown" 
            data-bs-toggle="dropdown">
        <i id="theme-icon" class="bi bi-moon-stars-fill"></i>
    </button>
    <ul class="dropdown-menu dropdown-menu-end">
        <li>
            <a class="dropdown-item" href="#" onclick="themeManager.setTheme('light')">
                <i class="bi bi-sun-fill me-2"></i> Tema Claro
            </a>
        </li>
        <li>
            <a class="dropdown-item" href="#" onclick="themeManager.setTheme('dark')">
                <i class="bi bi-moon-stars-fill me-2"></i> Tema Oscuro
            </a>
        </li>
        <li><hr class="dropdown-divider"></li>
        <li>
            <a class="dropdown-item" href="#" onclick="themeManager.setTheme('auto')">
                <i class="bi bi-circle-half me-2"></i> Automático
            </a>
        </li>
    </ul>
</div>
```

- [ ] **1.6** Actualizar estilos globales para usar variables CSS

```css
/* src/main/resources/static/css/main.css */

/* Aplicar variables de tema */
body {
    background-color: var(--bg-body);
    color: var(--text-primary);
    transition: background-color var(--transition-normal), 
                color var(--transition-normal);
}

.card {
    background-color: var(--bg-card);
    border-color: var(--border-color);
    color: var(--text-primary);
    box-shadow: var(--shadow-sm);
    transition: all var(--transition-normal);
}

.card:hover {
    box-shadow: var(--shadow);
}

.sidebar {
    background-color: var(--bg-sidebar);
    border-right: 1px solid var(--border-color);
}

.navbar {
    background-color: var(--bg-navbar);
    border-bottom: 1px solid var(--border-color);
}

input, select, textarea {
    background-color: var(--bg-input);
    border-color: var(--border-color);
    color: var(--text-primary);
}

input:disabled {
    background-color: var(--bg-input-disabled);
}

.table {
    color: var(--text-primary);
}

.table-hover tbody tr:hover {
    background-color: var(--bg-hover);
}

a {
    color: var(--text-link);
}

a:hover {
    color: var(--text-link-hover);
}
```

- [ ] **1.7** Agregar transición suave al cambiar tema

```css
/* Transición suave para cambio de tema */
* {
    transition: background-color 0.3s ease-in-out, 
                color 0.3s ease-in-out, 
                border-color 0.3s ease-in-out;
}

/* Evitar transición en elementos específicos */
.no-theme-transition {
    transition: none !important;
}
```

- [ ] **1.8** Tests de sistema de temas

---

## 📦 2. ACCESIBILIDAD WCAG 2.1 AA (7 tareas)

### 2.1. Descripción

Cumplir con estándares **WCAG 2.1 Nivel AA**:
- ✅ Navegación completa por teclado
- ✅ ARIA labels en todos los elementos
- ✅ Contraste de colores ≥ 4.5:1
- ✅ Tamaño de fuente ajustable
- ✅ Modo alto contraste
- ✅ Screen reader friendly

#### Tareas:

- [ ] **2.1** Agregar navegación por teclado completa

```javascript
// src/main/resources/static/js/accessibility.js

/**
 * Gestor de Accesibilidad
 */
class AccessibilityManager {
    
    constructor() {
        this.setupKeyboardNavigation();
        this.setupFocusIndicators();
        this.setupSkipLinks();
    }
    
    /**
     * Navegación por teclado
     */
    setupKeyboardNavigation() {
        document.addEventListener('keydown', (e) => {
            // Alt + 1: Ir a contenido principal
            if (e.altKey && e.key === '1') {
                document.getElementById('main-content')?.focus();
                e.preventDefault();
            }
            
            // Alt + 2: Ir a navegación
            if (e.altKey && e.key === '2') {
                document.getElementById('main-nav')?.focus();
                e.preventDefault();
            }
            
            // Alt + T: Cambiar tema
            if (e.altKey && e.key === 't') {
                window.themeManager?.toggleTheme();
                e.preventDefault();
            }
            
            // Escape: Cerrar modals/dropdowns
            if (e.key === 'Escape') {
                this.closeAllModals();
            }
        });
    }
    
    /**
     * Indicadores de foco visibles
     */
    setupFocusIndicators() {
        const style = document.createElement('style');
        style.textContent = `
            *:focus {
                outline: 3px solid var(--color-primary);
                outline-offset: 2px;
            }
            
            *:focus:not(:focus-visible) {
                outline: none;
            }
        `;
        document.head.appendChild(style);
    }
    
    /**
     * Skip links para screen readers
     */
    setupSkipLinks() {
        const skipLink = document.createElement('a');
        skipLink.href = '#main-content';
        skipLink.className = 'skip-link';
        skipLink.textContent = 'Saltar al contenido principal';
        document.body.prepend(skipLink);
    }
    
    closeAllModals() {
        document.querySelectorAll('.modal.show').forEach(modal => {
            const bsModal = bootstrap.Modal.getInstance(modal);
            bsModal?.hide();
        });
    }
}

// Inicializar
const accessibilityManager = new AccessibilityManager();
```

- [ ] **2.2** Agregar ARIA labels a todos los elementos interactivos

```html
<!-- Ejemplo de ARIA labels -->
<button class="btn btn-primary" 
        aria-label="Crear nueva factura">
    <i class="bi bi-plus"></i>
</button>

<input type="search" 
       placeholder="Buscar..." 
       aria-label="Buscar facturas">

<nav aria-label="Navegación principal">
    <!-- ... -->
</nav>

<table aria-label="Lista de facturas">
    <caption class="visually-hidden">Facturas del mes actual</caption>
    <!-- ... -->
</table>
```

- [ ] **2.3** Validar contraste de colores (WCAG AA ≥ 4.5:1)

```javascript
/**
 * Validador de contraste de colores
 */
function checkColorContrast() {
    const colors = {
        light: {
            text: '#212529',
            background: '#ffffff'
        },
        dark: {
            text: '#e6edf3',
            background: '#0d1117'
        }
    };
    
    // Función para calcular luminancia relativa
    function getLuminance(r, g, b) {
        const [rs, gs, bs] = [r, g, b].map(c => {
            c = c / 255;
            return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        });
        return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
    }
    
    // Calcular ratio de contraste
    function getContrastRatio(color1, color2) {
        const l1 = getLuminance(...hexToRgb(color1));
        const l2 = getLuminance(...hexToRgb(color2));
        const lighter = Math.max(l1, l2);
        const darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }
    
    // Validar ambos temas
    Object.entries(colors).forEach(([theme, {text, background}]) => {
        const ratio = getContrastRatio(text, background);
        const passes = ratio >= 4.5;
        console.log(`${theme}: ${ratio.toFixed(2)}:1 - ${passes ? '✅ PASA' : '❌ FALLA'}`);
    });
}
```

- [ ] **2.4** Agregar soporte para tamaño de fuente ajustable

```javascript
/**
 * Gestor de tamaño de fuente
 */
class FontSizeManager {
    
    constructor() {
        this.sizes = {
            'small': '14px',
            'normal': '16px',
            'large': '18px',
            'x-large': '20px'
        };
        this.currentSize = localStorage.getItem('font-size') || 'normal';
        this.apply();
    }
    
    apply() {
        document.documentElement.style.fontSize = this.sizes[this.currentSize];
        localStorage.setItem('font-size', this.currentSize);
    }
    
    increase() {
        const keys = Object.keys(this.sizes);
        const index = keys.indexOf(this.currentSize);
        if (index < keys.length - 1) {
            this.currentSize = keys[index + 1];
            this.apply();
        }
    }
    
    decrease() {
        const keys = Object.keys(this.sizes);
        const index = keys.indexOf(this.currentSize);
        if (index > 0) {
            this.currentSize = keys[index - 1];
            this.apply();
        }
    }
}

window.fontSizeManager = new FontSizeManager();
```

- [ ] **2.5** Implementar modo alto contraste

```css
/* Modo alto contraste */
:root[data-contrast="high"] {
    --color-primary: #0000ff;
    --bg-body: #ffffff;
    --bg-card: #ffffff;
    --text-primary: #000000;
    --border-color: #000000;
}

:root[data-theme="dark"][data-contrast="high"] {
    --color-primary: #ffff00;
    --bg-body: #000000;
    --bg-card: #000000;
    --text-primary: #ffffff;
    --border-color: #ffffff;
}
```

- [ ] **2.6** Tests con screen readers (NVDA, JAWS)
- [ ] **2.7** Audit con axe-core DevTools

---

## 📦 3. ANIMACIONES Y TRANSICIONES (5 tareas)

- [ ] **3.1** Crear animaciones de entrada para cards

```css
/* src/main/resources/static/css/animations.css */

@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(30px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.card {
    animation: fadeInUp 0.4s ease-out;
}

.card:nth-child(1) { animation-delay: 0.1s; }
.card:nth-child(2) { animation-delay: 0.2s; }
.card:nth-child(3) { animation-delay: 0.3s; }
```

- [ ] **3.2** Animaciones para botones y hover effects

```css
.btn {
    transition: all 0.3s ease;
}

.btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.btn:active {
    transform: translateY(0);
}
```

- [ ] **3.3** Transiciones suaves para modals

```css
.modal.fade .modal-dialog {
    transition: transform 0.3s ease-out;
}
```

- [ ] **3.4** Loader/spinner animado unificado

```html
<!-- Loader global -->
<div class="loader-overlay" id="global-loader">
    <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Cargando...</span>
    </div>
</div>
```

```css
.loader-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
    opacity: 0;
    visibility: hidden;
    transition: opacity 0.3s, visibility 0.3s;
}

.loader-overlay.active {
    opacity: 1;
    visibility: visible;
}
```

- [ ] **3.5** Skeleton loaders para contenido

```html
<div class="skeleton-card">
    <div class="skeleton-line"></div>
    <div class="skeleton-line"></div>
    <div class="skeleton-line short"></div>
</div>
```

---

## 📦 4. COMPONENTES UI MEJORADOS (8 tareas)

- [ ] **4.1** Tablas con paginación mejorada
- [ ] **4.2** Filtros avanzados en listados
- [ ] **4.3** Tooltips con información contextual
- [ ] **4.4** Breadcrumbs de navegación
- [ ] **4.5** Mensajes toast para feedback
- [ ] **4.6** Modals con mejor UX
- [ ] **4.7** Formularios con validación visual
- [ ] **4.8** Iconos consistentes (Bootstrap Icons)

---

## 📦 5. NAVEGACIÓN Y BREADCRUMBS (4 tareas)

- [ ] **5.1** Implementar breadcrumbs automáticos
- [ ] **5.2** Sidebar con iconos y tooltips
- [ ] **5.3** Menú de búsqueda global (Cmd+K)
- [ ] **5.4** Navegación por pestañas mejorada

---

## 📦 6. RESPONSIVE DESIGN (4 tareas)

- [ ] **6.1** Optimizar para móviles (< 768px)
- [ ] **6.2** Optimizar para tablets (768-1024px)
- [ ] **6.3** Sidebar colapsable en móvil
- [ ] **6.4** Tests en múltiples dispositivos

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ **Tema oscuro completo y funcional**  
✅ **Tema claro mejorado**  
✅ **Selector de tema con persistencia**  
✅ **Accesibilidad WCAG 2.1 AA**  
✅ **Navegación por teclado completa**  
✅ **Contraste de colores validado**  
✅ **Animaciones suaves implementadas**  
✅ **Responsive design perfeccionado**  
✅ **Breadcrumbs funcionando**  
✅ **Tooltips y ayudas contextuales**  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprints 1-8 completados
- ✅ Bootstrap 5
- ✅ Bootstrap Icons

**Habilita:**
- 🚀 Fase 2: PWA

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Frontend Team  
**Prioridad:** ALTA - Experiencia de usuario
