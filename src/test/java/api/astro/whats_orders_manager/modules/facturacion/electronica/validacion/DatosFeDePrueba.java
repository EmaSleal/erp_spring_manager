package api.astro.whats_orders_manager.modules.facturacion.electronica.validacion;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;

/**
 * Fixtures de T-038: completan solo lo que falta con datos FE válidos (las reglas no se relajan; los fixtures se
 * completan). Lo que la prueba ya fijó no se toca.
 */
public final class DatosFeDePrueba {

    private DatosFeDePrueba() {
    }

    /** Emisor válido: tipo según la longitud del número (9 física, 10 jurídica), ubicación, otras señas, teléfono y correo. */
    public static ConfiguracionEmpresa emisor(ConfiguracionEmpresa e) {
        if (e.getNumeroIdentificacion() == null) {
            e.setNumeroIdentificacion("3101234567");
        }
        if (e.getTipoIdentificacion() == null) {
            e.setTipoIdentificacion(e.getNumeroIdentificacion().length() == 9
                ? TipoIdentificacion.CEDULA_FISICA : TipoIdentificacion.CEDULA_JURIDICA);
        }
        if (e.getCodigoActividad() == null) {
            e.setCodigoActividad("4773.0");
        }
        if (e.getRazonSocial() == null) {
            e.setRazonSocial("Empresa Test S.A.");
        }
        if (e.getCodigoProvincia() == null) {
            e.setCodigoProvincia("1");
        }
        if (e.getCanton() == null) {
            e.setCanton("01");
        }
        if (e.getDistrito() == null) {
            e.setDistrito("01");
        }
        if (e.getOtrasSenas() == null) {
            e.setOtrasSenas("Avenida Central, San José");
        }
        if (e.getTelefono() == null) {
            e.setTelefono("22223333");
        }
        if (e.getEmail() == null) {
            e.setEmail("facturacion@empresa.example");
        }
        return e;
    }

    /** Receptor válido: tipo según la longitud de la identificación si falta. */
    public static Cliente cliente(Cliente c) {
        String numero = c.getNumeroIdentificacion() != null ? c.getNumeroIdentificacion() : c.getIdentificacion();
        if (numero == null) {
            c.setNumeroIdentificacion("109990999");
            numero = "109990999";
        }
        if (c.getTipoIdentificacion() == null) {
            c.setTipoIdentificacion(numero.length() == 10 ? TipoIdentificacion.CEDULA_JURIDICA
                : TipoIdentificacion.CEDULA_FISICA);
        }
        if (c.getNombre() == null) {
            c.setNombre("Cliente de prueba");
        }
        return c;
    }
}
