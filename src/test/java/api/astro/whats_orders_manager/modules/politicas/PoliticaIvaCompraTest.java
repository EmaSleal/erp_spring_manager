package api.astro.whats_orders_manager.modules.politicas;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAceptacionComprobante;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.RegimenHacienda;
import api.astro.whats_orders_manager.modules.regimen.enums.Regimen;
import api.astro.whats_orders_manager.modules.regimen.model.RegimenTributario;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenNoDefinidoException;
import api.astro.whats_orders_manager.modules.regimen.service.RegimenTributarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-058 criterios 1, 2, 6 y 8 (ADR-074 §1, §3, §5-6): PoliticaIvaCompra es el único punto de decisión del tratamiento de
 * IVA de una compra según el régimen de la empresa en la fecha del comprobante y el del proveedor.
 */
@DisplayName("T-058 — PoliticaIvaCompra: tratamiento de IVA de compra por régimen")
class PoliticaIvaCompraTest {

    private static final LocalDate FECHA_SIMPLIFICADO = LocalDate.of(2027, 2, 1);
    private static final LocalDate FECHA_GENERAL = LocalDate.of(2026, 10, 1);
    private static final LocalDate SIN_TRAMO = LocalDate.of(1999, 1, 1);

    private PoliticaIvaCompra politica;

    @BeforeEach
    void preparar() {
        RegimenTributarioService servicio = mock(RegimenTributarioService.class);
        when(servicio.vigenteEn(any())).thenAnswer(inv -> {
            LocalDate f = inv.getArgument(0);
            if (f.isBefore(LocalDate.of(2000, 1, 1))) {
                throw new RegimenNoDefinidoException(f);
            }
            return tramo(f.isBefore(LocalDate.of(2027, 1, 1)) ? Regimen.GENERAL : Regimen.SIMPLIFICADO);
        });
        politica = new PoliticaIvaCompra(servicio);
    }

    private static RegimenTributario tramo(Regimen r) {
        return RegimenTributario.builder().regimen(r).vigenteDesde(LocalDate.of(2000, 1, 1))
                .fechaRegistro(LocalDateTime.of(2026, 1, 1, 0, 0)).build();
    }

    private static CuentaPorPagarLineaDTO linea(String iva, TratamientoIvaCompra t) {
        return new CuentaPorPagarLineaDTO("x", 1L, "08", new BigDecimal("1000.00"), new BigDecimal(iva), t, null);
    }

