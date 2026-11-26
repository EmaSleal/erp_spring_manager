package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.EmisionNoPermitidaException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.politicas.PoliticaEmision;
import api.astro.whats_orders_manager.modules.politicas.PoliticaRegimenDocumento;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ArticuloMaestroRepository;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.repository.RegimenTributarioRepository;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * T-057 (ADR-073, ADR-076; criterios 2 a 5, 9, 14 y 17; P-132 casos 1, 2, 3, 5 de venta, 11, 12 y 13): frontera de
 * régimen con tramos GENERAL hasta 2026-12-31 y SIMPLIFICADO desde 2027-01-01 y un Clock que se mueve.
 *
 * Cada factura se crea por la ruta de la UI (POST /facturas/guardar y PUT /lineas-factura/actualizar), con el servicio, el
 * asiento de venta y el cálculo de IVA reales. La fecha de la factura (createDate) la fija la auditoría JPA con la hora
 * del sistema, no con el Clock; por eso, tras crear, la prueba la pone a la hora de Costa Rica del Clock (en producción
 * son la misma). Generador, firma y validador XSD son los reales; el cliente de Hacienda está simulado.
 *
 * Base: MySQL 8 en Testcontainers (t057-it-mysql, tmpfs); esquema de Hibernate y rutinas de database/routines.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-057 — venta según el régimen sellado: frontera 2026-12-31 / 2027-01-01 (MySQL)")
class VentaRegimenFronteraIT {

