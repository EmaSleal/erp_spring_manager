/**
 * ============================================================================
 * NOTIFICACIONES.JS - Gestión de Notificaciones (Página Lista)
 * ERP Orders Manager - Sprint 4 Fase 3.7
 * ============================================================================
 * Funcionalidad para la página de lista de notificaciones:
 * - Carga paginada de notificaciones
 * - Filtrado por tipo y estado
 * - Marcar como leída (individual y todas)
 * - Eliminar notificaciones
 * - Navegación entre páginas
 * ============================================================================
 */

class NotificacionesPage {
    constructor() {
        this.container = document.getElementById('notificacionesContainer');
        this.paginacionContainer = document.getElementById('paginacionContainer');
        this.paginacion = document.getElementById('paginacion');
        this.paginacionInfo = document.getElementById('paginacionInfo');
        
        this.filtroTipo = document.getElementById('filtroTipo');
        this.filtroEstado = document.getElementById('filtroEstado');
        this.btnAplicarFiltros = document.getElementById('btnAplicarFiltros');
        this.btnMarcarTodas = document.getElementById('marcarTodasLeidas');
        
        this.pageActual = 0;
        this.pageSize = 10;
        this.totalPages = 0;
        this.totalElements = 0;
        
        this.init();
    }

    init() {
        // Cargar notificaciones iniciales
        this.cargarNotificaciones();

        // Event listeners
        this.btnAplicarFiltros?.addEventListener('click', () => {
            this.pageActual = 0;
            this.cargarNotificaciones();
        });

        this.btnMarcarTodas?.addEventListener('click', () => {
            this.mostrarModalConfirmacion();
        });

        // Confirmar marcar todas
        document.getElementById('confirmarMarcarTodas')?.addEventListener('click', () => {
            this.marcarTodasComoLeidas();
        });
    }

    async cargarNotificaciones() {
        try {
            this.mostrarCargando();

            // Construir URL con filtros
            const params = new URLSearchParams({
                page: this.pageActual,
                size: this.pageSize
            });

            // Agregar filtros si existen
            const tipo = this.filtroTipo?.value;
            const leida = this.filtroEstado?.value;

            if (tipo) params.append('tipo', tipo);
            if (leida !== '') params.append('leida', leida);

            const response = await fetch(`/api/notificaciones?${params.toString()}`);
            
            if (!response.ok) {
                throw new Error('Error al cargar notificaciones');
            }

            const data = await response.json();
            this.renderNotificaciones(data);
            
        } catch (error) {
            console.error('Error cargando notificaciones:', error);
            this.mostrarError();
        }
    }

