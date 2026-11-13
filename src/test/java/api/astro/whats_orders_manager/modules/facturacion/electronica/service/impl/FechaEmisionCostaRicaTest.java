package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ProductosSinCabysException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.ClaveNumericaGenerator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.HoraFiscal;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.CalculadoraImpuestoService;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-033 (criterios 1 a 3 y 14): la fecha de emisión y el segmento de fecha de la clave salen del mismo reloj, en hora
 * de Costa Rica, con la JVM en UTC (como el contenedor de producción). Rechazo -53 de Hacienda en SANDBOX: el
 * comprobante del 2026-11-13T03:22:23Z se emitió con FechaEmision 03:22:23-06:00 (6 horas en el futuro) y clave 131126.
 * Sin CAByS no se emite: no se toma consecutivo ni se crea comprobante.
 */
@DisplayName("T-033 — fecha de emisión y clave en hora de Costa Rica")
@SuppressWarnings("deprecation")
class FechaEmisionCostaRicaTest {

    private static final Instant INSTANTE_SANDBOX = Instant.parse("2026-11-13T03:22:23Z");

    private final ComprobanteElectronicoRepository repository = mock(ComprobanteElectronicoRepository.class);
    private final FacturaRepository facturaRepository = mock(FacturaRepository.class);
    private final ConfiguracionHaciendaService configuracionService = mock(ConfiguracionHaciendaService.class);
    private final ConfiguracionEmpresaService empresaService = mock(ConfiguracionEmpresaService.class);
    private final XmlGeneratorServiceImpl generador = new XmlGeneratorServiceImpl(new CalculadoraImpuestoService(null));

    private TimeZone zonaOriginal;
    private ConfiguracionHacienda config;
    private Factura factura;

    @BeforeEach
    void setUp() {
        zonaOriginal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // JVM en UTC, como el contenedor de producción
        ConfiguracionEmpresa empresa = ConfiguracionEmpresa.builder()
                .idConfiguracion(1).razonSocial("EMANUEL SOTO LEAL").numeroIdentificacion("118200878")
                .codigoActividad("4773.0").codigoProvincia("1").canton("01").distrito("01")
                .otrasSenas("Avenida Central, San José").email("emisor@t033.example").build();
        config = ConfiguracionHacienda.builder().id(3L).empresa(empresa).ambiente(AmbienteHacienda.SANDBOX)
                .activa(true).consecutivoFactura(28L).build();
        when(empresaService.obtenerConfiguracion()).thenReturn(Optional.of(empresa));
        when(configuracionService.obtenerConfiguracionActiva()).thenReturn(config);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        factura = new Factura();
        factura.setIdFactura(3);
        factura.getLineas().add(linea(producto("1234567890123")));
        when(facturaRepository.findById(3)).thenReturn(Optional.of(factura));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(zonaOriginal);
    }

    @Test
    @DisplayName("a) instante 2026-11-13T03:22:23Z → FechaEmision 2026-11-12T21:22:23-06:00 en el comprobante y en el XML")
    void fechaDeEmision_enHoraDeCostaRica() {
        ComprobanteElectronico comprobante = emitirEn(INSTANTE_SANDBOX);

        assertThat(comprobante.getFechaEmision()).isEqualTo(LocalDateTime.of(2026, 11, 12, 21, 22, 23));
        assertThat(comprobante.getXmlComprobante())
                .contains("<FechaEmision>2026-11-12T21:22:23-06:00</FechaEmision>");
        assertThat(HoraFiscal.formatear(comprobante.getFechaEmision())).isEqualTo("2026-11-12T21:22:23-06:00");
    }

    @Test
    @DisplayName("b) la clave de ese comprobante lleva 121126, no 131126")
    void clave_segmentoDeFechaDeCostaRica() {
        ComprobanteElectronico comprobante = emitirEn(INSTANTE_SANDBOX);

        assertThat(comprobante.getClaveNumerica().substring(3, 9)).isEqualTo("121126");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"2026-11-12T05:59:59Z", "2026-11-12T06:00:00Z", "2026-11-12T17:30:00Z",
            "2026-11-13T00:00:00Z", "2026-11-13T03:22:23Z", "2026-11-13T05:59:59Z", "2026-12-31T23:30:00Z",
            "2027-01-01T05:59:59Z"})
    @DisplayName("c) la fecha de la clave coincide con la fecha de emisión y con el día de Costa Rica (incluye 18:00-23:59 CR)")
    void claveYFechaCoinciden(String instante) {
        Instant momento = Instant.parse(instante);
        ComprobanteElectronico comprobante = emitirEn(momento);

        String diaCostaRica = momento.atZone(HoraFiscal.ZONA).format(DateTimeFormatter.ofPattern("ddMMyy"));
        assertThat(comprobante.getClaveNumerica().substring(3, 9))
                .isEqualTo(comprobante.getFechaEmision().format(DateTimeFormatter.ofPattern("ddMMyy")))
                .isEqualTo(diaCostaRica);
    }

    @Test
    @DisplayName("HoraFiscal no depende de la zona del reloj: un reloj en UTC y otro en Asia/Tokyo dan la misma hora")
    void horaFiscal_independienteDeLaZonaDelReloj() {
        LocalDateTime utc = HoraFiscal.ahora(Clock.fixed(INSTANTE_SANDBOX, ZoneOffset.UTC));
        LocalDateTime tokio = HoraFiscal.ahora(Clock.fixed(INSTANTE_SANDBOX, ZoneId.of("Asia/Tokyo")));

        assertThat(utc).isEqualTo(tokio).isEqualTo(LocalDateTime.of(2026, 11, 12, 21, 22, 23));
    }

    @Test
    @DisplayName("producto sin CAByS → error que lo nombra, sin consecutivo ni comprobante (criterio 14)")
    void sinCabys_noConsumeConsecutivo() {
        Producto sinCabys = producto(null);
        sinCabys.setDescripcion("Servicio de instalación");
        factura.getLineas().add(linea(sinCabys));

        assertThatThrownBy(() -> emitirEn(INSTANTE_SANDBOX))
                .isInstanceOf(ProductosSinCabysException.class)
                .hasMessageContaining("Servicio de instalación");

        assertThat(config.getConsecutivoFactura()).isEqualTo(28L);
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
    }

    private ComprobanteElectronico emitirEn(Instant instante) {
        ComprobanteElectronicoServiceImpl service = new ComprobanteElectronicoServiceImpl(repository,
                facturaRepository, null, configuracionService, empresaService, generador, null, null,
                new ClaveNumericaGenerator(), mock(ComprobanteElectronicoMapper.class), null, null,
                Clock.fixed(instante, ZoneOffset.UTC), null, null);
        service.generarDesdeFactura(3L);
        ArgumentCaptor<ComprobanteElectronico> guardado = ArgumentCaptor.forClass(ComprobanteElectronico.class);
        verify(repository, atLeastOnce()).save(guardado.capture());
        clearInvocations(repository);
        return guardado.getValue();
    }

    private static Producto producto(String cabys) {
        Producto p = new Producto();
        p.setDescripcion("Producto T-033");
        p.setCodigoCabys(cabys);
        p.setGravado(true);
        p.setPorcentajeImpuesto(new BigDecimal("13"));
        p.setPrecioIncluyeIva(true);
        return p;
    }

    private LineaFactura linea(Producto producto) {
        LineaFactura linea = new LineaFactura();
        linea.setFactura(factura);
        linea.setProducto(producto);
        linea.setCantidad(1);
        linea.setSubtotal(new BigDecimal("1130.00"));
        return linea;
    }
}
