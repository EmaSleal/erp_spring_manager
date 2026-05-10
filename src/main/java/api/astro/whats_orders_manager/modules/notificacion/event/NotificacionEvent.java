package api.astro.whats_orders_manager.modules.notificacion.event;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * NOTIFICACIÓN EVENT
 * ERP Orders Manager - Sprint 4 Fase 3.6
 * ============================================================================
 * Evento base para el sistema de notificaciones.
 * 
 * Se publica cuando ocurre una acción que requiere notificar a usuarios.
 * Los listeners capturan este evento y envían notificaciones según configuración.
 * 
 * Tipos de eventos:
 * - FACTURA_CREADA: Nueva factura generada
 * - FACTURA_VENCIDA: Factura pasó fecha de vencimiento
 * - FACTURA_PROXIMA_VENCER: Factura próxima a vencer (3-7 días)
 * - PAGO_RECIBIDO: Pago registrado en el sistema
 * - STOCK_BAJO: Producto con inventario crítico
 * - NUEVO_CLIENTE: Cliente registrado
 * - NUEVO_USUARIO: Usuario creado
 * - MENSAJE_WHATSAPP: Mensaje recibido vía WhatsApp
 * 
 * Flujo:
 * 1. Service ejecuta acción (ej: crear factura)
 * 2. Service publica evento: applicationEventPublisher.publishEvent(new NotificacionEvent(...))
 * 3. NotificacionListener captura evento con @EventListener
 * 4. Listener llama a NotificacionService para enviar notificación
 * 5. NotificacionService valida preferencias y envía por canal adecuado
 * 
 * @author EmaSleal
 * @since Sprint 4 - Fase 3.6
 * ============================================================================
 */
@Getter
public class NotificacionEvent extends ApplicationEvent {

    /**
     * Tipo de notificación a enviar
     */
    private final TipoNotificacion tipo;

    /**
     * Usuario destinatario (puede ser null para broadcast)
     */
    private final Usuario usuario;

    /**
     * ID del usuario destinatario (alternativa a usuario)
     */
    private final Integer idUsuario;

    /**
     * Canal de envío preferido (null = todos los canales activos)
     */
    private final CanalNotificacion canal;

    /**
     * Título de la notificación
     */
    private final String titulo;

    /**
     * Mensaje/contenido de la notificación
     */
    private final String mensaje;

    /**
     * URL de acción (opcional)
     */
    private final String urlAccion;

    /**
     * Texto del botón de acción (opcional)
     */
    private final String textoBoton;

    /**
     * Datos adicionales relacionados con la notificación
     * Ej: idFactura, idCliente, idProducto, etc.
     */
    private final Map<String, Object> datosRelacionados;

    /**
     * ID de plantilla a usar (opcional)
     */
    private final Integer idPlantilla;

    /**
     * Variables para procesar plantilla (opcional)
     */
    private final Map<String, Object> variablesPlantilla;

    /**
     * Constructor completo
     */
    private NotificacionEvent(
            Object source,
            TipoNotificacion tipo,
            Usuario usuario,
            Integer idUsuario,
            CanalNotificacion canal,
            String titulo,
            String mensaje,
            String urlAccion,
            String textoBoton,
            Map<String, Object> datosRelacionados,
            Integer idPlantilla,
            Map<String, Object> variablesPlantilla) {
        
        super(source);
        this.tipo = tipo;
        this.usuario = usuario;
        this.idUsuario = idUsuario;
        this.canal = canal;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.urlAccion = urlAccion;
        this.textoBoton = textoBoton;
        this.datosRelacionados = datosRelacionados != null ? datosRelacionados : new HashMap<>();
        this.idPlantilla = idPlantilla;
        this.variablesPlantilla = variablesPlantilla;
    }

    /**
     * Builder para construcción fluida
     */
    public static Builder builder(Object source) {
        return new Builder(source);
    }

    /**
     * Builder Pattern
     */
    public static class Builder {
        private final Object source;
        private TipoNotificacion tipo;
        private Usuario usuario;
        private Integer idUsuario;
        private CanalNotificacion canal;
        private String titulo;
        private String mensaje;
        private String urlAccion;
        private String textoBoton;
        private Map<String, Object> datosRelacionados;
        private Integer idPlantilla;
        private Map<String, Object> variablesPlantilla;

        private Builder(Object source) {
            this.source = source;
        }

        public Builder tipo(TipoNotificacion tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder usuario(Usuario usuario) {
            this.usuario = usuario;
            return this;
        }

        public Builder idUsuario(Integer idUsuario) {
            this.idUsuario = idUsuario;
            return this;
        }

        public Builder canal(CanalNotificacion canal) {
            this.canal = canal;
            return this;
        }

        public Builder titulo(String titulo) {
            this.titulo = titulo;
            return this;
        }

        public Builder mensaje(String mensaje) {
            this.mensaje = mensaje;
            return this;
        }

        public Builder urlAccion(String urlAccion) {
            this.urlAccion = urlAccion;
            return this;
        }

        public Builder textoBoton(String textoBoton) {
            this.textoBoton = textoBoton;
            return this;
        }

        public Builder datosRelacionados(Map<String, Object> datos) {
            this.datosRelacionados = datos;
            return this;
        }

        public Builder agregarDato(String clave, Object valor) {
            if (this.datosRelacionados == null) {
                this.datosRelacionados = new HashMap<>();
            }
            this.datosRelacionados.put(clave, valor);
            return this;
        }

        public Builder idPlantilla(Integer idPlantilla) {
            this.idPlantilla = idPlantilla;
            return this;
        }

        public Builder variablesPlantilla(Map<String, Object> variables) {
            this.variablesPlantilla = variables;
            return this;
        }

        public NotificacionEvent build() {
            return new NotificacionEvent(
                source,
                tipo,
                usuario,
                idUsuario,
                canal,
                titulo,
                mensaje,
                urlAccion,
                textoBoton,
                datosRelacionados,
                idPlantilla,
                variablesPlantilla
            );
        }
    }

    /**
     * Obtiene un dato relacionado del evento
     */
    @SuppressWarnings("unchecked")
    public <T> T getDatoRelacionado(String clave, Class<T> tipo) {
        Object valor = datosRelacionados.get(clave);
        if (valor != null && tipo.isInstance(valor)) {
            return (T) valor;
        }
        return null;
    }

    /**
     * Verifica si tiene un dato relacionado
     */
    public boolean tieneDatoRelacionado(String clave) {
        return datosRelacionados.containsKey(clave);
    }

    @Override
    public String toString() {
        return "NotificacionEvent{" +
                "tipo=" + tipo +
                ", usuario=" + (usuario != null ? usuario.getIdUsuario() : idUsuario) +
                ", canal=" + canal +
                ", titulo='" + titulo + '\'' +
                ", mensaje='" + mensaje + '\'' +
                '}';
    }
}
