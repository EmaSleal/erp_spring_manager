package api.astro.whats_orders_manager.modules.politicas;

import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-058: ayuda de las pruebas anteriores a ADR-074 que construyen CuentaPorPagarServiceImpl y no hablan de regímenes. Una
 * PoliticaIvaCompra cuyo régimen de la empresa es GENERAL en cualquier fecha: el comportamiento de T-054 que esas pruebas
 * verifican. Como configuración de prueba ({@code @Import}) aporta el bean; {@link #politica()} sirve a quien usa {@code new}.
 */
@TestConfiguration
public class PoliticaIvaCompraGeneral {

    public static PoliticaIvaCompra politica() {
        RegimenTributarioService servicio = mock(RegimenTributarioService.class);
        when(servicio.vigenteEn(any(LocalDate.class))).thenReturn(RegimenTributario.builder().regimen(Regimen.GENERAL)
                .vigenteDesde(LocalDate.of(2000, 1, 1)).fechaRegistro(LocalDateTime.of(2026, 1, 1, 0, 0)).build());
        return new PoliticaIvaCompra(servicio);
    }

    @Bean
    PoliticaIvaCompra politicaIvaCompra() {
        return politica();
    }
}
