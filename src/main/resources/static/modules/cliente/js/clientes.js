// ============================================================================
// CLIENTES.JS - Gestión del modal de clientes con Bootstrap 5
// WhatsApp Orders Manager
// ============================================================================

// Referencia al modal de Bootstrap
let clienteModalElement = null;
let clienteModal = null;

// Inicializar modal cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    clienteModalElement = document.getElementById('clienteModal');
    if (clienteModalElement) {
        clienteModal = new bootstrap.Modal(clienteModalElement);
    }
    
    // Detectar parámetro 'edit' en la URL y abrir modal automáticamente
    const urlParams = new URLSearchParams(window.location.search);
    const editId = urlParams.get('edit');
    if (editId) {
        // Cargar el cliente desde el servidor y abrir el modal
        fetch(`/clientes/detalle/${editId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Cliente no encontrado');
                }
                return response.json();
            })
            .then(cliente => {
                // Abrir modal de edición con los datos del cliente
                openEditModal(
                    cliente.idCliente,
                    cliente.nombre,
                    cliente.usuario?.telefono || '',
                    cliente.tipoCliente,
                    cliente.requiereFacturaElectronica,
                    cliente.tipoIdentificacion,
                    cliente.numeroIdentificacion,
                    cliente.codigoActividad,
                    cliente.provincia.codigo,
                    cliente.canton,
                    cliente.distrito,
                    cliente.otrasSenas
                );
                
                // Limpiar el parámetro de la URL sin recargar la página
                window.history.replaceState({}, '', window.location.pathname);
            })
            .catch(error => {
                console.error('Error al cargar el cliente:', error);
                // Limpiar el parámetro de la URL en caso de error
                window.history.replaceState({}, '', window.location.pathname);
            });
    }
});

// Abrir modal para agregar nuevo cliente
document.getElementById('open-modal')?.addEventListener('click', function () {
    document.getElementById('modal-title').innerText = 'Agregar Cliente';
    document.getElementById('clienteForm').reset();
    document.getElementById('idCliente').value = '';
    
    // Establecer valor por defecto para Facturación Electrónica
    const requiereFECheckbox = document.getElementById('requiereFacturaElectronica');
    if (requiereFECheckbox) {
        requiereFECheckbox.checked = false; // Por defecto: SÍ requiere
        // Dispara manualmente el evento change para sincronizar la visualización
        requiereFECheckbox.dispatchEvent(new Event('change', { bubbles: true }));
    }
    
    if (clienteModal) {
        clienteModal.show();
    }
});

// Abrir modal para editar cliente existente
function openEditModal(clienteId, nombre, telefono, tipoCliente, requiereFacturaElectronica, tipoIdentificacion, numeroIdentificacion, codigoActividad, provincia, canton, distrito, otrasSenas) {
    // Cargar datos completos del cliente desde el servidor
    fetch(`/clientes/detalle/${clienteId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Cliente no encontrado');
            }
            return response.json();
        })
        .then(cliente => {
            document.getElementById('modal-title').innerText = 'Editar Cliente';
            document.getElementById('idCliente').value = cliente.idCliente || '';
            document.getElementById('nombre').value = cliente.nombre || '';
            document.getElementById('telefono').value = cliente.usuario?.telefono || '';
            document.getElementById('tipoCliente').value = cliente.tipoCliente || '';
            
            // Establecer checkbox de Facturación Electrónica
            const requiereFECheckbox = document.getElementById('requiereFacturaElectronica');
            if (requiereFECheckbox) {
                requiereFECheckbox.checked = cliente.requiereFacturaElectronica === true || cliente.requiereFacturaElectronica === 'true';
                // Dispara manualmente el evento change para sincronizar la visualización
                requiereFECheckbox.dispatchEvent(new Event('change', { bubbles: true }));
            }
            
            // Cargar datos de facturación electrónica
            document.getElementById('tipoIdentificacionCliente').value = cliente.tipoIdentificacion || '';
            document.getElementById('numeroIdentificacionCliente').value = cliente.numeroIdentificacion || '';
            document.getElementById('codigoActividadCliente').value = cliente.codigoActividad || '';
            
            // Cargar ubicación
            const provinciaCodigo = cliente.provincia?.codigo || '';
            const cantonCodigo = cliente.canton || '';
            const distritoCodigo = cliente.distrito || '';

            const modalCliente = document.getElementById('clienteModal');
            if (modalCliente) {
                modalCliente.dataset.provinciaCliente = provinciaCodigo;
                modalCliente.dataset.cantonCliente = cantonCodigo;
                modalCliente.dataset.distritoCliente = distritoCodigo;
            }

            document.getElementById('provinciaCliente').value = provinciaCodigo;
            document.getElementById('cantonCliente').value = cantonCodigo;
            document.getElementById('distritoCliente').value = distritoCodigo;
            document.getElementById('otrasSenasCliente').value = cliente.otrasSenas || '';
            
            if (clienteModal) {
                clienteModal.show();
            }
        })
        .catch(error => {
            console.error('Error al cargar el cliente:', error);
            Toast.fire({
                icon: 'error',
                title: 'Error al cargar los datos del cliente'
            });
        });
}

