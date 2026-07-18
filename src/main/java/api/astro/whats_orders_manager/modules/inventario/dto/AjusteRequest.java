package api.astro.whats_orders_manager.modules.inventario.dto;

import api.astro.whats_orders_manager.modules.inventario.enums.TipoAjuste;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AjusteRequest {

    private TipoAjuste tipoAjuste;
    private String motivo;
    private List<DetalleRequest> detalles;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DetalleRequest {
        private Integer productoId;
        private Integer cantidadFisica;
        private Integer cantidadSistema;
        private BigDecimal costoUnitario;
    }
}
