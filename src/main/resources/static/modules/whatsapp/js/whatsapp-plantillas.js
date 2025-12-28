/**
 * whatsapp-plantillas.js
 * Manejo de la vista de plantillas WhatsApp
 */

// Modal para detalle
let detallePlantillaModal;

document.addEventListener('DOMContentLoaded', function() {
    // Inicializar modal
    const modalElement = document.getElementById('detallePlantillaModal');
    if (modalElement) {
        detallePlantillaModal = new bootstrap.Modal(modalElement);
    }
});

/**
 * Aplica los filtros seleccionados
 */
function aplicarFiltros() {
    const estado = document.getElementById('estadoFilter').value;
    const categoria = document.getElementById('categoriaFilter').value;
    const nombre = document.getElementById('buscarNombre').value;
    
    let url = '/whatsapp/plantillas?';
    const params = [];
    
    if (estado) params.push(`estado=${encodeURIComponent(estado)}`);
    if (categoria) params.push(`categoria=${encodeURIComponent(categoria)}`);
    if (nombre) params.push(`nombre=${encodeURIComponent(nombre)}`);
    
    url += params.join('&');
    window.location.href = url;
}

/**
 * Limpia todos los filtros
 */
function limpiarFiltros() {
    window.location.href = '/whatsapp/plantillas';
}

/**
 * Ver detalle de una plantilla
 */
function verDetallePlantilla(button) {
    const idPlantilla = button.getAttribute('data-id');
    
    // Obtener token CSRF
    const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    
    const headers = {
        'Content-Type': 'application/json'
    };
    headers[header] = token;
    
    fetch(`/api/whatsapp/plantillas/${idPlantilla}`, {
        method: 'GET',
        headers: headers
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Error al obtener detalle de plantilla');
        }
        return response.json();
    })
    .then(data => {
        // Llenar datos en el modal
        document.getElementById('detalle_nombre').textContent = data.nombre || '-';
        document.getElementById('detalle_codigoMeta').textContent = data.codigoMeta || '-';
        
        // Categoría con badge
        let categoriaBadge = '';
        switch(data.categoria) {
            case 'UTILITY':
                categoriaBadge = '<span class="badge bg-info"><i class="fas fa-cog me-1"></i>Transaccional</span>';
                break;
            case 'MARKETING':
                categoriaBadge = '<span class="badge bg-primary"><i class="fas fa-bullhorn me-1"></i>Marketing</span>';
                break;
            case 'AUTHENTICATION':
                categoriaBadge = '<span class="badge bg-warning text-dark"><i class="fas fa-key me-1"></i>Autenticación</span>';
                break;
            default:
                categoriaBadge = data.categoria || '-';
        }
        document.getElementById('detalle_categoria').innerHTML = categoriaBadge;
        
        // Estado Meta con badge
        let estadoBadge = '';
        switch(data.estadoMeta) {
            case 'APPROVED':
                estadoBadge = '<span class="badge bg-success"><i class="fas fa-check-circle"></i> Aprobada</span>';
                break;
            case 'PENDING':
                estadoBadge = '<span class="badge bg-warning text-dark"><i class="fas fa-clock"></i> Pendiente</span>';
                break;
            case 'REJECTED':
                estadoBadge = '<span class="badge bg-danger"><i class="fas fa-times-circle"></i> Rechazada</span>';
                break;
            default:
                estadoBadge = data.estadoMeta || '-';
        }
        document.getElementById('detalle_estadoMeta').innerHTML = estadoBadge;
        
        // Activa
        const activaBadge = data.activo 
            ? '<span class="badge bg-success"><i class="fas fa-toggle-on"></i> Sí</span>'
            : '<span class="badge bg-secondary"><i class="fas fa-toggle-off"></i> No</span>';
        document.getElementById('detalle_activa').innerHTML = activaBadge;
        
        // Contenido
        document.getElementById('detalle_contenido').textContent = data.contenido || '-';
        
        // Parámetros (opcional)
        if (data.parametros) {
            document.getElementById('detalle_parametros_container').style.display = 'block';
            document.getElementById('detalle_parametros').textContent = data.parametros;
        } else {
            document.getElementById('detalle_parametros_container').style.display = 'none';
        }
        
        // Fechas
        document.getElementById('detalle_createDate').textContent = 
            data.createDate ? new Date(data.createDate).toLocaleString('es-MX') : '-';
        
        // Fecha aprobación (opcional)
        if (data.fechaAprobacion) {
            document.getElementById('detalle_fechaAprobacion_container').style.display = 'block';
            document.getElementById('detalle_fechaAprobacion').textContent = 
                new Date(data.fechaAprobacion).toLocaleString('es-MX');
        } else {
            document.getElementById('detalle_fechaAprobacion_container').style.display = 'none';
        }
        
        // Mostrar modal
        detallePlantillaModal.show();
    })
    .catch(error => {
        console.error('Error:', error);
        mostrarAlerta('Error al cargar detalle de plantilla', 'danger');
    });
}

