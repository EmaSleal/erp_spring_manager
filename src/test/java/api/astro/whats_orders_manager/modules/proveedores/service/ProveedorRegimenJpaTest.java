package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO.ResultadoConsulta;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaRegimenDTO;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.proveedores.dto.ProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.RegimenHacienda;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.exception.RegimenNoVerificableException;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.service.impl.ProveedorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T-058 criterios 3 a 5 (ADR-074 §2): el régimen del proveedor se consulta en /fe/ae al crearlo, al cambiarle la cédula y
 * con «Verificar régimen»; si la consulta falla el proveedor se guarda SIN_VERIFICAR; y la consulta nunca corre dentro de una
 * transacción de base de datos. Hoy es 2027-03-01 10:00 (Clock fijo).
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-only",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Import({ProveedorServiceImpl.class, ProveedorRegimenJpaTest.RelojFijo.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("T-058 — régimen del proveedor: consulta /fe/ae fuera de la transacción (H2)")
class ProveedorRegimenJpaTest {

    static final LocalDateTime AHORA = LocalDateTime.of(2027, 3, 1, 10, 0);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock clock() {
            return Clock.fixed(AHORA.atZone(ZoneId.of("America/Costa_Rica")).toInstant(), ZoneId.of("America/Costa_Rica"));
        }
    }

    @Autowired private ProveedorService service;
    @Autowired private ProveedorRepository repository;
    @MockitoBean private HaciendaApiService hacienda;

    @AfterEach
    void limpiar() {
        repository.deleteAllInBatch();
    }

    private static HaciendaConsultaDTO exitosa(Integer codigo) {
        return HaciendaConsultaDTO.builder().exitosa(true).resultado(ResultadoConsulta.EXITOSA)
                .regimen(HaciendaRegimenDTO.builder().codigo(codigo).descripcion("r").build()).build();
    }

    private static ProveedorDTO dto(String cedula) {
        return ProveedorDTO.builder().cedula(cedula).nombre("Proveedor " + cedula).tipoProveedor(TipoProveedor.values()[0]).build();
    }

    @Test
    @DisplayName("crear: códigos 1, 2, 3 y 9 → GENERAL, SIMPLIFICADO, NO_CONTRIBUYENTE y OTRO con la fecha del Clock")
    void crear_traduceElCodigo() {
        int[] codigos = {1, 2, 3, 9};
        RegimenHacienda[] esperados = {RegimenHacienda.GENERAL, RegimenHacienda.SIMPLIFICADO,
                RegimenHacienda.NO_CONTRIBUYENTE, RegimenHacienda.OTRO};
        for (int i = 0; i < codigos.length; i++) {
            String cedula = "310100000" + i;
            when(hacienda.consultarContribuyente(cedula)).thenReturn(exitosa(codigos[i]));
            Proveedor p = service.crear(dto(cedula));
            Proveedor guardado = repository.findById(p.getId()).orElseThrow();
            assertThat(guardado.getRegimenHacienda()).isEqualTo(esperados[i]);
            assertThat(guardado.getRegimenVerificadoEn()).isEqualTo(AHORA);
        }
    }

    @Test
    @DisplayName("crear con ERROR o sin respuesta: se guarda SIN_VERIFICAR, sin fecha, y el proveedor se crea igual")
    void crear_falla_sinVerificar() {
        when(hacienda.consultarContribuyente("3101111111")).thenReturn(HaciendaConsultaDTO.builder()
                .exitosa(false).resultado(ResultadoConsulta.ERROR).mensajeError("timeout").build());
        when(hacienda.consultarContribuyente("3101222222")).thenReturn(null);
        when(hacienda.consultarContribuyente("3101333333")).thenThrow(new IllegalStateException("caído"));
        for (String cedula : List.of("3101111111", "3101222222", "3101333333")) {
            Proveedor p = service.crear(dto(cedula));
            Proveedor guardado = repository.findById(p.getId()).orElseThrow();
            assertThat(guardado.getRegimenHacienda()).isEqualTo(RegimenHacienda.SIN_VERIFICAR);
            assertThat(guardado.getRegimenVerificadoEn()).isNull();
        }
    }

    @Test
    @DisplayName("la consulta a Hacienda no corre dentro de una transacción (crear, editar la cédula y verificar)")
    void consulta_fueraDeTransaccion() {
        List<Boolean> enTransaccion = new ArrayList<>();
        when(hacienda.consultarContribuyente(anyString())).thenAnswer(inv -> {
            enTransaccion.add(TransactionSynchronizationManager.isActualTransactionActive());
            return exitosa(1);
        });
        Proveedor p = service.crear(dto("3101444444"));
        service.actualizar(p.getId(), dto("3101555555"));
        service.verificarRegimen(p.getId());
        assertThat(enTransaccion).containsExactly(false, false, false);
    }

    @Test
    @DisplayName("editar: sin cambio de cédula no consulta; con cambio consulta y reemplaza el régimen")
    void actualizar_soloConsultaSiCambiaLaCedula() {
        when(hacienda.consultarContribuyente("3101666666")).thenReturn(exitosa(2));
        Proveedor p = service.crear(dto("3101666666"));
        ProveedorDTO cambio = dto("3101666666");
        cambio.setNombre("Otro nombre");
        service.actualizar(p.getId(), cambio);
        verify(hacienda, times(1)).consultarContribuyente(anyString());
        assertThat(repository.findById(p.getId()).orElseThrow().getRegimenHacienda()).isEqualTo(RegimenHacienda.SIMPLIFICADO);

        when(hacienda.consultarContribuyente("3101777777")).thenReturn(exitosa(1));
        service.actualizar(p.getId(), dto("3101777777"));
        verify(hacienda, times(2)).consultarContribuyente(anyString());
        assertThat(repository.findById(p.getId()).orElseThrow().getRegimenHacienda()).isEqualTo(RegimenHacienda.GENERAL);
    }

    @Test
    @DisplayName("Verificar régimen: actualiza régimen y fecha; si Hacienda no responde falla y deja lo guardado")
    void verificarRegimen() {
        when(hacienda.consultarContribuyente("3101888888")).thenReturn(exitosa(1));
        Proveedor p = service.crear(dto("3101888888"));
        when(hacienda.consultarContribuyente("3101888888")).thenReturn(exitosa(2));
        Proveedor v = service.verificarRegimen(p.getId());
        assertThat(v.getRegimenHacienda()).isEqualTo(RegimenHacienda.SIMPLIFICADO);
        assertThat(repository.findById(p.getId()).orElseThrow().getRegimenHacienda()).isEqualTo(RegimenHacienda.SIMPLIFICADO);

        when(hacienda.consultarContribuyente("3101888888")).thenReturn(HaciendaConsultaDTO.builder()
                .exitosa(false).resultado(ResultadoConsulta.ERROR).build());
        assertThatThrownBy(() -> service.verificarRegimen(p.getId())).isInstanceOf(RegimenNoVerificableException.class);
        assertThat(repository.findById(p.getId()).orElseThrow().getRegimenHacienda()).isEqualTo(RegimenHacienda.SIMPLIFICADO);
        verify(hacienda, never()).consultarContribuyente("otra");
    }
}
