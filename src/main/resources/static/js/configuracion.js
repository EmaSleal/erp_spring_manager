/* ====================================================
   CONFIGURACION.JS - Scripts para módulo de configuración
   Proyecto: WhatsApp Orders Manager
   Sprint: 2 - Fase 1
   ==================================================== */

// ==================== VALIDACIÓN DE FORMULARIOS ====================

// Validación de Bootstrap
(function() {
    'use strict';
    
    // Obtener todos los formularios que necesitan validación
    const forms = document.querySelectorAll('.needs-validation');
    
    // Aplicar validación personalizada
    Array.from(forms).forEach(function(form) {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            
            form.classList.add('was-validated');
        }, false);
    });
})();

// ==================== PREVIEW DE LOGO ====================

/**
 * Muestra preview del logo seleccionado
 */
function previewLogo(event) {
    const file = event.target.files[0];
    
    if (file) {
        // Validar tamaño (máximo 2MB)
        if (file.size > 2 * 1024 * 1024) {
            Swal.fire({
                icon: 'error',
                title: 'Archivo muy grande',
                text: 'El logo no puede superar los 2MB',
                confirmButtonColor: '#1976D2'
            });
            event.target.value = '';
            return;
        }
        
        // Validar tipo de archivo
        const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg', 'image/svg+xml'];
        if (!allowedTypes.includes(file.type)) {
            Swal.fire({
                icon: 'error',
                title: 'Formato no válido',
                text: 'Solo se permiten archivos PNG, JPG o SVG',
                confirmButtonColor: '#1976D2'
            });
            event.target.value = '';
            return;
        }
        
        // Mostrar preview
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('logoPreview');
            preview.src = e.target.result;
            preview.style.display = 'block';
            preview.classList.add('img-thumbnail', 'mt-3');
        };
        reader.readAsDataURL(file);
    }
}

/**
 * Sube el logo al servidor
 */
function subirLogo() {
    const fileInput = document.getElementById('logoInput');
    const file = fileInput.files[0];
    
    if (!file) {
        Swal.fire({
            icon: 'warning',
            title: 'Sin archivo',
            text: 'Por favor selecciona un archivo primero',
            confirmButtonColor: '#1976D2'
        });
        return;
    }
    
    // Obtener ID de la empresa
    const empresaId = document.querySelector('input[name="idEmpresa"]').value;
    
    if (!empresaId) {
        Swal.fire({
            icon: 'info',
            title: 'Guarda primero',
            text: 'Debes guardar los datos de la empresa antes de subir el logo',
            confirmButtonColor: '#1976D2'
        });
        return;
    }
    
    // Crear FormData
    const formData = new FormData();
    formData.append('logo', file);
    formData.append('empresaId', empresaId);
    
    // Enviar con fetch
    fetch('/configuracion/empresa/subir-logo', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (response.redirected) {
            window.location.href = response.url;
        }
    })
    .catch(error => {
        console.error('Error:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Ocurrió un error al subir el logo',
            confirmButtonColor: '#1976D2'
        });
    });
}

/**
 * Confirma y elimina el logo
 */
function confirmarEliminarLogo() {
    Swal.fire({
        title: '¿Eliminar logo?',
        text: 'Esta acción no se puede deshacer',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            document.getElementById('formEliminarLogo').submit();
        }
    });
}

// ==================== PREVIEW DE FAVICON ====================

/**
 * Muestra preview del favicon seleccionado
 */
function previewFavicon(event) {
    const file = event.target.files[0];
    
    if (file) {
        // Validar tamaño (máximo 2MB)
        if (file.size > 2 * 1024 * 1024) {
            Swal.fire({
                icon: 'error',
                title: 'Archivo muy grande',
                text: 'El favicon no puede superar los 2MB',
                confirmButtonColor: '#1976D2'
            });
            event.target.value = '';
            return;
        }
        
        // Validar tipo de archivo
        const allowedTypes = ['image/png', 'image/x-icon', 'image/vnd.microsoft.icon'];
        if (!allowedTypes.includes(file.type)) {
            Swal.fire({
                icon: 'error',
                title: 'Formato no válido',
                text: 'Solo se permiten archivos PNG o ICO',
                confirmButtonColor: '#1976D2'
            });
            event.target.value = '';
            return;
        }
        
        // Mostrar preview
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('faviconPreview');
            preview.src = e.target.result;
            preview.style.display = 'block';
            preview.classList.add('favicon-img', 'mt-3');
        };
        reader.readAsDataURL(file);
    }
}

/**
 * Sube el favicon al servidor
 */
function subirFavicon() {
    const fileInput = document.getElementById('faviconInput');
    const file = fileInput.files[0];
    
    if (!file) {
        Swal.fire({
            icon: 'warning',
            title: 'Sin archivo',
            text: 'Por favor selecciona un archivo primero',
            confirmButtonColor: '#1976D2'
        });
        return;
    }
    
    // Obtener ID de la empresa
    const empresaId = document.querySelector('input[name="idEmpresa"]').value;
    
    if (!empresaId) {
        Swal.fire({
            icon: 'info',
            title: 'Guarda primero',
            text: 'Debes guardar los datos de la empresa antes de subir el favicon',
            confirmButtonColor: '#1976D2'
        });
        return;
    }
    
    // Crear FormData
    const formData = new FormData();
    formData.append('favicon', file);
    formData.append('empresaId', empresaId);
    
    // Enviar con fetch
    fetch('/configuracion/empresa/subir-favicon', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (response.redirected) {
            window.location.href = response.url;
        }
    })
    .catch(error => {
        console.error('Error:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Ocurrió un error al subir el favicon',
            confirmButtonColor: '#1976D2'
        });
    });
}

