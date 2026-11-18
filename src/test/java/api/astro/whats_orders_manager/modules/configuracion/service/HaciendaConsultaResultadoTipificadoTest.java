package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO.ResultadoConsulta;
import api.astro.whats_orders_manager.modules.configuracion.service.impl.HaciendaConsultaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** T-038 criterio 13: consultarContribuyente distingue «no encontrada» de «API caída» con un campo tipificado. */
@DisplayName("T-038 — resultado tipificado de consultarContribuyente")
class HaciendaConsultaResultadoTipificadoTest {

    private static final String URL = "https://hacienda.test/fe/ae";
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

    @Test
    @DisplayName("200 con cuerpo: EXITOSA")
    void exitosa() {
        servidor.expect(requestTo(URL + "?identificacion=109990999")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"nombre\":\"JUAN PEREZ\",\"tipoIdentificacion\":\"01\"}", MediaType.APPLICATION_JSON));
        HaciendaConsultaDTO r = servicio.consultarContribuyente("1-0999-0999");
        assertThat(r.getResultado()).isEqualTo(ResultadoConsulta.EXITOSA);
        assertThat(r.getExitosa()).isTrue();
        assertThat(r.getNombre()).isEqualTo("JUAN PEREZ");
    }

    @Test
    @DisplayName("404: NO_ENCONTRADA")
    void noEncontrada() {
        servidor.expect(requestTo(URL + "?identificacion=109990999")).andRespond(withStatus(HttpStatus.NOT_FOUND));
        HaciendaConsultaDTO r = servicio.consultarContribuyente("109990999");
        assertThat(r.getResultado()).isEqualTo(ResultadoConsulta.NO_ENCONTRADA);
        assertThat(r.getExitosa()).isFalse();
    }

    @Test
    @DisplayName("500: ERROR (la API no está disponible, no es que no exista)")
    void errorDeServidor() {
        servidor.expect(requestTo(URL + "?identificacion=109990999")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(servicio.consultarContribuyente("109990999").getResultado()).isEqualTo(ResultadoConsulta.ERROR);
    }

    @Test
    @DisplayName("número en blanco: ERROR y sin llamada")
    void enBlanco() {
        assertThat(servicio.consultarContribuyente(" ").getResultado()).isEqualTo(ResultadoConsulta.ERROR);
        servidor.verify();
    }
}
