package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.NotaCreditoConsultaService;
import api.astro.whats_orders_manager.modules.facturacion.service.SustitucionElegibilidad;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConfiguracionHaciendaNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.PanelFacturacionService;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-043 (criterios 4, 7 y 8) — GET /facturas/inicio contra MySQL real: las consultas de conteo (incluida la de
 * sustitutas) corren en MySQL, y la tarjeta de Hacienda responde 200 con la configuración legible, sin configuración
 * activa y con las credenciales que no se descifran (mensajes fijos de T-041).
 *
 * <p>Base: MySQL 8 en Testcontainers (t043-it-mysql, tmpfs, sin volúmenes).
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("T-043 — panel de inicio de Facturación (MySQL)")
class PanelFacturacionIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t043")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t043-it-mysql"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("database/routines/stored_procedures.sql")),
                    "/docker-entrypoint-initdb.d/01-routines.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @MockitoBean(name = "permisoService") private PermisoService permisoService;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PanelFacturacionService panelService;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ConfiguracionHaciendaRepository configuracionRepository;
    @Autowired private ComprobanteElectronicoRepository comprobanteRepository;
    @Autowired private NotaFacturaRepository notaRepository;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private SustitucionElegibilidad sustitucionElegibilidad;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired private NotaCreditoConsultaService notaConsultaService;

    private ConfiguracionEmpresa empresa;
    private Cliente cliente;
    private Long idConfigSandbox;
    private Long idConfigProduccion;
    private int secuencia;

    @BeforeEach
    void preparar() {
        when(permisoService.tienePermisoPorCodigo(anyString(), anyString())).thenReturn(false);
        when(permisoService.tienePermisoPorCodigo(eq("gerente"), eq("FACTURA_VER"))).thenReturn(true);
        limpiarDatos();
        empresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc()
                .orElseGet(() -> empresaRepository.save(ConfiguracionEmpresa.builder().razonSocial("Empresa T043").build()));
        // dos configuraciones (SANDBOX y PRODUCCION); una activa, que se cambia por SQL como lo haría un administrador
        idConfigSandbox = configuracion(AmbienteHacienda.SANDBOX);
        idConfigProduccion = configuracion(AmbienteHacienda.PRODUCCION);
        activar(AmbienteHacienda.SANDBOX);
        cliente = clienteRepository.save(new Cliente());
    }

    private Long configuracion(AmbienteHacienda ambiente) {
        return configuracionRepository.save(ConfiguracionHacienda.builder().empresa(empresa).ambiente(ambiente)
                .usuarioHacienda("usuario-t043").passwordHacienda("clave-t043").activa(false)
                .rutaCertificado(CertificadoDePrueba.ruta()).pinCertificado(CertificadoDePrueba.PIN).build()).getId();
    }

    /** Activa la configuración del ambiente; null = ninguna activa. */
    private void activar(AmbienteHacienda ambiente) {
        Long id = ambiente == null ? -1L : ambiente == AmbienteHacienda.SANDBOX ? idConfigSandbox : idConfigProduccion;
        jdbc.update("UPDATE configuracion_hacienda SET activa = (id = ?)", id);
    }

    private void limpiarDatos() {
        jdbc.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            jdbc.update("DELETE FROM nota_factura");
            jdbc.update("DELETE FROM comprobante_electronico");
            jdbc.update("DELETE FROM factura");
        } finally {
            jdbc.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
        jdbc.update("DELETE FROM configuracion_hacienda");
    }

    @AfterEach
    void limpiar() {
        limpiarDatos();
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private Factura factura(boolean anulada) {
        Factura f = new Factura();
        f.setCliente(cliente);
        f.setNumeroFactura("F-T043-" + (++secuencia));
        f.setAnulada(anulada);
        return facturaRepository.save(f);
    }

    private ComprobanteElectronico comprobante(Factura f, NotaFactura n, EstadoComprobante estado, boolean vigente,
                                               AmbienteHacienda ambiente) {
        int k = ++secuencia;
        return comprobanteRepository.save(ComprobanteElectronico.builder().factura(f).notaFactura(n).empresa(empresa)
                .tipoComprobante(n == null ? TipoComprobanteElectronico.FACTURA_ELECTRONICA
                        : TipoComprobanteElectronico.NOTA_CREDITO)
                .claveNumerica("clave-t043-" + k).consecutivo("c" + k).fechaEmision(LocalDateTime.now().minusMinutes(k))
                .estado(estado).vigente(vigente).ambiente(ambiente).consultasErrorHacienda(0).build());
    }

    private NotaFactura nota(Factura f, ComprobanteElectronico ref, EstadoNotaFactura estado, String codigo) {
        return notaRepository.save(NotaFactura.builder().factura(f).tipo(TipoNotaFactura.CREDITO)
                .codigoReferencia(codigo).razon("razón de prueba").estado(estado).fechaEmision(LocalDateTime.now())
                .comprobanteReferencia(ref).claveReferencia(ref.getClaveNumerica())
                .fechaContable(java.time.LocalDate.now()).periodoFiscalAtribucion(java.time.YearMonth.now().toString())
                .fechaEmisionReferencia(LocalDateTime.now()).moneda("CRC").tipoCambio(BigDecimal.ONE)
                .subtotal(BigDecimal.ONE).descuento(BigDecimal.ZERO).impuesto(BigDecimal.ZERO).total(BigDecimal.ONE)
                .usuarioCreador("t043").createDate(LocalDateTime.now()).build());
    }

    /** Factura anulada que admite sustituta (nota 01 APLICADA con su comprobante ACEPTADO) en el ambiente. */
    private Factura anuladaElegible(AmbienteHacienda ambiente) {
        Factura f = factura(true);
        ComprobanteElectronico origen = comprobante(f, null, EstadoComprobante.ACEPTADO, true, ambiente);
        NotaFactura n = nota(f, origen, EstadoNotaFactura.APLICADA, "01");
        comprobante(null, n, EstadoComprobante.ACEPTADO, true, ambiente);
        return f;
    }

    private void notaEnTramite(AmbienteHacienda ambiente) {
        Factura f = factura(false);
        ComprobanteElectronico ref = comprobante(f, null, EstadoComprobante.ACEPTADO, true, ambiente);
        NotaFactura n = nota(f, ref, EstadoNotaFactura.EN_TRAMITE, "02");
        comprobante(null, n, EstadoComprobante.FIRMADO, true, ambiente);
    }

    private void comprobantes(int cantidad, EstadoComprobante estado, boolean vigente, AmbienteHacienda ambiente) {
        for (int i = 0; i < cantidad; i++) {
            comprobante(factura(false), null, estado, vigente, ambiente);
        }
    }

    /** Mundo mixto: cada ambiente con sus comprobantes, notas y anuladas. */
    private void datosMixtos() {
        comprobantes(3, EstadoComprobante.RECHAZADO, true, AmbienteHacienda.SANDBOX);
        comprobantes(1, EstadoComprobante.ERROR, true, AmbienteHacienda.SANDBOX);
        comprobantes(1, EstadoComprobante.RECHAZADO, true, AmbienteHacienda.PRODUCCION);
        comprobantes(2, EstadoComprobante.RECHAZADO, false, AmbienteHacienda.PRODUCCION); // historial: no cuenta
        comprobantes(1, EstadoComprobante.NO_TRANSMITIDO, true, AmbienteHacienda.PRODUCCION);
        notaEnTramite(AmbienteHacienda.SANDBOX);
        notaEnTramite(AmbienteHacienda.SANDBOX);
        notaEnTramite(AmbienteHacienda.PRODUCCION);
        anuladaElegible(AmbienteHacienda.SANDBOX);
        anuladaElegible(AmbienteHacienda.SANDBOX);
        anuladaElegible(AmbienteHacienda.PRODUCCION);
    }

    private String pagina() throws Exception {
        return mockMvc.perform(get("/facturas/inicio").with(user("gerente").roles("USER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("los contadores (COUNT en MySQL) son 0 sin datos y la configuración legible se muestra")
    void legible() throws Exception {
        var c = panelService.contadores(null);
        assertThat(c.comprobantesError() + c.comprobantesNoTransmitido() + c.comprobantesEnvioIncierto()
                + c.comprobantesRechazado() + c.notasEnTramite() + c.anuladasSinSustituta() + c.sinComprobante())
                .isZero();

        String html = pagina();

        assertThat(html).contains("id=\"hacienda-ambiente\"", "SANDBOX", "Vence el ");
        assertThat(html).doesNotContain("id=\"hacienda-aviso\"", "usuario-t043", "clave-t043", CertificadoDePrueba.PIN);
    }

    @Test
    @DisplayName("sin configuración activa: 200 con el aviso fijo")
    void sinActiva() throws Exception {
        jdbc.update("UPDATE configuracion_hacienda SET activa = b'0'");

        assertThat(pagina()).contains("id=\"hacienda-aviso\"")
                .contains(ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA);
    }

    @Test
    @DisplayName("credenciales que no se descifran: 200 con el aviso fijo, nunca 500")
    void ilegible() throws Exception {
        String original = jdbc.queryForObject("SELECT password_hacienda FROM configuracion_hacienda WHERE activa = b'1'",
                String.class);
        assertThat(original).startsWith("enc:v1:");
        String alterado = original.substring(0, original.length() - 6)
                + (original.endsWith("AAAAAA") ? "BBBBBB" : "AAAAAA");
        jdbc.update("UPDATE configuracion_hacienda SET password_hacienda = ? WHERE activa = b'1'", alterado);

        assertThat(pagina()).contains("id=\"hacienda-aviso\"")
                .contains(ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE);
    }

    // ── review 1: contadores por ambiente ────────────────────────────────────

    @Test
    @DisplayName("PRODUCCION activa: no cuenta comprobantes, notas ni anuladas de SANDBOX; el encabezado lo dice")
    void contadoresConProduccionActiva() throws Exception {
        datosMixtos();
        activar(AmbienteHacienda.PRODUCCION);

        var c = panelService.obtener().contadores();

        assertThat(c.comprobantesRechazado()).isEqualTo(1);
        assertThat(c.comprobantesError()).isZero();
        assertThat(c.comprobantesNoTransmitido()).isEqualTo(1);
        assertThat(c.notasEnTramite()).isEqualTo(1);
        assertThat(c.anuladasSinSustituta()).isEqualTo(1);
        String html = pagina();
        assertThat(html).contains("Contadores del ambiente PRODUCCION")
                .contains("estado=RECHAZADO&amp;vigente=true&amp;ambiente=PRODUCCION")
                .doesNotContain("Sin ambiente activo");
    }

    @Test
    @DisplayName("SANDBOX activa: la otra dirección, no cuenta lo de PRODUCCION")
    void contadoresConSandboxActiva() throws Exception {
        datosMixtos();
        activar(AmbienteHacienda.SANDBOX);

        var c = panelService.obtener().contadores();

        assertThat(c.comprobantesRechazado()).isEqualTo(3);
        assertThat(c.comprobantesError()).isEqualTo(1);
        assertThat(c.comprobantesNoTransmitido()).isZero();
        assertThat(c.notasEnTramite()).isEqualTo(2);
        assertThat(c.anuladasSinSustituta()).isEqualTo(2);
        assertThat(pagina()).contains("Contadores del ambiente SANDBOX");
    }

    @Test
    @DisplayName("sin configuración activa: cuentan todos los ambientes, con la leyenda fija, 200")
    void contadoresSinActiva() throws Exception {
        datosMixtos();
        activar(null);

        var panel = panelService.obtener();

        assertThat(panel.ambienteContadores()).isNull();
        assertThat(panel.contadores().comprobantesRechazado()).isEqualTo(4);
        assertThat(panel.contadores().comprobantesError()).isEqualTo(1);
        assertThat(panel.contadores().comprobantesNoTransmitido()).isEqualTo(1);
        assertThat(panel.contadores().notasEnTramite()).isEqualTo(3);
        assertThat(panel.contadores().anuladasSinSustituta()).isEqualTo(3);
        String html = pagina();
        assertThat(html).contains("Sin ambiente activo: los contadores incluyen todos los ambientes")
                .doesNotContain("Contadores del ambiente");
    }

    @Test
    @DisplayName("sin comprobante: cuenta las no anuladas sin ningún comprobante, de cualquier ambiente; el listado sin_fe coincide")
    void sinComprobanteCoincideConElListado() throws Exception {
        factura(false);
        factura(false);
        factura(true);                                                                  // anulada: no
        comprobante(factura(false), null, EstadoComprobante.ACEPTADO, true, AmbienteHacienda.PRODUCCION); // otro ambiente: no
        comprobante(factura(false), null, EstadoComprobante.RECHAZADO, false, AmbienteHacienda.SANDBOX); // historial: no

        for (AmbienteHacienda activo : new AmbienteHacienda[]{AmbienteHacienda.SANDBOX, AmbienteHacienda.PRODUCCION, null}) {
            activar(activo);
            long contador = panelService.obtener().contadores().sinComprobante();
            var modelo = mockMvc.perform(get("/facturas").param("estadoFE", "sin_fe").with(user("gerente").roles("USER")))
                    .andExpect(status().isOk()).andReturn().getModelAndView().getModel();
            assertThat(contador).as("contador con %s", activo).isEqualTo(2);
            assertThat(((Number) modelo.get("totalItems")).longValue()).as("listado con %s", activo).isEqualTo(contador);
        }
    }

    @Test
    @DisplayName("el listado de comprobantes filtra en el servidor: con más de 100 en un estado, el total y las páginas coinciden con el contador")
    void listadoDeComprobantesFiltradoEnElServidor() throws Exception {
        comprobantes(130, EstadoComprobante.RECHAZADO, true, AmbienteHacienda.SANDBOX);
        comprobantes(5, EstadoComprobante.RECHAZADO, false, AmbienteHacienda.SANDBOX);   // historial
        comprobantes(3, EstadoComprobante.RECHAZADO, true, AmbienteHacienda.PRODUCCION); // otro ambiente
        comprobantes(2, EstadoComprobante.ERROR, true, AmbienteHacienda.SANDBOX);        // otro estado
        activar(AmbienteHacienda.SANDBOX);
        long contador = panelService.obtener().contadores().comprobantesRechazado();
        assertThat(contador).isEqualTo(130);
        Integer idEmpresa = empresa.getIdConfiguracion();
        String base = "/api/facturas/electronica/comprobantes/empresa/" + idEmpresa + "/estado/RECHAZADO";

        var pagina0 = mockMvc.perform(get(base).param("vigente", "true").param("ambiente", "SANDBOX")
                        .param("page", "0").param("size", "100").with(user("gerente").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(100))
                .andReturn();
        assertThat(pagina0.getResponse().getHeader("X-Total-Count")).isEqualTo("130");
        mockMvc.perform(get(base).param("vigente", "true").param("ambiente", "SANDBOX")
                        .param("page", "1").param("size", "100").with(user("gerente").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(30))
                .andExpect(jsonPath("$[0].estado").value("RECHAZADO"))
                .andExpect(jsonPath("$[0].vigente").value(true))
                .andExpect(jsonPath("$[0].ambiente").value("SANDBOX"));
        // sin los filtros nuevos, el listado de siempre (por SP, sin la cabecera de total)
        var sinFiltro = mockMvc.perform(get(base).param("page", "0").param("size", "500")
                        .with(user("gerente").roles("ADMIN"))).andExpect(status().isOk()).andReturn();
        assertThat(sinFiltro.getResponse().getHeader("X-Total-Count")).isNull();
        // sin estado en la ruta: vigente y ambiente también se filtran
        var todosLosEstados = mockMvc.perform(get("/api/facturas/electronica/comprobantes/empresa/" + idEmpresa)
                        .param("vigente", "true").param("ambiente", "SANDBOX").param("size", "10")
                        .with(user("gerente").roles("ADMIN"))).andExpect(status().isOk()).andReturn();
        assertThat(todosLosEstados.getResponse().getHeader("X-Total-Count")).isEqualTo("132");
    }

    @Test
    @DisplayName("el listado de notas de crédito filtra por ambiente en el servidor y coincide con el contador")
    void listadoDeNotasPorAmbiente() {
        datosMixtos();
        for (AmbienteHacienda ambiente : new AmbienteHacienda[]{AmbienteHacienda.SANDBOX, AmbienteHacienda.PRODUCCION}) {
            activar(ambiente);
            long contador = panelService.obtener().contadores().notasEnTramite();
            var lista = notaConsultaService.listar(null, null, null, EstadoNotaFactura.EN_TRAMITE, ambiente, 0, 20);
            assertThat(lista.totalElementos()).as("%s", ambiente).isEqualTo(contador);
            assertThat(lista.contenido()).allSatisfy(fila -> assertThat(fila.ambiente()).isEqualTo(ambiente.name()));
        }
        assertThat(notaConsultaService.listar(null, null, null, EstadoNotaFactura.EN_TRAMITE, null, 0, 20)
                .totalElementos()).isEqualTo(3);
    }

    @Test
    @DisplayName("el enlace de anuladas que admiten sustituta lista exactamente las que cuenta el contador, cada una con su acción de sustituta")
    void enlaceDeAnuladasListaLasMismas() throws Exception {
        datosMixtos();
        Factura noElegible = factura(true);       // anulada sin nota: no cuenta ni se lista
        comprobante(noElegible, null, EstadoComprobante.ACEPTADO, true, AmbienteHacienda.SANDBOX);
        Factura yaTiene = anuladaElegible(AmbienteHacienda.SANDBOX);
        Factura sustituta = factura(false);
        sustituta.setFacturaSustituida(yaTiene);  // con sustituta no anulada: tampoco
        facturaRepository.save(sustituta);

        for (AmbienteHacienda activo : new AmbienteHacienda[]{AmbienteHacienda.SANDBOX, AmbienteHacienda.PRODUCCION, null}) {
            activar(activo);
            long contador = panelService.obtener().contadores().anuladasSinSustituta();
            var res = mockMvc.perform(get("/facturas").param("sustitutaPendiente", "true")
                            .with(user("gerente").roles("USER"))).andExpect(status().isOk()).andReturn();
            var modelo = res.getModelAndView().getModel();
            @SuppressWarnings("unchecked")
            List<Factura> listadas = (List<Factura>) modelo.get("facturas");

            assertThat(contador).as("contador con %s", activo).isEqualTo(activo == null ? 3 : activo == AmbienteHacienda.SANDBOX ? 2 : 1);
            assertThat(((Number) modelo.get("totalItems")).longValue()).as("total con %s", activo).isEqualTo(contador);
            assertThat(listadas).hasSize((int) contador);
            assertThat(listadas).noneMatch(f -> f.getIdFactura().equals(noElegible.getIdFactura()));
            assertThat(res.getResponse().getContentAsString()).contains("id=\"filtro-sustituta-pendiente\"");
            if (activo != null) {
                // la acción «Emitir factura sustituta» del detalle está disponible para todas las listadas
                var tx = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
                assertThat(listadas).allSatisfy(f -> assertThat(
                        Boolean.TRUE.equals(tx.execute(st -> sustitucionElegibilidad.puedeEmitirSustituta(f))))
                        .as("factura %s", f.getNumeroFactura()).isTrue());
            }
        }
    }
}