// Consultar datos de Hacienda para clientes
document.getElementById('btnConsultarHaciendaCliente')?.addEventListener('click', function() {
    const numeroIdentificacion = document.getElementById('numeroIdentificacionCliente').value.trim();
    
    if (!numeroIdentificacion || numeroIdentificacion.length < 9) {
        Swal.fire({
            icon: 'warning',
            title: 'Número requerido',
            text: 'Ingrese un número de identificación válido (mínimo 9 dígitos)',
            confirmButtonText: 'OK'
        });
        return;
    }
    
    // Mostrar loading
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
    
    // Consultar API
    fetch(`/api/configuracion/empresa/hacienda/consultar/${numeroIdentificacion}`)
        .then(response => response.json())
        .then(data => {
            if (data.success && data.data) {
                const hacienda = data.data;

                console.log('Datos recibidos de Hacienda:', hacienda);
                
                // Autocompletar nombre si no está lleno
                const inputNombre = document.getElementById('nombre');
                if (!inputNombre.value || inputNombre.value.trim() === '') {
                    inputNombre.value = hacienda.nombre;
                }
                
                // Tipo de identificación
                if (hacienda.tipoIdentificacion) {
                    const selectTipo = document.getElementById('tipoIdentificacionCliente');
                    const mapping = {'01': 'CEDULA_FISICA', '02': 'CEDULA_JURIDICA', '03': 'DIMEX', '04': 'NITE'};
                    if (mapping[hacienda.tipoIdentificacion]) {
                        selectTipo.value = mapping[hacienda.tipoIdentificacion];
                    }
                }

                // Código de actividad principal
                if (hacienda.actividades && hacienda.actividades.length > 0) {
                    const actividadPrincipal = hacienda.actividades.find(a => a.tipo === 'P' && a.estado === 'A')
                        || hacienda.actividades[0];

                    if (actividadPrincipal && actividadPrincipal.codigo) {
                        const inputCodigoActividad = document.getElementById('codigoActividadCliente');
                        if (inputCodigoActividad) {
                            inputCodigoActividad.value = actividadPrincipal.codigo;
                        }
                    }
                }
                
                // Mostrar éxito
                Toast.fire({
                    icon: 'success',
                    title: '¡Datos autocompletados!'
                });
                
                // Advertencia si no está al día
                if (!hacienda.estaAlDia || !hacienda.estaInscrito) {
                    setTimeout(() => {
                        let mensaje = 'Advertencia tributaria:\n';
                        if (!hacienda.estaInscrito) {
                            mensaje += '• No está inscrito en Hacienda\n';
                        }
                        if (hacienda.situacion) {
                            if (hacienda.situacion.moroso === 'SI') {
                                mensaje += '• Estado: MOROSO\n';
                            }
                            if (hacienda.situacion.omiso === 'SI') {
                                mensaje += '• Estado: OMISO\n';
                            }
                        }
                        
                        Swal.fire({
                            icon: 'warning',
                            title: 'Situación Tributaria',
                            text: mensaje,
                            confirmButtonText: 'Entendido'
                        });
                    }, 500);
                }
            } else {
                Toast.fire({
                    icon: 'error',
                    title: data.message || 'Identificación no encontrada'
                });
            }
        })
        .catch(error => {
            console.error('Error:', error);
            Toast.fire({
                icon: 'error',
                title: 'Error al consultar Hacienda'
            });
        });
});

// Cerrar modal (opcional - Bootstrap ya maneja esto)
function closeModal() {
    if (clienteModal) {
        clienteModal.hide();
    }
}

// Validación del formulario con Bootstrap
(function() {
    'use strict';
    const forms = document.querySelectorAll('.needs-validation');
    Array.from(forms).forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });
})();

// Enviar formulario de cliente via AJAX (evita navegación/recarga de página)
document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('clienteForm');
    if (!form) return;

    form.addEventListener('submit', function(event) {
        event.preventDefault();
        event.stopPropagation();

        const otrasSenas = (document.getElementById('otrasSenasCliente')?.value || '').trim();
        if (otrasSenas.length > 0 && otrasSenas.length < 5) {
            document.getElementById('otrasSenasCliente').classList.add('is-invalid');
            Swal.fire({
                icon: 'warning',
                title: 'Dirección muy corta',
                text: 'Otras señas debe tener al menos 5 caracteres o dejarlo vacío.',
                confirmButtonText: 'Entendido'
            });
            return;
        }
        document.getElementById('otrasSenasCliente')?.classList.remove('is-invalid');

        const btnGuardar = document.querySelector('[form="clienteForm"]');
        const originalHtml = btnGuardar ? btnGuardar.innerHTML : '';
        if (btnGuardar) {
            btnGuardar.disabled = true;
            btnGuardar.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Guardando...';
        }

        const formData = new FormData(form);

        fetch('/clientes/guardar', {
            method: 'POST',
            body: new URLSearchParams(formData),
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                if (clienteModal) clienteModal.hide();
                Swal.fire({
                    icon: 'success',
                    title: '¡Guardado!',
                    text: data.success,
                    timer: 2000,
                    showConfirmButton: false
                }).then(() => location.reload());
            } else {
                Swal.fire({
                    icon: 'error',
                    title: 'Error al guardar',
                    text: data.error || 'Ocurrió un error al guardar el cliente',
                    confirmButtonText: 'Entendido'
                });
            }
        })
        .catch(error => {
            console.error('Error al guardar cliente:', error);
            Swal.fire({
                icon: 'error',
                title: 'Error de conexión',
                text: 'No se pudo conectar con el servidor. Intente de nuevo.',
                confirmButtonText: 'Entendido'
            });
        })
        .finally(() => {
            if (btnGuardar) {
                btnGuardar.disabled = false;
                btnGuardar.innerHTML = originalHtml;
            }
        });
    });
});

