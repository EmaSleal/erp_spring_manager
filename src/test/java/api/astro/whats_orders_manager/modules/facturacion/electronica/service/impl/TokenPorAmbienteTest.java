package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-037 review 1, criterios 23 y 24 (P-091): el client_id y el realm del IdP salen de AmbienteHacienda. Un servidor
 * local recibe el POST de token en la misma ruta del realm del ambiente (/auth/realms/{realm}/protocol/openid-connect/
 * token) y registra el cuerpo: grant_type password (autenticar) y refresh_token (renovarToken).
 */
@DisplayName("T-037 — client_id y realm del IdP por ambiente (Anexo 3)")
class TokenPorAmbienteTest {

    private HttpServer servidor;
    private String base;
    private final List<String> rutas = new CopyOnWriteArrayList<>();
    private final List<Map<String, String>> cuerpos = new CopyOnWriteArrayList<>();
    private final ConfiguracionHaciendaRepository repository = mock(ConfiguracionHaciendaRepository.class);

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/auth/realms/", ex -> {
            rutas.add(ex.getRequestURI().getPath());
            String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            cuerpos.add(Arrays.stream(cuerpo.split("&")).map(p -> p.split("=", 2))
                .collect(Collectors.toMap(p -> p[0], p -> p.length > 1 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "")));
            byte[] json = "{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":300,\"token_type\":\"bearer\"}"
                .getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, json.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(json);
            }
        });
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    /** Servicio real; la URL del token conserva la ruta del realm del ambiente y cambia solo el host. */
    private HaciendaApiServiceImpl servicio(AmbienteHacienda ambiente) {
        ConfiguracionHacienda config = ConfiguracionHacienda.builder().id(1L).ambiente(ambiente)
            .empresa(new ConfiguracionEmpresa()).usuarioHacienda("cpj-3-101-234567@comprobanteselectronicos.go.cr")
            .passwordHacienda("clave").build();
        when(repository.findById(1L)).thenReturn(Optional.of(config));
        return new HaciendaApiServiceImpl(repository, new RestTemplate(new SimpleClientHttpRequestFactory()),
            new ObjectMapper()) {
            @Override
            protected String urlToken(ConfiguracionHacienda c) {
                return base + URI.create(c.getAmbiente().getUrlToken()).getPath();
            }
        };
    }

    @ParameterizedTest(name = "{0}: client_id {1}, realm {2}")
    @CsvSource({"SANDBOX, api-stag, rut-stag", "PRODUCCION, api-prod, rut"})
    @DisplayName("autenticar (password) y renovarToken (refresh_token) usan el client_id y el realm del ambiente")
    void tokenYRefreshPorAmbiente(AmbienteHacienda ambiente, String clientId, String realm) {
        HaciendaApiServiceImpl servicio = servicio(ambiente);

        servicio.autenticar(1L);
        servicio.renovarToken("refresh-anterior", 1L);

        assertThat(rutas).containsExactly("/auth/realms/" + realm + "/protocol/openid-connect/token",
            "/auth/realms/" + realm + "/protocol/openid-connect/token");
        assertThat(cuerpos.get(0)).containsEntry("grant_type", "password").containsEntry("client_id", clientId)
            .containsEntry("username", "cpj-3-101-234567@comprobanteselectronicos.go.cr");
        assertThat(cuerpos.get(1)).containsEntry("grant_type", "refresh_token").containsEntry("client_id", clientId)
            .containsEntry("refresh_token", "refresh-anterior");
    }

    @Test
    @DisplayName("valores de cada ambiente (Anexo 3: realm rut y api-prod en producción)")
    void valoresDelEnum() {
        assertThat(AmbienteHacienda.PRODUCCION.getUrlToken())
            .isEqualTo("https://idp.comprobanteselectronicos.go.cr/auth/realms/rut/protocol/openid-connect/token");
        assertThat(AmbienteHacienda.PRODUCCION.getClientId()).isEqualTo("api-prod");
        assertThat(AmbienteHacienda.PRODUCCION.getUrlRecepcion())
            .isEqualTo("https://api.comprobanteselectronicos.go.cr/recepcion/v1/recepcion");
        assertThat(AmbienteHacienda.SANDBOX.getUrlToken())
            .isEqualTo("https://idp.comprobanteselectronicos.go.cr/auth/realms/rut-stag/protocol/openid-connect/token");
        assertThat(AmbienteHacienda.SANDBOX.getClientId()).isEqualTo("api-stag");
        assertThat(AmbienteHacienda.SANDBOX.getUrlRecepcion())
            .isEqualTo("https://api-sandbox.comprobanteselectronicos.go.cr/recepcion/v1/recepcion");
    }
}
