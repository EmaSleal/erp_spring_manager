package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.service.PagoProveedorService;
import api.astro.whats_orders_manager.modules.proveedores.service.ProveedorService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-012 (ADR-016) — alta de CxP manual en /contabilidad/cuentas-pagar (plantilla Thymeleaf real) y
 * endpoint de cuentas elegibles como contrapartida, ambos protegidos con CUENTA_PAGAR_CREAR.
 */
@SpringJUnitWebConfig(CuentasPagarNuevaCxpViewTest.Config.class)
@DisplayName("Cuentas por pagar — alta de CxP manual (vista y cuentas de contrapartida)")
class CuentasPagarNuevaCxpViewTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class Config {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }

        @Bean(name = "permisoService")
        PermisoService permisoService() {
            return mock(PermisoService.class);
        }

        @Bean
        CuentaPorPagarService cuentaPorPagarService() {
            return mock(CuentaPorPagarService.class);
        }

        @Bean
        ProveedorService proveedorService() {
            return mock(ProveedorService.class);
        }

        @Bean
        CuentaContableRepository cuentaContableRepository() {
            return mock(CuentaContableRepository.class);
        }

        @Bean
        ParametroContableRepository parametroContableRepository() {
            return mock(ParametroContableRepository.class);
        }

        @Bean
        AsientoContableService asientoContableService(CuentaContableRepository cuentas,
                                                      ParametroContableRepository parametros) {
            return new AsientoContableService(mock(AsientoContableRepository.class),
                    mock(DetalleAsientoRepository.class), cuentas, parametros);
        }

        @Bean
        ContabilidadViewController contabilidadViewController(CuentaPorPagarService cxp, ProveedorService proveedores) {
            return new ContabilidadViewController(cxp, proveedores);
        }

        @Bean
        CuentaPorPagarRestController cuentaPorPagarRestController(CuentaPorPagarService cxp,
                                                                  AsientoContableService asientos) {
            return new CuentaPorPagarRestController(cxp, mock(PagoProveedorService.class),
                    mock(UsuarioService.class), asientos);
        }

        @Bean
        SpringResourceTemplateResolver templateResolver(ApplicationContext context) {
            SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
            resolver.setApplicationContext(context);
            resolver.setPrefix("classpath:/templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");
            resolver.setCacheable(false);
            return resolver;
        }

        @Bean
        SpringTemplateEngine templateEngine(SpringResourceTemplateResolver resolver) {
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            engine.addDialect(new SpringSecurityDialect());
            return engine;
        }

        @Bean
        ThymeleafViewResolver viewResolver(SpringTemplateEngine engine) {
            ThymeleafViewResolver resolver = new ThymeleafViewResolver();
            resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private PermisoService permisoService;
    @Autowired private CuentaPorPagarService cuentaPorPagarService;
    @Autowired private ProveedorService proveedorService;
    @Autowired private CuentaContableRepository cuentaContableRepository;
    @Autowired private ParametroContableRepository parametroContableRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, cuentaPorPagarService, proveedorService, cuentaContableRepository,
                parametroContableRepository);
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        for (String usuario : List.of("lector", "contador")) {
            when(permisoService.tienePermisoPorCodigo(eq(usuario), eq("CUENTA_PAGAR_VER"))).thenReturn(true);
        }
        when(permisoService.tienePermisoPorCodigo(eq("contador"), eq("CUENTA_PAGAR_CREAR"))).thenReturn(true);
        when(cuentaPorPagarService.findAll()).thenReturn(List.of());
        Proveedor proveedor = new Proveedor();
        proveedor.setId(7L);
        proveedor.setNombre("Servicios Técnicos S.A.");
        when(proveedorService.findActivos()).thenReturn(List.of(proveedor));
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ── Vista ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sin CUENTA_PAGAR_CREAR no se renderizan el botón ni el modal de alta")
    void sinPermisoCrear_noRenderizaBotonNiModal() throws Exception {
        mockMvc.perform(get("/contabilidad/cuentas-pagar").with(user("lector")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cuentas por Pagar")))
                .andExpect(content().string(not(containsString("Nueva cuenta por pagar"))))
                .andExpect(content().string(not(containsString("id=\"btnNuevaCpp\""))))
                .andExpect(content().string(not(containsString("id=\"modalNuevaCpp\""))));
    }

    @Test
    @DisplayName("con CUENTA_PAGAR_CREAR se renderizan el botón y el modal con todos sus campos")
    void conPermisoCrear_renderizaBotonYModal() throws Exception {
        mockMvc.perform(get("/contabilidad/cuentas-pagar").with(user("contador")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"btnNuevaCpp\"")))
                .andExpect(content().string(containsString("Nueva cuenta por pagar")))
                .andExpect(content().string(containsString("id=\"modalNuevaCpp\"")))
                .andExpect(content().string(containsString("id=\"cppProveedor\"")))
                .andExpect(content().string(containsString("Servicios Técnicos S.A.")))
                .andExpect(content().string(containsString("id=\"cppDescripcion\"")))
                .andExpect(content().string(containsString("id=\"cppFechaEmision\"")))
                .andExpect(content().string(containsString("id=\"cppFechaVencimiento\"")))
                .andExpect(content().string(containsString("id=\"cppMoneda\"")))
                .andExpect(content().string(containsString("<option value=\"USD\">USD</option>")))
                .andExpect(content().string(containsString("<option value=\"EUR\">EUR</option>")))
                .andExpect(content().string(containsString("id=\"cppTipoCambio\"")))
                .andExpect(content().string(containsString("id=\"cppSubtotal\"")))
                .andExpect(content().string(containsString("id=\"cppImpuesto\"")))
                .andExpect(content().string(containsString("id=\"cppTotal\"")))
                .andExpect(content().string(containsString("id=\"cppCuentaGasto\"")))
                .andExpect(content().string(containsString("/shared/js/tipo-cambio-documento.js")));
    }

    // ── Endpoint de cuentas de contrapartida ─────────────────────────────────

    @Test
    @DisplayName("cuentas de contrapartida: solo activas con movimientos EGRESO/ACTIVO no configuradas como banco, CxC, CxP o IVA; marca CPP_CUENTA_COMPRAS")
    void cuentasContrapartida_filtraYMarcaCompras() throws Exception {
        CuentaContable compras = cuenta(1L, "5.1.04.001", TipoCuenta.EGRESO, true, true);
        CuentaContable activoFijo = cuenta(2L, "1.2.01.001", TipoCuenta.ACTIVO, true, true);
        CuentaContable gasto = cuenta(3L, "5.1.03.001", TipoCuenta.EGRESO, true, true);
        CuentaContable ingreso = cuenta(4L, "4.1.01.001", TipoCuenta.INGRESO, true, true);
        CuentaContable pasivo = cuenta(5L, "2.1.05.001", TipoCuenta.PASIVO, true, true);
        CuentaContable capital = cuenta(6L, "3.1.01.001", TipoCuenta.CAPITAL, true, true);
        CuentaContable banco = cuenta(7L, "1.1.01.002", TipoCuenta.ACTIVO, true, true);
        CuentaContable caja = cuenta(8L, "1.1.01.001", TipoCuenta.ACTIVO, true, true);
        CuentaContable cxc = cuenta(9L, "1.1.02.001", TipoCuenta.ACTIVO, true, true);
        CuentaContable iva = cuenta(10L, "1.1.04.001", TipoCuenta.ACTIVO, true, true);
        // Inactive and grouping accounts are filtered by the repository query (activa, aceptaMovimientos)
        when(cuentaContableRepository.findByAceptaMovimientosAndActiva(true, true)).thenReturn(List.of(
                gasto, ingreso, pasivo, capital, banco, caja, cxc, iva, compras, activoFijo));
        ParametroContable pCompras = param("CPP_CUENTA_COMPRAS", compras);
        when(parametroContableRepository.findByClave("CPP_CUENTA_COMPRAS")).thenReturn(Optional.of(pCompras));
        when(parametroContableRepository.findAllWithCuentas()).thenReturn(List.of(
                pCompras,
                param("CPP_CUENTA_POR_PAGAR", pasivo),
                param("CPP_IVA_CREDITO_FISCAL", iva),
                param("CPP_BANCO_TRANSFERENCIA", banco),
                param("COBRO_CAJA_EFECTIVO", caja),
                param("VENTA_CXC_CLIENTES", cxc)));

        mockMvc.perform(get("/api/cuentas-pagar/cuentas-contrapartida").with(user("contador")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuentaComprasId").value(1))
                .andExpect(jsonPath("$.data.cuentas[*].codigo").value(contains("1.2.01.001", "5.1.03.001", "5.1.04.001")))
                .andExpect(jsonPath("$.data.cuentas[*].id").value(contains(2, 3, 1)))
                .andExpect(jsonPath("$.data.cuentas[*].tipo").value(contains("ACTIVO", "EGRESO", "EGRESO")))
                .andExpect(jsonPath("$.data.cuentas[*].predeterminada").value(contains(false, false, true)))
                .andExpect(jsonPath("$.data.cuentas[0].nombre").value("Cuenta 1.2.01.001"));
    }

    @Test
    @DisplayName("cuentas de contrapartida sin CUENTA_PAGAR_CREAR → 403")
    void cuentasContrapartida_sinPermiso403() throws Exception {
        mockMvc.perform(get("/api/cuentas-pagar/cuentas-contrapartida").with(user("lector")))
                .andExpect(status().isForbidden());
    }

    private CuentaContable cuenta(long id, String codigo, TipoCuenta tipo, boolean activa, boolean movimientos) {
        CuentaContable c = new CuentaContable();
        c.setIdCuenta(id);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setTipo(tipo);
        c.setActiva(activa);
        c.setAceptaMovimientos(movimientos);
        return c;
    }

    private ParametroContable param(String clave, CuentaContable cuenta) {
        ParametroContable p = new ParametroContable();
        p.setClave(clave);
        p.setCuentaContable(cuenta);
        return p;
    }
}
