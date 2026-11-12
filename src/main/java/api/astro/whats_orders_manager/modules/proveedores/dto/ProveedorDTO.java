package api.astro.whats_orders_manager.modules.proveedores.dto;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.proveedores.enums.CategoriaProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** T-030: las restricciones reflejan las columnas de Proveedor; un dato inválido responde 400 por campo, no un error de BD. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorDTO {

    @NotBlank(message = "La cédula es obligatoria")
    @Size(max = 20, message = "La cédula admite como máximo 20 caracteres")
    private String cedula;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre admite como máximo 200 caracteres")
    private String nombre;

    @Size(max = 200, message = "El nombre comercial admite como máximo 200 caracteres")
    private String nombreComercial;

    @NotNull(message = "El tipo de proveedor es obligatorio")
    private TipoProveedor tipoProveedor;

    private CategoriaProveedor categoria;

    @Size(max = 100, message = "El contacto admite como máximo 100 caracteres")
    private String contacto;
    @Size(max = 15, message = "El teléfono admite como máximo 15 caracteres")
    private String telefono;
    @Size(max = 100, message = "El email admite como máximo 100 caracteres")
    private String email;
    @Size(max = 100, message = "El sitio web admite como máximo 100 caracteres")
    private String sitioWeb;

    @Size(max = 500, message = "La dirección admite como máximo 500 caracteres")
    private String direccion;
    @Size(max = 100, message = "La ciudad admite como máximo 100 caracteres")
    private String ciudad;
    @Size(max = 50, message = "El país admite como máximo 50 caracteres")
    private String pais;

    private MonedaFE moneda;
    private Integer diasCredito;
    private BigDecimal limiteCredito;
    private BigDecimal descuentoHabitual;

    @Size(max = 100, message = "El banco admite como máximo 100 caracteres")
    private String banco;
    @Size(max = 50, message = "El número de cuenta admite como máximo 50 caracteres")
    private String numeroCuenta;
    @Size(max = 50, message = "La cuenta IBAN admite como máximo 50 caracteres")
    private String cuentaIban;
    @Size(max = 15, message = "El SINPE Móvil admite como máximo 15 caracteres")
    private String sinpeMovil;

    @Size(max = 1000, message = "Las notas admiten como máximo 1000 caracteres")
    private String notas;

    private List<Integer> itemIds;
}
