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