/**
 * Crear nueva plantilla
 */
function crearPlantilla() {
    const form = document.getElementById('formCrearPlantilla');
    const formData = new FormData(form);
    
    const plantilla = {
        nombre: formData.get('nombre'),
        codigoMeta: formData.get('codigoMeta'),
        categoria: formData.get('categoria'),
        idioma: formData.get('idioma'),
        contenido: formData.get('contenido'),
        parametros: formData.get('parametros')
    };
    
    // Validar campos requeridos
    if (!plantilla.nombre || !plantilla.codigoMeta || !plantilla.contenido) {
        mostrarAlerta('Complete todos los campos requeridos', 'warning');
        return;
    }
    
    // Obtener token CSRF
    const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    
    const headers = {
        'Content-Type': 'application/json'
    };
    headers[header] = token;
    
    fetch('/api/whatsapp/plantillas', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(plantilla)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Error al crear plantilla');
        }
        return response.json();
    })
    .then(data => {
        mostrarAlerta('Plantilla creada exitosamente', 'success');
        
        // Cerrar modal
        const modalElement = document.getElementById('crearPlantillaModal');
        const modal = bootstrap.Modal.getInstance(modalElement);
        modal.hide();
        
        // Resetear formulario
        form.reset();
        
        // Recargar página después de 2 segundos
        setTimeout(() => {
            window.location.reload();
        }, 2000);
    })
    .catch(error => {
        console.error('Error:', error);
        mostrarAlerta('Error al crear plantilla', 'danger');
    });
}

/**
 * Activar/desactivar plantilla
 */
function toggleActivar(button, activar) {
    const idPlantilla = button.getAttribute('data-id');
    const accion = activar ? 'activar' : 'desactivar';
    
    if (!confirm(`¿Está seguro que desea ${accion} esta plantilla?`)) {
        return;
    }
    
    // Obtener token CSRF
    const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    
    const headers = {
        'Content-Type': 'application/json'
    };
    headers[header] = token;
    
    const endpoint = activar 
        ? `/api/whatsapp/plantillas/${idPlantilla}/activar`
        : `/api/whatsapp/plantillas/${idPlantilla}/desactivar`;
    
    fetch(endpoint, {
        method: 'PUT',
        headers: headers
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`Error al ${accion} plantilla`);
        }
        return response.json();
    })
    .then(data => {
        mostrarAlerta(`Plantilla ${activar ? 'activada' : 'desactivada'} exitosamente`, 'success');
        
        // Recargar página después de 1 segundo
        setTimeout(() => {
            window.location.reload();
        }, 1000);
    })
    .catch(error => {
        console.error('Error:', error);
        mostrarAlerta(`Error al ${accion} plantilla`, 'danger');
    });
}

/**
 * Muestra una alerta temporal
 */
function mostrarAlerta(mensaje, tipo = 'info') {
    const alertHtml = `
        <div class="alert alert-${tipo} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3" 
             style="z-index: 9999; min-width: 300px;" role="alert">
            ${mensaje}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', alertHtml);
    
    // Auto-remover después de 5 segundos
    setTimeout(() => {
        const alert = document.querySelector('.alert');
        if (alert) {
            alert.remove();
        }
    }, 5000);
}
