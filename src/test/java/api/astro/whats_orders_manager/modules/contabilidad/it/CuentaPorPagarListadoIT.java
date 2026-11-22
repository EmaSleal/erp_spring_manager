package api.astro.whats_orders_manager.modules.contabilidad.it;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaPorPagarRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.OrdenCompraRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-052 (P-023 §1): GET /api/cuentas-pagar devolvía 500 porque serializaba la entidad con el proveedor y la orden de
 * compra como proxies perezosos. Con un usuario que solo tiene CUENTA_PAGAR_VER y una CxP ligada a proveedor y OC
 * (leída en otra transacción, como en una petición real) el listado, las vencidas y el detalle responden 200 con DTO.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-052 — listado de cuentas por pagar con carga diferida (MySQL)")
class CuentaPorPagarListadoIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t052l", "t052-it-mysql-listado");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @MockitoBean private HaciendaApiService haciendaApi;
    @MockitoBean private ConfiguracionHaciendaService configuracionHaciendaService;
    @MockitoBean private PermisoService permisoService;

    @Autowired private MockMvc mockMvc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private OrdenCompraRepository ordenCompraRepository;
    @Autowired private CuentaPorPagarRepository cuentaRepository;

    @Test
    @DisplayName("usuario solo con CUENTA_PAGAR_VER: listado, filtro por estado, vencidas y detalle responden 200")
    void listadoConProveedorYOrdenCompraPerezosos() throws Exception {
        when(permisoService.tienePermisoPorCodigo(any(), eq("CUENTA_PAGAR_VER"))).thenReturn(true);
        Long id = new TransactionTemplate(transactionManager).execute(s -> {
            Proveedor p = proveedorRepository.save(Proveedor.builder().cedula("3101555000")
                    .nombre("Proveedor Perezoso S.A.").tipoProveedor(TipoProveedor.values()[0]).build());
            OrdenCompra oc = ordenCompraRepository.save(OrdenCompra.builder().numero("OC-2026-90001").proveedor(p)
                    .estado(EstadoOrdenCompra.RECIBIDA).fecha(LocalDate.of(2026, 11, 1)).moneda(MonedaFE.CRC)
                    .subtotal(new BigDecimal("1000.00")).impuesto(new BigDecimal("130.00"))
                    .total(new BigDecimal("1130.00")).build());
            return cuentaRepository.save(CuentaPorPagar.builder().numero("CPP-2026-90001").proveedor(p)
                    .ordenCompra(oc).descripcion("Compra según OC").monto(new BigDecimal("1130.00"))
                    .saldoPendiente(new BigDecimal("1130.00")).moneda(MonedaFE.CRC).tipoCambio(BigDecimal.ONE)
                    .fechaEmision(LocalDate.of(2026, 11, 1)).fechaVencimiento(LocalDate.of(2026, 11, 10)).build()).getId();
        });

        mockMvc.perform(get("/api/cuentas-pagar").with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].numero").value("CPP-2026-90001"))
                .andExpect(jsonPath("$.data[0].proveedorNombre").value("Proveedor Perezoso S.A."))
                .andExpect(jsonPath("$.data[0].ordenCompraNumero").value("OC-2026-90001"));
        mockMvc.perform(get("/api/cuentas-pagar").param("estado", "PENDIENTE").with(user("lector")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/cuentas-pagar/vencidas").with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].proveedorNombre").value("Proveedor Perezoso S.A."));
        mockMvc.perform(get("/api/cuentas-pagar/" + id).with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.proveedorId").isNumber())
                .andExpect(jsonPath("$.data.ordenCompraNumero").value("OC-2026-90001"));
    }
}
