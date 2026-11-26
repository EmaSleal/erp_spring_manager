package api.astro.whats_orders_manager.modules.politicas;

import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.EmisionNoPermitidaException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.repository.RegimenTributarioRepository;
import api.astro.whats_orders_manager.modules.regimen.service.ConsultaDocumentosPorFecha;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioActividadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-057 (ADR-073 §2, ADR-076 §2; criterios 5 y 14): PoliticaEmision decide si se crea el comprobante de una factura.
 * (a) SIMPLIFICADO nunca; (b) GENERAL tampoco si desde su fecha empezó un tramo SIMPLIFICADO y hoy (Clock) ya llegó a él.
 */
@DisplayName("T-057 — PoliticaEmision: emisión según el régimen sellado y el Clock")
class PoliticaEmisionTest {

    private static final ZoneId CR = ZoneId.of("America/Costa_Rica");

    private static RegimenTributario tramo(Regimen regimen, String desde) {
        return RegimenTributario.builder().id((long) desde.hashCode()).regimen(regimen).vigenteDesde(LocalDate.parse(desde))
                .fechaRegistro(LocalDateTime.of(2026, 1, 1, 0, 0)).build();
    }

    private static PoliticaEmision politica(String hoy, RegimenTributario... tramos) {
        RegimenTributarioRepository repo = mock(RegimenTributarioRepository.class);
        when(repo.findAllByOrderByVigenteDesdeAsc()).thenReturn(new ArrayList<>(List.of(tramos)));
        Clock clock = Clock.fixed(LocalDate.parse(hoy).atTime(9, 30).atZone(CR).toInstant(), CR);
        RegimenTributarioService servicio = new RegimenTributarioService(repo, mock(ConsultaDocumentosPorFecha.class),
                mock(UsuarioActividadService.class), clock);
        return new PoliticaEmision(servicio, clock);
    }

    private static Factura factura(Regimen regimen, String fecha) {
        Factura f = new Factura();
        f.setRegimenEmisor(regimen);
        f.setCreateDate(LocalDate.parse(fecha).atTime(23, 59));
        return f;
    }

    private static final RegimenTributario GENERAL_2000 = tramo(Regimen.GENERAL, "2000-01-01");
    private static final RegimenTributario SIMPLIFICADO_2027 = tramo(Regimen.SIMPLIFICADO, "2027-01-01");

    @Test
    @DisplayName("(a) factura SIMPLIFICADO → EmisionNoPermitidaException con el mensaje fijo, sea cual sea la fecha de hoy")
    void simplificado_nuncaSeEmite() {
        PoliticaEmision politica = politica("2027-01-15", GENERAL_2000, SIMPLIFICADO_2027);

        assertThatThrownBy(() -> politica.exigirEmisible(factura(Regimen.SIMPLIFICADO, "2027-01-01")))
                .isInstanceOf(EmisionNoPermitidaException.class)
                .hasMessage("Las ventas del régimen simplificado no se emiten a Hacienda");
    }

    @Test
    @DisplayName("(b) GENERAL del 31-12 con el Clock el 31-12 → se puede emitir")
    void general_diciembreConClockEnDiciembre_sePuedeEmitir() {
        PoliticaEmision politica = politica("2026-12-31", GENERAL_2000, SIMPLIFICADO_2027);

        assertThatCode(() -> politica.exigirEmisible(factura(Regimen.GENERAL, "2026-12-31"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("(b) GENERAL del 31-12 con el Clock el 01-01 y el 02-01 → 409 con el mensaje fijo de la herramienta de Hacienda")
    void general_diciembreConClockEnEnero_409() {
        for (String hoy : List.of("2027-01-01", "2027-01-02", "2027-03-10")) {
            PoliticaEmision politica = politica(hoy, GENERAL_2000, SIMPLIFICADO_2027);

            assertThatThrownBy(() -> politica.exigirEmisible(factura(Regimen.GENERAL, "2026-12-31")))
                    .as("hoy = " + hoy)
                    .isInstanceOf(EmisionNoPermitidaException.class)
                    .hasMessage("Esta venta es del régimen general; su comprobante se emite en la herramienta de Hacienda "
                            + "y se registra aquí su clave");
        }
    }

    @Test
    @DisplayName("GENERAL sin ningún tramo SIMPLIFICADO posterior (semilla de producción: julio-agosto sellados GENERAL) → se emite como hoy")
    void general_sinTramoSimplificadoPosterior_seEmite() {
        PoliticaEmision politica = politica("2026-11-25", tramo(Regimen.SIMPLIFICADO, "2000-01-01"),
                tramo(Regimen.GENERAL, "2026-09-01"));

        assertThatCode(() -> politica.exigirEmisible(factura(Regimen.GENERAL, "2026-07-04"))).doesNotThrowAnyException();
        assertThatCode(() -> politica.exigirEmisible(factura(Regimen.GENERAL, "2026-11-20"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GENERAL con un tramo SIMPLIFICADO futuro (aún no llega hoy) → se emite")
    void general_tramoSimplificadoFuturo_seEmite() {
        PoliticaEmision politica = politica("2026-12-20", GENERAL_2000, SIMPLIFICADO_2027);

        assertThatCode(() -> politica.exigirEmisible(factura(Regimen.GENERAL, "2026-12-10"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("caso 13: tras salir del RTS (GENERAL desde 2027-07-01) una factura GENERAL de 2027-07-05 se emite; la de diciembre 2026 no")
    void salidaDelRts() {
        PoliticaEmision politica = politica("2027-08-01", GENERAL_2000, SIMPLIFICADO_2027, tramo(Regimen.GENERAL, "2027-07-01"));

        assertThatCode(() -> politica.exigirEmisible(factura(Regimen.GENERAL, "2027-07-05"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> politica.exigirEmisible(factura(Regimen.GENERAL, "2026-12-31")))
                .isInstanceOf(EmisionNoPermitidaException.class);
    }
}
