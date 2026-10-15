package api.astro.whats_orders_manager.modules.facturacion.electronica.service;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.configuracion.service.impl.ConfiguracionEmpresaServiceImpl;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ConfiguracionHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ConfiguracionHaciendaServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * T-018 (ADR-021) — identidad fiscal única y exactamente una configuración de Hacienda activa (H2, JPA).
 * Sin transacción de prueba: cada llamada al servicio confirma como en producción. Las credenciales del
 * fixture son valores de prueba; HaciendaApiService es un mock (sin llamadas a Hacienda).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({ConfiguracionHaciendaServiceImpl.class, ConfiguracionHaciendaMapper.class, ConfiguracionEmpresaServiceImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-018 — identidad fiscal única y una sola configuración de Hacienda activa (H2)")
class IdentidadFiscalUnicaJpaTest {

    @MockitoBean private HaciendaApiService haciendaApiService;

    @Autowired private ConfiguracionHaciendaService haciendaService;
    @Autowired private ConfiguracionEmpresaService empresaService;
    @Autowired private ConfiguracionHaciendaRepository haciendaRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;

    private ConfiguracionEmpresa empresa;

    @BeforeEach
    void preparar() {
        empresa = empresaRepository.save(ConfiguracionEmpresa.builder()
                .razonSocial("EMANUEL SOTO LEAL")
                .nombreComercial("MONRACHEM")
                .codigoActividad("4773.0")
                .build());
        when(haciendaApiService.autenticar(anyLong())).thenThrow(new IllegalStateException("sin red en la prueba"));
    }

    @AfterEach
    void limpiar() {
        haciendaRepository.deleteAll();
        empresaRepository.deleteAll();
    }

    private ConfiguracionHacienda configuracion(AmbienteHacienda ambiente, boolean activa) {
        return haciendaRepository.save(ConfiguracionHacienda.builder()
                .empresa(empresa)
                .ambiente(ambiente)
                .usuarioHacienda("usuario-de-prueba")
                .passwordHacienda("clave-de-prueba")
                .rutaCertificado("/tmp/certificado-de-prueba.p12")
                .pinCertificado("0000")
                .activa(activa)
                .build());
    }

    private boolean activa(Long id) {
        return haciendaRepository.findById(id).orElseThrow().getActiva();
    }

    @Test
    @DisplayName("obtenerConfiguracionActiva sin ninguna activa → IllegalStateException")
    void ningunaActiva_lanza() {
        configuracion(AmbienteHacienda.SANDBOX, false);

        assertThatThrownBy(() -> haciendaService.obtenerConfiguracionActiva())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay ninguna configuración de Hacienda activa");
    }

    @Test
    @DisplayName("obtenerConfiguracionActiva con una activa → la devuelve")
    void unaActiva_laDevuelve() {
        ConfiguracionHacienda sandbox = configuracion(AmbienteHacienda.SANDBOX, true);
        configuracion(AmbienteHacienda.PRODUCCION, false);

        assertThat(haciendaService.obtenerConfiguracionActiva().getId()).isEqualTo(sandbox.getId());
    }

    @Test
    @DisplayName("obtenerConfiguracionActiva con dos activas → IllegalStateException con cuántas y sus ids")
    void dosActivas_lanzaConIds() {
        ConfiguracionHacienda sandbox = configuracion(AmbienteHacienda.SANDBOX, true);
        ConfiguracionHacienda produccion = configuracion(AmbienteHacienda.PRODUCCION, true);

        assertThatThrownBy(() -> haciendaService.obtenerConfiguracionActiva())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hay 2")
                .hasMessageContaining("ids " + sandbox.getId() + ", " + produccion.getId());
    }

    @Test
    @DisplayName("activar una configuración desactiva la otra en la misma operación")
    void activar_desactivaLaOtra() {
        ConfiguracionHacienda sandbox = configuracion(AmbienteHacienda.SANDBOX, true);
        ConfiguracionHacienda produccion = configuracion(AmbienteHacienda.PRODUCCION, false);

        haciendaService.activar(produccion.getId());

        assertThat(activa(produccion.getId())).isTrue();
        assertThat(activa(sandbox.getId())).isFalse();
        assertThat(haciendaService.obtenerConfiguracionActiva().getId()).isEqualTo(produccion.getId());

        haciendaService.activar(sandbox.getId());

        assertThat(activa(sandbox.getId())).isTrue();
        assertThat(activa(produccion.getId())).isFalse();
    }

    @Test
    @DisplayName("crear una configuración marcada como activa desactiva las demás")
    void crearActiva_desactivaLasDemas() {
        ConfiguracionHacienda sandbox = configuracion(AmbienteHacienda.SANDBOX, true);

        ConfiguracionHaciendaDTO dto = new ConfiguracionHaciendaDTO();
        dto.setEmpresaId(empresa.getIdConfiguracion().longValue());
        dto.setAmbiente(AmbienteHacienda.PRODUCCION);
        dto.setUsuarioHacienda("usuario-de-prueba");
        dto.setPasswordHacienda("clave-de-prueba");
        dto.setRutaCertificado("/tmp/certificado-de-prueba.p12");
        dto.setActiva(true);

        ConfiguracionHaciendaDTO creada = haciendaService.crear(dto);

        assertThat(activa(creada.getId())).isTrue();
        assertThat(activa(sandbox.getId())).isFalse();
        assertThat(haciendaRepository.findByActivaTrueOrderByIdAsc()).hasSize(1);
    }

    @Test
    @DisplayName("crear una segunda configuracion_empresa se rechaza; obtenerConfiguracion devuelve la única")
    void segundaConfiguracionEmpresa_seRechaza() {
        ConfiguracionEmpresa otra = ConfiguracionEmpresa.builder()
                .razonSocial("Otra Empresa S.A.")
                .nombreComercial("Otra")
                .build();

        assertThatThrownBy(() -> empresaService.guardarConfiguracion(otra))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe una configuración de empresa");

        assertThat(empresaRepository.count()).isEqualTo(1);
        assertThat(empresaService.obtenerConfiguracion()).get()
                .extracting(ConfiguracionEmpresa::getIdConfiguracion).isEqualTo(empresa.getIdConfiguracion());
        assertThat(empresaService.obtenerOCrearConfiguracion().getIdConfiguracion()).isEqualTo(empresa.getIdConfiguracion());
        assertThat(empresaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("guardar la configuracion_empresa existente sigue funcionando")
    void guardarExistente_actualiza() {
        empresa.setCodigoActividad("4773.0");
        empresa.setRazonSocial("EMANUEL SOTO LEAL (actualizada)");

        empresaService.guardarConfiguracion(empresa);

        assertThat(empresaRepository.count()).isEqualTo(1);
        assertThat(empresaRepository.findById(empresa.getIdConfiguracion()).orElseThrow().getRazonSocial())
                .isEqualTo("EMANUEL SOTO LEAL (actualizada)");
    }
}
