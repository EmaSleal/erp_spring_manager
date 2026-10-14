package api.astro.whats_orders_manager.modules.configuracion.controller;

import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.CuentaBancariaService;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.contabilidad.controller.ContabilidadViewController;
import api.astro.whats_orders_manager.modules.contabilidad.controller.CuentaPorPagarRestController;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.facturacion.controller.PagoController;
import api.astro.whats_orders_manager.modules.facturacion.dto.mapper.PagoMapper;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.proveedores.service.PagoProveedorService;
import api.astro.whats_orders_manager.modules.proveedores.service.ProveedorService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-015 (ADR-018) — UI sobre las plantillas Thymeleaf reales: selector "Cuenta bancaria de destino"
 * en el cobro, "Cuenta bancaria de origen" en el pago a proveedor, endpoint de cuentas activas por
 * moneda (permisos de quien registra pagos) y mensajes de validación del backend que la UI muestra.
 */
@SpringJUnitWebConfig(CuentasBancariasUiViewTest.Config.class)
@DisplayName("T-015 — selectores de cuenta bancaria y mensajes de validación (plantillas reales)")
class CuentasBancariasUiViewTest {

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

        @Bean PagoService pagoService() { return mock(PagoService.class); }
        @Bean FacturaService facturaService() { return mock(FacturaService.class); }
        @Bean ClienteService clienteService() { return mock(ClienteService.class); }
        @Bean UsuarioService usuarioService() { return mock(UsuarioService.class); }
        @Bean CuentaPorPagarService cuentaPorPagarService() { return mock(CuentaPorPagarService.class); }
        @Bean PagoProveedorService pagoProveedorService() { return mock(PagoProveedorService.class); }
        @Bean ProveedorService proveedorService() { return mock(ProveedorService.class); }
        @Bean EmpresaService empresaService() { return mock(EmpresaService.class); }
        @Bean CuentaBancariaRepository cuentaBancariaRepository() { return mock(CuentaBancariaRepository.class); }
        @Bean CuentaContableRepository cuentaContableRepository() { return mock(CuentaContableRepository.class); }
        @Bean ParametroContableRepository parametroContableRepository() { return mock(ParametroContableRepository.class); }

        /** Servicio real sobre repositorios simulados: el filtro activa + moneda es la consulta del repositorio. */
        @Bean
        CuentaBancariaService cuentaBancariaService(CuentaBancariaRepository cuentas, CuentaContableRepository contables,
                                                    ParametroContableRepository parametros, EmpresaService empresas) {
            return new CuentaBancariaService(cuentas, contables, parametros, empresas);
        }

        @Bean
        PagoController pagoController(PagoService pagos, FacturaService facturas, ClienteService clientes,
                                      UsuarioService usuarios) {
            return new PagoController(pagos, facturas, clientes, new PagoMapper(), usuarios);
        }

        @Bean
        ContabilidadViewController contabilidadViewController(CuentaPorPagarService cxp, ProveedorService proveedores) {
            return new ContabilidadViewController(cxp, proveedores);
        }

        @Bean
        CuentaPorPagarRestController cuentaPorPagarRestController(CuentaPorPagarService cxp, PagoProveedorService pagos,
                                                                  UsuarioService usuarios) {
            return new CuentaPorPagarRestController(cxp, pagos, usuarios, mock(AsientoContableService.class));
        }

        @Bean
        CuentaBancariaRestController cuentaBancariaRestController(CuentaBancariaService service) {
            return new CuentaBancariaRestController(service);
        }

