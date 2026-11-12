package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.configuracion.repository.ProvinciaCostaRicaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.seguridad.security.ApiSecurityErrorHandler;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-030 (ADR-035, P-035 parcial) — Toda escritura de la configuración de Hacienda y la subida del certificado son
 * solo para ADMIN (VENDEDOR y USER reciben 403; sin sesión, el 401 JSON de ADR-031 §2). El certificado se guarda
 * con un nombre generado por el servidor, dentro de app.certificados.directorio y sin sobrescribir.
 */
@WebMvcTest(ConfiguracionHaciendaController.class)
@Import({ConfiguracionHaciendaSeguridadApiTest.SeguridadConfig.class, ApiSecurityErrorHandler.class})
@DisplayName("T-030 — configuración de Hacienda: escritura solo ADMIN y subida segura del certificado")
class ConfiguracionHaciendaSeguridadApiTest {

    private static final String BASE = "/api/facturas/configuracion-hacienda";
    private static final String BODY = """
            {"empresaId":1,"ambiente":"SANDBOX","usuarioHacienda":"u","passwordHacienda":"p"}""";

    @TempDir
    static Path raiz;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("app.certificados.directorio", () -> raiz.resolve("certificados").toString());
        registry.add("app.certificados.tamano-maximo-bytes", () -> "1024");
    }

    /** Las mismas piezas que SecurityConfig usa para /api/**: sesión obligatoria, CSRF, @PreAuthorize y JSON. */
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

    @MockitoBean private ConfiguracionHaciendaService service;
    @MockitoBean private ConfiguracionHaciendaMapper mapper;
    @MockitoBean private UsuarioActividadService usuarioActividadService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private ProvinciaCostaRicaRepository provinciaRepository;

    @Autowired private MockMvc mockMvc;

    private Path directorio;

    @BeforeEach
    void preparar() throws IOException {
        directorio = raiz.resolve("certificados");
        if (Files.exists(directorio)) {
            try (Stream<Path> archivos = Files.list(directorio)) {
                for (Path p : archivos.toList()) {
                    Files.delete(p);
                }
            }
        }
        ConfiguracionHaciendaDTO dto = ConfiguracionHaciendaDTO.builder().id(1L).build();
        when(service.crear(any())).thenReturn(dto);
        when(service.actualizar(anyLong(), any())).thenReturn(dto);
        when(service.activar(anyLong())).thenReturn(dto);
        when(service.desactivar(anyLong())).thenReturn(dto);
        when(service.renovarToken(anyLong())).thenReturn(true);
    }

    private static MockMultipartFile p12(String nombre) {
        return new MockMultipartFile("archivo", nombre, "application/x-pkcs12", new byte[]{1, 2, 3});
    }

    static Stream<Arguments> escrituras() {
        return Stream.of(
                Arguments.of("POST /", (Escritura) () -> post(BASE).contentType(MediaType.APPLICATION_JSON).content(BODY)),
                Arguments.of("PUT /{id}", (Escritura) () -> put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content(BODY)),
                Arguments.of("PUT /{id}/activar", (Escritura) () -> put(BASE + "/1/activar")),
                Arguments.of("PUT /{id}/desactivar", (Escritura) () -> put(BASE + "/1/desactivar")),
                Arguments.of("POST /{id}/renovar-token", (Escritura) () -> post(BASE + "/1/renovar-token")),
                Arguments.of("DELETE /{id}", (Escritura) () -> delete(BASE + "/1")),
                Arguments.of("POST /upload-certificado",
                        (Escritura) () -> multipart(BASE + "/upload-certificado").file(p12("cert.p12"))));
    }

    @FunctionalInterface
    interface Escritura {
        MockHttpServletRequestBuilder crear();
    }

    private List<Path> archivosGuardados() throws IOException {
        if (!Files.exists(directorio)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(directorio)) {
            return archivos.toList();
        }
    }

    @ParameterizedTest(name = "{0}: VENDEDOR y USER → 403, sin tocar el servicio ni el disco")
    @MethodSource("escrituras")
    void sinAdmin_403(String nombre, Escritura escritura) throws Exception {
        mockMvc.perform(escritura.crear().with(csrf()).with(user("vendedor").roles("VENDEDOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(escritura.crear().with(csrf()).with(user("usuario").roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
        assertThat(archivosGuardados()).isEmpty();
    }

    @ParameterizedTest(name = "{0}: ADMIN → 2xx")
    @MethodSource("escrituras")
    void admin_2xx(String nombre, Escritura escritura) throws Exception {
        mockMvc.perform(escritura.crear().with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest(name = "{0}: sin sesión → 401 JSON")
    @MethodSource("escrituras")
    void sinSesion_401(String nombre, Escritura escritura) throws Exception {
        mockMvc.perform(escritura.crear().with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(service);
    }

    private String subir(MockMultipartFile archivo) throws Exception {
        return mockMvc.perform(multipart(BASE + "/upload-certificado").file(archivo).param("ambiente", "SANDBOX")
                        .with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("«../x.p12» y un nombre absoluto se guardan dentro del directorio con nombre del servidor")
    void nombreDelCliente_noConstruyeLaRuta() throws Exception {
        Path absoluto = raiz.resolve("absoluto.p12").toAbsolutePath();

        subir(p12("../x.p12"));
        subir(p12(absoluto.toString()));

        assertThat(raiz.resolve("x.p12")).doesNotExist();
        assertThat(absoluto).doesNotExist();
        List<Path> guardados = archivosGuardados();
        assertThat(guardados).hasSize(2).allSatisfy(p -> {
            assertThat(p.getParent()).isEqualTo(directorio);
            assertThat(p.getFileName().toString()).startsWith("sandbox-").endsWith(".p12")
                    .doesNotContain("x.p12").doesNotContain("absoluto");
        });
        try (Stream<Path> todo = Files.walk(raiz)) {
            assertThat(todo.filter(Files::isRegularFile).toList()).containsExactlyInAnyOrderElementsOf(guardados);
        }
    }

    @Test
    @DisplayName("Dos subidas con el mismo nombre generan archivos distintos y la primera sigue existiendo")
    void dosSubidas_noSobrescriben() throws Exception {
        String primera = subir(p12("cert.p12"));
        String segunda = subir(p12("cert.p12"));

        assertThat(primera).isNotEqualTo(segunda);
        List<Path> guardados = archivosGuardados();
        assertThat(guardados).hasSize(2);
        for (Path p : guardados) {
            assertThat(Files.readAllBytes(p)).containsExactly(1, 2, 3);
        }
    }

    @Test
    @DisplayName(".pfx en mayúsculas se acepta y conserva la extensión .pfx")
    void pfx_aceptado() throws Exception {
        subir(p12("CERT.PFX"));

        assertThat(archivosGuardados()).singleElement()
                .satisfies(p -> assertThat(p.getFileName().toString()).endsWith(".pfx"));
    }

    @Test
    @DisplayName("Extensión no permitida, archivo vacío o demasiado grande → 400 sin escribir")
    void archivoInvalido_400() throws Exception {
        for (MockMultipartFile archivo : List.of(
                p12("cert.pem"),
                p12("cert.p12.exe"),
                new MockMultipartFile("archivo", "vacio.p12", "application/x-pkcs12", new byte[0]),
                new MockMultipartFile("archivo", "grande.p12", "application/x-pkcs12", new byte[1025]))) {
            mockMvc.perform(multipart(BASE + "/upload-certificado").file(archivo)
                            .with(csrf()).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        assertThat(archivosGuardados()).isEmpty();
    }
}
