package api.astro.whats_orders_manager.modules.facturacion;

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

/**
 * T-032 (ADR-037) — nota de crédito total de punta a punta sobre MySQL: emisión por la API, precondiciones (409),
 * concurrencia, comprobante tipo 03 con su consecutivo, estados de ADR-030 con el cliente de Hacienda simulado
 * (aceptado, rechazado, 404 en la verificación, timeout de lectura) y efectos al aceptarse.
 *
 * Base: MySQL 8 en Testcontainers (t032-it-mysql, tmpfs, sin volúmenes); esquema de Hibernate y rutinas de
 * database/routines, como BloqueoFiscalFacturaIT. El generador de XML, la firma (certificado de prueba) y el
 * validador XSD son los reales; el cliente y la configuración de Hacienda están simulados: ninguna prueba llama a Hacienda. El envío simulado responde como un
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
@ExtendWith(OutputCaptureExtension.class)
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
                    .orElseGet(() -> ConfiguracionEmpresa.builder().razonSocial("Empresa T032 S.A.").build());
            empresa.setNumeroIdentificacion("3101234567");
            empresa.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_JURIDICA);
            empresa.setCodigoActividad("4773.0");
            empresa.setEmail("facturacion@t032.example");
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
        long consecutivoAntes = configuracionHaciendaRepository.findById(configHacienda.getId()).orElseThrow().getConsecutivoNotaCredito();

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
        assertThat(configuracionHaciendaRepository.findById(configHacienda.getId()).orElseThrow().getConsecutivoNotaCredito()).isEqualTo(consecutivoAntes + 1);
        String xml = (String) comp.get("xml_comprobante");
        assertThat(xml).startsWith("<?xml").contains("<NotaCreditoElectronica")
                .contains("<Numero>" + claveFactura + "</Numero>").contains("<Codigo>01</Codigo>")
                .contains("<TotalComprobante>1630.00000</TotalComprobante>").contains("<ds:Signature");
        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.NOTA_CREDITO))
                .as("XML firmado de la nota según el validador XSD real").isEmpty();
        // el timeout simulado deja "Read timed out"; lo que no debe haber es un error de XSD
        assertThat(String.valueOf(comp.get("ultimo_error")))
                .doesNotContain(api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl.ENCABEZADO_XML_INVALIDO_XSD);
        // Timeout de lectura simulado: el envío queda incierto y la nota sigue en trámite
        assertThat(comp.get("estado")).isEqualTo("ENVIO_INCIERTO");
        assertThat(data.get("estadoComprobante").asText()).isEqualTo("ENVIO_INCIERTO");
        verify(haciendaApi).enviarComprobante(anyString(), eq(claveNota), anyLong());
    }

    @Test
    @DisplayName("T-036: nota con código 13 de punta a punta: XML firmado válido según el XSD real y enviado a Hacienda")
    void emitir_codigo13_xmlRealValido_enviado() throws Exception {
        int idFactura = facturaAceptada();
        clearInvocations(haciendaApi);

        Nota nota = emitirOk(idFactura, "13", "Error material en el cliente");

        Map<String, Object> comp = jdbc.queryForMap("SELECT * FROM comprobante_electronico WHERE clave_numerica = ?",
                nota.clave());
        String xml = (String) comp.get("xml_comprobante");
        assertThat(xml).contains("<NotaCreditoElectronica").contains("<Codigo>13</Codigo>").contains("<ds:Signature");
        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.NOTA_CREDITO))
                .as("XML firmado de la nota (13) según el validador XSD real").isEmpty();
        // el timeout simulado deja "Read timed out"; lo que no debe haber es un error de XSD
        assertThat(String.valueOf(comp.get("ultimo_error")))
                .doesNotContain(api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl.ENCABEZADO_XML_INVALIDO_XSD);
        assertThat(comp.get("estado")).isNotEqualTo("ERROR");
        verify(haciendaApi).enviarComprobante(anyString(), eq(nota.clave()), anyLong());
    }

    @Test
    @DisplayName("factura inexistente → 404")
    void facturaInexistente_404() throws Exception {
        assertThat(emitir(999_999, "01", "Anulación total").getResponse().getStatus()).isEqualTo(404);
    }

    @ParameterizedTest(name = "código {0}")
    @ValueSource(strings = {"06", "12", "99"})
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
    @DisplayName("d) T-048: factura con pagos aplicados → 201 (se levanta el 409 de T-032; el excedente irá a saldo a favor)")
    void facturaConPagos_ahoraSeEmite() throws Exception {
        int idFactura = facturaAceptada();
        Long pendiente = idPago(registrarPago(idFactura));
        jdbc.update("UPDATE pagos SET estado = 'CONFIRMADO' WHERE id_pago = ?", pendiente);

        MvcResult result = emitir(idFactura, "01", "Anulación total");

        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        assertThat(notas(idFactura)).isEqualTo(1);
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

    @Test
    @DisplayName("T-037 c.10: dos notas de crédito concurrentes de facturas distintas → consecutivos 03 distintos y seguidos")
    void notasConcurrentes_consecutivosDistintos() throws Exception {
        int facturaA = facturaAceptada();
        int facturaB = facturaAceptada();
        long antes = jdbc.queryForObject("SELECT consecutivo_nota_credito FROM configuracion_hacienda WHERE id = ?",
                Long.class, configHacienda.getId());
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Long> consecutivos = new java.util.ArrayList<>();
        try {
            List<Future<Long>> futuros = new java.util.ArrayList<>();
            for (int idFactura : new int[] {facturaA, facturaB}) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    NotaFactura nota = notaService.emitirNotaCreditoTotal(idFactura, TipoNotaFactura.CREDITO, "01",
                            "Concurrente T-037", USUARIO);
                    return Long.parseLong(jdbc.queryForObject("SELECT consecutivo FROM comprobante_electronico "
                            + "WHERE nota_factura_id = ? AND vigente = 1", String.class, nota.getId()));
                }));
            }
            salida.countDown();
            for (Future<Long> f : futuros) {
                consecutivos.add(f.get(60, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(consecutivos.stream().sorted().toList()).isEqualTo(List.of(antes, antes + 1));
        assertThat(jdbc.queryForObject("SELECT consecutivo_nota_credito FROM configuracion_hacienda WHERE id = ?",
                Long.class, configHacienda.getId())).isEqualTo(antes + 2);
    }

    @Test
    @DisplayName("T-037 c.12: Hacienda tarda 3 s → emitir la nota responde en menos de 6 s con ENVIADO")
    void emitirNota_respondeEnSegundos() throws Exception {
        int idFactura = facturaAceptada();
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenAnswer(inv -> {
            Thread.sleep(3000);
            return RespuestaHaciendaDTO.builder().resultadoEnvio(ResultadoEnvioHacienda.RECIBIDO).codigoHttp(202)
                    .exitoso(false).mensaje("Comprobante recibido por Hacienda, en procesamiento").build();
        });

        long inicio = System.nanoTime();
        MvcResult result = emitir(idFactura, "01", "Devolución total");
        long ms = java.time.Duration.ofNanos(System.nanoTime() - inicio).toMillis();

        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode cuerpo = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(cuerpo.get("data").get("estadoComprobante").asText()).isEqualTo("ENVIADO");
        assertThat(cuerpo.get("message").asText()).startsWith("Nota de crédito emitida. Recibido por Hacienda");
        assertThat(ms).as("sin sondeo dentro de la petición").isLessThan(6000);
        verify(haciendaApi, never()).consultarComprobante(anyString(), anyLong());
    }

    // ── Estados y efectos ────────────────────────────────────────────────────

    @Test
    @DisplayName("ACEPTADO (01): asiento de la nota (DR anulaciones y descuentos + IVA, CR cuentas por cobrar) ligado a nota "
            + "y factura, factura anulada, nota APLICADA, venta original intacta; un segundo sondeo no repite los efectos")
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

        // ADR-052: ya no se revierte el asiento de venta; la nota tiene su propio asiento
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE asiento_revertido_id = ?",
                Integer.class, idVenta)).isZero();
        assertThat(asientoVentaVigente(idFactura)).isEqualTo(idVenta);
        List<Map<String, Object>> asientos = jdbc.queryForList("SELECT id, fecha, estado, factura_id, "
                + "nota_factura_id, asiento_revertido_id FROM asientos_contables WHERE nota_factura_id = ?", nota.id());
        assertThat(asientos).hasSize(1);
        Map<String, Object> asiento = asientos.get(0);
        assertThat(asiento.get("estado")).isEqualTo("CONTABILIZADO");
        assertThat(asiento.get("asiento_revertido_id")).isNull();
        assertThat(((Number) asiento.get("factura_id")).intValue()).isEqualTo(idFactura);
        assertThat(asiento.get("fecha").toString()).isEqualTo(LocalDate.now(reloj).toString());
        long idAsiento = ((Number) asiento.get("id")).longValue();
        // base 1500.00 (1000 + 500 exento), IVA 130.00, total 1630.00
        assertThat(debeEn(idAsiento, "9.4.02.002")).isEqualByComparingTo("1500.00");
        assertThat(debeEn(idAsiento, "9.2.01.002")).isEqualByComparingTo("130.00");
        assertThat(haberEn(idAsiento, "9.1.02.001")).isEqualByComparingTo("1630.00");
        assertCuadra(idAsiento);
        // T-046: 01 → fecha contable y período en el mes de emisión de la nota
        Map<String, Object> atribucion = jdbc.queryForMap(
                "SELECT fecha_contable, periodo_fiscal_atribucion FROM nota_factura WHERE id = ?", nota.id());
        assertThat(atribucion.get("fecha_contable").toString()).isEqualTo(LocalDate.now(reloj).toString());
        assertThat(atribucion.get("periodo_fiscal_atribucion"))
                .isEqualTo(java.time.YearMonth.from(LocalDate.now(reloj)).toString());
    }

    @Test
    @DisplayName("T-046 ACEPTADO (13) sobre una factura de un mes anterior: asiento con la fecha de la nota, base a la "
            + "cuenta de ventas del original y período de atribución = mes de la factura original (ADR-050)")
    void aceptado_codigo13_fechaDeLaNota_periodoDelOriginal() throws Exception {
        int idFactura = facturaAceptada();
        Long idVenta = asientoVentaVigente(idFactura);
        jdbc.update("UPDATE asientos_contables SET fecha = '2026-09-29' WHERE id = ?", idVenta);
        jdbc.update("UPDATE comprobante_electronico SET fecha_emision = '2026-09-29 10:15:30' WHERE factura_id = ? "
                + "AND vigente = b'1'", idFactura);
        Nota nota = emitirOk(idFactura, "13", "Error material en el cliente");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));

        comprobanteService.consultarYActualizarEstado(nota.idComprobante());

        assertThat(jdbc.queryForObject("SELECT fecha FROM asientos_contables WHERE nota_factura_id = ?",
                java.sql.Date.class, nota.id()).toLocalDate()).isEqualTo(LocalDate.now(reloj));
        long idAsiento = jdbc.queryForObject("SELECT id FROM asientos_contables WHERE nota_factura_id = ?",
                Long.class, nota.id());
        // 13 → la cuenta de ventas del asiento original (9.4.01.001), no 4.1.02
        assertThat(debeEn(idAsiento, "9.4.01.001")).isEqualByComparingTo("1500.00");
        assertThat(debeEn(idAsiento, "9.4.02.002")).isZero();
        assertCuadra(idAsiento);
        Map<String, Object> n = jdbc.queryForMap(
                "SELECT fecha_contable, periodo_fiscal_atribucion FROM nota_factura WHERE id = ?", nota.id());
        assertThat(n.get("fecha_contable").toString()).isEqualTo(LocalDate.now(reloj).toString());
        assertThat(n.get("periodo_fiscal_atribucion")).isEqualTo("2026-09");
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
            p.getArticuloMaestro().setCodigoCabys("1111111111111");
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

    // ── Aislamiento del sondeo (criterios 27, 28 y 31) ───────────────────────

    @Test
    @DisplayName("job de sondeo: los efectos fallidos de una nota no revierten el lote; corregida la causa, se aplican "
            + "una sola vez")
    void sondeo_aislaElFalloDeUnaNota(CapturedOutput salida) throws Exception {
        int facturaDeLaNota = facturaAceptada();
        Nota nota = emitirOk(facturaDeLaNota, "01", "Anulación total");
        jdbc.update("UPDATE comprobante_electronico SET estado = 'ENVIADO', fecha_envio = ? WHERE id = ?",
                LocalDateTime.now().minusHours(1), nota.idComprobante());
        int otraFactura = facturaConLineasAyB();
        comprobanteDeFactura(otraFactura, EstadoComprobante.ENVIADO);
        Long otroComprobante = comprobanteRepository.findFirstByFactura_IdFacturaAndVigenteTrue(otraFactura)
                .orElseThrow().getId();
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta("aceptado"));
        // Causa del fallo (T-048: ya no son los pagos): falta el parámetro contable de la base de la nota 01
        Long idCuentaBase = jdbc.queryForObject("SELECT cuenta_contable_id FROM parametros_contables WHERE clave = "
                + "'NOTA_CREDITO_ANULACIONES_DESCUENTOS'", Long.class);
        jdbc.update("DELETE FROM parametros_contables WHERE clave = 'NOTA_CREDITO_ANULACIONES_DESCUENTOS'");

        reintentosJob.consultarEstadosPendientes();

        assertThat(estadoComprobante(otroComprobante)).isEqualTo("ACEPTADO");
        assertThat(estadoComprobante(nota.idComprobante())).isEqualTo("ENVIADO");
        assertThat(estadoNota(nota.id())).isEqualTo("EN_TRAMITE");
        assertThat(reversionesDeLaNota(nota.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class,
                facturaDeLaNota)).isFalse();
        assertThat(salida.getAll()).contains("Efectos de la nota " + nota.id());

        jdbc.update("INSERT INTO parametros_contables (clave, cuenta_contable_id) VALUES (?, ?)",
                "NOTA_CREDITO_ANULACIONES_DESCUENTOS", idCuentaBase);
        reintentosJob.consultarEstadosPendientes();
        reintentosJob.consultarEstadosPendientes();

        assertThat(estadoComprobante(nota.idComprobante())).isEqualTo("ACEPTADO");
        assertThat(estadoNota(nota.id())).isEqualTo("APLICADA");
        assertThat(reversionesDeLaNota(nota.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class,
                facturaDeLaNota)).isTrue();
    }

    // ── T-047: nota de crédito parcial (ADR-051) ─────────────────────────────

    @Test
    @DisplayName("T-047: dos parciales sucesivas (02 y luego 14) con sus asientos proporcionales, el saldo y los topes; "
            + "una tercera que excede → 409 sin consecutivo")
    void parcial_02y14_sucesivas_asientosSaldoYTopes() throws Exception {
        int idFactura = facturaAceptada();
        jdbc.update("UPDATE comprobante_electronico SET fecha_emision = '2026-09-29 10:15:30' WHERE factura_id = ? "
                + "AND vigente = b'1'", idFactura);
        List<Integer> lineas = lineasDeFactura(idFactura);
        Long idVenta = asientoVentaVigente(idFactura);
        String clave = claveVigente(idFactura);

        // Nota 1 (02): mitad de la línea A, 565.00 con IVA = base 500.00 + IVA 65.00
        Nota n1 = emitirParcialOk(idFactura, "02", "Corrección de precio de A", acreditar(lineas.get(0), "565.00"));
        Map<String, Object> fila1 = jdbc.queryForMap("SELECT estado, codigo_referencia, subtotal, impuesto, total, "
                + "periodo_fiscal_atribucion, fecha_contable FROM nota_factura WHERE id = ?", n1.id());
        assertThat(fila1.get("estado")).isEqualTo("EN_TRAMITE");
        assertThat((BigDecimal) fila1.get("subtotal")).isEqualByComparingTo("500.00");
        assertThat((BigDecimal) fila1.get("impuesto")).isEqualByComparingTo("65.00");
        assertThat((BigDecimal) fila1.get("total")).isEqualByComparingTo("565.00");
        // ADR-050: el 02 atribuye al mes de la nota; la fecha contable es la de la nota
        assertThat(fila1.get("periodo_fiscal_atribucion"))
                .isEqualTo(java.time.YearMonth.from(LocalDate.now(reloj)).toString());
        assertThat(fila1.get("fecha_contable").toString()).isEqualTo(LocalDate.now(reloj).toString());
        List<Map<String, Object>> lineasNota = lineasNota(n1.id());
        assertThat(lineasNota).hasSize(1);
        assertThat(lineasNota.get(0).get("descripcion")).isEqualTo("Producto A gravado 13 %");
        assertThat((BigDecimal) lineasNota.get(0).get("precio_unitario")).isEqualByComparingTo("500.00000");
        assertThat(jdbc.queryForObject("SELECT linea_factura_id FROM linea_nota_factura WHERE nota_factura_id = ?",
                Integer.class, n1.id())).isEqualTo(lineas.get(0));
        String xml1 = jdbc.queryForObject("SELECT xml_comprobante FROM comprobante_electronico WHERE id = ?",
                String.class, n1.idComprobante());
        assertThat(XmlValidator.erroresContraXsd(xml1, TipoComprobanteElectronico.NOTA_CREDITO)).isEmpty();
        assertThat(xml1).contains("<Numero>" + clave + "</Numero>").contains("<Codigo>02</Codigo>")
                .contains("<TotalComprobante>565.00000</TotalComprobante>");
        assertThat(detalle(n1.id()).get("parcial").asBoolean()).isTrue();

        aceptar(n1);
        assertThat(estadoNota(n1.id())).isEqualTo("APLICADA");
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class, idFactura))
                .as("la parcial no anula la factura").isFalse();
        assertThat(asientoVentaVigente(idFactura)).as("la venta original sigue vigente").isEqualTo(idVenta);
        long asiento1 = jdbc.queryForObject("SELECT id FROM asientos_contables WHERE nota_factura_id = ?", Long.class,
                n1.id());
        assertThat(debeEn(asiento1, "9.4.02.002")).isEqualByComparingTo("500.00");
        assertThat(debeEn(asiento1, "9.2.01.002")).isEqualByComparingTo("65.00");
        assertThat(haberEn(asiento1, "9.1.02.001")).isEqualByComparingTo("565.00");
        assertCuadra(asiento1);
        assertThat(jdbc.queryForObject("SELECT fecha FROM asientos_contables WHERE id = ?", java.sql.Date.class,
                asiento1).toLocalDate()).isEqualTo(LocalDate.now(reloj));
        // saldo: baja en lo acreditado, en el detalle de la factura y en el estado de cuenta del cliente
        JsonNode factura1 = detalleFactura(idFactura);
        assertThat(factura1.get("totalAcreditado").decimalValue()).isEqualByComparingTo("565.00");
        assertThat(factura1.get("saldoPendiente").decimalValue()).isEqualByComparingTo("1065.00");
        assertThat(factura1.get("acreditadaTotalmente").asBoolean()).isFalse();
        assertThat(factura1.get("puedeEmitirNotaTotal").asBoolean()).isFalse();
        assertThat(factura1.get("puedeEmitirNotaParcial").asBoolean()).isTrue();
        assertThat(factura1.get("motivoNotaTotalNoDisponible").asText())
                .isEqualTo("La factura tiene notas parciales; acredite el resto con una nota parcial");
        assertThat(saldoDelCliente(idFactura)).isEqualByComparingTo("1065.00");

        // Nota 2 (14): resto de A (565.00) y 200.00 de B (exento): base 700.00 e IVA 65.00
        Nota n2 = emitirParcialOk(idFactura, "14", "Error material en A y B", acreditar(lineas.get(0), "565.00"),
                acreditar(lineas.get(1), "200.00"));
        assertThat(jdbc.queryForObject("SELECT periodo_fiscal_atribucion FROM nota_factura WHERE id = ?",
                String.class, n2.id())).as("el 14 atribuye al mes de la factura original").isEqualTo("2026-09");
        aceptar(n2);
        long asiento2 = jdbc.queryForObject("SELECT id FROM asientos_contables WHERE nota_factura_id = ?", Long.class,
                n2.id());
        assertThat(debeEn(asiento2, "9.4.01.001")).as("14: base a la cuenta de ventas del original")
                .isEqualByComparingTo("700.00");
        assertThat(debeEn(asiento2, "9.4.02.002")).isZero();
        assertThat(debeEn(asiento2, "9.2.01.002")).isEqualByComparingTo("65.00");
        assertThat(haberEn(asiento2, "9.1.02.001")).isEqualByComparingTo("765.00");
        assertCuadra(asiento2);
        assertThat(jdbc.queryForObject("SELECT fecha FROM asientos_contables WHERE id = ?", java.sql.Date.class,
                asiento2).toLocalDate()).as("fecha contable = la de la nota").isEqualTo(LocalDate.now(reloj));
        assertThat(jdbc.queryForObject("SELECT monto_acreditado FROM factura WHERE id_factura = ?", BigDecimal.class,
                idFactura)).isEqualByComparingTo("1330.00");
        assertThat(detalleFactura(idFactura).get("saldoPendiente").decimalValue()).isEqualByComparingTo("300.00");

        // La tercera excede: a B le quedan 300.00; 300.01 → 409 sin nota ni consecutivo
        long consecutivo = configuracionHaciendaRepository.findById(configHacienda.getId()).orElseThrow()
                .getConsecutivoNotaCredito();
        MvcResult excede = emitirParcial(idFactura, "02", "Excede", acreditar(lineas.get(1), "300.01"));
        assertThat(excede.getResponse().getStatus()).isEqualTo(409);
        assertThat(excede.getResponse().getContentAsString()).contains(NotaFacturaService.MENSAJE_TOPE_LINEA);
        assertThat(notas(idFactura)).isEqualTo(2);
        assertThat(configuracionHaciendaRepository.findById(configHacienda.getId()).orElseThrow()
                .getConsecutivoNotaCredito()).as("sin consecutivo").isEqualTo(consecutivo);
    }

    @Test
    @DisplayName("T-047: parciales hasta el 100 % → acreditada totalmente (sin anular): saldo 0, sin más notas ni pagos, "
            + "sin sustituta")
    void parcial_llegaAl100_acreditadaTotalmente() throws Exception {
        int idFactura = facturaAceptada();
        List<Integer> lineas = lineasDeFactura(idFactura);
        Nota n1 = emitirParcialOk(idFactura, "02", "Primera mitad", acreditar(lineas.get(0), "1130.00"));
        aceptar(n1);
        assertThat(jdbc.queryForObject("SELECT acreditada_totalmente FROM factura WHERE id_factura = ?",
                Boolean.class, idFactura)).isFalse();
        Nota n2 = emitirParcialOk(idFactura, "02", "El resto", acreditar(lineas.get(1), "500.00"));
        aceptar(n2);

        Map<String, Object> f = jdbc.queryForMap("SELECT anulada, acreditada_totalmente, monto_acreditado "
                + "FROM factura WHERE id_factura = ?", idFactura);
        assertThat((Boolean) f.get("acreditada_totalmente")).isTrue();
        assertThat((Boolean) f.get("anulada")).as("no se anula").isFalse();
        assertThat((BigDecimal) f.get("monto_acreditado")).isEqualByComparingTo("1630.00");
        JsonNode detalle = detalleFactura(idFactura);
        assertThat(detalle.get("acreditadaTotalmente").asBoolean()).isTrue();
        assertThat(detalle.get("saldoPendiente").decimalValue()).isEqualByComparingTo("0");
        assertThat(detalle.get("puedeEmitirNotaCredito").asBoolean()).isFalse();
        assertThat(saldoDelCliente(idFactura)).isEqualByComparingTo("0");

        MvcResult otra = emitirParcial(idFactura, "02", "Otra", acreditar(lineas.get(0), "0.01"));
        assertThat(otra.getResponse().getStatus()).isEqualTo(409);
        assertThat(otra.getResponse().getContentAsString()).contains(NotaFacturaService.MENSAJE_ACREDITADA_TOTALMENTE);
        assertThat(emitir(idFactura, "01", "Total").getResponse().getStatus()).isEqualTo(409);
        assertThat(sustituta(idFactura, null).getResponse().getStatus())
                .as("una factura acreditada totalmente no es elegible para sustituta").isEqualTo(409);
        Factura entidad = facturaRepository.findById(idFactura).orElseThrow();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
            api.astro.whats_orders_manager.modules.facturacion.model.Pago pago =
                    new api.astro.whats_orders_manager.modules.facturacion.model.Pago();
            pago.setFactura(entidad);
            pago.setCliente(entidad.getCliente());
            pago.setMonto(BigDecimal.ONE);
            pagoService.registrarPago(pago);
        });
    }

    @Test
    @DisplayName("T-047: nota total (01 o 13) con una parcial aplicada o en trámite → 409 con el mensaje fijo")
    void totalDespuesDeParcial_409() throws Exception {
        int idFactura = facturaAceptada();
        Integer lineaA = lineasDeFactura(idFactura).get(0);
        Nota parcial = emitirParcialOk(idFactura, "02", "Parcial", acreditar(lineaA, "113.00"));
        String mensaje = "La factura tiene notas parciales; acredite el resto con una nota parcial";

        for (String codigo : new String[] {"01", "13"}) {
            MvcResult enTramite = emitir(idFactura, codigo, "Total tras parcial");
            assertThat(enTramite.getResponse().getStatus()).isEqualTo(409);
            assertThat(enTramite.getResponse().getContentAsString()).contains(mensaje);
        }
        aceptar(parcial);
        for (String codigo : new String[] {"01", "13"}) {
            MvcResult aplicada = emitir(idFactura, codigo, "Total tras parcial");
            assertThat(aplicada.getResponse().getStatus()).isEqualTo(409);
            assertThat(objectMapper.readTree(aplicada.getResponse().getContentAsString()).get("message").asText())
                    .isEqualTo(mensaje);
        }
        assertThat(notas(idFactura)).isEqualTo(1);
    }

    @Test
    @DisplayName("T-047: una parcial RECHAZADA no deja efectos y no cuenta en los topes")
    void parcialRechazada_sinEfectos_noCuentaEnTopes() throws Exception {
        int idFactura = facturaAceptada();
        Integer lineaB = lineasDeFactura(idFactura).get(1);
        Nota nota = emitirParcialOk(idFactura, "02", "Será rechazada", acreditar(lineaB, "500.00"));
        // en trámite sí cuenta: B ya no tiene disponible
        assertThat(emitirParcial(idFactura, "02", "Otra", acreditar(lineaB, "0.01")).getResponse().getStatus())
                .isEqualTo(409);
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("rechazado"));

        comprobanteService.consultarYActualizarEstado(nota.idComprobante());

        assertThat(estadoNota(nota.id())).isEqualTo("RECHAZADA");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE nota_factura_id = ?",
                Integer.class, nota.id())).isZero();
        Map<String, Object> f = jdbc.queryForMap("SELECT monto_acreditado, acreditada_totalmente FROM factura "
                + "WHERE id_factura = ?", idFactura);
        assertThat((BigDecimal) f.get("monto_acreditado")).isEqualByComparingTo("0");
        assertThat((Boolean) f.get("acreditada_totalmente")).isFalse();
        // rechazada: no cuenta en los topes, se puede acreditar el total de B otra vez
        Nota nueva = emitirParcialOk(idFactura, "02", "Otra vez", acreditar(lineaB, "500.00"));
        assertThat(estadoNota(nueva.id())).isEqualTo("EN_TRAMITE");
    }

    @Test
    @DisplayName("T-047 criterio 24: con el parámetro contable ausente la nota no se aplica, queda EN_TRAMITE y el mensaje lo nombra")
    void parametroAusente_notaQuedaEnTramite(CapturedOutput salida) throws Exception {
        int idFactura = facturaAceptada();
        Nota nota = emitirParcialOk(idFactura, "02", "Sin parámetro", acreditar(lineasDeFactura(idFactura).get(0), "113.00"));
        Long idCuenta = jdbc.queryForObject("SELECT cuenta_contable_id FROM parametros_contables WHERE clave = "
                + "'NOTA_CREDITO_ANULACIONES_DESCUENTOS'", Long.class);
        jdbc.update("DELETE FROM parametros_contables WHERE clave = 'NOTA_CREDITO_ANULACIONES_DESCUENTOS'");
        try {
            when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> comprobanteService.consultarYActualizarEstado(nota.idComprobante()));

            assertThat(estadoNota(nota.id())).isEqualTo("EN_TRAMITE");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE nota_factura_id = ?",
                    Integer.class, nota.id())).isZero();
            assertThat(jdbc.queryForObject("SELECT monto_acreditado FROM factura WHERE id_factura = ?",
                    BigDecimal.class, idFactura)).isEqualByComparingTo("0");
            assertThat(salida.getAll()).contains("Parámetro contable no configurado: NOTA_CREDITO_ANULACIONES_DESCUENTOS");
        } finally {
            jdbc.update("INSERT INTO parametros_contables (clave, cuenta_contable_id) VALUES (?, ?)",
                    "NOTA_CREDITO_ANULACIONES_DESCUENTOS", idCuenta);
        }
        // configurado el parámetro, el siguiente sondeo la aplica
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
        assertThat(estadoNota(nota.id())).isEqualTo("APLICADA");
    }

    @Test
    @DisplayName("T-047: pedido incoherente → 400 sin nota; línea ajena → 409 sin consecutivo; detalle de la factura con lo "
            + "disponible por línea")
    void parcial_validaciones_yDatosDelModal() throws Exception {
        int idFactura = facturaAceptada();
        List<Integer> lineas = lineasDeFactura(idFactura);
        MvcResult sinLineas = emitir(idFactura, "02", "Sin líneas");
        assertThat(sinLineas.getResponse().getStatus()).isEqualTo(400);
        assertThat(sinLineas.getResponse().getContentAsString()).contains(NotaFacturaService.MENSAJE_PARCIAL_SIN_LINEAS);
        MvcResult totalConLineas = emitirParcial(idFactura, "01", "Total con líneas", acreditar(lineas.get(0), "10.00"));
        assertThat(totalConLineas.getResponse().getStatus()).isEqualTo(400);
        assertThat(totalConLineas.getResponse().getContentAsString()).contains(NotaFacturaService.MENSAJE_TOTAL_CON_LINEAS);
        assertThat(emitirParcial(idFactura, "02", "Monto cero", acreditar(lineas.get(0), "0")).getResponse().getStatus())
                .isEqualTo(400);
        assertThat(emitirParcial(idFactura, "02", "Tres decimales", acreditar(lineas.get(0), "1.005")).getResponse()
                .getStatus()).isEqualTo(400);
        long consecutivo = configuracionHaciendaRepository.findById(configHacienda.getId()).orElseThrow()
                .getConsecutivoNotaCredito();
        MvcResult ajena = emitirParcial(idFactura, "02", "Línea ajena", acreditar(999_999, "10.00"));
        assertThat(ajena.getResponse().getStatus()).isEqualTo(409);
        assertThat(ajena.getResponse().getContentAsString()).contains(NotaFacturaService.MENSAJE_LINEA_AJENA);
        assertThat(notas(idFactura)).isZero();
        assertThat(configuracionHaciendaRepository.findById(configHacienda.getId()).orElseThrow()
                .getConsecutivoNotaCredito()).isEqualTo(consecutivo);

        JsonNode d = detalleFactura(idFactura);
        assertThat(d.get("puedeEmitirNotaTotal").asBoolean()).isTrue();
        assertThat(d.get("puedeEmitirNotaParcial").asBoolean()).isTrue();
        assertThat(d.get("notaCreditoTotalComprobante").decimalValue()).isEqualByComparingTo("1630.00");
        assertThat(d.get("notaCreditoTotalAcreditado").decimalValue()).isEqualByComparingTo("0");
        assertThat(d.get("notaCreditoDisponible").decimalValue()).isEqualByComparingTo("1630.00");
        JsonNode filas = d.get("notaCreditoLineas");
        assertThat(filas).hasSize(2);
        assertThat(filas.get(0).get("idLineaFactura").asInt()).isEqualTo(lineas.get(0));
        assertThat(filas.get(0).get("total").decimalValue()).isEqualByComparingTo("1130.00");
        assertThat(filas.get(0).get("acreditado").decimalValue()).isEqualByComparingTo("0");
        assertThat(filas.get(0).get("disponible").decimalValue()).isEqualByComparingTo("1130.00");

        // con una parcial en trámite: ni nota nueva ni bandera; los acreditados la cuentan
        emitirParcialOk(idFactura, "14", "En trámite", acreditar(lineas.get(0), "113.00"));
        JsonNode enTramite = detalleFactura(idFactura);
        assertThat(enTramite.get("puedeEmitirNotaCredito").asBoolean()).isFalse();
        assertThat(enTramite.get("notaCreditoLineas").isNull()).isTrue();
        assertThat(listar().get("contenido").get(0).get("parcial").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("T-047 (criterio 13): el PDF de la nota parcial se genera")
    void pdfDeLaNotaParcial() throws Exception {
        int idFactura = facturaAceptada();
        List<Integer> lineasFactura = lineasDeFactura(idFactura);
        Nota nota = emitirParcialOk(idFactura, "02", "Para el PDF", acreditar(lineasFactura.get(0), "113.00"),
                acreditar(lineasFactura.get(1), "250.00"));
        empresaService.getEmpresaPrincipal();

        MvcResult pdf = mockMvc.perform(get("/facturas/notas-credito/" + nota.id() + "/pdf")
                .with(user(USUARIO).roles("ADMIN"))).andReturn();

        assertThat(pdf.getResponse().getStatus()).as(pdf.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");
        // T-047 criterio 34: el texto extraído trae la modalidad, la descripción y el monto de la línea de la nota
        String texto = textoDelPdf(pdf.getResponse().getContentAsByteArray());
        assertThat(texto).contains("Nota de crédito parcial");
        assertThat(texto).contains("Producto A gravado 13 %");
        String plano = texto.replace(",", "");
        // cada fila muestra descripción, precio (base / cantidad) y total de la línea de la nota; el total lleva el IVA
        assertThat(plano).contains("Producto A gravado 13 % 1.00 Unid 100.00 13.00 100.00");
        assertThat(plano).contains("Producto B exento 1.00 Unid 250.00 0.00 250.00");
        assertThat(plano).contains("Total ¢363.00");
    }

    private static String textoDelPdf(byte[] bytes) throws Exception {
        try (com.itextpdf.kernel.pdf.PdfDocument doc = new com.itextpdf.kernel.pdf.PdfDocument(
                new com.itextpdf.kernel.pdf.PdfReader(new java.io.ByteArrayInputStream(bytes)))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                sb.append(com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(doc.getPage(i)))
                        .append('\n');
            }
            return sb.toString();
        }
    }

    @Test
    @DisplayName("T-047 (criterio 33): el asiento de una nota parcial usa AUTOMATICO_NOTA_CREDITO y no cuenta como venta")
    void asientoDeLaNota_tipoPropio() throws Exception {
        int idFactura = facturaAceptada();
        Long idVenta = asientoVentaVigente(idFactura);
        Nota nota = emitirParcialOk(idFactura, "02", "Tipo propio", acreditar(lineasDeFactura(idFactura).get(0), "113.00"));
        aceptar(nota);

        assertThat(jdbc.queryForObject("SELECT tipo FROM asientos_contables WHERE nota_factura_id = ?", String.class,
                nota.id())).isEqualTo("AUTOMATICO_NOTA_CREDITO");
        assertThat(asientoVentaVigente(idFactura)).as("el asiento de venta sigue siendo el único vigente").isEqualTo(idVenta);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE factura_id = ? AND tipo = "
                + "'AUTOMATICO_VENTA'", Integer.class, idFactura)).isEqualTo(1);
    }

    /** Factura con dos notas parciales por el 100 %, aceptadas: acreditada totalmente. */
    private int facturaAcreditadaTotalmente() throws Exception {
        int idFactura = facturaAceptada();
        List<Integer> lineas = lineasDeFactura(idFactura);
        aceptar(emitirParcialOk(idFactura, "02", "Línea A", acreditar(lineas.get(0), "1130.00")));
        aceptar(emitirParcialOk(idFactura, "02", "Línea B", acreditar(lineas.get(1), "500.00")));
        return idFactura;
    }

    private List<Integer> pendientesDelListado(Boolean pagada) {
        return facturaRepository.buscarConFiltros(null, null, null, pagada, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 200)).map(Factura::getIdFactura).getContent();
    }

    @Test
    @DisplayName("T-047 (criterio 31): una factura acreditada totalmente no aparece con pagada=false ni con pagada=true; "
            + "la parcial sí aparece como pendiente")
    void acreditadaTotalmente_noEsPendienteDePago() throws Exception {
        int acreditada = facturaAcreditadaTotalmente();
        int parcial = facturaAceptada();
        aceptar(emitirParcialOk(parcial, "02", "Parcial", acreditar(lineasDeFactura(parcial).get(0), "113.00")));

        assertThat(pendientesDelListado(false)).contains(parcial).doesNotContain(acreditada);
        assertThat(pendientesDelListado(true)).doesNotContain(acreditada, parcial);
        assertThat(pendientesDelListado(null)).contains(acreditada, parcial);
    }

    @Test
    @DisplayName("T-047 (criterio 32): findFacturasConPagoVencido excluye anuladas y acreditadas totalmente; la parcial se queda")
    void recordatorios_consulta() throws Exception {
        int acreditada = facturaAcreditadaTotalmente();
        int anulada = facturaAceptada();
        int parcial = facturaAceptada();
        aceptar(emitirParcialOk(parcial, "02", "Parcial", acreditar(lineasDeFactura(parcial).get(0), "113.00")));
        jdbc.update("UPDATE factura SET anulada = b'1' WHERE id_factura = ?", anulada);
        for (int id : List.of(acreditada, anulada, parcial)) {
            jdbc.update("UPDATE factura SET fecha_pago = ?, entregado = b'1' WHERE id_factura = ?",
                    LocalDate.now().minusDays(3), id);
            jdbc.update("UPDATE cliente SET email = 'cliente@example.invalid' WHERE id_cliente = "
                    + "(SELECT id_cliente FROM factura WHERE id_factura = ?)", id);
        }

        List<Integer> ids = tx.execute(s -> facturaRepository.findFacturasConPagoVencido().stream()
                .map(Factura::getIdFactura).toList());

        assertThat(ids).contains(parcial).doesNotContain(acreditada, anulada);
    }

    private static Map<String, Object> acreditar(int idLineaFactura, String monto) {
        Map<String, Object> l = new LinkedHashMap<>();
        l.put("idLineaFactura", idLineaFactura);
        l.put("montoAcreditar", new BigDecimal(monto));
        return l;
    }

    @SafeVarargs
    private MvcResult emitirParcial(int idFactura, String codigo, String razon, Map<String, Object>... lineas)
            throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idFactura", idFactura);
        cuerpo.put("codigoReferencia", codigo);
        cuerpo.put("razon", razon);
        cuerpo.put("lineas", List.of(lineas));
        return mockMvc.perform(post("/api/facturas/notas-credito")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andReturn();
    }

    @SafeVarargs
    private Nota emitirParcialOk(int idFactura, String codigo, String razon, Map<String, Object>... lineas)
            throws Exception {
        MvcResult result = emitirParcial(idFactura, codigo, razon, lineas);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return new Nota(data.get("id").asLong(), data.get("idComprobante").asLong(),
                data.get("claveComprobante").asText());
    }

    /** Hacienda acepta el comprobante de la nota: se aplican sus efectos. */
    private void aceptar(Nota nota) {
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
    }

    private List<Integer> lineasDeFactura(int idFactura) {
        return jdbc.queryForList("SELECT id_linea_factura FROM linea_factura WHERE id_factura = ? ORDER BY numero_linea",
                Integer.class, idFactura);
    }

    private BigDecimal saldoDelCliente(int idFactura) {
        Integer idCliente = jdbc.queryForObject("SELECT id_cliente FROM factura WHERE id_factura = ?", Integer.class,
                idFactura);
        return tx.execute(s -> pagoService.buildEstadoCuenta(idCliente).getSaldoPendiente());
    }

    // ── Pagos durante una nota en trámite (criterio 30) ──────────────────────

    @Test
    @DisplayName("con la nota EN_TRAMITE: registrar, confirmar, conciliar y conciliar en lote un pago → 409 que cita la nota")
    void pagos_bloqueadosConNotaEnTramite() throws Exception {
        int idFactura = facturaAceptada();
        Long pendiente = idPago(registrarPago(idFactura));
        Nota nota = emitirOk(idFactura, "01", "Anulación total");

        List<MvcResult> bloqueadas = List.of(
                registrarPago(idFactura),
                mockMvc.perform(put("/pagos/" + pendiente + "/confirmar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn(),
                mockMvc.perform(put("/pagos/" + pendiente + "/conciliar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn(),
                mockMvc.perform(post("/pagos/conciliar-lote").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + pendiente + "]}")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn());

        for (MvcResult r : bloqueadas) {
            assertThat(r.getResponse().getStatus()).as(r.getRequest().getRequestURI()).isEqualTo(409);
            assertThat(r.getResponse().getContentAsString()).contains("nota de crédito " + nota.id());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pagos WHERE id_factura = ?", Integer.class, idFactura))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM pagos WHERE id_pago = ?", String.class, pendiente))
                .isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("con la nota RECHAZADA la factura vuelve a admitir pagos")
    void pago_permitidoConNotaRechazada() throws Exception {
        int idFactura = facturaAceptada();
        Nota nota = emitirOk(idFactura, "01", "Anulación total");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("rechazado"));
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
        assertThat(estadoNota(nota.id())).isEqualTo("RECHAZADA");

        MvcResult result = registrarPago(idFactura);

        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
    }

    // ── Apoyo ────────────────────────────────────────────────────────────────

    /** POST /pagos/registrar: pago PENDIENTE en efectivo (sin asiento) de 100 sobre la factura. */
    private MvcResult registrarPago(int idFactura) throws Exception {
        Integer idCliente = jdbc.queryForObject("SELECT id_cliente FROM factura WHERE id_factura = ?", Integer.class,
                idFactura);
        Map<String, Object> pago = new LinkedHashMap<>();
        pago.put("idCliente", idCliente);
        pago.put("idFactura", idFactura);
        pago.put("tipoPago", "PARCIAL");
        pago.put("monto", 100);
        pago.put("metodoPago", "EFECTIVO");
        pago.put("fechaPago", LocalDateTime.now().withNano(0).toString());
        pago.put("estado", "PENDIENTE");
        return mockMvc.perform(post("/pagos/registrar").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pago))
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andReturn();
    }

    private Long idPago(MvcResult registrado) throws Exception {
        assertThat(registrado.getResponse().getStatus()).as(registrado.getResponse().getContentAsString())
                .isEqualTo(200);
        return objectMapper.readTree(registrado.getResponse().getContentAsString()).get("data").get("idPago").asLong();
    }

    private int reversionesDeLaNota(Long idNota) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE nota_factura_id = ?", Integer.class,
                idNota);
    }


    // ── T-042: pantalla de notas (listado, detalle, PDF, bandera del modal) ───────────

    @Test
    @DisplayName("T-042: listado con filtros de cliente, fechas y estado, paginado y ordenado por fecha descendente")
    void listado_filtrosPaginacionYOrden() throws Exception {
        String token = "Zeta" + UUID.randomUUID().toString().substring(0, 8);
        int idFactura1 = facturaAceptada();
        int idFactura2 = facturaAceptada();
        Nota n1 = emitirOk(idFactura1, "01", "Primera para el listado");
        Nota n2 = emitirOk(idFactura2, "13", "Segunda para el listado");
        for (int id : new int[] {idFactura1, idFactura2}) {
            jdbc.update("UPDATE cliente SET nombre = ? WHERE id_cliente = (SELECT id_cliente FROM factura "
                    + "WHERE id_factura = ?)", token + " S.A.", id);
        }
        // Fechas lejanas para no mezclarse con las notas de otras pruebas de la clase
        jdbc.update("UPDATE nota_factura SET fecha_emision = '2020-03-10 10:00:00' WHERE id = ?", n1.id());
        jdbc.update("UPDATE nota_factura SET fecha_emision = '2020-03-12 12:00:00', estado = 'APLICADA' WHERE id = ?",
                n2.id());

        // cliente: contiene, sin distinguir mayúsculas
        JsonNode porCliente = listar("cliente", token.toUpperCase().substring(0, 6).toLowerCase());
        assertThat(ids(porCliente)).containsExactly(n2.id(), n1.id());
        assertThat(porCliente.get("totalElementos").asLong()).isEqualTo(2);

        // fechas: ambos extremos inclusive; «hasta» llega al final del día
        assertThat(ids(listar("desde", "2020-03-10", "hasta", "2020-03-12"))).containsExactly(n2.id(), n1.id());
        assertThat(ids(listar("desde", "2020-03-11", "hasta", "2020-03-12"))).containsExactly(n2.id());
        assertThat(ids(listar("desde", "2020-03-10", "hasta", "2020-03-10"))).containsExactly(n1.id());
        assertThat(ids(listar("desde", "2020-03-13", "hasta", "2020-03-31"))).isEmpty();

        // estado de la nota
        assertThat(ids(listar("desde", "2020-03-01", "hasta", "2020-03-31", "estado", "APLICADA")))
                .containsExactly(n2.id());
        assertThat(ids(listar("desde", "2020-03-01", "hasta", "2020-03-31", "estado", "EN_TRAMITE")))
                .containsExactly(n1.id());

        // paginado en la base de datos: 0-based, orden fecha descendente
        JsonNode pagina0 = listar("cliente", token, "page", "0", "size", "1");
        assertThat(ids(pagina0)).containsExactly(n2.id());
        assertThat(pagina0.get("pagina").asInt()).isZero();
        assertThat(pagina0.get("tamano").asInt()).isEqualTo(1);
        assertThat(pagina0.get("totalElementos").asLong()).isEqualTo(2);
        assertThat(pagina0.get("totalPaginas").asInt()).isEqualTo(2);
        assertThat(ids(listar("cliente", token, "page", "1", "size", "1"))).containsExactly(n1.id());
        assertThat(listar("size", "1000").get("tamano").asInt()).isEqualTo(100);

        // contenido de la fila
        JsonNode fila = listar("cliente", token, "estado", "EN_TRAMITE").get("contenido").get(0);
        assertThat(fila.get("numero").asText()).isEqualTo(
                jdbc.queryForObject("SELECT consecutivo FROM comprobante_electronico WHERE id = ?", String.class,
                        n1.idComprobante()));
        assertThat(fila.get("idFactura").asInt()).isEqualTo(idFactura1);
        assertThat(fila.get("clienteNombre").asText()).isEqualTo(token + " S.A.");
        assertThat(fila.get("codigoReferencia").asText()).isEqualTo("01");
        assertThat(fila.get("codigoNombre").asText()).isEqualTo("Anula documento de referencia");
        assertThat(fila.get("total").decimalValue()).isEqualByComparingTo("1630.00");
        assertThat(fila.get("moneda").asText()).isEqualTo("CRC");
        assertThat(fila.get("fechaEmision").asText()).startsWith("2020-03-10T10:00");
        assertThat(fila.get("estado").asText()).isEqualTo("EN_TRAMITE");
        assertThat(fila.get("estadoComprobante").asText()).isEqualTo("ENVIO_INCIERTO");
        assertThat(fila.get("idComprobante").asLong()).isEqualTo(n1.idComprobante());
        // ENVIO_INCIERTO se consulta a mano, no se reenvía (mismas reglas de comprobantes)
        assertThat(fila.get("puedeReenviar").asBoolean()).isFalse();
        assertThat(fila.get("consultableEnHacienda").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("T-042: filtros inválidos → 400")
    void listado_invalido_400() throws Exception {
        assertThat(mockMvc.perform(get("/api/facturas/notas-credito").param("estado", "OTRO")
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getStatus()).isEqualTo(400);
        assertThat(mockMvc.perform(get("/api/facturas/notas-credito").param("desde", "10/03/2020")
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("T-042: detalle con líneas, referencia, comprobante, historial, asiento de reversión y sustituta; 404 si no existe")
    void detalle_completo() throws Exception {
        int idFactura = facturaAceptada();
        String claveFactura = claveVigente(idFactura);
        Long idVenta = asientoVentaVigente(idFactura);
        Nota nota = emitirOk(idFactura, "01", "Anulación para el detalle");

        // En trámite: sin asiento ni sustituta
        JsonNode enTramite = detalle(nota.id());
        assertThat(enTramite.get("estado").asText()).isEqualTo("EN_TRAMITE");
        assertThat(enTramite.get("idAsientoReversion").isNull()).isTrue();
        assertThat(enTramite.get("puedeEmitirSustituta").asBoolean()).isFalse();
        assertThat(enTramite.get("historial")).hasSize(1);

        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
        Long idReversion = jdbc.queryForObject("SELECT id FROM asientos_contables WHERE nota_factura_id = ?",
                Long.class, nota.id());
        String numeroReversion = jdbc.queryForObject("SELECT numero FROM asientos_contables WHERE id = ?",
                String.class, idReversion);

        JsonNode d = detalle(nota.id());
        assertThat(d.get("id").asLong()).isEqualTo(nota.id());
        assertThat(d.get("estado").asText()).isEqualTo("APLICADA");
        assertThat(d.get("tipo").asText()).isEqualTo("CREDITO");
        assertThat(d.get("codigoReferencia").asText()).isEqualTo("01");
        // T-046: fecha contable, período de atribución y fecha del asiento enlazado
        assertThat(d.get("fechaContable").asText()).isEqualTo(LocalDate.now(reloj).toString());
        assertThat(d.get("periodoFiscalAtribucion").asText())
                .isEqualTo(java.time.YearMonth.from(LocalDate.now(reloj)).toString());
        assertThat(d.get("fechaAsientoReversion").asText()).isEqualTo(LocalDate.now(reloj).toString());
        assertThat(d.get("codigoNombre").asText()).isEqualTo("Anula documento de referencia");
        assertThat(d.get("razon").asText()).isEqualTo("Anulación para el detalle");
        assertThat(d.get("moneda").asText()).isEqualTo("CRC");
        assertThat(d.get("total").decimalValue()).isEqualByComparingTo("1630.00");
        assertThat(d.get("usuarioCreador").asText()).isEqualTo(USUARIO);
        assertThat(d.get("fechaAplicacion").isNull()).isFalse();
        assertThat(d.get("idFactura").asInt()).isEqualTo(idFactura);
        assertThat(d.get("numeroFactura").asText()).isNotBlank();
        assertThat(d.get("clienteNombre").asText()).startsWith("Cliente T032");
        assertThat(d.get("claveReferencia").asText()).isEqualTo(claveFactura);
        assertThat(d.get("fechaEmisionReferencia").asText()).isNotBlank();

        JsonNode lineas = d.get("lineas");
        assertThat(lineas).hasSize(2);
        assertThat(lineas.get(0).get("numeroLinea").asInt()).isEqualTo(1);
        assertThat(lineas.get(0).get("producto").asText()).contains("Producto A");
        assertThat(lineas.get(0).get("descripcion").asText()).isNotBlank();
        assertThat(lineas.get(0).get("codigoCabys").asText()).hasSize(13);
        assertThat(lineas.get(0).get("cantidad").decimalValue()).isEqualByComparingTo("1");
        assertThat(lineas.get(0).get("tarifaIva").decimalValue()).isEqualByComparingTo("13");
        assertThat(lineas.get(1).get("producto").asText()).contains("Producto B");

        assertThat(d.get("comprobante").get("estado").asText()).isEqualTo("ACEPTADO");
        assertThat(d.get("comprobante").get("clave").asText()).isEqualTo(nota.clave());
        assertThat(d.get("comprobante").get("estadoDescripcion").asText()).isNotBlank();
        assertThat(d.get("historial")).hasSize(1);
        assertThat(d.get("historial").get(0).get("id").asLong()).isEqualTo(nota.idComprobante());
        assertThat(d.get("idAsientoReversion").asLong()).isEqualTo(idReversion);
        assertThat(d.get("numeroAsientoReversion").asText()).isEqualTo(numeroReversion);
        assertThat(d.get("puedeEmitirSustituta").asBoolean()).isTrue();

        assertThat(mockMvc.perform(get("/api/facturas/notas-credito/9999999/detalle")
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("T-042: el historial del detalle lleva todos los comprobantes de la nota")
    void detalle_historialConVariosComprobantes() throws Exception {
        int idFactura = facturaAceptada();
        Nota nota = emitirOk(idFactura, "13", "Con historial");
        tx.executeWithoutResult(s -> {
            ComprobanteElectronico vigente = comprobanteRepository.findById(nota.idComprobante()).orElseThrow();
            vigente.setVigente(false);
            vigente.setEstado(EstadoComprobante.RECHAZADO);
            comprobanteRepository.save(vigente);
        });
        tx.executeWithoutResult(s -> {
            ComprobanteElectronico anterior = comprobanteRepository.findById(nota.idComprobante()).orElseThrow();
            comprobanteRepository.save(ComprobanteElectronico.builder()
                    .notaFactura(anterior.getNotaFactura())
                    .empresa(anterior.getEmpresa())
                    .tipoComprobante(TipoComprobanteElectronico.NOTA_CREDITO)
                    .claveNumerica(clave())
                    .consecutivo(String.format("%020d", 7777L))
                    .fechaEmision(LocalDateTime.now().withNano(0))
                    .estado(EstadoComprobante.FIRMADO)
                    .vigente(true)
                    .build());
        });

        JsonNode d = detalle(nota.id());

        assertThat(d.get("historial")).hasSize(2);
        assertThat(d.get("historial").get(0).get("id").asLong()).isEqualTo(nota.idComprobante());
        assertThat(d.get("historial").get(0).get("estado").asText()).isEqualTo("RECHAZADO");
        assertThat(d.get("historial").get(1).get("estado").asText()).isEqualTo("FIRMADO");
        assertThat(d.get("comprobante").get("estado").asText()).isEqualTo("FIRMADO");
        assertThat(d.get("comprobante").get("puedeReenviar").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("T-042: PDF de la nota → application/pdf con el nombre nota-credito-<id>.pdf; 404 si no existe")
    void pdfDeLaNota() throws Exception {
        int idFactura = facturaAceptada();
        Nota nota = emitirOk(idFactura, "01", "Para el PDF");
        // La base de la clase no trae la empresa única (ADR-019); el PDF la lee en una transacción de solo lectura
        empresaService.getEmpresaPrincipal();

        MvcResult pdf = mockMvc.perform(get("/facturas/notas-credito/" + nota.id() + "/pdf")
                .with(user(USUARIO).roles("ADMIN"))).andReturn();

        assertThat(pdf.getResponse().getStatus()).as(pdf.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(pdf.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(pdf.getResponse().getHeader("Content-Disposition")).contains("nota-credito-" + nota.id() + ".pdf");
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 5)).isEqualTo("%PDF-");
        assertThat(mockMvc.perform(get("/facturas/notas-credito/9999999/pdf").with(user(USUARIO).roles("ADMIN")))
                .andReturn().getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("T-042: bandera del modal: aceptada → puede; con nota en trámite → no; con pagos → deshabilitada con motivo")
    void detalleFactura_banderaDeLaNota() throws Exception {
        int idFactura = facturaAceptada();
        JsonNode antes = detalleFactura(idFactura);
        assertThat(antes.get("puedeEmitirNotaCredito").asBoolean()).isTrue();
        assertThat(antes.get("motivoNotaCreditoNoDisponible").isNull()).isTrue();

        emitirOk(idFactura, "01", "Bandera");
        JsonNode despues = detalleFactura(idFactura);
        assertThat(despues.get("puedeEmitirNotaCredito").asBoolean()).isFalse();
        assertThat(despues.get("motivoNotaCreditoNoDisponible").isNull()).isTrue();

        // T-048: con pagos la nota se ofrece
        int conPagos = facturaAceptada();
        doReturn(List.of(new Pago())).when(pagoService).findPagosValidosByFacturaId(conPagos);
        JsonNode pagos = detalleFactura(conPagos);
        assertThat(pagos.get("puedeEmitirNotaCredito").asBoolean()).isTrue();
        assertThat(pagos.get("motivoNotaCreditoNoDisponible").isNull()).isTrue();

        int sinComprobante = facturaConLineasAyB();
        assertThat(detalleFactura(sinComprobante).get("puedeEmitirNotaCredito").asBoolean()).isFalse();
    }

    private JsonNode detalleFactura(int idFactura) throws Exception {
        MvcResult r = mockMvc.perform(get("/facturas/detalle/" + idFactura).with(user(USUARIO).roles("ADMIN")))
                .andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    private JsonNode detalle(Long idNota) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/facturas/notas-credito/" + idNota + "/detalle")
                .with(user(USUARIO).roles("ADMIN"))).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data");
    }

    /** GET del listado con pares nombre, valor; devuelve la página (data). */
    private JsonNode listar(String... parametros) throws Exception {
        var request = get("/api/facturas/notas-credito").with(user(USUARIO).roles("ADMIN"));
        for (int i = 0; i < parametros.length; i += 2) {
            request = request.param(parametros[i], parametros[i + 1]);
        }
        MvcResult r = mockMvc.perform(request).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data");
    }

    private static List<Long> ids(JsonNode pagina) {
        List<Long> ids = new ArrayList<>();
        pagina.get("contenido").forEach(f -> ids.add(f.get("id").asLong()));
        return ids;
    }

    // ── T-039: factura sustituta (ADR-045), de punta a punta ─────────────────

    @ParameterizedTest(name = "nota {0} → sustituta {1}")
    @org.junit.jupiter.params.provider.CsvSource({"13,15,2026-09-29", "01,07,"})
    @DisplayName("T-039: factura aceptada → nota aceptada → sustituta por el endpoint → emitida con 07/15, XML válido "
            + "contra el XSD y un solo asiento de venta (T-046: asiento con su fecha de emisión; período 15 = mes original, 07 = mes de emisión)")
    void sustituta_puntaAPunta(String codigoNota, String codigoSustituta, String fechaOriginal) throws Exception {
        int idFactura = facturaAceptada();
        if (fechaOriginal != null) {
            jdbc.update("UPDATE comprobante_electronico SET fecha_emision = ? WHERE factura_id = ? AND vigente = b'1'",
                    fechaOriginal + " 10:15:30", idFactura);
        }
        String claveOriginal = claveVigente(idFactura);
        Nota nota = emitirOk(idFactura, codigoNota, "Anulación para sustituir");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
        assertThat(estadoNota(nota.id())).isEqualTo("APLICADA");
        String numeroOriginal = jdbc.queryForObject("SELECT numero_factura FROM factura WHERE id_factura = ?",
                String.class, idFactura);

        MvcResult creada = sustituta(idFactura, null);

        assertThat(creada.getResponse().getStatus()).as(creada.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode data = objectMapper.readTree(creada.getResponse().getContentAsString()).get("data");
        int idSustituta = data.get("idFactura").asInt();
        assertThat(data.get("numeroFactura").asText()).isNotBlank().isNotEqualTo(numeroOriginal);
        Map<String, Object> fila = jdbc.queryForMap("SELECT sustituye_factura_id, codigo_sustitucion, "
                + "razon_sustitucion, total, anulada FROM factura WHERE id_factura = ?", idSustituta);
        assertThat(((Number) fila.get("sustituye_factura_id")).intValue()).isEqualTo(idFactura);
        assertThat(fila.get("codigo_sustitucion")).isEqualTo(codigoSustituta);
        assertThat((String) fila.get("razon_sustitucion")).startsWith("Sustituye la factura " + numeroOriginal
                + " anulada por la nota ");
        assertThat((BigDecimal) fila.get("total")).isEqualByComparingTo("1630.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM linea_factura WHERE id_factura = ?", Integer.class,
                idSustituta)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico WHERE factura_id = ?",
                Integer.class, idSustituta)).isZero();

        // Exactamente un asiento de venta, fechado según el código
        List<Map<String, Object>> ventas = jdbc.queryForList("SELECT fecha FROM asientos_contables "
                + "WHERE factura_id = ? AND tipo = 'AUTOMATICO_VENTA'", idSustituta);
        assertThat(ventas).hasSize(1);
        // T-046 (ADR-050): el asiento lleva la fecha de emisión de la sustituta, también con 15; el mes de la
        // original solo decide el período de atribución
        assertThat(ventas.get(0).get("fecha").toString()).isEqualTo(LocalDate.now(reloj).toString());
        Map<String, Object> atribucion = jdbc.queryForMap("SELECT fecha_contable, periodo_fiscal_atribucion "
                + "FROM factura WHERE id_factura = ?", idSustituta);
        assertThat(atribucion.get("fecha_contable").toString()).isEqualTo(LocalDate.now(reloj).toString());
        assertThat(atribucion.get("periodo_fiscal_atribucion")).isEqualTo(fechaOriginal != null
                ? fechaOriginal.substring(0, 7) : java.time.YearMonth.from(LocalDate.now(reloj)).toString());

        // Una segunda sustituta de la misma original: 409 con el número de la primera y nada nuevo
        MvcResult segunda = sustituta(idFactura, null);
        assertThat(segunda.getResponse().getStatus()).isEqualTo(409);
        assertThat(objectMapper.readTree(segunda.getResponse().getContentAsString()).get("message").asText())
                .isEqualTo("La factura ya tiene una sustituta: " + data.get("numeroFactura").asText());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE sustituye_factura_id = ?", Integer.class,
                idFactura)).isEqualTo(1);

        // Emisión: el XML firmado lleva la referencia y valida contra el XSD real
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/procesar/" + idSustituta)
                .with(user(USUARIO).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();
        String xml = jdbc.queryForObject("SELECT xml_comprobante FROM comprobante_electronico WHERE factura_id = ? "
                + "AND vigente = b'1'", String.class, idSustituta);
        assertThat(XmlValidator.erroresContraXsd(xml, TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<InformacionReferencia>(.*?)</InformacionReferencia>")
                .matcher(xml);
        assertThat(m.find()).isTrue();
        String referencia = m.group(1);
        assertThat(referencia).contains("<TipoDocIR>01</TipoDocIR>")
                .contains("<Numero>" + claveOriginal + "</Numero>")
                .contains("<Codigo>" + codigoSustituta + "</Codigo>")
                .contains("<Razon>Sustituye la factura " + numeroOriginal + " anulada por la nota ")
                .contains("<FechaEmisionIR>" + (fechaOriginal != null ? fechaOriginal + "T10:15:30" : ""));
        assertThat(m.find()).as("una sola referencia").isFalse();
    }


    @Test
    @DisplayName("T-046: con facturacion.notas.atribucion.codigo-15 = EMISION la sustituta 15 se atribuye al mes de "
            + "emisión; el cambio no toca la nota ya creada")
    void sustituta15_conParametroEmision() throws Exception {
        int idFactura = facturaAceptada();
        jdbc.update("UPDATE comprobante_electronico SET fecha_emision = '2026-09-29 10:15:30' WHERE factura_id = ? "
                + "AND vigente = b'1'", idFactura);
        Nota nota = emitirOk(idFactura, "13", "Anulación para sustituir");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
        assertThat(jdbc.queryForObject("SELECT periodo_fiscal_atribucion FROM nota_factura WHERE id = ?",
                String.class, nota.id())).isEqualTo("2026-09");
        try {
            org.mockito.Mockito.doReturn(PeriodoAtribucion.EMISION).when(atribucionFiscal)
                    .atribucionDe(CodigoReferenciaNota.SUSTITUYE_COMPROBANTE_ERROR_MATERIAL);

            MvcResult creada = sustituta(idFactura, null);

            assertThat(creada.getResponse().getStatus()).as(creada.getResponse().getContentAsString()).isEqualTo(201);
            int idSustituta = objectMapper.readTree(creada.getResponse().getContentAsString()).get("data")
                    .get("idFactura").asInt();
            assertThat(jdbc.queryForObject("SELECT periodo_fiscal_atribucion FROM factura WHERE id_factura = ?",
                    String.class, idSustituta)).isEqualTo(java.time.YearMonth.from(LocalDate.now(reloj)).toString());
            assertThat(jdbc.queryForObject("SELECT periodo_fiscal_atribucion FROM nota_factura WHERE id = ?",
                    String.class, nota.id())).as("la nota conserva su período").isEqualTo("2026-09");
        } finally {
            org.mockito.Mockito.reset(atribucionFiscal);
        }
    }

    @Test
    @DisplayName("T-046: editar los montos de una sustituta 15 (cuerpo sin los campos @JsonIgnore) conserva fecha "
            + "contable y período, y el asiento de venta nuevo lleva la fecha contable de la sustituta")
    void sustituta_editarMontos_conservaAtribucion() throws Exception {
        int idFactura = facturaAceptada();
        jdbc.update("UPDATE comprobante_electronico SET fecha_emision = '2026-09-29 10:15:30' WHERE factura_id = ? "
                + "AND vigente = b'1'", idFactura);
        Nota nota = emitirOk(idFactura, "13", "Anulación para sustituir");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
        MvcResult creada = sustituta(idFactura, null);
        assertThat(creada.getResponse().getStatus()).as(creada.getResponse().getContentAsString()).isEqualTo(201);
        int idSustituta = objectMapper.readTree(creada.getResponse().getContentAsString()).get("data")
                .get("idFactura").asInt();

        // Lo que llega del controlador: la factura sin fechaContable ni periodoFiscalAtribucion (@JsonIgnore)
        tx.executeWithoutResult(s -> {
            Factura actual = facturaRepository.findById(idSustituta).orElseThrow();
            Factura editada = new Factura();
            editada.setIdFactura(idSustituta);
            editada.setCliente(actual.getCliente());
            editada.setNumeroFactura(actual.getNumeroFactura());
            editada.setSerie(actual.getSerie());
            editada.setMonedaFE(actual.getMonedaFE());
            editada.setTipoCambio(actual.getTipoCambio());
            editada.setSubtotal(new BigDecimal("2000.00"));
            editada.setIgv(new BigDecimal("260.00"));
            editada.setTotal(new BigDecimal("2260.00"));
            editada.setAnulada(false);
            editada.setCreateDate(actual.getCreateDate());
            facturaService.save(editada);
        });

        Map<String, Object> fila = jdbc.queryForMap("SELECT fecha_contable, periodo_fiscal_atribucion, "
                + "sustituye_factura_id, codigo_sustitucion FROM factura WHERE id_factura = ?", idSustituta);
        assertThat(fila.get("fecha_contable").toString()).isEqualTo(LocalDate.now(reloj).toString());
        assertThat(fila.get("periodo_fiscal_atribucion")).isEqualTo("2026-09");
        assertThat(fila.get("codigo_sustitucion")).isEqualTo("15");
        List<Map<String, Object>> ventas = jdbc.queryForList("SELECT fecha FROM asientos_contables WHERE factura_id = ? "
                + "AND tipo = 'AUTOMATICO_VENTA' AND asiento_revertido_id IS NULL", idSustituta);
        assertThat(ventas).isNotEmpty().allSatisfy(v ->
                assertThat(v.get("fecha").toString()).isEqualTo(LocalDate.now(reloj).toString()));
    }

    @ParameterizedTest(name = "nota {0} → sustituta {1}")
    @org.junit.jupiter.params.provider.CsvSource({"01,07", "13,15"})
    @DisplayName("T-039 review 1 (ADR-037 §6, criterios 18-21): la sustituta no mueve inventario ni genera costo de venta; "
            + "el stock tras original + nota + sustituta aceptada es el de la venta original")
    void sustituta_noMueveInventario(String codigoNota, String codigoSustituta) throws Exception {
        jdbc.update("UPDATE producto SET stock = 10 WHERE id_producto IN (?, ?)", gravado13.getIdProducto(),
                exento.getIdProducto());
        List<String> tabla = new ArrayList<>();
        tabla.add("nota " + codigoNota + " -> sustituta " + codigoSustituta);
        tabla.add(String.format("%-28s %6s %6s | %-22s | %-22s | %s", "paso", "stockA", "stockB",
                "SALIDA_VENTA orig(n/qty)", "SALIDA_VENTA sust(n/qty)", "asientos venta(+reversion) orig/otros/sust, costo"));

        // Original con líneas en el cuerpo de save(): el camino que registra la salida de inventario
        int idFactura = facturaConLineasEnElCuerpo();
        instantanea("1 original creada", idFactura, null, tabla);
        int stockA = stock(gravado13), stockB = stock(exento);
        assertThat(stockA).as("la venta original descuenta 1").isEqualTo(9);
        assertThat(stockB).isEqualTo(9);
        assertThat(salidasVenta(idFactura)).isEqualTo(List.of(-1, -1));
        comprobanteDeFactura(idFactura, EstadoComprobante.ACEPTADO);
        instantanea("2 original aceptada", idFactura, null, tabla);

        Nota nota = emitirOk(idFactura, codigoNota, "Anulación para sustituir");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
        assertThat(estadoNota(nota.id())).isEqualTo("APLICADA");
        instantanea("3 nota " + codigoNota + " aceptada", idFactura, null, tabla);

        MvcResult creada = sustituta(idFactura, null);
        assertThat(creada.getResponse().getStatus()).as(creada.getResponse().getContentAsString()).isEqualTo(201);
        int idSustituta = objectMapper.readTree(creada.getResponse().getContentAsString()).get("data")
                .get("idFactura").asInt();
        instantanea("4 sustituta creada", idFactura, idSustituta, tabla);

        mockMvc.perform(post("/api/facturas/electronica/comprobantes/procesar/" + idSustituta)
                .with(user(USUARIO).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();
        String claveSustituta = claveVigente(idSustituta);
        when(haciendaApi.consultarComprobante(eq(claveSustituta), anyLong())).thenReturn(consulta("aceptado"));
        Long idComprobante = jdbc.queryForObject("SELECT id FROM comprobante_electronico WHERE factura_id = ? "
                + "AND vigente = b'1'", Long.class, idSustituta);
        comprobanteService.consultarYActualizarEstado(idComprobante);
        assertThat(estadoComprobante(idComprobante)).isEqualTo("ACEPTADO");
        instantanea("5 sustituta aceptada", idFactura, idSustituta, tabla);
        System.out.println("\nT039-INVENTARIO\n" + String.join("\n", tabla) + "\n");

        assertThat(stock(gravado13)).as("stock tras sustituta aceptada = stock tras la venta original").isEqualTo(stockA);
        assertThat(stock(exento)).isEqualTo(stockB);
        assertThat(salidasVenta(idFactura)).as("una sola salida: la de la original").isEqualTo(List.of(-1, -1));
        assertThat(salidasVenta(idSustituta)).as("la sustituta no registra salida").isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM movimientos_inventario WHERE tipo = 'SALIDA_VENTA' "
                + "AND documento_origen_id IN (?, ?)", Integer.class, idFactura, idSustituta)).isEqualTo(2);
        assertThat(costosDeVenta()).as("el sistema no tiene asientos de costo de venta").isZero();
        assertThat(asientoVentaVigente(idSustituta)).as("la sustituta conserva su asiento de ingreso e IVA").isNotNull();
        // ADR-052: la nota ya no revierte el asiento de la venta; lo compensa con su propio asiento
        assertThat(asientoVentaVigente(idFactura)).as("el de la original sigue vigente").isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE nota_factura_id IS NOT NULL "
                + "AND factura_id = ?", Integer.class, idFactura)).as("la nota tiene su asiento").isEqualTo(1);
    }

    private void instantanea(String paso, int idOriginal, Integer idSustituta, List<String> tabla) {
        List<Integer> orig = salidasVenta(idOriginal);
        List<Integer> sust = idSustituta == null ? List.of() : salidasVenta(idSustituta);
        int ventasOrig = jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE factura_id = ? AND "
                + "tipo = 'AUTOMATICO_VENTA'", Integer.class, idOriginal);
        int revOrig = jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE factura_id = ? AND "
                + "tipo <> 'AUTOMATICO_VENTA'", Integer.class, idOriginal);
        int ventasSust = idSustituta == null ? 0 : jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables "
                + "WHERE factura_id = ? AND tipo = 'AUTOMATICO_VENTA'", Integer.class, idSustituta);
        tabla.add(String.format("%-28s %6d %6d | %-24s | %-24s | %d/%d/%d, costo=%d", paso, stock(gravado13),
                stock(exento), orig.size() + "/" + orig.stream().mapToInt(Integer::intValue).sum(),
                sust.size() + "/" + sust.stream().mapToInt(Integer::intValue).sum(), ventasOrig, revOrig, ventasSust,
                costosDeVenta()));
    }

    private int stock(Producto p) {
        return jdbc.queryForObject("SELECT stock FROM producto WHERE id_producto = ?", Integer.class,
                p.getIdProducto());
    }

    /** Cantidades (negativas) de las salidas SALIDA_VENTA de la factura, por id de movimiento. */
    private List<Integer> salidasVenta(int idFactura) {
        return jdbc.queryForList("SELECT cantidad FROM movimientos_inventario WHERE tipo = 'SALIDA_VENTA' "
                + "AND documento_origen = 'FACTURA' AND documento_origen_id = ? ORDER BY id",
                Integer.class, (long) idFactura);
    }

    /** Asientos de costo de venta: el sistema no tiene tipo ni cuenta de costo; se busca por tipo, concepto y cuenta. */
    private int costosDeVenta() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE tipo NOT IN ('AUTOMATICO_VENTA', "
                + "'AUTOMATICO_REVERSION') AND (LOWER(concepto) LIKE '%costo%' OR LOWER(tipo) LIKE '%costo%')",
                Integer.class);
    }

    /** Original con total, líneas y stock descontado por FacturaService.save(): las líneas van en el cuerpo. */
    private int facturaConLineasEnElCuerpo() {
        return tx.execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T039 " + UUID.randomUUID());
            cliente.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_FISICA);
            cliente.setNumeroIdentificacion("109990999");
            cliente.setTelefono("88887777");
            cliente.setEmail("cliente@t039.example");
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T039-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T039");
            factura.setMonedaFE(MonedaFE.CRC);
            factura.setTipoCambio(BigDecimal.ONE);
            factura.setSubtotal(new BigDecimal("1500.00"));
            factura.setIgv(new BigDecimal("130.00"));
            factura.setTotal(new BigDecimal("1630.00"));
            factura.setAnulada(false);
            int n = 1;
            for (Object[] l : new Object[][] {{gravado13, "1130.00"}, {exento, "500.00"}}) {
                LineaFactura linea = new LineaFactura();
                linea.setFactura(factura);
                linea.setNumeroLinea(n++);
                linea.setProducto((Producto) l[0]);
                linea.setCantidad(1);
                linea.setPrecioUnitario(new BigDecimal((String) l[1]));
                linea.setSubtotal(new BigDecimal((String) l[1]));
                factura.getLineas().add(linea);
            }
            return facturaService.save(factura).getIdFactura();
        });
    }

    @Test
    @DisplayName("T-039: con la nota todavía en trámite (factura no anulada) no se crea la sustituta: 409 y nada nuevo")
    void sustituta_conNotaEnTramite_409() throws Exception {
        int idFactura = facturaAceptada();
        emitirOk(idFactura, "13", "Anulación para sustituir");

        MvcResult r = sustituta(idFactura, null);

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(objectMapper.readTree(r.getResponse().getContentAsString()).get("message").asText())
                .isEqualTo("La factura no está anulada por una nota de crédito total aceptada");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE sustituye_factura_id = ?", Integer.class,
                idFactura)).isZero();
    }

    @Test
    @DisplayName("T-039 (criterio 6): si la original deja de cumplir antes de emitir, 409 sin consumir consecutivo")
    void sustituta_originalRota_409SinConsecutivo() throws Exception {
        int idFactura = facturaAceptada();
        Nota nota = emitirOk(idFactura, "13", "Anulación para sustituir");
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
        int idSustituta = objectMapper.readTree(sustituta(idFactura, "Corrige el precio").getResponse()
                .getContentAsString()).get("data").get("idFactura").asInt();
        jdbc.update("UPDATE nota_factura SET estado = 'RECHAZADA' WHERE id = ?", nota.id());
        long antes = configuracionHaciendaRepository.findById(configHacienda.getId()).orElseThrow()
                .getConsecutivoFactura();

        MvcResult r = mockMvc.perform(post("/api/facturas/electronica/comprobantes/procesar/" + idSustituta)
                .with(user(USUARIO).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(configuracionHaciendaRepository.findById(configHacienda.getId()).orElseThrow()
                .getConsecutivoFactura()).isEqualTo(antes);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico WHERE factura_id = ?",
                Integer.class, idSustituta)).isZero();
    }

    private MvcResult sustituta(int idFactura, String razon) throws Exception {
        var req = post("/api/facturas/" + idFactura + "/sustituta").with(user(USUARIO).roles("ADMIN")).with(csrf());
        if (razon != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content("{\"razon\":\"" + razon + "\"}");
        }
        return mockMvc.perform(req).andReturn();
    }

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
                    .ambiente(api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda.SANDBOX)
                    .claveNumerica(clave())
                    .consecutivo(String.format("%020d", ThreadLocalRandom.current().nextLong(1, 1_000_000_000L)))
                    .fechaEmision(LocalDateTime.now().minusDays(1).withNano(0))
                    .fechaEnvio(LocalDateTime.now().minusDays(1))
                    .estado(estado)
                    .vigente(true)
                    .build();
            c.setXmlComprobante(firmaDigital.firmarXml(xmlGenerator.generarXmlFactura(c), configHacienda.getRutaCertificado(),
                    configHacienda.getPinCertificado()));
            comprobanteRepository.save(c);
        });
    }

    private int facturaConLineasAyB() throws Exception {
        int idFactura = tx.execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T032 " + UUID.randomUUID());
            cliente.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_FISICA);
            cliente.setNumeroIdentificacion("109990999");
            cliente.setTelefono("88887777");
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

    private BigDecimal debeEn(long idAsiento, String codigoCuenta) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(d.debe), 0) FROM detalles_asiento d JOIN cuentas_contables c "
                + "ON c.id = d.cuenta_id WHERE d.asiento_id = ? AND c.codigo = ?", BigDecimal.class, idAsiento,
                codigoCuenta);
    }

    private BigDecimal haberEn(long idAsiento, String codigoCuenta) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(d.haber), 0) FROM detalles_asiento d JOIN cuentas_contables c "
                + "ON c.id = d.cuenta_id WHERE d.asiento_id = ? AND c.codigo = ?", BigDecimal.class, idAsiento,
                codigoCuenta);
    }

    private void assertCuadra(long idAsiento) {
        Map<String, Object> t = jdbc.queryForMap("SELECT COALESCE(SUM(debe), 0) AS debe, COALESCE(SUM(haber), 0) AS haber "
                + "FROM detalles_asiento WHERE asiento_id = ?", idAsiento);
        assertThat((BigDecimal) t.get("debe")).isEqualByComparingTo((BigDecimal) t.get("haber"));
        assertThat((BigDecimal) t.get("debe")).isPositive();
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
