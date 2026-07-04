/**
 * ============================================================================
 * CONFIGURACION-EMPRESA.JS - Gestión de configuración de empresa
 * ERP Orders Manager - Sprint 4
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
                document.getElementById('idConfiguracionEmpresa').value = datos.idConfiguracion || '';
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

                // Logo uploaded as file
                if (datos.tieneLogoCargado && datos.logoPath) {
                    const previewLogo = document.getElementById('logo-preview');
                    const containerLogo = document.getElementById('logo-preview-container');
                    if (previewLogo && containerLogo) {
                        previewLogo.src = `/api/configuracion/empresa/assets/empresa/${datos.logoPath}`;
                        containerLogo.style.display = 'block';
                    }
                } else {
                    const containerLogo = document.getElementById('logo-preview-container');
                    if (containerLogo) containerLogo.style.display = 'none';
                }
                // Favicon uploaded as file
                if (datos.tieneFaviconCargado && datos.faviconPath) {
                    const previewFav = document.getElementById('favicon-preview');
                    const containerFav = document.getElementById('favicon-preview-container');
                    if (previewFav && containerFav) {
                        previewFav.src = `/api/configuracion/empresa/assets/empresa/${datos.faviconPath}`;
                        containerFav.style.display = 'block';
                    }
                } else {
                    const containerFav = document.getElementById('favicon-preview-container');
                    if (containerFav) containerFav.style.display = 'none';
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
                if (document.getElementById('descripcionActividad')) {
                    document.getElementById('descripcionActividad').value = datos.descripcionActividad || '';
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
                        if (actividadPrincipal.descripcion && document.getElementById('descripcionActividad')) {
                            document.getElementById('descripcionActividad').value = actividadPrincipal.descripcion;
                        }
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
    
    subirLogo: async function(file) {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const fd = new FormData();
        fd.append('logo', file);
        const headers = {};
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        try {
            const res = await fetch('/api/configuracion/empresa/logo', { method: 'POST', headers, body: fd });
            const data = await res.json();
            if (!data.success) Configuracion.mostrarAlertaEn('alert-empresa-container', 'warning', data.message || 'Error al subir logo');
        } catch (e) { console.error('Error subiendo logo:', e); }
    },

    subirFavicon: async function(file) {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const fd = new FormData();
        fd.append('favicon', file);
        const headers = {};
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        try {
            const res = await fetch('/api/configuracion/empresa/favicon', { method: 'POST', headers, body: fd });
            const data = await res.json();
            if (!data.success) Configuracion.mostrarAlertaEn('alert-empresa-container', 'warning', data.message || 'Error al subir favicon');
        } catch (e) { console.error('Error subiendo favicon:', e); }
    },

    eliminarLogo: async function() {
        const confirmado = await Swal.fire({ title: '¿Quitar logo?', icon: 'warning', showCancelButton: true, confirmButtonText: 'Sí, quitar', cancelButtonText: 'Cancelar' });
        if (!confirmado.isConfirmed) return;
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        try {
            const res = await fetch('/api/configuracion/empresa/logo', { method: 'DELETE', headers });
            const data = await res.json();
            if (data.success) {
                document.getElementById('logo-preview-container').style.display = 'none';
                Configuracion.mostrarAlertaEn('alert-empresa-container', 'success', 'Logo eliminado');
            }
        } catch (e) { console.error('Error eliminando logo:', e); }
    },

    eliminarFavicon: async function() {
        const confirmado = await Swal.fire({ title: '¿Quitar favicon?', icon: 'warning', showCancelButton: true, confirmButtonText: 'Sí, quitar', cancelButtonText: 'Cancelar' });
        if (!confirmado.isConfirmed) return;
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;
        try {
            const res = await fetch('/api/configuracion/empresa/favicon', { method: 'DELETE', headers });
            const data = await res.json();
            if (data.success) {
                document.getElementById('favicon-preview-container').style.display = 'none';
                Configuracion.mostrarAlertaEn('alert-empresa-container', 'success', 'Favicon eliminado');
            }
        } catch (e) { console.error('Error eliminando favicon:', e); }
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
                descripcionActividad: document.getElementById('descripcionActividad') ? (document.getElementById('descripcionActividad').value || null) : null,
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
                // Actualizar
                datos.idConfiguracion = parseInt(idConfiguracion);
                response = await Configuracion.put(this.API_URL, datos);
            } else {
                // Crear
                response = await Configuracion.post(this.API_URL, datos);
            }
            
            if (response.success) {
                Configuracion.mostrarAlertaEn('alert-empresa-container', 'success',
                    response.message || 'Configuración guardada exitosamente');

                // Upload logo if a file was selected
                const logoFile = document.getElementById('logoFile');
                if (logoFile && logoFile.files && logoFile.files[0]) {
                    await this.subirLogo(logoFile.files[0]);
                    logoFile.value = '';
                }
                // Upload favicon if a file was selected
                const faviconFile = document.getElementById('faviconFile');
                if (faviconFile && faviconFile.files && faviconFile.files[0]) {
                    await this.subirFavicon(faviconFile.files[0]);
                    faviconFile.value = '';
                }

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
        EmpresaPdf.init();
    }
});

// Inicializar si ya está en el tab
document.addEventListener('DOMContentLoaded', function() {
    const empresaTab = document.getElementById('empresa-tab');
    if (empresaTab && empresaTab.classList.contains('active')) {
        ConfiguracionEmpresa.init();
        EmpresaPdf.init();
    }
});

// ============================================================================
// EMPRESA PDF - PDF invoice config and bank accounts
// ============================================================================

const EmpresaPdf = {

    // ------------------------------------------------------------------ init

    init: function () {
        this.cargarConfigPdf();
        this.cargarCuentas();
    },

    // --------------------------------------------------------- PDF config

    cargarConfigPdf: async function () {
        try {
            const res = await Configuracion.get('/api/empresa/pdf-config');
            if (res.success && res.data) {
                const d = res.data;
                const el = (id) => document.getElementById(id);
                if (el('textoLegal'))           el('textoLegal').value           = d.textoLegal           || '';
                if (el('descripcionActividad')) el('descripcionActividad').value = d.descripcionActividad || '';
                if (d.tieneSello && d.selloTipoEmpresa) {
                    const container = el('sello-preview-container');
                    const img       = el('sello-preview');
                    if (container && img) {
                        img.src = '/api/empresa/assets/' + d.selloTipoEmpresa;
                        container.style.display = '';
                    }
                }
            }
        } catch (err) {
            console.error('Error loading PDF config:', err);
        }
    },

    guardarConfigPdf: async function () {
        try {
            const textoLegal           = document.getElementById('textoLegal')           ? document.getElementById('textoLegal').value           : '';
            const descripcionActividad = document.getElementById('descripcionActividad') ? document.getElementById('descripcionActividad').value : '';

            const res = await Configuracion.put('/api/empresa/pdf-config', { textoLegal, descripcionActividad });

            if (!res.success) {
                Swal.fire({ icon: 'error', title: 'Error', text: res.message || 'Error saving PDF config' });
                return;
            }

            // Upload sello if a file was selected
            const selloInput = document.getElementById('selloFile');
            if (selloInput && selloInput.files && selloInput.files.length > 0) {
                await this._subirSello(selloInput.files[0]);
                selloInput.value = '';
            }

            Swal.fire({ icon: 'success', title: 'Saved', text: 'PDF config saved successfully', timer: 1500, showConfirmButton: false });
            this.cargarConfigPdf();
        } catch (err) {
            console.error('Error saving PDF config:', err);
            Swal.fire({ icon: 'error', title: 'Error', text: 'Unexpected error saving PDF config' });
        }
    },

    _subirSello: async function (file) {
        const csrfToken  = document.querySelector('meta[name="_csrf"]')        ? document.querySelector('meta[name="_csrf"]').content        : '';
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]') ? document.querySelector('meta[name="_csrf_header"]').content : 'X-CSRF-TOKEN';

        const formData = new FormData();
        formData.append('sello', file);

        const response = await fetch('/api/empresa/sello', {
            method: 'POST',
            headers: { [csrfHeader]: csrfToken },
            body: formData
        });
        return await response.json();
    },

    eliminarSello: async function () {
        const confirm = await Swal.fire({
            icon: 'warning',
            title: '¿Quitar sello?',
            text: 'Se eliminará el sello actual.',
            showCancelButton: true,
            confirmButtonText: 'Sí, quitar',
            cancelButtonText: 'Cancelar'
        });
        if (!confirm.isConfirmed) return;

        try {
            const res = await Configuracion.delete('/api/empresa/sello');
            if (res.success) {
                const container = document.getElementById('sello-preview-container');
                if (container) container.style.display = 'none';
                Swal.fire({ icon: 'success', title: 'Done', text: 'Sello removed', timer: 1200, showConfirmButton: false });
            } else {
                Swal.fire({ icon: 'error', title: 'Error', text: res.message });
            }
        } catch (err) {
            console.error('Error removing sello:', err);
        }
    },

    // --------------------------------------------------------- Bank accounts

    cargarCuentas: async function () {
        try {
            const res = await Configuracion.get('/api/empresa/cuentas-bancarias');
            if (res.success) {
                this._renderCuentas(res.data || []);
            }
        } catch (err) {
            console.error('Error loading bank accounts:', err);
        }
    },

    _renderCuentas: function (cuentas) {
        const container = document.getElementById('lista-cuentas-bancarias');
        if (!container) return;

        if (!cuentas.length) {
            container.innerHTML = '<p class="text-muted small">No hay cuentas bancarias configuradas.</p>';
            return;
        }

        const rows = cuentas.map(c => `
            <tr>
                <td>${this._esc(c.entidad)}</td>
                <td><code>${this._esc(c.cuentaIban)}</code></td>
                <td>${this._esc(c.cuentaBanco)}</td>
                <td><span class="badge bg-secondary">${this._esc(c.moneda)}</span></td>
                <td class="text-end">
                    <button type="button" class="btn btn-sm btn-outline-primary me-1"
                            onclick="EmpresaPdf.mostrarFormCuenta(${c.idCuentaBancaria})">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-danger"
                            onclick="EmpresaPdf.eliminarCuenta(${c.idCuentaBancaria})">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>`).join('');

        container.innerHTML = `
            <table class="table table-sm table-hover">
                <thead class="table-light">
                    <tr>
                        <th>Entidad</th><th>IBAN</th><th>Cuenta Banco</th><th>Moneda</th><th></th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>`;
    },

    mostrarFormCuenta: async function (id) {
        const form = document.getElementById('form-cuenta-bancaria');
        if (!form) return;

        document.getElementById('cuentaId').value      = '';
        document.getElementById('cuentaEntidad').value  = '';
        document.getElementById('cuentaIban').value     = '';
        document.getElementById('cuentaBanco').value    = '';
        document.getElementById('cuentaMoneda').value   = 'CRC';

        if (id) {
            // Load existing account data from the rendered table if possible
            try {
                const res = await Configuracion.get('/api/empresa/cuentas-bancarias');
                if (res.success) {
                    const cuenta = (res.data || []).find(c => c.idCuentaBancaria === id);
                    if (cuenta) {
                        document.getElementById('cuentaId').value     = cuenta.idCuentaBancaria;
                        document.getElementById('cuentaEntidad').value = cuenta.entidad  || '';
                        document.getElementById('cuentaIban').value    = cuenta.cuentaIban  || '';
                        document.getElementById('cuentaBanco').value   = cuenta.cuentaBanco || '';
                        document.getElementById('cuentaMoneda').value  = cuenta.moneda || 'CRC';
                    }
                }
            } catch (err) {
                console.error('Error loading account for edit:', err);
            }
        }

        form.style.display = '';
        form.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    },

    cancelarCuenta: function () {
        const form = document.getElementById('form-cuenta-bancaria');
        if (form) form.style.display = 'none';
    },

    guardarCuenta: async function () {
        const id      = document.getElementById('cuentaId').value;
        const payload = {
            entidad:     document.getElementById('cuentaEntidad').value.trim(),
            cuentaIban:  document.getElementById('cuentaIban').value.trim(),
            cuentaBanco: document.getElementById('cuentaBanco').value.trim(),
            moneda:      document.getElementById('cuentaMoneda').value
        };

        if (!payload.entidad) {
            Swal.fire({ icon: 'warning', title: 'Requerido', text: 'El nombre de la entidad es obligatorio' });
            return;
        }

        try {
            let res;
            if (id) {
                res = await Configuracion.put(`/api/empresa/cuentas-bancarias/${id}`, payload);
            } else {
                res = await Configuracion.post('/api/empresa/cuentas-bancarias', payload);
            }

            if (res.success) {
                this.cancelarCuenta();
                await this.cargarCuentas();
                Swal.fire({ icon: 'success', title: 'Guardado', timer: 1200, showConfirmButton: false });
            } else {
                Swal.fire({ icon: 'error', title: 'Error', text: res.message });
            }
        } catch (err) {
            console.error('Error saving bank account:', err);
            Swal.fire({ icon: 'error', title: 'Error', text: 'Unexpected error saving bank account' });
        }
    },

    eliminarCuenta: async function (id) {
        const confirm = await Swal.fire({
            icon: 'warning',
            title: '¿Eliminar cuenta?',
            text: 'Se quitará esta cuenta bancaria del PDF.',
            showCancelButton: true,
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar'
        });
        if (!confirm.isConfirmed) return;

        try {
            const res = await Configuracion.delete(`/api/empresa/cuentas-bancarias/${id}`);
            if (res.success) {
                await this.cargarCuentas();
                Swal.fire({ icon: 'success', title: 'Eliminada', timer: 1200, showConfirmButton: false });
            } else {
                Swal.fire({ icon: 'error', title: 'Error', text: res.message });
            }
        } catch (err) {
            console.error('Error deleting bank account:', err);
        }
    },

    // ---------------------------------------------------------------- utils

    _esc: function (str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
};
