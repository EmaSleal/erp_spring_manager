package api.astro.whats_orders_manager.modules.facturacion.electronica.validacion;

/** Catálogo de ubicaciones de Costa Rica (Anexo v4.4, nota 14); puerto para poder simularlo en pruebas unitarias. */
public interface CatalogoUbicacion {

    boolean existeProvincia(String provincia);

    boolean existeCanton(String provincia, String canton);

    boolean existeDistrito(String provincia, String canton, String distrito);
}
