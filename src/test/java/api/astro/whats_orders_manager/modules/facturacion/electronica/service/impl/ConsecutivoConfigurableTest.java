package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConsecutivoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-033 (criterio 16): el siguiente consecutivo de cada tipo se fija desde la configuración de Hacienda. Antes, la
 * pantalla enviaba los «consecutivos iniciales» pero crear los forzaba a 1 y actualizar los ignoraba, así que no había
 * forma de saltar los números ya usados en SANDBOX (rechazo -99).
 */
@DisplayName("T-033 — consecutivos configurables por tipo de comprobante")
class ConsecutivoConfigurableTest {

    private final ConfiguracionHaciendaRepository repository = mock(ConfiguracionHaciendaRepository.class);
    private final ConfiguracionEmpresaRepository empresaRepository = mock(ConfiguracionEmpresaRepository.class);
    private final ConfiguracionHaciendaServiceImpl service = new ConfiguracionHaciendaServiceImpl(repository,
            empresaRepository, new ConfiguracionHaciendaMapper(), mock(HaciendaApiService.class));

    private ConfiguracionHacienda guardada;

    @BeforeEach
    void setUp() {
        ConfiguracionEmpresa empresa = ConfiguracionEmpresa.builder().idConfiguracion(1).razonSocial("Empresa").build();
        guardada = ConfiguracionHacienda.builder().id(3L).empresa(empresa).ambiente(AmbienteHacienda.SANDBOX)
                .usuarioHacienda("cpj").passwordHacienda("x").activa(true)
                .consecutivoFactura(29L).consecutivoTiquete(1L).consecutivoNotaCredito(4L).consecutivoNotaDebito(1L)
                .build();
        when(repository.findById(3L)).thenReturn(Optional.of(guardada));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));
    }

    @Test
    @DisplayName("actualizar fija el siguiente consecutivo de cada tipo")
    void actualizar_fijaConsecutivos() {
        ConfiguracionHaciendaDTO dto = dto();
        dto.setConsecutivoFactura(1000L);
        dto.setConsecutivoNotaCredito(50L);

        service.actualizar(3L, dto);

        assertThat(guardada.getConsecutivoFactura()).isEqualTo(1000L);
        assertThat(guardada.getConsecutivoNotaCredito()).isEqualTo(50L);
        assertThat(guardada.siguienteConsecutivo("01")).isEqualTo(1000L);
    }

    @Test
    @DisplayName("sin valor, actualizar conserva el consecutivo")
    void actualizar_sinValor_conserva() {
        service.actualizar(3L, dto());

        assertThat(guardada.getConsecutivoFactura()).isEqualTo(29L);
        assertThat(guardada.getConsecutivoNotaCredito()).isEqualTo(4L);
    }

    @Test
    @DisplayName("crear usa los consecutivos iniciales pedidos (1 si no se indican)")
    void crear_usaLosIniciales() {
        ConfiguracionHaciendaDTO dto = dto();
        dto.setEmpresaId(1L);
        dto.setConsecutivoFactura(500L);

        service.crear(dto);

        ArgumentCaptor<ConfiguracionHacienda> creada = ArgumentCaptor.forClass(ConfiguracionHacienda.class);
        verify(repository).save(creada.capture());
        assertThat(creada.getValue().getConsecutivoFactura()).isEqualTo(500L);
        assertThat(creada.getValue().getConsecutivoTiquete()).isEqualTo(1L);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(longs = {0L, -5L, 10_000_000_000L})
    @DisplayName("fuera de 1..9 999 999 999 → 400 (ConsecutivoInvalidoException) y no se guarda")
    void fueraDeRango(long valor) {
        ConfiguracionHaciendaDTO dto = dto();
        dto.setConsecutivoFactura(valor);

        assertThatThrownBy(() -> service.actualizar(3L, dto)).isInstanceOf(ConsecutivoInvalidoException.class)
                .hasMessageContaining("factura");
        verify(repository, never()).save(any());
    }

    private static ConfiguracionHaciendaDTO dto() {
        ConfiguracionHaciendaDTO dto = new ConfiguracionHaciendaDTO();
        dto.setAmbiente(AmbienteHacienda.SANDBOX);
        dto.setUsuarioHacienda("cpj");
        dto.setPasswordHacienda(ConfiguracionHaciendaMapper.MASK);
        return dto;
    }
}
