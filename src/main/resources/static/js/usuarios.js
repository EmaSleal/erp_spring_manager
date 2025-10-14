/**
 * USUARIOS.JS - Gestión de Usuarios
 * Maneja la interacción del lado del cliente para la gestión de usuarios
 */

$(document).ready(function() {
    // Inicializar tooltips de Bootstrap
    initTooltips();
    
    // Configurar eventos de formulario
    setupFormValidation();
    
    // Configurar eventos de tabla
    setupTableEvents();
    
    // Configurar modal de reset password
    setupResetPasswordModal();
    
    // Configurar switch de estado
    setupEstadoSwitch();
});

/**
 * Inicializa todos los tooltips de Bootstrap
 */
function initTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

/**
 * Configura la validación del formulario de usuario
 */
function setupFormValidation() {
    const form = document.getElementById('formUsuario');
    if (!form) return;
    
    // Validar en tiempo real
    const inputs = form.querySelectorAll('input, select');
    inputs.forEach(input => {
        input.addEventListener('blur', function() {
            validateField(this);
            checkFormCompletion(); // Verificar si el formulario está completo
        });
        
        input.addEventListener('input', function() {
            if (this.classList.contains('is-invalid')) {
                validateField(this);
            }
            checkFormCompletion(); // Verificar en cada cambio
        });
    });
    
    // Validación especial para contraseñas
    const password = document.getElementById('password');
    const passwordConfirm = document.getElementById('passwordConfirmacion');
    
    if (password && passwordConfirm) {
        passwordConfirm.addEventListener('input', function() {
            validatePasswordMatch();
            checkFormCompletion();
        });
        
        password.addEventListener('input', function() {
            // Validar longitud mínima en tiempo real
            if (this.value.length > 0 && this.value.length < 6) {
                this.classList.add('is-invalid');
                this.classList.remove('is-valid');
                const feedback = this.parentElement.querySelector('.invalid-feedback');
                if (feedback) {
                    feedback.textContent = 'La contraseña debe tener al menos 6 caracteres';
                    feedback.style.display = 'block';
                }
            } else if (this.value.length >= 6) {
                this.classList.remove('is-invalid');
                this.classList.add('is-valid');
                const feedback = this.parentElement.querySelector('.invalid-feedback');
                if (feedback) {
                    feedback.style.display = 'none';
                }
            }
            
            if (passwordConfirm.value) {
                validatePasswordMatch();
            }
            checkFormCompletion();
        });
    }
    
    // Validación al enviar
    form.addEventListener('submit', function(event) {
        event.preventDefault();
        event.stopPropagation();
        
        if (validateForm()) {
            // Agregar loading state al botón
            const submitBtn = form.querySelector('button[type="submit"]');
            const originalText = submitBtn.innerHTML;
            submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Guardando...';
            submitBtn.disabled = true;
            
            form.submit();
        }
    });
    
    // Verificar estado inicial del formulario
    checkFormCompletion();
}

/**
 * Valida un campo individual
 */
function validateField(field) {
    const isValid = field.checkValidity();
    
    if (isValid) {
        field.classList.remove('is-invalid');
        field.classList.add('is-valid');
    } else {
        field.classList.remove('is-valid');
        field.classList.add('is-invalid');
    }
    
    return isValid;
}

/**
 * Valida que las contraseñas coincidan
 */
function validatePasswordMatch() {
    const password = document.getElementById('password');
    const passwordConfirm = document.getElementById('passwordConfirmacion');
    
    if (!password || !passwordConfirm) return true;
    
    const match = password.value === passwordConfirm.value;
    const feedback = passwordConfirm.parentElement.querySelector('.invalid-feedback');
    
    if (passwordConfirm.value.length === 0) {
        // Si está vacío, no mostrar error aún
        passwordConfirm.classList.remove('is-invalid');
        passwordConfirm.classList.remove('is-valid');
        if (feedback) feedback.style.display = 'none';
        return false;
    }
    
    if (match && password.value.length >= 6) {
        passwordConfirm.classList.remove('is-invalid');
        passwordConfirm.classList.add('is-valid');
        if (feedback) {
            feedback.style.display = 'none';
        }
    } else {
        passwordConfirm.classList.remove('is-valid');
        passwordConfirm.classList.add('is-invalid');
        if (feedback) {
            feedback.textContent = match ? 'La contraseña debe tener al menos 6 caracteres' : 'Las contraseñas no coinciden';
            feedback.style.display = 'block';
        }
    }
    
    return match && password.value.length >= 6;
}

