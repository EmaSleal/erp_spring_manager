package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.DatosFeDePrueba;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.XmlGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.ClaveNumericaGenerator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConfiguracionHaciendaNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.EmisionNoPermitidaException;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-018 (ADR-021) — la emisión usa la única configuración de Hacienda activa y asocia el comprobante a la
 * empresa única; si la configuración activa es de otra empresa, falla de forma explícita.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T-018 — emisión con la configuración de Hacienda activa y la empresa única")
class EmisionConfiguracionActivaTest {

    @Mock private ComprobanteElectronicoRepository repository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private ConfiguracionHaciendaService configuracionService;
    @Mock private ConfiguracionEmpresaService configuracionEmpresaService;
    @Mock private XmlGeneratorService xmlGenerator;
    @Mock private ClaveNumericaGenerator claveGenerator;
    @Mock private ComprobanteElectronicoMapper mapper;
    @Mock private api.astro.whats_orders_manager.modules.facturacion.electronica.service.ValidadorXsdComprobante validadorXsd;
    @org.mockito.Spy private api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConsecutivoHacienda
            consecutivos = new ConsecutivoSinBloqueo();

    @org.mockito.Spy private api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.DatosFeValidator
            datosFeValidator = api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionDePrueba.validador();

    @InjectMocks private ComprobanteElectronicoServiceImpl service;

    private ConfiguracionEmpresa empresaUnica;
    private Factura factura;

    @BeforeEach
    void preparar() {
        empresaUnica = ConfiguracionEmpresa.builder()
                .idConfiguracion(1)
                .razonSocial("EMANUEL SOTO LEAL")
                .numeroIdentificacion("118200878")
                .codigoActividad("4773.0")
                .build();
        DatosFeDePrueba.emisor(empresaUnica);
        factura = new Factura();
        factura.setIdFactura(3);
        org.mockito.Mockito.lenient().when(facturaRepository.findById(3)).thenReturn(Optional.of(factura));
        org.mockito.Mockito.lenient().when(configuracionEmpresaService.obtenerConfiguracion()).thenReturn(Optional.of(empresaUnica));
        // T-033: la fecha de emisión sale del reloj de la aplicación (HoraFiscal)
        ReflectionTestUtils.setField(service, "clock", Clock.systemUTC());
    }

    private ConfiguracionHacienda sandboxDe(ConfiguracionEmpresa empresa) {
        return ConfiguracionHacienda.builder()
                .id(3L)
                .empresa(empresa)
                .ambiente(AmbienteHacienda.SANDBOX)
                .activa(true)
                .consecutivoFactura(26L)
                .build();
    }

    @Test
    @DisplayName("usa la configuración activa, toma su consecutivo y asocia el comprobante a la empresa única")
    void emision_usaLaActivaYLaEmpresaUnica() {
        ConfiguracionHacienda sandbox = sandboxDe(empresaUnica);
        when(configuracionService.obtenerConfiguracionActiva()).thenReturn(sandbox);
        when(claveGenerator.generar(eq("118200878"), eq(26L), eq(TipoComprobanteElectronico.FACTURA_ELECTRONICA), any()))
                .thenReturn("50601");
        when(xmlGenerator.generarXmlFactura(any())).thenReturn("<FacturaElectronica/>");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.generarDesdeFactura(3L);

        ArgumentCaptor<ComprobanteElectronico> guardado = ArgumentCaptor.forClass(ComprobanteElectronico.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getEmpresa()).isSameAs(empresaUnica);
        assertThat(guardado.getValue().getConsecutivo()).isEqualTo(String.format("%020d", 26L));
        assertThat(guardado.getValue().getClaveNumerica()).isEqualTo("50601");
        assertThat(sandbox.getConsecutivoFactura()).isEqualTo(27L);
        verify(configuracionService).obtenerConfiguracionActiva();
    }

