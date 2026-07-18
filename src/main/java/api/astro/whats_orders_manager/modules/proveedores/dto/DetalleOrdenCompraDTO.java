package api.astro.whats_orders_manager.modules.proveedores.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleOrdenCompraDTO {

    private Integer productoId;
    private String descripcion;
    private Integer cantidad;
    private BigDecimal precioUnitario;
}
