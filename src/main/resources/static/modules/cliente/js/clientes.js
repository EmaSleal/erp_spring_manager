// ============================================================================
// CLIENTES.JS - Gestión del modal de clientes con Bootstrap 5
// ERP Orders Manager
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
        httpGet(`/clientes/detalle/${editId}`, {
            showLoading: false,
            onSuccess: (cliente) => {
                openEditModal(
                    cliente.idCliente,
                    cliente.nombre,
                    cliente.usuario?.telefono || '',
                    cliente.tipoCliente,
                    cliente.requiereFacturaElectronica,
                    cliente.tipoIdentificacion,
                    cliente.numeroIdentificacion,
                    cliente.codigoActividad,
                    cliente.provincia?.codigo || '',
                    cliente.canton,
                    cliente.distrito,
                    cliente.otrasSenas
                );
                window.history.replaceState({}, '', window.location.pathname);
            },
            onError: () => {
                window.history.replaceState({}, '', window.location.pathname);
            }
        });
    }
});

// Abrir modal para agregar nuevo cliente
document.getElementById('open-modal')?.addEventListener('click', function () {
    document.getElementById('modal-title').innerText = 'Agregar Cliente';
    document.getElementById('clienteForm').reset();
    document.getElementById('idCliente').value = '';
    limpiarErroresCliente(document.getElementById('clienteForm'));
    limpiarResultadoHacienda();
    
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
    httpGet(`/clientes/detalle/${clienteId}`, {
        showLoading: false,
        onSuccess: (cliente) => {
            limpiarErroresCliente(document.getElementById('clienteForm'));
            limpiarResultadoHacienda();
            document.getElementById('modal-title').innerText = 'Editar Cliente';
            document.getElementById('idCliente').value = cliente.idCliente || '';
            document.getElementById('nombre').value = cliente.nombre || '';
            document.getElementById('telefono').value = cliente.telefono || '';
            document.getElementById('direccion').value = cliente.direccion || '';
            document.getElementById('email').value = cliente.email || '';
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
        },
        onError: () => {
            showToast('error', 'Error al cargar los datos del cliente');
        }
    });
}

// ============================================================================
// Verificar en Hacienda (T-038, ADR-044 §5)
// Consulta el padrón de contribuyentes con un endpoint de clientes (CLIENTE_CREAR/CLIENTE_EDITAR).
// No guarda nada ni copia solo: ofrece «Copiar al formulario». Una falla de la API no impide guardar.
// ============================================================================
let verificacionHacienda = null; // {nombre, tipoIdentificacionEnum} de la última verificación con resultado

function mostrarResultadoHacienda(texto, tipoAlerta, copiable) {
    const caja = document.getElementById('resultadoHaciendaCliente');
    const cuerpo = document.getElementById('resultadoHaciendaTexto');
    const btnCopiar = document.getElementById('btnCopiarHaciendaCliente');
    if (!caja || !cuerpo) return;
    cuerpo.textContent = texto; // texto del servidor: siempre textContent
    caja.className = 'alert alert-' + tipoAlerta + ' border py-2 px-3 mt-2 small';
    caja.style.display = 'block';
    if (btnCopiar) btnCopiar.style.display = copiable ? 'inline-block' : 'none';
}

function limpiarResultadoHacienda() {
    verificacionHacienda = null;
    const caja = document.getElementById('resultadoHaciendaCliente');
    if (caja) caja.style.display = 'none';
    const cuerpo = document.getElementById('resultadoHaciendaTexto');
    if (cuerpo) cuerpo.textContent = '';
}

document.getElementById('btnConsultarHaciendaCliente')?.addEventListener('click', async function() {
    const numero = document.getElementById('numeroIdentificacionCliente').value.replace(/[\s-]/g, '');

    if (!/^\d{9,12}$/.test(numero)) {
        showToast('warning', 'Ingrese una identificación de 9 a 12 dígitos, sin guiones.');
        return;
    }

    limpiarResultadoHacienda();
    mostrarResultadoHacienda('Consultando a Hacienda...', 'light', false);
    const boton = this;
    boton.disabled = true;
    try {
        const response = await fetch('/api/clientes/hacienda/contribuyente/' + encodeURIComponent(numero), {
            headers: { 'Accept': 'application/json' }
        });
        let datos = {};
        try { datos = await response.json(); } catch (e) { datos = {}; }

        if (response.status === 502) {
            // La API no responde: se avisa, pero el cliente se puede guardar igual.
            mostrarResultadoHacienda(datos.mensaje || 'No se pudo consultar a Hacienda en este momento. Puede guardar el cliente y verificar más tarde.', 'warning', false);
        } else if (!response.ok) {
            mostrarResultadoHacienda(datos.mensaje || 'No se pudo verificar la identificación. Puede guardar el cliente igualmente.', 'warning', false);
        } else if (datos.existe === true) {
            verificacionHacienda = { nombre: datos.nombre || '', tipoIdentificacionEnum: datos.tipoIdentificacionEnum || null };
            const tipo = datos.tipoIdentificacion ? ' | Tipo de identificación: ' + datos.tipoIdentificacion : '';
            mostrarResultadoHacienda('La identificación existe en Hacienda. Nombre: ' + (datos.nombre || '(sin nombre)') + tipo, 'success', true);
        } else {
            mostrarResultadoHacienda(datos.mensaje || 'La identificación no aparece en el padrón de Hacienda.', 'warning', false);
        }
    } catch (e) {
        mostrarResultadoHacienda('No se pudo consultar a Hacienda en este momento. Puede guardar el cliente y verificar más tarde.', 'warning', false);
    } finally {
        boton.disabled = false;
    }
});

// Copia nombre y tipo al formulario solo cuando el usuario lo pide; no guarda nada.
document.getElementById('btnCopiarHaciendaCliente')?.addEventListener('click', function() {
    if (!verificacionHacienda) return;
    if (verificacionHacienda.nombre) {
        document.getElementById('nombre').value = verificacionHacienda.nombre;
    }
    if (verificacionHacienda.tipoIdentificacionEnum) {
        document.getElementById('tipoIdentificacionCliente').value = verificacionHacienda.tipoIdentificacionEnum;
    }
    showToast('success', 'Datos copiados al formulario. Revíselos y guarde el cliente.');
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

        limpiarErroresCliente(form);

        const btnGuardar = document.querySelector('[form="clienteForm"]');
        const originalHtml = btnGuardar ? btnGuardar.innerHTML : '';
        if (btnGuardar) {
            btnGuardar.disabled = true;
            btnGuardar.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Guardando...';
        }

        const formData = new FormData(form);

        const idClienteRaw = formData.get('idCliente');
        const provinciaValue = formData.get('provincia');

        const clienteData = {
            idCliente: idClienteRaw ? parseInt(idClienteRaw) : null,
            nombre: formData.get('nombre') || null,
            telefono: formData.get('telefono') || null,
            direccion: formData.get('direccion') || null,
            email: formData.get('email') || null,
            tipoCliente: formData.get('tipoCliente') || null,
            requiereFacturaElectronica: formData.get('requiereFacturaElectronica') === 'true',
            tipoIdentificacion: formData.get('tipoIdentificacion') || null,
            numeroIdentificacion: formData.get('numeroIdentificacion') || null,
            codigoActividad: formData.get('codigoActividad') || null,
            provincia: provinciaValue ? { codigo: provinciaValue } : null,
            canton: formData.get('canton') || null,
            distrito: formData.get('distrito') || null,
            otrasSenas: formData.get('otrasSenas') || null
        };

        httpPost('/clientes/guardar', clienteData, {
            onSuccess: (data) => {
                if (clienteModal) clienteModal.hide();
                showToast('success', data.success || data.message);
                setTimeout(() => location.reload(), 2000);
            },
            onError: (data) => {
                // 400 con errores por campo: se pintan junto a cada campo; los que no tienen contenedor van al aviso.
                const sinContenedor = data && Array.isArray(data.errores) ? pintarErroresCliente(form, data.errores) : [];
                if (sinContenedor.length > 0) {
                    showToast('error', sinContenedor.map(e => e.mensaje).join(' '));
                }
            }
        }).finally(() => {
            if (btnGuardar) {
                btnGuardar.disabled = false;
                btnGuardar.innerHTML = originalHtml;
            }
        });
    });
});

