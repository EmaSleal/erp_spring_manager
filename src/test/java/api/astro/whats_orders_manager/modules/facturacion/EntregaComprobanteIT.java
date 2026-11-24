package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.enums.PeriodoAtribucion;
import api.astro.whats_orders_manager.modules.facturacion.service.AtribucionFiscalService;
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
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.scheduled.ComprobanteReintentosJob;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.FirmaDigitalService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.XmlGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.NotaFacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.repository.ArticuloMaestroRepository;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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

import java.math.BigDecimal;
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
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import api.astro.whats_orders_manager.modules.facturacion.correo.client.MensajeCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.client.ProveedorCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.client.ResultadoEnvioCorreo;
import api.astro.whats_orders_manager.modules.facturacion.correo.config.CorreoProperties;
import api.astro.whats_orders_manager.modules.facturacion.correo.scheduled.EntregaComprobanteJob;
import api.astro.whats_orders_manager.modules.facturacion.correo.service.EntregaComprobanteService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.modules.facturacion.service.PanelFacturacionService;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * T-055 (ADR-070) — envío de comprobantes por correo de punta a punta sobre MySQL: la entrega AUTOMATICO nace en la
 * misma transacción del paso a ACEPTADO por cualquier ruta (job de estados, sondeo, consulta manual, recuperación del
 * ENVIO_INCIERTO; factura y nota), el job las envía con reintentos, tope diario y reclamo sin duplicados, SANDBOX no
 * llega a clientes reales y el reenvío manual exige su permiso y deja auditoría.
 *
 * <p>Base: MySQL 8 en Testcontainers (t055-it-mysql, tmpfs, sin volúmenes). Hacienda y el proveedor de correo están
 * simulados (ninguna prueba sale a la red); el PDF se simula con bytes fijos.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "app.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("T-055 — correo de comprobantes: entregas, job y reenvío (MySQL)")
class EntregaComprobanteIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t055", "t055-it-mysql");

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
    /** T-036: generador, firma y validador XSD son los reales; el certificado es uno de prueba generado con keytool. */
    @MockitoBean private ConfiguracionHaciendaService configuracionHaciendaService;
    @MockitoSpyBean private PagoService pagoService;
    /** T-046: espía para cambiar el parámetro facturacion.notas.atribucion.codigo-15 en una prueba. */
    @MockitoSpyBean private AtribucionFiscalService atribucionFiscal;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private AsientoContableService asientoContableService;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ArticuloMaestroRepository articuloMaestroRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ComprobanteElectronicoRepository comprobanteRepository;
    @Autowired private ComprobanteElectronicoService comprobanteService;
    @Autowired private XmlGeneratorService xmlGenerator;
    @Autowired private FirmaDigitalService firmaDigital;
    @Autowired private NotaFacturaService notaService;
    @Autowired private FacturaService facturaService;
    @Autowired private api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService empresaService;
    @Autowired private RelojAjustable reloj;
    @Autowired private ComprobanteReintentosJob reintentosJob;

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
    @Autowired private api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository configuracionHaciendaRepository;

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
                // T-047 (ADR-054): grupo 4.1.02 y sus dos hojas, con los parámetros explícitos de la nota de crédito
                param(AsientoContableService.CLAVE_NOTA_CREDITO_DEVOLUCIONES,
                        cuenta("9.4.02.001", "Devoluciones sobre ventas", TipoCuenta.INGRESO));
                param(AsientoContableService.CLAVE_NOTA_CREDITO_ANULACIONES_DESCUENTOS,
                        cuenta("9.4.02.002", "Anulaciones, descuentos y rebajas sobre ventas", TipoCuenta.INGRESO));
            }
            ConfiguracionEmpresa empresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc()
                    .orElseGet(() -> ConfiguracionEmpresa.builder().razonSocial("Empresa T055 S.A.").build());
            empresa.setNumeroIdentificacion("3101234567");
            empresa.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_JURIDICA);
            empresa.setCodigoActividad("4773.0");
            empresa.setEmail("facturacion@t055.example");
            empresa.setCodigoProvincia("1");
            empresa.setCanton("01");
            empresa.setDistrito("01");
            empresa.setTelefono("22223333");
            empresa.setOtrasSenas("Avenida Central, San José");
            api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionSembrado.sembrar(jdbc);
            empresa = empresaRepository.save(empresa);
            if (configHacienda == null) {
                // T-037: persistida, porque el consecutivo se toma con bloqueo de su fila (SELECT ... FOR UPDATE).
                // Una sola por clase (la BD del contenedor se comparte entre pruebas): sus consecutivos siguen
                final ConfiguracionEmpresa empresaDeLaConfig = empresa;
                configHacienda = configuracionHaciendaRepository.findByActivaTrueOrderByIdAsc().stream().findFirst()
                        .orElseGet(() -> configuracionHaciendaRepository.save(ConfiguracionHacienda.builder().empresa(empresaDeLaConfig)
                        .ambiente(api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda.SANDBOX)
                        .usuarioHacienda("cpj-3-101-234567@stag.comprobanteselectronicos.go.cr")
                        .passwordHacienda("clave-de-prueba").activa(true)
                        .rutaCertificado(CertificadoDePrueba.ruta()).pinCertificado(CertificadoDePrueba.PIN).build()));
            }
            Long idConfigHacienda = configHacienda.getId();
            // La configuración activa se lee de la BD en cada llamada, como el servicio real
            when(configuracionHaciendaService.obtenerConfiguracionActiva())
                    .thenAnswer(inv -> configuracionHaciendaRepository.findById(idConfigHacienda).orElseThrow());
            gravado13 = producto("Producto A gravado 13 %", "1130.00", true);
            exento = producto("Producto B exento", "500.00", false);
        });
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(timeoutDeLectura());
        prepararCorreo();
    }

    @AfterEach
    void tearDown() {
        reloj.reiniciar();
        restaurarCorreo();
    }


    // ── Proveedor simulado y propiedades ─────────────────────────────────────

    /** Proveedor de correo simulado: registra cada mensaje y responde según la cola (éxito si está vacía). */
    static class ProveedorSimulado implements ProveedorCorreo {
        final List<MensajeCorreo> enviados = new CopyOnWriteArrayList<>();
        final Deque<ResultadoEnvioCorreo> respuestas = new ConcurrentLinkedDeque<>();
        final AtomicInteger contador = new AtomicInteger();
        volatile boolean configurado = true;
        volatile long demoraMs = 0;

        void reiniciar() {
            enviados.clear();
            respuestas.clear();
            contador.set(0);
            configurado = true;
            demoraMs = 0;
        }

        @Override
        public ResultadoEnvioCorreo enviar(MensajeCorreo mensaje) {
            enviados.add(mensaje);
            if (demoraMs > 0) {
                try {
                    Thread.sleep(demoraMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ResultadoEnvioCorreo programada = respuestas.poll();
            return programada != null ? programada : ResultadoEnvioCorreo.exito("prov-" + contador.incrementAndGet());
        }

        @Override
        public boolean configurado() {
            return configurado;
        }
    }

    @TestConfiguration
    static class CorreoDePrueba {
        @Bean
        @Primary
        ProveedorSimulado proveedorSimulado() {
            return new ProveedorSimulado();
        }
    }

    @Autowired private ProveedorSimulado proveedor;
    @Autowired private CorreoProperties correo;
    @Autowired private EntregaComprobanteJob jobCorreo;
    @Autowired private EntregaComprobanteService entregaService;
    @Autowired private PanelFacturacionService panelService;
    @Autowired private EnvioHaciendaService envioHaciendaService;
    @MockitoBean private FacturaPdfService pdfService;

    private static final String EMAIL_CLIENTE = "cliente@t055.example";
    private static final String PRUEBA = "prueba@monrachem.example";

    /** Estado limpio del correo para cada prueba (la BD de la clase se comparte): se llama al final de setUp. */
    void prepararCorreo() {
        jdbc.update("DELETE FROM entrega_comprobante");
        jdbc.update("UPDATE comprobante_electronico SET estado = 'ACEPTADO' WHERE estado IN ('GENERADO', 'FIRMADO', "
                + "'ERROR', 'NO_TRANSMITIDO', 'ENVIADO', 'PROCESANDO', 'ERROR_HACIENDA', 'ENVIO_INCIERTO')");
        activar(AmbienteHacienda.SANDBOX);
        proveedor.reiniciar();
        correo.getComprobantes().setHabilitado(true);
        correo.setRemitente("Monrachem <no-reply@t055.example>");
        correo.setReplyTo("");
        correo.setCco("");
        correo.getSandbox().setDestinatarioPrueba("");
        correo.setTopeDiario(100);
        correo.getReintentos().setMaximo(3);
        correo.getReintentos().setEsperaInicial(Duration.ofMinutes(5));
        correo.getJob().setLote(20);
        correo.getJob().setReserva(Duration.ofMinutes(10));
        when(pdfService.generarPdfFactura(any())).thenReturn("PDF-FACTURA".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(pdfService.generarPdfNotaCredito(any())).thenReturn("PDF-NOTA".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    void restaurarCorreo() {
        activar(AmbienteHacienda.SANDBOX);
        correo.getComprobantes().setHabilitado(false);
        correo.getSandbox().setDestinatarioPrueba("");
        correo.setCco("");
        correo.setReplyTo("");
        proveedor.reiniciar();
    }

    private void activar(AmbienteHacienda ambiente) {
        jdbc.update("UPDATE configuracion_hacienda SET ambiente = ? WHERE id = ?", ambiente.name(), configHacienda.getId());
    }

    // ── Creación de la entrega por cada ruta de aceptación (criterio 5) ──────

    record Doc(int idFactura, Long idComprobante, String clave) {
    }

    /** Factura (A + B) con su comprobante vigente en el estado y ambiente indicados (enviado hace un día). */
    private Doc facturaCon(EstadoComprobante estado, AmbienteHacienda ambiente) throws Exception {
        int idFactura = facturaConLineasAyB();
        Long idComprobante = comprobanteDeFactura(idFactura, estado, ambiente);
        return new Doc(idFactura, idComprobante, claveDe(idComprobante));
    }

    private String claveDe(Long idComprobante) {
        return jdbc.queryForObject("SELECT clave_numerica FROM comprobante_electronico WHERE id = ?", String.class,
                idComprobante);
    }

    private void haciendaResponde(String clave, String indEstado) {
        when(haciendaApi.consultarComprobante(eq(clave), anyLong())).thenReturn(consulta(indEstado));
    }

    private List<Map<String, Object>> entregas(Long idComprobante) {
        return jdbc.queryForList("SELECT * FROM entrega_comprobante WHERE comprobante_id = ? ORDER BY id", idComprobante);
    }

    private Map<String, Object> unica(Long idComprobante) {
        List<Map<String, Object>> filas = entregas(idComprobante);
        assertThat(filas).as("entregas del comprobante %s", idComprobante).hasSize(1);
        return filas.get(0);
    }

    private Map<String, Object> entrega(Long id) {
        return jdbc.queryForMap("SELECT * FROM entrega_comprobante WHERE id = ?", id);
    }

    private static LocalDateTime ldt(Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof java.sql.Timestamp t ? t.toLocalDateTime() : (LocalDateTime) valor;
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }

    @Test
    @DisplayName("5 (job de estados): ENVIADO → ACEPTADO crea una entrega AUTOMATICO PENDIENTE al correo del receptor, en la misma transacción")
    void aceptacionPorElJobDeEstados_factura() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        Doc f = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.PRODUCCION);
        haciendaResponde(f.clave(), "aceptado");

        reintentosJob.consultarEstadosPendientes();

        assertThat(estadoComprobante(f.idComprobante())).isEqualTo("ACEPTADO");
        Map<String, Object> e = unica(f.idComprobante());
        assertThat(e.get("origen")).isEqualTo("AUTOMATICO");
        assertThat(e.get("estado")).isEqualTo("PENDIENTE");
        assertThat(e.get("tipo_documento")).isEqualTo("FACTURA");
        assertThat(e.get("destinatario")).isEqualTo(EMAIL_CLIENTE);
        assertThat(e.get("usuario_id")).isNull();
        assertThat(((Number) e.get("intentos")).intValue()).isZero();
        assertThat(ldt(e.get("proximo_intento"))).isNotNull().isBeforeOrEqualTo(ahora());
        assertThat(ldt(e.get("fecha_creacion"))).isNotNull();
    }

    @Test
    @DisplayName("5 (recuperación): un ENVIO_INCIERTO recuperado a ACEPTADO por el job también crea la entrega")
    void aceptacionPorRecuperacionDeEnvioIncierto() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        Doc f = facturaCon(EstadoComprobante.ENVIO_INCIERTO, AmbienteHacienda.PRODUCCION);
        haciendaResponde(f.clave(), "aceptado");

        reintentosJob.consultarEstadosPendientes();

        assertThat(estadoComprobante(f.idComprobante())).isEqualTo("ACEPTADO");
        assertThat(unica(f.idComprobante()).get("estado")).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("5 (sondeo en segundo plano): EnvioHaciendaServiceImpl.consultar → ACEPTADO crea la entrega")
    void aceptacionPorElSondeoEnSegundoPlano() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        Doc f = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.PRODUCCION);
        haciendaResponde(f.clave(), "aceptado");
        Method consultar = envioHaciendaService.getClass().getDeclaredMethod("consultar", Long.class, int.class);
        consultar.setAccessible(true);

        consultar.invoke(envioHaciendaService, f.idComprobante(), 1);

        assertThat(estadoComprobante(f.idComprobante())).isEqualTo("ACEPTADO");
        assertThat(unica(f.idComprobante()).get("estado")).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("5 («Consultar estado en Hacienda»): consultarEstadoManual → ACEPTADO crea la entrega")
    void aceptacionPorLaConsultaManual() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        Doc f = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.PRODUCCION);
        haciendaResponde(f.clave(), "aceptado");

        comprobanteService.consultarEstadoManual(f.idComprobante());

        assertThat(estadoComprobante(f.idComprobante())).isEqualTo("ACEPTADO");
        assertThat(unica(f.idComprobante()).get("destinatario")).isEqualTo(EMAIL_CLIENTE);
    }

    @Test
    @DisplayName("5 (nota de crédito, job con ENVIO_INCIERTO): la nota aceptada crea su entrega NOTA_CREDITO al correo del receptor de la factura")
    void aceptacionDeNota_porElJob() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        Nota nota = emitirOk(f.idFactura(), "01", "Anulación total T-055");
        assertThat(entregas(nota.idComprobante())).isEmpty();
        haciendaResponde(nota.clave(), "aceptado");
        reloj.adelantar(Duration.ofMinutes(16));

        reintentosJob.consultarEstadosPendientes();

        assertThat(estadoComprobante(nota.idComprobante())).isEqualTo("ACEPTADO");
        Map<String, Object> e = unica(nota.idComprobante());
        assertThat(e.get("tipo_documento")).isEqualTo("NOTA_CREDITO");
        assertThat(e.get("origen")).isEqualTo("AUTOMATICO");
        assertThat(e.get("estado")).isEqualTo("PENDIENTE");
        assertThat(e.get("destinatario")).isEqualTo(EMAIL_CLIENTE);
    }

    @Test
    @DisplayName("5 (nota de crédito, sondeo y consulta manual): consultarYActualizarEstado y consultarEstadoManual también la crean")
    void aceptacionDeNota_porSondeoYConsultaManual() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        Doc f1 = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        Nota n1 = emitirOk(f1.idFactura(), "01", "Anulación total sondeo");
        haciendaResponde(n1.clave(), "aceptado");
        comprobanteService.consultarYActualizarEstado(n1.idComprobante());
        assertThat(unica(n1.idComprobante()).get("tipo_documento")).isEqualTo("NOTA_CREDITO");

        Doc f2 = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        Nota n2 = emitirOk(f2.idFactura(), "01", "Anulación total manual");
        haciendaResponde(n2.clave(), "aceptado");
        comprobanteService.consultarEstadoManual(n2.idComprobante());
        assertThat(unica(n2.idComprobante()).get("estado")).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("5: un comprobante RECHAZADO (factura o nota) o PROCESANDO no genera entrega")
    void rechazadoYOtrosEstados_noGeneranEntrega() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        Doc rechazada = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.PRODUCCION);
        haciendaResponde(rechazada.clave(), "rechazado");
        Doc procesando = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.PRODUCCION);
        haciendaResponde(procesando.clave(), "procesando");
        Doc base = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        Nota nota = emitirOk(base.idFactura(), "01", "Anulación rechazada");
        haciendaResponde(nota.clave(), "rechazado");

        reloj.adelantar(Duration.ofMinutes(16));
        reintentosJob.consultarEstadosPendientes();

        assertThat(estadoComprobante(rechazada.idComprobante())).isEqualTo("RECHAZADO");
        assertThat(estadoComprobante(procesando.idComprobante())).isEqualTo("PROCESANDO");
        assertThat(estadoComprobante(nota.idComprobante())).isEqualTo("RECHAZADO");
        assertThat(entregas(rechazada.idComprobante())).isEmpty();
        assertThat(entregas(procesando.idComprobante())).isEmpty();
        assertThat(entregas(nota.idComprobante())).isEmpty();
    }

    @Test
    @DisplayName("5 (idempotencia): una segunda aceptación o una segunda llamada no crean otra entrega; el índice único es la última defensa")
    void idempotencia() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        Doc f = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.PRODUCCION);
        haciendaResponde(f.clave(), "aceptado");
        comprobanteService.consultarEstadoManual(f.idComprobante());
        comprobanteService.consultarYActualizarEstado(f.idComprobante());
        reintentosJob.consultarEstadosPendientes();
        assertThat(entregas(f.idComprobante())).hasSize(1);

        tx.executeWithoutResult(s -> entregaService.registrarAutomatica(
                comprobanteRepository.findById(f.idComprobante()).orElseThrow()));
        tx.executeWithoutResult(s -> entregaService.registrarAutomatica(
                comprobanteRepository.findById(f.idComprobante()).orElseThrow()));
        assertThat(entregas(f.idComprobante())).hasSize(1);

        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> insertarEntrega(f.idComprobante(), "AUTOMATICO", "PENDIENTE", "otro@t055.example", 0, ahora()));
        insertarEntrega(f.idComprobante(), "MANUAL", "PENDIENTE", "otro@t055.example", 0, ahora());
        assertThat(entregas(f.idComprobante())).hasSize(2);
    }

    @Test
    @DisplayName("5: registrarAutomatica exige una transacción abierta (la misma del cambio de estado)")
    void registrarAutomatica_exigeTransaccion() throws Exception {
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        ComprobanteElectronico c = tx.execute(s -> comprobanteRepository.findById(f.idComprobante()).orElseThrow());

        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                () -> entregaService.registrarAutomatica(c));
    }

    @Test
    @DisplayName("5 (envío deshabilitado): la entrega igual se registra PENDIENTE; el job no envía ni consume intentos; al habilitar se envía el pendiente")
    void envioDeshabilitado_registraPeroNoEnvia() throws Exception {
        correo.getComprobantes().setHabilitado(false);
        activar(AmbienteHacienda.PRODUCCION);
        Doc f = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.PRODUCCION);
        haciendaResponde(f.clave(), "aceptado");
        comprobanteService.consultarEstadoManual(f.idComprobante());
        Map<String, Object> antes = unica(f.idComprobante());
        assertThat(antes.get("estado")).isEqualTo("PENDIENTE");

        jobCorreo.procesarPendientes();
        jobCorreo.procesarPendientes();

        assertThat(proveedor.enviados).isEmpty();
        Map<String, Object> despues = entrega(((Number) antes.get("id")).longValue());
        assertThat(despues.get("estado")).isEqualTo("PENDIENTE");
        assertThat(((Number) despues.get("intentos")).intValue()).isZero();
        assertThat(ldt(despues.get("proximo_intento"))).isEqualTo(ldt(antes.get("proximo_intento")));

        correo.getComprobantes().setHabilitado(true);
        jobCorreo.procesarPendientes();
        assertThat(proveedor.enviados).hasSize(1);
        assertThat(entrega(((Number) antes.get("id")).longValue()).get("estado")).isEqualTo("ENVIADO");
    }

    // ── Destino: sin correo y SANDBOX (criterios 7 y 8) ──────────────────────

    @Test
    @DisplayName("7: el receptor sin correo (nulo, vacío o inválido) deja la entrega OMITIDO_SIN_CORREO y no se envía nada")
    void sinCorreo() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        for (String email : new String[] {null, "   ", "no-es-un-correo"}) {
            Doc f = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.PRODUCCION);
            jdbc.update("UPDATE cliente SET email = ? WHERE id_cliente = (SELECT id_cliente FROM factura WHERE id_factura = ?)",
                    email, f.idFactura());
            haciendaResponde(f.clave(), "aceptado");

            comprobanteService.consultarEstadoManual(f.idComprobante());

            Map<String, Object> e = unica(f.idComprobante());
            assertThat(e.get("estado")).as("correo '%s'", email).isEqualTo("OMITIDO_SIN_CORREO");
            assertThat(e.get("origen")).isEqualTo("AUTOMATICO");
        }
        jobCorreo.procesarPendientes();
        assertThat(proveedor.enviados).isEmpty();
    }

    @Test
    @DisplayName("8: comprobante SANDBOX sin destinatario de prueba → OMITIDO_SANDBOX, sin destinatario, y nunca se envía al receptor")
    void sandboxSinDestinatarioDePrueba() throws Exception {
        Doc f = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.SANDBOX);
        haciendaResponde(f.clave(), "aceptado");

        comprobanteService.consultarEstadoManual(f.idComprobante());
        jobCorreo.procesarPendientes();

        Map<String, Object> e = unica(f.idComprobante());
        assertThat(e.get("estado")).isEqualTo("OMITIDO_SANDBOX");
        assertThat(e.get("destinatario")).isNull();
        assertThat(proveedor.enviados).isEmpty();
    }

    @Test
    @DisplayName("8: comprobante SANDBOX con destinatario de prueba → se envía solo a esa dirección con el prefijo [SANDBOX]; nunca al receptor")
    void sandboxConDestinatarioDePrueba() throws Exception {
        correo.getSandbox().setDestinatarioPrueba(PRUEBA);
        correo.setCco("archivo@t055.example");
        Doc f = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.SANDBOX);
        haciendaResponde(f.clave(), "aceptado");

        comprobanteService.consultarEstadoManual(f.idComprobante());
        assertThat(unica(f.idComprobante()).get("destinatario")).isEqualTo(PRUEBA);
        jobCorreo.procesarPendientes();

        assertThat(proveedor.enviados).hasSize(1);
        MensajeCorreo m = proveedor.enviados.get(0);
        assertThat(m.destinatario()).isEqualTo(PRUEBA);
        assertThat(m.asunto()).startsWith("[SANDBOX] ");
        assertThat(m.toString()).doesNotContain(EMAIL_CLIENTE);
        assertThat(m.html()).doesNotContain(EMAIL_CLIENTE);
        assertThat(unica(f.idComprobante()).get("estado")).isEqualTo("ENVIADO");
    }

    @Test
    @DisplayName("8 (defensa al enviar): una entrega PENDIENTE de un comprobante SANDBOX dirigida a otra dirección que la de prueba pasa a OMITIDO_SANDBOX")
    void sandboxDefensaEnElEnvio() throws Exception {
        correo.getSandbox().setDestinatarioPrueba(PRUEBA);
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.SANDBOX);
        Long id = insertarEntrega(f.idComprobante(), "MANUAL", "PENDIENTE", EMAIL_CLIENTE, 0, ahora().minusMinutes(1));

        jobCorreo.procesarPendientes();

        assertThat(proveedor.enviados).isEmpty();
        assertThat(entrega(id).get("estado")).isEqualTo("OMITIDO_SANDBOX");
    }

    // ── El job (criterio 6) y el contenido (criterio 7) ──────────────────────

    /** Entrega PENDIENTE vencida de un comprobante PRODUCCION aceptado, con el correo del receptor. */
    private Long entregaPendienteDeProduccion() throws Exception {
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        return insertarEntrega(f.idComprobante(), "AUTOMATICO", "PENDIENTE", EMAIL_CLIENTE, 0, ahora().minusMinutes(1));
    }

    private Long insertarEntrega(Long idComprobante, String origen, String estado, String destinatario, int intentos,
                                 LocalDateTime proximoIntento) {
        jdbc.update("INSERT INTO entrega_comprobante (comprobante_id, tipo_documento, origen, destinatario, estado, "
                        + "intentos, proximo_intento, fecha_creacion, clave_automatica) VALUES (?, 'FACTURA', ?, ?, ?, ?, ?, ?, ?)",
                idComprobante, origen, destinatario, estado, intentos, proximoIntento, ahora(),
                "AUTOMATICO".equals(origen) ? idComprobante : null);
        return jdbc.queryForObject("SELECT MAX(id) FROM entrega_comprobante", Long.class);
    }

    @Test
    @DisplayName("6/7: éxito → ENVIADO con el id del proveedor; mensaje con remitente, receptor, CCO, asunto, HTML y los 3 adjuntos con la clave")
    void exito_yContenidoDelMensaje() throws Exception {
        correo.setCco("archivo@t055.example, contabilidad@t055.example");
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        jdbc.update("UPDATE comprobante_electronico SET xml_respuesta = '<MensajeHacienda>ok</MensajeHacienda>' WHERE id = ?",
                f.idComprobante());
        jdbc.update("UPDATE cliente SET nombre = '<b>Ana</b> & Cía' WHERE id_cliente = "
                + "(SELECT id_cliente FROM factura WHERE id_factura = ?)", f.idFactura());
        Long id = insertarEntrega(f.idComprobante(), "AUTOMATICO", "PENDIENTE", EMAIL_CLIENTE, 0, ahora().minusMinutes(1));

        jobCorreo.procesarPendientes();

        assertThat(proveedor.enviados).hasSize(1);
        MensajeCorreo m = proveedor.enviados.get(0);
        assertThat(m.remitente()).isEqualTo("Monrachem <no-reply@t055.example>");
        assertThat(m.destinatario()).isEqualTo(EMAIL_CLIENTE);
        assertThat(m.cco()).containsExactly("archivo@t055.example", "contabilidad@t055.example");
        assertThat(m.replyTo()).isNull();
        String consecutivo = jdbc.queryForObject("SELECT consecutivo FROM comprobante_electronico WHERE id = ?", String.class,
                f.idComprobante());
        String emisor = jdbc.queryForObject("SELECT razon_social FROM configuracion_empresa ORDER BY id_configuracion LIMIT 1",
                String.class);
        assertThat(m.asunto()).isEqualTo("Factura electrónica " + consecutivo + " - " + emisor);
        assertThat(m.html()).contains("&lt;b&gt;Ana&lt;/b&gt;").doesNotContain("<b>Ana</b>").contains(f.clave())
                .contains("1630.00").contains("Este correo no recibe respuestas");
        assertThat(m.adjuntos()).extracting(a -> a.nombre()).containsExactly(f.clave() + ".pdf", f.clave() + ".xml",
                f.clave() + "-respuesta-hacienda.xml");
        assertThat(new String(m.adjuntos().get(0).contenido())).isEqualTo("PDF-FACTURA");
        assertThat(new String(m.adjuntos().get(1).contenido())).contains("<FacturaElectronica");
        assertThat(new String(m.adjuntos().get(2).contenido())).isEqualTo("<MensajeHacienda>ok</MensajeHacienda>");

        Map<String, Object> e = entrega(id);
        assertThat(e.get("estado")).isEqualTo("ENVIADO");
        assertThat(e.get("id_proveedor")).isEqualTo("prov-1");
        assertThat(((Number) e.get("intentos")).intValue()).isEqualTo(1);
        assertThat(ldt(e.get("fecha_envio"))).isNotNull();
        assertThat(e.get("ultimo_error")).isNull();
    }

    @Test
    @DisplayName("7: con Reply-To configurado el mensaje lo lleva y el cuerpo no dice «no recibe respuestas»; sin respuesta de Hacienda solo hay 2 adjuntos")
    void replyToYSinRespuestaDeHacienda() throws Exception {
        correo.setReplyTo("ventas@t055.example");
        Long id = entregaPendienteDeProduccion();

        jobCorreo.procesarPendientes();

        MensajeCorreo m = proveedor.enviados.get(0);
        assertThat(m.replyTo()).isEqualTo("ventas@t055.example");
        assertThat(m.html()).doesNotContain("no recibe respuestas");
        assertThat(m.adjuntos()).hasSize(2);
        assertThat(entrega(id).get("estado")).isEqualTo("ENVIADO");
    }

    @Test
    @DisplayName("7: el PDF y el XML de una nota de crédito son los de la nota (asunto «Nota de crédito electrónica»)")
    void contenidoDeNota() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        Nota nota = emitirOk(f.idFactura(), "01", "Anulación para correo");
        haciendaResponde(nota.clave(), "aceptado");
        comprobanteService.consultarEstadoManual(nota.idComprobante());

        jobCorreo.procesarPendientes();

        assertThat(proveedor.enviados).hasSize(1);
        MensajeCorreo m = proveedor.enviados.get(0);
        assertThat(m.asunto()).startsWith("Nota de crédito electrónica ");
        assertThat(m.adjuntos().get(0).nombre()).isEqualTo(nota.clave() + ".pdf");
        assertThat(new String(m.adjuntos().get(0).contenido())).isEqualTo("PDF-NOTA");
        assertThat(m.html()).contains("1630.00");
    }

    @Test
    @DisplayName("6: error reintentable → espera creciente (5 min, 10 min); al llegar al tope de intentos pasa a FALLIDO")
    void reintentoConEsperaCreciente_yTope() throws Exception {
        Long id = entregaPendienteDeProduccion();
        proveedor.respuestas.add(ResultadoEnvioCorreo.reintentable("HTTP 503 de Resend"));

        jobCorreo.procesarPendientes();

        Map<String, Object> e1 = entrega(id);
        assertThat(e1.get("estado")).isEqualTo("PENDIENTE");
        assertThat(((Number) e1.get("intentos")).intValue()).isEqualTo(1);
        assertThat((String) e1.get("ultimo_error")).contains("503");
        assertThat(ldt(e1.get("proximo_intento"))).isBetween(ahora().plusMinutes(4), ahora().plusMinutes(6));

        jobCorreo.procesarPendientes();
        assertThat(proveedor.enviados).as("todavía no vence la espera").hasSize(1);

        reloj.adelantar(Duration.ofMinutes(6));
        proveedor.respuestas.add(ResultadoEnvioCorreo.reintentable("timeout"));
        jobCorreo.procesarPendientes();
        Map<String, Object> e2 = entrega(id);
        assertThat(((Number) e2.get("intentos")).intValue()).isEqualTo(2);
        assertThat(e2.get("estado")).isEqualTo("PENDIENTE");
        assertThat(ldt(e2.get("proximo_intento"))).isBetween(ahora().plusMinutes(9), ahora().plusMinutes(11));

        reloj.adelantar(Duration.ofMinutes(11));
        proveedor.respuestas.add(ResultadoEnvioCorreo.reintentable("timeout otra vez"));
        jobCorreo.procesarPendientes();
        Map<String, Object> e3 = entrega(id);
        assertThat(e3.get("estado")).isEqualTo("FALLIDO");
        assertThat(((Number) e3.get("intentos")).intValue()).isEqualTo(3);
        assertThat((String) e3.get("ultimo_error")).contains("timeout otra vez");

        reloj.adelantar(Duration.ofDays(1));
        jobCorreo.procesarPendientes();
        assertThat(proveedor.enviados).as("una FALLIDO no se reintenta sola").hasSize(3);
    }

    @Test
    @DisplayName("6: 429 → se reprograma para mañana sin consumir intentos y el resto de las vencidas también se difiere")
    void limiteDelProveedor_mananaSinConsumirIntentos() throws Exception {
        Long a = entregaPendienteDeProduccion();
        Long b = entregaPendienteDeProduccion();
        proveedor.respuestas.add(ResultadoEnvioCorreo.limite("HTTP 429"));

        jobCorreo.procesarPendientes();

        assertThat(proveedor.enviados).as("tras el 429 se detiene la corrida").hasSize(1);
        LocalDateTime manana = LocalDate.now(reloj).plusDays(1).atStartOfDay();
        for (Long id : List.of(a, b)) {
            Map<String, Object> e = entrega(id);
            assertThat(e.get("estado")).isEqualTo("PENDIENTE");
            assertThat(((Number) e.get("intentos")).intValue()).isZero();
            assertThat(ldt(e.get("proximo_intento"))).isEqualTo(manana);
        }
    }

    @Test
    @DisplayName("6: tope diario alcanzado (ENVIADO del día) → las vencidas pasan a mañana sin consumir intentos y no se envía")
    void topeDiarioAlcanzado() throws Exception {
        correo.setTopeDiario(1);
        Long enviada = entregaPendienteDeProduccion();
        jdbc.update("UPDATE entrega_comprobante SET estado = 'ENVIADO', fecha_envio = ?, intentos = 1 WHERE id = ?",
                ahora(), enviada);
        Long pendiente = entregaPendienteDeProduccion();

        jobCorreo.procesarPendientes();

        assertThat(proveedor.enviados).isEmpty();
        Map<String, Object> e = entrega(pendiente);
        assertThat(e.get("estado")).isEqualTo("PENDIENTE");
        assertThat(((Number) e.get("intentos")).intValue()).isZero();
        assertThat(ldt(e.get("proximo_intento"))).isEqualTo(LocalDate.now(reloj).plusDays(1).atStartOfDay());
    }

    @Test
    @DisplayName("6: el tope se respeta dentro de la corrida (tope 2, 3 vencidas → 2 enviadas y 1 para mañana); un ENVIADO de ayer no cuenta")
    void topeDiarioDentroDeLaCorrida() throws Exception {
        correo.setTopeDiario(2);
        Long ayer = entregaPendienteDeProduccion();
        jdbc.update("UPDATE entrega_comprobante SET estado = 'ENVIADO', fecha_envio = ?, intentos = 1 WHERE id = ?",
                LocalDate.now(reloj).atStartOfDay().minusMinutes(5), ayer);
        List<Long> ids = List.of(entregaPendienteDeProduccion(), entregaPendienteDeProduccion(), entregaPendienteDeProduccion());

        jobCorreo.procesarPendientes();

        assertThat(proveedor.enviados).hasSize(2);
        long enviadas = ids.stream().filter(i -> "ENVIADO".equals(entrega(i).get("estado"))).count();
        assertThat(enviadas).isEqualTo(2);
        Map<String, Object> restante = ids.stream().map(this::entrega).filter(e -> "PENDIENTE".equals(e.get("estado")))
                .findFirst().orElseThrow();
        assertThat(((Number) restante.get("intentos")).intValue()).isZero();
        assertThat(ldt(restante.get("proximo_intento"))).isEqualTo(LocalDate.now(reloj).plusDays(1).atStartOfDay());
    }

    @Test
    @DisplayName("6: error definitivo (4xx de validación) → FALLIDO con el error guardado y sin más reintentos")
    void errorDefinitivo() throws Exception {
        Long id = entregaPendienteDeProduccion();
        proveedor.respuestas.add(ResultadoEnvioCorreo.definitivo("HTTP 422: Invalid `to` field"));

        jobCorreo.procesarPendientes();

        Map<String, Object> e = entrega(id);
        assertThat(e.get("estado")).isEqualTo("FALLIDO");
        assertThat(((Number) e.get("intentos")).intValue()).isEqualTo(1);
        assertThat((String) e.get("ultimo_error")).contains("422");
        reloj.adelantar(Duration.ofDays(2));
        jobCorreo.procesarPendientes();
        assertThat(proveedor.enviados).hasSize(1);
    }

    @Test
    @DisplayName("6: el último error se trunca a 500 caracteres")
    void ultimoErrorTruncado() throws Exception {
        Long id = entregaPendienteDeProduccion();
        proveedor.respuestas.add(ResultadoEnvioCorreo.definitivo("x".repeat(2000)));

        jobCorreo.procesarPendientes();

        assertThat(((String) entrega(id).get("ultimo_error")).length()).isLessThanOrEqualTo(500);
    }

    @Test
    @DisplayName("6: un fallo al armar el mensaje (PDF) cuenta como error reintentable")
    void falloAlArmarElMensaje() throws Exception {
        Long id = entregaPendienteDeProduccion();
        when(pdfService.generarPdfFactura(any())).thenThrow(new IllegalStateException("fallo del PDF"));

        jobCorreo.procesarPendientes();

        assertThat(proveedor.enviados).isEmpty();
        Map<String, Object> e = entrega(id);
        assertThat(e.get("estado")).isEqualTo("PENDIENTE");
        assertThat(((Number) e.get("intentos")).intValue()).isEqualTo(1);
        assertThat((String) e.get("ultimo_error")).contains("fallo del PDF");
    }

    @Test
    @DisplayName("3: habilitado pero sin clave/remitente (proveedor no configurado) → el job no envía ni consume intentos")
    void habilitadoPeroNoConfigurado() throws Exception {
        Long id = entregaPendienteDeProduccion();
        proveedor.configurado = false;
        LocalDateTime antes = ldt(entrega(id).get("proximo_intento"));

        jobCorreo.procesarPendientes();

        assertThat(proveedor.enviados).isEmpty();
        Map<String, Object> e = entrega(id);
        assertThat(e.get("estado")).isEqualTo("PENDIENTE");
        assertThat(((Number) e.get("intentos")).intValue()).isZero();
        assertThat(ldt(e.get("proximo_intento"))).isEqualTo(antes);
    }

    @Test
    @DisplayName("6 (concurrencia): dos hilos corriendo el job a la vez sobre las mismas entregas vencidas envían cada una exactamente una vez")
    void concurrencia_cadaEntregaSeEnviaUnaVez() throws Exception {
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ids.add(insertarEntrega(f.idComprobante(), "MANUAL", "PENDIENTE", "dest" + i + "@t055.example", 0,
                    ahora().minusMinutes(1)));
        }
        proveedor.demoraMs = 40;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> corridas = new ArrayList<>();
            for (int h = 0; h < 2; h++) {
                corridas.add(pool.submit(() -> {
                    salida.await();
                    jobCorreo.procesarPendientes();
                    return null;
                }));
            }
            salida.countDown();
            for (Future<?> c : corridas) {
                c.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(proveedor.enviados).extracting(MensajeCorreo::destinatario)
                .as("cada destinatario recibe exactamente un mensaje").doesNotHaveDuplicates().hasSize(8);
        for (Long id : ids) {
            Map<String, Object> e = entrega(id);
            assertThat(e.get("estado")).isEqualTo("ENVIADO");
            assertThat(((Number) e.get("intentos")).intValue()).isEqualTo(1);
        }
    }

    // ── Reenvío manual y consulta (criterio 9) ───────────────────────────────

    private static final String API_COMP = "/api/facturas/electronica/comprobantes/";

    private MvcResult reenviar(Long idComprobante, String cuerpo, String usuario, String rol) throws Exception {
        return mockMvc.perform(post(API_COMP + idComprobante + "/entregas").with(user(usuario).roles(rol)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    @Test
    @DisplayName("9: un usuario sin COMPROBANTE_REENVIAR_CORREO recibe 403 y no se crea la entrega")
    void reenvioManual_sinPermiso403() throws Exception {
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);

        MvcResult r = reenviar(f.idComprobante(), "{\"destinatario\":\"a@b.co\"}", "sin.permiso@example.invalid", "USER");

        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(entregas(f.idComprobante())).isEmpty();
    }

    @Test
    @DisplayName("9: con el permiso (ADMIN) → 201 con una entrega MANUAL PENDIENTE a la dirección indicada, el usuario y la actividad auditada")
    void reenvioManual_201() throws Exception {
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        int actividadesAntes = actividades();

        MvcResult r = reenviar(f.idComprobante(), "{\"destinatario\":\"  otro@t055.example \"}", USUARIO, "ADMIN");

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode cuerpo = objectMapper.readTree(r.getResponse().getContentAsString());
        assertThat(cuerpo.get("origen").asText()).isEqualTo("MANUAL");
        assertThat(cuerpo.get("estado").asText()).isEqualTo("PENDIENTE");
        assertThat(cuerpo.get("destinatario").asText()).isEqualTo("otro@t055.example");
        Map<String, Object> e = unica(f.idComprobante());
        assertThat(e.get("origen")).isEqualTo("MANUAL");
        assertThat(e.get("destinatario")).isEqualTo("otro@t055.example");
        assertThat(e.get("usuario_id")).isNotNull();
        assertThat(e.get("clave_automatica")).isNull();
        assertThat(actividades()).isEqualTo(actividadesAntes + 1);

        jobCorreo.procesarPendientes();
        assertThat(proveedor.enviados).hasSize(1);
        assertThat(proveedor.enviados.get(0).destinatario()).isEqualTo("otro@t055.example");
    }

    @Test
    @DisplayName("9: sin destinatario en el cuerpo se usa el correo del receptor; se puede reenviar varias veces")
    void reenvioManual_destinatarioPorDefecto() throws Exception {
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);

        assertThat(reenviar(f.idComprobante(), "{}", USUARIO, "ADMIN").getResponse().getStatus()).isEqualTo(201);
        assertThat(reenviar(f.idComprobante(), "{}", USUARIO, "ADMIN").getResponse().getStatus()).isEqualTo(201);

        assertThat(entregas(f.idComprobante())).hasSize(2).allSatisfy(e ->
                assertThat(e.get("destinatario")).isEqualTo(EMAIL_CLIENTE));
    }

    @Test
    @DisplayName("9: destinatario con formato inválido → 400 con {success:false, message}; no se crea nada")
    void reenvioManual_correoInvalido400() throws Exception {
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);

        for (String malo : new String[] {"no-es-correo", "a@b", "uno@x.co, dos@x.co", "con espacio@x.co"}) {
            MvcResult r = reenviar(f.idComprobante(), "{\"destinatario\":\"" + malo + "\"}", USUARIO, "ADMIN");
            assertThat(r.getResponse().getStatus()).as(malo).isEqualTo(400);
            JsonNode cuerpo = objectMapper.readTree(r.getResponse().getContentAsString());
            assertThat(cuerpo.get("success").asBoolean()).isFalse();
            assertThat(cuerpo.get("message").asText()).isNotBlank();
        }
        assertThat(entregas(f.idComprobante())).isEmpty();
    }

    @Test
    @DisplayName("9: un comprobante que no está ACEPTADO → 409; inexistente → 404")
    void reenvioManual_noAceptado409_inexistente404() throws Exception {
        Doc enviado = facturaCon(EstadoComprobante.ENVIADO, AmbienteHacienda.PRODUCCION);

        MvcResult r = reenviar(enviado.idComprobante(), "{\"destinatario\":\"a@b.co\"}", USUARIO, "ADMIN");
        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(objectMapper.readTree(r.getResponse().getContentAsString()).get("success").asBoolean()).isFalse();
        assertThat(entregas(enviado.idComprobante())).isEmpty();

        assertThat(reenviar(987654321L, "{\"destinatario\":\"a@b.co\"}", USUARIO, "ADMIN").getResponse().getStatus())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("9: el reenvío manual de un comprobante SANDBOX respeta la regla: dirección de prueba, o OMITIDO_SANDBOX sin ella")
    void reenvioManual_reglaSandbox() throws Exception {
        Doc sinPrueba = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.SANDBOX);
        assertThat(reenviar(sinPrueba.idComprobante(), "{\"destinatario\":\"real@cliente.example\"}", USUARIO, "ADMIN")
                .getResponse().getStatus()).isEqualTo(201);
        Map<String, Object> omitida = unica(sinPrueba.idComprobante());
        assertThat(omitida.get("estado")).isEqualTo("OMITIDO_SANDBOX");
        assertThat(omitida.get("destinatario")).isNull();

        correo.getSandbox().setDestinatarioPrueba(PRUEBA);
        Doc conPrueba = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.SANDBOX);
        assertThat(reenviar(conPrueba.idComprobante(), "{\"destinatario\":\"real@cliente.example\"}", USUARIO, "ADMIN")
                .getResponse().getStatus()).isEqualTo(201);
        Map<String, Object> aPrueba = unica(conPrueba.idComprobante());
        assertThat(aPrueba.get("estado")).isEqualTo("PENDIENTE");
        assertThat(aPrueba.get("destinatario")).isEqualTo(PRUEBA);
        jobCorreo.procesarPendientes();
        assertThat(proveedor.enviados).hasSize(1);
        assertThat(proveedor.enviados.get(0).destinatario()).isEqualTo(PRUEBA);
        assertThat(proveedor.enviados.get(0).asunto()).startsWith("[SANDBOX] ");
    }

    @Test
    @DisplayName("9: el historial (GET) devuelve las entregas, la más reciente primero, con el error resumido")
    void historialDeEntregas() throws Exception {
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        Long primera = insertarEntrega(f.idComprobante(), "AUTOMATICO", "FALLIDO", EMAIL_CLIENTE, 3, ahora());
        jdbc.update("UPDATE entrega_comprobante SET ultimo_error = ? WHERE id = ?", "e".repeat(400), primera);
        Long segunda = insertarEntrega(f.idComprobante(), "MANUAL", "PENDIENTE", "otro@t055.example", 0, ahora());

        MvcResult r = mockMvc.perform(get(API_COMP + f.idComprobante() + "/entregas").with(user(USUARIO).roles("ADMIN")))
                .andReturn();

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode lista = objectMapper.readTree(r.getResponse().getContentAsString());
        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).get("id").asLong()).isEqualTo(segunda);
        assertThat(lista.get(1).get("id").asLong()).isEqualTo(primera);
        assertThat(lista.get(1).get("estado").asText()).isEqualTo("FALLIDO");
        assertThat(lista.get(1).get("intentos").asInt()).isEqualTo(3);
        assertThat(lista.get(1).get("ultimoError").asText().length()).isLessThanOrEqualTo(130);
        assertThat(lista.get(0).has("fechaCreacion")).isTrue();
    }

    @Test
    @DisplayName("9: el detalle de la factura y de la nota exponen «correo» {comprobanteId, estadoComprobante, destinatarioPorDefecto, puedeReenviar}")
    void detalleExponeElCorreo() throws Exception {
        activar(AmbienteHacienda.PRODUCCION);
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);

        JsonNode factura = detalleFactura(f.idFactura()).get("correo");
        assertThat(factura.get("comprobanteId").asLong()).isEqualTo(f.idComprobante());
        assertThat(factura.get("estadoComprobante").asText()).isEqualTo("ACEPTADO");
        assertThat(factura.get("destinatarioPorDefecto").asText()).isEqualTo(EMAIL_CLIENTE);
        assertThat(factura.get("puedeReenviar").asBoolean()).isTrue();

        jdbc.update("UPDATE comprobante_electronico SET estado = 'ENVIADO' WHERE id = ?", f.idComprobante());
        assertThat(detalleFactura(f.idFactura()).get("correo").get("puedeReenviar").asBoolean()).isFalse();
        jdbc.update("UPDATE comprobante_electronico SET estado = 'ACEPTADO' WHERE id = ?", f.idComprobante());

        Nota nota = emitirOk(f.idFactura(), "01", "Anulación detalle");
        haciendaResponde(nota.clave(), "aceptado");
        comprobanteService.consultarEstadoManual(nota.idComprobante());
        MvcResult r = mockMvc.perform(get("/api/facturas/notas-credito/" + nota.id() + "/detalle")
                .with(user(USUARIO).roles("ADMIN"))).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        JsonNode correoNota = objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("correo");
        assertThat(correoNota.get("comprobanteId").asLong()).isEqualTo(nota.idComprobante());
        assertThat(correoNota.get("estadoComprobante").asText()).isEqualTo("ACEPTADO");
        assertThat(correoNota.get("destinatarioPorDefecto").asText()).isEqualTo(EMAIL_CLIENTE);
        assertThat(correoNota.get("puedeReenviar").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("9: sin comprobante vigente el campo «correo» del detalle de la factura es null")
    void detalleSinComprobante() throws Exception {
        int idFactura = facturaConLineasAyB();

        JsonNode detalle = detalleFactura(idFactura);

        assertThat(detalle.get("correo") == null || detalle.get("correo").isNull()).isTrue();
    }

    private JsonNode detalleFactura(int idFactura) throws Exception {
        MvcResult r = mockMvc.perform(get("/facturas/detalle/" + idFactura).with(user(USUARIO).roles("ADMIN"))).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    private int actividades() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM usuario_actividad WHERE tipo_actividad = 'COMPROBANTE_REENVIO_CORREO'",
                Integer.class);
    }

    // ── Panel y listado (criterio 10) ────────────────────────────────────────

    @Test
    @DisplayName("10: los contadores del panel cuentan FALLIDO y PENDIENTE vencidas del ambiente; sin ambiente cuentan todos")
    void contadoresDelPanel() throws Exception {
        Doc prod = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        Doc sand = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.SANDBOX);
        insertarEntrega(prod.idComprobante(), "MANUAL", "FALLIDO", EMAIL_CLIENTE, 3, ahora());
        insertarEntrega(prod.idComprobante(), "MANUAL", "PENDIENTE", EMAIL_CLIENTE, 0, ahora().minusMinutes(5));
        insertarEntrega(prod.idComprobante(), "MANUAL", "PENDIENTE", EMAIL_CLIENTE, 0, ahora().plusDays(1));
        insertarEntrega(prod.idComprobante(), "MANUAL", "ENVIADO", EMAIL_CLIENTE, 1, ahora());
        insertarEntrega(sand.idComprobante(), "MANUAL", "FALLIDO", PRUEBA, 3, ahora());
        insertarEntrega(sand.idComprobante(), "MANUAL", "FALLIDO", PRUEBA, 3, ahora());

        PanelFacturacionService.Contadores produccion = panelService.contadores(AmbienteHacienda.PRODUCCION);
        PanelFacturacionService.Contadores sandbox = panelService.contadores(AmbienteHacienda.SANDBOX);
        PanelFacturacionService.Contadores todos = panelService.contadores(null);

        assertThat(produccion.entregasFallidas()).isEqualTo(1);
        assertThat(produccion.entregasPendientesVencidas()).isEqualTo(1);
        assertThat(sandbox.entregasFallidas()).isEqualTo(2);
        assertThat(sandbox.entregasPendientesVencidas()).isZero();
        assertThat(todos.entregasFallidas()).isEqualTo(3);
        assertThat(todos.entregasPendientesVencidas()).isEqualTo(1);
    }

    @Test
    @DisplayName("10: GET /facturas/entregas?estado=FALLIDO lista las entregas con destinatario, estado, intentos y error resumido; rechaza un estado ajeno")
    void paginaDeEntregas() throws Exception {
        activar(AmbienteHacienda.PRODUCCION); // la página lista el ambiente activo, como los contadores del panel
        Doc f = facturaCon(EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        Long id = insertarEntrega(f.idComprobante(), "AUTOMATICO", "FALLIDO", "fallida@t055.example", 3, ahora());
        jdbc.update("UPDATE entrega_comprobante SET ultimo_error = ? WHERE id = ?", "HTTP 422: <script>x</script>", id);
        insertarEntrega(f.idComprobante(), "MANUAL", "PENDIENTE", "pendiente@t055.example", 0, ahora().minusMinutes(3));

        MvcResult fallidas = mockMvc.perform(get("/facturas/entregas").param("estado", "FALLIDO")
                .with(user(USUARIO).roles("ADMIN"))).andReturn();
        MvcResult pendientes = mockMvc.perform(get("/facturas/entregas").param("estado", "PENDIENTE")
                .with(user(USUARIO).roles("ADMIN"))).andReturn();

        String html = fallidas.getResponse().getContentAsString();
        assertThat(fallidas.getResponse().getStatus()).as(html).isEqualTo(200);
        assertThat(html).contains("fallida@t055.example").contains("HTTP 422").doesNotContain("pendiente@t055.example")
                .doesNotContain("<script>x</script>");
        assertThat(pendientes.getResponse().getContentAsString()).contains("pendiente@t055.example")
                .doesNotContain("fallida@t055.example");
        assertThat(mockMvc.perform(get("/facturas/entregas").param("estado", "OTRO").with(user(USUARIO).roles("ADMIN")))
                .andReturn().getResponse().getStatus()).isEqualTo(400);
    }

    // ── Fixture ──────────────────────────────────────────────────────────────

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


    /** Comprobante vigente de la factura con el XML real del generador, en el estado y ambiente indicados. */
    private Long comprobanteDeFactura(int idFactura, EstadoComprobante estado, AmbienteHacienda ambiente) {
        return tx.execute(s -> {
            Factura factura = facturaRepository.findById(idFactura).orElseThrow();
            ComprobanteElectronico c = ComprobanteElectronico.builder()
                    .factura(factura)
                    .empresa(configHacienda.getEmpresa())
                    .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA)
                    .ambiente(ambiente)
                    .claveNumerica(clave())
                    .consecutivo(String.format("%020d", ThreadLocalRandom.current().nextLong(1, 1_000_000_000L)))
                    .fechaEmision(LocalDateTime.now().minusDays(1).withNano(0))
                    .fechaEnvio(LocalDateTime.now().minusDays(1))
                    .estado(estado)
                    .vigente(true)
                    .build();
            c.setXmlComprobante(firmaDigital.firmarXml(xmlGenerator.generarXmlFactura(c), configHacienda.getRutaCertificado(),
                    configHacienda.getPinCertificado()));
            return comprobanteRepository.save(c).getId();
        });
    }

    private int facturaConLineasAyB() throws Exception {
        int idFactura = tx.execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T055 " + UUID.randomUUID());
            cliente.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_FISICA);
            cliente.setNumeroIdentificacion("109990999");
            cliente.setTelefono("88887777");
            cliente.setEmail("cliente@t055.example");
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T055-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T055");
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


    private String estadoComprobante(Long id) {
        return jdbc.queryForObject("SELECT estado FROM comprobante_electronico WHERE id = ?", String.class, id);
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
        // ADR-040: el CAByS vive solo en el artículo maestro
        // (la BD de la clase no se limpia entre pruebas y la descripción del maestro es única: se reutiliza)
        ArticuloMaestro maestro = articuloMaestroRepository.findByDescripcionContainingIgnoreCase(descripcion).stream()
                .filter(m -> descripcion.equals(m.getDescripcion())).findFirst().orElseGet(ArticuloMaestro::new);
        maestro.setDescripcion(descripcion);
        maestro.setCodigoCabys(gravado ? "1234567890123" : "9876543210987");
        p.setArticuloMaestro(articuloMaestroRepository.save(maestro));
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
