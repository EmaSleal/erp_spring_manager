/**
 * ============================================================================
 * CONFIGURACION-EMPRESA.JS - Gestión de configuración de empresa
 * WhatsApp Orders Manager - Sprint 4
 * ============================================================================
 */

const ConfiguracionEmpresa = {
    API_URL: '/api/configuracion/empresa',
    
    /**
     * Inicializa el módulo
     */
    init: function() {
        this.cargarConfiguracion();
        this.configurarEventos();
        console.log('✅ Módulo Empresa inicializado');
    },
    
    /**
     * Configura event listeners
     */
    configurarEventos: function() {
        const form = document.getElementById('form-empresa');
        if (form) {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.guardarConfiguracion();
            });
        }
        
        const btnCancelar = document.getElementById('btn-cancelar-empresa');
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
                document.getElementById('idConfiguracionEmpresa').value = datos.id || '';
                document.getElementById('razonSocial').value = datos.razonSocial || '';
                document.getElementById('nombreComercial').value = datos.nombreComercial || '';
                document.getElementById('rfc').value = datos.rfc || '';
                document.getElementById('regimenFiscal').value = datos.regimenFiscal || '';
                
                // Dirección
                document.getElementById('direccionCalle').value = datos.direccionCalle || '';
                document.getElementById('direccionCiudad').value = datos.direccionCiudad || '';
                document.getElementById('direccionEstado').value = datos.direccionEstado || '';
                document.getElementById('direccionCodigoPostal').value = datos.direccionCodigoPostal || '';
                document.getElementById('direccionPais').value = datos.direccionPais || '';
                
                // Contacto
                document.getElementById('telefono').value = datos.telefono || '';
                document.getElementById('email').value = datos.email || '';
                document.getElementById('sitioWeb').value = datos.sitioWeb || '';
                
                // Branding
                document.getElementById('logoUrl').value = datos.logoUrl || '';
                document.getElementById('faviconUrl').value = datos.faviconUrl || '';
                document.getElementById('colorPrimario').value = datos.colorPrimario || '#007bff';
                document.getElementById('colorPrimarioText').value = datos.colorPrimario || '#007bff';
                document.getElementById('colorSecundario').value = datos.colorSecundario || '#6c757d';
                document.getElementById('colorSecundarioText').value = datos.colorSecundario || '#6c757d';
                
                console.log('✅ Configuración de empresa cargada');
            } else if (response.message) {
                console.log('ℹ️ No hay configuración: ', response.message);
            }
        } catch (error) {
            console.error('❌ Error cargando configuración:', error);
            Configuracion.mostrarAlertaEn('alert-empresa-container', 'warning', 
                'No se pudo cargar la configuración. Puedes crear una nueva.');
        }
    },
    
    /**
     * Guarda la configuración
     */
    guardarConfiguracion: async function() {
        // Validar formulario
        if (!Configuracion.validarFormulario('form-empresa')) {
            Configuracion.mostrarAlertaEn('alert-empresa-container', 'warning', 
                'Por favor completa todos los campos requeridos');
            return;
        }
        
        try {
            // Obtener datos del formulario
            const datos = {
                razonSocial: document.getElementById('razonSocial').value,
                nombreComercial: document.getElementById('nombreComercial').value || null,
                rfc: document.getElementById('rfc').value,
                regimenFiscal: document.getElementById('regimenFiscal').value || null,
                direccionCalle: document.getElementById('direccionCalle').value || null,
                direccionCiudad: document.getElementById('direccionCiudad').value || null,
                direccionEstado: document.getElementById('direccionEstado').value || null,
                direccionCodigoPostal: document.getElementById('direccionCodigoPostal').value || null,
                direccionPais: document.getElementById('direccionPais').value || null,
                telefono: document.getElementById('telefono').value || null,
                email: document.getElementById('email').value || null,
                sitioWeb: document.getElementById('sitioWeb').value || null,
                logoUrl: document.getElementById('logoUrl').value || null,
                faviconUrl: document.getElementById('faviconUrl').value || null,
                colorPrimario: document.getElementById('colorPrimario').value || '#007bff',
                colorSecundario: document.getElementById('colorSecundario').value || '#6c757d'
            };
            
            const idConfiguracion = document.getElementById('idConfiguracionEmpresa').value;
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
                Configuracion.mostrarAlertaEn('alert-empresa-container', 'success', 
                    response.message || 'Configuración guardada exitosamente');
                
                // Recargar datos
                setTimeout(() => this.cargarConfiguracion(), 1000);
            } else {
                Configuracion.mostrarAlertaEn('alert-empresa-container', 'danger', 
                    response.message || 'Error al guardar la configuración');
            }
        } catch (error) {
            console.error('❌ Error guardando configuración:', error);
            Configuracion.mostrarAlertaEn('alert-empresa-container', 'danger', 
                'Error al guardar. Por favor intenta nuevamente.');
        }
    }
};

// Inicializar cuando el tab de empresa esté activo
document.addEventListener('shown.bs.tab', function (event) {
    if (event.target.id === 'empresa-tab') {
        ConfiguracionEmpresa.init();
    }
});

// Inicializar si ya está en el tab
document.addEventListener('DOMContentLoaded', function() {
    const empresaTab = document.getElementById('empresa-tab');
    if (empresaTab && empresaTab.classList.contains('active')) {
        ConfiguracionEmpresa.init();
    }
});
