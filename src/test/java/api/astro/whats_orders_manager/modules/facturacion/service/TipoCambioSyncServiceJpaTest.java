package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.ResultadoSincronizacionTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.dto.TasaDelDia;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoSincronizacionTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.exception.ProveedorTipoCambioException;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.repository.TipoCambioRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.impl.HaciendaTipoCambioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * T-008 — Sincronización diaria del tipo de cambio contra un proveedor JPA real (H2).
 * La API de Hacienda se simula con MockRestServiceServer (sin red).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("TipoCambioSyncService — sincronización con la API (H2)")
class TipoCambioSyncServiceJpaTest {

    private static final String URL_BASE = "https://api.hacienda.go.cr/indicadores/tc";
    private static final String RESPUESTA_DOLAR =
            "{\"venta\":{\"fecha\":\"2026-10-11\",\"valor\":455.6},\"compra\":{\"fecha\":\"2026-10-11\",\"valor\":449.86}}";
    private static final LocalDate DIA = LocalDate.of(2026, 10, 11);

    @Autowired
    private TipoCambioRepository repo;

    @Autowired
    private TestEntityManager em;

    private TipoCambioService tipoCambioService;
    private MockRestServiceServer server;
    private TipoCambioSyncService syncConHacienda;

    @BeforeEach
    void setUp() {
        tipoCambioService = new TipoCambioService(repo);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        syncConHacienda = new TipoCambioSyncService(
                new HaciendaTipoCambioClient(builder.build(), URL_BASE), tipoCambioService, "USD");
    }

    @Test
    @DisplayName("criterio 2: con la respuesta de ejemplo guarda USD/CRC 2026-10-11 449.86/455.6 API_EXTERNA activo, y obtenerTasaVenta la encuentra")
    void respuestaEjemplo_guardaFilaYEsEncontradaPorObtenerTasaVenta() {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withSuccess(RESPUESTA_DOLAR, MediaType.APPLICATION_JSON));

        List<ResultadoSincronizacionTipoCambio> resultados = syncConHacienda.sincronizar();
        sincronizarConBd();

