package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.exception.FacturaBloqueadaException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.scheduled.ComprobanteReintentosJob;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFacturaR;
import api.astro.whats_orders_manager.modules.facturacion.service.LineaFacturaService;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.LineaFacturaRepository;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.repository.ArticuloMaestroRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-025 (ADR-030, P-048) — los campos fiscales de una factura no se pueden modificar mientras su comprobante
 * electrónico vigente está en Hacienda (ENVIADO, PROCESANDO, ACEPTADO) o pudo haber llegado (ENVIO_INCIERTO).
 *
 * Cada caso crea una factura con dos líneas por la ruta de la UI (asiento de venta incluido), le asocia un
 * comprobante en el estado indicado y prueba cada ruta que modifica la factura: si la política la bloquea, la
 * respuesta es 409 y la factura, sus líneas y el asiento vigente quedan iguales.
 *
 * Base: MySQL 8 en Testcontainers (t025-it-mysql, tmpfs, sin volúmenes); esquema de Hibernate y rutinas de
 * database/routines, como en LineaFacturaSincronizacionIT. El cliente de Hacienda y la configuración de Hacienda
 * están simulados (@MockitoBean); generador, firma (certificado de prueba) y validador XSD son los reales: ninguna prueba llama a Hacienda.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-025 — bloqueo fiscal de facturas según el comprobante electrónico (MySQL)")
class BloqueoFiscalFacturaIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t025")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t025-it-mysql"))
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

    /** Estados del comprobante vigente que bloquean los campos fiscales (ADR-030 §2). */
    private static final Set<String> BLOQUEANTES = Set.of("ENVIO_INCIERTO", "ENVIADO", "PROCESANDO", "ACEPTADO",
            "ERROR_HACIENDA");

    @MockitoBean private HaciendaApiService haciendaApi;
    /** T-036: generador, firma y validador XSD son los reales; el certificado es uno de prueba generado con keytool. */
    @MockitoBean private ConfiguracionHaciendaService configuracionHaciendaService;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private LineaFacturaRepository lineaFacturaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ArticuloMaestroRepository articuloMaestroRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ComprobanteReintentosJob reintentosJob;
    @Autowired private LineaFacturaService lineaFacturaService;
    // T-037: espía para simular que la BD falla al registrar el resultado del envío (etapa d)
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private ComprobanteElectronicoRepository comprobanteRepository;
    @Autowired private api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService comprobanteService;
    @Autowired private api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService envioService;
    @Autowired private RelojAjustable reloj;

    /**
     * Reloj de la aplicación en estas pruebas: el del sistema, salvo que una prueba lo adelante (job de sondeo con
     * consultas repetidas). Reemplaza al bean de ClockConfig.
     */
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
    private Integer idEmpresa;
    /** Configuración de Hacienda activa simulada; una sola por clase para que los consecutivos no se repitan. */
    private ConfiguracionHacienda configHacienda;
    @Autowired private api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository configuracionHaciendaRepository;
    /** XML firmado real (generador + firma + XSD) que las filas insertadas por SQL llevan en vez de un stub. */
    private String xmlFirmadoReal;

    @AfterEach
    void tearDown() {
        reloj.reiniciar();
        SecurityContextHolder.clearContext();
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
                    .orElseGet(() -> ConfiguracionEmpresa.builder().razonSocial("Empresa T025 S.A.").build());
            empresa.setNumeroIdentificacion("3101234567");
            empresa.setCodigoActividad("4773.0");
            empresa.setCodigoProvincia("1");
            empresa.setCanton("01");
            empresa.setDistrito("01");
            empresa.setTelefono("22223333");
            empresa.setEmail("facturacion@t025.example");
            empresa.setOtrasSenas("Avenida Central, San José");
            empresa = empresaRepository.save(empresa);
            idEmpresa = empresa.getIdConfiguracion();
            // Configuración de Hacienda activa simulada, de la misma empresa (ADR-021)
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
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(RespuestaHaciendaDTO.builder()
                .resultadoEnvio(ResultadoEnvioHacienda.RECIBIDO).codigoHttp(202).exitoso(false)
                .mensaje("Comprobante recibido por Hacienda, en procesamiento").build());
    }

    // ── Líneas: PUT /lineas-factura/actualizar ───────────────────────────────

    @ParameterizedTest(name = "comprobante {0}: quitar una línea")
    @ValueSource(strings = {"SIN_COMPROBANTE", "GENERADO", "FIRMADO", "ERROR", "NO_TRANSMITIDO", "RECHAZADO",
            "ENVIO_INCIERTO", "ENVIADO", "PROCESANDO", "ACEPTADO", "ERROR_HACIENDA"})
    @DisplayName("PUT /lineas-factura/actualizar: 200 si el estado lo permite; 409 sin cambios si bloquea")
    void lineas_segunEstado(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estado);
        Estado antes = estado(idFactura);

        int status = guardarLineas(List.of(linea(antes.lineas().get(0).id(), idFactura, gravado13, 1, "1130.00")))
                .getResponse().getStatus();

        if (BLOQUEANTES.contains(estado)) {
            assertThat(status).isEqualTo(409);
            assertThat(estado(idFactura)).isEqualTo(antes);
        } else {
            assertThat(status).isEqualTo(200);
            assertThat(lineas(idFactura)).hasSize(1);
            assertThat(totales(idFactura).total()).isEqualByComparingTo("1130.00");
        }
    }

    // ── Factura: POST /facturas/guardar (edición) ────────────────────────────

    @ParameterizedTest(name = "comprobante {0}: cambiar la condición de venta")
    @ValueSource(strings = {"SIN_COMPROBANTE", "GENERADO", "FIRMADO", "ERROR", "NO_TRANSMITIDO", "RECHAZADO",
            "ENVIO_INCIERTO", "ENVIADO", "PROCESANDO", "ACEPTADO", "ERROR_HACIENDA"})
    @DisplayName("POST /facturas/guardar con idFactura: 200 si el estado lo permite; 409 sin cambios si bloquea")
    void guardarFactura_segunEstado(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estado);
        Estado antes = estado(idFactura);

        int status = mockMvc.perform(post("/facturas/guardar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facturaEditada(idFactura, "CREDITO"))))
                .andReturn().getResponse().getStatus();

        if (BLOQUEANTES.contains(estado)) {
            assertThat(status).isEqualTo(409);
            assertThat(estado(idFactura)).isEqualTo(antes);
            assertThat(condicionVenta(idFactura)).isNotEqualTo("CREDITO");
        } else {
            assertThat(status).isEqualTo(200);
            assertThat(condicionVenta(idFactura)).isEqualTo("CREDITO");
        }
    }

    // ── Eliminar y anular ────────────────────────────────────────────────────

    @ParameterizedTest(name = "comprobante {0}: eliminar")
    @ValueSource(strings = {"ENVIO_INCIERTO", "ENVIADO", "PROCESANDO", "ACEPTADO", "ERROR_HACIENDA"})
    @DisplayName("GET /facturas/eliminar/{id} con comprobante en Hacienda → 409 y la factura sigue")
    void eliminar_bloqueado(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estado);
        Estado antes = estado(idFactura);

        int status = mockMvc.perform(get("/facturas/eliminar/" + idFactura)
                        .with(user(USUARIO).roles("ADMIN")).accept(MediaType.TEXT_HTML))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(409);
        assertThat(estado(idFactura)).isEqualTo(antes);
        assertThat(comprobantes(idFactura)).isEqualTo(1);
    }

    @ParameterizedTest(name = "comprobante {0}: anular")
    @ValueSource(strings = {"ENVIO_INCIERTO", "ENVIADO", "PROCESANDO", "ACEPTADO", "ERROR_HACIENDA"})
    @DisplayName("POST /facturas/anular/{id} con comprobante en Hacienda → 409 y la factura no se anula")
    void anular_bloqueado(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estado);
        Estado antes = estado(idFactura);

        int status = mockMvc.perform(post("/facturas/anular/" + idFactura)
                        .param("motivo", "Prueba T-025")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()).accept(MediaType.TEXT_HTML))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(409);
        assertThat(estado(idFactura)).isEqualTo(antes);
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class, idFactura))
                .isFalse();
    }

    // ── Campo operativo ──────────────────────────────────────────────────────

    @Test
    @DisplayName("con comprobante ACEPTADO, cambiar observaciones (PUT /facturas/actualizar-estado) → 200")
    void campoOperativo_conAceptado_permitido() throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, "ACEPTADO");

        int status = mockMvc.perform(put("/facturas/actualizar-estado/" + idFactura)
                        .param("entregado", "true").param("descripcion", "Observación interna T-025")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT descripcion FROM factura WHERE id_factura = ?", String.class,
                idFactura)).isEqualTo("Observación interna T-025");
    }

    // ── Listener JPA en LineaFactura ─────────────────────────────────────────

    @ParameterizedTest(name = "comprobante {0}")
    @ValueSource(strings = {"ACEPTADO", "ENVIO_INCIERTO", "ERROR_HACIENDA"})
    @DisplayName("listener: con comprobante bloqueante, lineaFacturaRepository.save directo → excepción y BD sin cambios")
    void listener_saveDirecto_rechazado(String estadoComprobante) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estadoComprobante);
        Estado antes = estado(idFactura);
        int idLinea = antes.lineas().get(0).id();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            LineaFactura linea = lineaFacturaRepository.findById(idLinea).orElseThrow();
            linea.setCantidad(7);
            lineaFacturaRepository.saveAndFlush(linea);
        })).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            LineaFactura nueva = new LineaFactura();
            nueva.setFactura(facturaRepository.findById(idFactura).orElseThrow());
            nueva.setProducto(productoRepository.findById(exento.getIdProducto()).orElseThrow());
            nueva.setNumeroLinea(3);
            nueva.setCantidad(1);
            nueva.setPrecioUnitario(new BigDecimal("500.00"));
            nueva.setSubtotal(new BigDecimal("500.00"));
            lineaFacturaRepository.saveAndFlush(nueva);
        })).isInstanceOf(RuntimeException.class);

        assertThat(estado(idFactura)).isEqualTo(antes);
    }

    @ParameterizedTest(name = "comprobante {0}")
    @ValueSource(strings = {"ACEPTADO", "ENVIO_INCIERTO", "ERROR_HACIENDA"})
    @DisplayName("listener: con comprobante bloqueante, lineaFacturaRepository.delete directo → excepción y BD sin cambios")
    void listener_deleteDirecto_rechazado(String estadoComprobante) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estadoComprobante);
        Estado antes = estado(idFactura);
        int idLinea = antes.lineas().get(1).id();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            lineaFacturaRepository.delete(lineaFacturaRepository.findById(idLinea).orElseThrow());
            lineaFacturaRepository.flush();
        })).isInstanceOf(RuntimeException.class);

        assertThat(estado(idFactura)).isEqualTo(antes);
    }

    // ── Eliminar y anular: estados que no bloquean (comportamiento actual) y sin cascade ──

    @Test
    @DisplayName("anular con comprobante GENERADO → comportamiento actual: redirect y la factura queda anulada")
    void anular_permitido() throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, "GENERADO");

        int status = mockMvc.perform(post("/facturas/anular/" + idFactura)
                        .param("motivo", "Prueba T-025")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()).accept(MediaType.TEXT_HTML))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(302);
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class, idFactura))
                .isTrue();
    }

    @Test
    @DisplayName("eliminar factura sin asiento con comprobante GENERADO → se elimina; el comprobante local se borra explícitamente")
    void eliminar_conComprobanteLocal_borraExplicitamente() throws Exception {
        int idFactura = facturaSinLineas();
        Long idComprobante = comprobante(idFactura, "GENERADO");

        int status = mockMvc.perform(get("/facturas/eliminar/" + idFactura)
                        .with(user(USUARIO).roles("ADMIN")).accept(MediaType.TEXT_HTML))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(302);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE id_factura = ?", Integer.class, idFactura))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico WHERE id = ?", Integer.class,
                idComprobante)).isZero();
    }

    @ParameterizedTest(name = "comprobante {0}")
    @ValueSource(strings = {"RECHAZADO", "NO_TRANSMITIDO"})
    @DisplayName("eliminar factura sin asiento con comprobante que pasó por Hacienda → 409; factura y comprobante intactos")
    void eliminar_conHistorialFiscal_rechazado(String estado) throws Exception {
        int idFactura = facturaSinLineas();
        Long idComprobante = comprobante(idFactura, estado);
        ComprobanteBd antes = comprobanteBd(idComprobante);

        int status = mockMvc.perform(get("/facturas/eliminar/" + idFactura)
                        .with(user(USUARIO).roles("ADMIN")).accept(MediaType.TEXT_HTML))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE id_factura = ?", Integer.class, idFactura))
                .isEqualTo(1);
        assertThat(comprobanteBd(idComprobante)).isEqualTo(antes);
    }

    @Test
    @DisplayName("409 en una llamada JSON: cuerpo con success=false, motivo accionable y estado del comprobante")
    void respuesta409_json() throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, "ACEPTADO");

        MvcResult result = guardarLineas(List.of(linea(lineas(idFactura).get(0).id(), idFactura, gravado13, 1,
                "1130.00")));

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        Map<?, ?> cuerpo = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(cuerpo.get("success")).isEqualTo(false);
        assertThat((String) cuerpo.get("message")).contains("nota de crédito o débito");
        assertThat(cuerpo.get("estadoComprobante")).isEqualTo("ACEPTADO");
    }

    // ── GENERADO / FIRMADO: editar descarta XML y firma, la clave no cambia ──

    @ParameterizedTest(name = "comprobante {0}")
    @ValueSource(strings = {"GENERADO", "FIRMADO", "ERROR", "NO_TRANSMITIDO"})
    @DisplayName("editar líneas con comprobante local → XML y firma descartados, misma clave; el envío regenera con esa clave")
    void edicion_descartaXml_mismaClave(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        Long idComprobante = comprobante(idFactura, estado);
        ComprobanteBd antes = comprobanteBd(idComprobante);

        assertThat(guardarLineas(List.of(linea(lineas(idFactura).get(0).id(), idFactura, gravado13, 1, "1130.00")))
                .getResponse().getStatus()).isEqualTo(200);

        ComprobanteBd despues = comprobanteBd(idComprobante);
        assertThat(despues.xml()).isNull();
        assertThat(despues.rutaXml()).isNull();
        assertThat(despues.clave()).isEqualTo(antes.clave());
        assertThat(despues.estado()).isEqualTo("FIRMADO".equals(estado) ? "GENERADO" : estado);

        // El envío regenera el XML con la misma clave, lo firma y lo envía con esa clave
        clearInvocations(haciendaApi);
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/" + idComprobante + "/enviar")
                .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn();
        ComprobanteBd enviado = comprobanteBd(idComprobante);
        assertThat(enviado.clave()).isEqualTo(antes.clave());
        assertThat(enviado.xml()).contains("<Clave>" + antes.clave() + "</Clave>").contains("<ds:Signature");
        assertThat(enviado.estado()).isEqualTo("ENVIADO");
        verify(haciendaApi).enviarComprobante(anyString(), eq(antes.clave()), anyLong());
    }

    // ── T-036: emisión de punta a punta con generador, firma y validador XSD reales ──

    @Test
    @DisplayName("T-036: factura 01 persistida → generador, firma y XSD reales → envío simulado a Hacienda con XML válido")
    void emitirFactura_xmlRealValidoSegunXsd_seEnvia() throws Exception {
        int idFactura = facturaConLineasAyB();
        autenticar();
        clearInvocations(haciendaApi);

        Long id = comprobanteService.generarDesdeFactura((long) idFactura).getId();
        comprobanteService.firmar(id);
        envioService.enviar(id);

        ComprobanteBd enviado = comprobanteBd(id);
        assertThat(enviado.estado()).isEqualTo("ENVIADO");
        assertThat(enviado.xml()).contains("<FacturaElectronica").contains("<ds:Signature");
        assertThat(XmlValidator.erroresContraXsd(enviado.xml(), TipoComprobanteElectronico.FACTURA_ELECTRONICA))
                .as("XML firmado según el validador XSD real").isEmpty();
        assertThat(jdbc.queryForObject("SELECT ultimo_error FROM comprobante_electronico WHERE id = ?", String.class, id))
                .isNull();
        verify(haciendaApi).enviarComprobante(anyString(), eq(enviado.clave()), anyLong());
    }

    // ── RECHAZADO: se edita y se reemite con clave nueva; el rechazado queda intacto ──

    @Test
    @DisplayName("RECHAZADO + reemitir → 2 comprobantes; el rechazado conserva clave y SHA2 del XML; el nuevo es el vigente con otra clave y consecutivo")
    void rechazado_reemitir() throws Exception {
        int idFactura = facturaConLineasAyB();
        Long idRechazado = comprobante(idFactura, "RECHAZADO");
        ComprobanteBd antes = comprobanteBd(idRechazado);

        assertThat(guardarLineas(List.of(linea(lineas(idFactura).get(0).id(), idFactura, gravado13, 1, "1130.00")))
                .getResponse().getStatus()).isEqualTo(200);
        int status = mockMvc.perform(post("/api/facturas/electronica/comprobantes/" + idRechazado + "/reemitir")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(201);
        List<ComprobanteBd> comprobantes = comprobantesBd(idFactura);
        assertThat(comprobantes).hasSize(2);
        ComprobanteBd rechazado = comprobantes.get(0);
        ComprobanteBd nuevo = comprobantes.get(1);
        assertThat(rechazado.id()).isEqualTo(idRechazado);
        assertThat(rechazado.clave()).isEqualTo(antes.clave());
        assertThat(rechazado.hashXml()).isEqualTo(antes.hashXml());
        assertThat(rechazado.estado()).isEqualTo("RECHAZADO");
        assertThat(rechazado.vigente()).isFalse();
        assertThat(nuevo.vigente()).isTrue();
        assertThat(nuevo.estado()).isEqualTo("GENERADO");
        assertThat(nuevo.clave()).isNotEqualTo(antes.clave());
        assertThat(nuevo.consecutivo()).isNotEqualTo(antes.consecutivo());
    }

    @Test
    @DisplayName("reemitir un comprobante que no está RECHAZADO → 409 y un solo comprobante")
    void reemitir_noRechazado_409() throws Exception {
        int idFactura = facturaConLineasAyB();
        Long id = comprobante(idFactura, "ACEPTADO");

        int status = mockMvc.perform(post("/api/facturas/electronica/comprobantes/" + id + "/reemitir")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(409);
        assertThat(comprobantes(idFactura)).isEqualTo(1);
    }

    // ── ENVIO_INCIERTO: consultar estado en Hacienda (cliente simulado) ──────

    @Test
    @DisplayName("consultar ENVIO_INCIERTO a los 20 min: Hacienda 404 → NO_TRANSMITIDO y la edición pasa a 200")
    void verificar_404_desbloquea() throws Exception {
        int idFactura = facturaConLineasAyB();
        Long id = comprobante(idFactura, "ENVIO_INCIERTO", LocalDateTime.now().minusMinutes(20));
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta404());
        int idLinea = lineas(idFactura).get(0).id();
        assertThat(guardarLineas(List.of(linea(idLinea, idFactura, gravado13, 1, "1130.00")))
                .getResponse().getStatus()).isEqualTo(409);

        int status = verificar(id);

        assertThat(status).isEqualTo(200);
        assertThat(comprobanteBd(id).estado()).isEqualTo("NO_TRANSMITIDO");
        assertThat(guardarLineas(List.of(linea(idLinea, idFactura, gravado13, 1, "1130.00")))
                .getResponse().getStatus()).isEqualTo(200);
        verify(haciendaApi, times(1)).consultarComprobante(eq(comprobanteBd(id).clave()), anyLong());
    }

    @Test
    @DisplayName("GET /{id}/consultar-estado no existe ni muta (T-026 revisión 1): ENVIO_INCIERTO de 16 min sigue igual y bloqueado")
    void get_consultarEstado_noExisteNiMuta() throws Exception {
        int idFactura = facturaConLineasAyB();
        Long id = comprobante(idFactura, "ENVIO_INCIERTO", LocalDateTime.now().minusMinutes(16));
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta404());

        int status = mockMvc.perform(get("/api/facturas/electronica/comprobantes/" + id + "/consultar-estado")
                        .with(user(USUARIO).roles("ADMIN")))
                .andReturn().getResponse().getStatus();

        assertThat(status).isIn(404, 405);
        assertThat(comprobanteBd(id).estado()).isEqualTo("ENVIO_INCIERTO");
        verify(haciendaApi, never()).consultarComprobante(anyString(), anyLong());
        assertThat(guardarLineas(List.of(linea(lineas(idFactura).get(0).id(), idFactura, gravado13, 1, "1130.00")))
                .getResponse().getStatus()).isEqualTo(409);
    }

    @ParameterizedTest(name = "ind-estado {0} → {1}")
    @org.junit.jupiter.params.provider.CsvSource({"aceptado,ACEPTADO,409", "rechazado,RECHAZADO,200",
            "procesando,PROCESANDO,409", "recibido,PROCESANDO,409", "error,ERROR_HACIENDA,409",
            "otro-valor,ENVIO_INCIERTO,409"})
    @DisplayName("consultar ENVIO_INCIERTO: se adopta el estado informado por Hacienda")
    void verificar_adoptaEstado(String indEstado, String estadoEsperado, int statusEdicion) throws Exception {
        int idFactura = facturaConLineasAyB();
        Long id = comprobante(idFactura, "ENVIO_INCIERTO", LocalDateTime.now().minusMinutes(20));
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta(indEstado));

        assertThat(verificar(id)).isEqualTo(200);

        assertThat(comprobanteBd(id).estado()).isEqualTo(estadoEsperado);
        assertThat(guardarLineas(List.of(linea(lineas(idFactura).get(0).id(), idFactura, gravado13, 1, "1130.00")))
                .getResponse().getStatus()).isEqualTo(statusEdicion);
    }

    @Test
    @DisplayName("consultar ENVIO_INCIERTO antes de 15 minutos (T-026): se consulta y una respuesta que no es 404 se adopta")
    void consultar_antesDe15Minutos_adoptaEstado() throws Exception {
        int idFactura = facturaConLineasAyB();
        Long id = comprobante(idFactura, "ENVIO_INCIERTO", LocalDateTime.now().minusMinutes(5));
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenReturn(consulta("aceptado"));

        assertThat(verificar(id)).isEqualTo(200);

        assertThat(comprobanteBd(id).estado()).isEqualTo("ACEPTADO");
        verify(haciendaApi, times(1)).consultarComprobante(eq(comprobanteBd(id).clave()), anyLong());
    }

    // ── Sondeo y job de reintentos ───────────────────────────────────────────

    @Test
    @DisplayName("sondeo (job horario): 404 de un ENVIADO → ENVIO_INCIERTO, nunca PROCESANDO; 'recibido' → PROCESANDO")
    void sondeo_404_noEsProcesando() throws Exception {
        int factura404 = facturaConLineasAyB();
        Long enviado404 = comprobante(factura404, "ENVIADO", LocalDateTime.now().minusMinutes(30));
        int facturaRecibido = facturaConLineasAyB();
        Long enviadoRecibido = comprobante(facturaRecibido, "ENVIADO", LocalDateTime.now().minusMinutes(30));
        String clave404 = comprobanteBd(enviado404).clave();
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenAnswer(inv ->
                clave404.equals(inv.getArgument(0)) ? consulta404() : consulta("recibido"));

        reintentosJob.consultarEstadosPendientes();

        assertThat(comprobanteBd(enviado404).estado()).isEqualTo("ENVIO_INCIERTO");
        assertThat(comprobanteBd(enviadoRecibido).estado()).isEqualTo("PROCESANDO");
    }

    @Test
    @DisplayName("job de reintentos: reenvía ERROR y NO_TRANSMITIDO con su misma clave; nunca ENVIO_INCIERTO, ENVIADO, PROCESANDO ni ACEPTADO")
    void job_reenviaSoloLoQueHaciendaNoTiene() throws Exception {
        Map<String, String> clavePorEstado = new LinkedHashMap<>();
        for (String estado : List.of("ERROR", "NO_TRANSMITIDO", "ENVIO_INCIERTO", "ENVIADO", "PROCESANDO",
                "ACEPTADO")) {
            Long id = comprobante(facturaConLineasAyB(), estado);
            clavePorEstado.put(estado, comprobanteBd(id).clave());
        }
        clearInvocations(haciendaApi);

        reintentosJob.reintentarEnviosPendientes();

        verify(haciendaApi).enviarComprobante(anyString(), eq(clavePorEstado.get("ERROR")), anyLong());
        verify(haciendaApi).enviarComprobante(anyString(), eq(clavePorEstado.get("NO_TRANSMITIDO")), anyLong());
        for (String estado : List.of("ENVIO_INCIERTO", "ENVIADO", "PROCESANDO", "ACEPTADO")) {
            verify(haciendaApi, never()).enviarComprobante(anyString(), eq(clavePorEstado.get(estado)), anyLong());
        }
    }

    // ── Acceso a datos: la política trabaja con el contexto de persistencia (revisión 2) ──

    @Test
    @DisplayName("descarte con el comprobante FIRMADO gestionado en la misma transacción: tras el commit, XML nulo y misma clave")
    void descarte_conComprobanteGestionado_noSePierde() throws Exception {
        int idFactura = facturaConLineasAyB();
        Long idComprobante = comprobante(idFactura, "FIRMADO");
        ComprobanteBd antes = comprobanteBd(idComprobante);
        int idLinea = lineas(idFactura).get(0).id();
        autenticar();

        tx.executeWithoutResult(s -> {
            // El comprobante ya está gestionado cuando se edita la factura...
            ComprobanteElectronico gestionado = comprobanteRepository.findById(idComprobante).orElseThrow();
            lineaFacturaService.updateLineas(List.of(lineaR(idLinea, idFactura, gravado13, 1, "1130.00")));
            // ...y otra operación de la misma transacción lo modifica después (flush de la entidad completa)
            gestionado.setUltimoError("Nota posterior a la edición, misma transacción");
        });

        ComprobanteBd despues = comprobanteBd(idComprobante);
        assertThat(despues.xml()).isNull();
        assertThat(despues.rutaXml()).isNull();
        assertThat(despues.estado()).isEqualTo("GENERADO");
        assertThat(despues.clave()).isEqualTo(antes.clave());
        assertThat(lineas(idFactura)).hasSize(1);
    }

    @Test
    @DisplayName("lectura coherente: en la misma transacción, comprobante pasado a ACEPTADO sin flush → editar una línea da 409")
    void lecturaCoherente_aceptadoSinFlush_409() throws Exception {
        int idFactura = facturaConLineasAyB();
        Long idComprobante = comprobante(idFactura, "GENERADO");
        Estado antes = estado(idFactura);
        int idLinea = antes.lineas().get(0).id();
        autenticar();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            comprobanteRepository.findById(idComprobante).orElseThrow().setEstado(EstadoComprobante.ACEPTADO);
            lineaFacturaService.updateLineas(List.of(lineaR(idLinea, idFactura, gravado13, 1, "1130.00")));
        })).isInstanceOf(FacturaBloqueadaException.class)
                .satisfies(e -> assertThat(((FacturaBloqueadaException) e).getDecision().estado())
                        .isEqualTo(EstadoComprobante.ACEPTADO));

        assertThat(estado(idFactura)).isEqualTo(antes);
        assertThat(comprobanteBd(idComprobante).estado()).isEqualTo("GENERADO");
    }

    @ParameterizedTest(name = "comprobante {0}")
    @ValueSource(strings = {"SIN_COMPROBANTE", "GENERADO", "FIRMADO"})
    @DisplayName("serialización: guardar factura, líneas y detalle devuelven JSON sin proxys ni LazyInitializationException")
    void serializacion_endpointsDeLaUi(String estado) throws Exception {
        int idFactura = facturaConLineasAyB();
        comprobante(idFactura, estado);

        // Las líneas primero: la edición por /facturas/guardar reemplaza la entidad completa y el cuerpo no las trae
        MvcResult lineas = guardarLineas(List.of(linea(lineas(idFactura).get(0).id(), idFactura, gravado13, 1,
                "1130.00")));
        assertThat(lineas.getResponse().getStatus()).isEqualTo(200);
        assertThat(objectMapper.readValue(lineas.getResponse().getContentAsString(), Map.class).get("idFactura"))
                .isEqualTo(idFactura);

        MvcResult guardada = mockMvc.perform(post("/facturas/guardar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(facturaEditada(idFactura, "CREDITO"))))
                .andReturn();
        assertThat(guardada.getResponse().getStatus()).isEqualTo(200);
        String cuerpoFactura = guardada.getResponse().getContentAsString();
        assertThat(cuerpoFactura).doesNotContain("hibernateLazyInitializer");
        assertThat(objectMapper.readValue(cuerpoFactura, Map.class).get("idFactura")).isEqualTo(idFactura);

        for (String url : List.of("/lineas-factura/detalle/" + idFactura, "/facturas/detalle/" + idFactura)) {
            MvcResult detalle = mockMvc.perform(get(url).with(user(USUARIO).roles("ADMIN"))
                    .accept(MediaType.APPLICATION_JSON)).andReturn();
            assertThat(detalle.getResponse().getStatus()).as(url).isEqualTo(200);
            assertThat(detalle.getResponse().getContentAsString()).as(url)
                    .doesNotContain("hibernateLazyInitializer");
            objectMapper.readTree(detalle.getResponse().getContentAsString());
        }
    }

    // ── ind-estado de la consulta: mapeo explícito (revisión 2) ──────────────

    @ParameterizedTest(name = "ind-estado {0} → {1}")
    @org.junit.jupiter.params.provider.CsvSource({"recibido,PROCESANDO", "procesando,PROCESANDO",
            "aceptado,ACEPTADO", "rechazado,RECHAZADO", "error,ERROR_HACIENDA", "desconocido,ENVIO_INCIERTO",
            "'',ENVIO_INCIERTO"})
    @DisplayName("sondeo de un ENVIADO: cada ind-estado se mapea de forma explícita; un valor desconocido → ENVIO_INCIERTO")
    void sondeo_mapeoExplicitoDeIndEstado(String indEstado, String esperado) throws Exception {
        int idFactura = facturaConLineasAyB();
        Long id = comprobante(idFactura, "ENVIADO", LocalDateTime.now().minusMinutes(30));
        String clave = comprobanteBd(id).clave();
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenAnswer(inv ->
                clave.equals(inv.getArgument(0)) ? consulta(indEstado) : consulta("procesando"));

        reintentosJob.consultarEstadosPendientes();

        assertThat(comprobanteBd(id).estado()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("ind-estado 'error': factura en 409; el sondeo repite y tras 5 consultas seguidas deja de consultar y señala revisar en ATV")
    void errorHacienda_sondeoLimitadoYSenalado() throws Exception {
        int idFactura = facturaConLineasAyB();
        Long id = comprobante(idFactura, "ENVIADO", LocalDateTime.now().minusMinutes(30));
        String clave = comprobanteBd(id).clave();
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenAnswer(inv ->
                clave.equals(inv.getArgument(0)) ? consulta("error") : consulta("procesando"));
        clearInvocations(haciendaApi);

        for (int hora = 0; hora < 8; hora++) {
            reintentosJob.consultarEstadosPendientes();
            reloj.adelantar(Duration.ofHours(1));
        }

        verify(haciendaApi, times(5)).consultarComprobante(eq(clave), anyLong());
        assertThat(comprobanteBd(id).estado()).isEqualTo("ERROR_HACIENDA");
        assertThat(jdbc.queryForObject("SELECT consultas_error_hacienda FROM comprobante_electronico WHERE id = ?",
                Integer.class, id)).isEqualTo(5);
        Estado antes = estado(idFactura);
        assertThat(guardarLineas(List.of(linea(antes.lineas().get(0).id(), idFactura, gravado13, 1, "1130.00")))
                .getResponse().getStatus()).isEqualTo(409);
        assertThat(estado(idFactura)).isEqualTo(antes);

        MvcResult detalle = mockMvc.perform(get("/api/facturas/electronica/comprobantes/" + id)
                .with(user(USUARIO).roles("ADMIN"))).andReturn();
        Map<?, ?> dto = objectMapper.readValue(detalle.getResponse().getContentAsString(), Map.class);
        assertThat(dto.get("revisarEnAtv")).isEqualTo(true);
        assertThat((String) dto.get("mensajeRespuesta")).contains("ATV");
    }

    @Test
    @DisplayName("ind-estado 'error' seguido de otro estado: el contador de consultas con error vuelve a 0")
    void errorHacienda_contadorSeReinicia() throws Exception {
        int idFactura = facturaConLineasAyB();
        Long id = comprobante(idFactura, "ENVIADO", LocalDateTime.now().minusMinutes(30));
        String clave = comprobanteBd(id).clave();
        List<String> respuestas = new ArrayList<>(List.of("error", "error", "procesando"));
        when(haciendaApi.consultarComprobante(anyString(), anyLong())).thenAnswer(inv ->
                clave.equals(inv.getArgument(0)) ? consulta(respuestas.remove(0)) : consulta("procesando"));

        for (int i = 0; i < 3; i++) {
            reintentosJob.consultarEstadosPendientes();
            reloj.adelantar(Duration.ofHours(1));
        }

        assertThat(comprobanteBd(id).estado()).isEqualTo("PROCESANDO");
        assertThat(jdbc.queryForObject("SELECT consultas_error_hacienda FROM comprobante_electronico WHERE id = ?",
                Integer.class, id)).isZero();
    }

    // ── T-037: envío en etapas cortas (ADR-043) ──────────────────────────────

    private static RespuestaHaciendaDTO recibidoTrasDemora(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return RespuestaHaciendaDTO.builder().resultadoEnvio(ResultadoEnvioHacienda.RECIBIDO).codigoHttp(202)
                .exitoso(false).mensaje("Comprobante recibido por Hacienda, en procesamiento").build();
    }

    @Test
    @DisplayName("T-037 c.6: dos hilos envían el mismo comprobante a la vez → exactamente 1 POST; reenviar con la marca → 409")
    void marca_dosHilos_unSoloPost() throws Exception {
        int idFactura = facturaConLineasAyB();
        autenticar();
        Long id = comprobanteService.generarParaEmision((long) idFactura).getId();
        java.util.concurrent.atomic.AtomicInteger posts = new java.util.concurrent.atomic.AtomicInteger();
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenAnswer(inv -> {
            posts.incrementAndGet();
            return recibidoTrasDemora(500);
        });
        java.util.concurrent.CountDownLatch salida = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService hilos = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<String>> resultados = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                resultados.add(hilos.submit(() -> {
                    autenticar();
                    salida.await();
                    try {
                        return envioService.enviar(id).getEstado().name();
                    } catch (api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService.ComprobanteNoEnviableException e) {
                        return "NO_ENVIABLE:" + e.getEstado();
                    }
                }));
            }
            salida.countDown();
            List<String> estados = new java.util.ArrayList<>();
            for (var r : resultados) {
                estados.add(r.get(30, java.util.concurrent.TimeUnit.SECONDS));
            }
            assertThat(posts.get()).as("un solo POST de recepción").isEqualTo(1);
            assertThat(estados).contains("ENVIADO");
        } finally {
            hilos.shutdownNow();
        }
        assertThat(comprobanteBd(id).estado()).isEqualTo("ENVIADO");
        assertThat(jdbc.queryForObject("SELECT intentos_envio FROM comprobante_electronico WHERE id = ?", Integer.class, id))
                .isEqualTo(1);

        // Reenvío manual sobre un comprobante en marca (ENVIO_INCIERTO): el mensaje actual de «no se puede reenviar»
        jdbc.update("UPDATE comprobante_electronico SET estado = 'ENVIO_INCIERTO' WHERE id = ?", id);
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/" + id + "/reenviar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Comprobante no puede ser reenviado en estado ENVIO_INCIERTO"));
        assertThat(posts.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("T-037 c.7: el POST lanza una excepción no clasificada → ENVIO_INCIERTO; +15 min y 404 → NO_TRANSMITIDO → reenvío con la misma clave")
    void caida_trasLaMarca_seRecuperaConLaMismaClave() throws Exception {
        int idFactura = facturaConLineasAyB();
        autenticar();
        Long id = comprobanteService.generarParaEmision((long) idFactura).getId();
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong()))
                .thenThrow(new IllegalStateException("el proceso murió a mitad del POST"));

        envioService.enviar(id);

        ComprobanteBd incierto = comprobanteBd(id);
        assertThat(incierto.estado()).isEqualTo("ENVIO_INCIERTO");
        assertThat(jdbc.queryForObject("SELECT fecha_envio FROM comprobante_electronico WHERE id = ?",
                java.sql.Timestamp.class, id)).isNotNull();

        reloj.adelantar(Duration.ofMinutes(16));
        when(haciendaApi.consultarComprobante(eq(incierto.clave()), anyLong()))
                .thenReturn(RespuestaHaciendaDTO.builder().documentoNoExiste(true).codigoHttp(404).build());
        comprobanteService.consultarYActualizarEstado(id);
        assertThat(comprobanteBd(id).estado()).isEqualTo("NO_TRANSMITIDO");

        org.mockito.Mockito.reset(haciendaApi);
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(recibidoTrasDemora(0));
        envioService.reenviar(id);

        verify(haciendaApi, times(1)).enviarComprobante(anyString(), eq(incierto.clave()), anyLong());
        ComprobanteBd reenviado = comprobanteBd(id);
        assertThat(reenviado.estado()).isEqualTo("ENVIADO");
        assertThat(reenviado.clave()).isEqualTo(incierto.clave());
        assertThat(reenviado.consecutivo()).isEqualTo(incierto.consecutivo());
    }

    @Test
    @DisplayName("T-037 c.8: la BD falla al registrar el resultado (etapa d) → la marca queda: ENVIO_INCIERTO, nunca transmisible")
    void registroFalla_quedaLaMarca() throws Exception {
        int idFactura = facturaConLineasAyB();
        autenticar();
        Long id = comprobanteService.generarParaEmision((long) idFactura).getId();
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(recibidoTrasDemora(0));
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("BD caída"))
                .when(comprobanteRepository).save(org.mockito.ArgumentMatchers.<ComprobanteElectronico>argThat(
                        c -> c != null && c.getId() != null && c.getId().equals(id)
                                && c.getEstado() == EstadoComprobante.ENVIADO));

        var resultado = envioService.enviar(id);

        assertThat(resultado.getEstado()).isEqualTo(EstadoComprobante.ENVIO_INCIERTO);
        assertThat(comprobanteBd(id).estado()).isEqualTo("ENVIO_INCIERTO");
        verify(haciendaApi, times(1)).enviarComprobante(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("T-037 c.10: 6 emisiones concurrentes de facturas distintas → consecutivos distintos y sin huecos")
    void consecutivo_emisionesConcurrentes_sinRepetirNiHuecos() throws Exception {
        int n = 6;
        List<Integer> facturas = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            facturas.add(facturaConLineasAyB());
        }
        long antes = jdbc.queryForObject("SELECT consecutivo_factura FROM configuracion_hacienda WHERE id = ?", Long.class,
                configHacienda.getId());
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenAnswer(inv -> recibidoTrasDemora(50));
        java.util.concurrent.CountDownLatch salida = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService hilos = java.util.concurrent.Executors.newFixedThreadPool(n);
        List<Long> ids = new java.util.ArrayList<>();
        try {
            List<java.util.concurrent.Future<Long>> futuros = new java.util.ArrayList<>();
            for (Integer idFactura : facturas) {
                futuros.add(hilos.submit(() -> {
                    autenticar();
                    salida.await();
                    return envioService.emitirFactura((long) idFactura).getId();
                }));
            }
            salida.countDown();
            for (var f : futuros) {
                ids.add(f.get(60, java.util.concurrent.TimeUnit.SECONDS));
            }
        } finally {
            hilos.shutdownNow();
        }
        List<Long> consecutivos = ids.stream().map(id -> Long.parseLong(comprobanteBd(id).consecutivo())).sorted().toList();
        assertThat(consecutivos).as("distintos y seguidos").isEqualTo(
                java.util.stream.LongStream.range(antes, antes + n).boxed().toList());
        assertThat(jdbc.queryForObject("SELECT consecutivo_factura FROM configuracion_hacienda WHERE id = ?", Long.class,
                configHacienda.getId())).isEqualTo(antes + n);
        assertThat(ids.stream().map(id -> comprobanteBd(id).estado()).distinct().toList()).containsExactly("ENVIADO");
    }

    @Test
    @DisplayName("T-037 c.12 y c.2: Hacienda tarda 3 s → POST /procesar responde en menos de 6 s con ENVIADO; el POST corre sin transacción")
    void procesar_respondeEnSegundos_postSinTransaccion() throws Exception {
        int idFactura = facturaConLineasAyB();
        java.util.concurrent.atomic.AtomicBoolean postSinTransaccion = new java.util.concurrent.atomic.AtomicBoolean();
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenAnswer(inv -> {
            postSinTransaccion.set(!org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive());
            return recibidoTrasDemora(3000);
        });

        long inicio = System.nanoTime();
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/procesar/" + idFactura)
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.comprobante.estado").value("ENVIADO"));
        long ms = Duration.ofNanos(System.nanoTime() - inicio).toMillis();

        assertThat(ms).as("responde sin esperar el sondeo").isLessThan(6000);
        assertThat(postSinTransaccion).as("token + POST sin transacción activa (MySQL real)").isTrue();
        verify(haciendaApi, never()).consultarComprobante(anyString(), anyLong());
    }

    @Test
    @DisplayName("T-037 review 1, c.25-26: doble clic, dos hilos emiten la misma factura → 1 vigente, 1 consecutivo, 1 POST y 409")
    void dobleClic_mismaFactura_unSoloComprobante() throws Exception {
        int idFactura = facturaConLineasAyB();
        long antes = jdbc.queryForObject("SELECT consecutivo_factura FROM configuracion_hacienda WHERE id = ?", Long.class,
                configHacienda.getId());
        java.util.concurrent.atomic.AtomicInteger posts = new java.util.concurrent.atomic.AtomicInteger();
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenAnswer(inv -> {
            posts.incrementAndGet();
            return recibidoTrasDemora(200);
        });
        java.util.concurrent.CountDownLatch salida = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService hilos = java.util.concurrent.Executors.newFixedThreadPool(2);
        List<String> resultados = new java.util.ArrayList<>();
        try {
            List<java.util.concurrent.Future<String>> futuros = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futuros.add(hilos.submit(() -> {
                    autenticar();
                    salida.await();
                    try {
                        return envioService.emitirFactura((long) idFactura).getEstado().name();
                    } catch (api.astro.whats_orders_manager.modules.facturacion.electronica.exception.EmisionNoPermitidaException e) {
                        return "409:" + e.getMensajeUsuario();
                    }
                }));
            }
            salida.countDown();
            for (var f : futuros) {
                resultados.add(f.get(60, java.util.concurrent.TimeUnit.SECONDS));
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(resultados).containsExactlyInAnyOrder("ENVIADO",
                "409:" + api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl.MENSAJE_COMPROBANTE_EN_CURSO);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico WHERE factura_id = ?", Integer.class,
                idFactura)).as("un solo comprobante").isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico WHERE factura_id = ? AND vigente = 1",
                Integer.class, idFactura)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT consecutivo_factura FROM configuracion_hacienda WHERE id = ?", Long.class,
                configHacienda.getId())).as("un solo consecutivo consumido").isEqualTo(antes + 1);
        assertThat(posts.get()).isEqualTo(1);

        // La segunda petición por HTTP: 409 con el mensaje fijo
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/procesar/" + idFactura)
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ComprobanteElectronicoServiceImpl.MENSAJE_COMPROBANTE_EN_CURSO));
    }

    private void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(USUARIO, null,
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    private static LineaFacturaR lineaR(int idLinea, int idFactura, Producto producto, int cantidad, String precio) {
        return new LineaFacturaR(idLinea, 1, producto.getIdProducto(), idFactura, null, cantidad,
                new BigDecimal(precio), new BigDecimal(precio), null, null, null, null);
    }

    /** "Consultar estado en Hacienda" (T-026; reemplaza a "verificar con Hacienda"). */
    private int verificar(Long idComprobante) throws Exception {
        return mockMvc.perform(post("/api/facturas/electronica/comprobantes/" + idComprobante + "/consultar-estado-hacienda")
                .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn().getResponse().getStatus();
    }

    // ── Fixture ──────────────────────────────────────────────────────────────

    /** Factura en CRC con A (gravado 13 %, 1130) y B (exento, 500) guardadas por la ruta de la UI: total 1630. */
    private int facturaConLineasAyB() throws Exception {
        int idFactura = tx.execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T025 " + UUID.randomUUID());
            cliente.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_FISICA);
            cliente.setNumeroIdentificacion("109990999");
            cliente.setTelefono("88887777");
            cliente.setEmail("cliente@t025.example");
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T025-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T025");
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
        assertThat(totales(idFactura).total()).isEqualByComparingTo("1630.00");
        return idFactura;
    }

    /**
     * Comprobante vigente de la factura en el estado indicado ("SIN_COMPROBANTE" no crea ninguno). Se inserta por
     * SQL para fijar cualquier estado sin pasar por la generación ni por Hacienda.
     */
    private Long comprobante(int idFactura, String estado) {
        return comprobante(idFactura, estado, LocalDateTime.now().minusHours(1));
    }

    private Long comprobante(int idFactura, String estado, LocalDateTime fechaEnvio) {
        if ("SIN_COMPROBANTE".equals(estado)) {
            return null;
        }
        jdbc.update("INSERT INTO comprobante_electronico (clave_numerica, consecutivo, created_at, updated_at, estado, "
                        + "fecha_emision, fecha_envio, tipo_comprobante, empresa_id, factura_id, vigente, intentos_envio, "
                        + "enviado_email, xml_comprobante, ruta_archivo_xml) "
                        + "VALUES (?, ?, NOW(6), NOW(6), ?, NOW(6), ?, 'FACTURA_ELECTRONICA', ?, ?, b'1', 0, b'0', ?, ?)",
                clave(), String.format("%020d", ThreadLocalRandom.current().nextLong(1, 1_000_000_000L)), estado,
                fechaEnvio, idEmpresa, idFactura, xmlFirmadoReal(),
                "comprobantes/t025.xml");
        return jdbc.queryForObject("SELECT MAX(id) FROM comprobante_electronico WHERE factura_id = ?", Long.class,
                idFactura);
    }

    /**
     * XML firmado de una factura de la clase, producido por el flujo real (persistida → generador → firma) y validado
     * por el validador XSD real; las filas insertadas por SQL (NO_TRANSMITIDO, etc.) lo llevan para que el reintento
     * del job transmita un XML válido, no un texto de relleno.
     */
    private String xmlFirmadoReal() {
        if (xmlFirmadoReal == null) {
            try {
                int idFactura = facturaConLineasAyB();
                autenticar();
                Long id = comprobanteService.generarDesdeFactura((long) idFactura).getId();
                comprobanteService.firmar(id);
                xmlFirmadoReal = comprobanteBd(id).xml();
                jdbc.update("DELETE FROM comprobante_electronico WHERE id = ?", id);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            assertThat(XmlValidator.erroresContraXsd(xmlFirmadoReal, TipoComprobanteElectronico.FACTURA_ELECTRONICA))
                    .as("el XML firmado del fixture valida contra el XSD").isEmpty();
        }
        return xmlFirmadoReal;
    }

    /** Factura en CRC sin líneas ni asiento (como la deja POST /facturas/guardar antes de cargar las líneas). */
    private int facturaSinLineas() {
        return tx.execute(s -> {
            Cliente cliente = new Cliente();
            cliente.setNombre("Cliente T025 " + UUID.randomUUID());
            cliente.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_FISICA);
            cliente.setNumeroIdentificacion("109990999");
            cliente.setTelefono("88887777");
            cliente.setEmail("cliente@t025.example");
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T025-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T025");
            factura.setMonedaFE(MonedaFE.CRC);
            factura.setTipoCambio(BigDecimal.ONE);
            factura.setSubtotal(BigDecimal.ZERO);
            factura.setIgv(BigDecimal.ZERO);
            factura.setTotal(BigDecimal.ZERO);
            factura.setAnulada(false);
            return facturaRepository.save(factura).getIdFactura();
        });
    }

    record ComprobanteBd(Long id, String clave, String consecutivo, String estado, boolean vigente, String xml,
                         String hashXml, String rutaXml) {
    }

    private ComprobanteBd comprobanteBd(Long id) {
        return jdbc.queryForObject("SELECT id, clave_numerica, consecutivo, estado, vigente, xml_comprobante, "
                        + "SHA2(xml_comprobante, 256), ruta_archivo_xml FROM comprobante_electronico WHERE id = ?",
                (rs, i) -> new ComprobanteBd(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getBoolean(5), rs.getString(6), rs.getString(7), rs.getString(8)), id);
    }

    private List<ComprobanteBd> comprobantesBd(int idFactura) {
        return jdbc.queryForList("SELECT id FROM comprobante_electronico WHERE factura_id = ? ORDER BY id",
                Long.class, idFactura).stream().map(this::comprobanteBd).toList();
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
        p.setCodigo("T25-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDescripcion(descripcion);
        // ADR-040: el CAByS vive solo en el artículo maestro
        // (la BD de la clase no se limpia entre pruebas y la descripción del maestro es única: se reutiliza)
        ArticuloMaestro maestro = articuloMaestroRepository.findByDescripcionContainingIgnoreCase(descripcion).stream()
                .filter(m -> descripcion.equals(m.getDescripcion())).findFirst().orElseGet(ArticuloMaestro::new);
        maestro.setDescripcion(descripcion);
        maestro.setCodigoCabys("1234567890123");
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

    /** Línea tal como la arma guardarLineas() en editar-factura.js (ids negativos = filas nuevas). */
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

    /** Cuerpo de POST /facturas/guardar para editar la factura (entidad completa) cambiando la condición de venta. */
    private Map<String, Object> facturaEditada(int idFactura, String condicionVenta) {
        return jdbc.queryForObject("SELECT numero_factura, serie, id_cliente, total, subtotal, igv, create_date FROM factura "
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
            // La edición por esta ruta reemplaza la entidad completa: sin createDate la fecha de emisión queda nula
            f.put("createDate", rs.getTimestamp(7).toLocalDateTime().toString());
            return f;
        }, idFactura);
    }

    // ── Lecturas directas de la BD ───────────────────────────────────────────

    record LineaBd(int id, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
    }

    record TotalesBd(BigDecimal subtotal, BigDecimal igv, BigDecimal total) {
    }

    record AsientoBd(Long id, BigDecimal total) {
    }

    record Estado(List<LineaBd> lineas, TotalesBd totales, BigDecimal sumaLineas, AsientoBd asientoVigente) {
    }

    private List<LineaBd> lineas(int idFactura) {
        return jdbc.query("SELECT id_linea_factura, cantidad, precio_unitario, subtotal FROM linea_factura "
                        + "WHERE id_factura = ? ORDER BY id_linea_factura",
                (rs, i) -> new LineaBd(rs.getInt(1), rs.getInt(2), rs.getBigDecimal(3), rs.getBigDecimal(4)),
                idFactura);
    }

    private TotalesBd totales(int idFactura) {
        return jdbc.queryForObject("SELECT subtotal, igv, total FROM factura WHERE id_factura = ?",
                (rs, i) -> new TotalesBd(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3)),
                idFactura);
    }

    private String condicionVenta(int idFactura) {
        return jdbc.queryForObject("SELECT condicion_venta_fe FROM factura WHERE id_factura = ?", String.class,
                idFactura);
    }

    private int comprobantes(int idFactura) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico WHERE factura_id = ?",
                Integer.class, idFactura);
    }

    private AsientoBd asientoVigente(int idFactura) {
        return tx.execute(s -> {
            List<AsientoContable> vigentes = asientoRepository.findAsientosVentaVigentes((long) idFactura);
            assertThat(vigentes).hasSizeLessThanOrEqualTo(1);
            if (vigentes.isEmpty()) {
                return null;
            }
            AsientoContable asiento = vigentes.get(0);
            return new AsientoBd(asiento.getIdAsiento(), asiento.getTotalDebe().stripTrailingZeros());
        });
    }

    private Estado estado(int idFactura) {
        BigDecimal suma = jdbc.queryForObject(
                "SELECT COALESCE(SUM(subtotal), 0) FROM linea_factura WHERE id_factura = ?", BigDecimal.class,
                idFactura);
        return new Estado(lineas(idFactura), totales(idFactura), suma, asientoVigente(idFactura));
    }
}
