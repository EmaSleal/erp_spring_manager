package api.astro.whats_orders_manager.modules.contabilidad.controller;

import api.astro.whats_orders_manager.config.PersistenceConfig;
import api.astro.whats_orders_manager.modules.contabilidad.dto.mapper.AsientoContableMapper;
import api.astro.whats_orders_manager.modules.contabilidad.dto.mapper.DetalleAsientoMapper;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAsiento;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-020 (ADR-023) — POST /contabilidad/asientos/api/{id}/revertir con Spring MVC + Jackson reales y
 * JPA sobre H2: motivo obligatorio (400 con mensaje), la reversión se devuelve (200) y el original
 * queda CONTABILIZADO, un automático se rechaza (400), y /api/{id}/anular ya no existe.
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
@DisplayName("T-020 — API de reversión de asientos")
class AsientoRevertirApiTest {

    @MockitoBean(name = "permisoService") private PermisoService permisoService;
    @MockitoBean private MonedaService monedaService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private UsuarioActividadService usuarioActividadService;

    @Autowired private MockMvc mockMvc;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private DetalleAsientoRepository detalleRepository;

    private CuentaContable gasto;
    private CuentaContable caja;

    @BeforeEach
    void setUp() {
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(true);
        gasto = cuenta("5.1.03.001", TipoCuenta.EGRESO);
        caja = cuenta("1.1.01.001", TipoCuenta.ACTIVO);
    }

    @AfterEach
    void limpiar() {
        detalleRepository.deleteAllInBatch();
        asientoRepository.findAll().forEach(a -> a.setAsientoRevertido(null));
        asientoRepository.saveAllAndFlush(asientoRepository.findAll());
        asientoRepository.deleteAllInBatch();
        cuentaRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("sin motivo (sin cuerpo y con motivo en blanco) → 400 con el mensaje; nada se crea")
    void sinMotivo_400() throws Exception {
        Long id = contabilizado(TipoAsiento.MANUAL);

        mockMvc.perform(post("/contabilidad/asientos/api/{id}/revertir", id).with(user("admin")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El motivo de la reversión es obligatorio"));
        mockMvc.perform(post("/contabilidad/asientos/api/{id}/revertir", id).with(user("admin")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El motivo de la reversión es obligatorio"));

        assertThat(asientoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("con motivo en el cuerpo → 200 con la reversión (id, número, enlace al original); el original sigue CONTABILIZADO")
    void conMotivo_200ConReversion() throws Exception {
        Long id = contabilizado(TipoAsiento.MANUAL);

        mockMvc.perform(post("/contabilidad/asientos/api/{id}/revertir", id).with(user("admin")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\": \"Registro duplicado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAsiento").isNumber())
                .andExpect(jsonPath("$.numero").isString())
                .andExpect(jsonPath("$.estado").value("CONTABILIZADO"))
                .andExpect(jsonPath("$.reversionDeId").value(id))
                .andExpect(jsonPath("$.puedeRevertirse").value(false));

        assertThat(asientoRepository.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
        assertThat(asientoRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("motivo como parámetro también se acepta")
    void motivoComoParametro_200() throws Exception {
        Long id = contabilizado(TipoAsiento.AJUSTE);

        mockMvc.perform(post("/contabilidad/asientos/api/{id}/revertir", id).param("motivo", "Ajuste erróneo")
                        .with(user("admin")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reversionDeId").value(id));
    }

    @Test
    @DisplayName("sobre un asiento automático → 400 con el mensaje; nada se crea")
    void automatico_400() throws Exception {
        Long id = contabilizado(TipoAsiento.AUTOMATICO_VENTA);

        mockMvc.perform(post("/contabilidad/asientos/api/{id}/revertir", id).with(user("admin")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\": \"Prueba\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Asiento automático sin documento de origen; no se puede revertir desde Contabilidad"));

        assertThat(asientoRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/{id}/anular ya no existe (404/405) y no cambia el asiento")
    void anular_yaNoExiste() throws Exception {
        Long id = contabilizado(TipoAsiento.MANUAL);

        int status = mockMvc.perform(post("/contabilidad/asientos/api/{id}/anular", id).param("motivo", "x")
                        .with(user("admin")).with(csrf()))
                .andReturn().getResponse().getStatus();

        assertThat(status).isIn(404, 405);
        assertThat(asientoRepository.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoAsiento.CONTABILIZADO);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long contabilizado(TipoAsiento tipo) {
        AsientoContable a = new AsientoContable();
        a.setNumero("ASI-2026-0001");
        a.setFecha(LocalDate.of(2026, 9, 1));
        a.setConcepto("Asiento de prueba");
        a.setTipo(tipo);
        a.setEstado(EstadoAsiento.CONTABILIZADO);
        a.setFechaCreacion(LocalDateTime.now());
        a.agregarDetalle(linea(gasto, "100.00", "0.00"));
        a.agregarDetalle(linea(caja, "0.00", "100.00"));
        return asientoRepository.save(a).getIdAsiento();
    }

    private static DetalleAsiento linea(CuentaContable cuenta, String debe, String haber) {
        DetalleAsiento d = new DetalleAsiento();
        d.setCuenta(cuenta);
        d.setDebe(new BigDecimal(debe));
        d.setHaber(new BigDecimal(haber));
        return d;
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
