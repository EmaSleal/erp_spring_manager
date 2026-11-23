package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TratamientoIvaCompra;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-052 (ADR-066 §1-2) y T-054 (ADR-067 §2-3): validaciones del comprobante del proveedor y de sus líneas (cuenta, tarifa,
 * base, IVA y tratamiento), con mensajes fijos.
 */
@DisplayName("T-052/T-054 — validación del comprobante del proveedor y sus líneas")
class ComprobanteProveedorValidatorTest {

    // 2026-11-22 20:00 en Costa Rica = 2026-11-23 02:00 UTC: «hoy» es el 22 en Costa Rica
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-23T02:00:00Z"), ZoneId.of("America/Costa_Rica"));
    private final ComprobanteProveedorValidator validador = new ComprobanteProveedorValidator(RELOJ);

    private static CuentaPorPagarLineaDTO linea(String tarifa, String base, String iva) {
        return new CuentaPorPagarLineaDTO("Mercadería", 7L, tarifa, new BigDecimal(base), new BigDecimal(iva),
                TratamientoIvaCompra.PLENO, null);
    }

    private static CuentaPorPagarLineaDTO linea(TratamientoIvaCompra t, String tarifaLimite) {
        return new CuentaPorPagarLineaDTO("Mercadería", 7L, "08", new BigDecimal("1000.00"), new BigDecimal("130.00"), t,
                tarifaLimite == null ? null : new BigDecimal(tarifaLimite));
    }

    private static CuentaPorPagarDTO valido() {
        return CuentaPorPagarDTO.builder()
                .numeroComprobanteProveedor("FE-0001")
                .fechaComprobante(LocalDate.of(2026, 11, 20))
                .lineas(List.of(linea("08", "100000.00", "13000.00")))
                .build();
    }

    private void rechaza(CuentaPorPagarDTO dto, String mensaje) {
        assertThatThrownBy(() -> validador.validar(dto))
                .isInstanceOf(IllegalArgumentException.class).hasMessage(mensaje);
    }

    @Test
    @DisplayName("un comprobante completo y cuadrado pasa, sin avisos")
    void valido_pasa() {
        assertThatCode(() -> validador.validar(valido())).doesNotThrowAnyException();
        assertThat(validador.advertencias(valido())).isEmpty();
    }

