/**
 * ============================================================================
 * COMMON.JS - Funcionalidades Globales
 * ERP Orders Manager
 * ============================================================================
 * Utilidades compartidas por toda la aplicación
 */

// ============================================================================
// CONFIGURACIÓN GLOBAL
// ============================================================================
const APP_CONFIG = {
    name: 'ERP Orders Manager',
    version: '1.0.0',
    apiBaseUrl: window.location.origin,
    csrf: {
        token: document.querySelector('meta[name="_csrf"]')?.content || '',
        header: document.querySelector('meta[name="_csrf_header"]')?.content || ''
    }
};

// ============================================================================
// UTILIDADES GENERALES
// ============================================================================

/**
 * Mostrar toast de notificación con SweetAlert2
 * @param {string} type - success, error, warning, info
 * @param {string} message - Mensaje a mostrar
 */
function showToast(type, message) {
    const Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true,
        didOpen: (toast) => {
            toast.addEventListener('mouseenter', Swal.stopTimer);
            toast.addEventListener('mouseleave', Swal.resumeTimer);
        }
    });

    Toast.fire({
        icon: type,
        title: message
    });
}

/**
 * Mostrar confirmación antes de una acción
 * @param {string} title - Título del modal
 * @param {string} text - Texto descriptivo
 * @param {string} confirmButtonText - Texto del botón confirmar
 * @returns {Promise<boolean>} - true si confirma, false si cancela
 */
async function showConfirmDialog(title, text, confirmButtonText = 'Sí, confirmar') {
    const result = await Swal.fire({
        title: title,
        text: text,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#008040',
        cancelButtonColor: '#F44336',
        confirmButtonText: confirmButtonText,
        cancelButtonText: 'Cancelar'
    });

    return result.isConfirmed;
}

/**
 * Mostrar overlay de carga
 */
function showLoading() {
    Swal.fire({
        title: 'Cargando...',
        allowOutsideClick: false,
        allowEscapeKey: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });
}

/**
 * Ocultar overlay de carga
 */
function hideLoading() {
    Swal.close();
}

/**
 * Formatear fecha a formato local
 * @param {string|Date} date - Fecha a formatear
 * @param {string} format - 'short', 'long', 'time'
 * @returns {string} - Fecha formateada
 */
function formatDate(date, format = 'short') {
    const d = new Date(date);
    
    const options = {
        short: { year: 'numeric', month: '2-digit', day: '2-digit' },
        long: { year: 'numeric', month: 'long', day: 'numeric' },
        time: { hour: '2-digit', minute: '2-digit' }
    };

    return d.toLocaleDateString('es-MX', options[format] || options.short);
}

/**
 * Formatear número como moneda
 * @param {number} amount - Cantidad a formatear
 * @param {string} currency - Código de moneda (MXN, USD, etc)
 * @returns {string} - Cantidad formateada
 */
function formatCurrency(amount, currency = 'MXN') {
    return new Intl.NumberFormat('es-MX', {
        style: 'currency',
        currency: currency
    }).format(amount);
}

/**
 * Validar email
 * @param {string} email - Email a validar
 * @returns {boolean} - true si es válido
 */
function isValidEmail(email) {
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
}

/**
 * Validar teléfono mexicano (10 dígitos)
 * @param {string} phone - Teléfono a validar
 * @returns {boolean} - true si es válido
 */
function isValidPhone(phone) {
    const regex = /^\d{10}$/;
    return regex.test(phone.replace(/\D/g, ''));
}

/**
 * Debounce para limitar llamadas a funciones
 * @param {Function} func - Función a ejecutar
 * @param {number} wait - Tiempo de espera en ms
 * @returns {Function} - Función con debounce
 */
function debounce(func, wait = 300) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Hacer petición fetch con manejo de errores y CSRF
 * @param {string} url - URL del endpoint
 * @param {object} options - Opciones de fetch
 * @returns {Promise<any>} - Respuesta parseada
 */
async function fetchWithCsrf(url, options = {}) {
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            [APP_CONFIG.csrf.header]: APP_CONFIG.csrf.token
        },
        ...options
    };

    try {
        const response = await fetch(url, defaultOptions);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        return await response.json();
    } catch (error) {
        console.error('Fetch error:', error);
        showToast('error', 'Error al comunicarse con el servidor');
        throw error;
    }
}

/**
 * Realizar una petición HTTP con manejo completo de estados
 * @param {Object} config - Configuración de la petición
 * @param {string} config.url - URL del endpoint
 * @param {string} config.method - Método HTTP (GET, POST, PUT, DELETE)
 * @param {Object} config.data - Datos a enviar (opcional)
 * @param {boolean} config.showLoading - Mostrar loading (default: true)
 * @param {string} config.successMessage - Mensaje de éxito (opcional)
 * @param {string} config.errorMessage - Mensaje de error personalizado (opcional)
 * @param {Function} config.onSuccess - Callback en caso de éxito (opcional)
 * @param {Function} config.onError - Callback en caso de error (opcional)
 * @param {boolean} config.reloadOnSuccess - Recargar página en éxito (default: false)
 * @param {string} config.redirectOnSuccess - URL para redirigir en éxito (opcional)
 * @returns {Promise<Object>} - Respuesta del servidor
 */
