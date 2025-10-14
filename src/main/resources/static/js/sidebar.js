/**
 * ============================================================================
 * SIDEBAR.JS - Funcionalidad del Menú Lateral
 * WhatsApp Orders Manager
 * ============================================================================
 */

// ============================================================================
// CLASE SIDEBAR
// ============================================================================

class Sidebar {
    constructor() {
        this.sidebar = document.querySelector('.sidebar');
        this.sidebarToggle = document.querySelector('.sidebar-toggle');
        this.sidebarOverlay = document.querySelector('.sidebar-overlay');
        this.collapseBtn = document.querySelector('.sidebar-collapse-btn');
        this.mainContent = document.querySelector('.main-content');
        
        this.isMobile = window.innerWidth < 768;
        this.isCollapsed = this.loadCollapsedState();

        this.init();
    }

    init() {
        if (!this.sidebar) return;

        // Aplicar estado colapsado inicial (solo desktop)
        if (!this.isMobile && this.isCollapsed) {
            this.collapse();
        }

        // Event listeners
        this.setupEventListeners();
        
        // Resize handler
        window.addEventListener('resize', () => this.handleResize());

        // Resaltar módulo activo
        this.highlightActiveModule();
    }

    setupEventListeners() {
        // Toggle móvil
        if (this.sidebarToggle) {
            this.sidebarToggle.addEventListener('click', () => this.toggleMobile());
        }

        // Overlay para cerrar en móvil
        if (this.sidebarOverlay) {
            this.sidebarOverlay.addEventListener('click', () => this.closeMobile());
        }

        // Botón de colapso desktop
        if (this.collapseBtn) {
            this.collapseBtn.addEventListener('click', () => this.toggleCollapse());
        }

        // Cerrar móvil al navegar
        const menuLinks = this.sidebar.querySelectorAll('.menu-link:not(.disabled)');
        menuLinks.forEach(link => {
            link.addEventListener('click', () => {
                if (this.isMobile) {
                    this.closeMobile();
                }
            });
        });

        // Prevenir click en módulos deshabilitados
        const disabledLinks = this.sidebar.querySelectorAll('.menu-link.disabled');
        disabledLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                AppUtils.showToast('info', 'Este módulo estará disponible próximamente');
            });
        });
    }

    toggleMobile() {
        const isOpen = this.sidebar.classList.contains('show');
        if (isOpen) {
            this.closeMobile();
        } else {
            this.openMobile();
        }
    }

    openMobile() {
        this.sidebar.classList.add('show');
        this.sidebarOverlay.classList.add('show');
        document.body.style.overflow = 'hidden';
    }

    closeMobile() {
        this.sidebar.classList.remove('show');
        this.sidebarOverlay.classList.remove('show');
        document.body.style.overflow = '';
    }

    toggleCollapse() {
        if (this.isCollapsed) {
            this.expand();
        } else {
            this.collapse();
        }
    }

    collapse() {
        this.sidebar.classList.add('collapsed');
        this.mainContent.classList.add('sidebar-collapsed');
        this.isCollapsed = true;
        this.saveCollapsedState(true);
    }

    expand() {
        this.sidebar.classList.remove('collapsed');
        this.mainContent.classList.remove('sidebar-collapsed');
        this.isCollapsed = false;
        this.saveCollapsedState(false);
    }

    handleResize() {
        const wasMobile = this.isMobile;
        this.isMobile = window.innerWidth < 768;

        // Si cambió de móvil a desktop o viceversa
        if (wasMobile !== this.isMobile) {
            if (this.isMobile) {
                // Cambió a móvil: cerrar sidebar
                this.closeMobile();
                this.expand(); // Quitar estado collapsed
            } else {
                // Cambió a desktop: aplicar estado guardado
                if (this.isCollapsed) {
                    this.collapse();
                }
            }
        }
    }

    highlightActiveModule() {
        const currentPath = window.location.pathname;
        const menuLinks = this.sidebar.querySelectorAll('.menu-link');

        menuLinks.forEach(link => {
            const href = link.getAttribute('href');
            
            // Verificar si la ruta actual coincide
            if (href && currentPath.includes(href) && href !== '/') {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    saveCollapsedState(isCollapsed) {
        try {
            localStorage.setItem('sidebar-collapsed', isCollapsed);
        } catch (e) {
            console.warn('No se pudo guardar el estado del sidebar');
        }
    }

    loadCollapsedState() {
        try {
            const saved = localStorage.getItem('sidebar-collapsed');
            return saved === 'true';
        } catch (e) {
            return false;
        }
    }
}

// ============================================================================
// CONFIGURACIÓN DE MÓDULOS
// ============================================================================

const MODULES_CONFIG = {
    clientes: {
        name: 'Clientes',
        icon: 'fa-users',
        color: '#2196F3',
        enabled: true,
        path: '/clientes'
    },
    productos: {
        name: 'Productos',
        icon: 'fa-box',
        color: '#FF9800',
        enabled: true,
        path: '/productos'
    },
    facturas: {
        name: 'Facturas',
        icon: 'fa-file-invoice',
        color: '#4CAF50',
        enabled: true,
        path: '/facturas'
    },
    pedidos: {
        name: 'Pedidos',
        icon: 'fa-shopping-cart',
        color: '#9C27B0',
        enabled: false,
        path: '/pedidos'
    },
    reportes: {
        name: 'Reportes',
        icon: 'fa-chart-bar',
        color: '#009688',
        enabled: false,
        path: '/reportes'
    },
    inventario: {
        name: 'Inventario',
        icon: 'fa-warehouse',
        color: '#3F51B5',
        enabled: false,
        path: '/inventario'
    },
    usuarios: {
        name: 'Usuarios',
        icon: 'fa-user-cog',
        color: '#607D8B',
        enabled: false,
        path: '/usuarios',
        adminOnly: true
    },
    config: {
        name: 'Configuración',
        icon: 'fa-cog',
        color: '#9E9E9E',
        enabled: false,
        path: '/configuracion',
        adminOnly: true
    }
};

/**
 * Verificar si el usuario tiene acceso a un módulo
 * @param {string} moduleKey - Clave del módulo
 * @param {string} userRole - Rol del usuario (ADMIN, USER, CLIENTE)
 * @returns {boolean}
 */
function hasModuleAccess(moduleKey, userRole) {
    const module = MODULES_CONFIG[moduleKey];
    if (!module) return false;

    // Si requiere admin y no es admin
    if (module.adminOnly && userRole !== 'ADMIN') {
        return false;
    }

    return true;
}

// ============================================================================
// INICIALIZACIÓN
// ============================================================================

let sidebarInstance;

document.addEventListener('DOMContentLoaded', function() {
    // Inicializar sidebar
    sidebarInstance = new Sidebar();

    console.log('Sidebar inicializado');
});

// Exportar para uso global
window.SidebarApp = {
    instance: () => sidebarInstance,
    modules: MODULES_CONFIG,
    hasAccess: hasModuleAccess
};
