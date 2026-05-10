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
        
        // Evento para consultar Hacienda cuando cambia el número de identificación
        const inputIdentificacion = document.getElementById('numeroIdentificacion');
        if (inputIdentificacion) {
            inputIdentificacion.addEventListener('blur', () => {
                const numero = inputIdentificacion.value.trim();
                if (numero && numero.length >= 9) {
                    this.consultarHacienda(numero);
                }
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
                if (document.getElementById('faviconUrl')) {
                    document.getElementById('faviconUrl').value = datos.faviconUrl || '';
                }
                if (document.getElementById('colorPrimario')) {
                    document.getElementById('colorPrimario').value = datos.colorPrimario || '#007bff';
                    document.getElementById('colorPrimarioText').value = datos.colorPrimario || '#007bff';
                }
                if (document.getElementById('colorSecundario')) {
                    document.getElementById('colorSecundario').value = datos.colorSecundario || '#6c757d';
                    document.getElementById('colorSecundarioText').value = datos.colorSecundario || '#6c757d';
                }

                // Facturación Electrónica Costa Rica
                if (document.getElementById('tipoIdentificacion')) {
                    document.getElementById('tipoIdentificacion').value = datos.tipoIdentificacion || '';
                }
                if (document.getElementById('numeroIdentificacion')) {
                    document.getElementById('numeroIdentificacion').value = datos.numeroIdentificacion || '';
                }
                if (document.getElementById('codigoActividad')) {
                    document.getElementById('codigoActividad').value = datos.codigoActividad || '';
                }
                if (document.getElementById('nombreComercialFe')) {
                    document.getElementById('nombreComercialFe').value = datos.nombreComercialFe || '';
                }
                if (document.getElementById('otrasSenas')) {
                    document.getElementById('otrasSenas').value = datos.otrasSenas || '';
                }
                if (document.getElementById('barrio')) {
                    document.getElementById('barrio').value = datos.barrio || '';
                }
                // Ubicación CR (provincia → canton → distrito en cascada)
                if (datos.codigoProvincia && document.getElementById('provincia')) {
                    const selectProvincia = document.getElementById('provincia');
                    selectProvincia.value = datos.codigoProvincia;
                    selectProvincia.dispatchEvent(new Event('change'));
                    // Canton y distrito se cargan después del evento change de provincia
                    setTimeout(() => {
                        if (datos.canton && document.getElementById('canton')) {
                            document.getElementById('canton').value = datos.canton;
                            document.getElementById('canton').dispatchEvent(new Event('change'));
                            setTimeout(() => {
                                if (datos.distrito && document.getElementById('distrito')) {
                                    document.getElementById('distrito').value = datos.distrito;
                                }
                            }, 300);
                        }
                    }, 300);
                }

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
     * Consulta datos del contribuyente en API de Hacienda Costa Rica
     */
    consultarHacienda: async function(numeroIdentificacion) {
        // Mostrar indicador de carga
        const Toast = Swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 2000,
            timerProgressBar: true
        });
        
        Toast.fire({
            icon: 'info',
            title: 'Consultando Hacienda...'
        });
        
        try {
            const response = await fetch(`${this.API_URL}/hacienda/consultar/${numeroIdentificacion}`);
            const data = await response.json();
            
            if (data.success && data.data) {
                const hacienda = data.data;
                
                // Autocompletar campos
                if (hacienda.nombre) {
                    document.getElementById('razonSocial').value = hacienda.nombre;
                    if (!document.getElementById('nombreComercialFe').value) {
                        document.getElementById('nombreComercialFe').value = hacienda.nombre;
                    }
                }
                
                // Tipo de identificación
                if (hacienda.tipoIdentificacion) {
                    const selectTipo = document.getElementById('tipoIdentificacion');
                    const mapping = {'01': 'CEDULA_FISICA', '02': 'CEDULA_JURIDICA', '03': 'DIMEX', '04': 'NITE'};
                    if (mapping[hacienda.tipoIdentificacion]) {
                        selectTipo.value = mapping[hacienda.tipoIdentificacion];
                    }
                }
                
                // Código de actividad principal
                if (hacienda.actividades && hacienda.actividades.length > 0) {
                    const actividadPrincipal = hacienda.actividades.find(a => a.tipo === 'P' && a.estado === 'A');
                    if (actividadPrincipal && actividadPrincipal.codigo) {
                        // Código de actividad CAECR (formato 9999.9)
                        const codigo = actividadPrincipal.codigo;
                        document.getElementById('codigoActividad').value = codigo;
                    }
                }
                
                // Régimen fiscal
                if (hacienda.regimen && hacienda.regimen.descripcion) {
                    document.getElementById('regimenFiscal').value = hacienda.regimen.descripcion;
                }
                
                // Mostrar éxito
                Toast.fire({
                    icon: 'success',
                    title: '¡Datos autocompletados desde Hacienda!'
                });
                
                // Mostrar alerta si no está al día
                if (!hacienda.estaAlDia || !hacienda.estaInscrito) {
                    let mensaje = 'Advertencia: ';
                    if (!hacienda.estaInscrito) {
                        mensaje += 'No está inscrito en Hacienda. ';
                    }
                    if (hacienda.situacion) {
                        if (hacienda.situacion.moroso === 'SI') {
                            mensaje += 'Estado: MOROSO. ';
                        }
                        if (hacienda.situacion.omiso === 'SI') {
                            mensaje += 'Estado: OMISO. ';
                        }
                    }
                    
                    Swal.fire({
                        icon: 'warning',
                        title: 'Situación Tributaria',
                        text: mensaje,
                        confirmButtonText: 'Entendido'
                    });
                }
                
            } else {
                Toast.fire({
                    icon: 'error',
                    title: data.message || 'Identificación no encontrada en Hacienda'
                });
            }
        } catch (error) {
            console.error('Error consultando Hacienda:', error);
            Toast.fire({
                icon: 'error',
                title: 'Error al consultar Hacienda'
            });
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
                faviconUrl: document.getElementById('faviconUrl') ? document.getElementById('faviconUrl').value || null : null,
                colorPrimario: document.getElementById('colorPrimario').value || '#007bff',
                colorSecundario: document.getElementById('colorSecundario').value || '#6c757d',
                // Facturación Electrónica Costa Rica
                tipoIdentificacion: document.getElementById('tipoIdentificacion').value || null,
                numeroIdentificacion: document.getElementById('numeroIdentificacion').value || null,
                codigoActividad: document.getElementById('codigoActividad').value || null,
                nombreComercialFe: document.getElementById('nombreComercialFe').value || null,
                codigoProvincia: document.getElementById('provincia').value || null,
                canton: document.getElementById('canton').value || null,
                distrito: document.getElementById('distrito').value || null,
                barrio: document.getElementById('barrio').value || null,
                otrasSenas: document.getElementById('otrasSenas').value || null
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