/**
 * Valida todo el formulario
 */
function validateForm() {
    const form = document.getElementById('formUsuario');
    let isValid = true;
    
    // Validar todos los campos requeridos
    const requiredFields = form.querySelectorAll('[required]');
    requiredFields.forEach(field => {
        if (!validateField(field)) {
            isValid = false;
        }
    });
    
    // Validar contraseñas si existen
    const password = document.getElementById('password');
    const passwordConfirm = document.getElementById('passwordConfirmacion');
    
    if (password && passwordConfirm && password.value) {
        if (!validatePasswordMatch()) {
            isValid = false;
        }
    }
    
    // Validar teléfono (8 dígitos)
    const telefono = document.getElementById('telefono');
    if (telefono && telefono.value) {
        const telefonoRegex = /^[0-9]{8}$/;
        if (!telefonoRegex.test(telefono.value)) {
            telefono.classList.add('is-invalid');
            const feedback = telefono.parentElement.querySelector('.invalid-feedback');
            if (feedback) {
                feedback.textContent = 'El teléfono debe tener exactamente 8 dígitos';
                feedback.style.display = 'block';
            }
            isValid = false;
        }
    }
    
    // Validar email
    const email = document.getElementById('email');
    if (email && email.value) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email.value)) {
            email.classList.add('is-invalid');
            const feedback = email.parentElement.querySelector('.invalid-feedback');
            if (feedback) {
                feedback.textContent = 'Ingresa un email válido (ejemplo: usuario@dominio.com)';
                feedback.style.display = 'block';
            }
            isValid = false;
        }
    }
    
    if (!isValid) {
        mostrarAlerta('Por favor, completa todos los campos correctamente', 'warning');
    }
    
    return isValid;
}

/**
 * Verifica si el formulario está completo y habilita/deshabilita el botón submit
 */
function checkFormCompletion() {
    const form = document.getElementById('formUsuario');
    if (!form) return;
    
    const submitBtn = form.querySelector('button[type="submit"]');
    if (!submitBtn) return;
    
    // Obtener todos los campos requeridos
    const nombre = document.getElementById('nombre');
    const telefono = document.getElementById('telefono');
    const email = document.getElementById('email');
    const rol = document.getElementById('rol');
    const password = document.getElementById('password');
    const passwordConfirm = document.getElementById('passwordConfirmacion');
    
    let isComplete = true;
    let hasErrors = false;
    
    // Verificar campos básicos
    if (!nombre || !nombre.value.trim() || nombre.value.length < 3) isComplete = false;
    if (!telefono || !telefono.value.trim() || telefono.value.length !== 8) isComplete = false;
    if (!email || !email.value.trim() || !email.value.includes('@')) isComplete = false;
    if (!rol || !rol.value) isComplete = false;
    
    // Verificar contraseñas (solo en modo creación)
    if (password && passwordConfirm) {
        if (!password.value || password.value.length < 6) isComplete = false;
        if (!passwordConfirm.value || passwordConfirm.value !== password.value) isComplete = false;
    }
    
    // Verificar si hay campos con errores
    const invalidFields = form.querySelectorAll('.is-invalid');
    if (invalidFields.length > 0) hasErrors = true;
    
    // Habilitar o deshabilitar botón
    if (isComplete && !hasErrors) {
        submitBtn.disabled = false;
        submitBtn.classList.remove('btn-secondary');
        submitBtn.classList.add('btn-primary');
        submitBtn.title = '';
    } else {
        submitBtn.disabled = true;
        submitBtn.classList.remove('btn-primary');
        submitBtn.classList.add('btn-secondary');
        submitBtn.title = 'Completa todos los campos correctamente para habilitar';
    }
}

/**
 * Configura el toggle de visibilidad de contraseña
 */
