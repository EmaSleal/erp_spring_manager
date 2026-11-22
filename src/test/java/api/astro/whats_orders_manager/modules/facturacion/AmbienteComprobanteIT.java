package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
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

import java.math.BigDecimal;
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
 * T-044 (P-099) — un comprobante no se envía, consulta ni referencia en un ambiente distinto del suyo.
 *
 * <p>Dos configuraciones de Hacienda reales (SANDBOX y PRODUCCION, una activa; la activa se cambia por SQL, como lo haría
 * un administrador) y el transporte HTTP simulado sobre el {@code haciendaRestTemplate} real: sin expectativas, cualquier
 * llamada a Hacienda hace fallar la prueba. El generador, la firma y el XSD son los reales.
 *
 * <p>Base: MySQL 8 en Testcontainers (t044-it-mysql, tmpfs, sin volúmenes).
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "facturacion.hacienda.sondeo.espera=1h"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-044 — ambiente del comprobante: no enviar, consultar ni referenciar el de otro ambiente (MySQL)")
class AmbienteComprobanteIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t044", "t044-it-mysql");

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
                    .orElseGet(() -> ConfiguracionEmpresa.builder().razonSocial("Empresa T044 S.A.").build());
            empresa.setNumeroIdentificacion(RUC);
            empresa.setTipoIdentificacion(TipoIdentificacion.CEDULA_JURIDICA);
            empresa.setCodigoActividad("4773.0");
            empresa.setCodigoProvincia("1");
            empresa.setCanton("01");
            empresa.setDistrito("01");
            empresa.setTelefono("22223333");
            empresa.setEmail("facturacion@t044.example");
            empresa.setOtrasSenas("Avenida Central, San José");
            CatalogoUbicacionSembrado.sembrar(jdbc);
            final ConfiguracionEmpresa guardada = empresaRepository.save(empresa);
            idConfigSandbox = configuracion(guardada, AmbienteHacienda.SANDBOX);
            idConfigProduccion = configuracion(guardada, AmbienteHacienda.PRODUCCION);
            gravado13 = producto("Producto A gravado 13 % T044", "1130.00");
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
    }

    @AfterEach
    void tearDown() {
        reloj.reiniciar();
        SecurityContextHolder.clearContext();
    }

    // ── Envío: criterios 2 y 4 ───────────────────────────────────────────────

    @Test
    @DisplayName("PRODUCCION activa y un SANDBOX NO_TRANSMITIDO: el job no lo carga ni cuenta intentos; Reenviar 409 sin POST; nada cambia")
    void produccionActiva_sandboxNoTransmitido_noSeEnvia() throws Exception {
        Long id = comprobanteEn("NO_TRANSMITIDO", AmbienteHacienda.SANDBOX);
        jdbc.update("UPDATE comprobante_electronico SET intentos_envio = 2, ultimo_error = 'previo' WHERE id = ?", id);
        activar(AmbienteHacienda.PRODUCCION);
        Map<String, Object> antes = fila(id);

        assertThat(comprobanteRepository().findPendientesReintento(AmbienteHacienda.PRODUCCION))
                .extracting(c -> c.getId()).doesNotContain(id);
        assertThat(comprobanteRepository().countPendientesReintentoDeOtroAmbiente(AmbienteHacienda.PRODUCCION))
                .isGreaterThanOrEqualTo(1);
        reintentosJob.reintentarEnviosPendientes();
        assertThat(fila(id)).as("el job no toca el comprobante de otro ambiente").isEqualTo(antes);

        mockMvc.perform(post(API + id + "/reenviar").with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(MENSAJE_SANDBOX_EN_PRODUCCION));
        mockMvc.perform(post(API + id + "/enviar").with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(MENSAJE_SANDBOX_EN_PRODUCCION));
        assertThatThrownBy(() -> envioService.reenviar(id))
                .isInstanceOf(api.astro.whats_orders_manager.modules.facturacion.electronica.exception.AmbienteDistintoException.class);

        assertThat(fila(id)).as("estado, intentos, ultimo_error y fecha_envio sin cambios").isEqualTo(antes);
        assertThat(antes.get("estado")).isEqualTo("NO_TRANSMITIDO");
        assertThat(((Number) antes.get("intentos_envio")).intValue()).isEqualTo(2);
        assertThat(antes.get("ultimo_error")).isEqualTo("previo");
        hacienda.verify();
    }

    @Test
    @DisplayName("SANDBOX activa y un PRODUCCION GENERADO/FIRMADO/ERROR: tampoco se reenvía (la otra dirección)")
    void sandboxActiva_produccionReenviable_noSeEnvia() throws Exception {
        for (String estado : List.of("GENERADO", "FIRMADO", "ERROR")) {
            Long id = comprobanteEn(estado, AmbienteHacienda.PRODUCCION);
            Map<String, Object> antes = fila(id);
            mockMvc.perform(post(API + id + "/reenviar").with(user(USUARIO).roles("ADMIN")).with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(MENSAJE_PRODUCCION_EN_SANDBOX));
            reintentosJob.reintentarEnviosPendientes();
            assertThat(fila(id)).as(estado).isEqualTo(antes);
        }
        hacienda.verify();
    }

    @Test
    @DisplayName("control positivo: mismo ambiente, el job reintenta el NO_TRANSMITIDO y Reenviar transmite")
    void mismoAmbiente_envioFunciona() throws Exception {
        Long delJob = comprobanteEn("NO_TRANSMITIDO", AmbienteHacienda.SANDBOX);
        esperarToken();
        hacienda.expect(ExpectedCount.once(), requestTo(Matchers.containsString("/recepcion/v1/recepcion")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.ACCEPTED));
        reintentosJob.reintentarEnviosPendientes();
        assertThat(estado(delJob)).as("ultimoError: %s", ultimoError(delJob)).isEqualTo("ENVIADO");
        hacienda.verify();

        Long reenviado = comprobanteEn("NO_TRANSMITIDO", AmbienteHacienda.SANDBOX);
        hacienda.reset();
        esperarToken();
        hacienda.expect(ExpectedCount.once(), requestTo(Matchers.containsString("/recepcion/v1/recepcion")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.ACCEPTED));
        mockMvc.perform(post(API + reenviado + "/reenviar").with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprobante.estado").value("ENVIADO"));
        hacienda.verify();
    }

    // ── Consulta: criterios 3 y 4 ────────────────────────────────────────────

    @Test
    @DisplayName("SANDBOX activa y un PRODUCCION ENVIADO/ENVIO_INCIERTO: sondeo, job horario y consulta manual sin HTTP; no pasa a NO_TRANSMITIDO ni con el reloj +16 min")
    void sandboxActiva_produccionEnviado_noSeConsulta() throws Exception {
        Long enviado = comprobanteEn("ENVIADO", AmbienteHacienda.PRODUCCION);
        Long incierto = comprobanteEn("ENVIO_INCIERTO", AmbienteHacienda.PRODUCCION);
        jdbc.update("UPDATE comprobante_electronico SET fecha_envio = NOW(6) - INTERVAL 2 DAY, ultimo_error = 'previo' "
                + "WHERE id IN (?, ?)", enviado, incierto);
        reloj.adelantar(Duration.ofMinutes(16));
        Map<String, Object> antesEnviado = fila(enviado);
        Map<String, Object> antesIncierto = fila(incierto);

        assertThat(comprobanteRepository().findParaSondeo(java.time.LocalDateTime.now(reloj).minusMinutes(5), 5,
                AmbienteHacienda.SANDBOX)).extracting(c -> c.getId()).doesNotContain(enviado);
        assertThat(comprobanteRepository().countParaSondeoDeOtroAmbiente(java.time.LocalDateTime.now(reloj).minusMinutes(5), 5,
                AmbienteHacienda.SANDBOX)).isGreaterThanOrEqualTo(1);
        // Sondeo (la consulta que repite EnvioHaciendaServiceImpl.consultar) y job horario
        for (Long id : List.of(enviado, incierto)) {
            assertThatThrownBy(() -> comprobanteService.consultarYActualizarEstado(id))
                    .isInstanceOf(api.astro.whats_orders_manager.modules.facturacion.electronica.exception.AmbienteDistintoException.class)
                    .hasMessage(MENSAJE_PRODUCCION_EN_SANDBOX);
        }
        reintentosJob.consultarEstadosPendientes();
        // Consulta manual: 409 con el mismo mensaje fijo, también la segunda vez (no consume el turno de 30 s)
        for (int i = 0; i < 2; i++) {
            for (Long id : List.of(enviado, incierto)) {
                mockMvc.perform(post(API + id + "/consultar-estado-hacienda").with(user(USUARIO).roles("ADMIN")).with(csrf()))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value(MENSAJE_PRODUCCION_EN_SANDBOX));
            }
        }

        assertThat(fila(enviado)).isEqualTo(antesEnviado);
        assertThat(fila(incierto)).isEqualTo(antesIncierto);
        assertThat(estado(enviado)).isEqualTo("ENVIADO");
        assertThat(estado(incierto)).isEqualTo("ENVIO_INCIERTO").isNotEqualTo("NO_TRANSMITIDO");
        hacienda.verify();
    }

    @Test
    @DisplayName("PRODUCCION activa y un SANDBOX ENVIADO: la consulta manual y el sondeo tampoco hacen HTTP (la otra dirección)")
    void produccionActiva_sandboxEnviado_noSeConsulta() throws Exception {
        Long id = comprobanteEn("ENVIADO", AmbienteHacienda.SANDBOX);
        activar(AmbienteHacienda.PRODUCCION);
        Map<String, Object> antes = fila(id);
        mockMvc.perform(post(API + id + "/consultar-estado-hacienda").with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(MENSAJE_SANDBOX_EN_PRODUCCION));
        assertThatThrownBy(() -> comprobanteService.consultarYActualizarEstado(id))
                .isInstanceOf(api.astro.whats_orders_manager.modules.facturacion.electronica.exception.AmbienteDistintoException.class);
        assertThat(fila(id)).isEqualTo(antes);
        hacienda.verify();
    }

    @Test
    @DisplayName("control positivo: mismo ambiente, la consulta manual y el job consultan a Hacienda y adoptan su estado")
    void mismoAmbiente_consultaFunciona() throws Exception {
        Long manual = comprobanteEn("ENVIADO", AmbienteHacienda.SANDBOX);
        Long delJob = comprobanteEn("ENVIADO", AmbienteHacienda.SANDBOX);
        jdbc.update("UPDATE comprobante_electronico SET fecha_envio = NOW(6) - INTERVAL 2 DAY WHERE id = ?", delJob);
        esperarToken();
        for (Long id : List.of(manual, delJob)) {
            hacienda.expect(ExpectedCount.manyTimes(), requestTo(Matchers.containsString("/recepcion/v1/recepcion/" + clave(id))))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("{\"ind-estado\":\"recibido\"}", MediaType.APPLICATION_JSON));
        }
        mockMvc.perform(post(API + manual + "/consultar-estado-hacienda").with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprobante.estado").value("PROCESANDO"));

        reintentosJob.consultarEstadosPendientes();
        assertThat(estado(delJob)).as("ultimoError: %s", ultimoError(delJob)).isEqualTo("PROCESANDO");
    }

    // ── Referencias: criterio 5 ──────────────────────────────────────────────

    @Test
    @DisplayName("nota de crédito sobre una factura de otro ambiente: 409 con el motivo, sin nota ni consecutivo; la bandera del modal es false con el motivo")
    void notaDeCredito_otroAmbiente_409SinConsecutivo() throws Exception {
        int idFactura = facturaAceptada(AmbienteHacienda.SANDBOX);
        activar(AmbienteHacienda.PRODUCCION);
        long ncSandbox = consecutivo(idConfigSandbox, "consecutivo_nota_credito");
        long ncProduccion = consecutivo(idConfigProduccion, "consecutivo_nota_credito");
        int comprobantesAntes = jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico", Integer.class);

        JsonNode detalle = detalleFactura(idFactura);
        assertThat(detalle.get("puedeEmitirNotaCredito").asBoolean()).isFalse();
        assertThat(detalle.get("motivoNotaCreditoNoDisponible").asText()).isEqualTo(REFERENCIA_SANDBOX_EN_PRODUCCION);
        assertThat(detalle.get("ambienteComprobante").asText()).isEqualTo("SANDBOX");
        assertThat(detalle.get("ambienteComprobanteCoincide").asBoolean()).isFalse();
        assertThat(detalle.get("motivoAmbienteComprobante").asText()).isEqualTo(MENSAJE_SANDBOX_EN_PRODUCCION);

        MvcResult r = emitirNota(idFactura);

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(409);
        assertThat(objectMapper.readTree(r.getResponse().getContentAsString()).get("message").asText())
                .isEqualTo(REFERENCIA_SANDBOX_EN_PRODUCCION);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM nota_factura WHERE factura_id = ?", Integer.class, idFactura))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico", Integer.class))
                .isEqualTo(comprobantesAntes);
        assertThat(consecutivo(idConfigSandbox, "consecutivo_nota_credito")).isEqualTo(ncSandbox);
        assertThat(consecutivo(idConfigProduccion, "consecutivo_nota_credito")).isEqualTo(ncProduccion);
        hacienda.verify();
    }

    @Test
    @DisplayName("sustituta sobre una factura de otro ambiente: bandera false con motivo, crear y emitir → 409 sin consecutivo; con el mismo ambiente funciona")
    void sustituta_otroAmbiente_409SinConsecutivo() throws Exception {
        int idFactura = facturaAceptada(AmbienteHacienda.SANDBOX);
        // Control positivo de la nota (mismo ambiente): se emite y se envía; después se deja anulada por esa nota
        esperarToken();
        hacienda.expect(ExpectedCount.manyTimes(), requestTo(Matchers.containsString("/recepcion/v1/recepcion")))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.ACCEPTED));
        long ncAntes = consecutivo(idConfigSandbox, "consecutivo_nota_credito");
        MvcResult nota = emitirNota(idFactura);
        assertThat(nota.getResponse().getStatus()).as(nota.getResponse().getContentAsString()).isEqualTo(201);
        long idNota = objectMapper.readTree(nota.getResponse().getContentAsString()).get("data").get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT ambiente FROM comprobante_electronico WHERE nota_factura_id = ?",
                String.class, idNota)).isEqualTo("SANDBOX");
        assertThat(consecutivo(idConfigSandbox, "consecutivo_nota_credito")).isEqualTo(ncAntes + 1);
        jdbc.update("UPDATE nota_factura SET estado = 'APLICADA', fecha_aplicacion = NOW(6) WHERE id = ?", idNota);
        jdbc.update("UPDATE comprobante_electronico SET estado = 'ACEPTADO' WHERE nota_factura_id = ?", idNota);
        jdbc.update("UPDATE factura SET anulada = b'1' WHERE id_factura = ?", idFactura);
        assertThat(detalleFactura(idFactura).get("puedeEmitirSustituta").asBoolean()).isTrue();

        // Con PRODUCCION activa, la original (SANDBOX) ya no admite sustituta
        activar(AmbienteHacienda.PRODUCCION);
        long fSandbox = consecutivo(idConfigSandbox, "consecutivo_factura");
        long fProduccion = consecutivo(idConfigProduccion, "consecutivo_factura");
        int facturasAntes = jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE sustituye_factura_id = ?",
                Integer.class, idFactura);

        JsonNode detalle = detalleFactura(idFactura);
        assertThat(detalle.get("puedeEmitirSustituta").asBoolean()).isFalse();
        assertThat(detalle.get("motivoSustitutaNoDisponible").asText()).isEqualTo(REFERENCIA_SANDBOX_EN_PRODUCCION);
        MvcResult rechazada = sustituta(idFactura);
        assertThat(rechazada.getResponse().getStatus()).as(rechazada.getResponse().getContentAsString()).isEqualTo(409);
        assertThat(objectMapper.readTree(rechazada.getResponse().getContentAsString()).get("message").asText())
                .isEqualTo(REFERENCIA_SANDBOX_EN_PRODUCCION);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE sustituye_factura_id = ?", Integer.class,
                idFactura)).isEqualTo(facturasAntes);
        assertThat(consecutivo(idConfigSandbox, "consecutivo_factura")).isEqualTo(fSandbox);
        assertThat(consecutivo(idConfigProduccion, "consecutivo_factura")).isEqualTo(fProduccion);
        // El detalle de la nota lleva el mismo motivo y el comprobante de la nota, su ambiente
        JsonNode detalleNota = objectMapper.readTree(mockMvc.perform(get("/api/facturas/notas-credito/" + idNota + "/detalle")
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getContentAsString()).get("data");
        assertThat(detalleNota.get("puedeEmitirSustituta").asBoolean()).isFalse();
        assertThat(detalleNota.get("motivoSustitutaNoDisponible").asText()).isEqualTo(REFERENCIA_SANDBOX_EN_PRODUCCION);
        assertThat(detalleNota.get("comprobante").get("ambiente").asText()).isEqualTo("SANDBOX");
        assertThat(detalleNota.get("comprobante").get("ambienteCoincide").asBoolean()).isFalse();

        // Mismo ambiente otra vez: la sustituta se crea
        activar(AmbienteHacienda.SANDBOX);
        MvcResult ok = sustituta(idFactura);
        assertThat(ok.getResponse().getStatus()).as(ok.getResponse().getContentAsString()).isEqualTo(201);
    }

    @Test
    @DisplayName("emitir una sustituta (procesar) cuya original es de otro ambiente: 409 con el motivo, sin consecutivo ni comprobante")
    void sustituta_procesarConOriginalDeOtroAmbiente_409() throws Exception {
        int idFactura = facturaAceptada(AmbienteHacienda.SANDBOX);
        esperarToken();
        hacienda.expect(ExpectedCount.manyTimes(), requestTo(Matchers.containsString("/recepcion/v1/recepcion")))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.ACCEPTED));
        long idNota = objectMapper.readTree(emitirNota(idFactura).getResponse().getContentAsString()).get("data").get("id").asLong();
        jdbc.update("UPDATE nota_factura SET estado = 'APLICADA', fecha_aplicacion = NOW(6) WHERE id = ?", idNota);
        jdbc.update("UPDATE comprobante_electronico SET estado = 'ACEPTADO' WHERE nota_factura_id = ?", idNota);
        jdbc.update("UPDATE factura SET anulada = b'1' WHERE id_factura = ?", idFactura);
        int idSustituta = objectMapper.readTree(sustituta(idFactura).getResponse().getContentAsString())
                .get("data").get("idFactura").asInt();
        hacienda.reset();
        activar(AmbienteHacienda.PRODUCCION);
        long fSandbox = consecutivo(idConfigSandbox, "consecutivo_factura");
        long fProduccion = consecutivo(idConfigProduccion, "consecutivo_factura");

        MvcResult r = mockMvc.perform(post(API + "procesar/" + idSustituta)
                .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn();

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(409);
        assertThat(objectMapper.readTree(r.getResponse().getContentAsString()).get("message").asText())
                .isEqualTo(REFERENCIA_SANDBOX_EN_PRODUCCION);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico WHERE factura_id = ?",
                Integer.class, idSustituta)).isZero();
        assertThat(consecutivo(idConfigSandbox, "consecutivo_factura")).isEqualTo(fSandbox);
        assertThat(consecutivo(idConfigProduccion, "consecutivo_factura")).isEqualTo(fProduccion);
        hacienda.verify();
    }

    // ── Emisión: criterio 7 ──────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} activa")
    @ValueSource(strings = {"SANDBOX", "PRODUCCION"})
    @DisplayName("el comprobante nuevo (factura y nota) toma el ambiente de la configuración activa y su consecutivo sale de esa configuración")
    void emision_tomaElAmbienteActivo(String nombre) throws Exception {
        AmbienteHacienda activo = AmbienteHacienda.valueOf(nombre);
        Long idActiva = activo == AmbienteHacienda.SANDBOX ? idConfigSandbox : idConfigProduccion;
        Long idOtra = activo == AmbienteHacienda.SANDBOX ? idConfigProduccion : idConfigSandbox;
        activar(activo);
        esperarToken();
        hacienda.expect(ExpectedCount.manyTimes(), requestTo(Matchers.containsString("/recepcion/v1/recepcion")))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.ACCEPTED));
        long facturaActiva = consecutivo(idActiva, "consecutivo_factura");
        long facturaOtra = consecutivo(idOtra, "consecutivo_factura");
        long notaActiva = consecutivo(idActiva, "consecutivo_nota_credito");
        long notaOtra = consecutivo(idOtra, "consecutivo_nota_credito");

        // Factura
        int idFactura = facturaConLinea();
        mockMvc.perform(post(API + "procesar/" + idFactura).with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());
        Map<String, Object> comp = jdbc.queryForMap("SELECT ambiente, consecutivo FROM comprobante_electronico "
                + "WHERE factura_id = ?", idFactura);
        assertThat(comp.get("ambiente")).isEqualTo(nombre);
        assertThat((String) comp.get("consecutivo")).isEqualTo(String.format("%020d", facturaActiva));
        assertThat(consecutivo(idActiva, "consecutivo_factura")).isEqualTo(facturaActiva + 1);
        assertThat(consecutivo(idOtra, "consecutivo_factura")).isEqualTo(facturaOtra);

        // Nota de crédito sobre una factura aceptada de este mismo ambiente
        int aceptada = facturaAceptada(activo);
        MvcResult nota = emitirNota(aceptada);
        assertThat(nota.getResponse().getStatus()).as(nota.getResponse().getContentAsString()).isEqualTo(201);
        long idNota = objectMapper.readTree(nota.getResponse().getContentAsString()).get("data").get("id").asLong();
        Map<String, Object> compNota = jdbc.queryForMap("SELECT ambiente, consecutivo FROM comprobante_electronico "
                + "WHERE nota_factura_id = ?", idNota);
        assertThat(compNota.get("ambiente")).isEqualTo(nombre);
        assertThat((String) compNota.get("consecutivo")).isEqualTo(String.format("%020d", notaActiva));
        assertThat(consecutivo(idActiva, "consecutivo_nota_credito")).isEqualTo(notaActiva + 1);
        assertThat(consecutivo(idOtra, "consecutivo_nota_credito")).isEqualTo(notaOtra);
    }

    // ── Interfaz: criterio 8 (banderas del servidor) ─────────────────────────

    @Test
    @DisplayName("listado y detalle de comprobantes, listado de notas y detalle de la factura llevan el ambiente y las banderas")
    void banderasDeLaInterfaz() throws Exception {
        Long sandbox = comprobanteEn("NO_TRANSMITIDO", AmbienteHacienda.SANDBOX);
        Long produccion = comprobanteEn("NO_TRANSMITIDO", AmbienteHacienda.PRODUCCION);
        Integer idEmpresa = jdbc.queryForObject(
                "SELECT id_configuracion FROM configuracion_empresa ORDER BY id_configuracion LIMIT 1", Integer.class);

        JsonNode lista = objectMapper.readTree(mockMvc.perform(get(API + "empresa/" + idEmpresa).param("page", "0")
                .param("size", "1000").with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getContentAsString());
        Map<Long, JsonNode> porId = new LinkedHashMap<>();
        lista.forEach(f -> porId.put(f.get("id").asLong(), f));
        assertThat(porId.get(sandbox).get("ambiente").asText()).isEqualTo("SANDBOX");
        assertThat(porId.get(sandbox).get("ambienteCoincide").asBoolean()).isTrue();
        assertThat(porId.get(sandbox).get("motivoAmbiente").isNull()).isTrue();
        assertThat(porId.get(produccion).get("ambiente").asText()).isEqualTo("PRODUCCION");
        assertThat(porId.get(produccion).get("ambienteCoincide").asBoolean()).isFalse();
        assertThat(porId.get(produccion).get("motivoAmbiente").asText()).isEqualTo(MENSAJE_PRODUCCION_EN_SANDBOX);
        // El estado sigue mandando: puedeReenviar es la regla por estado; la UI lo deshabilita con el motivo
        assertThat(porId.get(produccion).get("puedeReenviar").asBoolean()).isTrue();

        JsonNode detalle = objectMapper.readTree(mockMvc.perform(get(API + produccion)
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getContentAsString());
        assertThat(detalle.get("ambiente").asText()).isEqualTo("PRODUCCION");
        assertThat(detalle.get("ambienteCoincide").asBoolean()).isFalse();
        assertThat(detalle.get("motivoAmbiente").asText()).isEqualTo(MENSAJE_PRODUCCION_EN_SANDBOX);

        // Listado de notas: la nota de una factura SANDBOX con PRODUCCION activa
        int idFactura = facturaAceptada(AmbienteHacienda.SANDBOX);
        esperarToken();
        hacienda.expect(ExpectedCount.manyTimes(), requestTo(Matchers.containsString("/recepcion/v1/recepcion")))
                .andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.ACCEPTED));
        long idNota = objectMapper.readTree(emitirNota(idFactura).getResponse().getContentAsString()).get("data").get("id").asLong();
        activar(AmbienteHacienda.PRODUCCION);
        JsonNode pagina = objectMapper.readTree(mockMvc.perform(get("/api/facturas/notas-credito")
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getContentAsString()).get("data");
        JsonNode fila = null;
        for (JsonNode f : pagina.get("contenido")) {
            if (f.get("id").asLong() == idNota) fila = f;
        }
        assertThat(fila).isNotNull();
        assertThat(fila.get("ambiente").asText()).isEqualTo("SANDBOX");
        assertThat(fila.get("ambienteCoincide").asBoolean()).isFalse();
        assertThat(fila.get("motivoAmbiente").asText()).isEqualTo(MENSAJE_SANDBOX_EN_PRODUCCION);
        assertThat(fila.get("consultableEnHacienda").asBoolean()).isTrue();
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
        hacienda.expect(ExpectedCount.manyTimes(), requestTo(Matchers.containsString("/openid-connect/token")))
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
                claveNueva(), consecutivoNuevo(), estado, idFactura, xmlFirmadoReal(), "comprobantes/t044.xml",
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
            cliente.setNombre("Cliente T044 " + UUID.randomUUID());
            cliente.setTipoIdentificacion(TipoIdentificacion.CEDULA_FISICA);
            cliente.setNumeroIdentificacion("109990999");
            cliente.setTelefono("88887777");
            cliente.setEmail("cliente@t044.example");
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T044-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T044");
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
        p.setCodigo("T44-" + UUID.randomUUID().toString().substring(0, 8));
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
