package api.astro.whats_orders_manager.modules.facturacion.correo.client;

/**
 * Resultado de un envío (ADR-070): éxito con el id del proveedor, error reintentable (5xx, timeout), límite del
 * proveedor (429: se reprograma para mañana sin consumir intento) o error definitivo (4xx de validación). El detalle
 * nunca contiene la clave del proveedor.
 */
public record ResultadoEnvioCorreo(Tipo tipo, String idProveedor, String detalle) {

    public enum Tipo { EXITO, REINTENTABLE, LIMITE, DEFINITIVO }

    public static ResultadoEnvioCorreo exito(String idProveedor) {
        return new ResultadoEnvioCorreo(Tipo.EXITO, idProveedor, null);
    }

    public static ResultadoEnvioCorreo reintentable(String detalle) {
        return new ResultadoEnvioCorreo(Tipo.REINTENTABLE, null, detalle);
    }

    public static ResultadoEnvioCorreo limite(String detalle) {
        return new ResultadoEnvioCorreo(Tipo.LIMITE, null, detalle);
    }

    public static ResultadoEnvioCorreo definitivo(String detalle) {
        return new ResultadoEnvioCorreo(Tipo.DEFINITIVO, null, detalle);
    }
}
