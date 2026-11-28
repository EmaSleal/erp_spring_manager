package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.configuracion.model.CantonCostaRica;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.ProvinciaCostaRica;
import api.astro.whats_orders_manager.modules.configuracion.repository.CantonCostaRicaRepository;
import api.astro.whats_orders_manager.modules.configuracion.repository.ProvinciaCostaRicaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * T-059 criterio 6: lugar de la venta del comprobante del régimen simplificado = cantón y provincia del emisor
 * (ConfiguracionEmpresa.canton y codigoProvincia), por nombre y no por código. Si no se puede resolver, devuelve null y
 * el PDF no imprime la línea.
 */
@Component
@RequiredArgsConstructor
public class LugarVentaResolver {

    private final ProvinciaCostaRicaRepository provincias;
    private final CantonCostaRicaRepository cantones;

    /** «Cantón, Provincia», solo la provincia si el cantón no se resuelve, o null. */
    @Transactional(readOnly = true)
    public String lugarDe(ConfiguracionEmpresa configuracion) {
        if (configuracion == null) {
            return null;
        }
        String codigoProvincia = limpio(configuracion.getCodigoProvincia());
        if (codigoProvincia == null) {
            return null;
        }
        ProvinciaCostaRica provincia = provincias.findByCodigo(codigoProvincia).orElse(null);
        if (provincia == null || limpio(provincia.getNombre()) == null) {
            return null;
        }
        String codigoCanton = limpio(configuracion.getCanton());
        CantonCostaRica canton = codigoCanton == null ? null
                : cantones.findByProvinciaCodigoAndCodigo(codigoProvincia, codigoCanton);
        if (canton != null && limpio(canton.getNombre()) != null) {
            return canton.getNombre().strip() + ", " + provincia.getNombre().strip();
        }
        return provincia.getNombre().strip();
    }

    private static String limpio(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
