package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
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
        int n1 = ++secuencia;
        ComprobanteElectronico c = ComprobanteElectronico.builder().factura(f).notaFactura(n).empresa(empresa)
            .tipoComprobante(n == null ? TipoComprobanteElectronico.FACTURA_ELECTRONICA
                : TipoComprobanteElectronico.NOTA_CREDITO)
            .claveNumerica("clave-" + n1).consecutivo("c" + n1).fechaEmision(LocalDateTime.now()).estado(estado)
            .vigente(vigente).consultasErrorHacienda(0).build();
        em.persist(c);
        return c;
    }

    private NotaFactura nota(Factura f, ComprobanteElectronico ref, TipoNotaFactura tipo, EstadoNotaFactura estado,
                             String codigo) {
        NotaFactura n = NotaFactura.builder().factura(f).tipo(tipo).codigoReferencia(codigo).razon("razón de prueba")
            .estado(estado).fechaEmision(LocalDateTime.now()).comprobanteReferencia(ref).claveReferencia(ref.getClaveNumerica())
            .fechaEmisionReferencia(LocalDateTime.now()).moneda("CRC").tipoCambio(BigDecimal.ONE)
            .subtotal(BigDecimal.ONE).descuento(BigDecimal.ZERO).impuesto(BigDecimal.ZERO).total(BigDecimal.ONE)
            .usuarioCreador("t043").createDate(LocalDateTime.now()).build();
        em.persist(n);
        return n;
    }

    /** Factura anulada con nota y comprobantes; devuelve la factura. */
    private Factura anulada(TipoNotaFactura tipo, EstadoNotaFactura estadoNota, String codigo,
                            EstadoComprobante estadoComprobanteNota, EstadoComprobante estadoOrigen) {
        Factura f = factura(true, null);
        ComprobanteElectronico origen = comprobante(f, null, estadoOrigen, true);
        NotaFactura n = nota(f, origen, tipo, estadoNota, codigo);
        comprobante(null, n, estadoComprobanteNota, true);
        return f;
    }

    private PanelFacturacionService servicio() {
        ArticuloMaestroService articulos = mock(ArticuloMaestroService.class);
        ClienteService clientes = mock(ClienteService.class);
        when(articulos.countActivosSinCabys()).thenReturn(4L);
        when(clientes.contarClientesConDatosFeInvalidos()).thenReturn(2L);
        return new PanelFacturacionService(comprobanteRepository, notaRepository, facturaRepository, articulos,
            clientes, mock(ConfiguracionHaciendaService.class), empresaRepository, new CertificadoHaciendaValidator(),
            null);
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

        var c = servicio().contadores();

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

        assertThat(servicio().contadores().notasEnTramite()).isEqualTo(2);
    }

    @Test
    @DisplayName("sin comprobante: igual que /facturas?estadoFE=sin_fe (sin comprobante vigente; incluye anuladas)")
    void sinComprobante() {
        factura(false, null);                                  // sin comprobante
        factura(true, null);                                   // anulada sin comprobante: el listado la incluye
        comprobante(factura(false, null), null, EstadoComprobante.ACEPTADO, true);   // con vigente
        comprobante(factura(false, null), null, EstadoComprobante.RECHAZADO, false); // solo historial no vigente
        em.flush();

        long delListado = facturaRepository.buscarConFiltros(null, null, null, null, Boolean.TRUE, null,
            org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        assertThat(servicio().contadores().sinComprobante()).isEqualTo(3).isEqualTo(delListado);
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
        assertThat(servicio().contadores().anuladasSinSustituta()).isEqualTo(esperado);
        assertThat(regla.evaluar(facturaRepository.findById(elegible.getIdFactura()).orElseThrow()).elegible()).isTrue();
        assertThat(regla.evaluar(facturaRepository.findById(yaTiene.getIdFactura()).orElseThrow()).elegible()).isFalse();
    }

    @Test
    @DisplayName("la página hace un número acotado de consultas: 4 COUNT propios, sin N+1")
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

        var c = servicio.contadores();

        assertThat(stats.getPrepareStatementCount()).isEqualTo(4);
        assertThat(stats.getEntityLoadCount()).isZero();
        assertThat(c.anuladasSinSustituta()).isEqualTo(5);
        assertThat(c.articulosSinCabys()).isEqualTo(4);
        assertThat(c.clientesFeInvalidos()).isEqualTo(2);
    }
}
