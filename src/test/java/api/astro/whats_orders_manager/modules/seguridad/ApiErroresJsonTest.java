package api.astro.whats_orders_manager.modules.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * T-026 (ADR-031 §2): para /api/** y peticiones AJAX (Accept: application/json o X-Requested-With: XMLHttpRequest),
 * un rechazo por CSRF o por permisos responde 403 en JSON {status, error, message}, y la falta de sesión 401 en JSON.
 * Las rutas de páginas conservan su comportamiento (P-008 sigue aparte): 403 reenviado a /error/403 y redirect al
 * login sin sesión.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("T-026 — 403/401 en JSON para la API y AJAX; páginas sin cambios")
class ApiErroresJsonTest {

    private static final String MENSAJE_CSRF = "Sesión vencida o token de seguridad inválido; recargue la página";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST a /api/** sin token CSRF → 403 JSON con el mensaje de CSRF")
    void api_sinCsrf_403Json() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/facturas/electronica/comprobantes/1/enviar")
                .with(user("admin.bootstrap@example.invalid").roles("ADMIN"))).andReturn();

        assertJson(r, 403, "Forbidden", MENSAJE_CSRF);
    }

    @Test
    @DisplayName("POST con Accept: application/json a una ruta que no es /api, sin token → 403 JSON")
    void ajaxAccept_sinCsrf_403Json() throws Exception {
        MvcResult r = mockMvc.perform(post("/facturas/guardar")
                .with(user("admin.bootstrap@example.invalid").roles("ADMIN"))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();

        assertJson(r, 403, "Forbidden", MENSAJE_CSRF);
    }

    @Test
    @DisplayName("POST con X-Requested-With: XMLHttpRequest a una ruta que no es /api, sin token → 403 JSON")
    void ajaxXRequestedWith_sinCsrf_403Json() throws Exception {
        MvcResult r = mockMvc.perform(post("/admin/roles/cambiar-estado/1").param("activo", "false")
                .with(user("admin.bootstrap@example.invalid").roles("ADMIN"))
                .header("X-Requested-With", "XMLHttpRequest")).andReturn();

        assertJson(r, 403, "Forbidden", MENSAJE_CSRF);
    }

    @Test
    @DisplayName("ruta de página sin token (POST con Accept text/html) → comportamiento previo: 403 reenviado a /error/403")
    void pagina_sinCsrf_comportamientoPrevio() throws Exception {
        MvcResult r = mockMvc.perform(post("/facturas/anular/1").param("motivo", "x")
                .with(user("admin.bootstrap@example.invalid").roles("ADMIN"))
                .accept(MediaType.TEXT_HTML)).andReturn();

        assertThat(r.getResponse().getStatus()).isEqualTo(403);
        assertThat(r.getResponse().getForwardedUrl()).isEqualTo("/error/403");
        assertThat(r.getResponse().getContentAsString()).doesNotContain(MENSAJE_CSRF);
    }

    @Test
    @DisplayName("GET a /api/** sin sesión → 401 JSON")
    void api_sinSesion_401Json() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/facturas/electronica/comprobantes/1")).andReturn();

        assertJson(r, 401, "Unauthorized", null);
    }

    @Test
    @DisplayName("GET a una página sin sesión → comportamiento previo: redirect al login (con y sin Accept)")
    void pagina_sinSesion_redirectAlLogin() throws Exception {
        MvcResult html = mockMvc.perform(get("/dashboard").accept(MediaType.TEXT_HTML)).andReturn();
        MvcResult sinAccept = mockMvc.perform(get("/facturas")).andReturn();

        for (MvcResult r : new MvcResult[]{html, sinAccept}) {
            assertThat(r.getResponse().getStatus()).isEqualTo(302);
            assertThat(r.getResponse().getRedirectedUrl()).isEqualTo("http://localhost/auth/login");
        }
    }

    @Test
    @DisplayName("GET a /api/** con sesión sin el rol requerido → 403 JSON de permisos")
    void api_sinPermiso_403Json() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/configuracion/empresa")
                .with(user("visualizador@example.invalid").roles("VISUALIZADOR"))).andReturn();

        assertJson(r, 403, "Forbidden", "No tiene permisos para realizar esta acción");
    }

    private void assertJson(MvcResult r, int status, String error, String message) throws Exception {
        assertThat(r.getResponse().getStatus()).isEqualTo(status);
        assertThat(r.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        Map<?, ?> body = objectMapper.readValue(r.getResponse().getContentAsByteArray(), Map.class);
        assertThat(body.get("status")).isEqualTo(status);
        assertThat(body.get("error")).isEqualTo(error);
        assertThat(body.get("message")).isInstanceOf(String.class);
        if (message != null) {
            assertThat(body.get("message")).isEqualTo(message);
        }
    }
}
