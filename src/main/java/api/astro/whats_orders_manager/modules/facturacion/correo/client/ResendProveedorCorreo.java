package api.astro.whats_orders_manager.modules.facturacion.correo.client;

import api.astro.whats_orders_manager.modules.facturacion.correo.config.CorreoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T-055 (ADR-070): cliente de la API de Resend, portado de fractall ({@code ResendEmailClient}) con lo que allá faltaba:
 * timeouts de conexión y lectura, resultado clasificado en lugar de un boolean, URL base configurable y la clave fuera de
 * cualquier log o error guardado.
 *
 * <p>{@code POST {url-base}/emails} con {@code Authorization: Bearer <clave>} y el JSON
 * {@code {from, to, bcc?, reply_to?, subject, html, attachments:[{filename, content(base64)}]}}.
 * Clasificación: 2xx → éxito (id del proveedor); 429 → límite; 5xx y errores de red/timeout → reintentable; otro 4xx →
 * definitivo. El método nunca lanza ante un fallo de entrega.
 */
@Component
@Slf4j
public class ResendProveedorCorreo implements ProveedorCorreo {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_DETALLE = 300;

    private final RestClient restClient;
    private final CorreoProperties props;

    @Autowired
    public ResendProveedorCorreo(CorreoProperties props) {
        this(RestClient.builder().requestFactory(fabrica(props)).build(), props);
    }

    /** Con un RestClient ya configurado (pruebas con MockRestServiceServer). */
    public ResendProveedorCorreo(RestClient restClient, CorreoProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    private static SimpleClientHttpRequestFactory fabrica(CorreoProperties props) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(props.getResend().getConnectTimeout());
        fabrica.setReadTimeout(props.getResend().getReadTimeout());
        return fabrica;
    }

    @Override
    public boolean configurado() {
        return tieneTexto(props.getResend().getApiKey()) && tieneTexto(props.getRemitente());
    }

    @Override
    public ResultadoEnvioCorreo enviar(MensajeCorreo mensaje) {
        if (!configurado()) {
            return ResultadoEnvioCorreo.reintentable("Resend no está configurado (falta RESEND_API_KEY o correo.remitente)");
        }
        String clave = props.getResend().getApiKey().trim();
        try {
            return restClient.post()
                .uri(urlBase() + "/emails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clave)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo(mensaje))
                .exchange((peticion, respuesta) -> {
                    String texto = leer(respuesta.getBody());
                    return clasificar(respuesta.getStatusCode().value(), texto, clave);
                });
        } catch (RuntimeException e) {
            // ResourceAccessException (timeout, conexión rechazada) u otro fallo de transporte: se puede reintentar
            String detalle = limpiar("Error de red al llamar a Resend: " + e.getClass().getSimpleName() + ": "
                + e.getMessage(), clave);
            log.warn("Resend: {}", detalle);
            return ResultadoEnvioCorreo.reintentable(detalle);
        }
    }

    private ResultadoEnvioCorreo clasificar(int estado, String cuerpo, String clave) {
        if (estado >= 200 && estado < 300) {
            return ResultadoEnvioCorreo.exito(idDe(cuerpo));
        }
        String detalle = limpiar("HTTP " + estado + (cuerpo.isBlank() ? "" : ": " + cuerpo), clave);
        log.warn("Resend respondió {}", detalle);
        if (estado == 429) {
            return ResultadoEnvioCorreo.limite(detalle);
        }
        if (estado >= 500 || estado == 408) {
            return ResultadoEnvioCorreo.reintentable(detalle);
        }
        return ResultadoEnvioCorreo.definitivo(detalle);
    }

    private Map<String, Object> cuerpo(MensajeCorreo m) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("from", m.remitente());
        json.put("to", List.of(m.destinatario()));
        if (m.cco() != null && !m.cco().isEmpty()) {
            json.put("bcc", m.cco());
        }
        if (tieneTexto(m.replyTo())) {
            json.put("reply_to", m.replyTo());
        }
        json.put("subject", m.asunto());
        json.put("html", m.html());
        if (m.adjuntos() != null && !m.adjuntos().isEmpty()) {
            List<Map<String, String>> adjuntos = new ArrayList<>();
            for (AdjuntoCorreo a : m.adjuntos()) {
                Map<String, String> adjunto = new LinkedHashMap<>();
                adjunto.put("filename", a.nombre());
                adjunto.put("content", Base64.getEncoder().encodeToString(a.contenido()));
                adjuntos.add(adjunto);
            }
            json.put("attachments", adjuntos);
        }
        return json;
    }

    private String urlBase() {
        String url = props.getResend().getUrlBase().trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String idDe(String cuerpo) {
        try {
            JsonNode id = MAPPER.readTree(cuerpo).path("id");
            return id.isMissingNode() || id.isNull() ? null : id.asText();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static String leer(java.io.InputStream flujo) throws IOException {
        return flujo == null ? "" : new String(flujo.readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Quita la clave de cualquier texto que se guarde o se registre y lo acota. */
    static String limpiar(String texto, String clave) {
        String limpio = texto == null ? "" : texto;
        if (tieneTexto(clave)) {
            limpio = limpio.replace(clave, "***");
        }
        limpio = limpio.replaceAll("(?i)Bearer\\s+\\S+", "Bearer ***").replaceAll("\\s+", " ").trim();
        return limpio.length() > MAX_DETALLE ? limpio.substring(0, MAX_DETALLE) : limpio;
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
