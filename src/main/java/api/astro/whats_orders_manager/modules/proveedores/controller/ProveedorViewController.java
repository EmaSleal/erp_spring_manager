package api.astro.whats_orders_manager.modules.proveedores.controller;

import api.astro.whats_orders_manager.modules.proveedores.enums.CategoriaProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.service.OrdenCompraService;
import api.astro.whats_orders_manager.modules.proveedores.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/proveedores")
@RequiredArgsConstructor
public class ProveedorViewController {

    private final ProveedorService proveedorService;
    private final OrdenCompraService ordenCompraService;

    @GetMapping
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_VER')")
    public String lista(Model model, Authentication authentication) {
        model.addAttribute("proveedores", proveedorService.findAll());
        model.addAttribute("tiposProveedor", TipoProveedor.values());
        model.addAttribute("categorias", CategoriaProveedor.values());
        return "modules/proveedores/proveedores";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_CREAR')")
    public String formularioNuevo(Model model, Authentication authentication) {
        model.addAttribute("proveedor", new Proveedor());
        model.addAttribute("tiposProveedor", TipoProveedor.values());
        model.addAttribute("categorias", CategoriaProveedor.values());
        model.addAttribute("esNuevo", true);
        return "modules/proveedores/proveedor-form";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_EDITAR')")
    public String formularioEditar(@PathVariable Long id, Model model, Authentication authentication) {
        model.addAttribute("proveedor", proveedorService.findById(id));
        model.addAttribute("tiposProveedor", TipoProveedor.values());
        model.addAttribute("categorias", CategoriaProveedor.values());
        model.addAttribute("esNuevo", false);
        return "modules/proveedores/proveedor-form";
    }

    @GetMapping("/ordenes-compra")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'ORDEN_COMPRA_VER')")
    public String ordenesCompra(Model model, Authentication authentication) {
        model.addAttribute("ordenes", ordenCompraService.findAll());
        model.addAttribute("estados", EstadoOrdenCompra.values());
        model.addAttribute("proveedores", proveedorService.findActivos());
        return "modules/proveedores/ordenes-compra";
    }
}