function setupPasswordToggle(btnId, inputId) {
    const btn = document.getElementById(btnId);
    const input = document.getElementById(inputId);
    
    if (!btn || !input) return;
    
    btn.addEventListener('click', function(e) {
        e.preventDefault(); // Prevenir submit del formulario
        const icon = btn.querySelector('i');
        
        if (input.type === 'password') {
            input.type = 'text';
            icon.classList.remove('bi-eye-fill');
            icon.classList.add('bi-eye-slash-fill');
            btn.title = 'Ocultar contraseña';
        } else {
            input.type = 'password';
            icon.classList.remove('bi-eye-slash-fill');
            icon.classList.add('bi-eye-fill');
            btn.title = 'Mostrar contraseña';
        }
    });
}

/**
 * Genera una contraseña segura aleatoria
 */
function generarPasswordSegura(e) {
    if (e) e.preventDefault(); // Prevenir submit del formulario
    
    const length = 12;
    const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%&*";
    let password = "";
    
    // Asegurar que tenga al menos un carácter de cada tipo
    password += "ABCDEFGHIJKLMNOPQRSTUVWXYZ"[Math.floor(Math.random() * 26)]; // Mayúscula
    password += "abcdefghijklmnopqrstuvwxyz"[Math.floor(Math.random() * 26)]; // Minúscula
    password += "0123456789"[Math.floor(Math.random() * 10)]; // Número
    password += "@#$%&*"[Math.floor(Math.random() * 6)]; // Símbolo
    
    // Completar el resto
    for (let i = password.length; i < length; i++) {
        password += charset[Math.floor(Math.random() * charset.length)];
    }
    
    // Mezclar los caracteres
    password = password.split('').sort(() => Math.random() - 0.5).join('');
    
    // Asignar al campo
    const passwordInput = document.getElementById('password');
    const passwordConfirmInput = document.getElementById('passwordConfirmacion');
    
    if (passwordInput) {
        passwordInput.value = password;
        passwordInput.type = 'text'; // Mostrar la contraseña generada
        passwordInput.classList.remove('is-invalid');
        passwordInput.classList.add('is-valid');
        
        // Actualizar ícono del botón toggle
        const btnToggle = document.getElementById('btnTogglePassword');
        if (btnToggle) {
            const icon = btnToggle.querySelector('i');
            if (icon) {
                icon.classList.remove('bi-eye-fill');
                icon.classList.add('bi-eye-slash-fill');
            }
        }
    }
    
    if (passwordConfirmInput) {
        passwordConfirmInput.value = password;
        passwordConfirmInput.type = 'text'; // Mostrar la contraseña generada
        passwordConfirmInput.classList.remove('is-invalid');
        passwordConfirmInput.classList.add('is-valid');
        
        // Actualizar ícono del botón toggle
        const btnToggleConfirm = document.getElementById('btnTogglePasswordConfirm');
        if (btnToggleConfirm) {
            const icon = btnToggleConfirm.querySelector('i');
            if (icon) {
                icon.classList.remove('bi-eye-fill');
                icon.classList.add('bi-eye-slash-fill');
            }
        }
    }
    
    // Mostrar notificación con la contraseña
    Swal.fire({
        title: '¡Contraseña Generada!',
        html: `<p class="mb-3">Contraseña generada correctamente:</p>
               <div class="alert alert-info">
                   <code style="font-size: 1.2em; user-select: all;">${password}</code>
               </div>
               <p class="text-warning small mb-0">
                   <i class="bi bi-exclamation-triangle-fill me-1"></i>
                   Asegúrate de copiarla y guardarla en un lugar seguro
               </p>`,
        icon: 'success',
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#198754'
    });
}

/**
 * Configura los eventos de la tabla de usuarios
 */