    @Test
    @DisplayName("si la configuración activa es de otra empresa, la emisión se rechaza (409) con mensaje fijo y sin guardar nada")
    void emision_configuracionDeOtraEmpresa_falla() {
        ConfiguracionEmpresa otra = ConfiguracionEmpresa.builder().idConfiguracion(7).razonSocial("Otra").build();
        when(configuracionService.obtenerConfiguracionActiva()).thenReturn(sandboxDe(otra));

        assertThatThrownBy(() -> service.generarDesdeFactura(3L))
                .isInstanceOf(EmisionNoPermitidaException.class)
                .hasMessage(ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA)
                .hasMessageNotContaining("empresa 7");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("si no hay exactamente una configuración activa, la emisión se rechaza con el mensaje fijo, sin el detalle")
    void emision_sinUnaActiva_falla() {
        when(configuracionService.obtenerConfiguracionActiva()).thenThrow(new ConfiguracionHaciendaNoDisponibleException(
                "Debe haber exactamente una configuración de Hacienda activa y hay 2 (ids 3, 4).",
                ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA));

        assertThatThrownBy(() -> service.generarDesdeFactura(3L))
                .isInstanceOf(EmisionNoPermitidaException.class)
                .hasMessage(ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("T-041: credenciales que no se descifran → la emisión de la factura se rechaza con el mensaje fijo, sin consecutivo ni comprobante")
    void emision_credencialesIlegibles_mensajeFijoSinConsumirConsecutivo() {
        ConfiguracionHacienda sandbox = sandboxDe(empresaUnica);
        when(configuracionService.obtenerConfiguracionActiva()).thenThrow(new RuntimeException(
                "Error attempting to apply AttributeConverter",
                new IllegalStateException("Credencial cifrada alterada o cifrada con otra clave (HACIENDA_CRYPTO_KEY)")));

        assertThatThrownBy(() -> service.generarDesdeFactura(3L))
                .isInstanceOf(EmisionNoPermitidaException.class)
                .hasMessage(ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE);
        verify(repository, never()).save(any());
        assertThat(sandbox.getConsecutivoFactura()).isEqualTo(26L);
    }

    @Test
    @DisplayName("T-041: credenciales que no se descifran → la nota de crédito se rechaza con el mensaje fijo (no 500), sin guardar nada")
    void nota_credencialesIlegibles_mensajeFijo() {
        Factura factura = new Factura();
        factura.setIdFactura(3);
        NotaFactura nota = new NotaFactura();
        nota.setId(5L);
        nota.setTipo(TipoNotaFactura.CREDITO);
        nota.setFactura(factura);
        when(configuracionService.obtenerConfiguracionActiva()).thenThrow(new RuntimeException(
                "Error attempting to apply AttributeConverter", new IllegalStateException("alterada")));

        assertThatThrownBy(() -> service.generarYFirmarDesdeNota(nota))
                .isInstanceOf(EmisionNoPermitidaException.class)
                .hasMessage(ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("T-041: prepararParaEnvio con credenciales ilegibles → ERROR, mensaje fijo sin el texto de la excepción, un intento más")
    void prepararParaEnvio_credencialesIlegibles_errorLocalConIntento() {
        when(validadorXsd.validar(any(), any())).thenReturn(java.util.List.of());
        when(configuracionService.obtenerConfiguracionActiva()).thenThrow(new RuntimeException(
                "Error attempting to apply AttributeConverter", new IllegalStateException("texto-interno-de-la-causa")));
        ComprobanteElectronico comprobante = comprobanteNoTransmitido();

        Long configuracionId = service.prepararParaEnvio(comprobante);

        assertThat(configuracionId).isNull();
        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobante.ERROR);
        assertThat(comprobante.getUltimoError()).isEqualTo(ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE);
        assertThat(comprobante.getIntentosEnvio()).isEqualTo(1);
    }

    @Test
    @DisplayName("T-041: prepararParaEnvio sin configuración activa → ERROR con mensaje fijo, no con el texto de la excepción")
    void prepararParaEnvio_sinConfiguracionActiva_mensajeFijo() {
        when(validadorXsd.validar(any(), any())).thenReturn(java.util.List.of());
        when(configuracionService.obtenerConfiguracionActiva()).thenThrow(new ConfiguracionHaciendaNoDisponibleException(
                "No hay ninguna configuración de Hacienda activa; detalle interno",
                ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA));
        ComprobanteElectronico comprobante = comprobanteNoTransmitido();

        assertThat(service.prepararParaEnvio(comprobante)).isNull();

        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobante.ERROR);
        assertThat(comprobante.getUltimoError()).isEqualTo(ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA);
        assertThat(comprobante.getIntentosEnvio()).isEqualTo(1);
    }

    private ComprobanteElectronico comprobanteNoTransmitido() {
        return ComprobanteElectronico.builder().claveNumerica("50601").empresa(empresaUnica)
                .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA)
                .estado(EstadoComprobante.NO_TRANSMITIDO).xmlComprobante("<FacturaElectronica/>").build();
    }

    /** El cálculo del consecutivo sin la BD (el bloqueo lo prueban las IT con MySQL). */
    static class ConsecutivoSinBloqueo
            implements api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConsecutivoHacienda {
        @Override
        public Long siguiente(api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda c,
                              String tipo) {
            return c.siguienteConsecutivo(tipo);
        }
    }
}
