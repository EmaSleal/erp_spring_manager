package api.astro.whats_orders_manager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * ============================================================================
 * WEBSOCKET CONFIGURATION
 * WhatsApp Orders Manager - Sprint 4 Fase 3.5
 * ============================================================================
 * Configuración de WebSocket con STOMP para notificaciones en tiempo real.
 * 
 * Características:
 * - STOMP sobre WebSocket para mensajería
 * - SockJS fallback para navegadores sin soporte WebSocket
 * - Broker simple en memoria para mensajes
 * - Prefijo /app para destinos de aplicación
 * - Prefijo /topic para suscripciones públicas
 * - Prefijo /queue para suscripciones privadas (usuario específico)
 * 
 * Endpoints:
 * - /ws-notificaciones: Conexión inicial WebSocket/SockJS
 * - /app/...: Envío de mensajes desde cliente
 * - /topic/...: Suscripción pública (broadcast)
 * - /queue/...: Suscripción privada (usuario específico)
 * 
 * @author EmaSleal
 * @since Sprint 4 - Fase 3.5
 * ============================================================================
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configura el message broker para enrutamiento de mensajes
     * 
     * - /topic: Mensajes broadcast (todos los conectados)
     * - /queue: Mensajes privados (usuario específico)
     * - /app: Prefijo para mensajes desde cliente a servidor
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilitar broker simple en memoria
        // /topic para broadcasts, /queue para mensajes privados
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefijo para mensajes desde cliente
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefijo para mensajes a usuario específico
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Registra endpoints STOMP para conexión de clientes
     * 
     * Endpoint: /ws-notificaciones
     * - Con SockJS fallback para navegadores sin WebSocket
     * - CORS permitido desde cualquier origen (ajustar en producción)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notificaciones")
                .setAllowedOriginPatterns("*")  // En producción: especificar dominios
                .withSockJS();  // Fallback para navegadores sin WebSocket
    }
}