        assertThat(resultados).singleElement()
                .extracting(ResultadoSincronizacionTipoCambio::estado).isEqualTo(EstadoSincronizacionTipoCambio.CREADO);
        assertThat(repo.findAll()).singleElement().satisfies(tc -> {
            assertThat(tc.getMonedaOrigen()).isEqualTo(MonedaFE.CRC);
            assertThat(tc.getMonedaDestino()).isEqualTo(MonedaFE.USD);
            assertThat(tc.getFecha()).isEqualTo(DIA);
            assertThat(tc.getTasaCompra()).isEqualByComparingTo("449.86");
            assertThat(tc.getTasaVenta()).isEqualByComparingTo("455.6");
            assertThat(tc.getFuente()).isEqualTo(FuenteTipoCambio.API_EXTERNA);
            assertThat(tc.isActivo()).isTrue();
        });
        // T-007: la consulta por fecha de las operaciones encuentra la tasa sincronizada
        assertThat(tipoCambioService.obtenerTasaVenta(MonedaFE.USD, DIA)).isEqualByComparingTo("455.6");
        assertThat(tipoCambioService.obtenerTasaVenta(MonedaFE.USD, DIA.plusDays(1))).isEqualByComparingTo("455.6");
        server.verify();
    }

    @Test
    @DisplayName("criterio 3: una tasa MANUAL activa del mismo día y moneda no se modifica ni se desactiva")
    void manualActiva_noSeReemplaza() {
        TipoCambio manual = persistir(FuenteTipoCambio.MANUAL, "440.00", "450.00");
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withSuccess(RESPUESTA_DOLAR, MediaType.APPLICATION_JSON));

        List<ResultadoSincronizacionTipoCambio> resultados = syncConHacienda.sincronizar();
        sincronizarConBd();

        assertThat(resultados).singleElement()
                .extracting(ResultadoSincronizacionTipoCambio::estado).isEqualTo(EstadoSincronizacionTipoCambio.CONSERVADO);
        assertThat(repo.findAll()).singleElement().satisfies(tc -> {
            assertThat(tc.getId()).isEqualTo(manual.getId());
            assertThat(tc.getFuente()).isEqualTo(FuenteTipoCambio.MANUAL);
            assertThat(tc.getTasaCompra()).isEqualByComparingTo("440.00");
            assertThat(tc.getTasaVenta()).isEqualByComparingTo("450.00");
            assertThat(tc.isActivo()).isTrue();
        });
    }

    @Test
    @DisplayName("criterio 4: segunda corrida del mismo día con el mismo valor no crea filas")
    void segundaCorridaMismoValor_noCreaFilas() {
        TipoCambioSyncService sync = syncCon(
                () -> new TasaDelDia(DIA, new BigDecimal("449.86"), new BigDecimal("455.6")),
                () -> new TasaDelDia(DIA, new BigDecimal("449.860"), new BigDecimal("455.60")));

        sync.sincronizar();
        sincronizarConBd();
        List<ResultadoSincronizacionTipoCambio> segunda = sync.sincronizar();
        sincronizarConBd();

        assertThat(segunda).singleElement()
                .extracting(ResultadoSincronizacionTipoCambio::estado).isEqualTo(EstadoSincronizacionTipoCambio.SIN_CAMBIOS);
        assertThat(repo.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("criterio 4: segunda corrida del mismo día con otro valor actualiza la fila API_EXTERNA")
    void segundaCorridaOtroValor_actualizaFila() {
        TipoCambioSyncService sync = syncCon(
                () -> new TasaDelDia(DIA, new BigDecimal("449.86"), new BigDecimal("455.6")),
                () -> new TasaDelDia(DIA, new BigDecimal("450.10"), new BigDecimal("456.20")));

        sync.sincronizar();
        sincronizarConBd();
        Long id = repo.findAll().get(0).getId();
        List<ResultadoSincronizacionTipoCambio> segunda = sync.sincronizar();
        sincronizarConBd();

        assertThat(segunda).singleElement()
                .extracting(ResultadoSincronizacionTipoCambio::estado).isEqualTo(EstadoSincronizacionTipoCambio.ACTUALIZADO);
        assertThat(repo.findAll()).singleElement().satisfies(tc -> {
            assertThat(tc.getId()).isEqualTo(id);
            assertThat(tc.getTasaCompra()).isEqualByComparingTo("450.10");
            assertThat(tc.getTasaVenta()).isEqualByComparingTo("456.20");
            assertThat(tc.getFuente()).isEqualTo(FuenteTipoCambio.API_EXTERNA);
            assertThat(tc.isActivo()).isTrue();
        });
    }

    @Test
    @DisplayName("criterio 5: HTTP 500 → no escribe nada, WARN y sin excepción")
    void http500_noEscribe(CapturedOutput output) {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertFalloSinEscritura(output);
    }

    @Test
    @DisplayName("criterio 5: timeout → no escribe nada, WARN y sin excepción")
    void timeout_noEscribe(CapturedOutput output) {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withException(new SocketTimeoutException("Read timed out")));
        assertFalloSinEscritura(output);
    }

    @Test
    @DisplayName("criterio 5: JSON malformado → no escribe nada, WARN y sin excepción")
    void jsonMalformado_noEscribe(CapturedOutput output) {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withSuccess("no-es-json", MediaType.APPLICATION_JSON));
        assertFalloSinEscritura(output);
    }

    @Test
    @DisplayName("criterio 5: valor <= 0 → no escribe nada, WARN y sin excepción")
    void valorNoPositivo_noEscribe(CapturedOutput output) {
        server.expect(requestTo(URL_BASE + "/dolar")).andRespond(withSuccess(
                "{\"venta\":{\"fecha\":\"2026-10-11\",\"valor\":-1},\"compra\":{\"fecha\":\"2026-10-11\",\"valor\":449.86}}",
                MediaType.APPLICATION_JSON));
        assertFalloSinEscritura(output);
    }

    @Test
    @DisplayName("el fallo de una moneda no impide sincronizar las demás configuradas")
    void falloDeUnaMoneda_noDetieneLasDemas() {
        ProveedorFalso proveedor = new ProveedorFalso();
        proveedor.respuestas.add(() -> { throw new ProveedorTipoCambioException("USD caído"); });
        proveedor.respuestas.add(() -> new TasaDelDia(DIA, new BigDecimal("525.2"), new BigDecimal("530.1")));
        TipoCambioSyncService sync = new TipoCambioSyncService(proveedor, tipoCambioService, "USD, EUR");

        List<ResultadoSincronizacionTipoCambio> resultados = sync.sincronizar();
        sincronizarConBd();

        assertThat(resultados).extracting(ResultadoSincronizacionTipoCambio::estado)
                .containsExactly(EstadoSincronizacionTipoCambio.ERROR, EstadoSincronizacionTipoCambio.CREADO);
        assertThat(repo.findAll()).singleElement()
                .extracting(TipoCambio::getMonedaDestino).isEqualTo(MonedaFE.EUR);
    }

    @Test
    @DisplayName("por defecto (propiedad vacía) sincroniza solo USD; monedas inválidas o CRC se ignoran")
    void monedasConfiguradas() {
        assertThat(new TipoCambioSyncService(new ProveedorFalso(), tipoCambioService, "").getMonedas())
                .containsExactly(MonedaFE.USD);
        assertThat(new TipoCambioSyncService(new ProveedorFalso(), tipoCambioService, "usd,XXX,CRC,EUR,USD").getMonedas())
                .containsExactly(MonedaFE.USD, MonedaFE.EUR);
    }

    private void assertFalloSinEscritura(CapturedOutput output) {
        List<ResultadoSincronizacionTipoCambio> resultados = assertSinExcepcion();
        sincronizarConBd();

        assertThat(resultados).singleElement()
                .extracting(ResultadoSincronizacionTipoCambio::estado).isEqualTo(EstadoSincronizacionTipoCambio.ERROR);
        assertThat(repo.count()).isZero();
        assertThat(output.getOut()).contains("WARN").contains("No se pudo sincronizar el tipo de cambio de USD");
    }

    private List<ResultadoSincronizacionTipoCambio> assertSinExcepcion() {
        @SuppressWarnings("unchecked")
        List<ResultadoSincronizacionTipoCambio>[] holder = new List[1];
        assertThatCode(() -> holder[0] = syncConHacienda.sincronizar()).doesNotThrowAnyException();
        return holder[0];
    }

    @SafeVarargs
    private TipoCambioSyncService syncCon(Supplier<TasaDelDia>... respuestas) {
        ProveedorFalso proveedor = new ProveedorFalso();
        proveedor.respuestas.addAll(List.of(respuestas));
        return new TipoCambioSyncService(proveedor, tipoCambioService, "USD");
    }

    private TipoCambio persistir(FuenteTipoCambio fuente, String compra, String venta) {
        TipoCambio tc = em.persistAndFlush(TipoCambio.builder()
                .monedaOrigen(MonedaFE.CRC)
                .monedaDestino(MonedaFE.USD)
                .fecha(DIA)
                .tasaCompra(new BigDecimal(compra))
                .tasaVenta(new BigDecimal(venta))
                .fuente(fuente)
                .activo(true)
                .build());
        em.clear();
        return tc;
    }

    private void sincronizarConBd() {
        em.flush();
        em.clear();
    }

    /** Proveedor simulado que entrega respuestas en orden. */
    private static class ProveedorFalso implements ProveedorTipoCambio {
        final Deque<Supplier<TasaDelDia>> respuestas = new ArrayDeque<>();

        @Override
        public TasaDelDia obtenerTasaDelDia(MonedaFE moneda) {
            return respuestas.removeFirst().get();
        }

        @Override
        public FuenteTipoCambio getFuente() {
            return FuenteTipoCambio.API_EXTERNA;
        }
    }
}
