package api.astro.whats_orders_manager.modules.regimen.verificacion;

/** Resultado de comparar el régimen del ERP con el que Hacienda reporta para el emisor (T-056). */
public enum ResultadoVerificacion {
    /** Hacienda reporta el mismo régimen que el ERP tiene vigente hoy. */
    COINCIDE,
    /** Hacienda reporta otro régimen (incluido «otro»: no contribuyente o un código desconocido). */
    DIFIERE,
    /** Hacienda no tiene la identificación del emisor. */
    NO_ENCONTRADO,
    /** No hubo respuesta utilizable: la API falló, no trae el régimen o falta la identificación de la empresa. */
    SIN_RESPUESTA
}
