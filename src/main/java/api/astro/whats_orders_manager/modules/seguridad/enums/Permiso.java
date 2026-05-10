package api.astro.whats_orders_manager.modules.seguridad.enums;

/**
 * ============================================================================
 * PERMISOS DEL SISTEMA
 * ERP Orders Manager
 * ============================================================================
 * Enum que define todos los permisos granulares del sistema.
 * Cada permiso representa una acción específica que puede realizar un usuario.
 * 
 * @version 1.0 - Sprint 4
 * @since 22/12/2025
 * ============================================================================
 */
public enum Permiso {
    
    // ==================== FACTURACIÓN ====================
    
    FACTURA_VER("Ver facturas", "Visualizar listado y detalle de facturas"),
    FACTURA_CREAR("Crear facturas", "Generar nuevas facturas de venta"),
    FACTURA_EDITAR("Editar facturas", "Modificar facturas existentes (solo si estado = PENDIENTE)"),
    FACTURA_ELIMINAR("Eliminar facturas", "Eliminar facturas del sistema"),
    FACTURA_ANULAR("Anular facturas", "Anular facturas pagadas/completadas"),
    FACTURA_EXPORTAR("Exportar facturas", "Exportar facturas a PDF/Excel"),
    FACTURA_ENVIAR_EMAIL("Enviar facturas", "Enviar facturas por email a clientes"),
    
    // ==================== CLIENTES ====================
    
    CLIENTE_VER("Ver clientes", "Visualizar listado y detalle de clientes"),
    CLIENTE_CREAR("Crear clientes", "Registrar nuevos clientes"),
    CLIENTE_EDITAR("Editar clientes", "Modificar información de clientes"),
    CLIENTE_ELIMINAR("Eliminar clientes", "Eliminar clientes del sistema"),
    CLIENTE_EXPORTAR("Exportar clientes", "Exportar listado de clientes"),
    
    // ==================== PRODUCTOS ====================
    
    PRODUCTO_VER("Ver productos", "Visualizar catálogo de productos"),
    PRODUCTO_CREAR("Crear productos", "Agregar nuevos productos al catálogo"),
    PRODUCTO_EDITAR("Editar productos", "Modificar información de productos"),
    PRODUCTO_ELIMINAR("Eliminar productos", "Eliminar productos del catálogo"),
    PRODUCTO_AJUSTAR_INVENTARIO("Ajustar inventario", "Modificar cantidades en stock"),
    PRODUCTO_EXPORTAR("Exportar productos", "Exportar catálogo de productos"),
    
    // ==================== REPORTES ====================
    
    REPORTE_VENTAS("Reporte de ventas", "Ver reporte de ventas y gráficas"),
    REPORTE_PRODUCTOS("Reporte de productos", "Ver reporte de productos más vendidos"),
    REPORTE_CLIENTES("Reporte de clientes", "Ver reporte de clientes frecuentes"),
    REPORTE_DASHBOARD("Dashboard", "Acceso al dashboard principal"),
    REPORTE_EXPORTAR_PDF("Exportar PDF", "Exportar reportes a PDF"),
    REPORTE_EXPORTAR_EXCEL("Exportar Excel", "Exportar reportes a Excel"),
    REPORTE_EXPORTAR_CSV("Exportar CSV", "Exportar reportes a CSV"),
    
    // ==================== CONFIGURACIÓN ====================
    
    CONFIG_VER("Ver configuración", "Visualizar configuración del sistema"),
    CONFIG_EDITAR_EMPRESA("Editar empresa", "Modificar datos de la empresa"),
    CONFIG_EDITAR_FACTURACION("Editar facturación", "Modificar configuración de facturación"),
    CONFIG_EDITAR_EMAIL("Editar email", "Modificar configuración de email"),
    CONFIG_EDITAR_WHATSAPP("Editar WhatsApp", "Modificar configuración de WhatsApp"),
    
    // ==================== NOTIFICACIONES ====================
    
    NOTIFICACION_VER("Ver notificaciones", "Ver notificaciones del sistema"),
    NOTIFICACION_CREAR("Crear notificaciones", "Enviar notificaciones manuales"),
    NOTIFICACION_MARCAR_LEIDA("Marcar leída", "Marcar notificaciones como leídas"),
    NOTIFICACION_ELIMINAR("Eliminar notificaciones", "Eliminar notificaciones"),
    NOTIFICACION_CONFIGURAR("Configurar notificaciones", "Gestionar preferencias de notificación"),
    
    // ==================== USUARIOS (ADMIN) ====================
    