/**
 * Confirma y elimina el favicon
 */
function confirmarEliminarFavicon() {
    Swal.fire({
        title: '¿Eliminar favicon?',
        text: 'Esta acción no se puede deshacer',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            document.getElementById('formEliminarFavicon').submit();
        }
    });
}

// ==================== UTILIDADES ====================

/**
 * Limpia el formulario
 */
function limpiarFormulario() {
    Swal.fire({
        title: '¿Limpiar formulario?',
        text: 'Se perderán los cambios no guardados',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#1976D2',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, limpiar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            // Limpiar formulario
            const form = document.querySelector('form.active') || document.querySelector('form');
            if (form) {
                form.reset();
                form.classList.remove('was-validated');
            }
            
            // Limpiar previews si existen
            const logoPreview = document.getElementById('logoPreview');
            const faviconPreview = document.getElementById('faviconPreview');
            const previewNumero = document.getElementById('previewNumero');
            
            if (logoPreview) logoPreview.style.display = 'none';
            if (faviconPreview) faviconPreview.style.display = 'none';
            
            // Limpiar inputs de archivos
            const logoInput = document.getElementById('logoInput');
            const faviconInput = document.getElementById('faviconInput');
            if (logoInput) logoInput.value = '';
            if (faviconInput) faviconInput.value = '';
            
            // Restaurar preview de facturación
            if (previewNumero) {
                actualizarPreview();
            }
        }
    });
}

// ==================== CONFIGURACIÓN DE FACTURACIÓN ====================

/**
 * Actualiza el preview del número de factura en tiempo real
 */
function actualizarPreview() {
    // Obtener valores de los campos
    const serie = document.getElementById('serieFactura')?.value || 'F001';
    const prefijo = document.getElementById('prefijoFactura')?.value || '';
    const numeroActual = document.getElementById('numeroActual')?.value || '1';
    const formato = document.getElementById('formatoNumero')?.value || '{serie}-{numero}';
    
    // Validar que el formato contenga {numero}
    if (!formato.includes('{numero}')) {
        document.getElementById('previewNumero').textContent = 'Error: El formato debe contener {numero}';
        document.getElementById('previewNumero').classList.add('text-danger');
        return;
    }
    
    // Formatear el número con padding de 5 dígitos
    const numeroFormateado = String(numeroActual).padStart(5, '0');
    
    // Generar preview
    let preview = formato
        .replace('{serie}', serie)
        .replace('{prefijo}', prefijo)
        .replace('{numero}', numeroFormateado);
    
    // Actualizar preview
    const previewElement = document.getElementById('previewNumero');
    if (previewElement) {
        previewElement.textContent = preview;
        previewElement.classList.remove('text-danger');
    }
}

/**
 * Validar campos de configuración de facturación
 */
function validarConfiguracionFacturacion() {
    const form = document.getElementById('formConfiguracionFacturacion');
    
    if (!form) return true;
    
    // Validar formato contiene {numero}
    const formato = document.getElementById('formatoNumero').value;
    if (!formato.includes('{numero}')) {
        Swal.fire({
            icon: 'error',
            title: 'Formato inválido',
            text: 'El formato de número debe contener el placeholder {numero}',
            confirmButtonColor: '#1976D2'
        });
        return false;
    }
    
    // Validar número actual >= número inicial
    const numeroInicial = parseInt(document.getElementById('numeroInicial').value);
    const numeroActual = parseInt(document.getElementById('numeroActual').value);
    
    if (numeroActual < numeroInicial) {
        Swal.fire({
            icon: 'error',
            title: 'Número inválido',
            text: 'El número actual no puede ser menor que el número inicial',
            confirmButtonColor: '#1976D2'
        });
        return false;
    }
    
    // Validar código de moneda (3 letras)
    const moneda = document.getElementById('moneda').value;
    if (moneda.length !== 3) {
        Swal.fire({
            icon: 'error',
            title: 'Código de moneda inválido',
            text: 'El código de moneda debe tener exactamente 3 letras (ISO 4217)',
            confirmButtonColor: '#1976D2'
        });
        return false;
    }
    
    return true;
}

/**
 * Convertir moneda a mayúsculas automáticamente
 */
document.addEventListener('DOMContentLoaded', function() {
    const monedaInput = document.getElementById('moneda');
    if (monedaInput) {
        monedaInput.addEventListener('input', function(e) {
            e.target.value = e.target.value.toUpperCase();
        });
    }
    
    // Agregar validación al submit del formulario de facturación
    const formFacturacion = document.getElementById('formConfiguracionFacturacion');
    if (formFacturacion) {
        formFacturacion.addEventListener('submit', function(e) {
            if (!validarConfiguracionFacturacion()) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
        
        // Actualizar preview inicial
        actualizarPreview();
    }
});

// ==================== INICIALIZACIÓN ====================

document.addEventListener('DOMContentLoaded', function() {
    console.log('Configuración JS cargado');
    
    // Auto-ocultar alertas después de 5 segundos
    setTimeout(function() {
        const alerts = document.querySelectorAll('.alert');
        alerts.forEach(alert => {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        });
    }, 5000);
});
