/**
 * ============================================================================
 * USUARIOS ADMIN - JavaScript
 * WhatsApp Orders Manager
 * ============================================================================
 * Manejo de la gestión de usuarios desde el panel de administración.
 * 
 * Funcionalidades:
 * - DataTables para lista de usuarios
 * - Modales de bloqueo/desbloqueo
 * - Cambio de rol y estado
 * - Búsqueda y filtrado
 * - AJAX para operaciones
 * 
 * @version 1.0 - Sprint 4
 * @since 22/12/2025
 * ============================================================================
 */

$(document).ready(function() {
    
    // ==================== DATATABLES ====================
    
    if ($('#usuariosTable').length) {
        const table = $('#usuariosTable').DataTable({
            language: {
                url: '//cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json'
            },
            pageLength: 10,
            order: [[0, 'asc']],
            columnDefs: [
                { orderable: false, targets: -1 } // Desactivar ordenamiento en columna de acciones
            ],
            dom: '<"row"<"col-sm-12 col-md-6"l><"col-sm-12 col-md-6"f>>rtip',
            initComplete: function() {
                console.log('DataTable inicializado correctamente');
            }
        });

        // Búsqueda personalizada
        $('#searchUsuarios').on('keyup', function() {
            table.search(this.value).draw();
        });
    }

    // ==================== MODALES DE BLOQUEO/DESBLOQUEO ====================
    
    let usuarioSeleccionadoId = null;
    let usuarioSeleccionadoNombre = '';

    // Abrir modal de bloqueo
    $(document).on('click', '.btn-bloquear', function() {
        usuarioSeleccionadoId = $(this).data('id');
        usuarioSeleccionadoNombre = $(this).data('nombre');
        
        $('#nombreBloquear').text(usuarioSeleccionadoNombre);
        $('#razonBloqueo').val('');
        
        const modal = new bootstrap.Modal(document.getElementById('modalBloquear'));
        modal.show();
    });

    // Confirmar bloqueo
    $('#confirmarBloqueo').on('click', function() {
        const razon = $('#razonBloqueo').val().trim();
        
        if (!razon) {
            alert('Por favor ingrese la razón del bloqueo');
            return;
        }

        bloquearUsuario(usuarioSeleccionadoId, razon);
    });

    // Abrir modal de desbloqueo
    $(document).on('click', '.btn-desbloquear', function() {
        usuarioSeleccionadoId = $(this).data('id');
        usuarioSeleccionadoNombre = $(this).data('nombre');
        
        $('#nombreDesbloquear').text(usuarioSeleccionadoNombre);
        
        const modal = new bootstrap.Modal(document.getElementById('modalDesbloquear'));
        modal.show();
    });

    // Confirmar desbloqueo
    $('#confirmarDesbloqueo').on('click', function() {
        desbloquearUsuario(usuarioSeleccionadoId);
    });

    // ==================== FUNCIONES AJAX ====================

    /**
     * Bloquea un usuario
     */
    function bloquearUsuario(idUsuario, razon) {
        const btn = $('#confirmarBloqueo');
        btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin me-2"></i>Bloqueando...');

        $.ajax({
            url: `/admin/usuarios/api/${idUsuario}/bloquear`,
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ razon: razon }),
            success: function(response) {
                console.log('Usuario bloqueado:', response);
                
                // Cerrar modal
                bootstrap.Modal.getInstance(document.getElementById('modalBloquear')).hide();
                
                // Mostrar mensaje de éxito
                mostrarNotificacion('success', 'Usuario bloqueado exitosamente');
                
                // Recargar página después de 1 segundo
                setTimeout(function() {
                    location.reload();
                }, 1000);
            },
            error: function(xhr, status, error) {
                console.error('Error al bloquear usuario:', error);
                mostrarNotificacion('error', 'Error al bloquear usuario: ' + (xhr.responseJSON?.error || error));
                btn.prop('disabled', false).html('<i class="fas fa-lock me-2"></i>Bloquear');
            }
        });
    }

    /**
     * Desbloquea un usuario
     */
    function desbloquearUsuario(idUsuario) {
        const btn = $('#confirmarDesbloqueo');
        btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin me-2"></i>Desbloqueando...');

        $.ajax({
            url: `/admin/usuarios/api/${idUsuario}/desbloquear`,
            type: 'PUT',
            contentType: 'application/json',
            success: function(response) {
                console.log('Usuario desbloqueado:', response);
                
                // Cerrar modal
                bootstrap.Modal.getInstance(document.getElementById('modalDesbloquear')).hide();
                
                // Mostrar mensaje de éxito
                mostrarNotificacion('success', 'Usuario desbloqueado exitosamente');
                
                // Recargar página después de 1 segundo
                setTimeout(function() {
                    location.reload();
                }, 1000);
            },
            error: function(xhr, status, error) {
                console.error('Error al desbloquear usuario:', error);
                mostrarNotificacion('error', 'Error al desbloquear usuario: ' + (xhr.responseJSON?.error || error));
                btn.prop('disabled', false).html('<i class="fas fa-unlock me-2"></i>Desbloquear');
            }
        });
    }

    /**
     * Cambia el rol de un usuario
     */
    window.cambiarRol = function(idUsuario, nuevoRol) {
        if (!confirm(`¿Está seguro de cambiar el rol a ${nuevoRol}?`)) {
            return;
        }

        $.ajax({
            url: `/admin/usuarios/api/${idUsuario}/rol`,
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ rol: nuevoRol }),
            success: function(response) {
                console.log('Rol cambiado:', response);
                mostrarNotificacion('success', 'Rol actualizado exitosamente');
                setTimeout(function() {
                    location.reload();
                }, 1000);
            },
            error: function(xhr, status, error) {
                console.error('Error al cambiar rol:', error);
                mostrarNotificacion('error', 'Error al cambiar rol: ' + (xhr.responseJSON?.error || error));
            }
        });
    };

    /**
     * Cambia el estado activo/inactivo de un usuario
     */
    window.cambiarEstado = function(idUsuario, activo) {
        const mensaje = activo ? 'activar' : 'desactivar';
        if (!confirm(`¿Está seguro de ${mensaje} este usuario?`)) {
            return;
        }

        $.ajax({
            url: `/admin/usuarios/api/${idUsuario}/estado`,
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ activo: activo }),
            success: function(response) {
                console.log('Estado cambiado:', response);
                mostrarNotificacion('success', 'Estado actualizado exitosamente');
                setTimeout(function() {
                    location.reload();
                }, 1000);
            },
            error: function(xhr, status, error) {
                console.error('Error al cambiar estado:', error);
                mostrarNotificacion('error', 'Error al cambiar estado: ' + (xhr.responseJSON?.error || error));
            }
        });
    };

    /**
     * Elimina (desactiva) un usuario
     */
    window.eliminarUsuario = function(idUsuario, nombre) {
        if (!confirm(`¿Está seguro de eliminar al usuario "${nombre}"?\n\nEl usuario será desactivado pero sus datos se conservarán.`)) {
            return;
        }

        $.ajax({
            url: `/admin/usuarios/api/${idUsuario}`,
            type: 'DELETE',
            success: function(response) {
                console.log('Usuario eliminado:', response);
                mostrarNotificacion('success', 'Usuario eliminado exitosamente');
                setTimeout(function() {
                    location.reload();
                }, 1000);
            },
            error: function(xhr, status, error) {
                console.error('Error al eliminar usuario:', error);
                mostrarNotificacion('error', 'Error al eliminar usuario: ' + (xhr.responseJSON?.error || error));
            }
        });
    };

    // ==================== CARGA DE ESTADÍSTICAS ====================

    /**
     * Carga las estadísticas de usuarios
     */
    function cargarEstadisticas() {
        $.ajax({
            url: '/admin/usuarios/api/estadisticas',
            type: 'GET',
            success: function(data) {
                console.log('Estadísticas cargadas:', data);
                actualizarEstadisticas(data);
            },
            error: function(xhr, status, error) {
                console.error('Error al cargar estadísticas:', error);
            }
        });
    }

    /**
     * Actualiza las tarjetas de estadísticas en la UI
     */
    function actualizarEstadisticas(data) {
        // Actualizar números en las tarjetas
        $('.stat-total').text(data.total || 0);
        $('.stat-activos').text(data.activos || 0);
        $('.stat-bloqueados').text(data.bloqueados || 0);
        $('.stat-admins').text(data.admins || 0);

        // Animar números
        $('.stats-card h3').each(function() {
            $(this).prop('Counter', 0).animate({
                Counter: $(this).text()
            }, {
                duration: 1000,
                easing: 'swing',
                step: function(now) {
                    $(this).text(Math.ceil(now));
                }
            });
        });
    }

    // ==================== BÚSQUEDA AVANZADA ====================

    /**
     * Búsqueda de usuarios por múltiples criterios
     */
    window.buscarUsuarios = function() {
        const searchTerm = $('#searchInput').val();
        const rol = $('#rolFilter').val();
        const activo = $('#activoFilter').val();
        const bloqueado = $('#bloqueadoFilter').val();

        const params = new URLSearchParams();
        if (searchTerm) params.append('search', searchTerm);
        if (rol) params.append('rol', rol);
        if (activo) params.append('activo', activo);
        if (bloqueado) params.append('bloqueado', bloqueado);

        window.location.href = `/admin/usuarios?${params.toString()}`;
    };

    // ==================== EXPORTACIÓN ====================

    /**
     * Exporta la lista de usuarios a Excel
     */
    window.exportarExcel = function() {
        const params = new URLSearchParams(window.location.search);
        window.location.href = `/admin/usuarios/exportar/excel?${params.toString()}`;
    };

    /**
     * Exporta la lista de usuarios a PDF
     */
    window.exportarPDF = function() {
        const params = new URLSearchParams(window.location.search);
        window.location.href = `/admin/usuarios/exportar/pdf?${params.toString()}`;
    };

    // ==================== UTILIDADES ====================

    /**
     * Muestra una notificación toast
     */
    function mostrarNotificacion(tipo, mensaje) {
        const iconos = {
            success: 'fa-check-circle',
            error: 'fa-exclamation-circle',
            warning: 'fa-exclamation-triangle',
            info: 'fa-info-circle'
        };

        const colores = {
            success: 'alert-success',
            error: 'alert-danger',
            warning: 'alert-warning',
            info: 'alert-info'
        };

        const html = `
            <div class="alert ${colores[tipo]} alert-dismissible fade show position-fixed top-0 end-0 m-3" 
                 role="alert" 
                 style="z-index: 9999; min-width: 300px;">
                <i class="fas ${iconos[tipo]} me-2"></i>
                ${mensaje}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;

        $('body').append(html);

        // Auto-cerrar después de 5 segundos
        setTimeout(function() {
            $('.alert').alert('close');
        }, 5000);
    }

    /**
     * Formatea una fecha a formato legible
     */
    function formatearFecha(fecha) {
        if (!fecha) return 'Nunca';
        
        const opciones = { 
            year: 'numeric', 
            month: 'long', 
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        };
        return new Date(fecha).toLocaleDateString('es-ES', opciones);
    }

    /**
     * Obtiene un badge de estado según el rol
     */
    function getBadgeRol(rol) {
        const badges = {
            'ADMIN': 'bg-danger',
            'GERENTE': 'bg-warning',
            'VENDEDOR': 'bg-info'
        };
        return badges[rol] || 'bg-secondary';
    }

    /**
     * Copia texto al portapapeles
     */
    window.copiarAlPortapapeles = function(texto) {
        navigator.clipboard.writeText(texto).then(function() {
            mostrarNotificacion('success', 'Copiado al portapapeles');
        }, function(err) {
            console.error('Error al copiar:', err);
            mostrarNotificacion('error', 'Error al copiar');
        });
    };


    // ==================== RESET PASSWORD Y REENVIAR CREDENCIALES ====================

    let usuarioResetId = null;

    // Abrir modal de reset password
    $(document).on('click', '.btn-reset-password', function() {
        usuarioResetId = $(this).data('id');
        const nombreUsuario = $(this).data('nombre');
        
        $('#resetPasswordNombre').text(nombreUsuario);
        $('#nuevaPasswordContainer').addClass('d-none');
        $('#nuevaPassword').val('');
        $('#btnConfirmarReset').prop('disabled', false).show();
        
        const modal = new bootstrap.Modal(document.getElementById('modalResetPassword'));
        modal.show();
    });

    // Confirmar reset password
    $('#btnConfirmarReset').on('click', function() {
        const btn = $(this);
        btn.prop('disabled', true).html('<i class="bi bi-hourglass-split me-2"></i>Generando...');

        $.ajax({
            url: `/admin/usuarios/api/${usuarioResetId}/reset-password`,
            method: 'POST',
            success: function(response) {
                if (response.success) {
                    $('#nuevaPassword').val(response.password);
                    $('#nuevaPasswordContainer').removeClass('d-none');
                    btn.hide();
                    
                    Swal.fire({
                        icon: 'success',
                        title: '¡Contraseña Generada!',
                        text: 'Se ha generado una nueva contraseña. Asegúrate de copiarla y comunicarla al usuario.',
                        confirmButtonText: 'Entendido'
                    });
                } else {
                    Swal.fire({
                        icon: 'error',
                        title: 'Error',
                        text: response.message || 'Error al resetear contraseña',
                        confirmButtonText: 'Cerrar'
                    });
                    btn.prop('disabled', false).html('<i class="bi bi-key-fill me-2"></i>Generar Nueva Contraseña');
                }
            },
            error: function(xhr) {
                console.error('Error:', xhr);
                Swal.fire({
                    icon: 'error',
                    title: 'Error',
                    text: xhr.responseJSON?.error || 'Error al resetear contraseña',
                    confirmButtonText: 'Cerrar'
                });
                btn.prop('disabled', false).html('<i class="bi bi-key-fill me-2"></i>Generar Nueva Contraseña');
            }
        });
    });

    // Copiar contraseña al portapapeles
    $('#btnCopiarPassword').on('click', function() {
        const password = $('#nuevaPassword').val();
        navigator.clipboard.writeText(password).then(function() {
            Swal.fire({
                icon: 'success',
                title: '¡Copiado!',
                text: 'Contraseña copiada al portapapeles',
                timer: 1500,
                showConfirmButton: false
            });
        }, function(err) {
            console.error('Error al copiar:', err);
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: 'No se pudo copiar al portapapeles',
                confirmButtonText: 'Cerrar'
            });
        });
    });

    // Reenviar credenciales por email
    $(document).on('click', '.btn-reenviar-credenciales', function() {
        const idUsuario = $(this).data('id');
        const nombreUsuario = $(this).data('nombre');
        const email = $(this).data('email');

        if (!email) {
            Swal.fire({
                icon: 'warning',
                title: 'Email no configurado',
                text: 'Este usuario no tiene un email configurado',
                confirmButtonText: 'Cerrar'
            });
            return;
        }

        Swal.fire({
            title: '¿Reenviar credenciales?',
            html: `Se enviará un email a <strong>${email}</strong> con las credenciales de <strong>${nombreUsuario}</strong>.<br><br><small class="text-warning">⚠️ Se generará una nueva contraseña temporal</small>`,
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#28a745',
            cancelButtonColor: '#6c757d',
            confirmButtonText: '<i class="bi bi-envelope-fill me-2"></i>Enviar Email',
            cancelButtonText: 'Cancelar',
            showLoaderOnConfirm: true,
            preConfirm: () => {
                return $.ajax({
                    url: `/admin/usuarios/api/${idUsuario}/reenviar-credenciales`,
                    method: 'POST'
                }).then(response => {
                    if (!response.success) {
                        throw new Error(response.message || 'Error al enviar credenciales');
                    }
                    return response;
                }).catch(error => {
                    Swal.showValidationMessage(
                        error.responseJSON?.error || error.message || 'Error al enviar email'
                    );
                });
            },
            allowOutsideClick: () => !Swal.isLoading()
        }).then((result) => {
            if (result.isConfirmed) {
                Swal.fire({
                    icon: 'success',
                    title: '¡Enviado!',
                    html: `Credenciales enviadas exitosamente a:<br><strong>${email}</strong>`,
                    confirmButtonText: 'Cerrar'
                });
            }
        });
    });

    // Activar/Desactivar usuario
    $(document).on('click', '.btn-toggle-active', function() {
        const idUsuario = $(this).data('id');
        const nombreUsuario = $(this).data('nombre');
        const activo = $(this).data('activo');
        const accion = activo ? 'desactivar' : 'activar';
        const colorBtn = activo ? '#dc3545' : '#28a745';

        Swal.fire({
            title: `¿${accion.charAt(0).toUpperCase() + accion.slice(1)} usuario?`,
            text: `Estás a punto de ${accion} a ${nombreUsuario}`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: colorBtn,
            cancelButtonColor: '#6c757d',
            confirmButtonText: `Sí, ${accion}`,
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                $.ajax({
                    url: `/admin/usuarios/api/${idUsuario}/estado`,
                    method: 'PUT',
                    contentType: 'application/json',
                    data: JSON.stringify({ activo: !activo }),
                    success: function(response) {
                        Swal.fire({
                            icon: 'success',
                            title: '¡Éxito!',
                            text: `Usuario ${activo ? 'desactivado' : 'activado'} correctamente`,
                            timer: 2000,
                            showConfirmButton: false
                        }).then(() => {
                            location.reload();
                        });
                    },
                    error: function(xhr) {
                        Swal.fire({
                            icon: 'error',
                            title: 'Error',
                            text: xhr.responseJSON?.error || 'Error al cambiar estado del usuario',
                            confirmButtonText: 'Cerrar'
                        });
                    }
                });
            }
        });
    });

    // ==================== EVENTOS GLOBALES ====================

    // Limpiar formularios al cerrar modales
    $('.modal').on('hidden.bs.modal', function () {
        $(this).find('form')[0]?.reset();
        $(this).find('.was-validated').removeClass('was-validated');
    });

    // Tooltips de Bootstrap
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Confirmar acciones destructivas
    $('[data-confirm]').on('click', function(e) {
        if (!confirm($(this).data('confirm'))) {
            e.preventDefault();
            return false;
        }
    });

    // Log de inicialización
    console.log('usuarios-admin.js cargado correctamente');
    console.log('DataTables inicializado:', $('#usuariosTable').length > 0);
});
