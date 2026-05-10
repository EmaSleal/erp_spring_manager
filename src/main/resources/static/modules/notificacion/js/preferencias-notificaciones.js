/**
 * ============================================================================
 * PREFERENCIAS-NOTIFICACIONES.JS
 * ERP Orders Manager - Sprint 4 Fase 3.7
 * ============================================================================
 * Gestión de preferencias de notificaciones por tipo y canal
 * ============================================================================
 */

class PreferenciasNotificaciones {
    constructor() {
        this.container = document.getElementById('preferenciasContainer');
        this.btnGuardar = document.getElementById('btnGuardarPreferencias');
        this.btnRestaurar = document.getElementById('btnRestaurarDefecto');
        
        this.preferencias = [];
        this.cambiosPendientes = false;
        
        this.init();
    }

    init() {
        // Cargar preferencias
        this.cargarPreferencias();

        // Event listeners
        this.btnGuardar?.addEventListener('click', () => this.guardarPreferencias());
        this.btnRestaurar?.addEventListener('click', () => this.restaurarDefecto());

        // Detectar cambios sin guardar
        window.addEventListener('beforeunload', (e) => {
            if (this.cambiosPendientes) {
                e.preventDefault();
                e.returnValue = '';
            }
        });
    }

    async cargarPreferencias() {
        try {
            const response = await fetch('/api/notificaciones/preferencias');
            
            if (!response.ok) {
                throw new Error('Error al cargar preferencias');
            }

            const preferencias = await response.json();
            this.preferencias = preferencias;
            this.renderPreferencias();
            
        } catch (error) {
            console.error('Error cargando preferencias:', error);
            this.mostrarError();
        }
    }

    renderPreferencias() {
        if (!this.preferencias || this.preferencias.length === 0) {
            this.container.innerHTML = `
                <div class="alert alert-warning">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    No se encontraron preferencias. Se crearán las predeterminadas al guardar.
                </div>
            `;
            return;
        }

        this.container.innerHTML = this.preferencias.map(p => this.crearPreferenciaHTML(p)).join('');

        // Agregar event listeners a los switches
        this.container.querySelectorAll('.form-check-input').forEach(input => {
            input.addEventListener('change', () => {
                this.cambiosPendientes = true;
                this.btnGuardar.classList.add('btn-warning');
                this.btnGuardar.innerHTML = '<i class="fas fa-save me-2"></i>Guardar Cambios';
            });
        });
    }

