package api.astro.whats_orders_manager.modules.facturacion.electronica.validacion;

/** Regla de datos FE incumplida (ADR-044): identifica el desglose del reporte y de los logs sin datos personales. */
public enum ReglaDatoFe {
    NOMBRE_LONGITUD,
    IDENTIFICACION_TIPO,
    IDENTIFICACION_NUMERO,
    TELEFONO,
    CORREO,
    UBICACION,
    OTRAS_SENAS,
    ACTIVIDAD
}
