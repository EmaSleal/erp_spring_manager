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
                    cliente.tipoCliente
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
    
    if (clienteModal) {
        clienteModal.show();
    }
});

// Abrir modal para editar cliente existente
function openEditModal(clienteId, nombre, telefono, tipoCliente) {
    document.getElementById('modal-title').innerText = 'Editar Cliente';
    document.getElementById('idCliente').value = clienteId;
    document.getElementById('nombre').value = nombre;
    document.getElementById('telefono').value = telefono || '';
    document.getElementById('tipoCliente').value = tipoCliente || '';
    
    if (clienteModal) {
        clienteModal.show();
    }
}

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

