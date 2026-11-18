package api.astro.whats_orders_manager.modules.facturacion.electronica.validacion;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.TelefonoFe;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validador único de los datos FE del receptor (Cliente) y del emisor (ConfiguracionEmpresa), T-038 / ADR-044.
 * Reglas del Anexo v4.4 (apartado a y notas 4 y 14), cruzadas con el XSD real en DatosFeValidatorOraculoXsdTest.
 * Lo usan la emisión (409 antes del consecutivo), el generador del XML (omite los opcionales inválidos) y, después, el
 * guardado del cliente y el reporte. Los mensajes son fijos y no incluyen el valor del dato.
 */
@Component
public class DatosFeValidator {

    /** Patrón de correo del Anexo v4.4 (filas CorreoElectronico del emisor y del receptor). */
    private static final Pattern CORREO = Pattern.compile(
        "^\\s*(([^<>()\\[\\]\\.,;:\\s@\\\"]+(\\.[^<>()\\[\\]\\.,;:\\s@\\\"]+)*)|(\\\".+\\\"))@"
            + "(([^<>()\\[\\]\\.,;:\\s@\\\"]+\\.)+[^<>()\\[\\]\\.,;:\\s@\\\"]{0,})\\s*$");
    private static final int CORREO_MAX = 160;
    private static final Pattern CODIGO_UBICACION = Pattern.compile("[1-9]\\d|0[1-9]");
    private static final Pattern PROVINCIA = Pattern.compile("[1-9]");
    /**
     * Código de actividad (CodigoActividadEmisor y CodigoActividadReceptor): el XSD solo exige un string de exactamente
     * 6 caracteres. Se aceptan 6 dígitos o el formato de /fe/ae «NNNN.N» (p. ej. «4773.0»), que Hacienda ya aceptó.
     */
    private static final Pattern ACTIVIDAD = Pattern.compile("\\d{6}|\\d{4}\\.\\d");

    private static final String MSG_ACTIVIDAD_RECEPTOR =
        "El código de actividad debe tener 6 caracteres: 6 dígitos o el formato de Hacienda, por ejemplo 4773.0.";

    private final CatalogoUbicacion catalogo;

    public DatosFeValidator(CatalogoUbicacion catalogo) {
        this.catalogo = catalogo;
    }

    // =====================================================================
    // Receptor
    // =====================================================================

    /** Problemas del receptor: nombre e identificación son obligatorios; el resto, opcional (se omite del XML). */
    public List<ProblemaDatoFe> validarReceptor(Cliente cliente) {
        List<ProblemaDatoFe> p = new ArrayList<>();
        validarIdentificacion(p, cliente.getTipoIdentificacion(), numeroIdentificacion(cliente),
            "tipoIdentificacion", "numeroIdentificacion");
        String nombre = recortar(cliente.getNombre());
        if (nombre.length() < 3 || nombre.length() > 100) {
            p.add(new ProblemaDatoFe("nombre", "El nombre debe tener entre 3 y 100 caracteres.",
                ReglaDatoFe.NOMBRE_LONGITUD, true));
        }

        if (!recortar(cliente.getTelefono()).isEmpty() && TelefonoFe.normalizar(cliente.getTelefono()).isEmpty()) {
            p.add(new ProblemaDatoFe("telefono", "El teléfono no es válido: debe tener de 8 a 20 dígitos y no ser cero.",
                ReglaDatoFe.TELEFONO, false));
        }
        String correo = recortar(cliente.getEmail());
        if (!correo.isEmpty() && !correoValido(correo)) {
            p.add(new ProblemaDatoFe("email", "El correo electrónico no es válido (máximo 160 caracteres).",
                ReglaDatoFe.CORREO, false));
        }
        validarUbicacionReceptor(p, cliente);

        String actividad = recortar(cliente.getCodigoActividad());
        if (!actividad.isEmpty() && !actividadValida(actividad)) {
            p.add(new ProblemaDatoFe("codigoActividad", MSG_ACTIVIDAD_RECEPTOR,
                ReglaDatoFe.ACTIVIDAD, false));
        }
        return p;
    }

