package api.astro.whats_orders_manager.modules.facturacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCuentaClienteDTO {

    // Client identity
    private Integer idCliente;
    private String nombreCliente;
    private String identificacion;
    private String email;
    private String telefono;

    // Aggregated summary
    private BigDecimal totalFacturado;
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;
    private int totalFacturas;
    private int facturasPendientes;

    // Detail lists
    @Builder.Default
    private List<FacturaDetalleDTO> facturas = new ArrayList<>();

    @Builder.Default
    private List<PagoDTO> pagos = new ArrayList<>();
}
