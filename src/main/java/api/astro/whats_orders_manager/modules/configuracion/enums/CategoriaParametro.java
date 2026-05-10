package api.astro.whats_orders_manager.modules.configuracion.enums;

/**
 * Categorías de parámetros del sistema para organizarlos.
 */
public enum CategoriaParametro {
    
    GENERAL("General"),
    FACTURACION("Facturación"),
    WHATSAPP("WhatsApp"),
    NOTIFICACIONES("Notificaciones"),
    REPORTES("Reportes"),
    SEGURIDAD("Seguridad"),
    INTEGRACIONES("Integraciones"),
    SISTEMA("Sistema");

    private final String nombre;

    CategoriaParametro(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
