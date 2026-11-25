package api.astro.whats_orders_manager.modules.regimen;

import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO.ResultadoConsulta;
import api.astro.whats_orders_manager.modules.configuracion.service.impl.HaciendaConsultaServiceImpl;
import api.astro.whats_orders_manager.modules.regimen.verificacion.VerificacionRegimenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * T-056 (criterio 6): la forma del campo {@code regimen} en el JSON de /fe/ae según las pruebas es un objeto
 * {@code {"codigo": <entero>, "descripcion": <texto>}} (1 general, 2 simplificado, 3 no contribuyente). Servidor
 * simulado con los códigos 1, 2, 3, uno desconocido, 404 (NO_ENCONTRADA) y 500 (ERROR). LA FORMA REAL FALTA CONFIRMARLA
 * con una respuesta del emisor (P-127): estas pruebas fijan lo que el código supone, no lo que Hacienda devuelve hoy.
 */
@DisplayName("T-056 — /fe/ae: forma del campo regimen y su traducción")
class HaciendaRegimenJsonTest {

    private static final String URL = "https://hacienda.test/fe/ae";
    private static final String CONSULTA = URL + "?identificacion=3101123456";
    private HaciendaConsultaServiceImpl servicio;
    private MockRestServiceServer servidor;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        servicio = new HaciendaConsultaServiceImpl();
        ReflectionTestUtils.setField(servicio, "restClient", builder.build());
        ReflectionTestUtils.setField(servicio, "haciendaApiUrl", URL);
    }

    private static String cuerpo(String regimen) {
        return "{\"nombre\":\"EMISOR S.A.\",\"tipoIdentificacion\":\"02\"," + regimen
                + ",\"situacion\":{\"moroso\":\"NO\",\"omiso\":\"NO\",\"estado\":\"Inscrito\"},"
                + "\"actividades\":[{\"estado\":\"A\",\"tipo\":\"P\",\"codigo\":\"4711\",\"descripcion\":\"Comercio\"}]}";
    }

    private HaciendaConsultaDTO consultar(String regimenJson) {
        servidor.expect(requestTo(CONSULTA)).andRespond(withSuccess(cuerpo(regimenJson), MediaType.APPLICATION_JSON));
        return servicio.consultarContribuyente("3-101-123456");
    }

    @Test
    @DisplayName("regimen {codigo, descripcion} con código 1, 2, 3 y uno desconocido se lee como entero y texto")
    void leeCodigoYDescripcion() {
        HaciendaConsultaDTO uno = consultar("\"regimen\":{\"codigo\":1,\"descripcion\":\"Régimen General\"}");
        assertThat(uno.getRegimen().getCodigo()).isEqualTo(1);
        assertThat(uno.getRegimen().getDescripcion()).isEqualTo("Régimen General");
        servidor.reset();
        assertThat(consultar("\"regimen\":{\"codigo\":2,\"descripcion\":\"Régimen Simplificado\"}").getRegimen().getCodigo())
                .isEqualTo(2);
        servidor.reset();
        assertThat(consultar("\"regimen\":{\"codigo\":3,\"descripcion\":\"No contribuyente\"}").getRegimen().getCodigo())
                .isEqualTo(3);
        servidor.reset();
        assertThat(consultar("\"regimen\":{\"codigo\":9,\"descripcion\":\"Otro\"}").getRegimen().getCodigo()).isEqualTo(9);
    }

    @Test
    @DisplayName("la traducción: 1 GENERAL, 2 SIMPLIFICADO, cualquier otro código u ausente OTRO")
    void traduccionDelCodigo() {
        assertThat(VerificacionRegimenService.traducir(1)).isEqualTo("GENERAL");
        assertThat(VerificacionRegimenService.traducir(2)).isEqualTo("SIMPLIFICADO");
        assertThat(VerificacionRegimenService.traducir(3)).isEqualTo("OTRO");
        assertThat(VerificacionRegimenService.traducir(9)).isEqualTo("OTRO");
        assertThat(VerificacionRegimenService.traducir(null)).isEqualTo("OTRO");
    }

    @Test
    @DisplayName("sin el campo regimen la consulta es EXITOSA con regimen nulo (la verificación lo trata como SIN_RESPUESTA)")
    void sinCampoRegimen() {
        servidor.expect(requestTo(CONSULTA)).andRespond(withSuccess("{\"nombre\":\"X\",\"tipoIdentificacion\":\"02\"}",
                MediaType.APPLICATION_JSON));
        HaciendaConsultaDTO r = servicio.consultarContribuyente("3101123456");
        assertThat(r.getResultado()).isEqualTo(ResultadoConsulta.EXITOSA);
        assertThat(r.getRegimen()).isNull();
    }

    @Test
    @DisplayName("404: NO_ENCONTRADA; 500: ERROR")
    void noEncontradaYError() {
        servidor.expect(requestTo(CONSULTA)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(servicio.consultarContribuyente("3101123456").getResultado()).isEqualTo(ResultadoConsulta.NO_ENCONTRADA);
        servidor.reset();
        servidor.expect(requestTo(CONSULTA)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(servicio.consultarContribuyente("3101123456").getResultado()).isEqualTo(ResultadoConsulta.ERROR);
    }
}
