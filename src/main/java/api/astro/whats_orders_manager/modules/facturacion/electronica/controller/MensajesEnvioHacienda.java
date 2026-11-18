package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl;
import org.springframework.http.HttpStatus;

/**
 * Mensajes fijos al usuario según el resultado de un envío a Hacienda (T-037, ADR-043 «Errores al usuario»). Nunca
 * incluyen el texto de una excepción (P-070): el detalle técnico queda en el log y en el detalle del comprobante.
 */
public final class MensajesEnvioHacienda {

    public static final String RECIBIDO = "Recibido por Hacienda; el resultado se consultará en unos segundos.";
    public static final String NO_TRANSMITIDO = "No se pudo transmitir a Hacienda; se reintentará.";
    public static final String ENVIO_INCIERTO = "Envío incierto; se verificará con Hacienda.";
    public static final String XML_INVALIDO = ComprobanteElectronicoServiceImpl.MENSAJE_XML_INVALIDO_XSD;
    public static final String ERROR_LOCAL = "No se pudo preparar el comprobante (generación, firma o configuración de "
        + "Hacienda). Revise el error en el detalle del comprobante.";
    public static final String ERROR_INESPERADO = "No se pudo completar la operación con el comprobante electrónico. "
        + "Intente de nuevo; si el problema sigue, contacte al administrador.";

    private MensajesEnvioHacienda() {
    }

    /** Mensaje según el estado en que quedó el comprobante tras el envío. */
    public static String mensaje(ComprobanteElectronicoDTO comprobante) {
        EstadoComprobante estado = comprobante.getEstado();
        if (estado == null) {
            return ERROR_INESPERADO;
        }
        return switch (estado) {
            case ENVIADO -> RECIBIDO;
            case NO_TRANSMITIDO -> NO_TRANSMITIDO;
            case ENVIO_INCIERTO -> ENVIO_INCIERTO;
            case ERROR -> esXmlInvalido(comprobante) ? XML_INVALIDO : ERROR_LOCAL;
            default -> "El comprobante quedó en estado " + estado.getCodigo() + ".";
        };
    }

    /** 422 para una falla local (XSD, firma, generación o configuración); 200 en otro caso. */
    public static HttpStatus estadoHttp(ComprobanteElectronicoDTO comprobante) {
        return comprobante.getEstado() == EstadoComprobante.ERROR ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK;
    }

    /** true si Hacienda recibió el comprobante. */
    public static boolean recibido(ComprobanteElectronicoDTO comprobante) {
        return comprobante.getEstado() == EstadoComprobante.ENVIADO;
    }

    /** «No se puede enviar/reenviar» según el estado (criterio 6: el mensaje actual), sin texto de excepción. */
    public static String noEnviable(EstadoComprobante estado, boolean reenvio) {
        return reenvio
            ? "Comprobante no puede ser reenviado en estado " + estado
            : "El comprobante en estado " + estado + " no se puede enviar: Hacienda ya lo tiene o pudo recibirlo";
    }

    static boolean esXmlInvalido(ComprobanteElectronicoDTO comprobante) {
        return comprobante.getEstado() == EstadoComprobante.ERROR && comprobante.getUltimoError() != null
            && comprobante.getUltimoError().startsWith(ComprobanteElectronicoServiceImpl.ENCABEZADO_XML_INVALIDO_XSD);
    }
}
