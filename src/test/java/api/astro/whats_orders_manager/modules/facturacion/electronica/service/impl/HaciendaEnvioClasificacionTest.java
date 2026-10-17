package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-025 (ADR-030 §3) — el cliente de Hacienda clasifica cada POST /recepcion en origen, contra un servidor de
 * Hacienda simulado (com.sun.net.httpserver en 127.0.0.1). Ninguna prueba llama a un host real de Hacienda:
 * las URLs apuntan al servidor local, a un puerto cerrado de 127.0.0.1 o a un dominio .invalid.
 */
@DisplayName("T-025 — clasificación del envío a Hacienda en origen y 404 de la consulta (servidor simulado)")
class HaciendaEnvioClasificacionTest {

    private static final String CLAVE = "50617102600310123456700100001010000000001100000001";
    private static final int CONNECT_TIMEOUT_MS = 500;
    private static final int READ_TIMEOUT_MS = 700;

    private HttpServer servidor;
    private final List<String> llamadas = new CopyOnWriteArrayList<>();
    private volatile Respuesta recepcion = new Respuesta(202, Map.of(), "", 0);
    private volatile Respuesta consulta = new Respuesta(200, Map.of(), "{}", 0);
    private volatile int statusToken = 200;
    private String urlBase;
    private String urlToken;
    private ConfiguracionHaciendaRepository configuracionRepository;

    record Respuesta(int status, Map<String, String> headers, String body, long demoraMs) {
    }

