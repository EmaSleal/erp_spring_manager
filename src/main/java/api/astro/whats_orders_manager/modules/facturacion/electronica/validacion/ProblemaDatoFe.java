package api.astro.whats_orders_manager.modules.facturacion.electronica.validacion;

/**
 * Un dato FE inválido o faltante (ADR-044).
 *
 * @param campo       nombre estable del campo, igual a la propiedad JSON de Cliente (o de ConfiguracionEmpresa)
 * @param mensaje     texto fijo en español para el usuario; nunca el texto de una excepción ni el valor del dato
 * @param regla       regla incumplida
 * @param obligatorio true si impide emitir; false si el dato es opcional y solo se omite del XML
 */
public record ProblemaDatoFe(String campo, String mensaje, ReglaDatoFe regla, boolean obligatorio) {
}
