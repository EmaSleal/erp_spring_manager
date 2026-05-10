/**
 * ============================================================================
 * WEBSOCKET NOTIFICACIONES - CLIENTE JAVASCRIPT
 * ERP Orders Manager - Sprint 4 Fase 3.5
 * ============================================================================
 * Cliente WebSocket con SockJS y STOMP para notificaciones en tiempo real.
 * 
 * Características:
 * - Conexión automática al cargar la página
 * - Reconexión automática en caso de desconexión
 * - Notificaciones en tiempo real con sonido y animación
 * - Actualización de contador de no leídas en navbar
 * - Dropdown de notificaciones recientes
 * - Toast notifications con Bootstrap
 * 
 * Dependencias:
 * - SockJS Client: https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js
 * - STOMP.js: https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/dist/stomp.umd.min.js
 * - Bootstrap 5 (para toasts)
 * 
 * @author EmaSleal
 * @since Sprint 4 - Fase 3.5
 * ============================================================================
 */

let stompClient = null;
let isConnected = false;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 3000; // 3 segundos

/**
 * Conecta al servidor WebSocket
 */
function conectarWebSocket() {
    console.log('🔌 Iniciando conexión WebSocket...');
    
    // Crear socket con SockJS
    const socket = new SockJS('/ws-notificaciones');
    stompClient = Stomp.over(socket);
    
    // Configurar logging (desactivar en producción)
    stompClient.debug = (str) => {
        // console.log(str); // Comentar en producción
    };
    
    // Conectar
    stompClient.connect({}, onConectado, onError);
}

/**
 * Callback cuando la conexión es exitosa
 */
function onConectado(frame) {
    console.log('✅ Conectado al WebSocket:', frame);
    isConnected = true;
    reconnectAttempts = 0;
    
    // Suscribirse a notificaciones personales (usuario específico)
    stompClient.subscribe('/user/queue/notificaciones', onNotificacionRecibida);
    
    // Suscribirse a contador de no leídas
    stompClient.subscribe('/user/queue/notificaciones/contador', onContadorActualizado);
    
    // Suscribirse a notificaciones broadcast (opcional)
    stompClient.subscribe('/topic/notificaciones', onNotificacionBroadcast);
    
    // Solicitar notificaciones no leídas al conectar
    solicitarNotificacionesNoLeidas();
    
    console.log('✅ Suscripciones establecidas');
}

/**
 * Callback cuando hay un error de conexión
 */
function onError(error) {
    console.error('❌ Error de conexión WebSocket:', error);
    isConnected = false;
    
    // Intentar reconectar
    if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        reconnectAttempts++;
        console.log(`🔄 Reintentando conexión (${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})...`);
        setTimeout(conectarWebSocket, RECONNECT_DELAY * reconnectAttempts);
    } else {
        console.error('❌ Máximo de intentos de reconexión alcanzado');
        mostrarNotificacionError('No se pudo conectar al servidor de notificaciones');
    }
}

/**
 * Callback cuando se recibe una notificación personal
 */
function onNotificacionRecibida(message) {
    const notificacion = JSON.parse(message.body);
    console.log('📬 Notificación recibida:', notificacion);
    
    // Mostrar notificación en UI
    mostrarNotificacion(notificacion);
    
    // Reproducir sonido
    reproducirSonidoNotificacion();
    
    // Actualizar badge
    actualizarBadgeNotificaciones();
}

/**
 * Callback cuando se actualiza el contador de no leídas
 */
function onContadorActualizado(message) {
    const contador = parseInt(message.body);
    console.log('🔢 Contador actualizado:', contador);
    
    actualizarBadge(contador);
}

/**
 * Callback cuando se recibe una notificación broadcast
 */
function onNotificacionBroadcast(message) {
    const notificacion = JSON.parse(message.body);
    console.log('📡 Notificación broadcast:', notificacion);
    
    // Mostrar como notificación menos prioritaria
    mostrarNotificacion(notificacion, 'info');
}

/**
 * Solicita notificaciones no leídas al servidor
 */
function solicitarNotificacionesNoLeidas() {
    if (stompClient && isConnected) {
        stompClient.send('/app/notificaciones/no-leidas', {}, '{}');
    }
}

/**
 * Marca una notificación como leída
 */
function marcarComoLeida(idNotificacion) {
    if (stompClient && isConnected) {
        stompClient.send('/app/notificaciones/marcar-leida', {}, 
            JSON.stringify(idNotificacion));
        console.log('✅ Notificación marcada como leída:', idNotificacion);
    } else {
        console.warn('⚠️ WebSocket no conectado, marcando por API REST');
        marcarComoLeidaViaREST(idNotificacion);
    }
}

/**
 * Fallback: Marcar como leída vía API REST
 */
