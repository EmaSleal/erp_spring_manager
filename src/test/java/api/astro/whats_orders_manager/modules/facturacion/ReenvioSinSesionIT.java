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

/**
 * T-040 (P-096, ADR-043) — el envío y el sondeo a Hacienda no dependen de una sesión de Hibernate abierta.
 *
 * <p>Reproduce lo que hace ComprobanteReintentosJob: se invoca el servicio desde un hilo sin petición web (sin
 * open-in-view) y sin transacción. La configuración de Hacienda y la empresa son entidades JPA reales y perezosas;
 * generador, firma y XSD son los reales. Solo el transporte HTTP está simulado (MockRestServiceServer sobre el
 * {@code haciendaRestTemplate} real): ninguna prueba llama a un host externo.
 *
 * <p>Base: MySQL 8 en Testcontainers (t040-it-mysql, tmpfs, sin volúmenes).
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
        "facturacion.hacienda.sondeo.espera=1h"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-040 — reenvío y sondeo a Hacienda fuera de una petición web (MySQL)")
class ReenvioSinSesionIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t040")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t040-it-mysql"))
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
    private static final String RUC = "3101234567";

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
    @Autowired @Qualifier("haciendaRestTemplate") private RestTemplate haciendaRestTemplate;
    @Autowired private org.springframework.cache.CacheManager cacheManager;

    private TransactionTemplate tx;
    private MockRestServiceServer hacienda;
    private Producto gravado13;
    private String xmlFirmadoReal;

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
                    .orElseGet(() -> ConfiguracionEmpresa.builder().razonSocial("Empresa T040 S.A.").build());
            empresa.setNumeroIdentificacion(RUC);
            empresa.setTipoIdentificacion(TipoIdentificacion.CEDULA_JURIDICA);
            empresa.setCodigoActividad("4773.0");
            empresa.setCodigoProvincia("1");
            empresa.setCanton("01");
            empresa.setDistrito("01");
            empresa.setTelefono("22223333");
            empresa.setEmail("facturacion@t040.example");
            empresa.setOtrasSenas("Avenida Central, San José");
            CatalogoUbicacionSembrado.sembrar(jdbc);
            final ConfiguracionEmpresa guardada = empresaRepository.save(empresa);
            if (configuracionHaciendaRepository.findByActivaTrueOrderByIdAsc().isEmpty()) {
                configuracionHaciendaRepository.save(ConfiguracionHacienda.builder().empresa(guardada)
                        .ambiente(AmbienteHacienda.SANDBOX)
                        .usuarioHacienda("cpj-3-101-234567@stag.comprobanteselectronicos.go.cr")
                        .passwordHacienda("clave-de-prueba").activa(true)
                        .rutaCertificado(CertificadoDePrueba.ruta()).pinCertificado(CertificadoDePrueba.PIN).build());
            }
            gravado13 = producto("Producto A gravado 13 % T040", "1130.00");
        });
        var cache = cacheManager.getCache("haciendaToken");
        if (cache != null) {
            cache.clear();
        }
        hacienda = MockRestServiceServer.bindTo(haciendaRestTemplate).ignoreExpectOrder(true).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Envío: criterio 1 y 2 ────────────────────────────────────────────────

    @Test
    @DisplayName("reenviar un NO_TRANSMITIDO desde un hilo sin sesión: el POST simulado se ejecuta con el emisor real y queda ENVIADO")
    void reenviar_sinSesion_enviaConElEmisorDeLaEmpresa() throws Exception {
        Long id = comprobanteEn("NO_TRANSMITIDO");
        esperarToken();
        hacienda.expect(ExpectedCount.once(), requestTo(Matchers.containsString("/recepcion/v1/recepcion")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.emisor.numeroIdentificacion").value(RUC))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.emisor.tipoIdentificacion").value("02"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.clave").value(clave(id)))
                .andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED));

        ComprobanteElectronicoDTO resultado = enHiloSinSesion(() -> envioService.reenviar(id));

        assertThat(estado(id)).as("ultimoError: %s", ultimoError(id)).isEqualTo("ENVIADO");
        assertThat(resultado.getEstado().name()).isEqualTo("ENVIADO");
        hacienda.verify();
    }

    @Test
    @DisplayName("ComprobanteReintentosJob (hilo del programador, sin petición web) reintenta un NO_TRANSMITIDO y queda ENVIADO")
    void job_reintentos_sinSesion_enviaYQuedaEnviado() throws Exception {
        Long id = comprobanteEn("NO_TRANSMITIDO");
        esperarToken();
        hacienda.expect(ExpectedCount.once(), requestTo(Matchers.containsString("/recepcion/v1/recepcion")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED));

        enHiloSinSesion(() -> {
            reintentosJob.reintentarEnviosPendientes();
            return null;
        });

        assertThat(estado(id)).as("ultimoError: %s", ultimoError(id)).isEqualTo("ENVIADO");
        hacienda.verify();
    }

    // ── Sondeo en segundo plano: criterio 3 ──────────────────────────────────

    @Test
    @DisplayName("sondeo (consultarYActualizarEstado y job horario) desde un hilo sin petición web: lee la empresa y adopta el estado de Hacienda")
    void sondeo_sinSesion_consultaYActualiza() throws Exception {
        Long id = comprobanteEn("ENVIADO");
        jdbc.update("UPDATE comprobante_electronico SET fecha_envio = NOW(6) - INTERVAL 2 DAY WHERE id = ?", id);
        esperarToken();
        hacienda.expect(ExpectedCount.manyTimes(),
                        requestTo(Matchers.containsString("/recepcion/v1/recepcion/" + clave(id))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"ind-estado\":\"recibido\"}", MediaType.APPLICATION_JSON));

        ComprobanteElectronicoDTO consultado = enHiloSinSesion(() -> comprobanteService.consultarYActualizarEstado(id));
        assertThat(consultado.getEstado().name()).isEqualTo("PROCESANDO");

        jdbc.update("UPDATE comprobante_electronico SET estado = 'ENVIADO' WHERE id = ?", id);
        enHiloSinSesion(() -> {
            reintentosJob.consultarEstadosPendientes();
            return null;
        });
        assertThat(estado(id)).as("ultimoError: %s", ultimoError(id)).isEqualTo("PROCESANDO");
    }

    // ── API: criterio 7 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /{id}/reenviar: ERROR → 200 y se transmite; ENVIO_INCIERTO → 409 con el mensaje fijo y sin POST")
    void api_reenviar_errorTransmite_envioIncierto409() throws Exception {
        Long error = comprobanteEn("ERROR");
        esperarToken();
        hacienda.expect(ExpectedCount.once(), requestTo(Matchers.containsString("/recepcion/v1/recepcion")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.ACCEPTED));

        mockMvc.perform(post("/api/facturas/electronica/comprobantes/" + error + "/reenviar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprobante.estado").value("ENVIADO"));
        assertThat(estado(error)).isEqualTo("ENVIADO");
        hacienda.verify();

        Long incierto = comprobanteEn("ENVIO_INCIERTO");
        hacienda.reset();
        mockMvc.perform(post("/api/facturas/electronica/comprobantes/" + incierto + "/reenviar")
                        .with(user(USUARIO).roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Comprobante no puede ser reenviado en estado ENVIO_INCIERTO"));
        assertThat(estado(incierto)).isEqualTo("ENVIO_INCIERTO");
        hacienda.verify();
    }

    // ── Listado: criterio 5 ──────────────────────────────────────────────────

    @Test
    @DisplayName("listado de comprobantes (sp_listar_comprobantes_por_empresa): puedeReenviar solo en GENERADO, FIRMADO, ERROR y NO_TRANSMITIDO")
    void listado_puedeReenviarSegunEstado() throws Exception {
        List<String> estados = List.of("GENERADO", "FIRMADO", "ERROR", "NO_TRANSMITIDO", "ENVIO_INCIERTO", "ENVIADO",
                "PROCESANDO", "ACEPTADO", "RECHAZADO", "ERROR_HACIENDA");
        Map<Long, String> estadoPorId = new LinkedHashMap<>();
        for (String estado : estados) {
            estadoPorId.put(comprobanteEn(estado), estado);
        }
        Integer idEmpresa = jdbc.queryForObject(
                "SELECT id_configuracion FROM configuracion_empresa ORDER BY id_configuracion LIMIT 1", Integer.class);

        String json = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/facturas/electronica/comprobantes/empresa/" + idEmpresa)
                        .param("page", "0").param("size", "1000")
                        .with(user(USUARIO).roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Map<Long, Boolean> puedeReenviar = new LinkedHashMap<>();
        for (var fila : objectMapper.readTree(json)) {
            puedeReenviar.put(fila.get("id").asLong(), fila.get("puedeReenviar").asBoolean());
        }
        estadoPorId.forEach((id, estado) -> assertThat(puedeReenviar.get(id))
                .as("puedeReenviar de un comprobante %s", estado)
                .isEqualTo(List.of("GENERADO", "FIRMADO", "ERROR", "NO_TRANSMITIDO").contains(estado)));
    }

    // ── Fixture ──────────────────────────────────────────────────────────────

    private void esperarToken() {
        hacienda.expect(ExpectedCount.manyTimes(), requestTo(Matchers.containsString("/openid-connect/token")))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok-de-prueba\",\"refresh_token\":\"r\","
                        + "\"expires_in\":300,\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));
    }

    /** Ejecuta en un hilo nuevo: sin SecurityContext, sin RequestContext y sin transacción, como el programador. */
    private <T> T enHiloSinSesion(Callable<T> tarea) throws Exception {
        ExecutorService hilo = Executors.newSingleThreadExecutor();
        try {
            return hilo.submit(tarea).get(60, TimeUnit.SECONDS);
        } finally {
            hilo.shutdownNow();
        }
    }

    /** Comprobante vigente de una factura nueva, con el XML firmado real, en el estado indicado. */
    private Long comprobanteEn(String estado) throws Exception {
        int idFactura = facturaConLinea();
        jdbc.update("INSERT INTO comprobante_electronico (clave_numerica, consecutivo, created_at, updated_at, estado, "
                        + "fecha_emision, fecha_envio, tipo_comprobante, empresa_id, factura_id, vigente, intentos_envio, "
                        + "enviado_email, xml_comprobante, ruta_archivo_xml) "
                        + "VALUES (?, ?, NOW(6), NOW(6), ?, NOW(6), NOW(6), 'FACTURA_ELECTRONICA', "
                        + "(SELECT id_configuracion FROM configuracion_empresa ORDER BY id_configuracion LIMIT 1), "
                        + "?, b'1', 0, b'0', ?, ?)",
                claveNueva(), consecutivoNuevo(), estado, idFactura, xmlFirmadoReal(), "comprobantes/t040.xml");
        return jdbc.queryForObject("SELECT MAX(id) FROM comprobante_electronico WHERE factura_id = ?", Long.class,
                idFactura);
    }

    private static String claveNueva() {
        StringBuilder clave = new StringBuilder("506");
        while (clave.length() < 50) {
            clave.append(java.util.concurrent.ThreadLocalRandom.current().nextInt(10));
        }
        return clave.toString();
    }

    private static String consecutivoNuevo() {
        return String.format("%020d", java.util.concurrent.ThreadLocalRandom.current().nextLong(1, 1_000_000_000L));
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
            int idFactura = facturaConLinea();
            autenticar();
            Long id = comprobanteService.generarDesdeFactura((long) idFactura).getId();
            comprobanteService.firmar(id);
            xmlFirmadoReal = jdbc.queryForObject("SELECT xml_comprobante FROM comprobante_electronico WHERE id = ?",
                    String.class, id);
            jdbc.update("DELETE FROM comprobante_electronico WHERE id = ?", id);
            SecurityContextHolder.clearContext();
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
            cliente.setNombre("Cliente T040 " + UUID.randomUUID());
            cliente.setTipoIdentificacion(TipoIdentificacion.CEDULA_FISICA);
            cliente.setNumeroIdentificacion("109990999");
            cliente.setTelefono("88887777");
            cliente.setEmail("cliente@t040.example");
            cliente = clienteRepository.save(cliente);
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T040-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T040");
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
        p.setCodigo("T40-" + UUID.randomUUID().toString().substring(0, 8));
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
