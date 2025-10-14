/**
 * ============================================================================
 * DASHBOARD.JS - Funcionalidad del Dashboard Principal
 * WhatsApp Orders Manager
 * ============================================================================
 */

// ============================================================================
// CONFIGURACIÓN
// ============================================================================

const DASHBOARD_CONFIG = {
    refreshInterval: 60000, // 1 minuto
    animationDuration: 300
};

// ============================================================================
// CLASE DASHBOARD
// ============================================================================

class Dashboard {
    constructor() {
        this.statsWidgets = document.querySelectorAll('.stat-widget');
        this.refreshTimer = null;
        this.init();
    }

    init() {
        console.log('Dashboard inicializado');
        
        // Cargar estadísticas
        this.loadStatistics();

        // Configurar auto-refresh
        this.startAutoRefresh();

        // Configurar acciones rápidas
        this.setupQuickActions();
    }

    /**
     * Cargar estadísticas desde el servidor
     */
    async loadStatistics() {
        try {
            // Mostrar skeleton loading
            this.showSkeletonLoading();

            // Hacer petición al endpoint
            const stats = await AppUtils.fetchWithCsrf('/api/dashboard/statistics');

            // Actualizar widgets con animación
            await this.updateStatistics(stats);

        } catch (error) {
            console.error('Error al cargar estadísticas:', error);
            AppUtils.showToast('error', 'Error al cargar las estadísticas');
            this.hideSkeletonLoading();
        }
    }

    /**
     * Mostrar skeleton loading en widgets
     */
    showSkeletonLoading() {
        this.statsWidgets.forEach(widget => {
            widget.classList.add('loading');
        });
    }

    /**
     * Ocultar skeleton loading
     */
    hideSkeletonLoading() {
        this.statsWidgets.forEach(widget => {
            widget.classList.remove('loading');
        });
    }

    /**
     * Actualizar estadísticas con animación
     * @param {Object} stats - Objeto con las estadísticas
     */
    async updateStatistics(stats) {
        // Simular delay para animación suave
        await new Promise(resolve => setTimeout(resolve, 500));

        // Actualizar cada widget
        this.updateWidget('clientes', stats.totalClientes || 0, stats.clientesTrend);
        this.updateWidget('productos', stats.totalProductos || 0, stats.productosTrend);
        this.updateWidget('facturas', stats.facturasHoy || 0, stats.facturasTrend);
        this.updateWidget('pagos', stats.pagosPendientes || 0, stats.pagosTrend);

        this.hideSkeletonLoading();
    }

    /**
     * Actualizar un widget específico
     * @param {string} type - Tipo de widget
     * @param {number} value - Valor a mostrar
     * @param {Object} trend - Tendencia {direction: 'up'|'down'|'neutral', percent: number}
     */
    updateWidget(type, value, trend = null) {
        const widget = document.querySelector(`.stat-widget.${type}`);
        if (!widget) return;

        const valueElement = widget.querySelector('.stat-value');
        if (valueElement) {
            this.animateValue(valueElement, 0, value, 1000);
        }

        // Actualizar tendencia si está disponible
        if (trend && trend.direction) {
            const trendElement = widget.querySelector('.stat-trend');
            if (trendElement) {
                trendElement.className = `stat-trend ${trend.direction}`;
                
                const icon = trend.direction === 'up' ? 'fa-arrow-up' : 
                            trend.direction === 'down' ? 'fa-arrow-down' : 
                            'fa-minus';
                
                trendElement.innerHTML = `
                    <i class="fas ${icon}"></i>
                    ${trend.percent || 0}%
                `;
            }
        }
    }

    /**
     * Animar contador de números
     * @param {HTMLElement} element - Elemento a animar
     * @param {number} start - Valor inicial
     * @param {number} end - Valor final
     * @param {number} duration - Duración en ms
     */
    animateValue(element, start, end, duration) {
        const range = end - start;
        const increment = range / (duration / 16); // 60 FPS
        let current = start;

        const timer = setInterval(() => {
            current += increment;
            
            if ((increment > 0 && current >= end) || (increment < 0 && current <= end)) {
                element.textContent = Math.floor(end).toLocaleString('es-MX');
                clearInterval(timer);
            } else {
                element.textContent = Math.floor(current).toLocaleString('es-MX');
            }
        }, 16);
    }

    /**
     * Iniciar auto-refresh de estadísticas
     */
    startAutoRefresh() {
        this.refreshTimer = setInterval(() => {
            this.loadStatistics();
        }, DASHBOARD_CONFIG.refreshInterval);
    }

    /**
     * Detener auto-refresh
     */
    stopAutoRefresh() {
        if (this.refreshTimer) {
            clearInterval(this.refreshTimer);
            this.refreshTimer = null;
        }
    }

