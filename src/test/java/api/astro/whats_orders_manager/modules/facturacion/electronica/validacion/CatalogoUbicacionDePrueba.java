package api.astro.whats_orders_manager.modules.facturacion.electronica.validacion;

/** Catálogo simulado para pruebas unitarias: provincias 1 a 7, cantones y distritos 01 a 99 (códigos de 2 dígitos). */
public final class CatalogoUbicacionDePrueba implements CatalogoUbicacion {

    public static final CatalogoUbicacionDePrueba INSTANCIA = new CatalogoUbicacionDePrueba();

    /** Validador real con el catálogo simulado. */
    public static DatosFeValidator validador() {
        return new DatosFeValidator(INSTANCIA);
    }

    @Override
    public boolean existeProvincia(String provincia) {
        return provincia != null && provincia.matches("[1-7]");
    }

    @Override
    public boolean existeCanton(String provincia, String canton) {
        return existeProvincia(provincia) && canton != null && canton.matches("\\d\\d");
    }

    @Override
    public boolean existeDistrito(String provincia, String canton, String distrito) {
        return existeCanton(provincia, canton) && distrito != null && distrito.matches("\\d\\d");
    }
}
