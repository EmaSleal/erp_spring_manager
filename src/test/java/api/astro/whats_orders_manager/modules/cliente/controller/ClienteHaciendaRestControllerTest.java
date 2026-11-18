package api.astro.whats_orders_manager.modules.cliente.controller;

import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO.ResultadoConsulta;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-038 criterios 12 y 13 (E): «Verificar en Hacienda» con servicio simulado y permisos CLIENTE_CREAR/CLIENTE_EDITAR. */
@SpringJUnitWebConfig(ClienteHaciendaRestControllerTest.Config.class)
@DisplayName("T-038 — verificar identificación en Hacienda desde el cliente")
class ClienteHaciendaRestControllerTest {

    private static final String URL = "/api/clientes/hacienda/contribuyente/{n}";

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class Config {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean PermisoService permisoService() { return mock(PermisoService.class); }

        @Bean HaciendaApiService haciendaApiService() { return mock(HaciendaApiService.class); }

        @Bean
        ClienteHaciendaRestController controller(HaciendaApiService hacienda) {
            return new ClienteHaciendaRestController(hacienda);
        }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private PermisoService permisoService;
    @Autowired private HaciendaApiService hacienda;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, hacienda);
        when(permisoService.tieneAlgunPermisoPorCodigo(eq("editor"), eq("CLIENTE_CREAR"), eq("CLIENTE_EDITAR")))
                .thenReturn(true);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static HaciendaConsultaDTO exitosa(String nombre, String tipo) {
        return HaciendaConsultaDTO.builder().exitosa(true).resultado(ResultadoConsulta.EXITOSA)
                .nombre(nombre).tipoIdentificacion(tipo).build();
    }

