package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.modules.proveedores.dto.PagoProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.dto.PagoProveedorVistaDTO;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;

import java.time.LocalDate;
import java.util.List;

public interface PagoProveedorService {

    String MSG_PAGO_YA_ANULADO = "El pago ya está anulado";

    PagoProveedor registrarPago(PagoProveedorDTO dto, Usuario usuario);

    void anularPago(Long pagoId, Usuario usuario);

    PagoProveedor findById(Long id);

    List<PagoProveedor> findByCuenta(Long cuentaId);

    List<PagoProveedor> findByPeriodo(LocalDate desde, LocalDate hasta);

    /** Pagos de la cuenta como DTO (API JSON, sin entidades; T-052). */
    List<PagoProveedorVistaDTO> findByCuentaVista(Long cuentaId);

    /** Pagos del período como DTO (API JSON, sin entidades; T-052). */
    List<PagoProveedorVistaDTO> findByPeriodoVista(LocalDate desde, LocalDate hasta);
}
