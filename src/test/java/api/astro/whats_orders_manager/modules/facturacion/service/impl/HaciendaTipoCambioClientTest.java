package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.dto.TasaDelDia;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.exception.ProveedorTipoCambioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * T-008 — Cliente del endpoint público de Hacienda (indicadores/tc) con HTTP simulado
 * (MockRestServiceServer, sin red).
 */
@DisplayName("HaciendaTipoCambioClient — consulta de tipo de cambio del día")
class HaciendaTipoCambioClientTest {

    static final String URL_BASE = "https://api.hacienda.go.cr/indicadores/tc";
    static final String RESPUESTA_DOLAR =
            "{\"venta\":{\"fecha\":\"2026-10-11\",\"valor\":455.6},\"compra\":{\"fecha\":\"2026-10-11\",\"valor\":449.86}}";

    private MockRestServiceServer server;
    private HaciendaTipoCambioClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HaciendaTipoCambioClient(builder.build(), URL_BASE);
    }

    @Test
    @DisplayName("USD: interpreta compra, venta y la fecha que devuelve la API")
    void dolar_respuestaValida() {
        server.expect(requestTo(URL_BASE + "/dolar")).andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(RESPUESTA_DOLAR, MediaType.APPLICATION_JSON));

        TasaDelDia tasa = client.obtenerTasaDelDia(MonedaFE.USD);

        assertThat(tasa.fecha()).isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(tasa.compra()).isEqualByComparingTo("449.86");
        assertThat(tasa.venta()).isEqualByComparingTo("455.6");
        assertThat(client.getFuente()).isEqualTo(FuenteTipoCambio.API_EXTERNA);
        server.verify();
    }

    @Test
    @DisplayName("HTTP 500 → ProveedorTipoCambioException")
    void http500() {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.obtenerTasaDelDia(MonedaFE.USD))
                .isInstanceOf(ProveedorTipoCambioException.class);
    }

    @Test
    @DisplayName("HTTP 2xx distinto de 200 → ProveedorTipoCambioException")
    void http204() {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThatThrownBy(() -> client.obtenerTasaDelDia(MonedaFE.USD))
                .isInstanceOf(ProveedorTipoCambioException.class);
    }

    @Test
    @DisplayName("timeout → ProveedorTipoCambioException")
    void timeout() {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.obtenerTasaDelDia(MonedaFE.USD))
                .isInstanceOf(ProveedorTipoCambioException.class);
    }

    @Test
    @DisplayName("JSON malformado → ProveedorTipoCambioException")
    void jsonMalformado() {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withSuccess("{\"venta\":{\"fecha\":", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.obtenerTasaDelDia(MonedaFE.USD))
                .isInstanceOf(ProveedorTipoCambioException.class);
    }

    @Test
    @DisplayName("JSON sin los campos esperados → ProveedorTipoCambioException")
    void jsonSinCampos() {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withSuccess("{\"otro\":1}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.obtenerTasaDelDia(MonedaFE.USD))
                .isInstanceOf(ProveedorTipoCambioException.class);
    }

    @Test
    @DisplayName("valor <= 0 → ProveedorTipoCambioException")
    void valorNoPositivo() {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withSuccess(
                "{\"venta\":{\"fecha\":\"2026-10-11\",\"valor\":0},\"compra\":{\"fecha\":\"2026-10-11\",\"valor\":449.86}}",
                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.obtenerTasaDelDia(MonedaFE.USD))
                .isInstanceOf(ProveedorTipoCambioException.class);
    }

    @Test
    @DisplayName("EUR (formato no verificado): acepta el mismo formato compra/venta que el dólar")
    void euro_formatoCompraVenta() {
        server.expect(requestTo(URL_BASE + "/euro")).andRespond(withSuccess(
                "{\"venta\":{\"fecha\":\"2026-10-11\",\"valor\":530.1},\"compra\":{\"fecha\":\"2026-10-11\",\"valor\":525.2}}",
                MediaType.APPLICATION_JSON));

        TasaDelDia tasa = client.obtenerTasaDelDia(MonedaFE.EUR);

        assertThat(tasa.compra()).isEqualByComparingTo("525.2");
        assertThat(tasa.venta()).isEqualByComparingTo("530.1");
    }

    @Test
    @DisplayName("EUR (formato no verificado): con {fecha, colones} usa colones como compra y venta")
    void euro_formatoColones() {
        server.expect(requestTo(URL_BASE + "/euro")).andRespond(withSuccess(
                "{\"fecha\":\"2026-10-11 00:00:00\",\"dolares\":1.16,\"colones\":528.4}",
                MediaType.APPLICATION_JSON));

        TasaDelDia tasa = client.obtenerTasaDelDia(MonedaFE.EUR);

        assertThat(tasa.fecha()).isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(tasa.compra()).isEqualByComparingTo("528.4");
        assertThat(tasa.venta()).isEqualByComparingTo("528.4");
    }

    @Test
    @DisplayName("CRC no es una moneda consultable → ProveedorTipoCambioException sin llamar a la API")
    void crcNoSoportada() {
        assertThatThrownBy(() -> client.obtenerTasaDelDia(MonedaFE.CRC))
                .isInstanceOf(ProveedorTipoCambioException.class);
        server.verify();
    }
}
