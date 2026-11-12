package api.astro.whats_orders_manager.modules.proveedores.controller;

import api.astro.whats_orders_manager.modules.configuracion.repository.ProvinciaCostaRicaRepository;
import api.astro.whats_orders_manager.modules.proveedores.exception.CedulaProveedorDuplicadaException;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.service.ProveedorService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-030 (ADR-035 §3) — La API de proveedores no devuelve textos de excepción ni SQL: validación por campo (400),
 * mensajes fijos para duplicado, no encontrado y conflicto de BD, 500 genérico para lo inesperado, y los rechazos
 * de @PreAuthorize siguen siendo 403.
 */
@WebMvcTest(ProveedorRestController.class)
@Import({ProveedorRestControllerErroresApiTest.SeguridadConfig.class, ApiSecurityErrorHandler.class})
@DisplayName("T-030 — API de proveedores sin fugas de excepciones")
class ProveedorRestControllerErroresApiTest {

    private static final String URL = "/api/proveedores";
    private static final String SQL = "could not execute statement [insert into proveedores (cedula,nombre) values (?,?)]"
            + " SQL [n/a]; Column 'cedula' cannot be null";
    private static final String VALIDO = """
            {"cedula":"3101123456","nombre":"Proveedor SA","tipoProveedor":"NACIONAL"}""";

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

    @MockitoBean private ProveedorService proveedorService;
    @MockitoBean(name = "permisoService") private PermisoService permisoService;
    @MockitoBean private UsuarioActividadService usuarioActividadService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private ProvinciaCostaRicaRepository provinciaRepository;

    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(true);
    }

    private ResultActions crear(String json) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json)
                .with(csrf()).with(user("admin").roles("ADMIN")));
    }

    private static void sinFugas(String cuerpo) {
        assertThat(cuerpo.toLowerCase())
                .doesNotContain("sql", "insert", "column", "proveedores", "exception", "could not");
    }

    @Test
    @DisplayName("POST sin cédula → 400 con mensaje legible por campo, sin SQL ni nombres de tabla")
    void sinCedula_400PorCampo() throws Exception {
        String cuerpo = crear("{\"nombre\":\"Proveedor SA\",\"tipoProveedor\":\"NACIONAL\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("La cédula es obligatoria"))
                .andExpect(jsonPath("$.data.cedula").value("La cédula es obligatoria"))
                .andReturn().getResponse().getContentAsString();

        sinFugas(cuerpo);
        verifyNoInteractions(proveedorService);
    }

    @Test
    @DisplayName("Varios campos inválidos → un mensaje por campo")
    void variosCampos_400() throws Exception {
        crear("{\"cedula\":\"\",\"nombre\":\"\",\"telefono\":\"1234567890123456\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.cedula").exists())
                .andExpect(jsonPath("$.data.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.data.tipoProveedor").value("El tipo de proveedor es obligatorio"))
                .andExpect(jsonPath("$.data.telefono").exists());
    }

    @Test
    @DisplayName("JSON ilegible o enum desconocido → 400 sin el detalle de Jackson")
    void formatoInvalido_400() throws Exception {
        for (String json : new String[]{"{\"cedula\":", "{\"cedula\":\"1\",\"nombre\":\"x\",\"tipoProveedor\":\"OTRO\"}"}) {
            String cuerpo = crear(json).andExpect(status().isBadRequest())
                    .andReturn().getResponse().getContentAsString();
            assertThat(cuerpo).doesNotContain("JSON", "Cannot", "TipoProveedor", "api.astro");
        }
    }

    @Test
    @DisplayName("Cédula duplicada → 400 con mensaje fijo")
    void duplicada_400() throws Exception {
        when(proveedorService.crear(any())).thenThrow(new CedulaProveedorDuplicadaException("3101123456"));

        crear(VALIDO).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ProveedorRestController.MENSAJE_DUPLICADO));
    }

    @Test
    @DisplayName("Violación de restricción en la BD → 409 sin el texto SQL")
    void restriccionBd_409() throws Exception {
        when(proveedorService.crear(any())).thenThrow(new DataIntegrityViolationException(SQL));

        sinFugas(crear(VALIDO).andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    @DisplayName("Error inesperado → 500 genérico, sin el mensaje de la excepción")
    void inesperado_500() throws Exception {
        when(proveedorService.crear(any())).thenThrow(new IllegalStateException(SQL));

        String cuerpo = crear(VALIDO).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(ProveedorRestController.MENSAJE_INESPERADO))
                .andReturn().getResponse().getContentAsString();
        sinFugas(cuerpo);
    }

    @Test
    @DisplayName("GET de un id inexistente → 404 con mensaje fijo; id no numérico → 400")
    void noEncontrado_404() throws Exception {
        when(proveedorService.findById(99L)).thenThrow(new NoSuchElementException("Proveedor no encontrado: 99"));

        mockMvc.perform(get(URL + "/99").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ProveedorRestController.MENSAJE_NO_ENCONTRADO));
        mockMvc.perform(get(URL + "/abc").with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sin permiso → 403 de Spring Security, no un 500 del manejador genérico")
    void sinPermiso_403() throws Exception {
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);

        crear(VALIDO).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        verifyNoInteractions(proveedorService);
    }

    @Test
    @DisplayName("POST válido → 200")
    void valido_200() throws Exception {
        when(proveedorService.crear(any())).thenReturn(Proveedor.builder().cedula("3101123456").build());

        crear(VALIDO).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }
}
