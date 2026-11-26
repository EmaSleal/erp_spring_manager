package api.astro.whats_orders_manager.modules.proveedores.it;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.proveedores.dto.PagoProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.service.PagoProveedorService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-053 (criterio 7): anularPago vuelve a leer el estado del pago después de tomar el bloqueo (proveedor → CxP → pago).
 * Dos anulaciones simultáneas del mismo pago: una termina bien y la otra recibe el 409 fijo; una sola reversión del
 * asiento y el saldo de la CxP se restituye una sola vez. Sobre MySQL real (Testcontainers).
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-053 — anulación de pagos a proveedores: lectura tras el bloqueo y 409 (MySQL)")
class PagoProveedorAnulacionConcurrenteIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t053p", "t053-it-mysql-pagos");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    private static final String USUARIO = "admin.bootstrap@example.invalid";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 20);

    @MockitoBean private HaciendaApiService haciendaApi;
    @MockitoBean private ConfiguracionHaciendaService configuracionHaciendaService;
    @MockitoBean private PermisoService permisoService;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService regimenServicio;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private CuentaPorPagarService cxpService;
    @Autowired private PagoProveedorService pagoService;
    @Autowired private UsuarioService usuarioService;

    private TransactionTemplate tx;
    private Long gastoId;

    @BeforeEach
    void setUp() {
        // T-058: toda CxP consulta el régimen de la empresa en la fecha del comprobante; con esquema de Hibernate no hay tramos
        api.astro.whats_orders_manager.modules.regimen.RegimenSembrado.generalSiVacia(jdbc, regimenServicio);
        tx = new TransactionTemplate(transactionManager);
        when(permisoService.tienePermisoPorCodigo(any(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq(USUARIO), eq("CUENTA_PAGAR_REGISTRAR_PAGO"))).thenReturn(true);
        tx.executeWithoutResult(s -> {
            if (parametroRepository.findByClave("CPP_CUENTA_POR_PAGAR").isEmpty()) {
                param("CPP_CUENTA_COMPRAS", cuenta("9.5.04.001", "Compras", TipoCuenta.EGRESO));
                param("CPP_CUENTA_POR_PAGAR", cuenta("9.2.05.001", "Proveedores", TipoCuenta.PASIVO));
                param("CPP_IVA_CREDITO_FISCAL", cuenta("9.1.04.001", "IVA crédito", TipoCuenta.ACTIVO));
            }
            if (parametroRepository.findByClave("CPP_BANCO_EFECTIVO").isEmpty()) {
                param("CPP_BANCO_EFECTIVO", cuenta("9.1.01.001", "Caja", TipoCuenta.ACTIVO));
            }
            gastoId = cuentaRepository.findByCodigo("9.5.04.001").map(CuentaContable::getIdCuenta).orElseThrow();
        });
    }

    @Test
    @DisplayName("dos hilos anulan el mismo pago: uno gana, el otro recibe el 409 «El pago ya está anulado»; una reversión y el saldo se restituye una vez")
    void anulacionConcurrente_soloUnaGana() throws Exception {
        Usuario usuario = usuarioService.findByEmail(USUARIO).orElseThrow();
        for (int vuelta = 0; vuelta < 3; vuelta++) {
            Long[] ids = cuentaConPago(usuario);
            Long cxpId = ids[0];
            Long pagoId = ids[1];
            assertThat(saldo(cxpId)).isEqualByComparingTo("800.00");

            int hilos = 2;
            CountDownLatch listos = new CountDownLatch(hilos);
            CountDownLatch salida = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(hilos);
            List<Future<Object>> futuros = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                futuros.add(pool.submit((Callable<Object>) () -> {
                    listos.countDown();
                    salida.await();
                    try {
                        pagoService.anularPago(pagoId, usuario);
                        return "OK";
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            listos.await();
            salida.countDown();
            List<Object> resultados = new ArrayList<>();
            for (Future<Object> f : futuros) {
                resultados.add(f.get(60, TimeUnit.SECONDS));
            }
            pool.shutdown();

            assertThat(resultados.stream().filter("OK"::equals).count()).as("vuelta %d: exitosas", vuelta).isEqualTo(1);
            List<Object> fallos = resultados.stream().filter(r -> r instanceof Exception).toList();
            assertThat(fallos).hasSize(1);
            assertThat(fallos.get(0)).isInstanceOf(IllegalStateException.class);
            assertThat(((Exception) fallos.get(0)).getMessage()).isEqualTo("El pago ya está anulado");
            assertThat(saldo(cxpId)).as("el saldo se restituye una sola vez").isEqualByComparingTo("1000.00");
            assertThat(jdbc.queryForObject("SELECT estado FROM pagos_proveedor WHERE id = ?", String.class, pagoId))
                    .isEqualTo("ANULADO");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE pago_proveedor_id = ?",
                    Integer.class, pagoId)).as("original + una reversión").isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT estado FROM cuentas_por_pagar WHERE id = ?", String.class, cxpId))
                    .isEqualTo("PENDIENTE");
        }
    }

    @Test
    @DisplayName("POST /api/cuentas-pagar/pagos/{id}/anular: 200 la primera vez y 409 con el mensaje fijo la segunda; 404 si no existe")
    void endpoint_409() throws Exception {
        Usuario usuario = usuarioService.findByEmail(USUARIO).orElseThrow();
        Long pagoId = cuentaConPago(usuario)[1];
        mockMvc.perform(post("/api/cuentas-pagar/pagos/" + pagoId + "/anular").with(user(USUARIO)).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cuentas-pagar/pagos/" + pagoId + "/anular").with(user(USUARIO)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El pago ya está anulado"));
        mockMvc.perform(post("/api/cuentas-pagar/pagos/987654/anular").with(user(USUARIO)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** CxP de 1000,00 con un pago confirmado de 200,00 (saldo 800,00). Devuelve {cxpId, pagoId}. */
    private Long[] cuentaConPago(Usuario usuario) {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        Proveedor p = proveedorRepository.save(Proveedor.builder().cedula("31" + sufijo).nombre("Proveedor " + sufijo)
                .tipoProveedor(TipoProveedor.values()[0]).build());
        var cpp = cxpService.crear(CuentaPorPagarDTO.builder().proveedorId(p.getId()).descripcion("Compra")
                .numeroComprobanteProveedor("FE-" + sufijo).fechaComprobante(FECHA).moneda(MonedaFE.CRC)
                .lineas(List.of(new CuentaPorPagarLineaDTO("Línea", gastoId, "08", new BigDecimal("884.96"),
                        new BigDecimal("115.04"), TratamientoIvaCompra.PLENO, null))).build());
        PagoProveedor pago = pagoService.registrarPago(PagoProveedorDTO.builder().cuentaPorPagarId(cpp.getId())
                .monto(new BigDecimal("200.00")).metodoPago(MetodoPagoProveedor.EFECTIVO).fecha(FECHA).build(), usuario);
        return new Long[] {cpp.getId(), pago.getId()};
    }

    private BigDecimal saldo(Long cxpId) {
        return jdbc.queryForObject("SELECT saldo_pendiente FROM cuentas_por_pagar WHERE id = ?", BigDecimal.class, cxpId);
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