function setupTableEvents() {
    // Evento para eliminar usuario
    $(document).on('click', '.btn-eliminar', function() {
        const id = $(this).data('id');
        const nombre = $(this).data('nombre');
        
        confirmarEliminacion(id, nombre);
    });
    
    // Evento para toggle activo/inactivo
    $(document).on('click', '.btn-toggle-active', function() {
        const id = $(this).data('id');
        const nombre = $(this).data('nombre');
        const activo = $(this).data('activo');
        
        toggleEstadoUsuario(id, nombre, activo);
    });
    
    // Evento para reenviar credenciales por email
    $(document).on('click', '.btn-reenviar-credenciales', function() {
        const id = $(this).data('id');
        const nombre = $(this).data('nombre');
        const email = $(this).data('email');
        
        if (!email) {
            mostrarAlerta('El usuario no tiene email configurado', 'warning');
            return;
        }
        
        reenviarCredenciales(id, nombre, email);
    });
}

/**
 * Confirma y ejecuta la eliminación de un usuario
 */
function confirmarEliminacion(id, nombre) {
    Swal.fire({
        title: '¿Eliminar Usuario?',
        html: `¿Estás seguro de que deseas eliminar al usuario <strong>${nombre}</strong>?<br><br>
               <span class="text-danger">Esta acción no se puede deshacer.</span>`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: '<i class="bi bi-trash-fill"></i> Sí, eliminar',
        cancelButtonText: '<i class="bi bi-x-circle"></i> Cancelar',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            eliminarUsuario(id);
        }
    });
}

/**
 * Elimina un usuario mediante AJAX
 */
function eliminarUsuario(id) {
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
    
    $.ajax({
        url: `/usuarios/delete/${id}`,
        type: 'DELETE',
        beforeSend: function(xhr) {
            if (csrfToken && csrfHeader) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        },
        success: function(response) {
            Swal.fire({
                title: '¡Eliminado!',
                text: 'El usuario ha sido eliminado correctamente',
                icon: 'success',
                timer: 2000,
                showConfirmButton: false
            }).then(() => {
                window.location.reload();
            });
        },
        error: function(xhr) {
            const errorMsg = xhr.responseText || 'Error al eliminar el usuario';
            Swal.fire({
                title: 'Error',
                text: errorMsg,
                icon: 'error',
                confirmButtonText: 'Aceptar'
            });
        }
    });
}

/**
 * Toggle del estado activo/inactivo de un usuario
 */
function toggleEstadoUsuario(id, nombre, activo) {
    const accion = activo ? 'desactivar' : 'activar';
    const estado = activo ? 'inactivo' : 'activo';
    
    Swal.fire({
        title: `¿${accion.charAt(0).toUpperCase() + accion.slice(1)} Usuario?`,
        html: `¿Estás seguro de que deseas ${accion} al usuario <strong>${nombre}</strong>?<br><br>
               ${activo ? '<span class="text-warning">Los usuarios inactivos no podrán iniciar sesión.</span>' : 
                          '<span class="text-success">El usuario podrá iniciar sesión nuevamente.</span>'}`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: activo ? '#ffc107' : '#198754',
        cancelButtonColor: '#6c757d',
        confirmButtonText: `<i class="bi ${activo ? 'bi-x-circle-fill' : 'bi-check-circle-fill'}"></i> Sí, ${accion}`,
        cancelButtonText: '<i class="bi bi-x-circle"></i> Cancelar',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            cambiarEstadoUsuario(id, nombre);
        }
    });
}

/**
 * Cambia el estado de un usuario mediante AJAX
 */
function cambiarEstadoUsuario(id, nombre) {
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
    
    $.ajax({
        url: `/usuarios/toggle-active/${id}`,
        type: 'POST',
        beforeSend: function(xhr) {
            if (csrfToken && csrfHeader) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        },
        success: function(response) {
            Swal.fire({
                title: '¡Estado Actualizado!',
                text: `El usuario ${nombre} ha sido actualizado correctamente`,
                icon: 'success',
                timer: 2000,
                showConfirmButton: false
            }).then(() => {
                window.location.reload();
            });
        },
        error: function(xhr) {
            const errorMsg = xhr.responseText || 'Error al cambiar el estado del usuario';
            Swal.fire({
                title: 'Error',
                text: errorMsg,
                icon: 'error',
                confirmButtonText: 'Aceptar'
            });
        }
    });
}

/**
 * Configura el modal de reset password
 */
