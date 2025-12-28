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
        
        console.log('[NavbarDropdown] Inicializando dropdown:', {
            triggerSelector,
            dropdownSelector,
            triggerFound: !!this.trigger,
            dropdownFound: !!this.dropdown
        });
        
        if (this.trigger && this.dropdown) {
            this.init();
        } else {
            console.warn('[NavbarDropdown] No se encontraron los elementos necesarios');
        }
    }

    init() {
        console.log('[NavbarDropdown] Inicializado correctamente');
        this.trigger.addEventListener('click', (e) => {
            console.log('[NavbarDropdown] Click detectado en trigger');
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
        console.log('[NavbarDropdown] Toggle llamado, isOpen:', isOpen);
        if (isOpen) {
            this.close();
        } else {
            this.open();
        }
    }

    open() {
        console.log('[NavbarDropdown] Abriendo dropdown');
        this.dropdown.classList.add('show');
        this.trigger.classList.add('active');
    }

    close() {
        console.log('[NavbarDropdown] Cerrando dropdown');
        this.dropdown.classList.remove('show');
        this.trigger.classList.remove('active');
    }
}

// ============================================================================
// LOGOUT CON CONFIRMACIÓN
// ============================================================================

async function handleLogout(event) {
    event.preventDefault();
    console.log('[Navbar] handleLogout llamado');
    console.log('[Navbar] AppUtils disponible:', typeof AppUtils !== 'undefined');
    
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
    console.log('[Navbar] DOM cargado, inicializando componentes...');
    
    // YA NO NECESARIO: Ahora usa Bootstrap dropdown nativo
    // const userDropdown = new NavbarDropdown('.navbar-user-trigger', '.navbar-dropdown');

    // Configurar botón de logout
    const logoutBtn = document.querySelector('.navbar-menu-logout');
    console.log('[Navbar] Botón logout encontrado:', logoutBtn);
    if (logoutBtn) {
        console.log('[Navbar] Agregando event listener al botón logout');
        logoutBtn.addEventListener('click', handleLogout);
    } else {
        console.error('[Navbar] No se encontró el botón de logout');
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

// ============================================================================
// DROPDOWN DE NOTIFICACIONES
// ============================================================================

class NotificacionesDropdown {
    constructor() {
        this.trigger = document.getElementById('notificacionesBtn');
        this.dropdown = document.getElementById('notificacionesDropdown');
        this.badge = document.getElementById('notificationsBadge');
        this.list = document.getElementById('notificationsList');
        this.maxNotificaciones = 5; // Mostrar solo las últimas 5 en dropdown
        
        if (this.trigger && this.dropdown) {
            this.init();
        }
    }

    init() {
        // Toggle dropdown
        this.trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            this.toggle();
        });

        // Cerrar al hacer click fuera
        document.addEventListener('click', () => {
            this.close();
        });

        // Prevenir cierre al hacer click dentro
        this.dropdown.addEventListener('click', (e) => {
            e.stopPropagation();
        });

        // Cargar notificaciones iniciales
        this.cargarNotificaciones();

        // Actualizar contador cada 30 segundos
        setInterval(() => this.actualizarContador(), 30000);
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
        // Recargar notificaciones al abrir
        this.cargarNotificaciones();
    }

    close() {
        this.dropdown.classList.remove('show');
        this.trigger.classList.remove('active');
    }

    async cargarNotificaciones() {
        try {
            const response = await fetch('/api/notificaciones/no-leidas?page=0&size=' + this.maxNotificaciones);
            
            if (!response.ok) {
                throw new Error('Error al cargar notificaciones');
            }

            const data = await response.json();
            this.renderNotificaciones(data.content || []);
            
        } catch (error) {
            console.error('Error cargando notificaciones:', error);
            this.mostrarError();
        }
    }

    renderNotificaciones(notificaciones) {
        if (notificaciones.length === 0) {
            this.list.innerHTML = `
                <div class="notifications-empty">
                    <i class="fas fa-check-circle"></i>
                    <p>No tienes notificaciones pendientes</p>
                </div>
            `;
            return;
        }

        this.list.innerHTML = notificaciones.map(n => this.crearItemHTML(n)).join('');

        // Agregar event listeners a cada item
        this.list.querySelectorAll('.notification-item').forEach(item => {
            item.addEventListener('click', () => {
                const id = item.dataset.id;
                const url = item.dataset.url;
                this.marcarComoLeida(id, url);
            });
        });
    }

    crearItemHTML(notificacion) {
        const tipoClass = this.getTipoClass(notificacion.tipo);
        const icono = this.getTipoIcono(notificacion.tipo);
        const tiempo = this.formatearTiempo(notificacion.fechaEnvio);
        const unreadClass = notificacion.leida ? '' : 'unread';

        return `
            <div class="notification-item ${unreadClass}" 
                 data-id="${notificacion.idNotificacion}"
                 data-url="${notificacion.urlAccion || '/notificaciones'}">
                <div class="notification-icon ${tipoClass}">
                    <i class="${icono}"></i>
                </div>
                <div class="notification-content">
                    <div class="notification-title">${this.escapeHtml(notificacion.titulo)}</div>
                    <div class="notification-message">${this.escapeHtml(notificacion.mensaje)}</div>
                    <div class="notification-time">
                        <i class="far fa-clock"></i>
                        ${tiempo}
                    </div>
                </div>
            </div>
        `;
    }

    getTipoClass(tipo) {
        const tipos = {
            'FACTURA_CREADA': 'type-factura-creada',
            'FACTURA_VENCIDA': 'type-factura-vencida',
            'FACTURA_PROXIMA_VENCER': 'type-factura-proxima-vencer',
            'PAGO_RECIBIDO': 'type-pago-recibido',
            'SISTEMA': 'type-sistema'
        };
        return tipos[tipo] || 'type-sistema';
    }

    getTipoIcono(tipo) {
        const iconos = {
            'FACTURA_CREADA': 'fas fa-file-invoice',
            'FACTURA_VENCIDA': 'fas fa-exclamation-circle',
            'FACTURA_PROXIMA_VENCER': 'fas fa-clock',
            'PAGO_RECIBIDO': 'fas fa-money-bill-wave',
            'SISTEMA': 'fas fa-info-circle'
        };
        return iconos[tipo] || 'fas fa-bell';
    }

    formatearTiempo(fecha) {
        const ahora = new Date();
        const fechaNotif = new Date(fecha);
        const diff = Math.floor((ahora - fechaNotif) / 1000); // segundos

        if (diff < 60) return 'Hace un momento';
        if (diff < 3600) return `Hace ${Math.floor(diff / 60)} min`;
        if (diff < 86400) return `Hace ${Math.floor(diff / 3600)} h`;
        if (diff < 604800) return `Hace ${Math.floor(diff / 86400)} d`;
        
        return fechaNotif.toLocaleDateString('es-ES', { 
            day: 'numeric', 
            month: 'short' 
        });
    }

    async marcarComoLeida(id, url) {
        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

            const response = await fetch(`/api/notificaciones/${id}/marcar-leida`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                }
            });

            if (response.ok) {
                // Actualizar contador
                this.actualizarContador();
                
                // Redirigir si hay URL
                if (url && url !== 'null' && url !== '/notificaciones') {
                    window.location.href = url;
                } else {
                    window.location.href = '/notificaciones';
                }
            }
        } catch (error) {
            console.error('Error marcando notificación como leída:', error);
        }
    }

    async actualizarContador() {
        try {
            const response = await fetch('/api/notificaciones/contador-no-leidas');
            
            if (!response.ok) throw new Error('Error al obtener contador');

            const contador = await response.json();
            this.actualizarBadge(contador);
            
        } catch (error) {
            console.error('Error actualizando contador:', error);
        }
    }

    actualizarBadge(contador) {
        if (contador > 0) {
            this.badge.textContent = contador > 99 ? '99+' : contador;
            this.badge.style.display = 'flex';
        } else {
            this.badge.style.display = 'none';
        }
    }

    mostrarError() {
        this.list.innerHTML = `
            <div class="notifications-empty">
                <i class="fas fa-exclamation-triangle"></i>
                <p>Error al cargar notificaciones</p>
            </div>
        `;
    }

    escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text ? text.replace(/[&<>"']/g, m => map[m]) : '';
    }
}

