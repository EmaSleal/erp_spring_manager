/**
 * ============================================================================
 * NAVBAR.JS - Funcionalidad de la Barra de Navegación
 * WhatsApp Orders Manager
 * ============================================================================
 */

// ============================================================================
// DROPDOWN DE USUARIO
// ============================================================================

class NavbarDropdown {
    constructor(triggerSelector, dropdownSelector) {
        this.trigger = document.querySelector(triggerSelector);
        this.dropdown = document.querySelector(dropdownSelector);
        
        if (this.trigger && this.dropdown) {
            this.init();
        }
    }

    init() {
        this.trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            this.toggle();
        });

        // Cerrar al hacer click fuera
        document.addEventListener('click', () => {
            this.close();
        });

        // Prevenir cierre al hacer click dentro del dropdown
        this.dropdown.addEventListener('click', (e) => {
            e.stopPropagation();
        });
    }

    toggle() {
        const isOpen = this.dropdown.classList.contains('show');
        if (isOpen) {
            this.close();
        } else {
            this.open();
        }
    }

    open() {
        this.dropdown.classList.add('show');
        this.trigger.classList.add('active');
    }

    close() {
        this.dropdown.classList.remove('show');
        this.trigger.classList.remove('active');
    }
}

// ============================================================================
// LOGOUT CON CONFIRMACIÓN
// ============================================================================

async function handleLogout(event) {
    event.preventDefault();
    
    const confirmed = await AppUtils.showConfirmDialog(
        '¿Cerrar sesión?',
        'Estás a punto de cerrar tu sesión. ¿Deseas continuar?',
        'Sí, cerrar sesión'
    );

    if (confirmed) {
        // Mostrar loading
        AppUtils.showLoading();
        
        // Crear formulario para logout (POST request)
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = '/logout';  // Spring Security espera /logout por defecto
        
        // Agregar CSRF token
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        
        if (csrfToken) {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';  // Spring Security espera el parámetro "_csrf"
            input.value = csrfToken;
            form.appendChild(input);
        }
        
        document.body.appendChild(form);
        form.submit();
    }
}

// ============================================================================
// BREADCRUMBS DINÁMICOS
// ============================================================================

