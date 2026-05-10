package api.astro.whats_orders_manager.modules.notificacion.enums;

/**
 * ============================================================================
 * TIPO DE NOTIFICACIÓN
 * ERP Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Enumera los diferentes tipos de notificaciones que el sistema puede enviar.
 * 
 * TIPOS:
 * - FACTURA_CREADA: Notificación cuando se crea una factura
 * - FACTURA_VENCIDA: Notificación cuando una factura está vencida
 * - FACTURA_PROXIMA_VENCER: Notificación preventiva antes del vencimiento
 * - PAGO_RECIBIDO: Notificación cuando se registra un pago
 * - STOCK_BAJO: Notificación cuando un producto tiene stock bajo
 * - NUEVO_CLIENTE: Notificación al admin cuando se crea un cliente
 * - NUEVO_USUARIO: Notificación al admin cuando se crea un usuario
 * - MENSAJE_WHATSAPP: Notificación de nuevo mensaje de WhatsApp
 * - SISTEMA: Notificaciones del sistema (actualizaciones, mantenimiento)
 * 
 * Cada tipo puede tener diferentes plantillas y canales de envío.
 * ============================================================================
 */
public enum TipoNotificacion {
    
    /**
     * Notificación enviada cuando se crea una nueva factura
     * Canal: EMAIL, WEB
     */
    FACTURA_CREADA("Factura Creada", "Se ha generado una nueva factura"),
    
    /**
     * Notificación enviada cuando una factura ya está vencida
     * Canal: EMAIL, WEB, WHATSAPP
     */
    FACTURA_VENCIDA("Factura Vencida", "Una factura ha vencido y requiere pago"),
    
    /**
     * Notificación preventiva enviada días antes del vencimiento
     * Canal: EMAIL, WEB, WHATSAPP
     */
    FACTURA_PROXIMA_VENCER("Factura Próxima a Vencer", "Recordatorio de factura próxima a vencer"),
    
    /**
     * Notificación enviada cuando se registra un pago
     * Canal: EMAIL, WEB
     */
    PAGO_RECIBIDO("Pago Recibido", "Se ha registrado un nuevo pago"),
    
    /**
     * Notificación enviada cuando el stock de un producto es bajo
     * Canal: EMAIL, WEB
     */
    STOCK_BAJO("Stock Bajo", "Un producto tiene stock bajo"),
    
    /**
     * Notificación al admin cuando se crea un nuevo cliente
     * Canal: EMAIL, WEB
     */
    NUEVO_CLIENTE("Nuevo Cliente", "Se ha registrado un nuevo cliente"),
    
    /**
     * Notificación al admin cuando se crea un nuevo usuario
     * Canal: EMAIL, WEB
     */
    NUEVO_USUARIO("Nuevo Usuario", "Se ha registrado un nuevo usuario"),
    
    /**
     * Notificación de nuevo mensaje recibido por WhatsApp
     * Canal: WEB
     */
    MENSAJE_WHATSAPP("Mensaje WhatsApp", "Nuevo mensaje de WhatsApp recibido"),
    
    /**
     * Notificaciones del sistema (actualizaciones, mantenimiento, etc.)
     * Canal: EMAIL, WEB
     */
    SISTEMA("Notificación del Sistema", "Notificación administrativa del sistema");

    // ==================== ATRIBUTOS ====================
    
    private final String titulo;
    private final String descripcion;

    // ==================== CONSTRUCTOR ====================
    
    TipoNotificacion(String titulo, String descripcion) {
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    // ==================== GETTERS ====================
    
    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Verifica si este tipo de notificación es crítica
     * (requiere atención inmediata)
     */
    public boolean esCritica() {
        return this == FACTURA_VENCIDA || this == STOCK_BAJO;
    }

    /**
     * Verifica si este tipo de notificación es para clientes
     */
    public boolean esParaCliente() {
        return this == FACTURA_CREADA 
            || this == FACTURA_VENCIDA 
            || this == FACTURA_PROXIMA_VENCER 
            || this == PAGO_RECIBIDO;
    }

    /**
     * Verifica si este tipo de notificación es solo para admin
     */
    public boolean esSoloAdmin() {
        return this == NUEVO_CLIENTE 
            || this == NUEVO_USUARIO 
            || this == STOCK_BAJO 
            || this == SISTEMA;
    }

    /**
     * Obtiene el icono Font Awesome para este tipo de notificación
     */
    public String getIcono() {
        return switch (this) {
            case FACTURA_CREADA -> "fa-file-invoice";
            case FACTURA_VENCIDA -> "fa-exclamation-triangle";
            case FACTURA_PROXIMA_VENCER -> "fa-clock";
            case PAGO_RECIBIDO -> "fa-money-bill-wave";
            case STOCK_BAJO -> "fa-box-open";
            case NUEVO_CLIENTE -> "fa-user-plus";
            case NUEVO_USUARIO -> "fa-user-shield";
            case MENSAJE_WHATSAPP -> "fa-whatsapp";
            case SISTEMA -> "fa-cog";
        };
    }

    /**
     * Obtiene el color de la notificación para la UI
     */
    public String getColor() {
        return switch (this) {
            case FACTURA_CREADA, PAGO_RECIBIDO -> "success"; // Verde
            case FACTURA_VENCIDA, STOCK_BAJO -> "danger";    // Rojo
            case FACTURA_PROXIMA_VENCER -> "warning";        // Amarillo
            case NUEVO_CLIENTE, NUEVO_USUARIO -> "info";     // Azul
            case MENSAJE_WHATSAPP -> "primary";              // Azul primario
            case SISTEMA -> "secondary";                     // Gris
        };
    }

    @Override
    public String toString() {
        return titulo;
    }
}