    USUARIO_VER("Ver usuarios", "Visualizar listado de usuarios"),
    USUARIO_CREAR("Crear usuarios", "Registrar nuevos usuarios"),
    USUARIO_EDITAR("Editar usuarios", "Modificar información de usuarios"),
    USUARIO_ELIMINAR("Eliminar usuarios", "Eliminar usuarios del sistema"),
    USUARIO_BLOQUEAR("Bloquear usuarios", "Bloquear/desbloquear usuarios"),
    USUARIO_CAMBIAR_ROL("Cambiar rol", "Modificar rol de usuarios"),
    USUARIO_VER_ACTIVIDAD("Ver actividad", "Ver registro de actividades de usuarios"),
    USUARIO_RESETEAR_PASSWORD("Resetear contraseña", "Forzar cambio de contraseña"),
    
    // ==================== PAGOS ====================
    
    PAGO_VER("Ver pagos", "Visualizar listado y detalle de pagos"),
    PAGO_CREAR("Crear pagos", "Registrar nuevos pagos de clientes"),
    PAGO_EDITAR("Editar pagos", "Modificar pagos pendientes"),
    PAGO_ELIMINAR("Eliminar pagos", "Eliminar pagos en borrador"),
    PAGO_CONFIRMAR("Confirmar pagos", "Confirmar pagos y generar asiento contable"),
    PAGO_ANULAR("Anular pagos", "Anular pagos confirmados (operación crítica)"),
    PAGO_CONCILIAR("Conciliar pagos", "Marcar pagos como conciliados"),
    PAGO_ESTADO_CUENTA("Estado de cuenta", "Ver estado de cuenta de clientes"),
    
    // ==================== CONTABILIDAD ====================
    
    CONTABILIDAD_VER("Ver contabilidad", "Visualizar plan de cuentas y asientos contables"),
    CONTABILIDAD_CREAR("Crear registros contables", "Crear cuentas contables y asientos en borrador"),
    CONTABILIDAD_EDITAR("Editar contabilidad", "Modificar cuentas y asientos en borrador"),
    CONTABILIDAD_ELIMINAR("Eliminar registros contables", "Eliminar cuentas sin movimientos y asientos en borrador"),
    CONTABILIDAD_CONTABILIZAR("Contabilizar asientos", "Cambiar asientos de borrador a contabilizado"),
    CONTABILIDAD_ANULAR("Anular asientos", "Anular asientos contabilizados (operación crítica)"),
    CONTABILIDAD_REPORTES("Reportes contables", "Ver reportes financieros: Balance, Estado de Resultados, Libro Mayor"),
    
    // ==================== AUDITORÍA ====================
    
    AUDITORIA_VER("Ver auditoría", "Acceso al registro de auditoría"),
    AUDITORIA_EXPORTAR("Exportar auditoría", "Exportar logs de auditoría"),
    
    // ==================== SISTEMA ====================
    
    SISTEMA_VER_LOGS("Ver logs", "Acceso a logs del sistema"),
    SISTEMA_BACKUP("Backup", "Realizar respaldos del sistema"),
    SISTEMA_MANTENIMIENTO("Mantenimiento", "Poner sistema en modo mantenimiento");
    
    private final String nombre;
    private final String descripcion;
    
    Permiso(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    /**
     * Obtiene la categoría del permiso basado en su prefijo
     */
    public String getCategoria() {
        String name = this.name();
        if (name.startsWith("FACTURA_")) return "Facturación";
        if (name.startsWith("CLIENTE_")) return "Clientes";
        if (name.startsWith("PRODUCTO_")) return "Productos";
        if (name.startsWith("PAGO_")) return "Pagos";
        if (name.startsWith("CONTABILIDAD_")) return "Contabilidad";
        if (name.startsWith("REPORTE_")) return "Reportes";
        if (name.startsWith("CONFIG_")) return "Configuración";
        if (name.startsWith("NOTIFICACION_")) return "Notificaciones";
        if (name.startsWith("USUARIO_")) return "Usuarios";
        if (name.startsWith("AUDITORIA_")) return "Auditoría";
        if (name.startsWith("SISTEMA_")) return "Sistema";
        return "Otros";
    }
    
    /**
     * Verifica si el permiso es crítico (requiere admin)
     */
    public boolean esCritico() {
        return this.name().startsWith("USUARIO_") || 
               this.name().startsWith("SISTEMA_") ||
               this.name().startsWith("AUDITORIA_") ||
               this == CONFIG_EDITAR_EMPRESA ||
               this == FACTURA_ELIMINAR ||
               this == FACTURA_ANULAR ||
               this == PAGO_ANULAR ||
               this == CONTABILIDAD_ELIMINAR ||
               this == CONTABILIDAD_ANULAR;
    }
}