    crearPreferenciaHTML(preferencia) {
        const iconoClass = this.getTipoClass(preferencia.tipo);
        const icono = this.getTipoIcono(preferencia.tipo);
        const descripcion = this.getTipoDescripcion(preferencia.tipo);

        return `
            <div class="preferencia-item" data-tipo="${preferencia.tipo}">
                <div class="preferencia-header">
                    <div class="preferencia-title">
                        <div class="preferencia-icon ${iconoClass}">
                            <i class="${icono}"></i>
                        </div>
                        <div>
                            <h6>${this.formatearTipo(preferencia.tipo)}</h6>
                        </div>
                    </div>
                </div>
                <div class="preferencia-description">
                    ${descripcion}
                </div>
                <div class="canales-switches">
                    <div class="canal-switch">
                        <div class="form-check form-switch">
                            <input class="form-check-input" 
                                   type="checkbox" 
                                   id="web_${preferencia.tipo}"
                                   data-tipo="${preferencia.tipo}"
                                   data-canal="WEB"
                                   ${preferencia.activoWeb ? 'checked' : ''}>
                            <label class="form-check-label" for="web_${preferencia.tipo}">
                                <i class="fas fa-globe canal-icon web"></i>
                                Web
                            </label>
                        </div>
                    </div>
                    <div class="canal-switch">
                        <div class="form-check form-switch">
                            <input class="form-check-input" 
                                   type="checkbox" 
                                   id="email_${preferencia.tipo}"
                                   data-tipo="${preferencia.tipo}"
                                   data-canal="EMAIL"
                                   ${preferencia.activoEmail ? 'checked' : ''}>
                            <label class="form-check-label" for="email_${preferencia.tipo}">
                                <i class="fas fa-envelope canal-icon email"></i>
                                Email
                            </label>
                        </div>
                    </div>
                    <div class="canal-switch">
                        <div class="form-check form-switch">
                            <input class="form-check-input" 
                                   type="checkbox" 
                                   id="whatsapp_${preferencia.tipo}"
                                   data-tipo="${preferencia.tipo}"
                                   data-canal="WHATSAPP"
                                   ${preferencia.activoWhatsapp ? 'checked' : ''}>
                            <label class="form-check-label" for="whatsapp_${preferencia.tipo}">
                                <i class="fab fa-whatsapp canal-icon whatsapp"></i>
                                WhatsApp
                            </label>
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

    getTipoDescripcion(tipo) {
        const descripciones = {
            'FACTURA_CREADA': 'Recibe una notificación cada vez que se genere una nueva factura en el sistema',
            'FACTURA_VENCIDA': 'Te avisaremos cuando una factura haya vencido y requiera atención inmediata',
            'FACTURA_PROXIMA_VENCER': 'Recordatorio 3 días antes del vencimiento para que puedas tomar acción',
            'PAGO_RECIBIDO': 'Confirmaremos cada vez que se registre un pago en el sistema',
            'SISTEMA': 'Notificaciones importantes sobre el funcionamiento del sistema'
        };
        return descripciones[tipo] || 'Notificación del sistema';
    }

    formatearTipo(tipo) {
        const nombres = {
            'FACTURA_CREADA': 'Factura Creada',
            'FACTURA_VENCIDA': 'Factura Vencida',
            'FACTURA_PROXIMA_VENCER': 'Factura Próxima a Vencer',
            'PAGO_RECIBIDO': 'Pago Recibido',
            'SISTEMA': 'Notificación de Sistema'
        };
        return nombres[tipo] || tipo;
    }

    async guardarPreferencias() {
        try {
            // Obtener valores actuales de los checkboxes
            const preferenciasActualizadas = [];
            
            this.container.querySelectorAll('.preferencia-item').forEach(item => {
                const tipo = item.dataset.tipo;
                const web = item.querySelector(`[data-tipo="${tipo}"][data-canal="WEB"]`).checked;
                const email = item.querySelector(`[data-tipo="${tipo}"][data-canal="EMAIL"]`).checked;
                const whatsapp = item.querySelector(`[data-tipo="${tipo}"][data-canal="WHATSAPP"]`).checked;

                preferenciasActualizadas.push({
                    tipo: tipo,
                    activoWeb: web,
                    activoEmail: email,
                    activoWhatsapp: whatsapp
                });
            });

            // Mostrar loading
            this.btnGuardar.disabled = true;
            this.btnGuardar.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Guardando...';

            // Enviar al backend
            const response = await fetch('/api/notificaciones/preferencias', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(preferenciasActualizadas)
            });

            if (!response.ok) {
                throw new Error('Error al guardar preferencias');
            }

            // Éxito
            this.cambiosPendientes = false;
            this.btnGuardar.classList.remove('btn-warning');
            this.btnGuardar.classList.add('btn-success');
            this.btnGuardar.innerHTML = '<i class="fas fa-check me-2"></i>Guardado';

            this.mostrarMensaje('Preferencias guardadas correctamente', 'success');

            // Restaurar botón después de 2 segundos
            setTimeout(() => {
                this.btnGuardar.classList.remove('btn-success');
                this.btnGuardar.classList.add('btn-primary');
                this.btnGuardar.innerHTML = '<i class="fas fa-save me-2"></i>Guardar Preferencias';
                this.btnGuardar.disabled = false;
            }, 2000);

        } catch (error) {
            console.error('Error guardando preferencias:', error);
            this.btnGuardar.disabled = false;
            this.btnGuardar.innerHTML = '<i class="fas fa-save me-2"></i>Guardar Preferencias';
            this.mostrarMensaje('Error al guardar las preferencias', 'error');
        }
    }

    async restaurarDefecto() {
        if (!confirm('¿Estás seguro de que deseas restaurar las preferencias por defecto?\n\nEsto activará todos los canales para todos los tipos de notificación.')) {
            return;
        }

        try {
            // TODO: Implementar endpoint de restaurar por defecto
            this.mostrarMensaje('Funcionalidad en desarrollo', 'warning');
            return;
            
            /* COMMENTED OUT - Endpoint no implementado aún
            const response = await fetch('/api/notificaciones/preferencias/restaurar-defecto', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                }
            });

            if (!response.ok) {
                throw new Error('Error al restaurar preferencias');
            }

            // Recargar preferencias
            await this.cargarPreferencias();
            this.cambiosPendientes = false;
            this.btnGuardar.classList.remove('btn-warning');
            this.btnGuardar.innerHTML = '<i class="fas fa-save me-2"></i>Guardar Preferencias';

            this.mostrarMensaje('Preferencias restauradas por defecto', 'success');
            */

        } catch (error) {
            console.error('Error restaurando preferencias:', error);
            this.mostrarMensaje('Error al restaurar las preferencias', 'error');
        }
    }

    mostrarError() {
        this.container.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-exclamation-triangle me-2"></i>
                Error al cargar las preferencias. Por favor, recarga la página.
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
}

// Instancia global
let preferenciasNotificaciones;

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    preferenciasNotificaciones = new PreferenciasNotificaciones();
});
