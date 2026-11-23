package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarIvaDTO;
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

/** T-052 (ADR-066 §1-2): validaciones del comprobante del proveedor y de su desglose de IVA, con mensajes fijos. */
@DisplayName("T-052 — validación del comprobante del proveedor y su desglose")
class ComprobanteProveedorValidatorTest {

    // 2026-11-22 20:00 en Costa Rica = 2026-11-23 02:00 UTC: «hoy» es el 22 en Costa Rica
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-23T02:00:00Z"), ZoneId.of("America/Costa_Rica"));
    private final ComprobanteProveedorValidator validador = new ComprobanteProveedorValidator(RELOJ);

    private static CuentaPorPagarIvaDTO fila(String tarifa, String base, String iva) {
        return new CuentaPorPagarIvaDTO(tarifa, new BigDecimal(base), new BigDecimal(iva));
    }

    private static CuentaPorPagarDTO valido() {
        return CuentaPorPagarDTO.builder()
                .numeroComprobanteProveedor("FE-0001")
                .fechaComprobante(LocalDate.of(2026, 11, 20))
                .subtotal(new BigDecimal("100000.00")).impuesto(new BigDecimal("13000.00"))
                .desglose(List.of(fila("08", "100000.00", "13000.00")))
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
    @DisplayName("el desglose debe tener al menos una fila")
    void desgloseNoVacio() {
        CuentaPorPagarDTO dto = valido();
        dto.setDesglose(List.of());
        rechaza(dto, ComprobanteProveedorValidator.MSG_DESGLOSE);
        dto.setDesglose(null);
        rechaza(dto, ComprobanteProveedorValidator.MSG_DESGLOSE);
    }

    @Test
    @DisplayName("cada fila exige tarifa del catálogo y base e IVA no negativos")
    void filas() {
        CuentaPorPagarDTO dto = valido();
        dto.setDesglose(List.of(fila("99", "100000.00", "13000.00")));
        rechaza(dto, ComprobanteProveedorValidator.MSG_TARIFA);
        dto.setDesglose(List.of(new CuentaPorPagarIvaDTO("08", null, new BigDecimal("13000.00"))));
        rechaza(dto, ComprobanteProveedorValidator.MSG_FILA);
        dto.setDesglose(List.of(fila("08", "-1.00", "13000.00")));
        rechaza(dto, ComprobanteProveedorValidator.MSG_FILA);
        dto.setDesglose(List.of(fila("08", "100000.00", "-0.01")));
        rechaza(dto, ComprobanteProveedorValidator.MSG_FILA);
    }

    @Test
    @DisplayName("Σ bases = subtotal y Σ IVA = impuesto, con tolerancia de 0,01")
    void sumas() {
        CuentaPorPagarDTO dto = valido();
        dto.setDesglose(List.of(fila("08", "60000.00", "7800.00"), fila("08", "40000.00", "5200.00")));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();

        dto.setSubtotal(new BigDecimal("100000.01"));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
        dto.setSubtotal(new BigDecimal("100000.02"));
        rechaza(dto, ComprobanteProveedorValidator.MSG_SUMA_BASES);

        dto.setSubtotal(new BigDecimal("100000.00"));
        dto.setImpuesto(new BigDecimal("13000.01"));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
        dto.setImpuesto(new BigDecimal("13000.02"));
        rechaza(dto, ComprobanteProveedorValidator.MSG_SUMA_IVA);
    }

    @Test
    @DisplayName("el IVA de la fila fuera de base × tarifa por más de 0,01 es un aviso, no un error")
    void avisoPorFila() {
        CuentaPorPagarDTO dto = valido();
        dto.setDesglose(List.of(fila("08", "100000.00", "13000.01")));
        dto.setImpuesto(new BigDecimal("13000.01"));
        assertThat(validador.advertencias(dto)).isEmpty();

        dto.setDesglose(List.of(fila("08", "100000.00", "12999.00")));
        dto.setImpuesto(new BigDecimal("12999.00"));
        assertThatCode(() -> validador.validar(dto)).doesNotThrowAnyException();
        assertThat(validador.advertencias(dto)).hasSize(1)
                .first().asString().contains("fila 1").contains("12999.00").contains("13000.00");
    }
}
