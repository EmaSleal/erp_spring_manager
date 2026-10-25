package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.scheduled.ComprobanteReintentosJob;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.FirmaDigitalService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.XmlGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * T-026 (ADR-031 §3 y §4) sobre MySQL:
 * <ul>
 *   <li>PARTE C — createDate de Factura y LineaFactura nunca proviene del cliente: editar sin createDate no falla y
 *       uno distinto se ignora;</li>
 *   <li>PARTE D — el detalle de la factura del modal trae estadoComprobante, claveComprobante y revisarEnAtv del
 *       comprobante vigente, sin la entidad ComprobanteElectronico;</li>
 *   <li>PARTE E — "Consultar estado en Hacienda": estados permitidos, 1 consulta cada 30 s, regla del 404 según el
 *       estado de origen y reinicio del contador de ERROR_HACIENDA.</li>
 * </ul>
 * Base: MySQL 8 en Testcontainers (t026-it-mysql, tmpfs, sin volúmenes), como BloqueoFiscalFacturaIT. Hacienda, la
 * firma, el XML y la configuración de Hacienda están simulados (@MockitoBean): ninguna prueba llama a Hacienda.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-026 — createDate, estado FE en el detalle y consulta manual de estado en Hacienda (MySQL)")
class EstadoFeYConsultaHaciendaIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t026")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t026-it-mysql"))
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

    private static final String USUARIO = "admin.bootstrap@example.invalid";
    private static final String CONSULTAR = "/api/facturas/electronica/comprobantes/%d/consultar-estado-hacienda";

    @MockitoBean private HaciendaApiService haciendaApi;
    @MockitoBean private FirmaDigitalService firmaDigital;
    @MockitoBean private XmlGeneratorService xmlGenerator;
    @MockitoBean private ConfiguracionHaciendaService configuracionHaciendaService;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ComprobanteElectronicoRepository comprobanteRepository;
    @Autowired private ComprobanteReintentosJob reintentosJob;
    @Autowired private RelojAjustable reloj;

    /** Reloj de la aplicación: el del sistema desplazado por un offset que las pruebas adelantan. */
    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        RelojAjustable relojAjustable() {
            return new RelojAjustable();
        }
    }

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

    private TransactionTemplate tx;
    private Producto gravado13;
    private Integer idEmpresa;

    @AfterEach
    void tearDown() {
        reloj.reiniciar();
    }

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            if (parametroRepository.findByClave(AsientoContableService.CLAVE_VENTA_CXC_CLIENTES).isEmpty()) {
                param(AsientoContableService.CLAVE_VENTA_CXC_CLIENTES,
                        cuenta("9.1.02.001", "Clientes", TipoCuenta.ACTIVO));
                param(AsientoContableService.CLAVE_VENTA_INGRESOS,
                        cuenta("9.4.01.001", "Ventas", TipoCuenta.INGRESO));
                param(AsientoContableService.CLAVE_VENTA_IVA_POR_PAGAR,
                        cuenta("9.2.01.002", "IVA por pagar", TipoCuenta.PASIVO));
            }
            ConfiguracionEmpresa empresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc()
                    .orElseGet(() -> ConfiguracionEmpresa.builder().razonSocial("Empresa T026 S.A.").build());
            if (empresa.getNumeroIdentificacion() == null) {
                empresa.setNumeroIdentificacion("3101234567");
            }
            empresa = empresaRepository.save(empresa);
            idEmpresa = empresa.getIdConfiguracion();
            ConfiguracionHacienda config = ConfiguracionHacienda.builder().id(1L).empresa(empresa).build();
            when(configuracionHaciendaService.obtenerConfiguracionActiva()).thenReturn(config);
            gravado13 = producto("Producto T026 gravado 13 %", "1130.00");
        });
        when(xmlGenerator.generarXmlFactura(any())).thenAnswer(inv ->
                "<FacturaElectronica><Clave>" + ((ComprobanteElectronico) inv.getArgument(0)).getClaveNumerica()
                        + "</Clave></FacturaElectronica>");
        when(firmaDigital.firmarXml(anyString(), any(), any())).thenAnswer(inv -> inv.getArgument(0) + "<!--firmado-->");
    }

    // ── PARTE C: createDate ──────────────────────────────────────────────────

    @Test
    @DisplayName("crear por POST /facturas/guardar (con una línea y su asiento) y editar sin createDate → 200 y createDate sin cambios")
    void editarSinCreateDate_200_yFechaSinCambios() throws Exception {
        int idFactura = crearPorGuardar();
        assertThat(guardarLineas(List.of(linea(-1, idFactura, 1, "1130.00"))).getResponse().getStatus())
                .isEqualTo(200);
        LocalDateTime creada = createDateFactura(idFactura);
        assertThat(creada).isNotNull();

        Map<String, Object> cuerpo = facturaEditada(idFactura, "CREDITO");
        MvcResult r = guardar(cuerpo);

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(condicionVenta(idFactura)).isEqualTo("CREDITO");
        assertThat(createDateFactura(idFactura)).isEqualTo(creada);
        Map<?, ?> json = objectMapper.readValue(r.getResponse().getContentAsString(), Map.class);
        assertThat(json.get("createDate")).as("createDate de la respuesta").isNotNull();
        assertThat(LocalDateTime.parse((String) json.get("createDate")).truncatedTo(ChronoUnit.SECONDS))
                .isEqualTo(creada.truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("editar enviando un createDate distinto → se ignora: queda el valor original en la BD")
    void editarConCreateDateDistinto_seIgnora() throws Exception {
        int idFactura = crearPorGuardar();
        assertThat(guardarLineas(List.of(linea(-1, idFactura, 1, "1130.00"))).getResponse().getStatus())
                .isEqualTo(200);
        LocalDateTime creada = createDateFactura(idFactura);

        Map<String, Object> cuerpo = facturaEditada(idFactura, "CREDITO");
        cuerpo.put("createDate", "2001-02-03T04:05:06");
        MvcResult r = guardar(cuerpo);

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(createDateFactura(idFactura)).isEqualTo(creada);
        Map<?, ?> json = objectMapper.readValue(r.getResponse().getContentAsString(), Map.class);
        assertThat((String) json.get("createDate")).doesNotStartWith("2001-02-03");
    }

    @Test
    @DisplayName("LineaFactura: editar una línea enviando create_date distinto → la BD conserva el de la creación")
    void lineaConCreateDateDistinto_seIgnora() throws Exception {
        int idFactura = crearPorGuardar();
        assertThat(guardarLineas(List.of(linea(-1, idFactura, 1, "1130.00"))).getResponse().getStatus())
                .isEqualTo(200);
        Integer idLinea = jdbc.queryForObject("SELECT id_linea_factura FROM linea_factura WHERE id_factura = ?",
                Integer.class, idFactura);
        LocalDateTime creada = jdbc.queryForObject("SELECT create_date FROM linea_factura WHERE id_linea_factura = ?",
                LocalDateTime.class, idLinea);
        assertThat(creada).isNotNull();

        Map<String, Object> linea = linea(idLinea, idFactura, 2, "1130.00");
        linea.put("create_date", "2001-02-03T04:05:06");
        assertThat(guardarLineas(List.of(linea)).getResponse().getStatus()).isEqualTo(200);

        assertThat(jdbc.queryForObject("SELECT create_date FROM linea_factura WHERE id_linea_factura = ?",
                LocalDateTime.class, idLinea)).isEqualTo(creada);
        assertThat(jdbc.queryForObject("SELECT cantidad FROM linea_factura WHERE id_linea_factura = ?",
                Integer.class, idLinea)).isEqualTo(2);
    }

    // ── PARTE D: estado FE en el detalle del modal ───────────────────────────

    @Test
    @DisplayName("detalle sin comprobante → estadoComprobante nulo")
    void detalle_sinComprobante() throws Exception {
        int idFactura = facturaSinLineas();

        Map<?, ?> json = detalle(idFactura);

        assertThat(json.get("estadoComprobante")).isNull();
        assertThat(json.get("claveComprobante")).isNull();
    }

    @Test
    @DisplayName("detalle con comprobante ACEPTADO → estadoComprobante ACEPTADO y su clave")
    void detalle_conAceptado() throws Exception {
        int idFactura = facturaSinLineas();
        Long id = comprobante(idFactura, "ACEPTADO");

        Map<?, ?> json = detalle(idFactura);

        assertThat(json.get("estadoComprobante")).isEqualTo("ACEPTADO");
        assertThat(json.get("claveComprobante")).isEqualTo(clave(id));
        assertThat(json.get("revisarEnAtv")).isEqualTo(false);
    }

    @Test
    @DisplayName("detalle con ERROR_HACIENDA señalado (5 consultas con error) → revisarEnAtv=true")
    void detalle_errorHaciendaSenalado() throws Exception {
        int idFactura = facturaSinLineas();
        Long id = comprobante(idFactura, "ERROR_HACIENDA");
        jdbc.update("UPDATE comprobante_electronico SET consultas_error_hacienda = 5 WHERE id = ?", id);

        Map<?, ?> json = detalle(idFactura);

        assertThat(json.get("estadoComprobante")).isEqualTo("ERROR_HACIENDA");
        assertThat(json.get("revisarEnAtv")).isEqualTo(true);
    }

    @Test
    @DisplayName("el JSON del detalle no contiene hibernateLazyInitializer ni la entidad ComprobanteElectronico")
    void detalle_sinEntidadNiProxys() throws Exception {
        int idFactura = facturaSinLineas();
        comprobante(idFactura, "ACEPTADO");

        String cuerpo = mockMvc.perform(get("/facturas/detalle/" + idFactura).with(user(USUARIO).roles("ADMIN"))
                .accept(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("hibernateLazyInitializer")
                .doesNotContain("comprobanteElectronico")
                .doesNotContain("xmlComprobante")
                .doesNotContain("claveNumerica");
    }

    // ── PARTE E: consultar estado en Hacienda ────────────────────────────────

    @Test
    @DisplayName("ENVIO_INCIERTO + 404 a los 10 min → sin cambios y mensaje; a los 16 min → NO_TRANSMITIDO")
    void envioIncierto_404_segunMinutos() throws Exception {
        Long id = comprobante(facturaSinLineas(), "ENVIO_INCIERTO", LocalDateTime.now().minusMinutes(10));
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta404());

        MvcResult aLos10 = consultar(id);

        assertThat(aLos10.getResponse().getStatus()).isEqualTo(200);
        assertThat(estado(id)).isEqualTo("ENVIO_INCIERTO");
        assertThat(mensaje(aLos10)).contains("15 minutos").contains("a partir de las");

        reloj.adelantar(Duration.ofMinutes(6));
        MvcResult aLos16 = consultar(id);

        assertThat(aLos16.getResponse().getStatus()).isEqualTo(200);
        assertThat(estado(id)).isEqualTo("NO_TRANSMITIDO");
    }

    @Test
    @DisplayName("ENVIADO + 404 en la consulta manual → estado de T-025 (ENVIO_INCIERTO), nunca NO_TRANSMITIDO")
    void enviado_404_manual_nuncaNoTransmitido() throws Exception {
        Long id = comprobante(facturaSinLineas(), "ENVIADO", LocalDateTime.now().minusMinutes(30));
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta404());

        assertThat(consultar(id).getResponse().getStatus()).isEqualTo(200);

        assertThat(estado(id)).isEqualTo("ENVIO_INCIERTO");
    }

    @Test
    @DisplayName("ENVIADO + 404 en el sondeo → estado de T-025 (ENVIO_INCIERTO), nunca NO_TRANSMITIDO")
    void enviado_404_sondeo_nuncaNoTransmitido() throws Exception {
        Long id = comprobante(facturaSinLineas(), "ENVIADO", LocalDateTime.now().minusMinutes(30));
        String clave = clave(id);
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenAnswer(inv ->
                clave.equals(inv.getArgument(0)) ? consulta404() : consulta("procesando"));

        reintentosJob.consultarEstadosPendientes();

        assertThat(estado(id)).isEqualTo("ENVIO_INCIERTO");
    }

    @Test
    @DisplayName("ERROR_HACIENDA con contador 5 + consulta manual 'error' → ERROR_HACIENDA, contador 1 y vuelve al sondeo")
    void errorHacienda_consultaManualError_reiniciaContador() throws Exception {
        Long id = comprobante(facturaSinLineas(), "ERROR_HACIENDA");
        jdbc.update("UPDATE comprobante_electronico SET consultas_error_hacienda = 5 WHERE id = ?", id);
        assertThat(enSondeo(id)).as("señalado: fuera del sondeo antes de la consulta").isFalse();
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta("error"));

        assertThat(consultar(id).getResponse().getStatus()).isEqualTo(200);

        assertThat(estado(id)).isEqualTo("ERROR_HACIENDA");
        assertThat(contadorError(id)).isEqualTo(1);
        assertThat(enSondeo(id)).isTrue();
    }

    @Test
    @DisplayName("ERROR_HACIENDA con contador 5 + consulta manual 'aceptado' → ACEPTADO con contador 0")
    void errorHacienda_consultaManualAceptado() throws Exception {
        Long id = comprobante(facturaSinLineas(), "ERROR_HACIENDA");
        jdbc.update("UPDATE comprobante_electronico SET consultas_error_hacienda = 5 WHERE id = ?", id);
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta("aceptado"));

        assertThat(consultar(id).getResponse().getStatus()).isEqualTo(200);

        assertThat(estado(id)).isEqualTo("ACEPTADO");
        assertThat(contadorError(id)).isZero();
    }

    @Test
    @DisplayName("PROCESANDO + consulta manual 'aceptado' → ACEPTADO")
    void procesando_aceptado() throws Exception {
        Long id = comprobante(facturaSinLineas(), "PROCESANDO");
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta("aceptado"));

        assertThat(consultar(id).getResponse().getStatus()).isEqualTo(200);

        assertThat(estado(id)).isEqualTo("ACEPTADO");
    }

    @ParameterizedTest(name = "{0} → 409")
    @ValueSource(strings = {"GENERADO", "NO_TRANSMITIDO", "ACEPTADO"})
    @DisplayName("consulta manual en un estado no consultable → 409 sin consultar a Hacienda")
    void estadoNoConsultable_409(String estado) throws Exception {
        Long id = comprobante(facturaSinLineas(), estado);
        clearInvocations(haciendaApi);

        MvcResult r = consultar(id);

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(estado(id)).isEqualTo(estado);
        verify(haciendaApi, never()).consultarComprobante(anyString(), anyLong());
    }

    @Test
    @DisplayName("dos consultas manuales en 10 s → la segunda 429 con la hora; a los 31 s → 200")
    void limiteDe30Segundos() throws Exception {
        Long id = comprobante(facturaSinLineas(), "PROCESANDO");
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta("procesando"));
        clearInvocations(haciendaApi);

        assertThat(consultar(id).getResponse().getStatus()).isEqualTo(200);
        reloj.adelantar(Duration.ofSeconds(10));
        MvcResult segunda = consultar(id);

        assertThat(segunda.getResponse().getStatus()).isEqualTo(429);
        assertThat(mensaje(segunda)).contains("a partir de las");
        Map<?, ?> json = objectMapper.readValue(segunda.getResponse().getContentAsString(), Map.class);
        assertThat(json.get("disponibleDesde")).isNotNull();
        verify(haciendaApi, times(1)).consultarComprobante(anyString(), anyLong());

        reloj.adelantar(Duration.ofSeconds(21));
        assertThat(consultar(id).getResponse().getStatus()).isEqualTo(200);
        verify(haciendaApi, times(2)).consultarComprobante(eq(clave(id)), anyLong());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private MvcResult consultar(Long id) throws Exception {
        return mockMvc.perform(post(String.format(CONSULTAR, id)).with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andReturn();
    }

    private String mensaje(MvcResult r) throws Exception {
        return (String) objectMapper.readValue(r.getResponse().getContentAsString(), Map.class).get("message");
    }

    private Map<?, ?> detalle(int idFactura) throws Exception {
        MvcResult r = mockMvc.perform(get("/facturas/detalle/" + idFactura).with(user(USUARIO).roles("ADMIN"))
                .accept(MediaType.APPLICATION_JSON)).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readValue(r.getResponse().getContentAsString(), Map.class);
    }

    private boolean enSondeo(Long id) {
        return comprobanteRepository.findParaSondeo(LocalDateTime.now(reloj).minusMinutes(5), 5).stream()
                .anyMatch(c -> c.getId().equals(id));
    }

    /** Crea una factura sin líneas por la ruta de la UI (POST /facturas/guardar sin idFactura). */
    private int crearPorGuardar() throws Exception {
        Cliente cliente = tx.execute(s -> {
            Cliente c = new Cliente();
            c.setNombre("Cliente T026 " + UUID.randomUUID());
            return clienteRepository.save(c);
        });
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("numeroFactura", "T026-" + UUID.randomUUID().toString().substring(0, 8));
        f.put("serie", "T026");
        f.put("cliente", Map.of("idCliente", cliente.getIdCliente()));
        f.put("monedaFE", "CRC");
        f.put("tipoCambio", 1);
        f.put("condicionVentaFE", "CONTADO");
        f.put("tipoFactura", "INSTITUCIONAL");
        f.put("subtotal", 0);
        f.put("igv", 0);
        f.put("total", 0);
        MvcResult r = guardar(f);
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return (Integer) objectMapper.readValue(r.getResponse().getContentAsString(), Map.class).get("idFactura");
    }

    private MvcResult guardar(Map<String, Object> cuerpo) throws Exception {
        return mockMvc.perform(post("/facturas/guardar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andReturn();
    }

    /** Cuerpo de edición tal como lo arma la UI, sin createDate. */
    private Map<String, Object> facturaEditada(int idFactura, String condicionVenta) {
        return jdbc.queryForObject("SELECT numero_factura, serie, id_cliente, total, subtotal, igv FROM factura "
                + "WHERE id_factura = ?", (rs, i) -> {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("idFactura", idFactura);
            f.put("numeroFactura", rs.getString(1));
            f.put("serie", rs.getString(2));
            f.put("cliente", Map.of("idCliente", rs.getInt(3)));
            f.put("monedaFE", "CRC");
            f.put("tipoCambio", 1);
            f.put("condicionVentaFE", condicionVenta);
            f.put("tipoFactura", "INSTITUCIONAL");
            f.put("total", rs.getBigDecimal(4));
            f.put("subtotal", rs.getBigDecimal(5));
            f.put("igv", rs.getBigDecimal(6));
            return f;
        }, idFactura);
    }

    private static Map<String, Object> linea(int idLinea, int idFactura, int cantidad, String precio) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("id_factura", idFactura);
        linea.put("id_linea_factura", idLinea);
        linea.put("numero_linea", 1);
        linea.put("cantidad", cantidad);
        linea.put("precioUnitario", new BigDecimal(precio));
        linea.put("subtotal", new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad)));
        return linea;
    }

    private MvcResult guardarLineas(List<Map<String, Object>> lineas) throws Exception {
        List<Map<String, Object>> conProducto = new ArrayList<>();
        for (Map<String, Object> l : lineas) {
            Map<String, Object> copia = new LinkedHashMap<>(l);
            copia.put("id_producto", gravado13.getIdProducto());
            conProducto.add(copia);
        }
        return mockMvc.perform(put("/lineas-factura/actualizar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conProducto)))
                .andReturn();
    }

    private int facturaSinLineas() {
        return tx.execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T026 " + UUID.randomUUID());
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T026-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T026");
            factura.setMonedaFE(MonedaFE.CRC);
            factura.setTipoCambio(BigDecimal.ONE);
            factura.setSubtotal(BigDecimal.ZERO);
            factura.setIgv(BigDecimal.ZERO);
            factura.setTotal(BigDecimal.ZERO);
            factura.setAnulada(false);
            return facturaRepository.save(factura).getIdFactura();
        });
    }

    private Long comprobante(int idFactura, String estado) {
        return comprobante(idFactura, estado, LocalDateTime.now().minusHours(1));
    }

    /** Comprobante vigente en el estado indicado, insertado por SQL (sin pasar por Hacienda). */
    private Long comprobante(int idFactura, String estado, LocalDateTime fechaEnvio) {
        StringBuilder clave = new StringBuilder("506");
        while (clave.length() < 50) {
            clave.append(ThreadLocalRandom.current().nextInt(10));
        }
        jdbc.update("INSERT INTO comprobante_electronico (clave_numerica, consecutivo, created_at, updated_at, estado, "
                        + "fecha_emision, fecha_envio, tipo_comprobante, empresa_id, factura_id, vigente, intentos_envio, "
                        + "enviado_email, xml_comprobante, ruta_archivo_xml) "
                        + "VALUES (?, ?, NOW(6), NOW(6), ?, NOW(6), ?, 'FACTURA_ELECTRONICA', ?, ?, b'1', 0, b'0', ?, ?)",
                clave.toString(), String.format("%020d", ThreadLocalRandom.current().nextLong(1, 1_000_000_000L)),
                estado, fechaEnvio, idEmpresa, idFactura, "<FacturaElectronica>T026</FacturaElectronica>",
                "comprobantes/t026.xml");
        return jdbc.queryForObject("SELECT MAX(id) FROM comprobante_electronico WHERE factura_id = ?", Long.class,
                idFactura);
    }

    private String estado(Long id) {
        return jdbc.queryForObject("SELECT estado FROM comprobante_electronico WHERE id = ?", String.class, id);
    }

    private String clave(Long id) {
        return jdbc.queryForObject("SELECT clave_numerica FROM comprobante_electronico WHERE id = ?", String.class, id);
    }

    private Integer contadorError(Long id) {
        return jdbc.queryForObject("SELECT consultas_error_hacienda FROM comprobante_electronico WHERE id = ?",
                Integer.class, id);
    }

    private LocalDateTime createDateFactura(int idFactura) {
        return jdbc.queryForObject("SELECT create_date FROM factura WHERE id_factura = ?", LocalDateTime.class,
                idFactura);
    }

    private String condicionVenta(int idFactura) {
        return jdbc.queryForObject("SELECT condicion_venta_fe FROM factura WHERE id_factura = ?", String.class,
                idFactura);
    }

    private static RespuestaHaciendaDTO consulta(String indEstado) {
        return RespuestaHaciendaDTO.builder().indicadorEstado(indEstado).codigoHttp(200).exitoso(false)
                .codigoMensaje(MensajeHacienda.PROCESANDO).mensaje("ind-estado " + indEstado).build();
    }

    private static RespuestaHaciendaDTO consulta404() {
        return RespuestaHaciendaDTO.builder().documentoNoExiste(true).codigoHttp(404).exitoso(false)
                .codigoMensaje(MensajeHacienda.ERROR).mensaje("Hacienda no tiene el documento (404)").build();
    }

    @SuppressWarnings("deprecation")
    private Producto producto(String descripcion, String precio) {
        Producto p = new Producto();
        p.setCodigo("T26-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDescripcion(descripcion);
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
