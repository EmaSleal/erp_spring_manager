package api.astro.whats_orders_manager.modules.compras;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.compras.dto.CompraContadoDTO;
import api.astro.whats_orders_manager.modules.compras.dto.CompraContadoResultadoDTO;
import api.astro.whats_orders_manager.modules.compras.service.AdjuntoComprobante;
import api.astro.whats_orders_manager.modules.compras.service.AdjuntoComprobanteService;
import api.astro.whats_orders_manager.modules.compras.service.CompraContadoService;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.ComprobanteProveedorDuplicadoException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.RegimenHacienda;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.regimen.tope.TopeComprasRtsService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-058 criterio 10 de punta a punta sobre MySQL (Testcontainers, esquema de Hibernate): la compra de contado crea la CxP y su
 * pago desde caja en una transacción, con asientos de compra y de pago, tope del RTS evaluado, reversión total ante cualquier
 * fallo (CxP, pago, asientos y archivo adjunto), duplicado 409 y orden de bloqueos proveedor primero.
 *
 * Tramos de régimen de la base de pruebas (declarados aquí, no los de Flyway): SIMPLIFICADO desde 2000-01-01 y GENERAL desde
 * 2026-09-01, como en V58; el validador rechaza fechas futuras, así que los casos SIMPLIFICADO usan fechas de junio de 2026.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-058 — compra de contado: CxP y pago desde caja en una transacción (MySQL)")
class CompraContadoIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t058c", "t058-it-mysql-compra-contado");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    private static final String USUARIO = "admin.bootstrap@example.invalid";
    private static final LocalDate FECHA_RTS = LocalDate.of(2026, 6, 15);
    private static final LocalDate FECHA_GENERAL = LocalDate.of(2026, 10, 20);
    private static final byte[] PDF = "%PDF-1.4 comprobante".getBytes(StandardCharsets.ISO_8859_1);

    @MockitoBean private HaciendaApiService haciendaApi;
    @MockitoBean private ConfiguracionHaciendaService configuracionHaciendaService;
    @MockitoBean private PermisoService permisoService;
    @MockitoSpyBean private TopeComprasRtsService topeService;
    @MockitoSpyBean private ProveedorRepository proveedorRepository;
    @MockitoSpyBean private CuentaPorPagarRepository cuentaPorPagarRepository;
    @MockitoSpyBean private AdjuntoComprobanteService adjuntoService;

    @Autowired private CompraContadoService compraService;
    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RegimenTributarioService regimenServicio;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private UsuarioService usuarioService;
    @org.springframework.beans.factory.annotation.Value("${app.uploads.directorio}") private String uploads;

    private TransactionTemplate tx;
    private Long comprasId;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        reset(topeService, proveedorRepository, cuentaPorPagarRepository, adjuntoService);
        when(permisoService.tienePermisoPorCodigo(any(), anyString())).thenReturn(true);
        jdbc.update("DELETE FROM regimen_tributario");
        jdbc.update("INSERT INTO regimen_tributario (regimen, vigente_desde, nota, fecha_registro) VALUES "
                + "('SIMPLIFICADO', '2000-01-01', 'T-058 IT', NOW()), ('GENERAL', '2026-09-01', 'T-058 IT', NOW())");
        regimenServicio.invalidarCache();
        usuario = usuarioService.findByEmail(USUARIO).orElseThrow();
        tx.executeWithoutResult(s -> {
            if (parametroRepository.findByClave("CPP_CUENTA_POR_PAGAR").isEmpty()) {
                param("CPP_CUENTA_COMPRAS", cuenta("5.3.01.001", "Compras de mercadería", TipoCuenta.EGRESO));
                param("CPP_CUENTA_POR_PAGAR", cuenta("2.1.01.001", "Proveedores", TipoCuenta.PASIVO));
                param("CPP_IVA_CREDITO_FISCAL", cuenta("1.1.04.001", "IVA crédito", TipoCuenta.ACTIVO));
                param("CPP_BANCO_EFECTIVO", cuenta("1.1.01.001", "Caja General", TipoCuenta.ACTIVO));
            }
            comprasId = cuentaRepository.findByCodigo("5.3.01.001").orElseThrow().getIdCuenta();
        });
    }

    // ── Camino feliz ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("SIMPLIFICADO, tarifa 08 e IVA 0, EFECTIVO desde caja: una CxP PAGADA, un pago, asientos de compra y de pago, tope evaluado")
    void camino_feliz() {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        // sin tratamiento: el servicio lo completa con el que fija la política (NO_ACREDITABLE)
        CompraContadoResultadoDTO r = compraService.registrar(dto(p, "F-1", FECHA_RTS, null, "0", MetodoPagoProveedor.EFECTIVO), null, usuario);

        assertThat(r.numeroCuentaPorPagar()).startsWith("CPP-");
        assertThat(r.tieneAdjunto()).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE proveedor_id = ?", Integer.class, p.getId())).isEqualTo(1);
        var cxp = jdbc.queryForMap("SELECT estado, saldo_pendiente, monto, regimen_proveedor, estado_aceptacion FROM cuentas_por_pagar WHERE id = ?",
                r.cuentaPorPagarId());
        assertThat(cxp.get("estado")).isEqualTo("PAGADA");
        assertThat((BigDecimal) cxp.get("saldo_pendiente")).isEqualByComparingTo("0");
        assertThat((BigDecimal) cxp.get("monto")).isEqualByComparingTo("1000.00");
        assertThat(cxp.get("regimen_proveedor")).isEqualTo("SIMPLIFICADO");
        assertThat(cxp.get("estado_aceptacion")).isEqualTo("NO_APLICA");
        assertThat(jdbc.queryForObject("SELECT tratamiento FROM cuenta_pagar_linea WHERE cuenta_por_pagar_id = ?", String.class, r.cuentaPorPagarId()))
                .isEqualTo("NO_ACREDITABLE");

        var pago = jdbc.queryForMap("SELECT metodo_pago, monto, estado, cuenta_bancaria_id FROM pagos_proveedor WHERE cuenta_por_pagar_id = ?",
                r.cuentaPorPagarId());
        assertThat(pago.get("metodo_pago")).isEqualTo("EFECTIVO");
        assertThat((BigDecimal) pago.get("monto")).isEqualByComparingTo("1000.00");
        assertThat(pago.get("estado")).isEqualTo("CONFIRMADO");
        assertThat(pago.get("cuenta_bancaria_id")).isNull();

        // asiento de compra: debe 5.3.01.001, haber CxP
        assertThat(movimiento("a.cuenta_por_pagar_id = ? AND a.pago_proveedor_id IS NULL", r.cuentaPorPagarId(), "5.3.01.001", "debe")).isEqualByComparingTo("1000.00");
        assertThat(movimiento("a.cuenta_por_pagar_id = ? AND a.pago_proveedor_id IS NULL", r.cuentaPorPagarId(), "2.1.01.001", "haber")).isEqualByComparingTo("1000.00");
        // asiento de pago: debe CxP, haber caja general 1.1.01.001
        assertThat(movimiento("a.pago_proveedor_id = ?", r.pagoId(), "2.1.01.001", "debe")).isEqualByComparingTo("1000.00");
        assertThat(movimiento("a.pago_proveedor_id = ?", r.pagoId(), "1.1.01.001", "haber")).isEqualByComparingTo("1000.00");
        // el tope del RTS se evalúa una vez, como en cualquier CxP
        verify(topeService, times(1)).evaluarTrasRegistro(2026);
    }

    @Test
    @DisplayName("proveedor SIMPLIFICADO con la empresa en GENERAL: IVA 0 y NO_ACREDITABLE fijado; aceptación PENDIENTE")
    void proveedorSimplificadoEmpresaGeneral() {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        CompraContadoResultadoDTO r = compraService.registrar(dto(p, "F-2", FECHA_GENERAL, null, "0", null), null, usuario);
        assertThat(jdbc.queryForObject("SELECT estado_aceptacion FROM cuentas_por_pagar WHERE id = ?", String.class, r.cuentaPorPagarId()))
                .isEqualTo("PENDIENTE");
        assertThat(jdbc.queryForObject("SELECT metodo_pago FROM pagos_proveedor WHERE cuenta_por_pagar_id = ?", String.class, r.cuentaPorPagarId()))
                .as("método por defecto").isEqualTo("EFECTIVO");
    }

    @Test
    @DisplayName("sin tratamiento y sin regla que lo fije (GENERAL/GENERAL): 400 con mensaje fijo y nada persistido")
    void sinTratamientoSinRegla() {
        Proveedor p = proveedor(RegimenHacienda.GENERAL);
        assertThatThrownBy(() -> compraService.registrar(dto(p, "F-3", FECHA_GENERAL, null, "130", null), null, usuario))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Cada línea debe indicar su tratamiento de IVA");
        assertNadaPersistido(p);
    }

    // ── Reversión ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reversión: el pago falla (TRANSFERENCIA sin cuenta bancaria) → no queda CxP, pago ni asiento")
    void reversion_pagoFalla() {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        long asientos = jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables", Long.class);
        assertThatThrownBy(() -> compraService.registrar(
                dto(p, "F-4", FECHA_RTS, TratamientoIvaCompra.NO_ACREDITABLE, "0", MetodoPagoProveedor.TRANSFERENCIA), null, usuario))
                .isInstanceOf(IllegalArgumentException.class);
        assertNadaPersistido(p);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables", Long.class)).isEqualTo(asientos);
        // evaluarTrasRegistro se invoca al crear la CxP, pero espera a que la transacción confirme: al revertirse no evalúa nada
    }

    @Test
    @DisplayName("reversión: falla la CxP (IVA > 0 con proveedor SIMPLIFICADO, 400) → nada persistido, no se intenta el pago")
    void reversion_cxpFalla() {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        assertThatThrownBy(() -> compraService.registrar(
                dto(p, "F-5", FECHA_RTS, TratamientoIvaCompra.NO_ACREDITABLE, "130", null), null, usuario))
                .isInstanceOf(IllegalArgumentException.class);
        assertNadaPersistido(p);
    }

    @Test
    @DisplayName("fecha sin tramo de régimen → RegimenNoDefinidoException (409 en la API) y nada persistido")
    void sinTramo() {
        jdbc.update("DELETE FROM regimen_tributario");
        regimenServicio.invalidarCache();
        Proveedor p = proveedor(RegimenHacienda.GENERAL);
        assertThatThrownBy(() -> compraService.registrar(dto(p, "F-6", FECHA_RTS, TratamientoIvaCompra.PLENO, "130", null), null, usuario))
                .isInstanceOf(RegimenNoDefinidoException.class);
        assertNadaPersistido(p);
    }

    @Test
    @DisplayName("comprobante duplicado del mismo proveedor: ComprobanteProveedorDuplicadoException y sigue habiendo una sola CxP y un solo pago")
    void duplicado() {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        compraService.registrar(dto(p, "F-7", FECHA_RTS, null, "0", null), null, usuario);
        assertThatThrownBy(() -> compraService.registrar(dto(p, " f-7", FECHA_RTS, null, "0", null), null, usuario))
                .isInstanceOf(ComprobanteProveedorDuplicadoException.class)
                .hasMessageStartingWith("Ya existe una cuenta por pagar de este proveedor con el comprobante");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE proveedor_id = ?", Integer.class, p.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pagos_proveedor pp JOIN cuentas_por_pagar c ON c.id = pp.cuenta_por_pagar_id "
                + "WHERE c.proveedor_id = ?", Integer.class, p.getId())).isEqualTo(1);
    }

    // ── Orden de bloqueos ────────────────────────────────────────────────────

    @Test
    @DisplayName("orden de bloqueos: proveedor FOR UPDATE antes de insertar la CxP y, en el pago, proveedor antes que la CxP")
    void ordenDeBloqueos() {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        reset(proveedorRepository, cuentaPorPagarRepository);
        compraService.registrar(dto(p, "F-8", FECHA_RTS, null, "0", null), null, usuario);
        InOrder orden = inOrder(proveedorRepository, cuentaPorPagarRepository);
        // registrar() bloquea el proveedor y crearConAdvertencias lo vuelve a pedir (mismo bloqueo): ambos antes del INSERT de la CxP
        orden.verify(proveedorRepository, times(2)).findByIdForUpdate(p.getId());
        orden.verify(cuentaPorPagarRepository).saveAndFlush(any());
        orden.verify(proveedorRepository).findByIdForUpdate(p.getId());
        orden.verify(cuentaPorPagarRepository).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("con el proveedor bloqueado por otra transacción, la compra espera sin tocar nada y termina al liberarlo")
    void esperaElBloqueoDelProveedor() throws Exception {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        CountDownLatch bloqueado = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> retiene = pool.submit(() -> tx.executeWithoutResult(s -> {
                proveedorRepository.findByIdForUpdate(p.getId());
                bloqueado.countDown();
                try {
                    liberar.await(20, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertThat(bloqueado.await(10, TimeUnit.SECONDS)).isTrue();
            Future<CompraContadoResultadoDTO> compra = pool.submit(
                    () -> compraService.registrar(dto(p, "F-9", FECHA_RTS, null, "0", null), null, usuario));
            assertThatThrownBy(() -> compra.get(1500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE proveedor_id = ?", Integer.class, p.getId())).isZero();
            liberar.countDown();
            retiene.get(10, TimeUnit.SECONDS);
            assertThat(compra.get(20, TimeUnit.SECONDS).numeroCuentaPorPagar()).startsWith("CPP-");
        } finally {
            liberar.countDown();
            pool.shutdownNow();
        }
    }

    // ── Adjunto ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("adjunto: se guarda con nombre no adivinable, la CxP lo referencia y se descarga con CUENTA_PAGAR_VER")
    void adjunto_guardadoYDescargable() throws Exception {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        AdjuntoComprobante a = adjuntoService.validar(new MockMultipartFile("adjunto", "mi factura.pdf", "application/pdf", PDF));
        CompraContadoResultadoDTO r = compraService.registrar(dto(p, "F-10", FECHA_RTS, null, "0", null), a, usuario);
        assertThat(r.tieneAdjunto()).isTrue();
        var fila = jdbc.queryForMap("SELECT adjunto_ruta, adjunto_nombre_original, adjunto_tipo FROM cuentas_por_pagar WHERE id = ?", r.cuentaPorPagarId());
        String ruta = (String) fila.get("adjunto_ruta");
        assertThat(ruta).matches("cxp/" + r.cuentaPorPagarId() + "/[0-9a-f-]{36}\\.pdf").doesNotContain("factura");
        assertThat(fila.get("adjunto_nombre_original")).isEqualTo("mi factura.pdf");
        assertThat(Files.readAllBytes(Paths.get(uploads).resolve(ruta))).isEqualTo(PDF);
        mockMvc.perform(get("/api/cuentas-pagar/" + r.cuentaPorPagarId() + "/adjunto").with(user("lector")))
                .andExpect(status().isOk()).andExpect(content().bytes(PDF));
        mockMvc.perform(get("/api/cuentas-pagar/" + r.cuentaPorPagarId()).with(user("lector")))
                .andExpect(jsonPath("$.data.adjuntoNombre").value("mi factura.pdf"));
    }

    @Test
    @DisplayName("adjunto: si la transacción se revierte después de escribir el archivo, el archivo se borra y la CxP no existe")
    void adjunto_seBorraAlRevertir() {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        AdjuntoComprobante a = adjuntoService.validar(new MockMultipartFile("adjunto", "f.pdf", "application/pdf", PDF));
        List<Path> antes = archivosDeCxp();
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            compraService.registrar(dto(p, "F-11", FECHA_RTS, null, "0", null), a, usuario);
            throw new IllegalStateException("falla posterior");
        })).hasMessage("falla posterior");
        assertNadaPersistido(p);
        assertThat(archivosDeCxp()).as("no queda archivo huérfano").containsExactlyInAnyOrderElementsOf(antes);
    }

    @Test
    @DisplayName("adjunto: si escribir el archivo falla, se revierten la CxP y el pago")
    void adjunto_falloDeEscrituraRevierte() {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        AdjuntoComprobante a = adjuntoService.validar(new MockMultipartFile("adjunto", "f.pdf", "application/pdf", PDF));
        doThrow(new IllegalStateException("disco lleno")).when(adjuntoService).guardar(any(), any());
        assertThatThrownBy(() -> compraService.registrar(dto(p, "F-12", FECHA_RTS, null, "0", null), a, usuario))
                .hasMessage("disco lleno");
        assertNadaPersistido(p);
    }

    @Test
    @DisplayName("POST multipart /api/compras/contado: registra con el adjunto y responde el número de la CxP")
    void endpointMultipart() throws Exception {
        Proveedor p = proveedor(RegimenHacienda.SIMPLIFICADO);
        String datos = "{\"proveedorId\":" + p.getId() + ",\"numeroComprobanteProveedor\":\"F-13\",\"fechaComprobante\":\"2026-06-15\","
                + "\"lineas\":[{\"cuentaContableId\":" + comprasId + ",\"codigoTarifa\":\"08\",\"base\":1000,\"iva\":0}]}";
        mockMvc.perform(multipart("/api/compras/contado")
                        .file(new MockMultipartFile("datos", "", "application/json", datos.getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("adjunto", "x.pdf", "application/pdf", PDF))
                        .with(user(USUARIO)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.numeroCuentaPorPagar").exists())
                .andExpect(jsonPath("$.data.tieneAdjunto").value(true));
        mockMvc.perform(multipart("/api/compras/contado")
                        .file(new MockMultipartFile("datos", "", "application/json", datos.getBytes(StandardCharsets.UTF_8)))
                        .with(user(USUARIO)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta por pagar de este proveedor con el comprobante F-13"));
    }

    // ── Ayudas ───────────────────────────────────────────────────────────────

    private CompraContadoDTO dto(Proveedor p, String numero, LocalDate fecha, TratamientoIvaCompra trat, String iva, MetodoPagoProveedor metodo) {
        CuentaPorPagarLineaDTO l = new CuentaPorPagarLineaDTO("Compra de mercadería", comprasId, "08", new BigDecimal("1000.00"),
                new BigDecimal(iva), trat, null);
        return CompraContadoDTO.builder().proveedorId(p.getId()).numeroComprobanteProveedor(numero).fechaComprobante(fecha)
                .lineas(List.of(l)).metodoPago(metodo).build();
    }

    private void assertNadaPersistido(Proveedor p) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cuentas_por_pagar WHERE proveedor_id = ?", Integer.class, p.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pagos_proveedor pp JOIN cuentas_por_pagar c ON c.id = pp.cuenta_por_pagar_id "
                + "WHERE c.proveedor_id = ?", Integer.class, p.getId())).isZero();
    }

    private BigDecimal movimiento(String filtro, Object arg, String codigoCuenta, String lado) {
        BigDecimal v = jdbc.queryForObject("SELECT COALESCE(SUM(d." + lado + "), 0) FROM detalles_asiento d "
                + "JOIN asientos_contables a ON a.id = d.asiento_id JOIN cuentas_contables c ON c.id = d.cuenta_id "
                + "WHERE " + filtro + " AND c.codigo = ?", BigDecimal.class, arg, codigoCuenta);
        return v;
    }

    private List<Path> archivosDeCxp() {
        Path dir = Paths.get(uploads, "cxp");
        if (!Files.exists(dir)) return List.of();
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile).toList();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Proveedor proveedor(RegimenHacienda regimen) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Proveedor p = Proveedor.builder().cedula("31" + sufijo).nombre("Proveedor " + sufijo)
                .tipoProveedor(TipoProveedor.values()[0]).regimenHacienda(regimen).build();
        return proveedorRepository.save(p);
    }

    private CuentaContable cuenta(String codigo, String nombre, TipoCuenta tipo) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setNivel(codigo.split("\\.").length);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return cuentaRepository.save(c);
    }

    private void param(String clave, CuentaContable cuenta) {
        parametroRepository.save(ParametroContable.builder().clave(clave).cuentaContable(cuenta).build());
    }
}