    /**
     * Configurar acciones rápidas
     */
    setupQuickActions() {
        const actionCards = document.querySelectorAll('.action-card');
        
        actionCards.forEach(card => {
            card.addEventListener('click', function(e) {
                const module = this.dataset.module;
                console.log(`Acción rápida: ${module}`);
            });
        });
    }
}

// ============================================================================
// ACTIVIDAD RECIENTE
// ============================================================================

class RecentActivity {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        if (this.container) {
            this.loadActivities();
        }
    }

    async loadActivities() {
        try {
            const activities = await AppUtils.fetchWithCsrf('/api/dashboard/recent-activity');
            this.renderActivities(activities);
        } catch (error) {
            console.error('Error al cargar actividades:', error);
        }
    }

    renderActivities(activities) {
        if (!activities || activities.length === 0) {
            this.container.innerHTML = `
                <div class="text-center text-muted py-4">
                    <i class="fas fa-inbox fa-3x mb-3"></i>
                    <p>No hay actividad reciente</p>
                </div>
            `;
            return;
        }

        const html = activities.map(activity => `
            <div class="activity-item">
                <div class="activity-icon ${activity.type}">
                    <i class="fas ${this.getActivityIcon(activity.type)}"></i>
                </div>
                <div class="activity-content">
                    <div class="activity-text">${activity.text}</div>
                    <div class="activity-time">${this.formatTime(activity.timestamp)}</div>
                </div>
            </div>
        `).join('');

        this.container.innerHTML = html;
    }

    getActivityIcon(type) {
        const icons = {
            'success': 'fa-check-circle',
            'info': 'fa-info-circle',
            'warning': 'fa-exclamation-circle',
            'error': 'fa-times-circle'
        };
        return icons[type] || 'fa-circle';
    }

    formatTime(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (minutes < 1) return 'Hace un momento';
        if (minutes < 60) return `Hace ${minutes} minuto${minutes > 1 ? 's' : ''}`;
        if (hours < 24) return `Hace ${hours} hora${hours > 1 ? 's' : ''}`;
        if (days < 7) return `Hace ${days} día${days > 1 ? 's' : ''}`;
        
        return AppUtils.formatDate(date, 'short');
    }
}

// ============================================================================
// GRÁFICAS (Chart.js) - Para implementación futura
// ============================================================================

class DashboardCharts {
    constructor() {
        // Preparado para Sprint futuro con gráficas
        console.log('DashboardCharts preparado para implementación futura');
    }

    initSalesChart() {
        // TODO: Implementar gráfica de ventas con Chart.js
    }

    initProductsChart() {
        // TODO: Implementar gráfica de productos
    }
}

// ============================================================================
// FUNCIONES GLOBALES PARA MÓDULOS
// ============================================================================

/**
 * Manejar click en una tarjeta de módulo
 * @param {HTMLElement} element - Elemento del módulo clickeado
 */
function handleModuleClick(element) {
    const activo = element.getAttribute('data-activo') === 'true';
    const ruta = element.getAttribute('data-ruta');
    const nombre = element.getAttribute('data-nombre');
    
    if (activo) {
        navegarModulo(ruta);
    } else {
        moduloNoDisponible(nombre);
    }
}

/**
 * Mostrar alerta cuando se hace clic en un módulo no disponible
 * @param {string} nombreModulo - Nombre del módulo
 */
function moduloNoDisponible(nombreModulo) {
    Swal.fire({
        icon: 'info',
        title: 'Módulo en desarrollo',
        text: `El módulo "${nombreModulo}" estará disponible próximamente`,
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#2196F3',
        showClass: {
            popup: 'animate__animated animate__fadeInDown'
        },
        hideClass: {
            popup: 'animate__animated animate__fadeOutUp'
        }
    });
}

/**
 * Navegar a un módulo activo
 * @param {string} ruta - Ruta del módulo
 */
function navegarModulo(ruta) {
    if (ruta && ruta !== '#') {
        window.location.href = ruta;
    }
}

// ============================================================================
// INICIALIZACIÓN
// ============================================================================

let dashboardInstance;
let activityInstance;

document.addEventListener('DOMContentLoaded', function() {
    // Solo inicializar si estamos en la página del dashboard
    if (document.querySelector('.dashboard-container')) {
        dashboardInstance = new Dashboard();
        activityInstance = new RecentActivity('activityList');
        
        console.log('Dashboard y actividades inicializados');
    }
    
    // Inicializar tooltips de Bootstrap si existen
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
});

// Limpiar al salir de la página
window.addEventListener('beforeunload', function() {
    if (dashboardInstance) {
        dashboardInstance.stopAutoRefresh();
    }
});

// Exportar para uso global
window.DashboardApp = {
    instance: () => dashboardInstance,
    activity: () => activityInstance
};