    @Test
    @DisplayName("el número del comprobante es obligatorio (nulo o en blanco) y de hasta 50 caracteres")
    void numeroObligatorio() {
        CuentaPorPagarDTO dto = valido();
        dto.setNumeroComprobanteProveedor(null);
        rechaza(dto, ComprobanteProveedorValidator.MSG_NUMERO);
        dto.setNumeroComprobanteProveedor("   ");
        rechaza(dto, ComprobanteProveedorValidator.MSG_NUMERO);
        dto.setNumeroComprobanteProveedor("N".repeat(51));
        rechaza(dto, ComprobanteProveedorValidator.MSG_NUMERO_LARGO);
        dto.setNumeroComprobanteProveedor("N".repeat(50));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("la fecha del comprobante es obligatoria y no puede ser futura (hoy de Costa Rica sí)")
    void fecha() {
        CuentaPorPagarDTO dto = valido();
        dto.setFechaComprobante(null);
        rechaza(dto, ComprobanteProveedorValidator.MSG_FECHA);
        dto.setFechaComprobante(LocalDate.of(2026, 11, 23));
        rechaza(dto, ComprobanteProveedorValidator.MSG_FECHA_FUTURA);
        dto.setFechaComprobante(LocalDate.of(2026, 11, 22));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("la clave, si viene, tiene exactamente 50 dígitos; en blanco se toma como ausente")
    void clave() {
        CuentaPorPagarDTO dto = valido();
        dto.setClaveComprobante("1".repeat(50));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
        dto.setClaveComprobante("1".repeat(49));
        rechaza(dto, ComprobanteProveedorValidator.MSG_CLAVE);
        dto.setClaveComprobante("1".repeat(49) + "X");
        rechaza(dto, ComprobanteProveedorValidator.MSG_CLAVE);
        dto.setClaveComprobante("  ");
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("debe haber al menos una línea")
    void lineasNoVacias() {
        CuentaPorPagarDTO dto = valido();
        dto.setLineas(List.of());
        rechaza(dto, ComprobanteProveedorValidator.MSG_LINEAS);
        dto.setLineas(null);
        rechaza(dto, ComprobanteProveedorValidator.MSG_LINEAS);
    }

    @Test
    @DisplayName("cada línea exige cuenta, tarifa del catálogo, base mayor a 0, IVA no negativo y tratamiento")
    void lineas() {
        CuentaPorPagarDTO dto = valido();
        dto.setLineas(List.of(new CuentaPorPagarLineaDTO("x", null, "08", new BigDecimal("100"), new BigDecimal("13"),
                TratamientoIvaCompra.PLENO, null)));
        rechaza(dto, ComprobanteProveedorValidator.MSG_LINEA_CUENTA);
        dto.setLineas(List.of(linea("99", "100000.00", "13000.00")));
        rechaza(dto, ComprobanteProveedorValidator.MSG_TARIFA);
        dto.setLineas(List.of(new CuentaPorPagarLineaDTO("x", 7L, "08", null, new BigDecimal("13"), TratamientoIvaCompra.PLENO, null)));
        rechaza(dto, ComprobanteProveedorValidator.MSG_LINEA);
        dto.setLineas(List.of(linea("08", "0.00", "0.00")));
        rechaza(dto, ComprobanteProveedorValidator.MSG_LINEA);
        dto.setLineas(List.of(linea("08", "-1.00", "13000.00")));
        rechaza(dto, ComprobanteProveedorValidator.MSG_LINEA);
        dto.setLineas(List.of(linea("08", "100000.00", "-0.01")));
        rechaza(dto, ComprobanteProveedorValidator.MSG_LINEA);
        dto.setLineas(List.of(new CuentaPorPagarLineaDTO("x", 7L, "08", new BigDecimal("100"), new BigDecimal("13"), null, null)));
        rechaza(dto, ComprobanteProveedorValidator.MSG_TRATAMIENTO);
        dto.setLineas(java.util.Arrays.asList(linea("08", "100000.00", "13000.00"), null));
        rechaza(dto, ComprobanteProveedorValidator.MSG_LINEA);
    }

    @Test
    @DisplayName("LIMITADO exige una tarifa límite mayor a 0 y menor que la tarifa de la línea")
    void limitado() {
        CuentaPorPagarDTO dto = valido();
        for (String limite : new String[] {null, "0", "-1", "13", "13.01", "20"}) {
            dto.setLineas(List.of(linea(TratamientoIvaCompra.LIMITADO, limite)));
            rechaza(dto, ComprobanteProveedorValidator.MSG_LIMITE);
        }
        dto.setLineas(List.of(linea(TratamientoIvaCompra.LIMITADO, "1")));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
        dto.setLineas(List.of(linea(TratamientoIvaCompra.LIMITADO, "12.99")));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("una tarifa límite en un tratamiento que no es LIMITADO se ignora, no es un error")
    void limiteIgnoradoSiNoEsLimitado() {
        CuentaPorPagarDTO dto = valido();
        dto.setLineas(List.of(linea(TratamientoIvaCompra.PLENO, "50")));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("el IVA de la línea fuera de base × tarifa por más de 0,01 es un aviso, no un error")
    void avisoPorLinea() {
        CuentaPorPagarDTO dto = valido();
        dto.setLineas(List.of(linea("08", "100000.00", "13000.01")));
        assertThat(validador.advertencias(dto)).isEmpty();

        dto.setLineas(List.of(linea("08", "100000.00", "12999.00")));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
        assertThat(validador.advertencias(dto)).hasSize(1)
                .first().asString().contains("línea 1").contains("12999.00").contains("13000.00");
    }
}