async function httpRequest(config) {
    const {
        url,
        method = 'GET',
        data = null,
        showLoading: shouldShowLoading = true,
        successMessage = null,
        errorMessage = null,
        onSuccess = null,
        onError = null,
        reloadOnSuccess = false,
        redirectOnSuccess = null
    } = config;

    // Mostrar loading si está habilitado
    if (shouldShowLoading) {
        showLoading();
    }

    try {
        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': APP_CONFIG.csrf.token
            }
        };

        // Agregar body si hay datos y no es GET
        if (data && method !== 'GET') {
            options.body = JSON.stringify(data);
        }

        const response = await fetch(url, options);
        const result = await response.json();

        // Ocultar loading
        if (shouldShowLoading) {
            hideLoading();
        }

        // Verificar si hay un error HTTP o un campo 'error' en la respuesta
        if (!response.ok || result.error) {
            // Manejar error del servidor
            const errorMsg = errorMessage || result.error || result.message || 'Error en la operación';
            showToast('error', errorMsg);

            if (onError) {
                await onError(result);
            }

            throw new Error(errorMsg);
        }

        // Verificar si la respuesta fue exitosa (success=true o no tiene error)
        if (result.success !== false) {
            // Mostrar mensaje de éxito si se proporcionó
            if (successMessage) {
                showToast('success', successMessage);
            } else if (result.message) {
                showToast('success', result.message);
            }

            // Ejecutar callback de éxito si existe
            if (onSuccess) {
                await onSuccess(result);
            }

            // Recargar página si se solicitó
            if (reloadOnSuccess) {
                setTimeout(() => location.reload(), 1500);
            }

            // Redirigir si se proporcionó URL
            if (redirectOnSuccess) {
                setTimeout(() => window.location.href = redirectOnSuccess, 1500);
            }

            return result;
        } else {
            // Manejar error del servidor
            const errorMsg = errorMessage || result.message || 'Error en la operación';
            showToast('error', errorMsg);

            if (onError) {
                await onError(result);
            }

            return result;
        }
    } catch (error) {
        // Ocultar loading en caso de error
        if (shouldShowLoading) {
            hideLoading();
        }

        console.error('HTTP Request error:', error);
        const errorMsg = errorMessage || 'Error al comunicarse con el servidor';
        showToast('error', errorMsg);

        if (onError) {
            await onError(error);
        }

        throw error;
    }
}

/**
 * Realizar petición GET
 * @param {string} url - URL del endpoint
 * @param {Object} options - Opciones adicionales
 * @returns {Promise<Object>}
 */
async function httpGet(url, options = {}) {
    return httpRequest({ url, method: 'GET', ...options });
}

/**
 * Realizar petición POST
 * @param {string} url - URL del endpoint
 * @param {Object} data - Datos a enviar
 * @param {Object} options - Opciones adicionales
 * @returns {Promise<Object>}
 */
async function httpPost(url, data, options = {}) {
    return httpRequest({ url, method: 'POST', data, ...options });
}

/**
 * Realizar petición PUT
 * @param {string} url - URL del endpoint
 * @param {Object} data - Datos a enviar
 * @param {Object} options - Opciones adicionales
 * @returns {Promise<Object>}
 */
async function httpPut(url, data = null, options = {}) {
    return httpRequest({ url, method: 'PUT', data, ...options });
}

/**
 * Realizar petición DELETE
 * @param {string} url - URL del endpoint
 * @param {Object} options - Opciones adicionales
 * @returns {Promise<Object>}
 */
async function httpDelete(url, options = {}) {
    return httpRequest({ url, method: 'DELETE', ...options });
}

/**
 * Realizar petición DELETE con confirmación previa
 * @param {string} url - URL del endpoint
 * @param {Object} confirmConfig - Configuración de confirmación
 * @param {string} confirmConfig.title - Título de confirmación
 * @param {string} confirmConfig.text - Texto de confirmación
 * @param {string} confirmConfig.confirmButtonText - Texto del botón
 * @param {Object} options - Opciones adicionales
 * @returns {Promise<Object|null>}
 */
async function httpDeleteWithConfirm(url, confirmConfig = {}, options = {}) {
    const {
        title = '¿Eliminar?',
        text = 'Esta acción no se puede deshacer',
        confirmButtonText = 'Sí, eliminar'
    } = confirmConfig;

    const confirmed = await showConfirmDialog(title, text, confirmButtonText);
    
    if (!confirmed) {
        return null;
    }

    return httpDelete(url, options);
}

// ============================================================================
// MANEJO DE FORMULARIOS
// ============================================================================

/**
 * Agregar validación visual a formularios
 * @param {HTMLFormElement} form - Formulario a validar
 */
