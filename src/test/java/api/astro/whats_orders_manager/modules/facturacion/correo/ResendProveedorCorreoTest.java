package api.astro.whats_orders_manager.modules.facturacion.correo;

import api.astro.whats_orders_manager.modules.facturacion.correo.client.AdjuntoCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.client.MensajeCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.client.ResendProveedorCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.client.ResultadoEnvioCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.config.CorreoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * T-055 (ADR-070): cliente Resend con HTTP simulado (MockRestServiceServer, sin red): forma de la petición, adjuntos en
 * base64, clasificación de la respuesta y la clave fuera del resultado y del log.
 */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("T-055 — ResendProveedorCorreo (HTTP simulado)")
class ResendProveedorCorreoTest {

    static final String URL_BASE = "https://resend.test";
    static final String CLAVE = "re_clave_SECRETA_de_prueba_123";

    private MockRestServiceServer server;
    private ResendProveedorCorreo cliente;

    @BeforeEach
    void setUp() {
        CorreoProperties props = new CorreoProperties();
        props.getResend().setUrlBase(URL_BASE + "/");
        props.getResend().setApiKey(CLAVE);
        props.setRemitente("Monrachem <no-reply@astro-solutions.net>");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        cliente = new ResendProveedorCorreo(builder.build(), props);
    }

