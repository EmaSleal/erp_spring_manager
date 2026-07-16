package api.astro.whats_orders_manager.modules.proveedores.controller;

import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.proveedores.enums.CategoriaProveedor;
import api.astro.whats_orders_manager.modules.proveedores.enums.EstadoOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.enums.TipoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.DetalleOrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.OrdenCompra;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/proveedores")
@RequiredArgsConstructor
public class ProveedorViewController {

    private final ProveedorService proveedorService;
    private final OrdenCompraService ordenCompraService;
    private final CuentaPorPagarService cuentaPorPagarService;

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
        model.addAttribute("proveedorItems", List.of());
        return "modules/proveedores/proveedor-form";
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'PROVEEDOR_EDITAR')")
    public String formularioEditar(@PathVariable Long id, Model model, Authentication authentication) {
        model.addAttribute("proveedor", proveedorService.findById(id));
        model.addAttribute("tiposProveedor", TipoProveedor.values());
        model.addAttribute("categorias", CategoriaProveedor.values());
        model.addAttribute("esNuevo", false);
        List<Map<String, Object>> itemsData = new ArrayList<>();
        for (var item : proveedorService.findItems(id, CategoriaProveedor.PRODUCTOS_TERMINADOS)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId", item.getItemId());
            m.put("descripcion", item.getDescripcion());
            itemsData.add(m);
        }
        model.addAttribute("proveedorItems", itemsData);
        return "modules/proveedores/proveedor-form";
    }

    @GetMapping("/ordenes-compra")
    @PreAuthorize("@permisoService.tienePermisoPorCodigo(#authentication.name, 'ORDEN_COMPRA_VER')")
    public String ordenesCompra(Model model, Authentication authentication) {
        List<OrdenCompra> ordenes = ordenCompraService.findAll();
        model.addAttribute("ordenes", ordenes);
        model.addAttribute("estados", EstadoOrdenCompra.values());
        model.addAttribute("proveedores", proveedorService.findActivos());
        model.addAttribute("monedas", MonedaFE.values());
        model.addAttribute("categorias", CategoriaProveedor.values());

        // Build detail map for reception modal (avoids JSON serialization of entities)
        Map<Long, List<Map<String, Object>>> ocDetallesMap = new LinkedHashMap<>();
        for (OrdenCompra oc : ordenes) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (DetalleOrdenCompra d : oc.getDetalles()) {
                if (d.getProducto() == null) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", d.getId());
                item.put("descripcion", d.getDescripcion() != null ? d.getDescripcion() : d.getProducto().getDescripcion());
                item.put("cantidad", d.getCantidad());
                item.put("cantidadRecibida", d.getCantidadRecibida());
                item.put("porRecibir", d.getCantidad() - d.getCantidadRecibida());
                item.put("precioUnitario", d.getPrecioUnitario());
                items.add(item);
            }
            ocDetallesMap.put(oc.getId(), items);
        }
        model.addAttribute("ocDetallesMap", ocDetallesMap);
        model.addAttribute("proveedorItemsMap", proveedorService.findItemIdsByCategoria(CategoriaProveedor.PRODUCTOS_TERMINADOS));
        model.addAttribute("ocsConCpp", cuentaPorPagarService.findOrdenCompraIdsConCpp());

        return "modules/proveedores/ordenes-compra";
    }
}
