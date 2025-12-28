/**
 * ============================================================================
 * CONFIGURACION-EMAIL.JS - Gestión de configuración SMTP y pruebas de email
 * WhatsApp Orders Manager - Sprint 4
 * ============================================================================
 */

const ConfiguracionEmail = {
    API_URL: '/api/configuracion/email',
    idConfiguracion: null,  // Para almacenar el ID de la configuración actual
    
    /**
     * Inicializa el módulo
     */
    init: function() {
        this.cargarConfiguracion();
        this.configurarEventos();
        console.log('✅ Módulo Email inicializado');
    },
    
    /**
     * Configura event listeners
     */
    configurarEventos: function() {
        // Form submit
        const form = document.getElementById('form-email');
        if (form) {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.guardarConfiguracion();
            });
        }
        
        // Botón cancelar
        const btnCancelar = document.getElementById('btn-cancelar-email');
        if (btnCancelar) {
            btnCancelar.addEventListener('click', () => {
                this.cargarConfiguracion();
            });
        }
        
        // Botón prueba de email
        const btnPrueba = document.getElementById('btn-prueba-email');
        if (btnPrueba) {
            btnPrueba.addEventListener('click', () => {
                this.mostrarModalPrueba();
            });
        }
        
        // Botón validar configuración
        const btnValidar = document.getElementById('btn-validar-configuracion');
        if (btnValidar) {
            btnValidar.addEventListener('click', () => {
                this.validarConfiguracion();
            });
        }
        
        // Botón enviar email de prueba (modal)
        const btnEnviarPrueba = document.getElementById('btn-enviar-prueba');
        if (btnEnviarPrueba) {
            btnEnviarPrueba.addEventListener('click', () => {
                this.enviarEmailPrueba();
            });
        }
        
        // Toggle password
        const togglePassword = document.getElementById('toggle-password');
        const passwordField = document.getElementById('password');
        if (togglePassword && passwordField) {
            togglePassword.addEventListener('click', () => {
                const type = passwordField.type === 'password' ? 'text' : 'password';
                passwordField.type = type;
                togglePassword.querySelector('i').classList.toggle('fa-eye');
                togglePassword.querySelector('i').classList.toggle('fa-eye-slash');
            });
        }
    },
    
    /**
     * Carga la configuración actual
     */
    cargarConfiguracion: async function() {
        try {
            const response = await Configuracion.get(this.API_URL);
            
            if (response.success && response.data) {
                const datos = response.data;
                
                // Guardar ID para actualizaciones
                this.idConfiguracion = datos.idConfiguracion || null;
                
                // Campos SMTP (usando IDs del HTML)
                document.getElementById('smtp-host').value = datos.smtpHost || '';
                document.getElementById('smtp-port').value = datos.smtpPort || 587;
                document.getElementById('smtp-usuario').value = datos.smtpUsuario || '';
                document.getElementById('smtp-password').value = datos.smtpPassword || '';
                document.getElementById('email-remitente').value = datos.emailRemitente || '';
                document.getElementById('nombre-remitente').value = datos.nombreRemitente || '';
                
                // Seguridad
                document.getElementById('smtp-tls').checked = datos.smtpTls !== false;
                document.getElementById('smtp-ssl').checked = datos.smtpSsl || false;
                document.getElementById('smtp-auth').checked = datos.smtpAuth !== false;
                
                // Timeouts
                const timeoutField = document.getElementById('timeout');
                if (timeoutField) {
                    timeoutField.value = datos.timeout || 5000;
                }
                
                // Codificación
                const charsetField = document.getElementById('charset');
                if (charsetField) {
                    charsetField.value = datos.charset || 'UTF-8';
                }
                
                // Estado
                document.getElementById('activoEmail').checked = datos.activo !== false;
                
                // Info última prueba
                if (datos.ultimaPruebaExitosa) {
                    document.getElementById('ultima-prueba-exitosa').textContent = 
                        Configuracion.formatearFechaHora(new Date(datos.ultimaPruebaExitosa));
                }
                if (datos.ultimaPruebaError) {
                    document.getElementById('ultima-prueba-error').textContent = datos.ultimaPruebaError;
                }
                
                console.log('✅ Configuración de email cargada');
            } else {
                console.log('ℹ️ No hay configuración de email');
            }
        } catch (error) {
            console.error('❌ Error cargando configuración:', error);
            Configuracion.mostrarAlertaEn('alert-email-container', 'warning', 
                'No se pudo cargar la configuración. Puedes crear una nueva.');
        }
    },
    
    /**
     * Guarda la configuración
     */
    guardarConfiguracion: async function() {
        // Validar formulario
        if (!Configuracion.validarFormulario('form-email')) {
            Configuracion.mostrarAlertaEn('alert-email-container', 'warning', 
                'Por favor completa todos los campos requeridos');
            return;
        }
        
        try {
            // Obtener datos del formulario (usando IDs del HTML)
            const datos = {
                smtpHost: document.getElementById('smtp-host').value,
                smtpPort: parseInt(document.getElementById('smtp-port').value),
                smtpUsuario: document.getElementById('smtp-usuario').value,
                smtpPassword: document.getElementById('smtp-password').value,
                emailRemitente: document.getElementById('email-remitente').value,
                nombreRemitente: document.getElementById('nombre-remitente').value || null,
                smtpTls: document.getElementById('smtp-tls').checked,
                smtpSsl: document.getElementById('smtp-ssl').checked,
                smtpAuth: document.getElementById('smtp-auth').checked,
                timeout: parseInt(document.getElementById('timeout')?.value) || 5000,
                charset: document.getElementById('charset')?.value || 'UTF-8',
                activo: document.getElementById('activoEmail').checked
            };
            
            // Agregar ID si existe (para actualizaciones)
            if (this.idConfiguracion) {
                datos.idConfiguracion = this.idConfiguracion;
            }
            
            // Decidir método HTTP según si hay ID
            let response;
            if (this.idConfiguracion) {
                // Actualizar con PUT
                response = await Configuracion.put(this.API_URL, datos);
            } else {
                // Crear con POST
                response = await Configuracion.post(this.API_URL, datos);
            }
            
            if (response.success) {
                Configuracion.mostrarAlertaEn('alert-email-container', 'success', 
                    response.message || 'Configuración guardada exitosamente');
                
                // Recargar datos
                setTimeout(() => this.cargarConfiguracion(), 1000);
            } else {
                Configuracion.mostrarAlertaEn('alert-email-container', 'danger', 
                    response.message || 'Error al guardar la configuración');
            }
        } catch (error) {
            console.error('❌ Error guardando configuración:', error);
            Configuracion.mostrarAlertaEn('alert-email-container', 'danger', 
                'Error al guardar. Por favor intenta nuevamente.');
        }
    },
    
    /**
     * Valida la configuración SMTP
     */
    validarConfiguracion: async function() {
        try {
            Configuracion.mostrarAlertaEn('alert-email-container', 'info', 
                '<i class="fas fa-spinner fa-spin"></i> Validando configuración...');
            
            const response = await Configuracion.get(`${this.API_URL}/validar`);
            
            if (response.success) {
                Configuracion.mostrarAlertaEn('alert-email-container', 'success', 
                    '<i class="fas fa-check-circle"></i> ' + (response.message || 'Configuración válida'));
            } else {
                Configuracion.mostrarAlertaEn('alert-email-container', 'danger', 
                    '<i class="fas fa-times-circle"></i> ' + (response.message || 'Configuración inválida'));
            }
        } catch (error) {
            console.error('❌ Error validando configuración:', error);
            Configuracion.mostrarAlertaEn('alert-email-container', 'danger', 
                'Error al validar la configuración');
        }
    },
    
    /**
     * Muestra el modal de prueba de email
     */
    mostrarModalPrueba: function() {
        const modal = new bootstrap.Modal(document.getElementById('modal-prueba-email'));
        modal.show();
    },
    
    /**
     * Envía un email de prueba
     */
    enviarEmailPrueba: async function() {
        const emailDestino = document.getElementById('email-prueba-destino').value;
        
        if (!emailDestino) {
            Configuracion.mostrarAlertaEn('alert-prueba-email', 'warning', 
                'Por favor ingresa un email de destino');
            return;
        }
        
        // Validar formato de email
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(emailDestino)) {
            Configuracion.mostrarAlertaEn('alert-prueba-email', 'warning', 
                'Por favor ingresa un email válido');
            return;
        }
        
        try {
            // Deshabilitar botón y mostrar loading
            const btn = document.getElementById('btn-enviar-prueba');
            const originalHtml = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';
            
            Configuracion.mostrarAlertaEn('alert-prueba-email', 'info', 
                '<i class="fas fa-paper-plane"></i> Enviando email de prueba...');
            
            const response = await Configuracion.post(`${this.API_URL}/probar`, {
                emailDestino: emailDestino
            });
            
            // Restaurar botón
            btn.disabled = false;
            btn.innerHTML = originalHtml;
            
            if (response.success) {
                Configuracion.mostrarAlertaEn('alert-prueba-email', 'success', 
                    '<i class="fas fa-check-circle"></i> ' + (response.message || 'Email enviado exitosamente'));
                
                // Actualizar info en el formulario principal
                document.getElementById('ultima-prueba-exitosa').textContent = 
                    Configuracion.formatearFechaHora(new Date());
                document.getElementById('ultima-prueba-error').textContent = '-';
                
                // Cerrar modal después de 2 segundos
                setTimeout(() => {
                    bootstrap.Modal.getInstance(document.getElementById('modal-prueba-email')).hide();
                    document.getElementById('email-prueba-destino').value = '';
                }, 2000);
            } else {
                Configuracion.mostrarAlertaEn('alert-prueba-email', 'danger', 
                    '<i class="fas fa-times-circle"></i> ' + (response.message || 'Error al enviar email'));
                
                // Actualizar error en el formulario principal
                document.getElementById('ultima-prueba-error').textContent = response.message || 'Error desconocido';
            }
        } catch (error) {
            console.error('❌ Error enviando email de prueba:', error);
            
            // Restaurar botón
            const btn = document.getElementById('btn-enviar-prueba');
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-paper-plane me-2"></i>Enviar Prueba';
            
            Configuracion.mostrarAlertaEn('alert-prueba-email', 'danger', 
                'Error al enviar email de prueba. Verifica la configuración.');
        }
    }
};

// Inicializar cuando el tab de email esté activo
document.addEventListener('shown.bs.tab', function (event) {
    console.log('🔍 Tab changed to:', event.target.id);
    if (event.target.id === 'tab-email-smtp') {
        console.log('📧 Inicializando módulo Email...');
        ConfiguracionEmail.init();
    }
});

// Inicializar si ya está en el tab
document.addEventListener('DOMContentLoaded', function() {
    console.log('📄 DOM cargado - Verificando tab Email...');
    const emailTab = document.getElementById('tab-email-smtp');
    if (emailTab) {
        console.log('✅ Tab Email encontrado');
        if (emailTab.classList.contains('active')) {
            console.log('📧 Tab Email ya está activo, inicializando...');
            ConfiguracionEmail.init();
        }
    } else {
        console.log('❌ Tab Email NO encontrado');
    }
});