function updateBreadcrumbs() {
    const breadcrumbsContainer = document.querySelector('.breadcrumbs');
    if (!breadcrumbsContainer) return;

    const path = window.location.pathname;
    const queryString = window.location.search;
    const urlParams = new URLSearchParams(queryString);
    
    // Limpiar breadcrumbs actuales
    breadcrumbsContainer.innerHTML = '';

    // Agregar Home (siempre primero)
    addBreadcrumb(breadcrumbsContainer, 'Dashboard', '/dashboard', false);

    // Mapeo completo de rutas del sistema
    const routeNames = {
        // Módulo Clientes
        'clientes': 'Clientes',
        
        // Módulo Productos
        'productos': 'Productos',
        
        // Módulo Facturas
        'facturas': 'Facturas',
        'ver': 'Ver Detalle',
        
        // Módulo Configuración
        'configuracion': 'Configuración',
        'empresa': 'Empresa',
        'facturacion': 'Facturación',
        'notificaciones': 'Notificaciones',
        
        // Módulo Usuarios
        'usuarios': 'Usuarios',
        
        // Módulo Reportes
        'reportes': 'Reportes',
        'ventas': 'Reporte de Ventas',
        
        // Módulo Perfil
        'perfil': 'Mi Perfil',
        
        // Acciones comunes
        'form': 'Nuevo',
        'editar': 'Editar',
        'nuevo': 'Nuevo',
        'save': 'Guardar',
        'delete': 'Eliminar'
    };

    // Rutas especiales con manejo personalizado
    const segments = path.split('/').filter(s => s);
    
    // Casos especiales por ruta completa
    if (path === '/dashboard') {
        // Solo Dashboard (ya agregado arriba, no hacer nada más)
        return;
    }
    
    // === MÓDULO CLIENTES ===
    if (path.startsWith('/clientes')) {
        addBreadcrumb(breadcrumbsContainer, 'Clientes', '/clientes', false);
        
        if (path === '/clientes/form') {
            addBreadcrumb(breadcrumbsContainer, 'Nuevo Cliente', path, true);
        } else if (path.match(/\/clientes\/form\/\d+/)) {
            const id = path.split('/').pop();
            addBreadcrumb(breadcrumbsContainer, `Editar Cliente #${id}`, path, true);
        }
        return;
    }
    
    // === MÓDULO PRODUCTOS ===
    if (path.startsWith('/productos')) {
        addBreadcrumb(breadcrumbsContainer, 'Productos', '/productos', false);
        
        if (path === '/productos/form') {
            addBreadcrumb(breadcrumbsContainer, 'Nuevo Producto', path, true);
        } else if (path.match(/\/productos\/form\/\d+/)) {
            const id = path.split('/').pop();
            addBreadcrumb(breadcrumbsContainer, `Editar Producto #${id}`, path, true);
        }
        return;
    }
    
    // === MÓDULO FACTURAS ===
    if (path.startsWith('/facturas')) {
        addBreadcrumb(breadcrumbsContainer, 'Facturas', '/facturas', false);
        
        if (path === '/facturas/form') {
            addBreadcrumb(breadcrumbsContainer, 'Nueva Factura', path, true);
        } else if (path.match(/\/facturas\/editar\/\d+/)) {
            const id = path.split('/').pop();
            addBreadcrumb(breadcrumbsContainer, `Editar Factura #${id}`, path, true);
        } else if (path.match(/\/facturas\/ver\/\d+/)) {
            const id = path.split('/').pop();
            addBreadcrumb(breadcrumbsContainer, `Ver Factura #${id}`, path, true);
        }
        return;
    }
    
    // === MÓDULO CONFIGURACIÓN ===
    if (path.startsWith('/configuracion')) {
        addBreadcrumb(breadcrumbsContainer, 'Configuración', '/configuracion', false);
        
        // Detectar tab activo desde parámetro ?tab=
        const tab = urlParams.get('tab');
        if (tab === 'empresa') {
            addBreadcrumb(breadcrumbsContainer, 'Empresa', `${path}?tab=empresa`, true);
        } else if (tab === 'facturacion') {
            addBreadcrumb(breadcrumbsContainer, 'Facturación', `${path}?tab=facturacion`, true);
        } else if (tab === 'notificaciones') {
            addBreadcrumb(breadcrumbsContainer, 'Notificaciones', `${path}?tab=notificaciones`, true);
        } else {
            // Tab por defecto (empresa)
            addBreadcrumb(breadcrumbsContainer, 'Empresa', path, true);
        }
        return;
    }
    
    // === MÓDULO USUARIOS ===
    if (path.startsWith('/usuarios')) {
        addBreadcrumb(breadcrumbsContainer, 'Usuarios', '/usuarios', false);
        
        if (path === '/usuarios/form') {
            addBreadcrumb(breadcrumbsContainer, 'Nuevo Usuario', path, true);
        } else if (path.match(/\/usuarios\/form\/\d+/)) {
            const id = path.split('/').pop();
            addBreadcrumb(breadcrumbsContainer, `Editar Usuario #${id}`, path, true);
        }
        return;
    }
    
    // === MÓDULO REPORTES ===
    if (path.startsWith('/reportes')) {
        addBreadcrumb(breadcrumbsContainer, 'Reportes', '/reportes', false);
        
        if (path === '/reportes/ventas') {
            addBreadcrumb(breadcrumbsContainer, 'Reporte de Ventas', path, true);
        } else if (path === '/reportes/clientes') {
            addBreadcrumb(breadcrumbsContainer, 'Reporte de Clientes', path, true);
        } else if (path === '/reportes/productos') {
            addBreadcrumb(breadcrumbsContainer, 'Reporte de Productos', path, true);
        }
        return;
    }
    
    // === MÓDULO PERFIL ===
    if (path.startsWith('/perfil')) {
        addBreadcrumb(breadcrumbsContainer, 'Mi Perfil', '/perfil', false);
        
        if (path === '/perfil/editar') {
            addBreadcrumb(breadcrumbsContainer, 'Editar Perfil', path, true);
        }
        return;
    }
    
    // === FALLBACK: Construcción genérica para rutas no mapeadas ===
    let currentPath = '';
    segments.forEach((segment, index) => {
        // Saltar dashboard ya que siempre está primero
        if (segment === 'dashboard') return;
        
        currentPath += '/' + segment;
        
        // Si el segmento es un número (ID), mostrarlo como #ID
        const name = /^\d+$/.test(segment) 
            ? `#${segment}` 
            : (routeNames[segment] || capitalizeFirst(segment));
        
        const isLast = index === segments.length - 1;
        addBreadcrumb(breadcrumbsContainer, name, currentPath, isLast);
    });
}

/**
 * Capitalizar primera letra de un string
 */
function capitalizeFirst(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}

function addBreadcrumb(container, name, path, isActive) {
    const item = document.createElement('div');
    item.className = 'breadcrumb-item' + (isActive ? ' active' : '');

    if (!isActive) {
        const link = document.createElement('a');
        link.href = path;
        link.textContent = name;
        item.appendChild(link);

        const separator = document.createElement('span');
        separator.className = 'breadcrumb-separator';
        separator.innerHTML = '<i class="fas fa-chevron-right"></i>';
        item.appendChild(separator);
    } else {
        item.textContent = name;
    }

    container.appendChild(item);
}

// ============================================================================
// INICIALIZACIÓN
// ============================================================================

document.addEventListener('DOMContentLoaded', function() {
    // Inicializar dropdown de usuario
    const userDropdown = new NavbarDropdown('.navbar-user-trigger', '.navbar-dropdown');

    // Configurar botón de logout
    const logoutBtn = document.querySelector('.navbar-menu-logout');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }

    // Actualizar breadcrumbs
    updateBreadcrumbs();

    // Resaltar elemento activo en dropdown según ruta actual
    highlightActiveMenuItem();
});

/**
 * Resaltar elemento activo del menú
 */
function highlightActiveMenuItem() {
    const currentPath = window.location.pathname;
    const menuLinks = document.querySelectorAll('.navbar-menu-link');

    menuLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href && currentPath.includes(href) && href !== '/') {
            link.classList.add('active');
        }
    });
}
