package api.astro.whats_orders_manager.modules.facturacion.electronica.exception;

import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.ProblemaDatoFe;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Los datos FE obligatorios del receptor o del emisor faltan o son inválidos (T-038, ADR-044): la emisión se bloquea en
 * la etapa a, antes de tomar el consecutivo. Es una EmisionNoPermitidaException, así que se responde 409 donde ya se
 * responde así (procesar, generar y reemitir). El mensaje es fijo: nombra los campos, nunca sus valores.
 */
public class DatosFeIncompletosException extends EmisionNoPermitidaException {

    private static final Map<String, String> ETIQUETA = Map.ofEntries(
        Map.entry("nombre", "nombre"),
        Map.entry("tipoIdentificacion", "identificación"),
        Map.entry("numeroIdentificacion", "identificación"),
        Map.entry("razonSocial", "razón social"),
        Map.entry("codigoProvincia", "provincia"),
        Map.entry("canton", "cantón"),
        Map.entry("distrito", "distrito"),
        Map.entry("otrasSenas", "otras señas"),
        Map.entry("telefono", "teléfono"),
        Map.entry("email", "correo electrónico"));

    private final List<ProblemaDatoFe> problemas;

    private DatosFeIncompletosException(String mensaje, List<ProblemaDatoFe> problemas) {
        super(mensaje);
        this.problemas = List.copyOf(problemas);
    }

    /** Receptor con obligatorios inválidos; {@code etiquetaCliente} es el nombre del cliente o su id. */
    public static DatosFeIncompletosException delReceptor(String etiquetaCliente, List<ProblemaDatoFe> problemas) {
        return new DatosFeIncompletosException("El cliente " + etiquetaCliente
            + " no tiene los datos de facturación electrónica completos: " + campos(problemas), problemas);
    }

    /** Emisor (configuración de la empresa) con datos inválidos. */
    public static DatosFeIncompletosException delEmisor(List<ProblemaDatoFe> problemas) {
        return new DatosFeIncompletosException("Los datos de facturación electrónica de la empresa están incompletos o "
            + "no son válidos (" + campos(problemas) + "). Corríjalos en Configuración → Empresa.", problemas);
    }

    public List<ProblemaDatoFe> getProblemas() {
        return problemas;
    }

    private static String campos(List<ProblemaDatoFe> problemas) {
        Set<String> etiquetas = new LinkedHashSet<>();
        problemas.forEach(p -> etiquetas.add(ETIQUETA.getOrDefault(p.campo(), p.campo())));
        return String.join(", ", etiquetas);
    }
}
