/**
 * ============================================================================
 * COMMON.JS - Funcionalidades Globales
 * WhatsApp Orders Manager
 * ============================================================================
 * Utilidades compartidas por toda la aplicación
 */

// ============================================================================
// CONFIGURACIÓN GLOBAL
// ============================================================================
const APP_CONFIG = {
    name: 'WhatsApp Orders Manager',
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
        confirmButtonColor: '#1976D2',
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

    // Cerrar dropdowns al hacer click fuera
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.dropdown')) {
            document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
                menu.classList.remove('show');
            });
        }
    });

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
    getAvatarColor
};
