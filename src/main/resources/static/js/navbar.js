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
    const segments = path.split('/').filter(s => s);
    
    // Limpiar breadcrumbs actuales
    breadcrumbsContainer.innerHTML = '';

    // Agregar Home
    addBreadcrumb(breadcrumbsContainer, 'Dashboard', '/dashboard', false);

    // Mapeo de rutas a nombres amigables
    const routeNames = {
        'clientes': 'Clientes',
        'productos': 'Productos',
        'facturas': 'Facturas',
        'pedidos': 'Pedidos',
        'perfil': 'Mi Perfil',
        'form': 'Formulario',
        'editar': 'Editar',
        'nuevo': 'Nuevo'
    };

    // Construir breadcrumbs
    let currentPath = '';
    segments.forEach((segment, index) => {
        currentPath += '/' + segment;
        const name = routeNames[segment] || segment;
        const isLast = index === segments.length - 1;
        
        addBreadcrumb(breadcrumbsContainer, name, currentPath, isLast);
    });
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
