package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import java.time.LocalDateTime;

/**
 * Bloque InformacionReferencia de los comprobantes v4.4 (apartado d del Anexo; etiquetas del XSD). Lo usa la nota de
 * crédito (T-032) y lo usarán los comprobantes sustitutos con referencia (P-073).
 *
 * <pre>
 * &lt;InformacionReferencia&gt;
 *   &lt;TipoDocIR&gt;  nota 10 (01 = factura electrónica)
 *   &lt;Numero&gt;     clave numérica de 50 dígitos del documento referenciado
 *   &lt;FechaEmisionIR&gt; fecha de emisión del documento referenciado
 *   &lt;Codigo&gt;     nota 9
 *   &lt;Razon&gt;      de 3 a 180 caracteres
 * &lt;/InformacionReferencia&gt;
 * </pre>
 */
public final class InformacionReferenciaXml {

    /** Nota 10: factura electrónica. */
    public static final String TIPO_DOC_FACTURA_ELECTRONICA = "01";

    private InformacionReferenciaXml() {
    }

    /**
     * @param tipoDocIR      código de la nota 10
     * @param numero         clave numérica (o consecutivo) del documento referenciado
     * @param fechaEmisionIR fecha de emisión del documento referenciado (hora local de Costa Rica, HoraFiscal)
     * @param codigo         código de la nota 9
     * @param razon          razón de la referencia; se escapa
     * @return el elemento InformacionReferencia, sin declaración de espacio de nombres (hereda el del documento)
     */
    public static String construir(String tipoDocIR, String numero, LocalDateTime fechaEmisionIR, String codigo,
                                   String razon) {
        if (fechaEmisionIR == null) {
            throw new IllegalArgumentException("La fecha de emisión del documento referenciado es obligatoria");
        }
        return "<InformacionReferencia>"
            + "<TipoDocIR>" + escapar(tipoDocIR) + "</TipoDocIR>"
            + "<Numero>" + escapar(numero) + "</Numero>"
            + "<FechaEmisionIR>" + HoraFiscal.formatear(fechaEmisionIR) + "</FechaEmisionIR>"
            + "<Codigo>" + escapar(codigo) + "</Codigo>"
            + "<Razon>" + escapar(razon) + "</Razon>"
            + "</InformacionReferencia>";
    }

    static String escapar(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
