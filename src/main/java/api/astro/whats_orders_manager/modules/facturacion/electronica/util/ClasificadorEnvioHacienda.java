package api.astro.whats_orders_manager.modules.facturacion.electronica.util;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Clasifica en origen el resultado de un POST /recepcion a Hacienda (ADR-030 §3).
 *
 * <p>Regla prudente: solo se declara NO_TRANSMITIDO cuando hay evidencia de que Hacienda no recibió el documento;
 * ante la duda, ENVIO_INCIERTO (se resuelve con "Consultar estado en Hacienda").
 *
 * <p>Fuente de los textos de X-Error-Cause: la documentación oficial del API de recepción
 * (https://www.hacienda.go.cr/docs/ComprobantesElectronicosAPI.html, resumida en
 * docs/referencias/HACIENDA_API_RECEPCION.md) documenta para el 400 de POST /recepcion la causa
 * "El comprobante con la Clave [&lt;clave&gt;] ya fue recibido.", que se reconoce de forma explícita. No hay un
 * catálogo oficial del resto de causas: los demás patrones de "ya recibido" son amplios y cualquier causa que no
 * coincida con un patrón de validación conocido se trata como incierta.
 */
public final class ClasificadorEnvioHacienda {

    /** Encabezado con la causa del rechazo de la recepción. */
    public static final String HEADER_ERROR_CAUSE = "X-Error-Cause";

    /**
     * Causa documentada por Hacienda para un comprobante ya recibido, normalizada (sin tildes, minúsculas): "el
     * comprobante con la clave [&lt;clave&gt;] ya fue recibido", con o sin corchetes y punto final.
     */
    private static final Pattern YA_RECIBIDO_DOCUMENTADO = Pattern.compile(
            "^el comprobante con la clave\\s*\\[?\\s*\\d+\\s*\\]?\\s*ya fue recibido\\s*\\.?$");

    /** Otras formas de decir que Hacienda ya tenía el documento: reenviar o reemitir duplicaría (P-048). */
    private static final List<String> PATRONES_YA_RECIBIDO = List.of(
            "ya fue recibid", "ya ha sido recibid", "recibido anteriormente", "ya se recibi", "ya existe",
            "ya registrad", "duplicad");

    /**
     * La causa es una validación del documento o de la petición: Hacienda no lo aceptó para procesar. Ninguno
     * coincide con el texto de "ya fue recibido" (ClasificadorEnvioHaciendaTest).
     */
    static final List<String> PATRONES_VALIDACION = List.of(
            "no es valid", "invalid", "no cumple", "esquema", "xsd", "formato", "firma", "no coincide",
            "requerid", "obligatori", "no corresponde", "incorrect", "mal formad", "no se pudo leer",
            "no permitid", "vencid");

    private ClasificadorEnvioHacienda() {
    }

    /**
     * Respuesta HTTP no exitosa de la recepción.
     *
     * @param status       código HTTP (4xx o 5xx)
     * @param xErrorCause  valor de X-Error-Cause, si vino
     */
    public static ResultadoEnvioHacienda porRespuesta(int status, String xErrorCause) {
        if (status >= 200 && status < 300) {
            return ResultadoEnvioHacienda.RECIBIDO;
        }
        if (status == 401 || status == 403) {
            return ResultadoEnvioHacienda.NO_TRANSMITIDO; // token rechazado: la recepción no se procesó
        }
        if (status >= 500) {
            return ResultadoEnvioHacienda.ENVIO_INCIERTO;
        }
        String causa = normalizar(xErrorCause);
        if (causa.isEmpty()) {
            return ResultadoEnvioHacienda.ENVIO_INCIERTO;
        }
        if (esYaRecibidoDocumentado(xErrorCause) || PATRONES_YA_RECIBIDO.stream().anyMatch(causa::contains)) {
            return ResultadoEnvioHacienda.ENVIO_INCIERTO;
        }
        if (PATRONES_VALIDACION.stream().anyMatch(causa::contains)) {
            return ResultadoEnvioHacienda.NO_TRANSMITIDO;
        }
        return ResultadoEnvioHacienda.ENVIO_INCIERTO;
    }

    /**
     * Falla de transporte (sin respuesta HTTP). NO_TRANSMITIDO si no llegó a haber conexión (host no resoluble,
     * conexión rechazada, sin ruta, timeout de conexión); ENVIO_INCIERTO si la conexión existía (timeout de
     * lectura, conexión cortada, respuesta incompleta).
     */
    public static ResultadoEnvioHacienda porFalloDeTransporte(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof UnknownHostException || t instanceof ConnectException
                    || t instanceof NoRouteToHostException || t instanceof PortUnreachableException
                    || t instanceof java.net.http.HttpConnectTimeoutException) {
                return ResultadoEnvioHacienda.NO_TRANSMITIDO;
            }
            if (t instanceof SocketTimeoutException) {
                // HttpURLConnection: "Connect timed out" (sin conexión) frente a "Read timed out" (ya enviado)
                String mensaje = t.getMessage() != null ? t.getMessage().toLowerCase(Locale.ROOT) : "";
                return mensaje.contains("connect")
                        ? ResultadoEnvioHacienda.NO_TRANSMITIDO
                        : ResultadoEnvioHacienda.ENVIO_INCIERTO;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return ResultadoEnvioHacienda.ENVIO_INCIERTO;
    }

    /**
     * true si la causa es la documentada por Hacienda para un comprobante ya recibido, sin distinguir mayúsculas ni
     * tildes y con la clave entre corchetes o sin ellos.
     */
    public static boolean esYaRecibidoDocumentado(String xErrorCause) {
        return YA_RECIBIDO_DOCUMENTADO.matcher(normalizar(xErrorCause)).matches();
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
