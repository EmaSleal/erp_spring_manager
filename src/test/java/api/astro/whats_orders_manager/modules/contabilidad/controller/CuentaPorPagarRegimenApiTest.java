package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.politicas.PoliticaIvaCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.RegimenHacienda;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.service.PagoProveedorService;
import api.astro.whats_orders_manager.modules.proveedores.service.ProveedorService;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.seguridad.security.ApiSecurityErrorHandler;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-058 criterios 1, 2 y 7 a nivel del controlador: IVA trasladado > 0 con proveedor SIMPLIFICADO → 400; fecha sin tramo → 409
 * (RegimenNoDefinidoException es una IllegalStateException: no debe caer en el 400 genérico); aceptación sobre NO_APLICA → 409;
 * y el endpoint de solo lectura de la política que usa la pantalla.
 */
@WebMvcTest(CuentaPorPagarRestController.class)
@Import({CuentaPorPagarRegimenApiTest.SeguridadConfig.class, ApiSecurityErrorHandler.class})
@DisplayName("T-058 — API de CxP: 400, 409 y endpoint de política de IVA por fecha")
class CuentaPorPagarRegimenApiTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class SeguridadConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http, ApiSecurityErrorHandler api) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .exceptionHandling(e -> e.accessDeniedHandler(api).authenticationEntryPoint(api));
            return http.build();
        }
    }

    private static final String JSON = """
            {"proveedorId":1,"descripcion":"x","numeroComprobanteProveedor":"F-1","fechaComprobante":"2027-02-01",
             "lineas":[{"cuentaContableId":1,"codigoTarifa":"08","base":1000,"iva":130,"tratamiento":"NO_ACREDITABLE"}]}""";

    @MockitoBean private CuentaPorPagarService cuentaPorPagarService;
    @MockitoBean private PagoProveedorService pagoProveedorService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private AsientoContableService asientoContableService;
    @MockitoBean private ProveedorService proveedorService;
    @MockitoBean private PoliticaIvaCompra politicaIvaCompra;
    @MockitoBean(name = "permisoService") private PermisoService permisoService;
    @MockitoBean private UsuarioActividadService usuarioActividadService;
    @MockitoBean private api.astro.whats_orders_manager.modules.configuracion.repository.ProvinciaCostaRicaRepository provinciaRepository;

    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("IVA > 0 con proveedor SIMPLIFICADO → 400 con el mensaje fijo")
    void ivaConProveedorSimplificado_400() throws Exception {
        when(cuentaPorPagarService.crearConAdvertencias(any()))
                .thenThrow(new IllegalArgumentException(PoliticaIvaCompra.MSG_PROVEEDOR_SIN_IVA));
        mockMvc.perform(post("/api/cuentas-pagar").contentType(MediaType.APPLICATION_JSON).content(JSON)
                        .with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PoliticaIvaCompra.MSG_PROVEEDOR_SIN_IVA));
    }

    @Test
    @DisplayName("fecha sin tramo de régimen → 409, no 400")
    void sinTramo_409() throws Exception {
        when(cuentaPorPagarService.crearConAdvertencias(any()))
                .thenThrow(new RegimenNoDefinidoException(LocalDate.of(1999, 1, 1)));
        mockMvc.perform(post("/api/cuentas-pagar").contentType(MediaType.APPLICATION_JSON).content(JSON)
                        .with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No hay un régimen tributario definido para la fecha 1999-01-01"));
    }

    @Test
    @DisplayName("GET politica-iva en SIMPLIFICADO: régimen, tratamientos admitidos, tratamiento fijo y mensaje")
    void politicaIva_simplificado() throws Exception {
        var regla = new PoliticaIvaCompra.ReglaIvaCompra(Regimen.SIMPLIFICADO, List.of(TratamientoIvaCompra.NO_ACREDITABLE),
                TratamientoIvaCompra.NO_ACREDITABLE, false, "Régimen simplificado: sin crédito de IVA");
        when(politicaIvaCompra.reglaPara(LocalDate.of(2027, 2, 1), RegimenHacienda.SIMPLIFICADO)).thenReturn(regla);
        when(proveedorService.findById(5L)).thenReturn(Proveedor.builder().id(5L).regimenHacienda(RegimenHacienda.SIMPLIFICADO).build());
        mockMvc.perform(get("/api/cuentas-pagar/politica-iva").param("fecha", "2027-02-01").param("proveedorId", "5")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.regimen").value("SIMPLIFICADO"))
                .andExpect(jsonPath("$.data.tratamientosAdmitidos[0]").value("NO_ACREDITABLE"))
                .andExpect(jsonPath("$.data.tratamientoFijo").value("NO_ACREDITABLE"))
                .andExpect(jsonPath("$.data.mensaje").value("Régimen simplificado: sin crédito de IVA"));
    }

    @Test
    @DisplayName("GET politica-iva sin tramo → 409 con el mensaje de la excepción; proveedor inexistente → 404")
    void politicaIva_errores() throws Exception {
        when(politicaIvaCompra.reglaPara(LocalDate.of(1999, 1, 1), null))
                .thenThrow(new RegimenNoDefinidoException(LocalDate.of(1999, 1, 1)));
        mockMvc.perform(get("/api/cuentas-pagar/politica-iva").param("fecha", "1999-01-01")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No hay un régimen tributario definido para la fecha 1999-01-01"));
        when(proveedorService.findById(9L)).thenThrow(new NoSuchElementException("x"));
        mockMvc.perform(get("/api/cuentas-pagar/politica-iva").param("fecha", "2027-02-01").param("proveedorId", "9")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}
