package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.dto.TasaDelDia;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.exception.ProveedorTipoCambioException;
import api.astro.whats_orders_manager.modules.facturacion.service.ProveedorTipoCambio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Tipo de cambio del día desde la API pública de Hacienda (sin credenciales, sin SLA, solo el día actual).
 *
 * <ul>
 *   <li>USD — {@code GET {url-base}/dolar}, verificado 2026-10-11:
 *       {@code {"venta":{"fecha":"2026-10-11","valor":455.6},"compra":{"fecha":"2026-10-11","valor":449.86}}}.</li>
 *   <li>EUR — {@code GET {url-base}/euro}, verificado 2026-10-12:
 *       {@code {"fecha":"2026-10-09","dolares":1.1191,"colones":509.86}}. {@code colones} se usa como compra
 *       y venta; {@code dolares} se ignora. La fecha es la de la última publicación de Hacienda (puede ser
 *       anterior al día de la consulta). También se acepta el formato compra/venta del dólar.
 *       Solo se consulta si EUR está en {@code tipo-cambio.sync.monedas}.</li>
 * </ul>
 *
 * La fecha guardada es la que informa la API (la de venta), no la del servidor.
 */
@Component
@Slf4j
public class HaciendaTipoCambioClient implements ProveedorTipoCambio {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final String urlBase;

    @Autowired
    public HaciendaTipoCambioClient(
            @Value("${tipo-cambio.hacienda.url-base:https://api.hacienda.go.cr/indicadores/tc}") String urlBase,
            @Value("${tipo-cambio.hacienda.timeout-segundos:10}") int timeoutSegundos) {
        this(RestClient.builder()
                .requestFactory(requestFactory(Duration.ofSeconds(timeoutSegundos)))
                // Mismo User-Agent que HaciendaConsultaServiceImpl (la API de Hacienda filtra clientes desconocidos)
                .defaultHeader("User-Agent", "curl/7.81.0")
                .defaultHeader("Accept", "application/json")
                .build(), urlBase);
    }

    /** Constructor con un RestClient ya configurado (pruebas con MockRestServiceServer). */
    public HaciendaTipoCambioClient(RestClient restClient, String urlBase) {
        this.restClient = restClient;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    private static SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }

    @Override
    public FuenteTipoCambio getFuente() {
        return FuenteTipoCambio.API_EXTERNA;
    }

    @Override
    public TasaDelDia obtenerTasaDelDia(MonedaFE moneda) {
        String url = urlBase + ruta(moneda);
        log.debug("Consultando tipo de cambio de {} en {}", moneda, url);

        ResponseEntity<String> respuesta;
        try {
            respuesta = restClient.get().uri(url).retrieve().toEntity(String.class);
        } catch (RestClientException e) {
            throw new ProveedorTipoCambioException(
                    "Error al consultar " + url + ": " + e.getMessage(), e);
        }
        if (respuesta.getStatusCode().value() != HttpStatus.OK.value()) {
            throw new ProveedorTipoCambioException(
                    "Respuesta HTTP " + respuesta.getStatusCode().value() + " de " + url);
        }
        return parsear(moneda, respuesta.getBody());
    }

    private static String ruta(MonedaFE moneda) {
        if (moneda == MonedaFE.USD) {
            return "/dolar";
        }
        if (moneda == MonedaFE.EUR) {
            return "/euro";
        }
        throw new ProveedorTipoCambioException("Moneda no soportada por la API de Hacienda: " + moneda);
    }

    private static TasaDelDia parsear(MonedaFE moneda, String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new ProveedorTipoCambioException("Respuesta vacía de la API de Hacienda para " + moneda);
        }
        JsonNode json;
        try {
            json = MAPPER.readTree(cuerpo);
        } catch (JsonProcessingException e) {
            throw new ProveedorTipoCambioException("JSON inválido de la API de Hacienda para " + moneda, e);
        }
        try {
            if (json.has("venta") && json.has("compra")) {
                JsonNode venta = json.get("venta");
                return new TasaDelDia(fecha(venta.get("fecha")), valor(json.get("compra").get("valor")), valor(venta.get("valor")));
            }
            if (moneda == MonedaFE.EUR && json.has("colones")) {
                BigDecimal colones = valor(json.get("colones"));
                return new TasaDelDia(fecha(json.get("fecha")), colones, colones);
            }
        } catch (IllegalArgumentException | NullPointerException | DateTimeParseException e) {
            throw new ProveedorTipoCambioException(
                    "Datos inválidos de la API de Hacienda para " + moneda + ": " + e.getMessage(), e);
        }
        throw new ProveedorTipoCambioException("Formato no reconocido de la API de Hacienda para " + moneda + ": " + cuerpo);
    }

    private static LocalDate fecha(JsonNode nodo) {
        if (nodo == null || !nodo.isTextual()) {
            throw new IllegalArgumentException("fecha ausente");
        }
        String texto = nodo.asText().trim();
        // Admite "2026-10-11" y "2026-10-11 00:00:00" / "2026-10-11T00:00:00"
        return LocalDate.parse(texto.length() > 10 ? texto.substring(0, 10) : texto);
    }

    private static BigDecimal valor(JsonNode nodo) {
        if (nodo == null || !(nodo.isNumber() || nodo.isTextual())) {
            throw new IllegalArgumentException("valor ausente");
        }
        return new BigDecimal(nodo.asText().trim());
    }
}
