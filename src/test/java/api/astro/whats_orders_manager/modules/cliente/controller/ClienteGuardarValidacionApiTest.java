package api.astro.whats_orders_manager.modules.cliente.controller;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.cliente.service.impl.ClienteServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionDePrueba;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-038 criterios 4 y 5 (B): POST /clientes/guardar valida los datos FE del cliente con el servicio real y el validador
 * único; con datos inválidos responde 400 por campo y no guarda nada.
 */
@SpringJUnitWebConfig(ClienteGuardarValidacionApiTest.Config.class)
@DisplayName("T-038 — guardado del cliente con validación de datos FE")
class ClienteGuardarValidacionApiTest {

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

        @Bean ClienteRepository clienteRepository() { return mock(ClienteRepository.class); }

        @Bean
        ClienteService clienteService(ClienteRepository repo) {
            return new ClienteServiceImpl(repo, CatalogoUbicacionDePrueba.validador());
        }

        @Bean
        ClienteController clienteController(ClienteService servicio) {
            return new ClienteController(servicio);
        }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private PermisoService permisoService;
    @Autowired private ClienteRepository repo;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(permisoService, repo);
        when(permisoService.tieneAlgunPermisoPorCodigo(eq("editor"), eq("CLIENTE_CREAR"), eq("CLIENTE_EDITAR")))
                .thenReturn(true);
        when(repo.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private MvcResult guardar(String json, int estado) throws Exception {
        return mockMvc.perform(post("/clientes/guardar").with(user("editor").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(estado)).andReturn();
    }

    @Test
    @DisplayName("FE activa con datos inválidos: 400 con campo y mensaje por problema y nada guardado")
    void feInvalido_400SinGuardar() throws Exception {
        String json = """
            {"nombre":"Cl","requiereFacturaElectronica":true,"tipoIdentificacion":"CEDULA_FISICA",
             "numeroIdentificacion":"12345678","telefono":"0000000000","email":"sin-arroba",
             "canton":"01","distrito":"01","otrasSenas":"abc"}""";
        MvcResult r = mockMvc.perform(post("/clientes/guardar").with(user("editor").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.errores[?(@.campo=='nombre')].mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores[?(@.campo=='numeroIdentificacion')].mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores[?(@.campo=='telefono')].mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores[?(@.campo=='email')].mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores[?(@.campo=='provincia')].mensaje").isNotEmpty())
                .andExpect(jsonPath("$.errores[?(@.campo=='otrasSenas')].mensaje").isNotEmpty())
                .andReturn();
        verify(repo, never()).save(any());
        // los mensajes son fijos: no repiten el valor que escribió el usuario
        assertFalse(r.getResponse().getContentAsString().contains("sin-arroba"));
    }

    @Test
    @DisplayName("requiereFacturaElectronica ausente (null) se valida como activa")
    void feNull_seValida() throws Exception {
        guardar("{\"nombre\":\"Cliente Sin Datos\"}", 400);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("FE activa con datos válidos: 200 y se guarda")
    void feValido_200() throws Exception {
        guardar("""
            {"nombre":"Cliente Válido","requiereFacturaElectronica":true,"tipoIdentificacion":"CEDULA_FISICA",
             "numeroIdentificacion":"109990999","telefono":"88887777","email":"cliente@correo.example",
             "provincia":{"codigo":"1"},"canton":"01","distrito":"01","otrasSenas":"Frente al parque central"}""", 200);
        verify(repo).save(any(Cliente.class));
    }

    @Test
    @DisplayName("cliente sin FE con datos que no pasarían las reglas FE: 200 y se guarda")
    void sinFe_noSeValida() throws Exception {
        guardar("""
            {"nombre":"X","requiereFacturaElectronica":false,"telefono":"0000000000","email":"mal","otrasSenas":"a"}""", 200);
        verify(repo).save(any(Cliente.class));
    }

    @Test
    @DisplayName("IllegalArgumentException del servicio: 400 con mensaje fijo, sin e.getMessage()")
    void illegalArgument_mensajeFijo() throws Exception {
        when(repo.save(any(Cliente.class))).thenThrow(new IllegalArgumentException("detalle-interno-secreto"));
        MvcResult r = guardar("""
            {"nombre":"Cliente","requiereFacturaElectronica":false}""", 400);
        assertFalse(r.getResponse().getContentAsString().contains("detalle-interno-secreto"));
    }

    @Test
    @DisplayName("sin CLIENTE_CREAR ni CLIENTE_EDITAR: 403 y no se guarda")
    void sinPermiso_403() throws Exception {
        mockMvc.perform(post("/clientes/guardar").with(user("otro").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cliente\"}"))
                .andExpect(status().isForbidden());
        verify(repo, never()).save(any());
    }
}
