package api.astro.whats_orders_manager.modules.notificacion.controller;

import api.astro.whats_orders_manager.modules.notificacion.enums.CanalNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.enums.TipoNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.model.PreferenciaNotificacion;
import api.astro.whats_orders_manager.modules.notificacion.repository.PreferenciaNotificacionRepository;
import api.astro.whats_orders_manager.modules.notificacion.service.NotificacionService;
import api.astro.whats_orders_manager.modules.notificacion.service.PreferenciaNotificacionService;
import api.astro.whats_orders_manager.modules.notificacion.service.impl.PreferenciaNotificacionServiceImpl;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-030 (ADR-035 §2, P-032) — GET /api/notificaciones/preferencias devuelve un DTO por tipo sin datos del usuario;
 * PUT acepta el payload de preferencias-notificaciones.js ({tipo, activoWeb, activoEmail, activoWhatsapp}) y lo
 * guarda como filas (usuario, tipo, canal) sin duplicar, sin filas NULL y en una sola transacción.
 * MVC + Jackson reales, JPA sobre H2.
 */
@WebMvcTest(value = NotificacionRestController.class, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@Import(PreferenciaNotificacionServiceImpl.class)
@DisplayName("T-030 — preferencias de notificaciones: DTO sin usuario y guardado por tipo y canal")
class PreferenciasNotificacionApiTest {

    private static final String URL = "/api/notificaciones/preferencias";
    private static final String EMAIL = "ana@example.com";

    @MockitoBean private NotificacionService notificacionService;
    @MockitoBean private UsuarioActividadService usuarioActividadService;
    @MockitoBean private UsuarioService usuarioService;

    @MockitoSpyBean private PreferenciaNotificacionRepository preferenciaRepository;

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PreferenciaNotificacionService preferenciaService;
    @Autowired private ObjectMapper objectMapper;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setEmail(EMAIL);
        usuario.setPassword("$2a$10$hashDeLaClaveQueNoDebeSalir");
        usuario.setRol("ADMIN");
        usuario = usuarioRepository.save(usuario);
    }

    @AfterEach
    void limpiar() {
        reset(preferenciaRepository);
        preferenciaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private PreferenciaNotificacion fila(TipoNotificacion tipo, CanalNotificacion canal, boolean activa) {
        return preferenciaRepository.save(PreferenciaNotificacion.builder()
                .usuario(usuario).tipoNotificacion(tipo).canal(canal).activa(activa).build());
    }

    private ResultActions guardar(String json) throws Exception {
        return mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(json)
                .with(csrf()).with(user(EMAIL).roles("ADMIN")));
    }

    private JsonNode leer() throws Exception {
        String json = mockMvc.perform(get(URL).with(user(EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private static Map<String, Boolean> canales(JsonNode lista, TipoNotificacion tipo) {
        for (JsonNode n : lista) {
            if (tipo.name().equals(n.get("tipo").asText())) {
                return Map.of("WEB", n.get("activoWeb").asBoolean(),
                        "EMAIL", n.get("activoEmail").asBoolean(),
                        "WHATSAPP", n.get("activoWhatsapp").asBoolean());
            }
        }
        throw new AssertionError("Sin entrada para " + tipo);
    }

    private static void recogerClaves(JsonNode nodo, List<String> claves) {
        if (nodo.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = nodo.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                claves.add(e.getKey().toLowerCase(Locale.ROOT));
                recogerClaves(e.getValue(), claves);
            }
        } else if (nodo.isArray()) {
            nodo.forEach(n -> recogerClaves(n, claves));
        }
    }

    private long filasConNull() {
        return preferenciaRepository.findAll().stream()
                .filter(p -> p.getTipoNotificacion() == null || p.getCanal() == null).count();
    }

    private static final String DOS_TIPOS = """
            [{"tipo":"FACTURA_VENCIDA","activoWeb":true,"activoEmail":false,"activoWhatsapp":true},
             {"tipo":"STOCK_BAJO","activoWeb":false,"activoEmail":true,"activoWhatsapp":false}]""";

    @Test
    @DisplayName("GET: un DTO por tipo, sin usuario, password, clave, contrasena ni rol en ningún nivel")
    void get_sinDatosDelUsuario() throws Exception {
        fila(TipoNotificacion.FACTURA_VENCIDA, CanalNotificacion.EMAIL, true);
        fila(null, null, true);

        JsonNode respuesta = leer();

        List<String> claves = new ArrayList<>();
        recogerClaves(respuesta, claves);
        assertThat(claves).doesNotContain("usuario", "password", "clave", "contrasena", "rol");
        assertThat(respuesta.toString()).doesNotContain("hashDeLaClave", EMAIL);
        assertThat(respuesta).hasSize(TipoNotificacion.values().length);
        assertThat(canales(respuesta, TipoNotificacion.FACTURA_VENCIDA))
                .containsExactlyInAnyOrderEntriesOf(Map.of("WEB", true, "EMAIL", true, "WHATSAPP", true));
    }

    @Test
    @DisplayName("GET muestra el valor efectivo que usa el envío: global activa → todo activo; tras desactivar X/EMAIL, inactivo en ambos")
    void get_coincideConElEnvio() throws Exception {
        fila(null, null, true);

        JsonNode antes = leer();
        for (TipoNotificacion tipo : TipoNotificacion.values()) {
            assertThat(canales(antes, tipo)).as(tipo.name())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("WEB", true, "EMAIL", true, "WHATSAPP", true));
        }

        guardar("""
                [{"tipo":"FACTURA_VENCIDA","activoWeb":true,"activoEmail":false,"activoWhatsapp":true}]""")
                .andExpect(status().isOk());

        assertThat(canales(leer(), TipoNotificacion.FACTURA_VENCIDA))
                .containsExactlyInAnyOrderEntriesOf(Map.of("WEB", true, "EMAIL", false, "WHATSAPP", true));
        assertThat(preferenciaService.usuarioAceptaNotificacion(
                usuario, TipoNotificacion.FACTURA_VENCIDA, CanalNotificacion.EMAIL)).isFalse();
        assertThat(preferenciaService.usuarioAceptaNotificacion(
                usuario, TipoNotificacion.FACTURA_VENCIDA, CanalNotificacion.WEB)).isTrue();
        assertThat(preferenciaService.usuarioAceptaNotificacion(
                usuario, TipoNotificacion.STOCK_BAJO, CanalNotificacion.EMAIL)).isTrue();
    }

    @Test
    @DisplayName("PUT de dos tipos con varios canales y relectura: devuelve exactamente lo guardado")
    void guardarYReleer() throws Exception {
        fila(TipoNotificacion.FACTURA_VENCIDA, CanalNotificacion.EMAIL, true); // como la fila 22 de T-029
        fila(null, null, true); // preferencia global legítima: no se toca

        guardar(DOS_TIPOS).andExpect(status().isOk());

        JsonNode respuesta = leer();
        assertThat(canales(respuesta, TipoNotificacion.FACTURA_VENCIDA))
                .containsExactlyInAnyOrderEntriesOf(Map.of("WEB", true, "EMAIL", false, "WHATSAPP", true));
        assertThat(canales(respuesta, TipoNotificacion.STOCK_BAJO))
                .containsExactlyInAnyOrderEntriesOf(Map.of("WEB", false, "EMAIL", true, "WHATSAPP", false));
        assertThat(canales(respuesta, TipoNotificacion.PAGO_RECIBIDO)) // sin filas propias: sigue a la global
                .containsExactlyInAnyOrderEntriesOf(Map.of("WEB", true, "EMAIL", true, "WHATSAPP", true));
        assertThat(preferenciaRepository.count()).isEqualTo(7); // 2 tipos x 3 canales + la global
        assertThat(filasConNull()).isEqualTo(1);
    }

    @Test
    @DisplayName("Guardar dos veces lo mismo: 200 ambas veces, sin duplicados")
    void guardarDosVeces_sinDuplicados() throws Exception {
        guardar(DOS_TIPOS).andExpect(status().isOk());
        guardar(DOS_TIPOS).andExpect(status().isOk());

        assertThat(preferenciaRepository.count()).isEqualTo(6);
        assertThat(filasConNull()).isZero();
    }

    @Test
    @DisplayName("Payload inválido → 400 sin crear filas")
    void payloadInvalido_400() throws Exception {
        for (String json : List.of(
                "[{\"activoWeb\":true,\"activoEmail\":true,\"activoWhatsapp\":true}]",
                "[{\"tipo\":\"NO_EXISTE\",\"activoWeb\":true,\"activoEmail\":true,\"activoWhatsapp\":true}]",
                "[{\"tipo\":\"STOCK_BAJO\",\"activoWeb\":true,\"activoEmail\":true,\"activoWhatsapp\":true},"
                        + "{\"tipo\":\"FACTURA_VENCIDA\",\"activoWeb\":true}]",
                "[{\"tipo\":\"STOCK_BAJO\",\"activoWeb\":true,\"activoEmail\":true,\"activoWhatsapp\":true},"
                        + "{\"tipo\":\"STOCK_BAJO\",\"activoWeb\":false,\"activoEmail\":false,\"activoWhatsapp\":false}]",
                "[null]",
                "{\"tipo\":\"STOCK_BAJO\"}")) {
            guardar(json).andExpect(status().isBadRequest());
        }

        assertThat(preferenciaRepository.count()).isZero();
    }

    @Test
    @DisplayName("Un fallo a mitad del guardado revierte todo: 500 genérico y ninguna fila")
    void falloAMitad_noPersisteNada() throws Exception {
        AtomicInteger guardados = new AtomicInteger();
        doAnswer(inv -> {
            if (guardados.incrementAndGet() == 4) {
                throw new IllegalStateException("fallo simulado");
            }
            return inv.callRealMethod();
        }).when(preferenciaRepository).save(any(PreferenciaNotificacion.class));

        String cuerpo = guardar(DOS_TIPOS).andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("fallo simulado");
        reset(preferenciaRepository);
        assertThat(preferenciaRepository.count()).isZero();
    }
}
