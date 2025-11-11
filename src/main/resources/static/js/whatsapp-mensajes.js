// ========================================
// Variables Globales
// ========================================
let mensajeModal;
let mensajeModalElement;

// ========================================
// Inicialización
// ========================================
document.addEventListener('DOMContentLoaded', () => {
    // Inicializar modal de Bootstrap
    mensajeModalElement = document.getElementById('mensajeModal');
    if (mensajeModalElement) {
        mensajeModal = new bootstrap.Modal(mensajeModalElement);
    }
});

// ========================================
// Funciones de Filtros
// ========================================
function aplicarFiltros() {
    const estado = document.getElementById('estadoFilter').value;
    const tipo = document.getElementById('tipoFilter').value;
    
    let url = '/whatsapp/mensajes?';
    const params = [];
    
    if (estado) params.push(`estado=${estado}`);
    if (tipo) params.push(`tipo=${tipo}`);
    
    url += params.join('&');
    window.location.href = url;
}

function limpiarFiltros() {
    window.location.href = '/whatsapp/mensajes';
}

// ========================================
// Funciones de Modal
// ========================================
function verDetalleMensaje(button) {
    const mensajeId = button.getAttribute("data-id");
    
    // Obtener token CSRF
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    // Crear headers
    const headers = {};
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    fetch(`/api/whatsapp/mensajes/${mensajeId}`, {
        headers: headers
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Mensaje no encontrado');
            }
            return response.json();
        })
        .then(data => {
            // Datos básicos
            document.getElementById("modal-idWhatsApp").innerText = data.idMensajeWhatsapp || 'N/A';
            document.getElementById("modal-telefono").innerText = data.telefono;
            document.getElementById("modal-mensaje").innerText = data.mensaje || 'Sin mensaje';
            document.getElementById("modal-idUsuario").innerText = data.idUsuario || 'N/A';
            document.getElementById("modal-fechaEnvio").innerText = new Date(data.fechaEnvio).toLocaleString('es-MX');
            
            // Tipo con badge
            const tipoBadge = document.getElementById("modal-tipo");
            if (data.tipo === 'ENVIADO') {
                tipoBadge.innerHTML = '<i class="fas fa-arrow-up me-1"></i><span class="badge bg-info">Enviado</span>';
            } else {
                tipoBadge.innerHTML = '<i class="fas fa-arrow-down me-1"></i><span class="badge bg-primary">Recibido</span>';
            }
            
            // Estado con badge
            const estadoBadge = document.getElementById("modal-estado");
            switch(data.estado) {
                case 'PENDIENTE':
                    estadoBadge.innerHTML = '<span class="badge bg-warning text-dark"><i class="fas fa-clock"></i> Pendiente</span>';
                    break;
                case 'ENVIADO':
                    estadoBadge.innerHTML = '<span class="badge bg-info"><i class="fas fa-paper-plane"></i> Enviado</span>';
                    break;
                case 'ENTREGADO':
                    estadoBadge.innerHTML = '<span class="badge bg-primary"><i class="fas fa-check-double"></i> Entregado</span>';
                    break;
                case 'LEIDO':
                    estadoBadge.innerHTML = '<span class="badge bg-success"><i class="fas fa-check-circle"></i> Leído</span>';
                    break;
                case 'FALLIDO':
                    estadoBadge.innerHTML = '<span class="badge bg-danger"><i class="fas fa-times"></i> Fallido</span>';
                    break;
                default:
                    estadoBadge.innerText = data.estado;
            }
            
            // Plantilla (opcional)
            const plantillaSection = document.getElementById("modal-plantilla-section");
            if (data.nombrePlantilla) {
                plantillaSection.style.display = 'block';
                document.getElementById("modal-plantilla").innerText = data.nombrePlantilla;
            } else {
                plantillaSection.style.display = 'none';
            }
            
            // Mensaje de error (opcional)
            const errorSection = document.getElementById("modal-error-section");
            if (data.mensajeError) {
                errorSection.style.display = 'block';
                document.getElementById("modal-mensajeError").innerText = data.mensajeError;
            } else {
                errorSection.style.display = 'none';
            }
            
            // Mostrar modal
            mensajeModal.show();
            
        })
        .catch(error => {
            console.error('Error al cargar detalle del mensaje:', error);
            mostrarAlerta('Error al cargar el detalle del mensaje', 'danger');
        });
}

// ========================================
// Funciones de Acción
// ========================================
function reintentarFallidos() {
    if (!confirm('¿Deseas reintentar el envío de todos los mensajes fallidos?')) {
        return;
    }
    
    // Obtener token CSRF
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    // Crear headers
    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    
    fetch('/api/whatsapp/mensajes/reintentar', {
        method: 'POST',
        headers: headers
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            mostrarAlerta(data.message, 'success');
            setTimeout(() => {
                window.location.reload();
            }, 2000);
        } else {
            mostrarAlerta(data.message, 'danger');
        }
    })
    .catch(error => {
        console.error('Error al reintentar mensajes:', error);
        mostrarAlerta('Error al reintentar mensajes fallidos', 'danger');
    });
}

// ========================================
// Utilidades
// ========================================
function mostrarAlerta(mensaje, tipo = 'info') {
    // Crear elemento de alerta
    const alerta = document.createElement('div');
    alerta.className = `alert alert-${tipo} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
    alerta.style.zIndex = '9999';
    alerta.innerHTML = `
        ${mensaje}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;
    
    // Agregar al body
    document.body.appendChild(alerta);
    
    // Auto-cerrar después de 5 segundos
    setTimeout(() => {
        alerta.remove();
    }, 5000);
}