    static final ZoneId CR = ZoneId.of("America/Costa_Rica");
    static final String USUARIO = "admin.bootstrap@example.invalid";
    static final String MSG_GENERAL_FUERA_DEL_ERP = "Esta venta es del régimen general; su comprobante se emite en la "
            + "herramienta de Hacienda y se registra aquí su clave";
    static final String MSG_SIMPLIFICADO = "Las ventas del régimen simplificado no se emiten a Hacienda";

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t057", "t057-it-mysql");

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
        RelojMovil relojMovil() {
            return new RelojMovil();
        }
    }

    /** Reloj de la aplicación en estas pruebas: una hora de Costa Rica que cada prueba fija. */
    static class RelojMovil extends Clock {
        private volatile Instant instante = Instant.now();

        void fijar(LocalDateTime horaCostaRica) {
            instante = horaCostaRica.atZone(CR).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return CR;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instante, zone);
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    @MockitoBean private HaciendaApiService haciendaApi;
    @MockitoBean private ConfiguracionHaciendaService configuracionHaciendaService;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private RelojMovil reloj;
    @Autowired private FacturaService facturaService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ArticuloMaestroRepository articuloMaestroRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ConfiguracionHaciendaRepository configuracionHaciendaRepository;
    @Autowired private RegimenTributarioRepository regimenRepository;
    @Autowired private RegimenTributarioService regimenService;
    @Autowired private ComprobanteElectronicoService comprobanteService;
    @Autowired private EnvioHaciendaService envioService;
    @Autowired private PoliticaEmision politicaEmision;
    @Autowired private PoliticaRegimenDocumento politicaRegimen;

    private TransactionTemplate tx;
    private Producto gravado13;
    private Producto exento;
    private Long idConfigHacienda;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            if (parametroRepository.findByClave(AsientoContableService.CLAVE_VENTA_CXC_CLIENTES).isEmpty()) {
                param(AsientoContableService.CLAVE_VENTA_CXC_CLIENTES, cuenta("9.1.02.001", "Clientes", TipoCuenta.ACTIVO));
                param(AsientoContableService.CLAVE_VENTA_INGRESOS, cuenta("9.4.01.001", "Ventas", TipoCuenta.INGRESO));
                param(AsientoContableService.CLAVE_VENTA_IVA_POR_PAGAR,
                        cuenta("9.2.01.002", "IVA por pagar", TipoCuenta.PASIVO));
            }
            ConfiguracionEmpresa empresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc()
                    .orElseGet(() -> ConfiguracionEmpresa.builder().razonSocial("Empresa T057 S.A.").build());
            empresa.setNumeroIdentificacion("3101234567");
            empresa.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_JURIDICA);
            empresa.setCodigoActividad("4773.0");
            empresa.setCodigoProvincia("1");
            empresa.setCanton("01");
            empresa.setDistrito("01");
            empresa.setTelefono("22223333");
            empresa.setEmail("facturacion@t057.example");
            empresa.setOtrasSenas("Avenida Central, San José");
            api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionSembrado.sembrar(jdbc);
            empresa = empresaRepository.save(empresa);
            if (idConfigHacienda == null) {
                final ConfiguracionEmpresa empresaDeLaConfig = empresa;
                idConfigHacienda = configuracionHaciendaRepository.findByActivaTrueOrderByIdAsc().stream().findFirst()
                        .orElseGet(() -> configuracionHaciendaRepository.save(ConfiguracionHacienda.builder()
                                .empresa(empresaDeLaConfig).ambiente(AmbienteHacienda.SANDBOX)
                                .usuarioHacienda("cpj-3-101-234567@stag.comprobanteselectronicos.go.cr")
                                .passwordHacienda("clave-de-prueba").activa(true)
                                .rutaCertificado(CertificadoDePrueba.ruta()).pinCertificado(CertificadoDePrueba.PIN)
                                .build())).getId();
            }
            final Long idConfig = idConfigHacienda;
            when(configuracionHaciendaService.obtenerConfiguracionActiva())
                    .thenAnswer(inv -> configuracionHaciendaRepository.findById(idConfig).orElseThrow());
            gravado13 = producto("Producto A gravado 13 % T057", "1130.00", true);
            exento = producto("Producto B exento T057", "500.00", false);
        });
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(RespuestaHaciendaDTO.builder()
                .resultadoEnvio(ResultadoEnvioHacienda.RECIBIDO).codigoHttp(202).exitoso(false)
                .mensaje("Comprobante recibido por Hacienda, en procesamiento").build());
        tramos(Regimen.GENERAL, "2000-01-01", Regimen.SIMPLIFICADO, "2027-01-01");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Sello al crear (casos 1 y 2, criterios 3 y 9) ────────────────────────

    @Test
    @DisplayName("caso 1: creada el 31-12-2026 23:59 → sellada GENERAL, IVA explícito y asiento con VENTA_IVA_POR_PAGAR")
    void creada_31_12_general() throws Exception {
        int id = crearFactura(LocalDateTime.of(2026, 12, 31, 23, 59));

        assertThat(regimen(id)).isEqualTo("GENERAL");
        assertThat(totales(id).igv()).isEqualByComparingTo("130.00");
        assertThat(totales(id).total()).isEqualByComparingTo("1630.00");
        Map<String, BigDecimal[]> asiento = asientoVigente(id);
        assertThat(asiento.get("9.2.01.002")[1]).isEqualByComparingTo("130.00");
        assertThat(asiento.get("9.1.02.001")[0]).isEqualByComparingTo("1630.00");
    }

    @Test
    @DisplayName("caso 2: creada el 01-01-2027 00:00 → SIMPLIFICADO, IVA 0, total = suma de líneas y asiento solo con CxC e ingreso")
    void creada_01_01_simplificado() throws Exception {
        int id = crearFactura(LocalDateTime.of(2027, 1, 1, 0, 0));

        assertThat(regimen(id)).isEqualTo("SIMPLIFICADO");
        assertThat(totales(id).igv()).isEqualByComparingTo("0.00");
        assertThat(totales(id).total()).isEqualByComparingTo("1630.00");
        Map<String, BigDecimal[]> asiento = asientoVigente(id);
        assertThat(asiento).containsOnlyKeys("9.1.02.001", "9.4.01.001");
        assertThat(asiento.get("9.1.02.001")[0]).isEqualByComparingTo("1630.00");
        assertThat(asiento.get("9.4.01.001")[1]).isEqualByComparingTo("1630.00");
    }

    @Test
    @DisplayName("caso 11: una venta del RTS no deja ninguna línea en la cuenta de IVA por pagar")
    void ventaRts_sinIvaPorPagar() throws Exception {
        int id = crearFactura(LocalDateTime.of(2027, 1, 20, 9, 0));

        assertThat(asientoVigente(id)).doesNotContainKey("9.2.01.002");
    }

    @Test
    @DisplayName("caso 13: tramo GENERAL desde 2027-07-01 → la del 30-06 SIMPLIFICADO, la del 01-07 GENERAL")
    void salidaDelRts() throws Exception {
        tramos(Regimen.GENERAL, "2000-01-01", Regimen.SIMPLIFICADO, "2027-01-01", Regimen.GENERAL, "2027-07-01");

        int del30 = crearFactura(LocalDateTime.of(2027, 6, 30, 12, 0));
        int del01 = crearFactura(LocalDateTime.of(2027, 7, 1, 12, 0));

        assertThat(regimen(del30)).isEqualTo("SIMPLIFICADO");
        assertThat(regimen(del01)).isEqualTo("GENERAL");
        assertThat(totales(del01).igv()).isEqualByComparingTo("130.00");
    }

    @Test
    @DisplayName("sin ningún tramo, o con fecha anterior al primero (caso 12) → RegimenNoDefinidoException al crear; nada se guarda")
    void sinTramo_error() {
        tramos();
        reloj.fijar(LocalDateTime.of(2026, 12, 31, 10, 0));
        assertThatThrownBy(() -> facturaService.save(facturaNueva())).isInstanceOf(RegimenNoDefinidoException.class);

        tramos(Regimen.GENERAL, "2026-09-01");
        reloj.fijar(LocalDateTime.of(2026, 8, 31, 10, 0));
        assertThatThrownBy(() -> facturaService.save(facturaNueva())).isInstanceOf(RegimenNoDefinidoException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM factura WHERE serie = 'T057SOLO'", Integer.class)).isZero();
    }

    // ── Edición (caso 3, criterios 3 y 9) ────────────────────────────────────

    @Test
    @DisplayName("caso 3: editar el 05-01-2027 una factura GENERAL de diciembre → conserva GENERAL e IVA; el asiento se regenera con su fecha")
    void editarEnEnero_conservaGeneral() throws Exception {
        int id = crearFactura(LocalDateTime.of(2026, 12, 31, 23, 59));
        reloj.fijar(LocalDateTime.of(2027, 1, 5, 10, 0));

        MvcResult r = guardarEdicion(id, "CREDITO", null);

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(regimen(id)).isEqualTo("GENERAL");
        assertThat(totales(id).igv()).isEqualByComparingTo("130.00");
        assertThat(jdbc.queryForObject("SELECT condicion_venta_fe FROM factura WHERE id_factura = ?", String.class, id))
                .isEqualTo("CREDITO");
        assertThat(asientoVigente(id).get("9.2.01.002")[1]).isEqualByComparingTo("130.00");
        assertThat(fechaAsientoVigente(id)).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("el régimen no se recibe del cliente: una edición con regimenEmisor GENERAL no cambia una SIMPLIFICADO")
    void edicion_ignoraElRegimenDelCuerpo() throws Exception {
        int id = crearFactura(LocalDateTime.of(2027, 1, 3, 10, 0));

        MvcResult r = guardarEdicion(id, "CREDITO", Map.of("regimenEmisor", "GENERAL"));

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(regimen(id)).isEqualTo("SIMPLIFICADO");
        assertThat(totales(id).igv()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("mover la fecha de una GENERAL de diciembre al 2027-01-05 → 409 con mensaje fijo y la factura no cambia; dentro del mismo tramo no hay 409")
    void moverFechaEntreTramos_409() throws Exception {
        int id = crearFactura(LocalDateTime.of(2026, 12, 31, 23, 59));
        String fechaAntes = jdbc.queryForObject("SELECT create_date FROM factura WHERE id_factura = ?", String.class, id);

        MvcResult r = guardarEdicion(id, "CREDITO", Map.of("createDate", "2027-01-05T10:00:00"));

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        JsonNode cuerpo = objectMapper.readTree(r.getResponse().getContentAsString());
        assertThat(cuerpo.get("success").asBoolean()).isFalse();
        assertThat(cuerpo.get("message").asText()).isEqualTo(PoliticaRegimenDocumento.MENSAJE_CAMBIO_DE_REGIMEN);
        assertThat(regimen(id)).isEqualTo("GENERAL");
        assertThat(jdbc.queryForObject("SELECT create_date FROM factura WHERE id_factura = ?", String.class, id))
                .isEqualTo(fechaAntes);
        assertThat(jdbc.queryForObject("SELECT condicion_venta_fe FROM factura WHERE id_factura = ?", String.class, id))
                .isNotEqualTo("CREDITO");

        MvcResult mismoTramo = guardarEdicion(id, "CREDITO", Map.of("createDate", "2026-12-15T10:00:00"));
        assertThat(mismoTramo.getResponse().getStatus()).isEqualTo(200);
        assertThat(regimen(id)).isEqualTo("GENERAL");
    }

    // ── Emisión (criterios 5 y 14 y 17) ─────────────────────────────────────

    @Test
    @DisplayName("GENERAL del 31-12 con el Clock el 31-12: se emite (comprobante creado, consecutivo consumido, enviado)")
    void generalDiciembre_clockEnDiciembre_seEmite() throws Exception {
        int id = crearFactura(LocalDateTime.of(2026, 12, 31, 23, 0));
        autenticar();
        long consecutivoAntes = consecutivoFactura();
        clearInvocations(haciendaApi);

        ComprobanteElectronicoDTO dto = envioService.emitirFactura((long) id);

        assertThat(dto.getEstado()).isEqualTo(EstadoComprobante.ENVIADO);
        assertThat(comprobantes(id)).isEqualTo(1);
        assertThat(consecutivoFactura()).isEqualTo(consecutivoAntes + 1);
        verify(haciendaApi).enviarComprobante(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("GENERAL del 31-12 con el Clock el 02-01: emitir (servicio, /procesar, /generar) → 409 con el mensaje fijo, sin comprobante ni consecutivo")
    void generalDiciembre_clockEnEnero_emitir409() throws Exception {
        int id = crearFactura(LocalDateTime.of(2026, 12, 31, 23, 0));
        reloj.fijar(LocalDateTime.of(2027, 1, 2, 9, 0));
        autenticar();
        long consecutivoAntes = consecutivoFactura();
        clearInvocations(haciendaApi);

        assertThatThrownBy(() -> envioService.emitirFactura((long) id)).isInstanceOf(EmisionNoPermitidaException.class)
                .hasMessage(MSG_GENERAL_FUERA_DEL_ERP);
        assertThatThrownBy(() -> comprobanteService.generarDesdeFactura((long) id))
                .isInstanceOf(EmisionNoPermitidaException.class).hasMessage(MSG_GENERAL_FUERA_DEL_ERP);
        assertThatThrownBy(() -> comprobanteService.generarParaEmision((long) id))
                .isInstanceOf(EmisionNoPermitidaException.class).hasMessage(MSG_GENERAL_FUERA_DEL_ERP);
        MvcResult procesar = mockMvc.perform(post("/api/facturas/electronica/comprobantes/procesar/" + id)
                .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn();
        MvcResult generar = mockMvc.perform(post("/api/facturas/electronica/comprobantes/generar")
                .param("facturaId", String.valueOf(id)).with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn();

        for (MvcResult r : List.of(procesar, generar)) {
            assertThat(r.getResponse().getStatus()).isEqualTo(409);
            assertThat(objectMapper.readTree(r.getResponse().getContentAsString()).get("message").asText())
                    .isEqualTo(MSG_GENERAL_FUERA_DEL_ERP);
        }
        assertThat(comprobantes(id)).isZero();
        assertThat(consecutivoFactura()).isEqualTo(consecutivoAntes);
        verify(haciendaApi, never()).enviarComprobante(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("reemitir en enero el comprobante RECHAZADO de una factura de diciembre → 409 (caso 6 sustituido por ADR-076 §2): el rechazado no cambia y no hay consecutivo nuevo")
    void generalDiciembre_clockEnEnero_reemitir409() throws Exception {
        int id = crearFactura(LocalDateTime.of(2026, 12, 20, 10, 0));
        Long rechazado = comprobanteInsertado(id, "RECHAZADO");
        reloj.fijar(LocalDateTime.of(2027, 1, 3, 9, 0));
        long consecutivoAntes = consecutivoFactura();

        MvcResult r = mockMvc.perform(post("/api/facturas/electronica/comprobantes/" + rechazado + "/reemitir")
                .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn();

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(objectMapper.readTree(r.getResponse().getContentAsString()).get("message").asText())
                .isEqualTo(MSG_GENERAL_FUERA_DEL_ERP);
        assertThat(comprobantes(id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT vigente FROM comprobante_electronico WHERE id = ?", Boolean.class, rechazado))
                .isTrue();
        assertThat(consecutivoFactura()).isEqualTo(consecutivoAntes);
    }

    @Test
    @DisplayName("decisión del usuario: enviar un comprobante ya generado en diciembre sigue permitido en enero (misma clave y fecha; no es una emisión nueva)")
    void enviarComprobanteYaGenerado_enEnero_sigueSiendoPosible() throws Exception {
        int id = crearFactura(LocalDateTime.of(2026, 12, 31, 23, 0));
        autenticar();
        Long idComprobante = comprobanteService.generarDesdeFactura((long) id).getId();
        comprobanteService.firmar(idComprobante);
        reloj.fijar(LocalDateTime.of(2027, 1, 2, 9, 0));
        clearInvocations(haciendaApi);

        ComprobanteElectronicoDTO enviado = envioService.enviar(idComprobante);

        assertThat(enviado.getEstado()).isEqualTo(EstadoComprobante.ENVIADO);
        verify(haciendaApi).enviarComprobante(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("SIMPLIFICADO: emitir (servicio, /procesar, /generar) → 409 con el mensaje fijo, sin comprobante ni consecutivo; también con la política directa")
    void simplificado_emitir409() throws Exception {
        int id = crearFactura(LocalDateTime.of(2027, 1, 1, 0, 0));
        autenticar();
        long consecutivoAntes = consecutivoFactura();
        clearInvocations(haciendaApi);

        assertThatThrownBy(() -> envioService.emitirFactura((long) id)).isInstanceOf(EmisionNoPermitidaException.class)
                .hasMessage(MSG_SIMPLIFICADO);
        assertThatThrownBy(() -> comprobanteService.generarDesdeFactura((long) id))
                .isInstanceOf(EmisionNoPermitidaException.class).hasMessage(MSG_SIMPLIFICADO);
        MvcResult procesar = mockMvc.perform(post("/api/facturas/electronica/comprobantes/procesar/" + id)
                .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn();
        MvcResult generar = mockMvc.perform(post("/api/facturas/electronica/comprobantes/generar")
                .param("facturaId", String.valueOf(id)).with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn();

        for (MvcResult r : List.of(procesar, generar)) {
            assertThat(r.getResponse().getStatus()).isEqualTo(409);
            assertThat(objectMapper.readTree(r.getResponse().getContentAsString()).get("message").asText())
                    .isEqualTo(MSG_SIMPLIFICADO);
        }
        Factura factura = tx.execute(s -> facturaService.findById(id).orElseThrow());
        assertThatThrownBy(() -> politicaEmision.exigirEmisible(factura)).isInstanceOf(EmisionNoPermitidaException.class);
        assertThat(comprobantes(id)).isZero();
        assertThat(consecutivoFactura()).isEqualTo(consecutivoAntes);
        verify(haciendaApi, never()).enviarComprobante(anyString(), anyString(), anyLong());
    }

    // ── Detalle (criterios 5 y 11) ───────────────────────────────────────────

    @Test
    @DisplayName("GET /facturas/detalle/{id} informa regimenEmisor para que la UI oculte el botón de Hacienda")
    void detalle_incluyeRegimenEmisor() throws Exception {
        int general = crearFactura(LocalDateTime.of(2026, 12, 31, 12, 0));
        int rts = crearFactura(LocalDateTime.of(2027, 1, 4, 12, 0));

        for (Object[] caso : new Object[][] {{general, "GENERAL"}, {rts, "SIMPLIFICADO"}}) {
            MvcResult r = mockMvc.perform(get("/facturas/detalle/" + caso[0]).with(user(USUARIO).roles("ADMIN"))
                    .accept(MediaType.APPLICATION_JSON)).andReturn();
            assertThat(r.getResponse().getStatus()).isEqualTo(200);
            assertThat(objectMapper.readTree(r.getResponse().getContentAsString()).get("regimenEmisor").asText())
                    .isEqualTo(caso[1]);
        }
    }

    // ── Política de sello directa ────────────────────────────────────────────

    @Test
    @DisplayName("PoliticaRegimenDocumento sobre la tabla real: 2026-12-31 GENERAL, 2027-01-01 SIMPLIFICADO")
    void politicaSello_sobreLaTabla() {
        assertThat(politicaRegimen.sellar(LocalDate.of(2026, 12, 31))).isEqualTo(Regimen.GENERAL);
        assertThat(politicaRegimen.sellar(LocalDate.of(2027, 1, 1))).isEqualTo(Regimen.SIMPLIFICADO);
    }

    // ── Apoyo ────────────────────────────────────────────────────────────────

    /** Reemplaza los tramos: pares (régimen, inicio). Vacía la caché del servicio, que escribe la prueba por el repositorio. */
    private void tramos(Object... paresRegimenInicio) {
        regimenRepository.deleteAllInBatch();
        for (int i = 0; i < paresRegimenInicio.length; i += 2) {
            regimenRepository.save(RegimenTributario.builder().regimen((Regimen) paresRegimenInicio[i])
                    .vigenteDesde(LocalDate.parse((String) paresRegimenInicio[i + 1]))
                    .fechaRegistro(LocalDateTime.of(2026, 1, 1, 0, 0)).build());
        }
        regimenService.invalidarCache();
    }

    private Factura facturaNueva() {
        Cliente cliente = clienteConFe();
        Factura f = new Factura();
        f.setCliente(cliente);
        f.setSerie("T057SOLO");
        f.setMonedaFE(MonedaFE.CRC);
        f.setTipoCambio(BigDecimal.ONE);
        f.setSubtotal(BigDecimal.ZERO);
        f.setIgv(BigDecimal.ZERO);
        f.setTotal(BigDecimal.ZERO);
        return f;
    }

    private Cliente clienteConFe() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente T057 " + UUID.randomUUID());
        cliente.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_FISICA);
        cliente.setNumeroIdentificacion("109990999");
        cliente.setTelefono("88887777");
        cliente.setEmail("cliente@t057.example");
        return clienteRepository.save(cliente);
    }

    /**
     * Crea una factura como la UI: POST /facturas/guardar (cabecera en cero) y PUT /lineas-factura/actualizar con A (gravado
     * 13 %, 1130 con IVA incluido) y B (exento, 500). Entre los dos pasos pone createDate a la hora indicada, para que la
     * fecha de emisión y el asiento coincidan con el Clock.
     */
    private int crearFactura(LocalDateTime horaCostaRica) throws Exception {
        reloj.fijar(horaCostaRica);
        Cliente cliente = clienteConFe();
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("cliente", Map.of("idCliente", cliente.getIdCliente()));
        cuerpo.put("numeroFactura", "T057-" + UUID.randomUUID().toString().substring(0, 8));
        cuerpo.put("serie", "T057");
        cuerpo.put("monedaFE", "CRC");
        cuerpo.put("tipoCambio", 1);
        cuerpo.put("tipoFactura", "INSTITUCIONAL");
        cuerpo.put("subtotal", 0);
        cuerpo.put("igv", 0);
        cuerpo.put("total", 0);
        MvcResult creada = mockMvc.perform(post("/facturas/guardar").with(user(USUARIO).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(cuerpo))).andReturn();
        assertThat(creada.getResponse().getStatus()).as(creada.getResponse().getContentAsString()).isEqualTo(200);
        int id = objectMapper.readTree(creada.getResponse().getContentAsString()).get("idFactura").asInt();
        jdbc.update("UPDATE factura SET create_date = ? WHERE id_factura = ?", horaCostaRica, id);

        List<Map<String, Object>> lineas = new ArrayList<>();
        lineas.add(linea(-1, 1, id, gravado13, "1130.00"));
        lineas.add(linea(-2, 2, id, exento, "500.00"));
        MvcResult guardadas = mockMvc.perform(put("/lineas-factura/actualizar").with(user(USUARIO).roles("ADMIN"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(lineas)))
                .andReturn();
        assertThat(guardadas.getResponse().getStatus()).as(guardadas.getResponse().getContentAsString()).isEqualTo(200);
        return id;
    }

    private static Map<String, Object> linea(int idLinea, int numero, int idFactura, Producto producto, String precio) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("id_factura", idFactura);
        linea.put("id_linea_factura", idLinea);
        linea.put("numero_linea", numero);
        linea.put("id_producto", producto.getIdProducto());
        linea.put("cantidad", 1);
        linea.put("precioUnitario", new BigDecimal(precio));
        linea.put("subtotal", new BigDecimal(precio));
        return linea;
    }

    /** POST /facturas/guardar para editar la factura (entidad completa) con campos extra que pisan los del cuerpo base. */
    private MvcResult guardarEdicion(int idFactura, String condicionVenta, Map<String, Object> extra) throws Exception {
        Map<String, Object> f = jdbc.queryForObject("SELECT numero_factura, serie, id_cliente, total, subtotal, igv, create_date "
                + "FROM factura WHERE id_factura = ?", (rs, i) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idFactura", idFactura);
            m.put("numeroFactura", rs.getString(1));
            m.put("serie", rs.getString(2));
            m.put("cliente", Map.of("idCliente", rs.getInt(3)));
            m.put("monedaFE", "CRC");
            m.put("tipoCambio", 1);
            m.put("condicionVentaFE", condicionVenta);
            m.put("tipoFactura", "INSTITUCIONAL");
            m.put("total", rs.getBigDecimal(4));
            m.put("subtotal", rs.getBigDecimal(5));
            m.put("igv", rs.getBigDecimal(6));
            m.put("createDate", rs.getTimestamp(7).toLocalDateTime().toString());
            return m;
        }, idFactura);
        if (extra != null) {
            f.putAll(extra);
        }
        return mockMvc.perform(post("/facturas/guardar").with(user(USUARIO).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(f))).andReturn();
    }

    private String regimen(int idFactura) {
        return jdbc.queryForObject("SELECT regimen_emisor FROM factura WHERE id_factura = ?", String.class, idFactura);
    }

    record Totales(BigDecimal subtotal, BigDecimal igv, BigDecimal total) {
    }

    private Totales totales(int idFactura) {
        return jdbc.queryForObject("SELECT subtotal, igv, total FROM factura WHERE id_factura = ?",
                (rs, i) -> new Totales(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3)), idFactura);
    }

    /** Líneas del asiento de venta vigente por código de cuenta: [debe, haber]. */
    private Map<String, BigDecimal[]> asientoVigente(int idFactura) {
        return tx.execute(s -> {
            List<AsientoContable> vigentes = asientoRepository.findAsientosVentaVigentes((long) idFactura);
            assertThat(vigentes).hasSize(1);
            Map<String, BigDecimal[]> res = new LinkedHashMap<>();
            for (DetalleAsiento d : vigentes.get(0).getDetalles()) {
                BigDecimal[] acumulado = res.computeIfAbsent(d.getCuenta().getCodigo(),
                        k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
                acumulado[0] = acumulado[0].add(d.getDebe() != null ? d.getDebe() : BigDecimal.ZERO);
                acumulado[1] = acumulado[1].add(d.getHaber() != null ? d.getHaber() : BigDecimal.ZERO);
            }
            return res;
        });
    }

    private LocalDate fechaAsientoVigente(int idFactura) {
        return tx.execute(s -> asientoRepository.findAsientosVentaVigentes((long) idFactura).get(0).getFecha());
    }

    private int comprobantes(int idFactura) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico WHERE factura_id = ?", Integer.class,
                idFactura);
    }

    private long consecutivoFactura() {
        return jdbc.queryForObject("SELECT consecutivo_factura FROM configuracion_hacienda WHERE id = ?", Long.class,
                idConfigHacienda);
    }

    /** Comprobante vigente de la factura en el estado dado, insertado por SQL (sin pasar por la generación). */
    private Long comprobanteInsertado(int idFactura, String estado) {
        Long idEmpresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc().orElseThrow().getIdConfiguracion()
                .longValue();
        StringBuilder clave = new StringBuilder("506");
        while (clave.length() < 50) {
            clave.append(ThreadLocalRandom.current().nextInt(10));
        }
        jdbc.update("INSERT INTO comprobante_electronico (clave_numerica, consecutivo, created_at, updated_at, estado, "
                        + "fecha_emision, fecha_envio, tipo_comprobante, empresa_id, factura_id, vigente, intentos_envio, "
                        + "enviado_email, xml_comprobante, ruta_archivo_xml, ambiente) "
                        + "VALUES (?, ?, NOW(6), NOW(6), ?, NOW(6), NOW(6), 'FACTURA_ELECTRONICA', ?, ?, b'1', 0, b'0', ?, ?, 'SANDBOX')",
                clave.toString(), String.format("%020d", ThreadLocalRandom.current().nextLong(1, 1_000_000_000L)), estado,
                idEmpresa, idFactura, "<FacturaElectronica/>", "comprobantes/t057.xml");
        return jdbc.queryForObject("SELECT MAX(id) FROM comprobante_electronico WHERE factura_id = ?", Long.class,
                idFactura);
    }

    private void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(USUARIO, null,
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @SuppressWarnings("deprecation")
    private Producto producto(String descripcion, String precio, boolean gravado) {
        Producto p = new Producto();
        p.setCodigo("T57-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDescripcion(descripcion);
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
}