    private void validarUbicacionReceptor(List<ProblemaDatoFe> p, Cliente c) {
        String provincia = c.getProvincia() != null ? recortar(c.getProvincia().getCodigo()) : "";
        String canton = recortar(c.getCanton());
        String distrito = recortar(c.getDistrito());
        String otras = recortar(c.getOtrasSenas());
        if (provincia.isEmpty() && canton.isEmpty() && distrito.isEmpty() && otras.isEmpty()) {
            return; // sin ubicación: es opcional
        }
        boolean provinciaOk = PROVINCIA.matcher(provincia).matches() && catalogo.existeProvincia(provincia);
        if (!provinciaOk) {
            p.add(problemaUbicacion("provincia", "La provincia no es válida."));
        }
        boolean cantonOk = CODIGO_UBICACION.matcher(canton).matches() && provinciaOk
            && catalogo.existeCanton(provincia, canton);
        if (!cantonOk) {
            p.add(problemaUbicacion("canton", "El cantón no es válido para la provincia."));
        }
        if (!(CODIGO_UBICACION.matcher(distrito).matches() && cantonOk
            && catalogo.existeDistrito(provincia, canton, distrito))) {
            p.add(problemaUbicacion("distrito", "El distrito no es válido para el cantón."));
        }
        if (otras.length() < 5 || otras.length() > 160) {
            p.add(new ProblemaDatoFe("otrasSenas", "Las otras señas deben tener entre 5 y 160 caracteres.",
                ReglaDatoFe.OTRAS_SENAS, false));
        }
    }

    private static ProblemaDatoFe problemaUbicacion(String campo, String mensaje) {
        return new ProblemaDatoFe(campo, mensaje, ReglaDatoFe.UBICACION, false);
    }

    /** Campos del receptor cuyo problema impide emitir su nodo Ubicacion (el resto de la ubicación se omite junto). */
    public static boolean esCampoDeUbicacion(String campo) {
        return "provincia".equals(campo) || "canton".equals(campo) || "distrito".equals(campo)
            || "otrasSenas".equals(campo);
    }

    // =====================================================================
    // Emisor
    // =====================================================================

    /** Problemas del emisor: todos obligatorios (la factura no se emite con datos inventados). */
    public List<ProblemaDatoFe> validarEmisor(ConfiguracionEmpresa empresa) {
        List<ProblemaDatoFe> p = new ArrayList<>();
        String nombre = recortar(empresa.getRazonSocial());
        if (nombre.length() < 5 || nombre.length() > 100) {
            p.add(new ProblemaDatoFe("razonSocial", "La razón social debe tener entre 5 y 100 caracteres.",
                ReglaDatoFe.NOMBRE_LONGITUD, true));
        }
        validarIdentificacion(p, empresa.getTipoIdentificacion(), numeroLimpio(empresa.getNumeroIdentificacion()),
            "tipoIdentificacion", "numeroIdentificacion");

        if (!actividadValida(empresa.getCodigoActividad())) {
            p.add(new ProblemaDatoFe("codigoActividad", "El código de actividad económica no está configurado o no es "
                + "válido (6 caracteres, por ejemplo 4773.0); configúrelo en Configuración → Empresa.",
                ReglaDatoFe.ACTIVIDAD, true));
        }

        String provincia = recortar(empresa.getCodigoProvincia());
        String canton = recortar(empresa.getCanton());
        String distrito = recortar(empresa.getDistrito());
        boolean provinciaOk = PROVINCIA.matcher(provincia).matches() && catalogo.existeProvincia(provincia);
        if (!provinciaOk) {
            p.add(new ProblemaDatoFe("codigoProvincia", "La provincia no está configurada o no es válida.",
                ReglaDatoFe.UBICACION, true));
        }
        boolean cantonOk = provinciaOk && CODIGO_UBICACION.matcher(canton).matches()
            && catalogo.existeCanton(provincia, canton);
        if (!cantonOk) {
            p.add(new ProblemaDatoFe("canton", "El cantón no está configurado o no es válido.",
                ReglaDatoFe.UBICACION, true));
        }
        if (!(cantonOk && CODIGO_UBICACION.matcher(distrito).matches()
            && catalogo.existeDistrito(provincia, canton, distrito))) {
            p.add(new ProblemaDatoFe("distrito", "El distrito no está configurado o no es válido.",
                ReglaDatoFe.UBICACION, true));
        }
        String otras = otrasSenasEmisor(empresa);
        if (otras.length() < 5 || otras.length() > 250) {
            p.add(new ProblemaDatoFe("otrasSenas", "Las otras señas deben tener entre 5 y 250 caracteres.",
                ReglaDatoFe.OTRAS_SENAS, true));
        }
        if (TelefonoFe.normalizar(empresa.getTelefono()).isEmpty()) {
            p.add(new ProblemaDatoFe("telefono", "El teléfono no está configurado o no es válido.",
                ReglaDatoFe.TELEFONO, true));
        }
        if (!correoValido(recortar(empresa.getEmail()))) {
            p.add(new ProblemaDatoFe("email", "El correo electrónico no está configurado o no es válido.",
                ReglaDatoFe.CORREO, true));
        }
        return p;
    }

