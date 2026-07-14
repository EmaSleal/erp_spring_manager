package api.astro.whats_orders_manager.modules.proveedores.dto;

import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoProveedorDTO {

    private Long cuentaPorPagarId;
    private BigDecimal monto;
    private MetodoPagoProveedor metodoPago;
    private String referencia;
    private LocalDate fecha;
    private String notas;
}
