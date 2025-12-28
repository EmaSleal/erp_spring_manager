/**
 * ============================================================================
 * CONFIGURACION-FACTURACION.JS - Gestión de configuración de facturación
 * WhatsApp Orders Manager - Sprint 4
 * ============================================================================
 */

const ConfiguracionFacturacion = {
    API_URL: '/api/configuracion/facturacion',
    
    /**
     * Inicializa el módulo
     */
    init: function() {
        this.cargarConfiguracion();
        this.configurarEventos();
        console.log('✅ Módulo Facturación inicializado');
    },
    
    /**
     * Configura event listeners
     */
    configurarEventos: function() {
        const form = document.getElementById('form-facturacion');
        if (form) {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.guardarConfiguracion();
            });
        }
        
        const btnCancelar = document.getElementById('btn-cancelar-facturacion');
        if (btnCancelar) {
            btnCancelar.addEventListener('click', () => {
                this.cargarConfiguracion();
            });
        }
    },
    
    /**
     * Carga la configuración actual
     */
    cargarConfiguracion: async function() {
        try {
            const response = await Configuracion.get(this.API_URL);
            
            if (response.success && response.data) {
                const datos = response.data;
                
                // Cargar campos en el formulario
                document.getElementById('idConfiguracionFacturacion').value = datos.id || '';
                document.getElementById('serieFactura').value = datos.serieFactura || '';
                document.getElementById('prefijoFactura').value = datos.prefijoFactura || '';
                document.getElementById('numeroInicial').value = datos.numeroInicial || 1;
                document.getElementById('numeroActual').value = datos.numeroActual || 1;
                document.getElementById('formatoNumero').value = datos.formatoNumero || '';
                document.getElementById('igv').value = datos.igv || 0;
                document.getElementById('incluirIgvEnPrecio').checked = datos.incluirIgvEnPrecio || false;
                document.getElementById('moneda').value = datos.moneda || '';
                document.getElementById('simboloMoneda').value = datos.simboloMoneda || '';
                document.getElementById('decimales').value = datos.decimales || 2;
                document.getElementById('terminosCondiciones').value = datos.terminosCondiciones || '';
                document.getElementById('notaPiePagina').value = datos.notaPiePagina || '';
                document.getElementById('activoFacturacion').checked = datos.activo !== false;
                
                console.log('✅ Configuración de facturación cargada');
            } else if (response.message) {
                console.log('ℹ️ No hay configuración: ', response.message);
            }
        } catch (error) {
            console.error('❌ Error cargando configuración:', error);
            Configuracion.mostrarAlertaEn('alert-facturacion-container', 'warning', 
                'No se pudo cargar la configuración. Puedes crear una nueva.');
        }
    },
    
    /**
     * Guarda la configuración
     */
    guardarConfiguracion: async function() {
        // Validar formulario
        if (!Configuracion.validarFormulario('form-facturacion')) {
            Configuracion.mostrarAlertaEn('alert-facturacion-container', 'warning', 
                'Por favor completa todos los campos requeridos');
            return;
        }
        
        try {
            // Obtener datos del formulario
            const datos = {
                serieFactura: document.getElementById('serieFactura').value || null,
                prefijoFactura: document.getElementById('prefijoFactura').value || null,
                numeroInicial: parseInt(document.getElementById('numeroInicial').value) || 1,
                numeroActual: parseInt(document.getElementById('numeroActual').value) || 1,
                formatoNumero: document.getElementById('formatoNumero').value || null,
                igv: parseFloat(document.getElementById('igv').value) || 0,
                incluirIgvEnPrecio: document.getElementById('incluirIgvEnPrecio').checked,
                moneda: document.getElementById('moneda').value || null,
                simboloMoneda: document.getElementById('simboloMoneda').value || null,
                decimales: parseInt(document.getElementById('decimales').value) || 2,
                terminosCondiciones: document.getElementById('terminosCondiciones').value || null,
                notaPiePagina: document.getElementById('notaPiePagina').value || null,
                activo: document.getElementById('activoFacturacion').checked
            };
            
            const idConfiguracion = document.getElementById('idConfiguracionFacturacion').value;
            let response;
            
            if (idConfiguracion) {
                // Actualizar - usar 'id' en lugar de 'idConfiguracion'
                datos.id = parseInt(idConfiguracion);
                response = await Configuracion.put(this.API_URL, datos);
            } else {
                // Crear
                response = await Configuracion.post(this.API_URL, datos);
            }
            
            if (response.success) {
                Configuracion.mostrarAlertaEn('alert-facturacion-container', 'success', 
                    response.message || 'Configuración guardada exitosamente');
                
                // Recargar datos
                setTimeout(() => this.cargarConfiguracion(), 1000);
            } else {
                Configuracion.mostrarAlertaEn('alert-facturacion-container', 'danger', 
                    response.message || 'Error al guardar la configuración');
            }
        } catch (error) {
            console.error('❌ Error guardando configuración:', error);
            Configuracion.mostrarAlertaEn('alert-facturacion-container', 'danger', 
                'Error al guardar. Por favor intenta nuevamente.');
        }
    }
};

// Inicializar cuando el tab de facturación esté activo
document.addEventListener('shown.bs.tab', function (event) {
    if (event.target.id === 'facturacion-tab') {
        ConfiguracionFacturacion.init();
    }
});

// Inicializar si ya está en el tab
document.addEventListener('DOMContentLoaded', function() {
    const facturacionTab = document.getElementById('facturacion-tab');
    if (facturacionTab && facturacionTab.classList.contains('active')) {
        ConfiguracionFacturacion.init();
    }
});
