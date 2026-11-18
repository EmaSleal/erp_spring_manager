package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.EmisionNoPermitidaException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.XmlGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.ClaveNumericaGenerator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.DatosFeDePrueba;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.SustitucionElegibilidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-039 (criterio 6, ADR-045 §3): etapa a de la emisión de una sustituta. Si la original ya no cumple las condiciones, la
 * emisión responde 409 sin consumir consecutivo ni guardar el comprobante.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("T-039 — etapa a: la sustituta se revisa antes del consecutivo")
class SustitutaEtapaATest {

    @Mock private ComprobanteElectronicoRepository repository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private ConfiguracionHaciendaService configuracionService;
    @Mock private ConfiguracionEmpresaService configuracionEmpresaService;
    @Mock private XmlGeneratorService xmlGenerator;
    @Mock private ClaveNumericaGenerator claveGenerator;
    @Mock private ComprobanteElectronicoMapper mapper;
    @org.mockito.Spy private api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConsecutivoHacienda
            consecutivos = new EmisionConfiguracionActivaTest.ConsecutivoSinBloqueo();
    @org.mockito.Spy private api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.DatosFeValidator
            datosFeValidator = api.astro.whats_orders_manager.modules.facturacion.electronica.validacion
            .CatalogoUbicacionDePrueba.validador();

    @InjectMocks private ComprobanteElectronicoServiceImpl service;

    private SustitucionElegibilidad elegibilidad;
    private Factura sustituta;
    private Factura original;
    private ConfiguracionHacienda sandbox;

    @BeforeEach
    void preparar() {
        ConfiguracionEmpresa empresa = ConfiguracionEmpresa.builder().idConfiguracion(1).razonSocial("EMPRESA")
                .numeroIdentificacion("118200878").codigoActividad("4773.0").build();
        DatosFeDePrueba.emisor(empresa);
        original = new Factura();
        original.setIdFactura(2);
        original.setNumeroFactura("FA01-00002");
        sustituta = new Factura();
        sustituta.setIdFactura(3);
        sustituta.setFacturaSustituida(original);
        sustituta.setCodigoSustitucion("15");
        sustituta.setRazonSustitucion("Sustituye la factura FA01-00002");
        when(facturaRepository.findById(3)).thenReturn(Optional.of(sustituta));
        when(configuracionEmpresaService.obtenerConfiguracion()).thenReturn(Optional.of(empresa));
        sandbox = ConfiguracionHacienda.builder().id(3L).empresa(empresa).ambiente(AmbienteHacienda.SANDBOX)
                .activa(true).consecutivoFactura(26L).build();
        when(configuracionService.obtenerConfiguracionActiva()).thenReturn(sandbox);
        when(claveGenerator.generar(any(), any(), any(), any())).thenReturn("50601");
        when(xmlGenerator.generarXmlFactura(any())).thenReturn("<FacturaElectronica/>");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ReflectionTestUtils.setField(service, "clock", Clock.systemUTC());
        elegibilidad = mock(SustitucionElegibilidad.class);
        ReflectionTestUtils.setField(service, "sustitucionElegibilidad", elegibilidad);
    }

    @Test
    @DisplayName("la original ya no cumple: 409 fijo, el consecutivo no cambia y no se guarda el comprobante")
    void originalYaNoCumple_409SinConsumirConsecutivo() {
        when(elegibilidad.evaluarSinSustituta(original)).thenReturn(
                new SustitucionElegibilidad.Evaluacion(false, "no importa", null, null, null, null));

        assertThatThrownBy(() -> service.generarDesdeFactura(3L))
                .isInstanceOf(EmisionNoPermitidaException.class)
                .hasMessage(SustitucionElegibilidad.MENSAJE_ORIGINAL_YA_NO_CUMPLE);

        assertThat(sandbox.getConsecutivoFactura()).isEqualTo(26L);
        verify(consecutivos, never()).siguiente(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("la original cumple: la sustituta se emite y toma el consecutivo")
    void originalCumple_seEmite() {
        when(elegibilidad.evaluarSinSustituta(original)).thenReturn(
                new SustitucionElegibilidad.Evaluacion(true, null, null, "N", "15", null));

        service.generarDesdeFactura(3L);

        assertThat(sandbox.getConsecutivoFactura()).isEqualTo(27L);
        verify(repository).save(any());
    }

    @Test
    @DisplayName("una factura normal no consulta la regla de sustitución")
    void facturaNormal_noConsulta() {
        sustituta.setFacturaSustituida(null);

        service.generarDesdeFactura(3L);

        verify(elegibilidad, never()).evaluarSinSustituta(any());
        verify(repository).save(any());
    }
}
