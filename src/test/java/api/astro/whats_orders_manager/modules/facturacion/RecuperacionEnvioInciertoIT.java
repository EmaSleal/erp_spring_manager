package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.scheduled.ComprobanteReintentosJob;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionSembrado;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ArticuloMaestroRepository;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.hamcrest.Matchers;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.HttpStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * T-045 (ADR-049, resuelve P-100) — recuperación automática del ENVIO_INCIERTO: el job horario lo consulta pasados 15
 * minutos (ambiente activo, contador por debajo del máximo) y adopta el estado real de Hacienda o lo pasa a
 * NO_TRANSMITIDO para que el job de reintentos lo reenvíe con la misma clave.
 *
 * <p>Dos configuraciones de Hacienda reales, el transporte HTTP simulado sobre el {@code haciendaRestTemplate} real y un
 * reloj adelantable. Base: MySQL 8 en Testcontainers (t045-it-mysql, tmpfs, sin volúmenes).
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "facturacion.hacienda.sondeo.espera=1h"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-045 — recuperación automática del ENVIO_INCIERTO por el job (MySQL)")
class RecuperacionEnvioInciertoIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t045")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t045-it-mysql"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("database/routines/stored_procedures.sql")),
                    "/docker-entrypoint-initdb.d/01-routines.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        RelojAjustable relojAjustable() {
            return new RelojAjustable();
        }
    }

    /** Clock del sistema desplazado por un offset que las pruebas pueden adelantar. */
    static class RelojAjustable extends Clock {
        private volatile Duration offset = Duration.ZERO;

        void adelantar(Duration d) {
            offset = offset.plus(d);
        }

        void reiniciar() {
            offset = Duration.ZERO;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.now().plus(offset);
        }
    }

    private static final String USUARIO = "admin.bootstrap@example.invalid";
    private static final String RUC = "3101234567";
    private static final String API = "/api/facturas/electronica/comprobantes/";
    private static final String MENSAJE_SANDBOX_EN_PRODUCCION =
            "El comprobante pertenece al ambiente SANDBOX y la configuración activa es PRODUCCION; no se envía";
    private static final String MENSAJE_PRODUCCION_EN_SANDBOX =
            "El comprobante pertenece al ambiente PRODUCCION y la configuración activa es SANDBOX; no se envía";
    private static final String REFERENCIA_SANDBOX_EN_PRODUCCION =
            "La factura se emitió en el ambiente SANDBOX; la configuración activa es PRODUCCION";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ArticuloMaestroRepository articuloMaestroRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ConfiguracionHaciendaRepository configuracionHaciendaRepository;
    @Autowired private ComprobanteReintentosJob reintentosJob;
    @Autowired private ComprobanteElectronicoService comprobanteService;
    @Autowired private EnvioHaciendaService envioService;
    @Autowired private RelojAjustable reloj;
    @Autowired @Qualifier("haciendaRestTemplate") private RestTemplate haciendaRestTemplate;
    @Autowired private org.springframework.cache.CacheManager cacheManager;
    @Autowired private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakers;

    private TransactionTemplate tx;
    private MockRestServiceServer hacienda;
    private Producto gravado13;
    private String xmlFirmadoReal;
    private Long idConfigSandbox;
    private Long idConfigProduccion;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            if (parametroRepository.findByClave(AsientoContableService.CLAVE_VENTA_CXC_CLIENTES).isEmpty()) {
                param(AsientoContableService.CLAVE_VENTA_CXC_CLIENTES, cuenta("9.1.02.001", "Clientes", TipoCuenta.ACTIVO));
                param(AsientoContableService.CLAVE_VENTA_INGRESOS, cuenta("9.4.01.001", "Ventas", TipoCuenta.INGRESO));
                param(AsientoContableService.CLAVE_VENTA_IVA_POR_PAGAR, cuenta("9.2.01.002", "IVA por pagar", TipoCuenta.PASIVO));
            }
            ConfiguracionEmpresa empresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc()
                    .orElseGet(() -> ConfiguracionEmpresa.builder().razonSocial("Empresa T045 S.A.").build());
            empresa.setNumeroIdentificacion(RUC);
            empresa.setTipoIdentificacion(TipoIdentificacion.CEDULA_JURIDICA);
            empresa.setCodigoActividad("4773.0");
            empresa.setCodigoProvincia("1");
            empresa.setCanton("01");
            empresa.setDistrito("01");
            empresa.setTelefono("22223333");
            empresa.setEmail("facturacion@t045.example");
            empresa.setOtrasSenas("Avenida Central, San José");
            CatalogoUbicacionSembrado.sembrar(jdbc);
            final ConfiguracionEmpresa guardada = empresaRepository.save(empresa);
            idConfigSandbox = configuracion(guardada, AmbienteHacienda.SANDBOX);
            idConfigProduccion = configuracion(guardada, AmbienteHacienda.PRODUCCION);
            gravado13 = producto("Producto A gravado 13 % T045", "1130.00");
        });
        // Lo que dejaron otras pruebas no debe verse como pendiente de este job: se deja como terminal
        jdbc.update("UPDATE comprobante_electronico SET estado = 'ACEPTADO' WHERE estado IN ('GENERADO', 'FIRMADO', "
                + "'ERROR', 'NO_TRANSMITIDO', 'ENVIADO', 'PROCESANDO', 'ERROR_HACIENDA', 'ENVIO_INCIERTO')");
        activar(AmbienteHacienda.SANDBOX);
        var cache = cacheManager.getCache("haciendaToken");
        if (cache != null) {
            cache.clear();
        }
        // Sin expectativas: cualquier llamada a Hacienda hace fallar la prueba
        hacienda = MockRestServiceServer.bindTo(haciendaRestTemplate).ignoreExpectOrder(true).build();
        cerrarCircuitBreaker();
    }

    /** Las consultas que fallan a propósito abren el circuit breaker de Hacienda: se cierra para no afectar a otras pruebas. */
    private void cerrarCircuitBreaker() {
        circuitBreakers.circuitBreaker("haciendaAPI").reset();
    }

    @AfterEach
    void tearDown() {
        cerrarCircuitBreaker();
        reloj.reiniciar();
        SecurityContextHolder.clearContext();
    }

    // ── Criterios 6 a 10 ─────────────────────────────────────────────────────

    private static final String CAUSA_YA_RECIBIDO = "El comprobante con la Clave [%s] ya fue recibido.";

    @Test
    @DisplayName("6: ENVIO_INCIERTO sin la etapa d → a +16 min el job consulta → 404 → NO_TRANSMITIDO → reintentos reenvía con la misma clave y consecutivo → ENVIADO (un POST)")
    void caidaTrasLaMarca_seRecuperaYSeReenvia() throws Exception {
        Long id = comprobanteEnviadoAhora("ENVIO_INCIERTO", AmbienteHacienda.SANDBOX);
        String clave = clave(id);
        String consecutivo = consecutivoDe(id);
        reloj.adelantar(Duration.ofMinutes(16));

        esperarToken();
        esperarConsulta(clave, withStatus(HttpStatus.NOT_FOUND));
        reintentosJob.consultarEstadosPendientes();
        hacienda.verify();
        assertThat(estado(id)).isEqualTo("NO_TRANSMITIDO");
        assertThat(contador(id)).as("salir de ENVIO_INCIERTO reinicia el contador").isZero();

        hacienda.reset();
        esperarToken();
        esperarPost(withStatus(HttpStatus.ACCEPTED));
        reintentosJob.reintentarEnviosPendientes();
        hacienda.verify();
        assertThat(estado(id)).as("ultimoError: %s", ultimoError(id)).isEqualTo("ENVIADO");
        assertThat(clave(id)).isEqualTo(clave);
        assertThat(consecutivoDe(id)).isEqualTo(consecutivo);
        assertThat(jdbc.queryForObject("SELECT intentos_envio FROM comprobante_electronico WHERE id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("7: ENVIADO + 404 en el sondeo → ENVIO_INCIERTO; no se vuelve a consultar antes de 15 min; a +16 min recuperación como en 6")
    void enviadoCon404_pasaAIncierto_yLuegoSeRecupera() throws Exception {
        Long id = comprobanteEnviadoAhora("ENVIADO", AmbienteHacienda.SANDBOX);
        enviadoHace(id, Duration.ofMinutes(6));
        String clave = clave(id);

        esperarToken();
        esperarConsulta(clave, withStatus(HttpStatus.NOT_FOUND));
        reintentosJob.consultarEstadosPendientes();
        hacienda.verify();
        assertThat(estado(id)).isEqualTo("ENVIO_INCIERTO");
        assertThat(contador(id)).as("el paso de ENVIADO a ENVIO_INCIERTO no cuenta como consulta sin resolver").isZero();

        // Pasaron 6 minutos desde el envío: todavía no se consulta (sin expectativas, cualquier llamada falla)
        hacienda.reset();
        reintentosJob.consultarEstadosPendientes();
        hacienda.verify();
        assertThat(estado(id)).isEqualTo("ENVIO_INCIERTO");

        reloj.adelantar(Duration.ofMinutes(10));
        esperarToken();
        esperarConsulta(clave, withStatus(HttpStatus.NOT_FOUND));
        reintentosJob.consultarEstadosPendientes();
        hacienda.verify();
        assertThat(estado(id)).isEqualTo("NO_TRANSMITIDO");

        hacienda.reset();
        esperarToken();
        esperarPost(withStatus(HttpStatus.ACCEPTED));
        reintentosJob.reintentarEnviosPendientes();
        hacienda.verify();
        assertThat(estado(id)).as("ultimoError: %s", ultimoError(id)).isEqualTo("ENVIADO");
        assertThat(clave(id)).isEqualTo(clave);
    }

    @Test
    @DisplayName("8: Hacienda sí lo tiene: ind-estado aceptado → ACEPTADO y rechazado → RECHAZADO, sin POST; el contador vuelve a 0")
    void haciendaLoTiene_seAdoptaSuEstado() throws Exception {
        Long aceptado = comprobanteEnviadoAhora("ENVIO_INCIERTO", AmbienteHacienda.SANDBOX);
        Long rechazado = comprobanteEnviadoAhora("ENVIO_INCIERTO", AmbienteHacienda.SANDBOX);
        jdbc.update("UPDATE comprobante_electronico SET consultas_error_hacienda = 3 WHERE id IN (?, ?)", aceptado, rechazado);
        reloj.adelantar(Duration.ofMinutes(16));

        esperarToken();
        esperarConsulta(clave(aceptado), withSuccess("{\"ind-estado\":\"aceptado\"}", MediaType.APPLICATION_JSON));
        esperarConsulta(clave(rechazado), withSuccess("{\"ind-estado\":\"rechazado\"}", MediaType.APPLICATION_JSON));
        reintentosJob.consultarEstadosPendientes();
        hacienda.verify(); // ningún POST esperado: uno haría fallar la prueba
        assertThat(estado(aceptado)).isEqualTo("ACEPTADO");
        assertThat(estado(rechazado)).isEqualTo("RECHAZADO");
        assertThat(contador(aceptado)).isZero();
        assertThat(contador(rechazado)).isZero();
    }

    @Test
    @DisplayName("9: «ya recibido»: el reenvío vuelve a ENVIO_INCIERTO (un POST); la consulta siguiente adopta el estado real")
    void yaRecibido_laSiguienteConsultaAdoptaElEstadoReal() throws Exception {
        Long id = comprobanteEnviadoAhora("ENVIO_INCIERTO", AmbienteHacienda.SANDBOX);
        String clave = clave(id);
        reloj.adelantar(Duration.ofMinutes(16));

        esperarToken();
        esperarConsulta(clave, withStatus(HttpStatus.NOT_FOUND));
        reintentosJob.consultarEstadosPendientes();
        hacienda.verify();
        assertThat(estado(id)).isEqualTo("NO_TRANSMITIDO");

        hacienda.reset();
        esperarToken();
        esperarPost(yaRecibido(clave));
        reintentosJob.reintentarEnviosPendientes();
        hacienda.verify();
        assertThat(estado(id)).as("ultimoError: %s", ultimoError(id)).isEqualTo("ENVIO_INCIERTO");

        // Todavía no pasaron 15 min desde este envío: ninguna llamada
        hacienda.reset();
        reintentosJob.consultarEstadosPendientes();
        hacienda.verify();

        reloj.adelantar(Duration.ofMinutes(16));
        esperarToken();
        esperarConsulta(clave, withSuccess("{\"ind-estado\":\"aceptado\"}", MediaType.APPLICATION_JSON));
        reintentosJob.consultarEstadosPendientes();
        hacienda.verify();
        assertThat(estado(id)).isEqualTo("ACEPTADO");
    }

    @Test
    @DisplayName("9: el ciclo 404 → reenvío → «ya recibido» está acotado por los 5 intentos: a lo sumo 5 POST y luego nada se consulta ni envía")
    void yaRecibido_cicloAcotadoPorLosIntentos() throws Exception {
        Long id = comprobanteEnviadoAhora("ENVIO_INCIERTO", AmbienteHacienda.SANDBOX);
        String clave = clave(id);
        int posts = 0;
        for (int ciclo = 1; ciclo <= 7; ciclo++) {
            reloj.adelantar(Duration.ofMinutes(16));
            boolean seConsulta = ciclo <= 6;
            boolean seEnvia = ciclo <= 5;
            hacienda.reset();
            if (seConsulta) {
                esperarToken();
                esperarConsulta(clave, withStatus(HttpStatus.NOT_FOUND));
            }
            reintentosJob.consultarEstadosPendientes();
            hacienda.verify();
            hacienda.reset();
            if (seEnvia) {
                esperarToken();
                esperarPost(yaRecibido(clave));
                posts++;
            }
            reintentosJob.reintentarEnviosPendientes();
            hacienda.verify();
        }
        assertThat(posts).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT intentos_envio FROM comprobante_electronico WHERE id = ?", Integer.class, id))
                .isEqualTo(5);
        // Tras el 5.º POST «ya recibido» el comprobante sigue en ENVIO_INCIERTO; el ciclo 6 lo consulta (404 → NO_TRANSMITIDO)
        // y el job de reintentos lo deja por el límite de intentos, que el ciclo 7 ya no consulta
        assertThat(estado(id)).isEqualTo("NO_TRANSMITIDO");
    }

    @Test
    @DisplayName("10: antes de 15 min el job no consulta el ENVIO_INCIERTO; el de otro ambiente (T-044) tampoco, ni a los 2 días")
    void noSeConsulta_antesDe15Min_niDeOtroAmbiente() throws Exception {
        Long reciente = comprobanteEnviadoAhora("ENVIO_INCIERTO", AmbienteHacienda.SANDBOX);
        Long deOtroAmbiente = comprobanteEnviadoAhora("ENVIO_INCIERTO", AmbienteHacienda.PRODUCCION);
        enviadoHace(deOtroAmbiente, Duration.ofDays(2));
        reloj.adelantar(Duration.ofMinutes(14));
        Map<String, Object> antesReciente = fila(reciente);
        Map<String, Object> antesOtro = fila(deOtroAmbiente);

        reintentosJob.consultarEstadosPendientes(); // sin expectativas: una llamada haría fallar la prueba
        hacienda.verify();

        assertThat(fila(reciente)).isEqualTo(antesReciente);
        assertThat(fila(deOtroAmbiente)).isEqualTo(antesOtro);
        assertThat(contador(reciente)).isZero();
        assertThat(contador(deOtroAmbiente)).isZero();
    }

    @Test
    @DisplayName("4 y 10: cada consulta que falla cuenta; al llegar al máximo el job deja de consultar y lo registra; la consulta manual lo reactiva")
    void tope_dejaDeConsultar_yLaManualLoReactiva() throws Exception {
        Long id = comprobanteEnviadoAhora("ENVIO_INCIERTO", AmbienteHacienda.SANDBOX);
        String clave = clave(id);
        reloj.adelantar(Duration.ofMinutes(16));
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> log =
                new ch.qos.logback.core.read.ListAppender<>();
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl.class);
        log.start();
        logger.addAppender(log);
        try {
            for (int i = 1; i <= 5; i++) {
                hacienda.reset();
                cerrarCircuitBreaker();
                esperarToken();
                esperarConsulta(clave, withServerError());
                reintentosJob.consultarEstadosPendientes();
                hacienda.verify();
                assertThat(contador(id)).as("tras la consulta %s", i).isEqualTo(i);
                assertThat(estado(id)).isEqualTo("ENVIO_INCIERTO");
            }
            assertThat(log.list.stream().map(e -> e.getFormattedMessage()).filter(m -> m.contains("se deja de consultar")))
                    .hasSize(1);

            cerrarCircuitBreaker();
            hacienda.reset(); // en el tope ya no se consulta: sin expectativas
            reintentosJob.consultarEstadosPendientes();
            hacienda.verify();
            assertThat(contador(id)).isEqualTo(5);

            // Consulta manual: reinicia el contador (la respuesta sin estado lo deja en ENVIO_INCIERTO) y vuelve al sondeo
            hacienda.reset();
            esperarToken();
            esperarConsulta(clave, withSuccess("{\"ind-estado\":\"desconocido\"}", MediaType.APPLICATION_JSON));
            mockMvc.perform(post(API + id + "/consultar-estado-hacienda").with(user(USUARIO).roles("ADMIN")).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comprobante.estado").value("ENVIO_INCIERTO"));
            hacienda.verify();
            assertThat(contador(id)).isZero();

            hacienda.reset();
            cerrarCircuitBreaker();
            esperarToken();
            esperarConsulta(clave, withServerError());
            reintentosJob.consultarEstadosPendientes();
            hacienda.verify();
            assertThat(contador(id)).as("el job lo vuelve a consultar y lo cuenta").isEqualTo(1);
        } finally {
            logger.detachAppender(log);
        }
    }

    @Test
    @DisplayName("4: una respuesta sin ind-estado clasificable deja el ENVIO_INCIERTO y cuenta como consulta sin resolver")
    void respuestaSinEstado_cuenta() throws Exception {
        Long id = comprobanteEnviadoAhora("ENVIO_INCIERTO", AmbienteHacienda.SANDBOX);
        reloj.adelantar(Duration.ofMinutes(16));
        for (int i = 1; i <= 2; i++) {
            hacienda.reset();
            esperarToken();
            esperarConsulta(clave(id), withSuccess("{\"ind-estado\":\"desconocido\"}", MediaType.APPLICATION_JSON));
            reintentosJob.consultarEstadosPendientes();
            hacienda.verify();
            assertThat(estado(id)).isEqualTo("ENVIO_INCIERTO");
            assertThat(contador(id)).isEqualTo(i);
        }
    }

    // ── Apoyo de las pruebas ─────────────────────────────────────────────────

    private void esperarConsulta(String clave, org.springframework.test.web.client.ResponseCreator respuesta) {
        hacienda.expect(ExpectedCount.once(), requestTo(Matchers.containsString("/recepcion/v1/recepcion/" + clave)))
                .andExpect(method(HttpMethod.GET)).andRespond(respuesta);
    }

    private void esperarPost(org.springframework.test.web.client.ResponseCreator respuesta) {
        hacienda.expect(ExpectedCount.once(), requestTo(Matchers.endsWith("/recepcion/v1/recepcion")))
                .andExpect(method(HttpMethod.POST)).andRespond(respuesta);
    }

    private static org.springframework.test.web.client.ResponseCreator yaRecibido(String clave) {
        return withStatus(HttpStatus.BAD_REQUEST).header("X-Error-Cause", String.format(CAUSA_YA_RECIBIDO, clave));
    }

    private static org.springframework.test.web.client.ResponseCreator withServerError() {
        return withStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /** fecha_envio en la hora del reloj de la aplicación (la base usa otra zona horaria que NOW()). */
    private void enviadoHace(Long id, Duration hace) {
        jdbc.update("UPDATE comprobante_electronico SET fecha_envio = ? WHERE id = ?",
                java.time.LocalDateTime.now(reloj).minus(hace), id);
    }

    /** Comprobante nuevo en el estado y ambiente indicados, con el último envío en este instante (del reloj). */
    private Long comprobanteEnviadoAhora(String estado, AmbienteHacienda ambiente) throws Exception {
        Long id = comprobanteEn(estado, ambiente);
        enviadoHace(id, Duration.ZERO);
        return id;
    }

    private int contador(Long id) {
        return jdbc.queryForObject("SELECT consultas_error_hacienda FROM comprobante_electronico WHERE id = ?",
                Integer.class, id);
    }

    private String consecutivoDe(Long id) {
        return jdbc.queryForObject("SELECT consecutivo FROM comprobante_electronico WHERE id = ?", String.class, id);
    }

    // ── Fixture ──────────────────────────────────────────────────────────────

    private api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository
            comprobanteRepository;

    @Autowired
    void inyectarRepositorio(api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository r) {
        this.comprobanteRepository = r;
    }

    private api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository
            comprobanteRepository() {
        return comprobanteRepository;
    }

    /** Id de la configuración de ese ambiente para la empresa; la crea (inactiva) si no existe. */
    private Long configuracion(ConfiguracionEmpresa empresa, AmbienteHacienda ambiente) {
        return configuracionHaciendaRepository.findAll().stream().filter(c -> c.getAmbiente() == ambiente).findFirst()
                .orElseGet(() -> configuracionHaciendaRepository.save(ConfiguracionHacienda.builder().empresa(empresa)
                        .ambiente(ambiente)
                        .usuarioHacienda("cpj-3-101-234567@" + (ambiente == AmbienteHacienda.SANDBOX ? "stag." : "")
                                + "comprobanteselectronicos.go.cr")
                        .passwordHacienda("clave-de-prueba").activa(false)
                        .rutaCertificado(CertificadoDePrueba.ruta()).pinCertificado(CertificadoDePrueba.PIN).build()))
                .getId();
    }

    /** Deja activa solo la configuración del ambiente indicado, como lo haría el administrador. */
    private void activar(AmbienteHacienda ambiente) {
        Long id = ambiente == AmbienteHacienda.SANDBOX ? idConfigSandbox : idConfigProduccion;
        jdbc.update("UPDATE configuracion_hacienda SET activa = (id = ?)", id);
        var cache = cacheManager.getCache("haciendaToken");
        if (cache != null) {
            cache.clear();
        }
    }

    private long consecutivo(Long idConfiguracion, String columna) {
        return jdbc.queryForObject("SELECT " + columna + " FROM configuracion_hacienda WHERE id = ?", Long.class,
                idConfiguracion);
    }

    private void esperarToken() {
        // El token queda en caché: puede no pedirse de nuevo tras reiniciar el servidor simulado
        hacienda.expect(ExpectedCount.between(0, 100), requestTo(Matchers.containsString("/openid-connect/token")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok-de-prueba\",\"refresh_token\":\"r\","
                        + "\"expires_in\":300,\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));
    }

    /** Lo que la regla no debe tocar: estado, intentos, ultimo_error y fecha_envio. */
    private Map<String, Object> fila(Long id) {
        return jdbc.queryForMap("SELECT estado, intentos_envio, ultimo_error, fecha_envio, ambiente, vigente "
                + "FROM comprobante_electronico WHERE id = ?", id);
    }

    private MvcResult emitirNota(int idFactura) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idFactura", idFactura);
        cuerpo.put("codigoReferencia", "01");
        cuerpo.put("razon", "Anulación total T-044");
        return mockMvc.perform(post("/api/facturas/notas-credito").with(user(USUARIO).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(cuerpo))).andReturn();
    }

    private MvcResult sustituta(int idFactura) throws Exception {
        return mockMvc.perform(post("/api/facturas/" + idFactura + "/sustituta").with(user(USUARIO).roles("ADMIN"))
                .with(csrf())).andReturn();
    }

    private JsonNode detalleFactura(int idFactura) throws Exception {
        MvcResult r = mockMvc.perform(get("/facturas/detalle/" + idFactura).with(user(USUARIO).roles("ADMIN")))
                .andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    /** Factura nueva con su comprobante vigente ACEPTADO en el ambiente indicado. */
    private int facturaAceptada(AmbienteHacienda ambiente) throws Exception {
        int idFactura = facturaConLinea();
        insertarComprobante(idFactura, "ACEPTADO", ambiente);
        return idFactura;
    }

    /** Comprobante vigente de una factura nueva, con el XML firmado real, en el estado y ambiente indicados. */
    private Long comprobanteEn(String estado, AmbienteHacienda ambiente) throws Exception {
        int idFactura = facturaConLinea();
        insertarComprobante(idFactura, estado, ambiente);
        return jdbc.queryForObject("SELECT MAX(id) FROM comprobante_electronico WHERE factura_id = ?", Long.class,
                idFactura);
    }

    private void insertarComprobante(int idFactura, String estado, AmbienteHacienda ambiente) throws Exception {
        jdbc.update("INSERT INTO comprobante_electronico (clave_numerica, consecutivo, created_at, updated_at, estado, "
                        + "fecha_emision, fecha_envio, tipo_comprobante, empresa_id, factura_id, vigente, intentos_envio, "
                        + "enviado_email, xml_comprobante, ruta_archivo_xml, ambiente) "
                        + "VALUES (?, ?, NOW(6), NOW(6), ?, NOW(6), NOW(6), 'FACTURA_ELECTRONICA', "
                        + "(SELECT id_configuracion FROM configuracion_empresa ORDER BY id_configuracion LIMIT 1), "
                        + "?, b'1', 0, b'0', ?, ?, ?)",
                claveNueva(), consecutivoNuevo(), estado, idFactura, xmlFirmadoReal(), "comprobantes/t045.xml",
                ambiente.name());
    }

    private static String claveNueva() {
        StringBuilder clave = new StringBuilder("506");
        while (clave.length() < 50) {
            clave.append(java.util.concurrent.ThreadLocalRandom.current().nextInt(10));
        }
        return clave.toString();
    }

    private static String consecutivoNuevo() {
        return String.format("%020d", java.util.concurrent.ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L));
    }

    private String clave(Long id) {
        return jdbc.queryForObject("SELECT clave_numerica FROM comprobante_electronico WHERE id = ?", String.class, id);
    }

    private String estado(Long id) {
        return jdbc.queryForObject("SELECT estado FROM comprobante_electronico WHERE id = ?", String.class, id);
    }

    private String ultimoError(Long id) {
        return jdbc.queryForObject("SELECT ultimo_error FROM comprobante_electronico WHERE id = ?", String.class, id);
    }

    /** XML firmado producido por el flujo real (factura persistida → generador → firma) y reutilizado en las filas. */
    private String xmlFirmadoReal() throws Exception {
        if (xmlFirmadoReal == null) {
            AmbienteHacienda activaAntes = jdbc.queryForObject("SELECT ambiente FROM configuracion_hacienda WHERE activa = b'1'",
                    String.class).equals("SANDBOX") ? AmbienteHacienda.SANDBOX : AmbienteHacienda.PRODUCCION;
            activar(AmbienteHacienda.SANDBOX);
            int idFactura = facturaConLinea();
            autenticar();
            long fSandbox = consecutivo(idConfigSandbox, "consecutivo_factura");
            Long id = comprobanteService.generarDesdeFactura((long) idFactura).getId();
            comprobanteService.firmar(id);
            xmlFirmadoReal = jdbc.queryForObject("SELECT xml_comprobante FROM comprobante_electronico WHERE id = ?",
                    String.class, id);
            jdbc.update("DELETE FROM comprobante_electronico WHERE id = ?", id);
            jdbc.update("UPDATE configuracion_hacienda SET consecutivo_factura = ? WHERE id = ?", fSandbox, idConfigSandbox);
            SecurityContextHolder.clearContext();
            activar(activaAntes);
        }
        return xmlFirmadoReal;
    }

    private void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(USUARIO, null,
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    private int facturaConLinea() throws Exception {
        int idFactura = tx.execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T045 " + UUID.randomUUID());
            cliente.setTipoIdentificacion(TipoIdentificacion.CEDULA_FISICA);
            cliente.setNumeroIdentificacion("109990999");
            cliente.setTelefono("88887777");
            cliente.setEmail("cliente@t045.example");
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T045-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T045");
            factura.setMonedaFE(MonedaFE.CRC);
            factura.setTipoCambio(BigDecimal.ONE);
            factura.setSubtotal(BigDecimal.ZERO);
            factura.setIgv(BigDecimal.ZERO);
            factura.setTotal(BigDecimal.ZERO);
            factura.setAnulada(false);
            return facturaRepository.save(factura).getIdFactura();
        });
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("id_factura", idFactura);
        linea.put("id_linea_factura", -1);
        linea.put("numero_linea", 1);
        linea.put("id_producto", gravado13.getIdProducto());
        linea.put("cantidad", 1);
        linea.put("precioUnitario", new BigDecimal("1130.00"));
        linea.put("subtotal", new BigDecimal("1130.00"));
        int status = mockMvc.perform(put("/lineas-factura/actualizar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(linea))))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);
        return idFactura;
    }

    @SuppressWarnings("deprecation")
    private Producto producto(String descripcion, String precio) {
        Producto p = new Producto();
        p.setCodigo("T45-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDescripcion(descripcion);
        ArticuloMaestro maestro = articuloMaestroRepository.findByDescripcionContainingIgnoreCase(descripcion).stream()
                .filter(m -> descripcion.equals(m.getDescripcion())).findFirst().orElseGet(ArticuloMaestro::new);
        maestro.setDescripcion(descripcion);
        maestro.setCodigoCabys("1234567890123");
        p.setArticuloMaestro(articuloMaestroRepository.save(maestro));
        p.setPrecioInstitucional(new BigDecimal(precio));
        p.setPrecioMayorista(new BigDecimal(precio));
        p.setActive(true);
        p.setGravado(true);
        p.setPorcentajeImpuesto(new BigDecimal("13"));
        p.setPrecioIncluyeIva(true);
        return productoRepository.save(p);
    }

    private CuentaContable cuenta(String codigo, String nombre, TipoCuenta tipo) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setNaturaleza(NaturalezaCuenta.fromTipoCuenta(tipo));
        c.setNivel(codigo.split("\\.").length);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return cuentaRepository.save(c);
    }

    private void param(String clave, CuentaContable cuenta) {
        parametroRepository.save(ParametroContable.builder().clave(clave).cuentaContable(cuenta).build());
    }
}