    private static MensajeCorreo mensaje(List<String> cco, String replyTo) {
        return new MensajeCorreo("Monrachem <no-reply@astro-solutions.net>", "cliente@example.test", cco, replyTo,
                "Factura electrónica 0010000101", "<p>Hola</p>",
                List.of(new AdjuntoCorreo("clave.pdf", "PDF-BYTES".getBytes(StandardCharsets.UTF_8)),
                        new AdjuntoCorreo("clave.xml", "<xml/>".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("POST {url-base}/emails con Bearer, JSON from/to/bcc/reply_to/subject/html y adjuntos en base64")
    void formaDeLaPeticion() {
        server.expect(requestTo(URL_BASE + "/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + CLAVE))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.containsString("application/json")))
                .andExpect(jsonPath("$.from").value("Monrachem <no-reply@astro-solutions.net>"))
                .andExpect(jsonPath("$.to[0]").value("cliente@example.test"))
                .andExpect(jsonPath("$.to.length()").value(1))
                .andExpect(jsonPath("$.bcc[0]").value("archivo@monrachem.test"))
                .andExpect(jsonPath("$.reply_to").value("ventas@monrachem.test"))
                .andExpect(jsonPath("$.subject").value("Factura electrónica 0010000101"))
                .andExpect(jsonPath("$.html").value("<p>Hola</p>"))
                .andExpect(jsonPath("$.attachments.length()").value(2))
                .andExpect(jsonPath("$.attachments[0].filename").value("clave.pdf"))
                .andExpect(jsonPath("$.attachments[0].content").value(
                        Base64.getEncoder().encodeToString("PDF-BYTES".getBytes(StandardCharsets.UTF_8))))
                .andExpect(jsonPath("$.attachments[1].filename").value("clave.xml"))
                .andRespond(withSuccess("{\"id\":\"re-id-1\"}", MediaType.APPLICATION_JSON));

        ResultadoEnvioCorreo r = cliente.enviar(mensaje(List.of("archivo@monrachem.test"), "ventas@monrachem.test"));

        assertThat(r.tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.EXITO);
        assertThat(r.idProveedor()).isEqualTo("re-id-1");
        server.verify();
    }

    @Test
    @DisplayName("sin CCO ni Reply-To el JSON no lleva bcc ni reply_to")
    void sinCcoNiReplyTo() {
        server.expect(requestTo(URL_BASE + "/emails"))
                .andExpect(jsonPath("$.bcc").doesNotExist())
                .andExpect(jsonPath("$.reply_to").doesNotExist())
                .andRespond(withSuccess("{\"id\":\"x\"}", MediaType.APPLICATION_JSON));

        assertThat(cliente.enviar(mensaje(List.of(), null)).tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.EXITO);
        server.verify();
    }

    @Test
    @DisplayName("2xx sin id legible sigue siendo éxito (id nulo)")
    void exitoSinId() {
        server.expect(requestTo(URL_BASE + "/emails")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        ResultadoEnvioCorreo r = cliente.enviar(mensaje(List.of(), null));

        assertThat(r.tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.EXITO);
    }

    @Test
    @DisplayName("429 → LIMITE (se reprograma sin consumir intento)")
    void limite429() {
        server.expect(requestTo(URL_BASE + "/emails"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Too many requests\"}"));

        assertThat(cliente.enviar(mensaje(List.of(), null)).tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.LIMITE);
    }

    @Test
    @DisplayName("5xx → REINTENTABLE")
    void error5xx() {
        server.expect(requestTo(URL_BASE + "/emails")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo(URL_BASE + "/emails")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(cliente.enviar(mensaje(List.of(), null)).tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.REINTENTABLE);
        assertThat(cliente.enviar(mensaje(List.of(), null)).tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.REINTENTABLE);
    }

    @Test
    @DisplayName("timeout o error de red → REINTENTABLE")
    void timeout() {
        server.expect(requestTo(URL_BASE + "/emails")).andRespond(withException(new SocketTimeoutException("Read timed out")));

        ResultadoEnvioCorreo r = cliente.enviar(mensaje(List.of(), null));

        assertThat(r.tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.REINTENTABLE);
        assertThat(r.detalle()).contains("timed out");
    }

    @Test
    @DisplayName("4xx de validación (422, 400, 401) → DEFINITIVO con el detalle del proveedor")
    void definitivo4xx() {
        server.expect(requestTo(URL_BASE + "/emails"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"validation_error\",\"message\":\"Invalid `to` field\"}"));
        server.expect(requestTo(URL_BASE + "/emails")).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        server.expect(requestTo(URL_BASE + "/emails")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ResultadoEnvioCorreo r = cliente.enviar(mensaje(List.of(), null));
        assertThat(r.tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.DEFINITIVO);
        assertThat(r.detalle()).contains("422").contains("Invalid");
        assertThat(cliente.enviar(mensaje(List.of(), null)).tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.DEFINITIVO);
        assertThat(cliente.enviar(mensaje(List.of(), null)).tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.DEFINITIVO);
    }

    @Test
    @DisplayName("la clave nunca aparece en el detalle del resultado ni en el log, aunque el proveedor la repita")
    void claveNuncaEnDetalleNiLog(CapturedOutput salida) {
        server.expect(requestTo(URL_BASE + "/emails"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"API key " + CLAVE + " is invalid; Authorization: Bearer " + CLAVE + "\"}"));
        server.expect(requestTo(URL_BASE + "/emails"))
                .andRespond(withException(new SocketTimeoutException("falló con Bearer " + CLAVE)));
        server.expect(requestTo(URL_BASE + "/emails"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.TEXT_PLAIN).body("eco " + CLAVE));

        List<ResultadoEnvioCorreo> resultados = List.of(cliente.enviar(mensaje(List.of(), null)),
                cliente.enviar(mensaje(List.of(), null)), cliente.enviar(mensaje(List.of(), null)));

        for (ResultadoEnvioCorreo r : resultados) {
            assertThat(r.detalle()).isNotBlank().doesNotContain(CLAVE).doesNotContain("SECRETA");
            assertThat(r.toString()).doesNotContain(CLAVE);
        }
        assertThat(salida.getAll()).as("el log no debe contener la clave").doesNotContain(CLAVE).doesNotContain("SECRETA");
        assertThat(salida.getAll()).as("el fallo sí se registra").contains("Resend");
    }

    @Test
    @DisplayName("configurado() exige clave y remitente")
    void configurado() {
        CorreoProperties p = new CorreoProperties();
        assertThat(new ResendProveedorCorreo(RestClient.builder().build(), p).configurado()).isFalse();
        p.getResend().setApiKey("k");
        p.setRemitente("");
        assertThat(new ResendProveedorCorreo(RestClient.builder().build(), p).configurado()).isFalse();
        p.setRemitente("Monrachem <no-reply@astro-solutions.net>");
        assertThat(new ResendProveedorCorreo(RestClient.builder().build(), p).configurado()).isTrue();
        p.getResend().setApiKey("  ");
        assertThat(new ResendProveedorCorreo(RestClient.builder().build(), p).configurado()).isFalse();
    }

    @Test
    @DisplayName("sin clave configurada no se hace ninguna llamada: REINTENTABLE sin tocar la red")
    void sinClaveNoLlama() {
        CorreoProperties p = new CorreoProperties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer vacio = MockRestServiceServer.bindTo(builder).build();
        ResendProveedorCorreo sinClave = new ResendProveedorCorreo(builder.build(), p);

        ResultadoEnvioCorreo r = sinClave.enviar(mensaje(List.of(), null));

        assertThat(r.tipo()).isEqualTo(ResultadoEnvioCorreo.Tipo.REINTENTABLE);
        vacio.verify();
    }
}