function initFormValidation(form) {
    form.addEventListener('submit', function(e) {
        if (!form.checkValidity()) {
            e.preventDefault();
            e.stopPropagation();
        }
        
        form.classList.add('was-validated');
    });

    // Validación en tiempo real
    const inputs = form.querySelectorAll('input, select, textarea');
    inputs.forEach(input => {
        input.addEventListener('blur', function() {
            if (input.validity.valid) {
                input.classList.remove('is-invalid');
                input.classList.add('is-valid');
            } else {
                input.classList.remove('is-valid');
                input.classList.add('is-invalid');
            }
        });
    });
}

/**
 * Limpiar validación de formulario
 * @param {HTMLFormElement} form - Formulario a limpiar
 */
function resetFormValidation(form) {
    form.classList.remove('was-validated');
    const inputs = form.querySelectorAll('input, select, textarea');
    inputs.forEach(input => {
        input.classList.remove('is-valid', 'is-invalid');
    });
}

// ============================================================================
// MANEJO DE TABLAS
// ============================================================================

/**
 * Inicializar búsqueda en tabla
 * @param {string} inputId - ID del input de búsqueda
 * @param {string} tableId - ID de la tabla
 */
function initTableSearch(inputId, tableId) {
    const searchInput = document.getElementById(inputId);
    const table = document.getElementById(tableId);
    
    if (!searchInput || !table) return;

    const searchHandler = debounce(function() {
        const filter = searchInput.value.toLowerCase();
        const rows = table.querySelectorAll('tbody tr');

        rows.forEach(row => {
            const text = row.textContent.toLowerCase();
            row.style.display = text.includes(filter) ? '' : 'none';
        });
    }, 300);

    searchInput.addEventListener('keyup', searchHandler);
}

/**
 * Hacer tabla sortable
 * @param {string} tableId - ID de la tabla
 */
function initTableSort(tableId) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const headers = table.querySelectorAll('th.sortable');
    
    headers.forEach((header, index) => {
        header.addEventListener('click', function() {
            sortTable(table, index);
            updateSortIndicators(headers, header);
        });
    });
}

function sortTable(table, columnIndex) {
    const tbody = table.querySelector('tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    const isAscending = table.dataset.sortOrder !== 'asc';

    rows.sort((a, b) => {
        const aValue = a.cells[columnIndex].textContent.trim();
        const bValue = b.cells[columnIndex].textContent.trim();

        if (isAscending) {
            return aValue.localeCompare(bValue, 'es', { numeric: true });
        } else {
            return bValue.localeCompare(aValue, 'es', { numeric: true });
        }
    });

    rows.forEach(row => tbody.appendChild(row));
    table.dataset.sortOrder = isAscending ? 'asc' : 'desc';
}

function updateSortIndicators(headers, activeHeader) {
    headers.forEach(h => {
        h.classList.remove('sort-asc', 'sort-desc');
    });

    const sortOrder = activeHeader.closest('table').dataset.sortOrder;
    activeHeader.classList.add(sortOrder === 'asc' ? 'sort-asc' : 'sort-desc');
}

// ============================================================================
// OBTENER INICIALES DE NOMBRE
// ============================================================================

/**
 * Obtener iniciales de un nombre completo
 * @param {string} fullName - Nombre completo
 * @returns {string} - Iniciales (máximo 2 caracteres)
 */
function getInitials(fullName) {
    if (!fullName) return '??';
    
    const parts = fullName.trim().split(' ');
    if (parts.length === 1) {
        return parts[0].substring(0, 2).toUpperCase();
    }
    
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/**
 * Generar color de avatar basado en nombre
 * @param {string} name - Nombre
 * @returns {string} - Color hexadecimal
 */
function getAvatarColor(name) {
    const colors = [
        '#1976D2', '#FF9800', '#4CAF50', '#9C27B0',
        '#009688', '#3F51B5', '#E91E63', '#00BCD4'
    ];
    
    const hash = name.split('').reduce((acc, char) => {
        return char.charCodeAt(0) + ((acc << 5) - acc);
    }, 0);
    
    return colors[Math.abs(hash) % colors.length];
}

// ============================================================================
// INICIALIZACIÓN AL CARGAR LA PÁGINA
// ============================================================================
document.addEventListener('DOMContentLoaded', function() {
    console.log(`${APP_CONFIG.name} v${APP_CONFIG.version} - Inicializado`);

    // Inicializar tooltips de Bootstrap si está disponible
    if (typeof bootstrap !== 'undefined') {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }
});

// Exportar para uso global
window.AppUtils = {
    showToast,
    showConfirmDialog,
    showLoading,
    hideLoading,
    formatDate,
    formatCurrency,
    isValidEmail,
    isValidPhone,
    debounce,
    fetchWithCsrf,
    initFormValidation,
    resetFormValidation,
    initTableSearch,
    initTableSort,
    getInitials,
    getAvatarColor,
    httpRequest,
    httpGet,
    httpPost,
    httpPut,
    httpDelete,
    httpDeleteWithConfirm
};
