/**
 * ============================================================================
 * CONFIGURATION.JS - Utilidades comunes para módulo de configuración
 * ERP Orders Manager - Sprint 4
 * ============================================================================
 */

// ==================== NAMESPACE ====================
const Configuracion = {
    /**
     * Muestra un toast de éxito
     */
    mostrarExito: function(mensaje) {
        this.mostrarAlerta('success', mensaje, 'check-circle');
    },

    /**
     * Muestra un toast de error
     */
    mostrarError: function(mensaje) {
        this.mostrarAlerta('danger', mensaje, 'exclamation-triangle');
    },

    /**
     * Muestra un toast de advertencia
     */
    mostrarAdvertencia: function(mensaje) {
        this.mostrarAlerta('warning', mensaje, 'exclamation-circle');
    },

    /**
     * Muestra un toast de información
     */
    mostrarInfo: function(mensaje) {
        this.mostrarAlerta('info', mensaje, 'info-circle');
    },

    /**
     * Muestra una alerta en el container especificado
     */
    mostrarAlerta: function(tipo, mensaje, icono) {
        const iconMap = {
            'success': 'check-circle',
            'danger': 'exclamation-triangle',
            'warning': 'exclamation-circle',
            'info': 'info-circle'
        };

        const iconoFinal = icono || iconMap[tipo] || 'info-circle';

        const alertHtml = `
            <div class="alert alert-${tipo} alert-dismissible fade show" role="alert">
                <i class="fas fa-${iconoFinal} me-2"></i>
                <strong>${mensaje}</strong>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;

        // Buscar el tab activo y su container de alertas
        const activeTab = document.querySelector('.tab-pane.active');
        if (activeTab) {
            const alertContainer = activeTab.querySelector('[id$="-container"]');
            if (alertContainer) {
                alertContainer.innerHTML = alertHtml;
                alertContainer.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

                // Auto-cerrar después de 5 segundos
                setTimeout(() => {
                    const alert = alertContainer.querySelector('.alert');
                    if (alert) {
                        const bsAlert = new bootstrap.Alert(alert);
                        bsAlert.close();
                    }
                }, 5000);
            }
        }
    },

    /**
     * Muestra una alerta en un container específico
     */
    mostrarAlertaEn: function(containerId, tipo, mensaje, icono) {
        const iconMap = {
            'success': 'check-circle',
            'danger': 'exclamation-triangle',
            'warning': 'exclamation-circle',
            'info': 'info-circle'
        };

        const iconoFinal = icono || iconMap[tipo] || 'info-circle';

        const alertHtml = `
            <div class="alert alert-${tipo} alert-dismissible fade show" role="alert">
                <i class="fas fa-${iconoFinal} me-2"></i>
                <strong>${mensaje}</strong>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;

        const container = document.getElementById(containerId);
        if (container) {
            container.innerHTML = alertHtml;
            container.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

            setTimeout(() => {
                const alert = container.querySelector('.alert');
                if (alert) {
                    const bsAlert = new bootstrap.Alert(alert);
                    bsAlert.close();
                }
            }, 5000);
        }
    },

    /**
     * Limpia las alertas de un container
     */
    limpiarAlertas: function(containerId) {
        const container = document.getElementById(containerId);
        if (container) {
            container.innerHTML = '';
        }
    },

    /**
     * Valida un formulario HTML5
     */
    validarFormulario: function(formId) {
        const form = document.getElementById(formId);
        if (!form) return false;

        // Agregar clase was-validated para mostrar feedback
        form.classList.add('was-validated');

        // Verificar validez
        if (!form.checkValidity()) {
            // Buscar primer campo inválido y hacer focus
            const primerInvalido = form.querySelector(':invalid');
            if (primerInvalido) {
                primerInvalido.focus();
            }
            return false;
        }

        return true;
    },

    /**
     * Obtiene los datos de un formulario como objeto
     */
    obtenerDatosFormulario: function(formId) {
        const form = document.getElementById(formId);
        if (!form) return null;

        const formData = new FormData(form);
        const datos = {};

        for (let [key, value] of formData.entries()) {
            // Manejar checkboxes
            const input = form.querySelector(`[name="${key}"]`);
            if (input && input.type === 'checkbox') {
                datos[key] = input.checked;
            } else if (input && input.type === 'number') {
                datos[key] = value ? parseFloat(value) : null;
            } else {
                datos[key] = value || null;
            }
        }

        return datos;
    },

    /**
     * Carga datos en un formulario
     */
    cargarDatosEnFormulario: function(formId, datos) {
        const form = document.getElementById(formId);
        if (!form || !datos) return;

        for (let [key, value] of Object.entries(datos)) {
            const input = form.querySelector(`[name="${key}"], #${key}`);
            if (input) {
                if (input.type === 'checkbox') {
                    input.checked = !!value;
                } else if (input.type === 'radio') {
                    const radio = form.querySelector(`[name="${key}"][value="${value}"]`);
                    if (radio) radio.checked = true;
                } else {
                    input.value = value !== null && value !== undefined ? value : '';
                }

                // Actualizar color pickers si existen
                if (input.type === 'color' && value) {
                    const textInput = form.querySelector(`#${key}Text`);
                    if (textInput) textInput.value = value;
                }
            }
        }
    },

    /**
     * Limpia un formulario
     */
    limpiarFormulario: function(formId) {
        const form = document.getElementById(formId);
        if (!form) return;

        form.reset();
        form.classList.remove('was-validated');
    },

    /**
     * Obtiene el token CSRF de las meta tags
     */
    getCsrfToken: function() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        return { token, header };
    },

    /**
     * Realiza una petición GET a la API
     */
    get: async function(url) {
        try {
            const csrf = this.getCsrfToken();
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };
            
            if (csrf.token && csrf.header) {
                headers[csrf.header] = csrf.token;
            }

            const response = await fetch(url, {
                method: 'GET',
                headers: headers
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error en GET:', error);
            throw error;
        }
    },

    /**
     * Realiza una petición POST a la API
     */
    post: async function(url, datos) {
        try {
            const csrf = this.getCsrfToken();
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };
            
            if (csrf.token && csrf.header) {
                headers[csrf.header] = csrf.token;
            }

            const response = await fetch(url, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(datos)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error en POST:', error);
            throw error;
        }
    },

    /**
     * Realiza una petición PUT a la API
     */
    put: async function(url, datos) {
        try {
            const csrf = this.getCsrfToken();
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };
            
            if (csrf.token && csrf.header) {
                headers[csrf.header] = csrf.token;
            }

            const response = await fetch(url, {
                method: 'PUT',
                headers: headers,
                body: JSON.stringify(datos)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error en PUT:', error);
            throw error;
        }
    },

    /**
     * Realiza una petición PATCH a la API
     */
    patch: async function(url, datos) {
        try {
            const csrf = this.getCsrfToken();
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };
            
            if (csrf.token && csrf.header) {
                headers[csrf.header] = csrf.token;
            }

            const response = await fetch(url, {
                method: 'PATCH',
                headers: headers,
                body: JSON.stringify(datos)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error en PATCH:', error);
            throw error;
        }
    },

    /**
     * Realiza una petición DELETE a la API
     */
    delete: async function(url) {
        try {
            const csrf = this.getCsrfToken();
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };
            
            if (csrf.token && csrf.header) {
                headers[csrf.header] = csrf.token;
            }

            const response = await fetch(url, {
                method: 'DELETE',
                headers: headers
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error en DELETE:', error);
            throw error;
        }
    },

    /**
     * Muestra un spinner en un botón
     */
    mostrarSpinner: function(botonId) {
        const boton = document.getElementById(botonId);
        if (!boton) return;

        // Guardar texto original
        boton.dataset.textoOriginal = boton.innerHTML;

        // Mostrar spinner
        boton.disabled = true;
        boton.innerHTML = `
            <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
            Procesando...
        `;
    },

    /**
     * Oculta el spinner de un botón
     */
    ocultarSpinner: function(botonId) {
        const boton = document.getElementById(botonId);
        if (!boton) return;

        boton.disabled = false;
        if (boton.dataset.textoOriginal) {
            boton.innerHTML = boton.dataset.textoOriginal;
            delete boton.dataset.textoOriginal;
        }
    },

    /**
     * Confirma una acción con SweetAlert2
     */
    confirmar: async function(titulo, texto, textoBotonConfirmar = 'Sí, confirmar') {
        if (typeof Swal !== 'undefined') {
            const result = await Swal.fire({
                title: titulo,
                text: texto,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#d33',
                cancelButtonColor: '#6c757d',
                confirmButtonText: textoBotonConfirmar,
                cancelButtonText: 'Cancelar'
            });
            return result.isConfirmed;
        } else {
            return confirm(`${titulo}\n\n${texto}`);
        }
    },

    /**
     * Formatea una fecha al formato local
     */
    formatearFecha: function(fecha) {
        if (!fecha) return '';
        const date = new Date(fecha);
        return date.toLocaleDateString('es-PE', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        });
    },

    /**
     * Formatea una fecha y hora al formato local
     */
    formatearFechaHora: function(fecha) {
        if (!fecha) return '';
        const date = new Date(fecha);
        return date.toLocaleString('es-PE', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    },

    /**
     * Sincroniza color picker con input de texto
     */
    sincronizarColorPicker: function(colorPickerId, textInputId) {
        const colorPicker = document.getElementById(colorPickerId);
        const textInput = document.getElementById(textInputId);

        if (colorPicker && textInput) {
            colorPicker.addEventListener('input', function() {
                textInput.value = this.value;
            });
        }
    }
};

// ==================== VALIDACIÓN DE FORMULARIOS ====================
(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
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
    });
})();

// ==================== INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    console.log('✅ Configuration JS cargado correctamente');

    // Sincronizar color pickers si existen
    Configuracion.sincronizarColorPicker('colorPrimario', 'colorPrimarioText');
    Configuracion.sincronizarColorPicker('colorSecundario', 'colorSecundarioText');

    // Auto-ocultar alertas después de 5 segundos
    setTimeout(function() {
        const alerts = document.querySelectorAll('.alert');
        alerts.forEach(alert => {
            try {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            } catch (e) {
                // Ignorar errores si el alert ya fue cerrado
            }
        });
    }, 5000);
});
