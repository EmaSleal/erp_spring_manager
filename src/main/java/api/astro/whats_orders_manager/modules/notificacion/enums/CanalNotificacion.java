package api.astro.whats_orders_manager.modules.notificacion.enums;

/**
 * ============================================================================
 * CANAL DE NOTIFICACIÓN
 * WhatsApp Orders Manager - Sprint 4 Fase 3
 * ============================================================================
 * Enumera los diferentes canales por los cuales se pueden enviar notificaciones.
 * 
 * CANALES:
 * - WEB: Notificación en la aplicación web (WebSocket, badge)
 * - EMAIL: Notificación por correo electrónico
 * - WHATSAPP: Notificación por WhatsApp (usando API)
 * - SMS: Notificación por SMS (reservado para futuro)
 * 
 * Cada usuario puede configurar sus preferencias por canal.
 * ============================================================================
 */
public enum CanalNotificacion {
    
    /**
     * Notificación en la aplicación web
     * - Aparece en el badge del navbar
     * - Se muestra en el dropdown de notificaciones
     * - Se envía por WebSocket en tiempo real
     */
    WEB("Web", "Notificaciones en la aplicación web", "fa-bell", true),
    
    /**
     * Notificación por correo electrónico
     * - Requiere que el usuario tenga email configurado
     * - Usa las plantillas HTML configuradas
     * - Puede incluir adjuntos (facturas PDF)
     */
    EMAIL("Email", "Notificaciones por correo electrónico", "fa-envelope", true),
    
    /**
     * Notificación por WhatsApp
     * - Requiere integración con API de WhatsApp
     * - Requiere que el destinatario tenga teléfono
     * - Usa plantillas aprobadas por Meta
     */
    WHATSAPP("WhatsApp", "Notificaciones por WhatsApp", "fa-whatsapp", true),
    
    /**
     * Notificación por SMS
     * - Actualmente no implementado
     * - Reservado para futuras versiones
     * - Requiere integración con proveedor SMS
     */
    SMS("SMS", "Notificaciones por mensaje de texto", "fa-comment-sms", false);

    // ==================== ATRIBUTOS ====================
    
    private final String nombre;
    private final String descripcion;
    private final String icono;
    private final boolean disponible;

    // ==================== CONSTRUCTOR ====================
    
    CanalNotificacion(String nombre, String descripcion, String icono, boolean disponible) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.icono = icono;
        this.disponible = disponible;
    }

    // ==================== GETTERS ====================
    
    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getIcono() {
        return icono;
    }

    public boolean isDisponible() {
        return disponible;
    }

    // ==================== MÉTODOS DE UTILIDAD ====================
    
    /**
     * Verifica si este canal está disponible
     */
    public boolean estaDisponible() {
        return disponible;
    }

    /**
     * Verifica si este canal requiere configuración previa
     */
    public boolean requiereConfiguracion() {
        return this == EMAIL || this == WHATSAPP || this == SMS;
    }

    /**
     * Verifica si este canal es en tiempo real
     */
    public boolean esEnTiempoReal() {
        return this == WEB;
    }

    /**
     * Verifica si este canal puede incluir adjuntos
     */
    public boolean permiteAdjuntos() {
        return this == EMAIL;
    }

    /**
     * Obtiene el color del badge para este canal
     */
    public String getColorBadge() {
        return switch (this) {
            case WEB -> "primary";      // Azul
            case EMAIL -> "info";       // Azul claro
            case WHATSAPP -> "success"; // Verde
            case SMS -> "warning";      // Amarillo
        };
    }

    /**
     * Obtiene una lista de canales disponibles
     */
    public static CanalNotificacion[] getCanalesDisponibles() {
        return java.util.Arrays.stream(values())
                .filter(CanalNotificacion::isDisponible)
                .toArray(CanalNotificacion[]::new);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
