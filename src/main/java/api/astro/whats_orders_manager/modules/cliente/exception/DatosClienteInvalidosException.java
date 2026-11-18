package api.astro.whats_orders_manager.modules.cliente.exception;

import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.ProblemaDatoFe;

import java.util.List;

/**
 * El cliente con facturación electrónica activa tiene datos FE inválidos (T-038, ADR-044). Lleva la lista de problemas
 * por campo; sus mensajes son fijos y no incluyen el valor del dato.
 */
public class DatosClienteInvalidosException extends RuntimeException {

    public static final String RESUMEN =
        "El cliente tiene datos de facturación electrónica inválidos. Corrija los campos indicados.";

    private final transient List<ProblemaDatoFe> problemas;

    public DatosClienteInvalidosException(List<ProblemaDatoFe> problemas) {
        super(RESUMEN);
        this.problemas = List.copyOf(problemas);
    }

    public List<ProblemaDatoFe> getProblemas() {
        return problemas;
    }
}