function setupResetPasswordModal() {
    const modal = document.getElementById('modalResetPassword');
    if (!modal) return;
    
    let usuarioIdActual = null;
    
    // Abrir modal
    $(document).on('click', '.btn-reset-password', function() {
        usuarioIdActual = $(this).data('id');
        const nombre = $(this).data('nombre');
        
        $('#resetPasswordNombre').text(nombre);
        $('#nuevaPasswordContainer').addClass('d-none');
        $('#btnConfirmarReset').show();
        
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
    });
    
    // Confirmar reset
    $('#btnConfirmarReset').on('click', function() {
        if (!usuarioIdActual) return;
        
        const btn = $(this);
        btn.prop('disabled', true).addClass('loading');
        
        resetearPassword(usuarioIdActual, btn);
    });
    
    // Copiar password
    $('#btnCopiarPassword').on('click', function() {
        const password = $('#nuevaPassword').val();
        
        navigator.clipboard.writeText(password).then(() => {
            const btn = $(this);
            const originalHtml = btn.html();
            
            btn.html('<i class="bi bi-check"></i> Copiado');
            btn.removeClass('btn-outline-secondary').addClass('btn-success');
            
            setTimeout(() => {
                btn.html(originalHtml);
                btn.removeClass('btn-success').addClass('btn-outline-secondary');
            }, 2000);
            
            mostrarAlerta('Contraseña copiada al portapapeles', 'success');
        }).catch(err => {
            mostrarAlerta('Error al copiar la contraseña', 'error');
        });
    });
}

/**
 * Resetea la contraseña de un usuario
 */
function resetearPassword(id, btn) {
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
    
    $.ajax({
        url: `/usuarios/reset-password/${id}`,
        type: 'POST',
        beforeSend: function(xhr) {
            if (csrfToken && csrfHeader) {
                xhr.setRequestHeader(csrfHeader, csrfToken);
            }
        },
        success: function(response) {
            // Mostrar la nueva contraseña
            $('#nuevaPassword').val(response);
            $('#nuevaPasswordContainer').removeClass('d-none');
            btn.hide();
            
            mostrarAlerta('Contraseña generada correctamente. Asegúrate de copiarla.', 'success');
        },
        error: function(xhr) {
            const errorMsg = xhr.responseText || 'Error al resetear la contraseña';
            mostrarAlerta(errorMsg, 'error');
            
            btn.prop('disabled', false).removeClass('loading');
        }
    });
}

/**
 * Reenvía las credenciales de un usuario por email
 */
