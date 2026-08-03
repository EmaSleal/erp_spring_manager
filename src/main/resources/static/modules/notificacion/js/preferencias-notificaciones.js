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
            this.preferencias = await httpGet('/api/notificaciones/preferencias', { showLoading: false });
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

    // Tipos y colores alineados con TipoNotificacion (Java) — misma
    // clasificación que usa el backend, no inventada en el frontend.
    getTipoClass(tipo) {
        const tipos = {
            'FACTURA_CREADA': 'type-factura-creada',
            'FACTURA_VENCIDA': 'type-factura-vencida',
            'FACTURA_PROXIMA_VENCER': 'type-factura-proxima-vencer',
            'PAGO_RECIBIDO': 'type-pago-recibido',
            'STOCK_BAJO': 'type-stock-bajo',
            'NUEVO_CLIENTE': 'type-nuevo-cliente',
            'NUEVO_USUARIO': 'type-nuevo-usuario',
            'MENSAJE_WHATSAPP': 'type-mensaje-whatsapp',
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
            'STOCK_BAJO': 'fas fa-box-open',
            'NUEVO_CLIENTE': 'fas fa-user-plus',
            'NUEVO_USUARIO': 'fas fa-user-shield',
            'MENSAJE_WHATSAPP': 'fab fa-whatsapp',
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
            'STOCK_BAJO': 'Te avisaremos cuando el stock de un producto esté por debajo del mínimo configurado',
            'NUEVO_CLIENTE': 'Notificación al equipo administrativo cuando se registre un nuevo cliente',
            'NUEVO_USUARIO': 'Notificación al equipo administrativo cuando se registre un nuevo usuario del sistema',
            'MENSAJE_WHATSAPP': 'Te avisaremos cuando llegue un nuevo mensaje de WhatsApp',
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
            'STOCK_BAJO': 'Stock Bajo',
            'NUEVO_CLIENTE': 'Nuevo Cliente',
            'NUEVO_USUARIO': 'Nuevo Usuario',
            'MENSAJE_WHATSAPP': 'Mensaje WhatsApp',
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

            await httpPut('/api/notificaciones/preferencias', preferenciasActualizadas, { showLoading: false });

            this.cambiosPendientes = false;
            this.btnGuardar.classList.remove('btn-warning');
            this.btnGuardar.classList.add('btn-success');
            this.btnGuardar.innerHTML = '<i class="fas fa-check me-2"></i>Guardado';

            showToast('success', 'Preferencias guardadas correctamente');

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
            showToast('error', 'Error al guardar las preferencias');
        }
    }

    async restaurarDefecto() {
        const confirmed = await showConfirmDialog(
            '¿Restaurar preferencias por defecto?',
            'Esto activará todos los canales para todos los tipos de notificación.'
        );
        if (!confirmed) return;

        try {
            // TODO: Implementar endpoint de restaurar por defecto
            showToast('warning', 'Funcionalidad en desarrollo');
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

}

// Instancia global
let preferenciasNotificaciones;

// Inicialización
document.addEventListener('DOMContentLoaded', function() {
    preferenciasNotificaciones = new PreferenciasNotificaciones();
});
