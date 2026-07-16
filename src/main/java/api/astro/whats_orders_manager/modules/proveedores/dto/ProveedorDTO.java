package api.astro.whats_orders_manager.modules.proveedores.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.proveedores.enums.CategoriaProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorDTO {

    private String cedula;
    private String nombre;
    private String nombreComercial;
    private TipoProveedor tipoProveedor;
    private CategoriaProveedor categoria;

    private String contacto;
    private String telefono;
    private String email;
    private String sitioWeb;

    private String direccion;
    private String ciudad;
    private String pais;

    private MonedaFE moneda;
    private Integer diasCredito;
    private BigDecimal limiteCredito;
    private BigDecimal descuentoHabitual;

    private String banco;
    private String numeroCuenta;
    private String cuentaIban;
    private String sinpeMovil;

    private String notas;

    private List<Integer> itemIds;
}