        @Bean
        EmpresaRestController empresaRestController(EmpresaService empresas, UsuarioService usuarios,
                                                    CuentaBancariaService service) {
            return new EmpresaRestController(empresas, usuarios, service);
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
    @Autowired private PagoService pagoService;
    @Autowired private ClienteService clienteService;
    @Autowired private CuentaPorPagarService cuentaPorPagarService;
    @Autowired private PagoProveedorService pagoProveedorService;
    @Autowired private ProveedorService proveedorService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private EmpresaService empresaService;
    @Autowired private CuentaBancariaRepository cuentaBancariaRepository;
    @Autowired private CuentaContableRepository cuentaContableRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, pagoService, clienteService, cuentaPorPagarService, pagoProveedorService,
                proveedorService, usuarioService, empresaService, cuentaBancariaRepository, cuentaContableRepository);
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq("cajero"), eq("PAGO_CREAR"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("tesorero"), eq("CUENTA_PAGAR_VER"))).thenReturn(true);
        when(permisoService.tienePermisoPorCodigo(eq("tesorero"), eq("CUENTA_PAGAR_REGISTRAR_PAGO"))).thenReturn(true);
        when(clienteService.findAll()).thenReturn(List.of());
        when(cuentaPorPagarService.findAll()).thenReturn(List.of());
        when(proveedorService.findActivos()).thenReturn(List.of());
        Empresa empresa = new Empresa();
        empresa.setIdEmpresa(1);
        when(empresaService.getEmpresaPrincipal()).thenReturn(empresa);
        Usuario usuario = new Usuario();
        usuario.setNombre("Tesorero");
        when(usuarioService.findByEmail(anyString())).thenReturn(Optional.of(usuario));
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ── Cobro ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("formulario de cobro: selector 'Cuenta bancaria de destino' oculto por defecto; solo TRANSFERENCIA lo requiere")
    void formularioCobro_selectorDestino() throws Exception {
        mockMvc.perform(get("/pagos/nuevo").with(user("cajero")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cuenta bancaria de destino")))
                .andExpect(content().string(containsString(
                        "<div class=\"mb-3\" id=\"cuentaBancariaDestinoGroup\" style=\"display: none;\">")))
                .andExpect(content().string(containsString("id=\"idCuentaBancariaDestino\"")))
                .andExpect(content().string(containsString(
                        "data-requiere-ref=\"true\" data-requiere-cuenta-bancaria=\"true\">04 - Transferencia")))
                .andExpect(content().string(containsString(
                        "data-requiere-ref=\"false\" data-requiere-cuenta-bancaria=\"false\">01 - Efectivo")))
                .andExpect(content().string(containsString(
                        "data-requiere-ref=\"true\" data-requiere-cuenta-bancaria=\"false\">03 - Cheque")))
                .andExpect(content().string(containsString(
                        "data-requiere-ref=\"false\" data-requiere-cuenta-bancaria=\"false\">02 - Tarjeta")))
                .andExpect(content().string(containsString("Banco del cliente")))
                .andExpect(content().string(containsString("/modules/facturacion/js/pagos.js")));
    }

    @Test
    @DisplayName("cobro rechazado por el backend (TRANSFERENCIA sin cuenta): 400 con el mensaje que pagos.js muestra")
    void registrarCobro_errorDeValidacionVisible() throws Exception {
        when(pagoService.registrarPago(any(api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO.class)))
                .thenThrow(new IllegalArgumentException(
                        "El cobro por Transferencia bancaria - depósito bancario requiere la cuenta bancaria de destino"));

        mockMvc.perform(post("/pagos/registrar").with(user("cajero")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\":1,\"tipoPago\":\"ADELANTO\",\"monto\":1000,\"metodoPago\":\"TRANSFERENCIA\","
                                + "\"fechaPago\":\"2026-10-01T10:00:00\",\"estado\":\"CONFIRMADO\",\"referenciaBancaria\":\"T-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("requiere la cuenta bancaria de destino")));
    }

    @Test
    @DisplayName("cobro rechazado por una regla contable (IllegalStateException): 400 con su mensaje, no un error genérico")
    void registrarCobro_errorContableVisible() throws Exception {
        when(pagoService.registrarPago(any(api.astro.whats_orders_manager.modules.facturacion.dto.PagoDTO.class)))
                .thenThrow(new IllegalStateException("La cuenta bancaria BN de el cobro PAG-1 apunta a la cuenta "
                        + "1.1.01.002.001, que está inactiva"));

        mockMvc.perform(post("/pagos/registrar").with(user("cajero")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\":1,\"tipoPago\":\"ADELANTO\",\"monto\":1000,\"metodoPago\":\"TRANSFERENCIA\","
                                + "\"fechaPago\":\"2026-10-01T10:00:00\",\"estado\":\"CONFIRMADO\",\"referenciaBancaria\":\"T-1\","
                                + "\"idCuentaBancariaDestino\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("1.1.01.002.001")));
    }

    // ── Pago a proveedor ─────────────────────────────────────────────────────

    @Test
    @DisplayName("modal de pago a proveedor: selector 'Cuenta bancaria de origen'; TRANSFERENCIA/CHEQUE/SINPE/DEPOSITO lo requieren, EFECTIVO no")
    void modalPagoProveedor_selectorOrigen() throws Exception {
        mockMvc.perform(get("/contabilidad/cuentas-pagar").with(user("tesorero")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cuenta bancaria de origen")))
                .andExpect(content().string(containsString(
                        "<div class=\"mb-3\" id=\"pagoCuentaBancariaGroup\" style=\"display: none;\">")))
                .andExpect(content().string(containsString("id=\"pagoCuentaBancaria\"")))
                .andExpect(content().string(containsString("<option value=\"TRANSFERENCIA\" data-requiere-cuenta-bancaria=\"true\">")))
                .andExpect(content().string(containsString("<option value=\"CHEQUE\" data-requiere-cuenta-bancaria=\"true\">")))
                .andExpect(content().string(containsString("<option value=\"SINPE\" data-requiere-cuenta-bancaria=\"true\">")))
                .andExpect(content().string(containsString("<option value=\"DEPOSITO\" data-requiere-cuenta-bancaria=\"true\">")))
                .andExpect(content().string(containsString("<option value=\"EFECTIVO\" data-requiere-cuenta-bancaria=\"false\">")))
                .andExpect(content().string(containsString("/api/cuentas-bancarias/activas?moneda=")))
                .andExpect(content().string(containsString("idCuentaBancariaOrigen: cuentaOrigen")));
    }

    @Test
    @DisplayName("pago a proveedor rechazado por el backend: 400 con el mensaje que el modal muestra (data.message)")
    void registrarPagoProveedor_errorDeValidacionVisible() throws Exception {
        when(pagoProveedorService.registrarPago(any(), any())).thenThrow(new IllegalArgumentException(
                "La cuenta bancaria BAC San José - Dólares está en USD y el pago en CRC; elija una cuenta bancaria en CRC"));

        mockMvc.perform(post("/api/cuentas-pagar/5/pagar").with(user("tesorero")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":1000,\"metodoPago\":\"SINPE\",\"moneda\":\"CRC\",\"idCuentaBancariaOrigen\":4,"
                                + "\"fecha\":\"2026-10-02\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("está en USD y el pago en CRC")));
    }

    // ── Endpoint de cuentas activas por moneda ───────────────────────────────

    @Test
    @DisplayName("cuentas activas por moneda: solo las activas de la moneda pedida (consulta activa + moneda)")
    void cuentasActivas_porMoneda() throws Exception {
        when(cuentaBancariaRepository.findByEmpresaIdEmpresaAndActivaTrueAndMonedaOrderByOrdenAscIdCuentaBancariaAsc(1, "USD"))
                .thenReturn(List.of(banco(2, "Banco Nacional - Dólares", "USD", "1.1.01.002.002"),
                        banco(4, "BAC San José - Dólares", "USD", "1.1.01.002.004")));

        mockMvc.perform(get("/api/cuentas-bancarias/activas").param("moneda", "usd").with(user("cajero")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].idCuentaBancaria").value(contains(2, 4)))
                .andExpect(jsonPath("$.data[*].moneda").value(contains("USD", "USD")))
                .andExpect(jsonPath("$.data[1].codigoCuentaContable").value("1.1.01.002.004"));
        verify(cuentaBancariaRepository)
                .findByEmpresaIdEmpresaAndActivaTrueAndMonedaOrderByOrdenAscIdCuentaBancariaAsc(1, "USD");
    }

    @Test
    @DisplayName("cuentas activas: también con CUENTA_PAGAR_REGISTRAR_PAGO; sin permiso de registrar pagos → 403")
    void cuentasActivas_permisos() throws Exception {
        mockMvc.perform(get("/api/cuentas-bancarias/activas").param("moneda", "CRC").with(user("tesorero")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/cuentas-bancarias/activas").param("moneda", "CRC").with(user("lector")))
                .andExpect(status().isForbidden());
    }

    // ── Configuración → Empresa ──────────────────────────────────────────────

    @Test
    @DisplayName("Configuración → Empresa: DELETE rechazado devuelve 400 con el mensaje (pide desactivarla)")
    void eliminarCuentaBancaria_rechazoVisible() throws Exception {
        CuentaBancaria cuenta = banco(1, "Banco Nacional - Colones", "CRC", "1.1.01.002.001");
        when(cuentaBancariaRepository.findById(1)).thenReturn(Optional.of(cuenta));
        when(cuentaBancariaRepository.estaReferenciadaPorPagos(1)).thenReturn(true);

        mockMvc.perform(delete("/api/empresa/cuentas-bancarias/1").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Desactívela en su lugar")));
    }

    @Test
    @DisplayName("Configuración → Empresa: la lista incluye la cuenta contable de cada cuenta bancaria")
    void listarCuentasBancarias_incluyeCuentaContable() throws Exception {
        CuentaBancaria inactiva = banco(2, "Banco Nacional - Dólares", "USD", "1.1.01.002.002");
        inactiva.setActiva(false);
        when(cuentaBancariaRepository.findByEmpresaIdEmpresaOrderByOrdenAsc(1))
                .thenReturn(List.of(banco(1, "Banco Nacional - Colones", "CRC", "1.1.01.002.001"), inactiva));

        mockMvc.perform(get("/api/empresa/cuentas-bancarias").param("incluirInactivas", "true")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].codigoCuentaContable").value(contains("1.1.01.002.001", "1.1.01.002.002")))
                .andExpect(jsonPath("$.data[*].activa").value(contains(true, false)));
    }

    private static CuentaBancaria banco(int id, String entidad, String moneda, String codigoHoja) {
        CuentaContable hoja = new CuentaContable();
        hoja.setIdCuenta((long) id + 100);
        hoja.setCodigo(codigoHoja);
        hoja.setNombre(entidad + " " + moneda);
        CuentaBancaria b = new CuentaBancaria();
        b.setIdCuentaBancaria(id);
        b.setEntidad(entidad);
        b.setMoneda(moneda);
        b.setActiva(true);
        b.setCuentaContable(hoja);
        return b;
    }
}
