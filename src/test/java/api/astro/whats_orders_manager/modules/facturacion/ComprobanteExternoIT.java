package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.PanelFacturacionService;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.repository.RegimenTributarioRepository;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * T-057 (ADR-076 §3-4; criterios 15, 16 y 17): clave de un comprobante emitido fuera del ERP y aviso de cierre, con un
 * Clock que se mueve. Cada factura se inserta con el repositorio y se le fijan por SQL su fecha (createDate, que la
 * auditoría JPA llena con la hora del sistema) y su régimen sellado.
 *
 * Base: MySQL 8 en Testcontainers (t057-ce-it-mysql, tmpfs); esquema de Hibernate.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-057 — clave externa y aviso de cierre (MySQL)")
class ComprobanteExternoIT {

    static final ZoneId CR = ZoneId.of("America/Costa_Rica");
    static final String USUARIO = "admin.bootstrap@example.invalid";
    static final String CLAVE_A = "50628122600310123456700100001010000000011100000001";
    static final String CLAVE_B = "50628122600310123456700100001010000000022100000002";

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t057ce", "t057-ce-it-mysql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        RelojMovil relojMovil() {
            return new RelojMovil();
        }
    }

    /** Reloj de la aplicación en estas pruebas: una hora de Costa Rica que cada prueba fija. */
    static class RelojMovil extends Clock {
        private volatile Instant instante = Instant.now();

        void fijar(LocalDateTime horaCostaRica) {
            instante = horaCostaRica.atZone(CR).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return CR;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instante, zone);
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RelojMovil reloj;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ComprobanteElectronicoRepository comprobanteRepository;
    @Autowired private RegimenTributarioRepository regimenRepository;
    @Autowired private RegimenTributarioService regimenService;
    @Autowired private PanelFacturacionService panelService;

    private Cliente cliente;
    private ConfiguracionEmpresa empresa;
    private int secuencia;

    @BeforeEach
    void preparar() {
        limpiar();
        cliente = clienteRepository.save(new Cliente());
        empresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc()
                .orElseGet(() -> empresaRepository.save(ConfiguracionEmpresa.builder().razonSocial("Empresa T057 CE").build()));
        tramos(Regimen.GENERAL, "2026-09-01", Regimen.SIMPLIFICADO, "2027-01-01");
        reloj.fijar(LocalDateTime.of(2027, 1, 5, 10, 0));
    }

    @AfterEach
    void limpiar() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            jdbc.update("DELETE FROM comprobante_electronico");
            jdbc.update("DELETE FROM factura");
        } finally {
            jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private void tramos(Object... paresRegimenInicio) {
        regimenRepository.deleteAllInBatch();
        for (int i = 0; i < paresRegimenInicio.length; i += 2) {
            regimenRepository.save(RegimenTributario.builder().regimen((Regimen) paresRegimenInicio[i])
                    .vigenteDesde(LocalDate.parse((String) paresRegimenInicio[i + 1]))
                    .fechaRegistro(LocalDateTime.of(2026, 1, 1, 0, 0)).build());
        }
        regimenService.invalidarCache();
    }

    private int factura(Regimen regimen, LocalDateTime fecha, boolean anulada) {
        Factura f = new Factura();
        f.setCliente(cliente);
        f.setNumeroFactura("F-T057CE-" + (++secuencia));
        f.setAnulada(anulada);
        f.setSubtotal(BigDecimal.TEN);
        f.setIgv(BigDecimal.ZERO);
        f.setTotal(BigDecimal.TEN);
        int id = facturaRepository.save(f).getIdFactura();
        jdbc.update("UPDATE factura SET create_date = ?, regimen_emisor = ? WHERE id_factura = ?", fecha, regimen.name(), id);
        return id;
    }

    private int generalDeDiciembre() {
        return factura(Regimen.GENERAL, LocalDateTime.of(2026, 12, 15, 10, 0), false);
    }

    private void comprobante(int idFactura, EstadoComprobante estado) {
        int k = ++secuencia;
        comprobanteRepository.save(ComprobanteElectronico.builder().factura(facturaRepository.findById(idFactura).orElseThrow())
                .empresa(empresa).tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA)
                .claveNumerica("clave-t057ce-" + k).consecutivo("c" + k).fechaEmision(LocalDateTime.now().minusMinutes(k))
                .estado(estado).vigente(true).ambiente(AmbienteHacienda.SANDBOX).consultasErrorHacienda(0).build());
    }

    private MvcResult registrar(int id, String clave, String fecha) throws Exception {
        String cuerpo = "{" + (clave == null ? "" : "\"clave\":\"" + clave + "\"")
                + (clave != null && fecha != null ? "," : "") + (fecha == null ? "" : "\"fecha\":\"" + fecha + "\"") + "}";
        return mockMvc.perform(put("/api/facturas/" + id + "/comprobante-externo").with(user(USUARIO).roles("ADMIN"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    private MvcResult quitar(int id) throws Exception {
        return mockMvc.perform(delete("/api/facturas/" + id + "/comprobante-externo").with(user(USUARIO).roles("ADMIN"))
                .with(csrf())).andReturn();
    }

    private String claveDe(int id) {
        return jdbc.queryForObject("SELECT comprobante_externo_clave FROM factura WHERE id_factura = ?", String.class, id);
    }

    private List<String> auditoria(String tipo) {
        return jdbc.queryForList("SELECT descripcion FROM usuario_actividad WHERE tipo_actividad = ? ORDER BY id_actividad",
                String.class, tipo);
    }

    private String inicio() throws Exception {
        return mockMvc.perform(get("/facturas/inicio").with(user(USUARIO).roles("ADMIN")))
                .andReturn().getResponse().getContentAsString();
    }

    // ── clave externa (criterio 15) ──────────────────────────────────────────

    @Test
    @DisplayName("clave válida en una factura GENERAL sin comprobante → 200; queda la clave y la fecha, con auditoría")
    void registraClaveValida() throws Exception {
        int id = generalDeDiciembre();

        MvcResult r = registrar(id, CLAVE_A, "2026-12-28");

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(claveDe(id)).isEqualTo(CLAVE_A);
        assertThat(jdbc.queryForObject("SELECT comprobante_externo_fecha FROM factura WHERE id_factura = ?", String.class, id))
                .isEqualTo("2026-12-28");
        assertThat(auditoria("FACTURA_COMPROBANTE_EXTERNO_REGISTRADO")).singleElement().asString().contains(CLAVE_A);
    }

    @Test
    @DisplayName("clave ya registrada en otra factura → 409; la segunda factura no cambia")
    void claveDuplicada() throws Exception {
        int a = generalDeDiciembre();
        int b = generalDeDiciembre();
        assertThat(registrar(a, CLAVE_A, "2026-12-28").getResponse().getStatus()).isEqualTo(200);

        MvcResult r = registrar(b, CLAVE_A, "2026-12-28");

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(r.getResponse().getContentAsString()).contains("ya está registrada en otra factura");
        assertThat(claveDe(b)).isNull();
    }

    @Test
    @DisplayName("clave con longitud o caracteres inválidos, o sin fecha o con fecha futura → 400")
    void validacionesDeEntrada() throws Exception {
        int id = generalDeDiciembre();

        assertThat(registrar(id, CLAVE_A.substring(1), "2026-12-28").getResponse().getStatus()).isEqualTo(400);
        assertThat(registrar(id, CLAVE_A + "1", "2026-12-28").getResponse().getStatus()).isEqualTo(400);
        assertThat(registrar(id, CLAVE_A.substring(1) + "X", "2026-12-28").getResponse().getStatus()).isEqualTo(400);
        assertThat(registrar(id, null, "2026-12-28").getResponse().getStatus()).isEqualTo(400);
        assertThat(registrar(id, CLAVE_A, null).getResponse().getStatus()).isEqualTo(400);
        assertThat(registrar(id, CLAVE_A, "2027-01-06").getResponse().getStatus()).isEqualTo(400);
        assertThat(claveDe(id)).isNull();
        assertThat(registrar(id, CLAVE_A, "2027-01-05").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("factura SIMPLIFICADO → 409; factura anulada → 409; factura inexistente → 404")
    void facturasQueNoLaAceptan() throws Exception {
        int rts = factura(Regimen.SIMPLIFICADO, LocalDateTime.of(2027, 1, 2, 10, 0), false);
        int anulada = factura(Regimen.GENERAL, LocalDateTime.of(2026, 12, 15, 10, 0), true);

        MvcResult r = registrar(rts, CLAVE_A, "2027-01-02");
        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(r.getResponse().getContentAsString()).contains("régimen simplificado");
        assertThat(registrar(anulada, CLAVE_A, "2026-12-28").getResponse().getStatus()).isEqualTo(409);
        assertThat(registrar(999999, CLAVE_A, "2026-12-28").getResponse().getStatus()).isEqualTo(404);
        assertThat(claveDe(rts)).isNull();
        assertThat(claveDe(anulada)).isNull();
    }

    @Test
    @DisplayName("factura con comprobante ACEPTADO del ERP → 409; con solo RECHAZADO → 200")
    void conComprobanteDelErp() throws Exception {
        int aceptada = generalDeDiciembre();
        comprobante(aceptada, EstadoComprobante.ACEPTADO);
        int rechazada = generalDeDiciembre();
        comprobante(rechazada, EstadoComprobante.RECHAZADO);

        MvcResult r = registrar(aceptada, CLAVE_A, "2026-12-28");

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(r.getResponse().getContentAsString()).contains("comprobante generado por el sistema");
        assertThat(claveDe(aceptada)).isNull();
        assertThat(registrar(rechazada, CLAVE_B, "2026-12-28").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("corregir la clave y quitarla quedan auditados con la clave anterior y la nueva")
    void corregirYQuitar() throws Exception {
        int id = generalDeDiciembre();
        registrar(id, CLAVE_A, "2026-12-28");

        assertThat(registrar(id, CLAVE_B, "2026-12-29").getResponse().getStatus()).isEqualTo(200);
        assertThat(claveDe(id)).isEqualTo(CLAVE_B);
        assertThat(auditoria("FACTURA_COMPROBANTE_EXTERNO_CORREGIDO")).singleElement().asString()
                .contains(CLAVE_A).contains(CLAVE_B);
        // la misma clave sobre la misma factura (corregir solo la fecha) no es un duplicado
        assertThat(registrar(id, CLAVE_B, "2026-12-30").getResponse().getStatus()).isEqualTo(200);

        assertThat(quitar(id).getResponse().getStatus()).isEqualTo(200);
        assertThat(claveDe(id)).isNull();
        assertThat(jdbc.queryForObject("SELECT comprobante_externo_fecha FROM factura WHERE id_factura = ?", String.class, id))
                .isNull();
        assertThat(auditoria("FACTURA_COMPROBANTE_EXTERNO_ELIMINADO")).singleElement().asString().contains(CLAVE_B);
        assertThat(quitar(id).getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("el panel no cuenta como «sin comprobante» a la factura con clave externa")
    void panelSinLaFacturaConClave() throws Exception {
        int conClave = generalDeDiciembre();
        generalDeDiciembre();
        assertThat(panelService.contadores(null).sinComprobante()).isEqualTo(2);

        registrar(conClave, CLAVE_A, "2026-12-28");

        assertThat(panelService.contadores(null).sinComprobante()).isEqualTo(1);
    }

    // ── aviso de cierre (criterio 16) ────────────────────────────────────────

    /** Tres pendientes (una con solo un comprobante vigente RECHAZADO) y cuatro que no cuentan: con clave, con comprobante, anulada y SIMPLIFICADO. */
    private void mundoDeCierre() throws Exception {
        comprobante(generalDeDiciembre(), EstadoComprobante.RECHAZADO);
        generalDeDiciembre();
        factura(Regimen.GENERAL, LocalDateTime.of(2026, 10, 3, 9, 0), false);
        int conClave = generalDeDiciembre();
        registrar(conClave, CLAVE_A, "2026-12-16");
        comprobante(generalDeDiciembre(), EstadoComprobante.ACEPTADO);
        factura(Regimen.GENERAL, LocalDateTime.of(2026, 12, 10, 9, 0), true);
        factura(Regimen.SIMPLIFICADO, LocalDateTime.of(2027, 1, 2, 9, 0), false);
    }

    @Test
    @DisplayName("20-12: el aviso aparece con la cantidad de GENERAL sin comprobante ni clave y el enlace a la lista filtrada")
    void avisoElVeinte() throws Exception {
        reloj.fijar(LocalDateTime.of(2026, 12, 20, 8, 0));
        mundoDeCierre();

        String html = inicio();

        assertThat(html).contains("id=\"aviso-cierre\"");
        assertThat(html).containsPattern("id=\"aviso-cierre-cantidad\"[^>]*>\\s*3\\s*<");
        assertThat(html).contains("estadoFE=pendiente_cierre");
    }

    @Test
    @DisplayName("la lista filtrada por el enlace del aviso trae exactamente las facturas contadas")
    void listaFiltradaDelAviso() throws Exception {
        reloj.fijar(LocalDateTime.of(2026, 12, 22, 8, 0));
        mundoDeCierre();

        String html = inicio();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("href=\"([^\"]*estadoFE=pendiente_cierre[^\"]*)\"").matcher(html);
        assertThat(m.find()).isTrue();
        String url = m.group(1).replace("&amp;", "&");
        MvcResult r = mockMvc.perform(get(url).with(user(USUARIO).roles("ADMIN"))).andReturn();

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Factura> facturas = (List<Factura>) r.getModelAndView().getModel().get("facturas");
        assertThat(facturas).hasSize(3);
    }

    @Test
    @DisplayName("31-12: sigue apareciendo")
    void avisoElTreintaYUno() throws Exception {
        reloj.fijar(LocalDateTime.of(2026, 12, 31, 23, 59));
        mundoDeCierre();

        assertThat(inicio()).contains("id=\"aviso-cierre\"");
    }

    @Test
    @DisplayName("19-12 y 01-01: el aviso no aparece")
    void avisoFueraDelRango() throws Exception {
        mundoDeCierre();

        reloj.fijar(LocalDateTime.of(2026, 12, 19, 23, 59));
        assertThat(inicio()).doesNotContain("id=\"aviso-cierre\"");
        reloj.fijar(LocalDateTime.of(2027, 1, 1, 0, 0));
        assertThat(inicio()).doesNotContain("id=\"aviso-cierre\"");
    }

    @Test
    @DisplayName("sin un tramo SIMPLIFICADO siguiente, el 20-12 no aparece")
    void avisoSinTramoSiguiente() throws Exception {
        tramos(Regimen.GENERAL, "2026-09-01");
        reloj.fijar(LocalDateTime.of(2026, 12, 20, 8, 0));
        mundoDeCierre();

        assertThat(inicio()).doesNotContain("id=\"aviso-cierre\"");
    }

    @Test
    @DisplayName("en el rango y sin pendientes el aviso aparece con cantidad 0")
    void avisoSinPendientes() throws Exception {
        reloj.fijar(LocalDateTime.of(2026, 12, 20, 8, 0));

        String html = inicio();

        assertThat(html).contains("id=\"aviso-cierre\"");
        assertThat(html).containsPattern("id=\"aviso-cierre-cantidad\"[^>]*>\\s*0\\s*<");
    }
}