    @BeforeEach
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/token", ex -> {
            llamadas.add("token");
            responder(ex, new Respuesta(statusToken, Map.of("Content-Type", "application/json"),
                    "{\"access_token\":\"t\",\"refresh_token\":\"r\",\"expires_in\":300,\"token_type\":\"bearer\"}", 0));
        });
        servidor.createContext("/recepcion/v1/recepcion", ex -> {
            if ("POST".equals(ex.getRequestMethod())) {
                llamadas.add("POST recepcion");
                responder(ex, recepcion);
            } else {
                llamadas.add("GET recepcion");
                responder(ex, consulta);
            }
        });
        servidor.start();
        String local = "http://127.0.0.1:" + servidor.getAddress().getPort();
        urlBase = local;
        urlToken = local + "/token";

        ConfiguracionEmpresa empresa = new ConfiguracionEmpresa();
        empresa.setNumeroIdentificacion("3101234567");
        ConfiguracionHacienda config = ConfiguracionHacienda.builder()
                .id(1L).empresa(empresa).usuarioHacienda("usuario").passwordHacienda("clave").build();
        configuracionRepository = mock(ConfiguracionHaciendaRepository.class);
        when(configuracionRepository.findById(1L)).thenReturn(Optional.of(config));
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    // ── Envío: fallas de conexión ────────────────────────────────────────────

    @Test
    @DisplayName("conexión rechazada (puerto cerrado) → NO_TRANSMITIDO")
    void conexionRechazada() throws IOException {
        urlBase = "http://127.0.0.1:" + puertoCerrado();
        assertThat(enviar().getResultadoEnvio()).isEqualTo(ResultadoEnvioHacienda.NO_TRANSMITIDO);
    }

    @Test
    @DisplayName("host no resoluble (.invalid) → NO_TRANSMITIDO")
    void hostNoResoluble() {
        urlBase = "http://recepcion.hacienda-t025.invalid";
        assertThat(enviar().getResultadoEnvio()).isEqualTo(ResultadoEnvioHacienda.NO_TRANSMITIDO);
    }

    @Test
    @DisplayName("timeout de conexión → NO_TRANSMITIDO (fábrica que reproduce el SocketTimeoutException de connect)")
    void timeoutDeConexion() {
        // Un timeout de conexión real depende de la red (IP que descarte el SYN). La fábrica falla con la misma
        // excepción que HttpURLConnection lanza en connect(): SocketTimeoutException("Connect timed out").
        ClientHttpRequestFactory base = fabrica();
        ClientHttpRequestFactory fabrica = (uri, method) -> {
            if (method == HttpMethod.POST && uri.getPath().startsWith("/recepcion")) {
                throw new SocketTimeoutException("Connect timed out");
            }
            return base.createRequest(uri, method);
        };
        assertThat(servicio(fabrica).enviarComprobante("<xml/>", CLAVE, 1L).getResultadoEnvio())
                .isEqualTo(ResultadoEnvioHacienda.NO_TRANSMITIDO);
        assertThat(llamadas).doesNotContain("POST recepcion");
    }

    @Test
    @DisplayName("timeout de lectura (respuesta más lenta que el read timeout) → ENVIO_INCIERTO")
    void timeoutDeLectura() {
        recepcion = new Respuesta(202, Map.of(), "", READ_TIMEOUT_MS * 3L);
        assertThat(enviar().getResultadoEnvio()).isEqualTo(ResultadoEnvioHacienda.ENVIO_INCIERTO);
        assertThat(llamadas).contains("POST recepcion");
    }

    @Test
    @DisplayName("no se obtiene el token (401 del IdP) → NO_TRANSMITIDO, sin POST a la recepción")
    void sinToken() {
        statusToken = 401;
        assertThat(enviar().getResultadoEnvio()).isEqualTo(ResultadoEnvioHacienda.NO_TRANSMITIDO);
        assertThat(llamadas).doesNotContain("POST recepcion");
    }

    // ── Envío: respuestas HTTP ───────────────────────────────────────────────

    @Test
    @DisplayName("202 → RECIBIDO (ENVIADO)")
    void recibido202() {
        recepcion = new Respuesta(202, Map.of(), "", 0);
        RespuestaHaciendaDTO respuesta = enviar();
        assertThat(respuesta.getResultadoEnvio()).isEqualTo(ResultadoEnvioHacienda.RECIBIDO);
        assertThat(respuesta.getCodigoHttp()).isEqualTo(202);
    }

    @ParameterizedTest(name = "{0} {1} → {2}")
    @CsvSource(delimiter = '|', nullValues = "SIN_CAUSA", value = {
            "500|SIN_CAUSA|ENVIO_INCIERTO",
            "503|SIN_CAUSA|ENVIO_INCIERTO",
            "400|El comprobante con la clave indicada ya fue recibido anteriormente|ENVIO_INCIERTO",
            "400|Ya existe un comprobante registrado con esa clave|ENVIO_INCIERTO",
            "400|El XML no cumple con el esquema de validación|NO_TRANSMITIDO",
            "400|La firma del comprobante no es válida|NO_TRANSMITIDO",
            "400|SIN_CAUSA|ENVIO_INCIERTO",
            "400|Ocurrió una situación no documentada|ENVIO_INCIERTO",
            "401|SIN_CAUSA|NO_TRANSMITIDO",
            "403|SIN_CAUSA|NO_TRANSMITIDO"
    })
    @DisplayName("respuestas de error de la recepción")
    void respuestasHttp(int status, String causa, ResultadoEnvioHacienda esperado) {
        recepcion = new Respuesta(status, causa != null ? Map.of("X-Error-Cause", causa) : Map.of(), "", 0);
        RespuestaHaciendaDTO respuesta = enviar();
        assertThat(respuesta.getResultadoEnvio()).isEqualTo(esperado);
        assertThat(respuesta.getCodigoHttp()).isEqualTo(status);
        assertThat(respuesta.getCausaError()).isEqualTo(causa);
    }

    // ── Consulta ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("consulta con 404 → documentoNoExiste (ya no se lee como procesando)")
    void consulta404() {
        consulta = new Respuesta(404, Map.of(), "", 0);
        RespuestaHaciendaDTO respuesta = servicio(fabrica()).consultarComprobante(CLAVE, 1L);
        assertThat(respuesta.getDocumentoNoExiste()).isTrue();
        assertThat(respuesta.getIndicadorEstado()).isNull();
    }

    @ParameterizedTest(name = "ind-estado {0}")
    @CsvSource({"aceptado", "rechazado", "procesando", "recibido"})
    @DisplayName("consulta con ind-estado → se informa tal cual, sin documentoNoExiste")
    void consultaConEstado(String indEstado) {
        consulta = new Respuesta(200, Map.of("Content-Type", "application/json"),
                "{\"clave\":\"" + CLAVE + "\",\"ind-estado\":\"" + indEstado + "\"}", 0);
        RespuestaHaciendaDTO respuesta = servicio(fabrica()).consultarComprobante(CLAVE, 1L);
        assertThat(respuesta.getIndicadorEstado()).isEqualTo(indEstado);
        assertThat(respuesta.getDocumentoNoExiste()).isNotEqualTo(Boolean.TRUE);
    }

    // ── Soporte ──────────────────────────────────────────────────────────────

    private RespuestaHaciendaDTO enviar() {
        return servicio(fabrica()).enviarComprobante("<xml/>", CLAVE, 1L);
    }

    private SimpleClientHttpRequestFactory fabrica() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(CONNECT_TIMEOUT_MS);
        fabrica.setReadTimeout(READ_TIMEOUT_MS);
        return fabrica;
    }

    /** Servicio real con las URLs de Hacienda reemplazadas por las del servidor simulado. */
    private HaciendaApiServiceImpl servicio(ClientHttpRequestFactory fabrica) {
        return new HaciendaApiServiceImpl(configuracionRepository, new RestTemplate(fabrica), new ObjectMapper()) {
            @Override
            protected String urlBase(ConfiguracionHacienda config) {
                return urlBase;
            }

            @Override
            protected String urlToken(ConfiguracionHacienda config) {
                return urlToken;
            }
        };
    }

    private static int puertoCerrado() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void responder(HttpExchange ex, Respuesta respuesta) throws IOException {
        try {
            if (respuesta.demoraMs() > 0) {
                Thread.sleep(respuesta.demoraMs());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ex.getRequestBody().readAllBytes();
        respuesta.headers().forEach((k, v) -> ex.getResponseHeaders().add(k, v));
        byte[] cuerpo = respuesta.body().getBytes(StandardCharsets.UTF_8);
        try {
            ex.sendResponseHeaders(respuesta.status(), cuerpo.length == 0 ? -1 : cuerpo.length);
            if (cuerpo.length > 0) {
                try (OutputStream out = ex.getResponseBody()) {
                    out.write(cuerpo);
                }
            }
        } catch (IOException e) {
            // el cliente cerró la conexión (timeout de lectura)
        } finally {
            ex.close();
        }
    }
}
