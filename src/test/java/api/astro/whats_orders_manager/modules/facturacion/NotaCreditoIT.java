package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.FirmaDigitalService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.XmlGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.NotaFacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * T-032 (ADR-037) — nota de crédito total de punta a punta sobre MySQL: emisión por la API, precondiciones (409),
 * concurrencia, comprobante tipo 03 con su consecutivo, estados de ADR-030 con el cliente de Hacienda simulado
 * (aceptado, rechazado, 404 en la verificación, timeout de lectura) y efectos al aceptarse.
 *
 * Base: MySQL 8 en Testcontainers (t032-it-mysql, tmpfs, sin volúmenes); esquema de Hibernate y rutinas de
 * database/routines, como BloqueoFiscalFacturaIT. El generador de XML es el real; la firma, el cliente de Hacienda y
 * la configuración de Hacienda están simulados: ninguna prueba llama a Hacienda. El envío simulado responde como un
 * timeout de lectura (ENVIO_INCIERTO) salvo que la prueba diga otra cosa, para no entrar en el sondeo con esperas del
 * envío asíncrono.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-032 — nota de crédito electrónica total (MySQL)")
class NotaCreditoIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t032")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t032-it-mysql"))
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

    /** Administrador de bootstrap que DataInitializer crea en la base de pruebas. */
    private static final String USUARIO = "admin.bootstrap@example.invalid";

    @MockitoBean private HaciendaApiService haciendaApi;
    @MockitoBean private FirmaDigitalService firmaDigital;
    @MockitoBean private ConfiguracionHaciendaService configuracionHaciendaService;
    @MockitoSpyBean private PagoService pagoService;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private AsientoContableService asientoContableService;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ComprobanteElectronicoRepository comprobanteRepository;
    @Autowired private ComprobanteElectronicoService comprobanteService;
    @Autowired private XmlGeneratorService xmlGenerator;
    @Autowired private NotaFacturaService notaService;
    @Autowired private RelojAjustable reloj;

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

    private TransactionTemplate tx;
    private Producto gravado13;
    private Producto exento;
    private ConfiguracionHacienda configHacienda;

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
                    .orElseGet(() -> ConfiguracionEmpresa.builder().razonSocial("Empresa T032 S.A.").build());
            empresa.setNumeroIdentificacion("3101234567");
            empresa.setCodigoActividad("4773.0");
            empresa.setEmail("facturacion@t032.example");
            empresa.setOtrasSenas("Avenida Central, San José");
            empresa = empresaRepository.save(empresa);
            if (configHacienda == null) {
                configHacienda = ConfiguracionHacienda.builder().id(1L).empresa(empresa).build();
            }
            when(configuracionHaciendaService.obtenerConfiguracionActiva()).thenReturn(configHacienda);
            gravado13 = producto("Producto A gravado 13 %", "1130.00", true);
            exento = producto("Producto B exento", "500.00", false);
        });
        when(firmaDigital.firmarXml(anyString(), any(), any())).thenAnswer(inv -> inv.getArgument(0) + "<!--firmado-->");
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(timeoutDeLectura());
    }

    @AfterEach
    void tearDown() {
        reloj.reiniciar();
    }

    // ── Emisión ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST → 201: nota EN_TRAMITE con foto de las líneas y comprobante 03 con consecutivo propio, enviado")
    void emitir_201() throws Exception {
        int idFactura = facturaAceptada();
        String claveFactura = claveVigente(idFactura);
        long consecutivoAntes = configHacienda.getConsecutivoNotaCredito();

        MvcResult result = emitir(idFactura, "01", "Cliente devolvió el pedido completo");

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        long idNota = data.get("id").asLong();
        assertThat(data.get("estado").asText()).isEqualTo("EN_TRAMITE");
        String claveNota = data.get("claveComprobante").asText();
        assertThat(claveNota).hasSize(50);
        assertThat(claveNota.substring(29, 31)).as("tipo de comprobante en la clave").isEqualTo("03");

        Map<String, Object> nota = jdbc.queryForMap("SELECT * FROM nota_factura WHERE id = ?", idNota);
        assertThat(nota.get("estado")).isEqualTo("EN_TRAMITE");
        assertThat(nota.get("codigo_referencia")).isEqualTo("01");
        assertThat(nota.get("clave_referencia")).isEqualTo(claveFactura);
        assertThat((BigDecimal) nota.get("total")).isEqualByComparingTo("1630.00");
        assertThat(nota.get("moneda")).isEqualTo("CRC");
        assertThat(nota.get("usuario_creador")).isEqualTo(USUARIO);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM linea_nota_factura WHERE nota_factura_id = ?",
                Integer.class, idNota)).isEqualTo(2);

        Map<String, Object> comp = jdbc.queryForMap("SELECT * FROM comprobante_electronico WHERE clave_numerica = ?",
                claveNota);
        assertThat(comp.get("tipo_comprobante")).isEqualTo("NOTA_CREDITO");
        assertThat(comp.get("factura_id")).isNull();
        assertThat(((Number) comp.get("nota_factura_id")).longValue()).isEqualTo(idNota);
        assertThat((String) comp.get("consecutivo")).isEqualTo(String.format("%020d", consecutivoAntes));
        assertThat(configHacienda.getConsecutivoNotaCredito()).isEqualTo(consecutivoAntes + 1);
        String xml = (String) comp.get("xml_comprobante");
        assertThat(xml).startsWith("<?xml").contains("<NotaCreditoElectronica")
                .contains("<Numero>" + claveFactura + "</Numero>").contains("<Codigo>01</Codigo>")
                .contains("<TotalComprobante>1630.00000</TotalComprobante>").endsWith("<!--firmado-->");
        // Timeout de lectura simulado: el envío queda incierto y la nota sigue en trámite
        assertThat(comp.get("estado")).isEqualTo("ENVIO_INCIERTO");
        assertThat(data.get("estadoComprobante").asText()).isEqualTo("ENVIO_INCIERTO");
        verify(haciendaApi).enviarComprobante(anyString(), eq(claveNota), anyLong());
    }

    @Test
    @DisplayName("factura inexistente → 404")
    void facturaInexistente_404() throws Exception {
        assertThat(emitir(999_999, "01", "Anulación total").getResponse().getStatus()).isEqualTo(404);
    }

    @ParameterizedTest(name = "código {0}")
    @ValueSource(strings = {"02", "06", "14"})
    @DisplayName("otro código → 400 «disponible en una fase posterior», sin nota")
    void codigoDeFasePosterior_400(String codigo) throws Exception {
        int idFactura = facturaAceptada();

        MvcResult result = emitir(idFactura, codigo, "Corrección parcial");

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).contains("disponible en una fase posterior");
        assertThat(notas(idFactura)).isZero();
    }

    @Test
    @DisplayName("razón de 2 o de 181 caracteres → 400")
    void razonFueraDeRango_400() throws Exception {
        int idFactura = facturaAceptada();

        assertThat(emitir(idFactura, "01", "ab").getResponse().getStatus()).isEqualTo(400);
        assertThat(emitir(idFactura, "01", "x".repeat(181)).getResponse().getStatus()).isEqualTo(400);
        assertThat(notas(idFactura)).isZero();
    }

    // ── Precondiciones (409) ─────────────────────────────────────────────────

    @ParameterizedTest(name = "comprobante vigente {0}")
    @ValueSource(strings = {"SIN_COMPROBANTE", "GENERADO", "ENVIADO", "PROCESANDO", "ENVIO_INCIERTO", "RECHAZADO"})
    @DisplayName("a) comprobante vigente no ACEPTADO → 409")
    void sinComprobanteAceptado_409(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        if (!"SIN_COMPROBANTE".equals(estado)) {
            comprobanteDeFactura(idFactura, EstadoComprobante.valueOf(estado));
        }

        MvcResult result = emitir(idFactura, "01", "Anulación total");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("ACEPTADO");
        assertThat(notas(idFactura)).isZero();
    }

    @Test
    @DisplayName("b) factura ya anulada → 409")
    void facturaAnulada_409() throws Exception {
        int idFactura = facturaAceptada();
        jdbc.update("UPDATE factura SET anulada = b'1' WHERE id_factura = ?", idFactura);

        MvcResult result = emitir(idFactura, "01", "Anulación total");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("ya está anulada");
    }

    @Test
    @DisplayName("c) factura con una nota EN_TRAMITE o APLICADA → 409")
    void notaEnTramiteOAplicada_409() throws Exception {
        int idFactura = facturaAceptada();
        assertThat(emitir(idFactura, "01", "Primera nota").getResponse().getStatus()).isEqualTo(201);

        MvcResult enTramite = emitir(idFactura, "13", "Segunda nota");
        jdbc.update("UPDATE nota_factura SET estado = 'APLICADA' WHERE factura_id = ?", idFactura);
        MvcResult aplicada = emitir(idFactura, "01", "Tercera nota");

        assertThat(enTramite.getResponse().getStatus()).isEqualTo(409);
        assertThat(enTramite.getResponse().getContentAsString()).contains("en trámite o aplicada");
        assertThat(aplicada.getResponse().getStatus()).isEqualTo(409);
        assertThat(notas(idFactura)).isEqualTo(1);
    }

    @Test
    @DisplayName("d) factura con pagos aplicados → 409 «se habilitará con la nota parcial y el saldo a favor»")
    void facturaConPagos_409() throws Exception {
        int idFactura = facturaAceptada();
        doReturn(List.of(new Pago())).when(pagoService).findPagosValidosByFacturaId(idFactura);

        MvcResult result = emitir(idFactura, "01", "Anulación total");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString())
                .contains("se habilitará con la nota parcial y el saldo a favor");
        assertThat(notas(idFactura)).isZero();
    }

    @Test
    @DisplayName("dos emisiones concurrentes sobre la misma factura crean una sola nota")
    void emisionConcurrente_unaSolaNota() throws Exception {
        int idFactura = facturaAceptada();
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Boolean> emision = () -> {
            salida.await();
            try {
                notaService.emitirNotaCreditoTotal(idFactura, TipoNotaFactura.CREDITO, "01", "Concurrente", USUARIO);
                return true;
            } catch (NotaFacturaConflictoException e) {
                return false;
            }
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = pool.submit(emision);
            Future<Boolean> b = pool.submit(emision);
            salida.countDown();
            List<Boolean> resultados = List.of(a.get(60, TimeUnit.SECONDS), b.get(60, TimeUnit.SECONDS));
            assertThat(resultados).containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }
        assertThat(notas(idFactura)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico c JOIN nota_factura n "
                + "ON n.id = c.nota_factura_id WHERE n.factura_id = ?", Integer.class, idFactura)).isEqualTo(1);
    }

    // ── Estados y efectos ────────────────────────────────────────────────────

    @Test
    @DisplayName("ACEPTADO (01): reversión espejo contabilizada ligada a nota y factura, factura anulada, nota APLICADA; "
            + "un segundo sondeo no repite los efectos")
    void aceptado_aplicaEfectosUnaVez() throws Exception {
        int idFactura = facturaAceptada();
        Long idVenta = asientoVentaVigente(idFactura);
        Nota nota = emitirOk(idFactura, "01", "Anulación total");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));

        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());

        assertThat(estadoComprobante(nota.idComprobante())).isEqualTo("ACEPTADO");
        Map<String, Object> n = jdbc.queryForMap("SELECT estado, fecha_aplicacion FROM nota_factura WHERE id = ?",
                nota.id());
        assertThat(n.get("estado")).isEqualTo("APLICADA");
        assertThat(n.get("fecha_aplicacion")).isNotNull();

        Map<String, Object> f = jdbc.queryForMap("SELECT anulada, motivo_anulacion, fecha_anulacion FROM factura "
                + "WHERE id_factura = ?", idFactura);
        assertThat((Boolean) f.get("anulada")).isTrue();
        assertThat((String) f.get("motivo_anulacion")).contains("Nota de crédito " + nota.id()).contains(nota.clave());
        assertThat(f.get("fecha_anulacion")).isNotNull();

        List<Map<String, Object>> reversiones = jdbc.queryForList("SELECT id, fecha, estado, factura_id, "
                + "nota_factura_id FROM asientos_contables WHERE asiento_revertido_id = ?", idVenta);
        assertThat(reversiones).hasSize(1);
        Map<String, Object> reversion = reversiones.get(0);
        assertThat(reversion.get("estado")).isEqualTo("CONTABILIZADO");
        assertThat(((Number) reversion.get("factura_id")).intValue()).isEqualTo(idFactura);
        assertThat(((Number) reversion.get("nota_factura_id")).longValue()).isEqualTo(nota.id());
        assertThat(reversion.get("fecha").toString()).isEqualTo(LocalDate.now(reloj).toString());
        assertThat(espejo(idVenta, ((Number) reversion.get("id")).longValue())).isTrue();
    }

    @Test
    @DisplayName("ACEPTADO (13): la reversión lleva la fecha del asiento de venta original")
    void aceptado_codigo13_fechaDelOriginal() throws Exception {
        int idFactura = facturaAceptada();
        Long idVenta = asientoVentaVigente(idFactura);
        jdbc.update("UPDATE asientos_contables SET fecha = '2026-10-03' WHERE id = ?", idVenta);
        Nota nota = emitirOk(idFactura, "13", "Error material en el cliente");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));

        comprobanteService.consultarYActualizarEstado(nota.idComprobante());

        assertThat(jdbc.queryForObject("SELECT fecha FROM asientos_contables WHERE asiento_revertido_id = ?",
                java.sql.Date.class, idVenta).toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    @Test
    @DisplayName("RECHAZADO: nota RECHAZADA, sin asiento, la factura sigue igual y admite una nota nueva")
    void rechazado_sinEfectos_admiteOtra() throws Exception {
        int idFactura = facturaAceptada();
        Long idVenta = asientoVentaVigente(idFactura);
        Nota nota = emitirOk(idFactura, "01", "Primera nota");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("rechazado"));

        comprobanteService.consultarYActualizarEstado(nota.idComprobante());

        assertThat(estadoComprobante(nota.idComprobante())).isEqualTo("RECHAZADO");
        assertThat(estadoNota(nota.id())).isEqualTo("RECHAZADA");
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class, idFactura))
                .isFalse();
        assertThat(asientoVentaVigente(idFactura)).isEqualTo(idVenta);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE nota_factura_id IS NOT NULL "
                + "AND factura_id = ?", Integer.class, idFactura)).isZero();

        Nota nueva = emitirOk(idFactura, "01", "Segunda nota");
        assertThat(nueva.id()).isNotEqualTo(nota.id());
        assertThat(estadoNota(nueva.id())).isEqualTo("EN_TRAMITE");
    }

    @Test
    @DisplayName("404 en la verificación de un ENVIO_INCIERTO (pasados 15 min) → NO_TRANSMITIDO; la nota sigue EN_TRAMITE")
    void verificacion404_noTransmitido() throws Exception {
        int idFactura = facturaAceptada();
        Nota nota = emitirOk(idFactura, "01", "Anulación total");
        assertThat(estadoComprobante(nota.idComprobante())).isEqualTo("ENVIO_INCIERTO");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta404());
        reloj.adelantar(Duration.ofMinutes(16));

        int status = mockMvc.perform(post("/api/facturas/electronica/comprobantes/" + nota.idComprobante()
                        + "/consultar-estado-hacienda").with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(200);
        assertThat(estadoComprobante(nota.idComprobante())).isEqualTo("NO_TRANSMITIDO");
        assertThat(estadoNota(nota.id())).isEqualTo("EN_TRAMITE");
    }

    @ParameterizedTest(name = "ind-estado {0}")
    @ValueSource(strings = {"error", "procesando"})
    @DisplayName("ERROR_HACIENDA o PROCESANDO: la nota sigue EN_TRAMITE y la factura sin cambios")
    void otrosEstados_enTramite(String indEstado) throws Exception {
        int idFactura = facturaAceptada();
        Nota nota = emitirOk(idFactura, "01", "Anulación total");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta(indEstado));

        comprobanteService.consultarYActualizarEstado(nota.idComprobante());

        assertThat(estadoNota(nota.id())).isEqualTo("EN_TRAMITE");
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class, idFactura))
                .isFalse();
    }

    @Test
    @DisplayName("envío recibido (ENVIADO) y luego aceptado por el job de sondeo → efectos aplicados")
    void enviadoYAceptadoPorElJob() throws Exception {
        int idFactura = facturaAceptada();
        Nota nota = emitirOk(idFactura, "01", "Anulación total");
        jdbc.update("UPDATE comprobante_electronico SET estado = 'ENVIADO' WHERE id = ?", nota.idComprobante());
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));

        comprobanteService.consultarYActualizarEstado(nota.idComprobante());

        assertThat(estadoNota(nota.id())).isEqualTo("APLICADA");
    }

    @Test
    @DisplayName("factura sin asiento de venta vigente: se omite la reversión y el resto se aplica")
    void sinAsientoDeVenta() throws Exception {
        int idFactura = facturaAceptada();
        Long idVenta = asientoVentaVigente(idFactura);
        // Factura histórica: su asiento de venta ya no está vigente (revertido fuera de la nota)
        tx.executeWithoutResult(s -> asientoContableService.generarAsientoReversion(
                asientoRepository.findById(idVenta).orElseThrow(), "Histórico T-032"));
        assertThat(asientoVentaVigente(idFactura)).isNull();
        Nota nota = emitirOk(idFactura, "01", "Anulación total");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));

        comprobanteService.consultarYActualizarEstado(nota.idComprobante());

        assertThat(estadoNota(nota.id())).isEqualTo("APLICADA");
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class, idFactura))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE nota_factura_id = ?",
                Integer.class, nota.id())).isZero();
    }

    @Test
    @DisplayName("editar después el producto no cambia la foto de la nota")
    void fotoInmutable() throws Exception {
        int idFactura = facturaAceptada();
        Nota nota = emitirOk(idFactura, "01", "Anulación total");
        List<Map<String, Object>> antes = lineasNota(nota.id());

        tx.executeWithoutResult(s -> {
            Producto p = productoRepository.findById(gravado13.getIdProducto()).orElseThrow();
            p.setDescripcion("Descripción cambiada después de la nota");
            p.setCodigoCabys("1111111111111");
            p.setPorcentajeImpuesto(new BigDecimal("4"));
            productoRepository.save(p);
        });

        assertThat(lineasNota(nota.id())).isEqualTo(antes);
        assertThat(antes.get(0).get("descripcion")).isEqualTo("Producto A gravado 13 %");
    }

    @Test
    @DisplayName("con la nota EN_TRAMITE la factura sigue bloqueada: anular directamente → 409 que remite a la nota")
    void notaEnTramite_facturaBloqueada() throws Exception {
        int idFactura = facturaAceptada();
        emitirOk(idFactura, "01", "Anulación total");

        MvcResult result = mockMvc.perform(post("/facturas/anular/" + idFactura).param("motivo", "Directo")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("nota de crédito");
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class, idFactura))
                .isFalse();
    }

    @Test
    @DisplayName("anular el comprobante ACEPTADO directamente ya no lo marca ANULADO")
    void anularComprobanteDirecto_noMarcaAnulado() throws Exception {
        int idFactura = facturaAceptada();
        Long idComprobante = comprobanteRepository.findFirstByFactura_IdFacturaAndVigenteTrue(idFactura)
                .orElseThrow().getId();

        mockMvc.perform(post("/api/facturas/electronica/comprobantes/" + idComprobante + "/anular")
                .param("motivo", "Directo").with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn();

        assertThat(estadoComprobante(idComprobante)).isEqualTo("ACEPTADO");
    }

    // ── Apoyo ────────────────────────────────────────────────────────────────

    record Nota(Long id, Long idComprobante, String clave) {
    }

    private MvcResult emitir(int idFactura, String codigo, String razon) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idFactura", idFactura);
        cuerpo.put("codigoReferencia", codigo);
        cuerpo.put("razon", razon);
        return mockMvc.perform(post("/api/facturas/notas-credito")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andReturn();
    }

    private Nota emitirOk(int idFactura, String codigo, String razon) throws Exception {
        MvcResult result = emitir(idFactura, codigo, razon);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return new Nota(data.get("id").asLong(), data.get("idComprobante").asLong(),
                data.get("claveComprobante").asText());
    }

    /** Factura A (gravado 13 %, 1130) + B (exento, 500) con su asiento de venta y comprobante ACEPTADO real. */
    private int facturaAceptada() throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobanteDeFactura(idFactura, EstadoComprobante.ACEPTADO);
        return idFactura;
    }

    /** Comprobante vigente de la factura con el XML real del generador, en el estado indicado. */
    private void comprobanteDeFactura(int idFactura, EstadoComprobante estado) {
        tx.executeWithoutResult(s -> {
            Factura factura = facturaRepository.findById(idFactura).orElseThrow();
            ComprobanteElectronico c = ComprobanteElectronico.builder()
                    .factura(factura)
                    .empresa(configHacienda.getEmpresa())
                    .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA)
                    .claveNumerica(clave())
                    .consecutivo(String.format("%020d", ThreadLocalRandom.current().nextLong(1, 1_000_000_000L)))
                    .fechaEmision(LocalDateTime.now().minusDays(1).withNano(0))
                    .fechaEnvio(LocalDateTime.now().minusDays(1))
                    .estado(estado)
                    .vigente(true)
                    .build();
            c.setXmlComprobante(xmlGenerator.generarXmlFactura(c) + "<!--firmado-->");
            comprobanteRepository.save(c);
        });
    }

    private int facturaConLineasAyB() throws Exception {
        int idFactura = tx.execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T032 " + UUID.randomUUID());
            cliente.setNumeroIdentificacion("109990999");
            cliente.setEmail("cliente@t032.example");
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T032-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T032");
            factura.setMonedaFE(MonedaFE.CRC);
            factura.setTipoCambio(BigDecimal.ONE);
            factura.setSubtotal(BigDecimal.ZERO);
            factura.setIgv(BigDecimal.ZERO);
            factura.setTotal(BigDecimal.ZERO);
            factura.setAnulada(false);
            return facturaRepository.save(factura).getIdFactura();
        });
        MvcResult result = guardarLineas(List.of(
                linea(-1, idFactura, gravado13, 1, "1130.00"),
                linea(-2, idFactura, exento, 1, "500.00")));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT total FROM factura WHERE id_factura = ?", BigDecimal.class, idFactura))
                .isEqualByComparingTo("1630.00");
        return idFactura;
    }

    private static Map<String, Object> linea(int idLinea, int idFactura, Producto producto, int cantidad, String precio) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("id_factura", idFactura);
        linea.put("id_linea_factura", idLinea);
        linea.put("numero_linea", 1);
        linea.put("id_producto", producto.getIdProducto());
        linea.put("cantidad", cantidad);
        linea.put("precioUnitario", new BigDecimal(precio));
        linea.put("subtotal", new BigDecimal(precio));
        return linea;
    }

    private MvcResult guardarLineas(List<Map<String, Object>> lineas) throws Exception {
        List<Map<String, Object>> numeradas = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            Map<String, Object> l = new LinkedHashMap<>(lineas.get(i));
            l.put("numero_linea", i + 1);
            numeradas.add(l);
        }
        return mockMvc.perform(put("/lineas-factura/actualizar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(numeradas)))
                .andReturn();
    }

    private Long asientoVentaVigente(int idFactura) {
        return tx.execute(s -> {
            var vigentes = asientoRepository.findAsientosVentaVigentes((long) idFactura);
            assertThat(vigentes).hasSizeLessThanOrEqualTo(1);
            return vigentes.isEmpty() ? null : vigentes.get(0).getIdAsiento();
        });
    }

    /** La reversión tiene las mismas cuentas con debe y haber intercambiados, y cuadra. */
    private boolean espejo(Long idOriginal, Long idReversion) {
        String sql = "SELECT cuenta_id, debe, haber FROM detalles_asiento WHERE asiento_id = ? ORDER BY cuenta_id, debe, haber";
        List<Map<String, Object>> original = jdbc.queryForList(sql, idOriginal);
        List<Map<String, Object>> reversion = jdbc.queryForList(sql, idReversion);
        if (original.size() != reversion.size() || original.isEmpty()) {
            return false;
        }
        BigDecimal debe = BigDecimal.ZERO;
        BigDecimal haber = BigDecimal.ZERO;
        for (Map<String, Object> o : original) {
            boolean hallada = reversion.stream().anyMatch(r -> r.get("cuenta_id").equals(o.get("cuenta_id"))
                    && cmp(r.get("debe"), o.get("haber")) && cmp(r.get("haber"), o.get("debe")));
            if (!hallada) {
                return false;
            }
        }
        for (Map<String, Object> r : reversion) {
            debe = debe.add(dec(r.get("debe")));
            haber = haber.add(dec(r.get("haber")));
        }
        return debe.compareTo(haber) == 0 && debe.signum() > 0;
    }

    private static boolean cmp(Object a, Object b) {
        return dec(a).compareTo(dec(b)) == 0;
    }

    private static BigDecimal dec(Object o) {
        return o == null ? BigDecimal.ZERO : (BigDecimal) o;
    }

    private String claveVigente(int idFactura) {
        return jdbc.queryForObject("SELECT clave_numerica FROM comprobante_electronico WHERE factura_id = ? "
                + "AND vigente = b'1'", String.class, idFactura);
    }

    private int notas(int idFactura) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM nota_factura WHERE factura_id = ?", Integer.class, idFactura);
    }

    private String estadoNota(Long id) {
        return jdbc.queryForObject("SELECT estado FROM nota_factura WHERE id = ?", String.class, id);
    }

    private String estadoComprobante(Long id) {
        return jdbc.queryForObject("SELECT estado FROM comprobante_electronico WHERE id = ?", String.class, id);
    }

    private List<Map<String, Object>> lineasNota(Long idNota) {
        return jdbc.queryForList("SELECT numero_linea, descripcion, codigo_cabys, cantidad, precio_unitario, tarifa_iva, "
                + "monto_iva, subtotal, total FROM linea_nota_factura WHERE nota_factura_id = ? ORDER BY numero_linea",
                idNota);
    }

    /** Respuesta del cliente de Hacienda ante un timeout de lectura del POST (ADR-030: ENVIO_INCIERTO). */
    private static RespuestaHaciendaDTO timeoutDeLectura() {
        return RespuestaHaciendaDTO.builder().resultadoEnvio(ResultadoEnvioHacienda.ENVIO_INCIERTO).exitoso(false)
                .mensaje("Read timed out").build();
    }

    private static RespuestaHaciendaDTO consulta(String indEstado) {
        return RespuestaHaciendaDTO.builder().indicadorEstado(indEstado).codigoHttp(200).exitoso(false)
                .codigoMensaje(MensajeHacienda.PROCESANDO).mensaje("ind-estado " + indEstado).build();
    }

    private static RespuestaHaciendaDTO consulta404() {
        return RespuestaHaciendaDTO.builder().documentoNoExiste(true).codigoHttp(404).exitoso(false)
                .codigoMensaje(MensajeHacienda.ERROR).mensaje("Hacienda no tiene el documento (404)").build();
    }

    private static String clave() {
        StringBuilder clave = new StringBuilder("506");
        while (clave.length() < 50) {
            clave.append(ThreadLocalRandom.current().nextInt(10));
        }
        return clave.toString();
    }

    @SuppressWarnings("deprecation")
    private Producto producto(String descripcion, String precio, boolean gravado) {
        Producto p = new Producto();
        p.setCodigo("T32-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDescripcion(descripcion);
        p.setCodigoCabys(gravado ? "1234567890123" : "9876543210987");
        p.setPrecioInstitucional(new BigDecimal(precio));
        p.setPrecioMayorista(new BigDecimal(precio));
        p.setActive(true);
        p.setGravado(gravado);
        p.setPorcentajeImpuesto(gravado ? new BigDecimal("13") : BigDecimal.ZERO);
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
