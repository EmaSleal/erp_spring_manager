package api.astro.whats_orders_manager.modules.facturacion.correo.service;

import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Asunto, cuerpo HTML y nombres de adjuntos del correo de un comprobante (puro, sin acceso a datos). */
public final class ContenidoCorreoComprobante {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final String PREFIJO_SANDBOX = "[SANDBOX] ";

    private ContenidoCorreoComprobante() {
    }

    public record DatosCorreoComprobante(boolean esNota, String receptorNombre, String consecutivo, String clave,
                                         LocalDateTime fechaEmision, BigDecimal total, String moneda, String emisor) {
    }

    public static String tipo(DatosCorreoComprobante d) {
        return d.esNota() ? "Nota de crédito electrónica" : "Factura electrónica";
    }

    public static String asunto(DatosCorreoComprobante d, boolean sandbox) {
        String base = tipo(d) + " " + d.consecutivo() + " - " + d.emisor();
        return sandbox ? PREFIJO_SANDBOX + base : base;
    }

    /** Todo valor que viene de la base se escapa; el nombre del receptor es texto libre. */
    public static String html(DatosCorreoComprobante d, boolean conReplyTo) {
        StringBuilder html = new StringBuilder(1024);
        html.append("<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:14px;color:#222\">");
        html.append("<p>Estimado(a) ").append(esc(d.receptorNombre())).append(":</p>");
        html.append("<p>").append(esc(d.emisor())).append(" le envía su ").append(tipo(d).toLowerCase())
            .append(". Adjuntamos el PDF, el XML firmado y la respuesta de Hacienda.</p>");
        html.append("<table style=\"border-collapse:collapse\">");
        fila(html, "Documento", tipo(d));
        fila(html, "Consecutivo", d.consecutivo());
        fila(html, "Clave", d.clave());
        fila(html, "Fecha de emisión", d.fechaEmision() == null ? "" : FECHA.format(d.fechaEmision()));
        fila(html, "Total", d.moneda() + " " + (d.total() == null ? "" : d.total().setScale(2, RoundingMode.HALF_UP).toPlainString()));
        html.append("</table>");
        if (!conReplyTo) {
            html.append("<p style=\"color:#666;font-size:12px\">Este correo no recibe respuestas.</p>");
        }
        html.append("</div>");
        return html.toString();
    }

    private static void fila(StringBuilder html, String etiqueta, String valor) {
        html.append("<tr><td style=\"padding:2px 12px 2px 0;color:#555\">").append(esc(etiqueta))
            .append("</td><td style=\"padding:2px 0\"><strong>").append(esc(valor)).append("</strong></td></tr>");
    }

    private static String esc(String texto) {
        return texto == null ? "" : HtmlUtils.htmlEscape(texto, "UTF-8");
    }

    public static String nombrePdf(String clave) {
        return clave + ".pdf";
    }

    public static String nombreXml(String clave) {
        return clave + ".xml";
    }

    public static String nombreRespuestaHacienda(String clave) {
        return clave + "-respuesta-hacienda.xml";
    }
}
