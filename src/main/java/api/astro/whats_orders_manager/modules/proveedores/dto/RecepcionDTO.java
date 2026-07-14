package api.astro.whats_orders_manager.modules.proveedores.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecepcionDTO {

    private Long ordenCompraId;
    private String notas;
    private List<ItemRecepcionDTO> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRecepcionDTO {
        private Long detalleId;
        private int cantidadRecibida;
        private String codigoLote;
        private LocalDate fechaVencimiento;
        private LocalDate fechaFabricacion;
        private BigDecimal costoUnitario;
    }
}