    /**
     * Otras señas del emisor tal como van al XML: solo otrasSenas, recortadas. Sin recurso a la dirección descriptiva:
     * getDireccion() puede armarse con país y ciudad por defecto y daría otras señas que nadie escribió.
     */
    public static String otrasSenasEmisor(ConfiguracionEmpresa empresa) {
        return recortar(empresa.getOtrasSenas());
    }

    // =====================================================================
    // Reglas compartidas (también las usa el generador del XML)
    // =====================================================================

    private static void validarIdentificacion(List<ProblemaDatoFe> p, TipoIdentificacion tipo, String numero,
                                              String campoTipo, String campoNumero) {
        if (tipo == null) {
            p.add(new ProblemaDatoFe(campoTipo, "El tipo de identificación es obligatorio.",
                ReglaDatoFe.IDENTIFICACION_TIPO, true));
        }
        if (numero.isEmpty()) {
            p.add(new ProblemaDatoFe(campoNumero, "El número de identificación es obligatorio.",
                ReglaDatoFe.IDENTIFICACION_NUMERO, true));
        } else if (tipo != null && !numeroValidoPara(tipo, numero)) {
            p.add(new ProblemaDatoFe(campoNumero, mensajeNumero(tipo), ReglaDatoFe.IDENTIFICACION_NUMERO, true));
        }
    }

    /** Anexo v4.4, nota 4: física 9 dígitos sin cero inicial; jurídica 10; DIMEX 11 o 12 sin cero inicial; NITE 10. */
    static boolean numeroValidoPara(TipoIdentificacion tipo, String numero) {
        return switch (tipo) {
            case CEDULA_FISICA -> numero.matches("[1-9]\\d{8}");
            case CEDULA_JURIDICA, NITE -> numero.matches("\\d{10}");
            case DIMEX -> numero.matches("[1-9]\\d{10,11}");
        };
    }

    private static String mensajeNumero(TipoIdentificacion tipo) {
        return switch (tipo) {
            case CEDULA_FISICA -> "La cédula física debe tener 9 dígitos, sin cero inicial ni guiones.";
            case CEDULA_JURIDICA -> "La cédula jurídica debe tener 10 dígitos, sin guiones.";
            case DIMEX -> "El DIMEX debe tener 11 o 12 dígitos, sin cero inicial ni guiones.";
            case NITE -> "El NITE debe tener 10 dígitos, sin guiones.";
        };
    }

    /** Regla única de actividad (emisor y receptor), sobre el valor recortado: 6 dígitos o «NNNN.N». */
    public static boolean actividadValida(String codigo) {
        return ACTIVIDAD.matcher(recortar(codigo)).matches();
    }

    /** Correo con el patrón del Anexo y como máximo 160 caracteres; el vacío no es válido. */
    public static boolean correoValido(String correo) {
        String c = recortar(correo);
        return !c.isEmpty() && c.length() <= CORREO_MAX && CORREO.matcher(c).matches();
    }

    /**
     * Número de identificación del receptor tal como va al XML y se valida: numeroIdentificacion si no está en blanco,
     * si no identificacion; sin espacios ni guiones (el Anexo pide el número sin guiones).
     */
    public static String numeroIdentificacion(Cliente cliente) {
        String n = cliente.getNumeroIdentificacion() != null && !cliente.getNumeroIdentificacion().isBlank()
            ? cliente.getNumeroIdentificacion() : cliente.getIdentificacion();
        return numeroLimpio(n);
    }

    /** Número sin espacios ni guiones; vacío si es null. */
    public static String numeroLimpio(String numero) {
        return numero == null ? "" : numero.replaceAll("[\\s-]", "");
    }

    private static String recortar(String s) {
        return s == null ? "" : s.trim();
    }
}