    @Test
    @DisplayName("200 con nombre y tipo (sin ADMIN), mapeo 01 a CEDULA_FISICA; no guarda nada")
    void existe() throws Exception {
        when(hacienda.consultarContribuyente("109990999")).thenReturn(exitosa("JUAN PEREZ", "01"));
        mockMvc.perform(get(URL, "1-0999-0999").with(user("editor").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.nombre").value("JUAN PEREZ"))
                .andExpect(jsonPath("$.tipoIdentificacion").value("01"))
                .andExpect(jsonPath("$.tipoIdentificacionEnum").value("CEDULA_FISICA"));
    }

    private static api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaActividadDTO act(
            String codigo, String desc, String estado, String tipo) {
        return api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaActividadDTO.builder()
                .codigo(codigo).descripcion(desc).estado(estado).tipo(tipo).build();
    }

    @Test
    @DisplayName("sin actividades (null o vacía): actividades es una lista vacía")
    void sinActividades() throws Exception {
        when(hacienda.consultarContribuyente("109990999")).thenReturn(exitosa("JUAN PEREZ", "01"));
        mockMvc.perform(get(URL, "109990999").with(user("editor").roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.actividades").isArray())
                .andExpect(jsonPath("$.actividades.length()").value(0));
        HaciendaConsultaDTO vacia = exitosa("JUAN PEREZ", "01");
        vacia.setActividades(java.util.List.of());
        when(hacienda.consultarContribuyente("109990999")).thenReturn(vacia);
        mockMvc.perform(get(URL, "109990999").with(user("editor").roles("USER")))
                .andExpect(jsonPath("$.actividades.length()").value(0));
    }

    @Test
    @DisplayName("una actividad: código, descripción, estado y tipo tal como los da /fe/ae")
    void unaActividad() throws Exception {
        HaciendaConsultaDTO c = exitosa("JUAN PEREZ", "01");
        c.setActividades(java.util.List.of(act("4773.0", "Venta de productos", "A", "P")));
        when(hacienda.consultarContribuyente("109990999")).thenReturn(c);
        mockMvc.perform(get(URL, "109990999").with(user("editor").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actividades.length()").value(1))
                .andExpect(jsonPath("$.actividades[0].codigo").value("4773.0"))
                .andExpect(jsonPath("$.actividades[0].descripcion").value("Venta de productos"))
                .andExpect(jsonPath("$.actividades[0].estado").value("A"))
                .andExpect(jsonPath("$.actividades[0].tipo").value("P"));
    }

    @Test
    @DisplayName("varias actividades: todas, en el orden de Hacienda")
    void variasActividades() throws Exception {
        HaciendaConsultaDTO c = exitosa("JUAN PEREZ", "01");
        c.setActividades(java.util.List.of(act("4773.0", "Uno", "A", "P"), act("620100", "Dos", "I", "S"),
                act("4759.0", "Tres", "A", "S")));
        when(hacienda.consultarContribuyente("109990999")).thenReturn(c);
        mockMvc.perform(get(URL, "109990999").with(user("editor").roles("USER")))
                .andExpect(jsonPath("$.actividades.length()").value(3))
                .andExpect(jsonPath("$.actividades[1].codigo").value("620100"))
                .andExpect(jsonPath("$.actividades[1].estado").value("I"))
                .andExpect(jsonPath("$.actividades[2].tipo").value("S"));
    }

    @Test
    @DisplayName("tipos 02, 03 y 04 se mapean; otro código (05) da existe=true con tipoIdentificacionEnum null")
    void mapeoDeTipos() {
        org.junit.jupiter.api.Assertions.assertEquals("CEDULA_JURIDICA", ClienteHaciendaRestController.tipoEnum("02"));
        org.junit.jupiter.api.Assertions.assertEquals("DIMEX", ClienteHaciendaRestController.tipoEnum("03"));
        org.junit.jupiter.api.Assertions.assertEquals("NITE", ClienteHaciendaRestController.tipoEnum("04"));
        org.junit.jupiter.api.Assertions.assertNull(ClienteHaciendaRestController.tipoEnum("05"));
        org.junit.jupiter.api.Assertions.assertNull(ClienteHaciendaRestController.tipoEnum(null));
    }

    @Test
    @DisplayName("tipo desconocido: 200 existe=true y tipoIdentificacionEnum null")
    void tipoDesconocido() throws Exception {
        when(hacienda.consultarContribuyente("109990999")).thenReturn(exitosa("EXTRANJERO", "05"));
        mockMvc.perform(get(URL, "109990999").with(user("editor").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.tipoIdentificacionEnum").doesNotExist());
    }

    @Test
    @DisplayName("identificación inexistente: 200 con existe=false y mensaje fijo")
    void noExiste() throws Exception {
        when(hacienda.consultarContribuyente("109990999")).thenReturn(HaciendaConsultaDTO.builder()
                .exitosa(false).resultado(ResultadoConsulta.NO_ENCONTRADA).mensajeError("texto-interno").build());
        MvcResult r = mockMvc.perform(get(URL, "109990999").with(user("editor").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false))
                .andExpect(jsonPath("$.mensaje").value(ClienteHaciendaRestController.MSG_NO_EXISTE))
                .andReturn();
        assertFalse(r.getResponse().getContentAsString().contains("texto-interno"));
    }

    @Test
    @DisplayName("la API de Hacienda falla: 502 con mensaje fijo, sin el texto del error")
    void apiCaida() throws Exception {
        when(hacienda.consultarContribuyente("109990999")).thenReturn(HaciendaConsultaDTO.builder()
                .exitosa(false).resultado(ResultadoConsulta.ERROR).mensajeError("Error al conectar: detalle-interno").build());
        MvcResult r = mockMvc.perform(get(URL, "109990999").with(user("editor").roles("USER")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.mensaje").value(ClienteHaciendaRestController.MSG_HACIENDA))
                .andReturn();
        assertFalse(r.getResponse().getContentAsString().contains("detalle-interno"));
    }

    @Test
    @DisplayName("el servicio lanza una excepción: 502 con mensaje fijo")
    void servicioLanza() throws Exception {
        when(hacienda.consultarContribuyente(any())).thenThrow(new IllegalStateException("boom-interno"));
        MvcResult r = mockMvc.perform(get(URL, "109990999").with(user("editor").roles("USER")))
                .andExpect(status().isBadGateway()).andReturn();
        assertFalse(r.getResponse().getContentAsString().contains("boom-interno"));
    }

    @Test
    @DisplayName("sin CLIENTE_CREAR ni CLIENTE_EDITAR: 403 aunque sea ADMIN, y no se consulta")
    void sinPermiso_403() throws Exception {
        mockMvc.perform(get(URL, "109990999").with(user("otro").roles("ADMIN")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(hacienda);
    }

    @Test
    @DisplayName("identificación con letras o fuera de 9 a 12 dígitos: 400 sin consultar")
    void identificacionInvalida() throws Exception {
        mockMvc.perform(get(URL, "abc").with(user("editor").roles("USER"))).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL, "1234").with(user("editor").roles("USER"))).andExpect(status().isBadRequest());
        verifyNoInteractions(hacienda);
    }
}
