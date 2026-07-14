package api.astro.whats_orders_manager.modules.inventario.enums;

public enum TipoMovimientoInventario {
    ENTRADA_COMPRA,
    ENTRADA_DEVOLUCION,
    ENTRADA_AJUSTE,
    ENTRADA_TRANSFERENCIA,
    SALIDA_VENTA,
    SALIDA_DEVOLUCION,
    SALIDA_AJUSTE,
    SALIDA_TRANSFERENCIA,
    SALIDA_PRODUCCION;

    public boolean isEntrada() {
        return name().startsWith("ENTRADA_");
    }

    public boolean isSalida() {
        return name().startsWith("SALIDA_");
    }
}