function marcarComoLeidaViaREST(idNotificacion) {
    fetch(`/api/notificaciones/${idNotificacion}/marcar-leida`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (response.ok) {
            console.log('✅ Notificación marcada como leída (REST)');
            actualizarBadgeNotificaciones();
        }
    })
    .catch(error => console.error('Error al marcar como leída:', error));
}

/**
 * Muestra una notificación en la UI usando Bootstrap Toast
 */
function mostrarNotificacion(notificacion, tipo = null) {
    // Determinar tipo de notificación según prioridad
    const tipoNotif = tipo || notificacion.tipoColor || 'primary';
    const iconoNotif = notificacion.tipoIcono || 'fa-bell';
    
    // Crear toast
    const toastHTML = `
        <div class="toast align-items-center text-bg-${tipoNotif} border-0" 
             role="alert" aria-live="assertive" aria-atomic="true"
             data-bs-autohide="true" data-bs-delay="5000">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas ${iconoNotif} me-2"></i>
                    <strong>${notificacion.titulo || 'Notificación'}</strong>
                    <p class="mb-0 mt-1 small">${notificacion.mensaje || ''}</p>
                    ${notificacion.urlAccion ? 
                        `<a href="${notificacion.urlAccion}" class="btn btn-sm btn-light mt-2">
                            ${notificacion.textoBoton || 'Ver más'}
                        </a>` : ''}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" 
                        data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;
    
    // Agregar al contenedor de toasts
    let toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toast-container';
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        toastContainer.style.zIndex = '9999';
        document.body.appendChild(toastContainer);
    }
    
    const toastElement = document.createElement('div');
    toastElement.innerHTML = toastHTML;
    toastContainer.appendChild(toastElement.firstElementChild);
    
    // Mostrar toast
    const toast = new bootstrap.Toast(toastElement.firstElementChild);
    toast.show();
    
    // Eliminar después de ocultarse
    toastElement.firstElementChild.addEventListener('hidden.bs.toast', function() {
        this.remove();
    });
}

/**
 * Muestra una notificación de error
 */
function mostrarNotificacionError(mensaje) {
    mostrarNotificacion({
        titulo: 'Error',
        mensaje: mensaje,
        tipoColor: 'danger',
        tipoIcono: 'fa-exclamation-triangle'
    }, 'danger');
}

/**
 * Actualiza el badge de notificaciones en el navbar
 */
function actualizarBadgeNotificaciones() {
    // Obtener contador desde servidor vía REST
    fetch('/api/notificaciones/no-leidas/count')
        .then(response => response.json())
        .then(count => actualizarBadge(count))
        .catch(error => console.error('Error al obtener contador:', error));
}

/**
 * Actualiza el badge con el contador especificado
 */
function actualizarBadge(contador) {
    const badge = document.getElementById('notificaciones-badge');
    if (badge) {
        if (contador > 0) {
            badge.textContent = contador > 99 ? '99+' : contador;
            badge.style.display = 'inline-block';
            
            // Animación de "pop"
            badge.classList.add('animate__animated', 'animate__bounceIn');
            setTimeout(() => {
                badge.classList.remove('animate__animated', 'animate__bounceIn');
            }, 1000);
        } else {
            badge.style.display = 'none';
        }
    }
}

/**
 * Reproduce sonido de notificación
 */
function reproducirSonidoNotificacion() {
    // Verificar preferencias del usuario (localStorage)
    const sonidoHabilitado = localStorage.getItem('notificaciones-sonido') !== 'false';
    
    if (sonidoHabilitado) {
        // Usar Audio API del navegador
        const audio = new Audio('/sounds/notification.mp3');
        audio.volume = 0.3; // 30% volumen
        audio.play().catch(error => {
            console.log('No se pudo reproducir sonido:', error);
        });
    }
}

/**
 * Desconecta del WebSocket
 */
function desconectarWebSocket() {
    if (stompClient !== null) {
        stompClient.disconnect(() => {
            console.log('👋 Desconectado del WebSocket');
            isConnected = false;
        });
    }
}

/**
 * Inicializar al cargar la página
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Inicializando cliente WebSocket de notificaciones...');
    
    // Conectar WebSocket
    conectarWebSocket();
    
    // Actualizar badge inicial
    actualizarBadgeNotificaciones();
    
    // Desconectar al cerrar/recargar página
    window.addEventListener('beforeunload', desconectarWebSocket);
    
    console.log('✅ Cliente WebSocket inicializado');
});

/**
 * Exponer funciones globales para uso desde otros scripts
 */
window.NotificacionesWebSocket = {
    conectar: conectarWebSocket,
    desconectar: desconectarWebSocket,
    marcarComoLeida: marcarComoLeida,
    actualizarBadge: actualizarBadgeNotificaciones,
    isConnected: () => isConnected
};