// Instancia global
let notificacionesDropdown;

// ============================================================================
// INICIALIZACIÓN ACTUALIZADA
// ============================================================================

document.addEventListener('DOMContentLoaded', function() {
    // YA NO NECESARIO: Ahora usa Bootstrap dropdown nativo
    // const userDropdown = new NavbarDropdown('.navbar-user-trigger', '.navbar-dropdown');

    // ✅ NUEVO: Inicializar dropdown de notificaciones
    notificacionesDropdown = new NotificacionesDropdown();

    // ✅ NUEVO: Conectar con WebSocket si está disponible
    if (typeof NotificacionesWebSocket !== 'undefined') {
        // Suscribirse a eventos de WebSocket
        document.addEventListener('notificacionRecibida', (event) => {
            console.log('Nueva notificación recibida vía WebSocket:', event.detail);
            // Actualizar contador y dropdown
            if (notificacionesDropdown) {
                notificacionesDropdown.actualizarContador();
                // Si el dropdown está abierto, recargar lista
                if (notificacionesDropdown.dropdown.classList.contains('show')) {
                    notificacionesDropdown.cargarNotificaciones();
                }
            }
        });

        document.addEventListener('contadorActualizado', (event) => {
            console.log('Contador actualizado vía WebSocket:', event.detail);
            if (notificacionesDropdown) {
                notificacionesDropdown.actualizarBadge(event.detail.contador);
            }
        });
    }

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
