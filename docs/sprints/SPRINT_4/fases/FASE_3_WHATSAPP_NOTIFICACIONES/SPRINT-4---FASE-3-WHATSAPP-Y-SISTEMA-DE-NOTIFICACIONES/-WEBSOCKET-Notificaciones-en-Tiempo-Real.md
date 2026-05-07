## 🌐 WEBSOCKET (Notificaciones en Tiempo Real)

### Configuración

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}
```

### Controller WebSocket

```java
@Controller
@Slf4j
public class NotificacionWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public void enviarNotificacion(Long usuarioId, Notificacion notificacion) {
        try {
            // Enviar a cola personal del usuario
            String destino = "/queue/notificaciones/" + usuarioId;
            
            NotificacionDTO dto = convertirADTO(notificacion);
            
            messagingTemplate.convertAndSend(destino, dto);
            
            log.info("Notificación enviada por WebSocket a usuario {}", usuarioId);
            
        } catch (Exception e) {
            log.error("Error al enviar notificación por WebSocket", e);
        }
    }
    
    public void enviarAGrupo(String grupo, Object mensaje) {
        messagingTemplate.convertAndSend("/topic/" + grupo, mensaje);
    }
    
    private NotificacionDTO convertirADTO(Notificacion notificacion) {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(notificacion.getId());
        dto.setTitulo(notificacion.getTitulo());
        dto.setMensaje(notificacion.getMensaje());
        dto.setTipo(notificacion.getTipo().name());
        dto.setFecha(notificacion.getCreadoEn());
        dto.setLeida(notificacion.getLeida());
        return dto;
    }
}
```

### Cliente JavaScript

```html
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>

<script>
    let stompClient = null;
    const usuarioId = [[${#authentication.principal.id}]];

    function conectarWebSocket() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log('Conectado a WebSocket');

            // Suscribirse a notificaciones personales
            stompClient.subscribe('/queue/notificaciones/' + usuarioId, function(mensaje) {
                const notificacion = JSON.parse(mensaje.body);
                mostrarNotificacion(notificacion);
                actualizarContadorNotificaciones();
            });
        });
    }

    function mostrarNotificacion(notificacion) {
        // Mostrar toast/banner
        const toast = `
            <div class="toast" role="alert">
                <div class="toast-header">
                    <strong>${notificacion.titulo}</strong>
                </div>
                <div class="toast-body">
                    ${notificacion.mensaje}
                </div>
            </div>
        `;
        
        document.getElementById('toast-container').innerHTML += toast;
        
        // Actualizar badge en navbar
        document.getElementById('notif-badge').textContent = 
            parseInt(document.getElementById('notif-badge').textContent) + 1;
    }

    // Conectar al cargar la página
    document.addEventListener('DOMContentLoaded', conectarWebSocket);
</script>
```

---