    renderNotificaciones(data) {
        this.totalPages = data.totalPages;
        this.totalElements = data.totalElements;

        if (data.content.length === 0) {
            this.mostrarVacio();
            this.paginacionContainer.style.display = 'none';
            return;
        }

        // Renderizar notificaciones
        this.container.innerHTML = data.content.map(n => this.crearItemHTML(n)).join('');

        // Agregar event listeners
        this.container.querySelectorAll('.notification-item-page').forEach(item => {
            const id = item.dataset.id;
            const url = item.dataset.url;

            item.addEventListener('click', (e) => {
                // Si se hizo click en un botón de acción, no navegar
                if (e.target.closest('.notification-actions')) {
                    return;
                }
                this.marcarYNavegar(id, url);
            });

            // Botón de marcar como leída
            const btnMarcar = item.querySelector('.btn-marcar-leida');
            if (btnMarcar) {
                btnMarcar.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.marcarComoLeida(id);
                });
            }

            // Botón de eliminar
            const btnEliminar = item.querySelector('.btn-eliminar');
            if (btnEliminar) {
                btnEliminar.addEventListener('click', (e) => {
                    e.stopPropagation();
                    this.eliminarNotificacion(id);
                });
            }
        });

        // Actualizar paginación
        this.renderPaginacion();
        this.actualizarInfo();
    }

    crearItemHTML(notificacion) {
        const tipoClass = this.getTipoClass(notificacion.tipo);
        const icono = this.getTipoIcono(notificacion.tipo);
        const tiempo = this.formatearTiempo(notificacion.fechaEnvio);
        const unreadClass = notificacion.leida ? '' : 'unread';
        const badgeTipo = this.getTipoBadge(notificacion.tipo);

        return `
            <div class="notification-item-page ${unreadClass}" 
                 data-id="${notificacion.idNotificacion}"
                 data-url="${notificacion.urlAccion || '/notificaciones'}">
                <div class="notification-icon-page ${tipoClass}">
                    <i class="${icono}"></i>
                </div>
                <div class="notification-content-page">
                    <div class="notification-header-page">
                        <div class="notification-title-page">
                            ${this.escapeHtml(notificacion.titulo)}
                        </div>
                        <span class="badge ${badgeTipo.class}">${badgeTipo.text}</span>
                    </div>
                    <div class="notification-message-page">
                        ${this.escapeHtml(notificacion.mensaje)}
                    </div>
                    <div class="notification-footer-page">
                        <div class="notification-time-page">
                            <i class="far fa-clock"></i>
                            ${tiempo}
                            ${!notificacion.leida ? '<span class="badge bg-primary ms-2">Nueva</span>' : ''}
                        </div>
                        <div class="notification-actions">
                            ${!notificacion.leida ? `
                                <button class="btn btn-sm btn-outline-primary btn-notification-action btn-marcar-leida" title="Marcar como leída">
                                    <i class="fas fa-check"></i>
                                </button>
                            ` : ''}
                            <button class="btn btn-sm btn-outline-danger btn-notification-action btn-eliminar" title="Eliminar">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
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

    getTipoBadge(tipo) {
        const badges = {
            'FACTURA_CREADA': { text: 'Factura Creada', class: 'bg-success' },
            'FACTURA_VENCIDA': { text: 'Vencida', class: 'bg-danger' },
            'FACTURA_PROXIMA_VENCER': { text: 'Por Vencer', class: 'bg-warning' },
            'PAGO_RECIBIDO': { text: 'Pago', class: 'bg-info' },
            'SISTEMA': { text: 'Sistema', class: 'bg-secondary' }
        };
        return badges[tipo] || { text: 'Notificación', class: 'bg-secondary' };
    }

    formatearTiempo(fecha) {
        const ahora = new Date();
        const fechaNotif = new Date(fecha);
        const diff = Math.floor((ahora - fechaNotif) / 1000); // segundos

        if (diff < 60) return 'Hace un momento';
        if (diff < 3600) return `Hace ${Math.floor(diff / 60)} minutos`;
        if (diff < 86400) return `Hace ${Math.floor(diff / 3600)} horas`;
        if (diff < 604800) return `Hace ${Math.floor(diff / 86400)} días`;
        
        return fechaNotif.toLocaleDateString('es-ES', { 
            day: 'numeric', 
            month: 'long',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    async marcarComoLeida(id) {
        try {
            const response = await fetch(`/api/notificaciones/${id}/marcar-leida`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                }
            });

            if (response.ok) {
                // Recargar lista
                this.cargarNotificaciones();
                // Actualizar badge del navbar
                if (typeof notificacionesDropdown !== 'undefined') {
                    notificacionesDropdown.actualizarContador();
                }
            }
        } catch (error) {
            console.error('Error marcando como leída:', error);
        }
    }

    async marcarYNavegar(id, url) {
        await this.marcarComoLeida(id);
        if (url && url !== 'null' && url !== '/notificaciones') {
            window.location.href = url;
        }
    }

    async marcarTodasComoLeidas() {
        try {
            const response = await fetch('/api/notificaciones/marcar-todas-leidas', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                }
            });

            if (response.ok) {
                // Cerrar modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('confirmarModal'));
                modal?.hide();

                // Recargar lista
                this.cargarNotificaciones();

                // Actualizar badge
                if (typeof notificacionesDropdown !== 'undefined') {
                    notificacionesDropdown.actualizarBadge(0);
                }

                // Mostrar mensaje de éxito
                this.mostrarMensaje('Todas las notificaciones han sido marcadas como leídas', 'success');
            }
        } catch (error) {
            console.error('Error marcando todas como leídas:', error);
            this.mostrarMensaje('Error al marcar las notificaciones', 'error');
        }
    }

    async eliminarNotificacion(id) {
        if (!confirm('¿Estás seguro de que deseas eliminar esta notificación?')) {
            return;
        }

        try {
            const response = await fetch(`/api/notificaciones/${id}`, {
                method: 'DELETE',
                headers: {
                    [csrfHeader]: csrfToken
                }
            });

            if (response.ok) {
                this.cargarNotificaciones();
                this.mostrarMensaje('Notificación eliminada', 'success');
            }
        } catch (error) {
            console.error('Error eliminando notificación:', error);
            this.mostrarMensaje('Error al eliminar la notificación', 'error');
        }
    }

    renderPaginacion() {
        if (this.totalPages <= 1) {
            this.paginacionContainer.style.display = 'none';
            return;
        }

        this.paginacionContainer.style.display = 'flex';

        let html = '';

        // Botón anterior
        html += `
            <li class="page-item ${this.pageActual === 0 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${this.pageActual - 1}">
                    <i class="fas fa-chevron-left"></i>
                </a>
            </li>
        `;

        // Páginas
        const maxPaginas = 5;
        let inicio = Math.max(0, this.pageActual - Math.floor(maxPaginas / 2));
        let fin = Math.min(this.totalPages, inicio + maxPaginas);

        if (fin - inicio < maxPaginas) {
            inicio = Math.max(0, fin - maxPaginas);
        }

        for (let i = inicio; i < fin; i++) {
            html += `
                <li class="page-item ${i === this.pageActual ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
                </li>
            `;
        }

        // Botón siguiente
        html += `
            <li class="page-item ${this.pageActual >= this.totalPages - 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${this.pageActual + 1}">
                    <i class="fas fa-chevron-right"></i>
                </a>
            </li>
        `;

        this.paginacion.innerHTML = html;

        // Event listeners para paginación
        this.paginacion.querySelectorAll('.page-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const page = parseInt(e.target.closest('.page-link').dataset.page);
                if (!isNaN(page) && page >= 0 && page < this.totalPages) {
                    this.pageActual = page;
                    this.cargarNotificaciones();
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                }
            });
        });
    }

    actualizarInfo() {
        const inicio = this.pageActual * this.pageSize + 1;
        const fin = Math.min((this.pageActual + 1) * this.pageSize, this.totalElements);
        this.paginacionInfo.textContent = `${inicio}-${fin} de ${this.totalElements}`;
    }

    mostrarCargando() {
        this.container.innerHTML = `
            <div class="notifications-loading-page">
                <i class="fas fa-spinner fa-spin fa-3x text-primary mb-3"></i>
                <p class="text-muted">Cargando notificaciones...</p>
            </div>
        `;
    }

    mostrarVacio() {
        this.container.innerHTML = `
            <div class="notifications-empty-page">
                <i class="fas fa-inbox"></i>
                <h4>No hay notificaciones</h4>
                <p class="text-muted">No se encontraron notificaciones con los filtros seleccionados</p>
            </div>
        `;
    }

    mostrarError() {
        this.container.innerHTML = `
            <div class="notifications-empty-page">
                <i class="fas fa-exclamation-triangle text-danger"></i>
                <h4>Error al cargar</h4>
                <p class="text-muted">Hubo un problema al cargar las notificaciones</p>
                <button class="btn btn-primary mt-3" onclick="notificacionesPage.cargarNotificaciones()">
                    <i class="fas fa-redo me-2"></i>Reintentar
                </button>
            </div>
        `;
    }

    mostrarMensaje(mensaje, tipo) {
        // Crear toast de Bootstrap
        const toastHTML = `
            <div class="toast align-items-center text-white bg-${tipo === 'success' ? 'success' : 'danger'} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="fas fa-${tipo === 'success' ? 'check-circle' : 'exclamation-circle'} me-2"></i>
                        ${mensaje}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;

        // Contenedor de toasts
        let toastContainer = document.querySelector('.toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            document.body.appendChild(toastContainer);
        }

        toastContainer.insertAdjacentHTML('beforeend', toastHTML);
        const toastElement = toastContainer.lastElementChild;
        const toast = new bootstrap.Toast(toastElement);
        toast.show();

        // Eliminar del DOM después de ocultar
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }

    mostrarModalConfirmacion() {
        const modal = new bootstrap.Modal(document.getElementById('confirmarModal'));
        modal.show();
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
let notificacionesPage;

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    notificacionesPage = new NotificacionesPage();
});