function reenviarCredenciales(id, nombre, email) {
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
    
    // Confirmación con SweetAlert2
    Swal.fire({
        title: '📧 Reenviar Credenciales',
        html: `
            <p>Se generará una nueva contraseña temporal y se enviará por email a:</p>
            <p class="fw-bold text-primary">${nombre}</p>
            <p class="text-muted">${email}</p>
            <hr>
            <p class="text-warning"><i class="bi bi-exclamation-triangle"></i> La contraseña actual será reemplazada.</p>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: '<i class="bi bi-send-fill"></i> Enviar',
        cancelButtonText: '<i class="bi bi-x"></i> Cancelar',
        confirmButtonColor: '#0d6efd',
        cancelButtonColor: '#6c757d',
        showLoaderOnConfirm: true,
        preConfirm: () => {
            return $.ajax({
                url: `/usuarios/${id}/reenviar-credenciales`,
                type: 'POST',
                beforeSend: function(xhr) {
                    if (csrfToken && csrfHeader) {
                        xhr.setRequestHeader(csrfHeader, csrfToken);
                    }
                }
            });
        },
        allowOutsideClick: () => !Swal.isLoading()
    }).then((result) => {
        if (result.isConfirmed) {
            if (result.value.success) {
                Swal.fire({
                    title: '✅ Credenciales Enviadas',
                    html: `
                        <p>${result.value.message}</p>
                        <hr>
                        <p class="text-muted small">
                            <i class="bi bi-info-circle"></i> 
                            El usuario recibirá un email con sus nuevas credenciales de acceso.
                        </p>
                    `,
                    icon: 'success',
                    confirmButtonColor: '#198754'
                });
            } else {
                Swal.fire({
                    title: '❌ Error',
                    text: result.value.message || 'No se pudieron enviar las credenciales',
                    icon: 'error',
                    confirmButtonColor: '#dc3545'
                });
            }
        }
    }).catch((error) => {
        console.error('Error al reenviar credenciales:', error);
        Swal.fire({
            title: '❌ Error',
            text: error.responseJSON?.message || 'Error al conectar con el servidor',
            icon: 'error',
            confirmButtonColor: '#dc3545'
        });
    });
}

/**
 * Configura el switch de estado en el formulario
 */
function setupEstadoSwitch() {
    const estadoSwitch = document.getElementById('activo');
    const estadoLabel = document.getElementById('estadoLabel');
    
    if (!estadoSwitch || !estadoLabel) return;
    
    estadoSwitch.addEventListener('change', function() {
        if (this.checked) {
            estadoLabel.textContent = 'Usuario activo';
            estadoLabel.classList.remove('text-danger');
            estadoLabel.classList.add('text-success');
        } else {
            estadoLabel.textContent = 'Usuario inactivo';
            estadoLabel.classList.remove('text-success');
            estadoLabel.classList.add('text-danger');
        }
    });
    
    // Trigger inicial
    estadoSwitch.dispatchEvent(new Event('change'));
}

/**
 * Muestra una alerta con SweetAlert2
 */
function mostrarAlerta(mensaje, tipo = 'info') {
    const iconos = {
        success: 'success',
        error: 'error',
        warning: 'warning',
        info: 'info'
    };
    
    Swal.fire({
        text: mensaje,
        icon: iconos[tipo] || 'info',
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true
    });
}

/**
 * Auto-submit del formulario de filtros al cambiar valores
 */
$(document).ready(function() {
    const filtrosForm = document.getElementById('filtrosForm');
    if (!filtrosForm) return;
    
    // Auto-submit en cambios de select (opcional)
    $('#rol, #activo, #sortBy, #sortDir').on('change', function() {
        // Comentado para permitir cambios múltiples antes de buscar
        // filtrosForm.submit();
    });
});

/**
 * Inicialización de botones de contraseña en el formulario
 * Se ejecuta cuando el DOM está listo
 */
document.addEventListener('DOMContentLoaded', function() {
    // Configurar botones de toggle password
    setupPasswordToggle('btnTogglePassword', 'password');
    setupPasswordToggle('btnTogglePasswordConfirm', 'passwordConfirmacion');
    
    // Configurar botón generar password
    const btnGenerar = document.getElementById('btnGenerarPassword');
    if (btnGenerar) {
        btnGenerar.addEventListener('click', generarPasswordSegura);
    }
    
    // Validación en tiempo real para email
    const emailInput = document.getElementById('email');
    if (emailInput) {
        emailInput.addEventListener('input', function() {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (this.value && !emailRegex.test(this.value)) {
                this.classList.add('is-invalid');
                this.classList.remove('is-valid');
            } else if (this.value) {
                this.classList.remove('is-invalid');
                this.classList.add('is-valid');
            }
        });
    }
    
    // Validación en tiempo real para teléfono
    const telefonoInput = document.getElementById('telefono');
    if (telefonoInput) {
        telefonoInput.addEventListener('input', function() {
            // Solo permitir números
            this.value = this.value.replace(/[^0-9]/g, '');
            
            // Validar longitud de 8 dígitos
            if (this.value.length === 8) {
                this.classList.remove('is-invalid');
                this.classList.add('is-valid');
            } else if (this.value.length > 0) {
                this.classList.add('is-invalid');
                this.classList.remove('is-valid');
            }
        });
    }
    
    // Validación en tiempo real para contraseña
    const passwordInput = document.getElementById('password');
    const passwordConfirmInput = document.getElementById('passwordConfirmacion');
    
    if (passwordInput) {
        passwordInput.addEventListener('input', function() {
            if (this.value.length >= 6) {
                this.classList.remove('is-invalid');
                this.classList.add('is-valid');
                
                // Validar también la confirmación si tiene valor
                if (passwordConfirmInput && passwordConfirmInput.value) {
                    validatePasswordMatch();
                }
            } else if (this.value.length > 0) {
                this.classList.add('is-invalid');
                this.classList.remove('is-valid');
            }
        });
    }
});
