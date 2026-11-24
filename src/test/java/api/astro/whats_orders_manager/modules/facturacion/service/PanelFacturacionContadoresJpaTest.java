package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.CertificadoHaciendaValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.producto.service.ArticuloMaestroService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-043 (criterios 3, 4 y 9): los contadores del panel con datos reales (H2): comprobantes vigentes por estado,
 * notas EN_TRAMITE, facturas sin comprobante y facturas anuladas que admiten sustituta (comparado contra
 * {@link SustitucionElegibilidad#evaluar} sobre el mismo conjunto); y el número de consultas de la página.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@DisplayName("T-043 — contadores del panel de Facturación (H2)")
class PanelFacturacionContadoresJpaTest {

    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;
    @Autowired private ComprobanteElectronicoRepository comprobanteRepository;
    @Autowired private NotaFacturaRepository notaRepository;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private api.astro.whats_orders_manager.modules.facturacion.correo.repository.EntregaComprobanteRepository entregaRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;

    private ConfiguracionEmpresa empresa;
    private Cliente cliente;
    private int secuencia;

    @BeforeEach
    void preparar() {
        empresa = empresaRepository.save(ConfiguracionEmpresa.builder().razonSocial("Empresa").build());
        cliente = new Cliente();
        em.persist(cliente);
    }

    private Factura factura(boolean anulada, Factura sustituye) {
        Factura f = new Factura();
        f.setCliente(cliente);
        f.setNumeroFactura("F-" + (++secuencia));
        f.setAnulada(anulada);
        f.setFacturaSustituida(sustituye);
        em.persist(f);
        return f;
    }

    private ComprobanteElectronico comprobante(Factura f, NotaFactura n, EstadoComprobante estado, boolean vigente) {
        return comprobante(f, n, estado, vigente, null);
    }

    private ComprobanteElectronico comprobante(Factura f, NotaFactura n, EstadoComprobante estado, boolean vigente,
                                               AmbienteHacienda ambiente) {
        int n1 = ++secuencia;
        ComprobanteElectronico c = ComprobanteElectronico.builder().factura(f).notaFactura(n).empresa(empresa)
            .tipoComprobante(n == null ? TipoComprobanteElectronico.FACTURA_ELECTRONICA
                : TipoComprobanteElectronico.NOTA_CREDITO)
            .claveNumerica("clave-" + n1).consecutivo("c" + n1).fechaEmision(LocalDateTime.now()).estado(estado)
            .vigente(vigente).ambiente(ambiente).consultasErrorHacienda(0).build();
        em.persist(c);
        return c;
    }

    private NotaFactura nota(Factura f, ComprobanteElectronico ref, TipoNotaFactura tipo, EstadoNotaFactura estado,
                             String codigo) {
        NotaFactura n = NotaFactura.builder().factura(f).tipo(tipo).codigoReferencia(codigo).razon("razón de prueba")
            .estado(estado).fechaEmision(LocalDateTime.now()).comprobanteReferencia(ref).claveReferencia(ref.getClaveNumerica())
            .fechaContable(java.time.LocalDate.now()).periodoFiscalAtribucion(java.time.YearMonth.now().toString())
            .fechaEmisionReferencia(LocalDateTime.now()).moneda("CRC").tipoCambio(BigDecimal.ONE)
            .subtotal(BigDecimal.ONE).descuento(BigDecimal.ZERO).impuesto(BigDecimal.ZERO).total(BigDecimal.ONE)
            .usuarioCreador("t043").createDate(LocalDateTime.now()).build();
        em.persist(n);
        return n;
    }

    /** Factura anulada con nota y comprobantes; devuelve la factura. */
    private Factura anulada(TipoNotaFactura tipo, EstadoNotaFactura estadoNota, String codigo,
                            EstadoComprobante estadoComprobanteNota, EstadoComprobante estadoOrigen) {
        return anulada(tipo, estadoNota, codigo, estadoComprobanteNota, estadoOrigen, null);
    }

    private Factura anulada(TipoNotaFactura tipo, EstadoNotaFactura estadoNota, String codigo,
                            EstadoComprobante estadoComprobanteNota, EstadoComprobante estadoOrigen,
                            AmbienteHacienda ambiente) {
        Factura f = factura(true, null);
        ComprobanteElectronico origen = comprobante(f, null, estadoOrigen, true, ambiente);
        NotaFactura n = nota(f, origen, tipo, estadoNota, codigo);
        comprobante(null, n, estadoComprobanteNota, true, ambiente);
        return f;
    }

    private PanelFacturacionService servicio() {
        ArticuloMaestroService articulos = mock(ArticuloMaestroService.class);
        ClienteService clientes = mock(ClienteService.class);
        when(articulos.countActivosSinCabys()).thenReturn(4L);
        when(clientes.contarClientesConDatosFeInvalidos()).thenReturn(2L);
        return new PanelFacturacionService(comprobanteRepository, notaRepository, facturaRepository, articulos,
            clientes, mock(ConfiguracionHaciendaService.class), empresaRepository, new CertificadoHaciendaValidator(),
            null, entregaRepository, java.time.Clock.systemDefaultZone());
    }

    @Test
    @DisplayName("comprobantes: solo vigentes, por estado; el historial no vigente no cuenta")
    void comprobantesVigentesPorEstado() {
        for (EstadoComprobante e : List.of(EstadoComprobante.ERROR, EstadoComprobante.ERROR,
            EstadoComprobante.NO_TRANSMITIDO, EstadoComprobante.ENVIO_INCIERTO, EstadoComprobante.RECHAZADO,
            EstadoComprobante.ACEPTADO)) {
            comprobante(factura(false, null), null, e, true);
        }
        // historial: un RECHAZADO y un ERROR no vigentes
        comprobante(factura(false, null), null, EstadoComprobante.RECHAZADO, false);
        comprobante(factura(false, null), null, EstadoComprobante.ERROR, false);
        em.flush();

        var c = servicio().contadores(null);

        assertThat(c.comprobantesError()).isEqualTo(2);
        assertThat(c.comprobantesNoTransmitido()).isEqualTo(1);
        assertThat(c.comprobantesEnvioIncierto()).isEqualTo(1);
        assertThat(c.comprobantesRechazado()).isEqualTo(1);
    }

    @Test
    @DisplayName("notas: solo EN_TRAMITE")
    void notasEnTramite() {
        Factura f = factura(false, null);
        ComprobanteElectronico ref = comprobante(f, null, EstadoComprobante.ACEPTADO, true);
        nota(f, ref, TipoNotaFactura.CREDITO, EstadoNotaFactura.EN_TRAMITE, "02");
        nota(f, ref, TipoNotaFactura.CREDITO, EstadoNotaFactura.EN_TRAMITE, "02");
        nota(f, ref, TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "02");
        nota(f, ref, TipoNotaFactura.CREDITO, EstadoNotaFactura.RECHAZADA, "02");
        em.flush();

        assertThat(servicio().contadores(null).notasEnTramite()).isEqualTo(2);
    }

    @Test
    @DisplayName("sin comprobante: no anuladas sin ningún comprobante (de cualquier ambiente); igual que /facturas?estadoFE=sin_fe")
    void sinComprobante() {
        factura(false, null);                                  // sin comprobante: cuenta
        factura(false, null);                                  // sin comprobante: cuenta
        factura(true, null);                                   // anulada sin comprobante: no cuenta
        comprobante(factura(false, null), null, EstadoComprobante.ACEPTADO, true, AmbienteHacienda.SANDBOX);     // con comprobante
        comprobante(factura(false, null), null, EstadoComprobante.ACEPTADO, true, AmbienteHacienda.PRODUCCION);  // de otro ambiente: no cuenta
        comprobante(factura(false, null), null, EstadoComprobante.RECHAZADO, false);  // solo historial no vigente: no cuenta
        em.flush();

        long delListado = facturaRepository.buscarConFiltros(null, null, null, null, Boolean.TRUE, null, null, null,
            org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        assertThat(servicio().contadores(null).sinComprobante()).isEqualTo(2).isEqualTo(delListado);
        // no depende del ambiente
        assertThat(servicio().contadores(AmbienteHacienda.PRODUCCION).sinComprobante()).isEqualTo(2);
    }

    @Test
    @DisplayName("review 1: comprobantes vigentes por estado cuentan solo el ambiente indicado (null = todos)")
    void comprobantesPorAmbiente() {
        comprobante(factura(false, null), null, EstadoComprobante.ERROR, true, AmbienteHacienda.PRODUCCION);
        comprobante(factura(false, null), null, EstadoComprobante.RECHAZADO, true, AmbienteHacienda.PRODUCCION);
        comprobante(factura(false, null), null, EstadoComprobante.ERROR, true, AmbienteHacienda.SANDBOX);
        comprobante(factura(false, null), null, EstadoComprobante.ERROR, true, AmbienteHacienda.SANDBOX);
        comprobante(factura(false, null), null, EstadoComprobante.RECHAZADO, true, AmbienteHacienda.SANDBOX);
        em.flush();

        var prod = servicio().contadores(AmbienteHacienda.PRODUCCION);
        var sand = servicio().contadores(AmbienteHacienda.SANDBOX);
        var todos = servicio().contadores(null);

        assertThat(prod.comprobantesError()).isEqualTo(1);
        assertThat(prod.comprobantesRechazado()).isEqualTo(1);
        assertThat(sand.comprobantesError()).isEqualTo(2);
        assertThat(sand.comprobantesRechazado()).isEqualTo(1);
        assertThat(todos.comprobantesError()).isEqualTo(3);
        assertThat(todos.comprobantesRechazado()).isEqualTo(2);
    }

    @Test
    @DisplayName("review 1: notas EN_TRAMITE cuentan por el ambiente de su comprobante vigente")
    void notasPorAmbiente() {
        Factura f = factura(false, null);
        ComprobanteElectronico ref = comprobante(f, null, EstadoComprobante.ACEPTADO, true, AmbienteHacienda.PRODUCCION);
        NotaFactura nProd = nota(f, ref, TipoNotaFactura.CREDITO, EstadoNotaFactura.EN_TRAMITE, "02");
        comprobante(null, nProd, EstadoComprobante.FIRMADO, true, AmbienteHacienda.PRODUCCION);
        NotaFactura nSand = nota(f, ref, TipoNotaFactura.CREDITO, EstadoNotaFactura.EN_TRAMITE, "02");
        comprobante(null, nSand, EstadoComprobante.FIRMADO, true, AmbienteHacienda.SANDBOX);
        NotaFactura nHistorial = nota(f, ref, TipoNotaFactura.CREDITO, EstadoNotaFactura.EN_TRAMITE, "02");
        comprobante(null, nHistorial, EstadoComprobante.RECHAZADO, false, AmbienteHacienda.SANDBOX);
        comprobante(null, nHistorial, EstadoComprobante.FIRMADO, true, AmbienteHacienda.PRODUCCION);
        em.flush();

        assertThat(servicio().contadores(AmbienteHacienda.PRODUCCION).notasEnTramite()).isEqualTo(2);
        assertThat(servicio().contadores(AmbienteHacienda.SANDBOX).notasEnTramite()).isEqualTo(1);
        assertThat(servicio().contadores(null).notasEnTramite()).isEqualTo(3);
    }

    @Test
    @DisplayName("review 1: anuladas que admiten sustituta por el ambiente del comprobante ACEPTADO de la original; la lista coincide")
    void anuladasPorAmbienteYListaCoincide() {
        anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "01", EstadoComprobante.ACEPTADO,
            EstadoComprobante.ACEPTADO, AmbienteHacienda.PRODUCCION);
        anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "13", EstadoComprobante.ACEPTADO,
            EstadoComprobante.ACEPTADO, AmbienteHacienda.SANDBOX);
        anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "01", EstadoComprobante.ACEPTADO,
            EstadoComprobante.ACEPTADO, AmbienteHacienda.SANDBOX);
        anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "01", EstadoComprobante.RECHAZADO,
            EstadoComprobante.ACEPTADO, AmbienteHacienda.SANDBOX); // no elegible
        em.flush();
        var sin = org.springframework.data.domain.Pageable.unpaged();

        for (var par : new Object[][]{{AmbienteHacienda.PRODUCCION, 1L}, {AmbienteHacienda.SANDBOX, 2L}, {null, 3L}}) {
            AmbienteHacienda ambiente = (AmbienteHacienda) par[0];
            long contador = servicio().contadores(ambiente).anuladasSinSustituta();
            long lista = facturaRepository.buscarConFiltros(null, null, null, null, null, null, Boolean.TRUE,
                ambiente, sin).getTotalElements();
            assertThat(contador).as("contador %s", ambiente).isEqualTo((Long) par[1]);
            assertThat(lista).as("lista %s", ambiente).isEqualTo(contador);
        }
    }

    @Test
    @DisplayName("anuladas que admiten sustituta: el COUNT coincide con SustitucionElegibilidad.evaluar")
    void sustitutaCoincideConLaRegla() {
        List<Factura> todas = new ArrayList<>();
        Factura elegible = anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "01",
            EstadoComprobante.ACEPTADO, EstadoComprobante.ACEPTADO);
        Factura elegible13 = anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "13",
            EstadoComprobante.ACEPTADO, EstadoComprobante.ACEPTADO);
        Factura yaTiene = anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "01",
            EstadoComprobante.ACEPTADO, EstadoComprobante.ACEPTADO);
        factura(false, yaTiene); // sustituta no anulada
        Factura sustitutaAnulada = anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "01",
            EstadoComprobante.ACEPTADO, EstadoComprobante.ACEPTADO);
        factura(true, sustitutaAnulada); // su sustituta está anulada: no cuenta como existente
        Factura notaNoAplicada = anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.EN_TRAMITE, "01",
            EstadoComprobante.ACEPTADO, EstadoComprobante.ACEPTADO);
        Factura codigoIncorrecto = anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "02",
            EstadoComprobante.ACEPTADO, EstadoComprobante.ACEPTADO);
        Factura notaSinAceptar = anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "01",
            EstadoComprobante.RECHAZADO, EstadoComprobante.ACEPTADO);
        Factura origenSinAceptar = anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "01",
            EstadoComprobante.ACEPTADO, EstadoComprobante.RECHAZADO);
        Factura notaDebito = anulada(TipoNotaFactura.DEBITO, EstadoNotaFactura.APLICADA, "01",
            EstadoComprobante.ACEPTADO, EstadoComprobante.ACEPTADO);
        Factura noAnulada = factura(false, null);
        ComprobanteElectronico origen = comprobante(noAnulada, null, EstadoComprobante.ACEPTADO, true);
        comprobante(null, nota(noAnulada, origen, TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "01"),
            EstadoComprobante.ACEPTADO, true);
        todas.addAll(List.of(elegible, elegible13, yaTiene, sustitutaAnulada, notaNoAplicada, codigoIncorrecto,
            notaSinAceptar, origenSinAceptar, notaDebito, noAnulada));
        em.flush();
        em.clear();

        var regla = new SustitucionElegibilidad(notaRepository, comprobanteRepository, facturaRepository,
            mock(AmbienteComprobanteGuard.class));
        long esperado = facturaRepository.findAll().stream().filter(f -> regla.evaluar(f).elegible()).count();

        assertThat(esperado).isEqualTo(3); // elegible, elegible13 y sustitutaAnulada (su sustituta está anulada)
        assertThat(servicio().contadores(null).anuladasSinSustituta()).isEqualTo(esperado);
        assertThat(regla.evaluar(facturaRepository.findById(elegible.getIdFactura()).orElseThrow()).elegible()).isTrue();
        assertThat(regla.evaluar(facturaRepository.findById(yaTiene.getIdFactura()).orElseThrow()).elegible()).isFalse();
    }

    @Test
    @DisplayName("la página hace un número acotado de consultas: 4 COUNT propios + 2 de entregas por correo (T-055), sin N+1")
    void consultasAcotadas() {
        for (int i = 0; i < 5; i++) {
            Factura f = anulada(TipoNotaFactura.CREDITO, EstadoNotaFactura.APLICADA, "01",
                EstadoComprobante.ACEPTADO, EstadoComprobante.ACEPTADO);
            comprobante(factura(false, null), null, EstadoComprobante.ERROR, true);
        }
        em.flush();
        em.clear();
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        var servicio = servicio();
        stats.clear();

        var c = servicio.contadores(null);

        assertThat(stats.getPrepareStatementCount()).isEqualTo(6);
        assertThat(stats.getEntityLoadCount()).isZero();
        assertThat(c.anuladasSinSustituta()).isEqualTo(5);
        assertThat(c.articulosSinCabys()).isEqualTo(4);
        assertThat(c.clientesFeInvalidos()).isEqualTo(2);
    }
}
