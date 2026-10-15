package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

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
        factura = new Factura();
        factura.setIdFactura(3);
        when(facturaRepository.findById(3)).thenReturn(Optional.of(factura));
        when(configuracionEmpresaService.obtenerConfiguracion()).thenReturn(Optional.of(empresaUnica));
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
    @DisplayName("si la configuración activa es de otra empresa, la emisión falla sin guardar nada")
    void emision_configuracionDeOtraEmpresa_falla() {
        ConfiguracionEmpresa otra = ConfiguracionEmpresa.builder().idConfiguracion(7).razonSocial("Otra").build();
        when(configuracionService.obtenerConfiguracionActiva()).thenReturn(sandboxDe(otra));

        assertThatThrownBy(() -> service.generarDesdeFactura(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pertenece a la empresa 7, no a la empresa 1");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("si no hay exactamente una configuración activa, la emisión propaga el error explícito")
    void emision_sinUnaActiva_falla() {
        when(configuracionService.obtenerConfiguracionActiva())
                .thenThrow(new IllegalStateException("Debe haber exactamente una configuración de Hacienda activa y hay 2 (ids 3, 4)."));

        assertThatThrownBy(() -> service.generarDesdeFactura(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ids 3, 4");
        verify(repository, never()).save(any());
    }
}
