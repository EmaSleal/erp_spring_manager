package api.astro.whats_orders_manager.modules.facturacion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoCambioDTO {

    private Long id;
    private String monedaOrigen;
    @NotNull
    private String monedaDestino;
    private String simboloMoneda;
    @NotNull
    private LocalDate fecha;
    @NotNull @Positive
    private BigDecimal tasaCompra;
    @NotNull @Positive
    private BigDecimal tasaVenta;
    private String fuente;
}
