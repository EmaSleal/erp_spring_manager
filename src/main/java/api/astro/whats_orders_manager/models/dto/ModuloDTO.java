package api.astro.whats_orders_manager.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================================
 * MODULO DTO
 * WhatsApp Orders Manager
 * ============================================================================
 * Data Transfer Object para representar los módulos del sistema en el dashboard.
 * 
 * Cada módulo representa una funcionalidad o sección del sistema con:
 * - Información de visualización (nombre, descripción, icono, color)
 * - Información de navegación (ruta)
 * - Estado de implementación (activo)
 * - Permisos de visibilidad (visible)
 * ============================================================================
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModuloDTO {

    /**
     * Nombre del módulo a mostrar en la UI
     * Ejemplo: "Clientes", "Productos", "Facturas"
     */
    private String nombre;

    /**
     * Descripción breve del módulo
     * Ejemplo: "Gestión de clientes", "Catálogo de productos"
     */
    private String descripcion;

    /**
     * Clase de icono de Font Awesome
     * Ejemplo: "fas fa-users", "fas fa-box", "fas fa-file-invoice-dollar"
     */
    private String icono;

    /**
     * Color hexadecimal para el icono
     * Ejemplo: "#2196F3", "#4CAF50", "#FF9800"
     */
    private String color;

    /**
     * Ruta de navegación del módulo
     * Ejemplo: "/clientes", "/productos", "/facturas"
     */
    private String ruta;

    /**
     * Indica si el módulo está implementado y funcional
     * true = Se puede hacer click y navegar
     * false = Muestra badge "Próximamente" y alerta al hacer click
     */
    private boolean activo;

    /**
     * Indica si el módulo es visible para el rol del usuario actual
     * true = Se muestra en el dashboard
     * false = No se renderiza en la vista
     */
    private boolean visible;
}
