package api.astro.whats_orders_manager.modules.facturacion.electronica.validacion;

import api.astro.whats_orders_manager.modules.configuracion.repository.CantonCostaRicaRepository;
import api.astro.whats_orders_manager.modules.configuracion.repository.DistritoCostaRicaRepository;
import api.astro.whats_orders_manager.modules.configuracion.repository.ProvinciaCostaRicaRepository;
import org.springframework.stereotype.Component;

/** Catálogo de ubicaciones respaldado por las tablas cat_provincia_cr, cat_canton_cr y cat_distrito_cr. */
@Component
public class CatalogoUbicacionJpa implements CatalogoUbicacion {

    private final ProvinciaCostaRicaRepository provincias;
    private final CantonCostaRicaRepository cantones;
    private final DistritoCostaRicaRepository distritos;

    public CatalogoUbicacionJpa(ProvinciaCostaRicaRepository provincias, CantonCostaRicaRepository cantones,
                                DistritoCostaRicaRepository distritos) {
        this.provincias = provincias;
        this.cantones = cantones;
        this.distritos = distritos;
    }

    @Override
    public boolean existeProvincia(String provincia) {
        return provincia != null && provincias.existsByCodigo(provincia);
    }

    @Override
    public boolean existeCanton(String provincia, String canton) {
        return provincia != null && canton != null && cantones.findByProvinciaCodigoAndCodigo(provincia, canton) != null;
    }

    @Override
    public boolean existeDistrito(String provincia, String canton, String distrito) {
        return provincia != null && canton != null && distrito != null
            && distritos.findByProvinciaCodigoAndCantonCodigoAndCodigo(provincia, canton, distrito) != null;
    }
}