    @Test
    @DisplayName("empresa SIMPLIFICADO + línea PLENO → 400 con mensaje fijo")
    void simplificadoRechazaPleno() {
        assertThatThrownBy(() -> politica.aplicar(FECHA_SIMPLIFICADO, RegimenHacienda.GENERAL,
                List.of(linea("130.00", TratamientoIvaCompra.PLENO))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(PoliticaIvaCompra.MSG_SOLO_NO_ACREDITABLE);
    }

    @Test
    @DisplayName("empresa SIMPLIFICADO + NO_ACREDITABLE → admitido y sin cambios")
    void simplificadoAdmiteNoAcreditable() {
        CuentaPorPagarLineaDTO l = linea("130.00", TratamientoIvaCompra.NO_ACREDITABLE);
        assertThatCode(() -> politica.aplicar(FECHA_SIMPLIFICADO, RegimenHacienda.GENERAL, List.of(l))).doesNotThrowAnyException();
        assertThat(l.getTratamiento()).isEqualTo(TratamientoIvaCompra.NO_ACREDITABLE);
    }

    @Test
    @DisplayName("fecha sin tramo → RegimenNoDefinidoException (409), no IllegalArgumentException")
    void sinTramo() {
        assertThatThrownBy(() -> politica.aplicar(SIN_TRAMO, RegimenHacienda.GENERAL,
                List.of(linea("130.00", TratamientoIvaCompra.PLENO))))
                .isInstanceOf(RegimenNoDefinidoException.class);
        assertThatThrownBy(() -> politica.reglaPara(SIN_TRAMO, null)).isInstanceOf(RegimenNoDefinidoException.class);
    }

    @Test
    @DisplayName("empresa GENERAL + proveedor GENERAL/SIN_VERIFICAR/null: los tratamientos de T-054 quedan como vienen")
    void generalSinCambios() {
        for (RegimenHacienda prov : new RegimenHacienda[]{RegimenHacienda.GENERAL, RegimenHacienda.SIN_VERIFICAR, null}) {
            for (TratamientoIvaCompra t : TratamientoIvaCompra.values()) {
                CuentaPorPagarLineaDTO l = linea("130.00", t);
                politica.aplicar(FECHA_GENERAL, prov, List.of(l));
                assertThat(l.getTratamiento()).isEqualTo(t);
            }
        }
        var regla = politica.reglaPara(FECHA_GENERAL, RegimenHacienda.GENERAL);
        assertThat(regla.regimen()).isEqualTo(Regimen.GENERAL);
        assertThat(regla.tratamientosAdmitidos()).containsExactlyInAnyOrder(TratamientoIvaCompra.values());
        assertThat(regla.tratamientoFijo()).isNull();
        assertThat(regla.ivaTrasladadoCero()).isFalse();
    }

    @Test
    @DisplayName("proveedor SIMPLIFICADO: fija NO_ACREDITABLE (también en fecha general), conserva la tarifa 08 y exige IVA 0")
    void proveedorSimplificado() {
        CuentaPorPagarLineaDTO pleno = linea("0.00", TratamientoIvaCompra.PLENO);
        politica.aplicar(FECHA_GENERAL, RegimenHacienda.SIMPLIFICADO, List.of(pleno));
        assertThat(pleno.getTratamiento()).isEqualTo(TratamientoIvaCompra.NO_ACREDITABLE);
        assertThat(pleno.getCodigoTarifa()).isEqualTo("08");

        CuentaPorPagarLineaDTO sinTratamiento = linea("0.00", null);
        politica.aplicar(FECHA_SIMPLIFICADO, RegimenHacienda.SIMPLIFICADO, List.of(sinTratamiento));
        assertThat(sinTratamiento.getTratamiento()).isEqualTo(TratamientoIvaCompra.NO_ACREDITABLE);

        assertThatThrownBy(() -> politica.aplicar(FECHA_GENERAL, RegimenHacienda.SIMPLIFICADO,
                List.of(linea("130.00", TratamientoIvaCompra.NO_ACREDITABLE))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(PoliticaIvaCompra.MSG_PROVEEDOR_SIN_IVA);

        var regla = politica.reglaPara(FECHA_GENERAL, RegimenHacienda.SIMPLIFICADO);
        assertThat(regla.tratamientoFijo()).isEqualTo(TratamientoIvaCompra.NO_ACREDITABLE);
        assertThat(regla.ivaTrasladadoCero()).isTrue();
        assertThat(regla.mensaje()).isNotBlank();
    }

    @Test
    @DisplayName("regla para el endpoint: SIMPLIFICADO fija NO_ACREDITABLE como único tratamiento admitido")
    void reglaSimplificado() {
        var regla = politica.reglaPara(FECHA_SIMPLIFICADO, RegimenHacienda.GENERAL);
        assertThat(regla.regimen()).isEqualTo(Regimen.SIMPLIFICADO);
        assertThat(regla.tratamientosAdmitidos()).containsExactly(TratamientoIvaCompra.NO_ACREDITABLE);
        assertThat(regla.tratamientoFijo()).isEqualTo(TratamientoIvaCompra.NO_ACREDITABLE);
    }

    @Test
    @DisplayName("aceptación inicial: NO_APLICA en SIMPLIFICADO, PENDIENTE en GENERAL, 409 sin tramo")
    void aceptacionInicial() {
        assertThat(politica.estadoAceptacionInicial(FECHA_SIMPLIFICADO)).isEqualTo(EstadoAceptacionComprobante.NO_APLICA);
        assertThat(politica.estadoAceptacionInicial(FECHA_GENERAL)).isEqualTo(EstadoAceptacionComprobante.PENDIENTE);
        assertThatThrownBy(() -> politica.estadoAceptacionInicial(SIN_TRAMO)).isInstanceOf(RegimenNoDefinidoException.class);
    }
}
