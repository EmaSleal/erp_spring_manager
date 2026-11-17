package api.astro.whats_orders_manager.modules.producto.controller;

import api.astro.whats_orders_manager.shared.exception.ValidacionException;
import api.astro.whats_orders_manager.modules.configuracion.dto.CabysBusquedaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.CabysDTO;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.configuracion.service.PresentacionService;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.service.ArticuloMaestroService;
import api.astro.whats_orders_manager.modules.producto.service.ProductoService;
import api.astro.whats_orders_manager.modules.producto.service.impl.ArticuloMaestroServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-034 criterios 3, 4 y 6 — endpoint de búsqueda CAByS (permisos PRODUCTO_CREAR/PRODUCTO_EDITAR,
 * sin exigir ADMIN, mensajes fijos) y rechazo 400 del CAByS inválido al guardar el artículo maestro.
 */
@SpringJUnitWebConfig(CabysBusquedaSeguridadApiTest.Config.class)
@DisplayName("Búsqueda CAByS y validación del artículo maestro")
class CabysBusquedaSeguridadApiTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class Config {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .csrf(csrf -> csrf.disable());
            return http.build();
        }

        @Bean
        PermisoService permisoService() {
            return mock(PermisoService.class);
        }

        @Bean
        HaciendaApiService haciendaApiService() {
            return mock(HaciendaApiService.class);
        }

        @Bean
        ArticuloMaestroService articuloMaestroService() {
            return mock(ArticuloMaestroService.class);
        }

        @Bean
        CabysBusquedaRestController cabysBusquedaRestController(HaciendaApiService hacienda) {
            return new CabysBusquedaRestController(hacienda);
        }

        @Bean
        ArticuloMaestroRestController articuloMaestroRestController(ArticuloMaestroService servicio) {
            return new ArticuloMaestroRestController(servicio, mock(ProductoService.class), mock(PresentacionService.class));
        }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private PermisoService permisoService;
    @Autowired private HaciendaApiService hacienda;
    @Autowired private ArticuloMaestroService articuloService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, hacienda, articuloService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private void concederPermisos(String username) {
        when(permisoService.tieneAlgunPermisoPorCodigo(eq(username), eq("PRODUCTO_CREAR"), eq("PRODUCTO_EDITAR")))
                .thenReturn(true);
    }

    @Test
    @DisplayName("usuario con PRODUCTO_EDITAR y sin rol ADMIN obtiene 200 con resultados")
    void conPermisoSinAdmin_200() throws Exception {
        concederPermisos("editor");
        CabysDTO item = CabysDTO.builder().codigo("2132100000100").descripcion("Jugo").impuesto(13)
                .categorias(List.of("A", "B")).build();
        when(hacienda.buscarCabys("jugo", 10)).thenReturn(CabysBusquedaDTO.builder()
                .exitosa(true).total(1).cantidad(1).cabys(List.of(item)).build());

        mockMvc.perform(get("/api/articulos/cabys/buscar").param("q", "jugo").with(user("editor").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].codigo").value("2132100000100"))
                .andExpect(jsonPath("$.data[0].impuesto").value(13));
    }

    @Test
    @DisplayName("usuario sin PRODUCTO_CREAR ni PRODUCTO_EDITAR obtiene 403 aunque sea ADMIN o VENDEDOR")
    void sinPermiso_403() throws Exception {
        when(permisoService.tieneAlgunPermisoPorCodigo(any(), any(String[].class))).thenReturn(false);

        mockMvc.perform(get("/api/articulos/cabys/buscar").param("q", "jugo").with(user("x").roles("ADMIN", "VENDEDOR")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(hacienda);
    }

    @Test
    @DisplayName("sin resultados responde 200 con lista vacía")
    void sinResultados_listaVacia() throws Exception {
        concederPermisos("editor");
        when(hacienda.buscarCabys("zzz", 10)).thenReturn(CabysBusquedaDTO.builder()
                .exitosa(false).mensajeError("No se encontraron códigos CABYS").build());

        mockMvc.perform(get("/api/articulos/cabys/buscar").param("q", "zzz").with(user("editor").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("término en blanco responde 400 con mensaje fijo")
    void qEnBlanco_400() throws Exception {
        concederPermisos("editor");
        mockMvc.perform(get("/api/articulos/cabys/buscar").param("q", "  ").with(user("editor").roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(CabysBusquedaRestController.MSG_Q_REQUERIDO));
    }

    @Test
    @DisplayName("Hacienda no responde: 502 con mensaje fijo, sin el texto de la excepción")
    void haciendaFalla_502MensajeFijo() throws Exception {
        concederPermisos("editor");
        when(hacienda.buscarCabys("jugo", 10)).thenReturn(CabysBusquedaDTO.builder()
                .exitosa(false).fallaServicio(true).mensajeError("detalle-interno-secreto").build());

        MvcResult r = mockMvc.perform(get("/api/articulos/cabys/buscar").param("q", "jugo").with(user("editor").roles("USER")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(CabysBusquedaRestController.MSG_HACIENDA))
                .andReturn();
        assertFalse(r.getResponse().getContentAsString().contains("detalle-interno-secreto"));
    }

    @Test
    @DisplayName("excepción inesperada: 500 con mensaje fijo, sin e.getMessage()")
    void errorInesperado_500MensajeFijo() throws Exception {
        concederPermisos("editor");
        when(hacienda.buscarCabys("jugo", 10)).thenThrow(new IllegalStateException("boom-interno"));

        MvcResult r = mockMvc.perform(get("/api/articulos/cabys/buscar").param("q", "jugo").with(user("editor").roles("USER")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(CabysBusquedaRestController.MSG_ERROR))
                .andReturn();
        assertFalse(r.getResponse().getContentAsString().contains("boom-interno"));
    }

    @Test
    @DisplayName("P-070: una IllegalArgumentException cualquiera al guardar responde 500 fijo, sin su texto")
    void guardarIllegalArgumentGenerica_500Fijo() throws Exception {
        when(articuloService.save(any(ArticuloMaestro.class))).thenThrow(new IllegalArgumentException("detalle-interno-spring"));

        for (var req : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[]{
                post("/api/articulos"), org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/articulos/3")}) {
            MvcResult r = mockMvc.perform(req.with(user("v").roles("VENDEDOR"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"descripcion\":\"X\"}"))
                    .andExpect(status().isInternalServerError()).andReturn();
            assertFalse(r.getResponse().getContentAsString().contains("detalle-interno-spring"));
        }
    }

    @Test
    @DisplayName("guardar artículo con CAByS inválido responde 400 con el mensaje fijo")
    void guardarCabysInvalido_400() throws Exception {
        when(articuloService.save(any(ArticuloMaestro.class)))
                .thenThrow(new ValidacionException(ArticuloMaestroServiceImpl.MSG_CABYS_INVALIDO));

        mockMvc.perform(post("/api/articulos").with(user("v").roles("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"X\",\"codigoCabys\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ArticuloMaestroServiceImpl.MSG_CABYS_INVALIDO));
    }
}
