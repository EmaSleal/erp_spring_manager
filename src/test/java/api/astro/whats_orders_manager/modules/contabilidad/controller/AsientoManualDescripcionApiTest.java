package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.config.PersistenceConfig;
import api.astro.whats_orders_manager.modules.contabilidad.dto.mapper.AsientoContableMapper;
import api.astro.whats_orders_manager.modules.contabilidad.dto.mapper.DetalleAsientoMapper;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.DetalleAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.DetalleAsientoRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.seguridad.config.AuditorAwareImpl;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import api.astro.whats_orders_manager.shared.service.MonedaService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-019 (P-031) — the manual journal entry API with the real Spring MVC + Jackson setup and JPA on H2:
 * the line description travels as detalles[].descripcion in both directions (POST/PUT persist it, GET
 * returns it), and service validations reach the client as a 400 with the server message.
 */
@WebMvcTest(value = AsientoContableController.class, properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@Import({AsientoContableService.class, AsientoContableMapper.class, DetalleAsientoMapper.class,
        PersistenceConfig.class, AuditorAwareImpl.class})
@DisplayName("T-019 — API de asiento manual: descripción de línea y mensajes del servidor")
class AsientoManualDescripcionApiTest {

    @MockitoBean(name = "permisoService") private PermisoService permisoService;
    @MockitoBean private MonedaService monedaService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private UsuarioActividadService usuarioActividadService;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private DetalleAsientoRepository detalleRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private CuentaContable caja;
    private CuentaContable gasto;
    private CuentaContable otroGasto;

    @BeforeEach
    void setUp() {
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(true);
        tx = new TransactionTemplate(transactionManager);
        caja = cuenta("1.1.01.001", TipoCuenta.ACTIVO);
        gasto = cuenta("5.1.03.001", TipoCuenta.EGRESO);
        otroGasto = cuenta("5.1.03.002", TipoCuenta.EGRESO);
    }

    @AfterEach
    void limpiar() {
        detalleRepository.deleteAllInBatch();
        asientoRepository.deleteAllInBatch();
        cuentaRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("POST con detalles[].descripcion la persiste por línea y GET /api/{id} la devuelve en descripcion")
    void post_persisteDescripcion_yGetLaDevuelve() throws Exception {
        String cuerpo = """
                {"fecha": "2026-10-15", "tipo": "MANUAL", "concepto": "Pago de servicios",
                 "detalles": [
                   {"cuentaId": %d, "descripcion": "Servicio de limpieza", "debe": 150.00, "haber": 0},
                   {"cuentaId": %d, "descripcion": "Salida de caja", "debe": 0, "haber": 150.00}
                 ]}
                """.formatted(gasto.getIdCuenta(), caja.getIdCuenta());

        String respuesta = mockMvc.perform(post("/contabilidad/asientos/api")
                        .with(user("admin")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(respuesta).get("idAsiento").asLong();

        assertThat(descripcionesEnBd(id)).containsExactlyInAnyOrder("Servicio de limpieza", "Salida de caja");

        String detalle = mockMvc.perform(get("/contabilidad/asientos/api/{id}", id).with(user("admin")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode lineas = objectMapper.readTree(detalle).get("detalles");
        assertThat(lineas).hasSize(2);
        assertThat(List.of(lineas.get(0).get("descripcion").asText(), lineas.get(1).get("descripcion").asText()))
                .containsExactlyInAnyOrder("Servicio de limpieza", "Salida de caja");
        assertThat(lineas.get(0).has("concepto")).isFalse();
    }

    @Test
    @DisplayName("PUT cambia una línea y su descripción; un borrador descuadrado se guarda")
    void put_cambiaLineaYDescripcion_aceptaDescuadrado() throws Exception {
        long id = crearBorrador();
        String cambios = """
                {"fecha": "2026-10-16", "tipo": "MANUAL", "concepto": "Pago de servicios (editado)",
                 "detalles": [
                   {"cuentaId": %d, "descripcion": "Servicio de jardinería", "debe": 200.00, "haber": 0},
                   {"cuentaId": %d, "descripcion": "Salida de caja", "debe": 0, "haber": 150.00}
                 ]}
                """.formatted(otroGasto.getIdCuenta(), caja.getIdCuenta());

        mockMvc.perform(put("/contabilidad/asientos/api/{id}", id)
                        .with(user("admin")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cambios))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estaCuadrado").value(false));

        assertThat(descripcionesEnBd(id)).containsExactlyInAnyOrder("Servicio de jardinería", "Salida de caja");
    }

    @Test
    @DisplayName("POST sin fecha → 400 con el mensaje del servidor y nada guardado")
    void post_sinFecha_400ConMensaje() throws Exception {
        String cuerpo = """
                {"tipo": "MANUAL", "concepto": "Sin fecha",
                 "detalles": [{"cuentaId": %d, "descripcion": "x", "debe": 10, "haber": 0}]}
                """.formatted(gasto.getIdCuenta());

        mockMvc.perform(post("/contabilidad/asientos/api")
                        .with(user("admin")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La fecha del asiento es obligatoria"));

        assertThat(asientoRepository.count()).isZero();
    }

    @Test
    @DisplayName("contabilizar un borrador descuadrado → 400 con el mensaje del cuadre; sigue en BORRADOR")
    void contabilizar_descuadrado_400ConMensaje() throws Exception {
        long id = crearBorrador();
        mockMvc.perform(put("/contabilidad/asientos/api/{id}", id)
                        .with(user("admin")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fecha": "2026-10-15", "tipo": "MANUAL", "concepto": "Descuadrado",
                                 "detalles": [
                                   {"cuentaId": %d, "descripcion": "a", "debe": 100.00, "haber": 0},
                                   {"cuentaId": %d, "descripcion": "b", "debe": 0, "haber": 60.00}
                                 ]}
                                """.formatted(gasto.getIdCuenta(), caja.getIdCuenta())))
                .andExpect(status().isOk());

        String respuesta = mockMvc.perform(post("/contabilidad/asientos/api/{id}/contabilizar", id)
                        .with(user("admin")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(respuesta).get("error").asText()).contains("no está cuadrado");
        assertThat(asientoRepository.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoAsiento.BORRADOR);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private long crearBorrador() throws Exception {
        String cuerpo = """
                {"fecha": "2026-10-15", "tipo": "MANUAL", "concepto": "Pago de servicios",
                 "detalles": [
                   {"cuentaId": %d, "descripcion": "Servicio de limpieza", "debe": 150.00, "haber": 0},
                   {"cuentaId": %d, "descripcion": "Salida de caja", "debe": 0, "haber": 150.00}
                 ]}
                """.formatted(gasto.getIdCuenta(), caja.getIdCuenta());
        String respuesta = mockMvc.perform(post("/contabilidad/asientos/api")
                        .with(user("admin")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("idAsiento").asLong();
    }

    private List<String> descripcionesEnBd(long asientoId) {
        return tx.execute(s -> {
            AsientoContable asiento = asientoRepository.findById(asientoId).orElseThrow();
            return asiento.getDetalles().stream().map(DetalleAsiento::getDescripcion).toList();
        });
    }

    private CuentaContable cuenta(String codigo, TipoCuenta tipo) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setTipo(tipo);
        c.setNivel(4);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return cuentaRepository.save(c);
    }
}
