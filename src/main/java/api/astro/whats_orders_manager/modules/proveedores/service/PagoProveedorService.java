package api.astro.whats_orders_manager.modules.proveedores.service;

import api.astro.whats_orders_manager.modules.proveedores.dto.PagoProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;

import java.time.LocalDate;
import java.util.List;

public interface PagoProveedorService {

    PagoProveedor registrarPago(PagoProveedorDTO dto, Usuario usuario);

    void anularPago(Long pagoId, Usuario usuario);

    PagoProveedor findById(Long id);

    List<PagoProveedor> findByCuenta(Long cuentaId);

    List<PagoProveedor> findByPeriodo(LocalDate desde, LocalDate hasta);
}
