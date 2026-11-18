package api.astro.whats_orders_manager.modules.facturacion.electronica.validacion;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.ProvinciaCostaRica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** T-038 (criterio 1): reglas del validador único de datos FE, sin Spring y con el catálogo simulado. */
@DisplayName("T-038 — validador de datos FE del receptor y del emisor")
class DatosFeValidatorTest {

    private final DatosFeValidator validador = CatalogoUbicacionDePrueba.validador();

    private static Cliente clienteValido() {
        Cliente c = new Cliente();
        c.setIdCliente(9);
        c.setNombre("Cliente Válido");
        c.setTipoIdentificacion(TipoIdentificacion.CEDULA_FISICA);
        c.setNumeroIdentificacion("109990999");
        return c;
    }

    private static List<String> campos(List<ProblemaDatoFe> p) {
        return p.stream().map(ProblemaDatoFe::campo).toList();
    }

    private static ProvinciaCostaRica provincia(String codigo) {
        ProvinciaCostaRica p = new ProvinciaCostaRica();
        p.setCodigo(codigo);
        return p;
    }

    @Test
    @DisplayName("cliente completo → sin problemas; los de FE opcionales ausentes no son problema")
    void valido() {
        assertThat(validador.validarReceptor(clienteValido())).isEmpty();
    }

    @ParameterizedTest(name = "nombre «{0}» → obligatorio")
    @ValueSource(strings = {"", "  ", "ab", " ab "})
    void nombreCorto(String nombre) {
        Cliente c = clienteValido();
        c.setNombre(nombre);
        List<ProblemaDatoFe> p = validador.validarReceptor(c);
        assertThat(p).singleElement().satisfies(x -> {
            assertThat(x.campo()).isEqualTo("nombre");
            assertThat(x.regla()).isEqualTo(ReglaDatoFe.NOMBRE_LONGITUD);
            assertThat(x.obligatorio()).isTrue();
        });
    }

    @Test
    @DisplayName("nombre de 101 caracteres → inválido; de 100 → válido")
    void nombreLargo() {
        Cliente c = clienteValido();
        c.setNombre("a".repeat(100));
        assertThat(validador.validarReceptor(c)).isEmpty();
        c.setNombre("a".repeat(101));
        assertThat(campos(validador.validarReceptor(c))).containsExactly("nombre");
    }

    @ParameterizedTest(name = "{0} {1} → válido={2}")
    @CsvSource({"CEDULA_FISICA,109990999,true", "CEDULA_FISICA,10999099,false", "CEDULA_FISICA,009990999,false",
        "CEDULA_FISICA,1-0999-0999,true", "CEDULA_FISICA,10999099X,false", "CEDULA_JURIDICA,3101234567,true",
        "CEDULA_JURIDICA,310123456,false", "CEDULA_JURIDICA,0101234567,true", "DIMEX,12345678901,true",
        "DIMEX,123456789012,true", "DIMEX,1234567890,false", "DIMEX,012345678901,false", "NITE,1234567890,true",
        "NITE,123456789,false"})
    void numeroSegunTipo(TipoIdentificacion tipo, String numero, boolean valido) {
        Cliente c = clienteValido();
        c.setTipoIdentificacion(tipo);
        c.setNumeroIdentificacion(numero);
        List<ProblemaDatoFe> p = validador.validarReceptor(c);
        assertThat(p.isEmpty()).isEqualTo(valido);
        if (!valido) {
            assertThat(p).singleElement().satisfies(x -> {
                assertThat(x.campo()).isEqualTo("numeroIdentificacion");
                assertThat(x.regla()).isEqualTo(ReglaDatoFe.IDENTIFICACION_NUMERO);
                assertThat(x.obligatorio()).isTrue();
            });
        }
    }

    @Test
    @DisplayName("sin tipo no se infiere: problema de tipo; sin número: problema de número; usa identificacion si falta")
    void tipoYNumeroAusentes() {
        Cliente c = clienteValido();
        c.setTipoIdentificacion(null);
        assertThat(campos(validador.validarReceptor(c))).containsExactly("tipoIdentificacion");

        c = clienteValido();
        c.setNumeroIdentificacion(" ");
        assertThat(campos(validador.validarReceptor(c))).containsExactly("numeroIdentificacion");

        c.setIdentificacion("109990999");
        assertThat(validador.validarReceptor(c)).isEmpty();
    }

    @Test
    @DisplayName("teléfono 0000000000 (valor 0 < minInclusive 100 del XSD) y correo inválido son opcionales")
    void opcionalesInvalidos() {
        Cliente c = clienteValido();
        c.setTelefono("0000000000");
        c.setEmail("sin-arroba");
        List<ProblemaDatoFe> p = validador.validarReceptor(c);
        assertThat(campos(p)).containsExactly("telefono", "email");
        assertThat(p).allMatch(x -> !x.obligatorio());
        assertThat(p).extracting(ProblemaDatoFe::regla).containsExactly(ReglaDatoFe.TELEFONO, ReglaDatoFe.CORREO);
    }

    @Test
    @DisplayName("correo en blanco = ausente, no es problema; de más de 160 caracteres sí")
    void correo() {
        Cliente c = clienteValido();
        c.setEmail("   ");
        assertThat(validador.validarReceptor(c)).isEmpty();
        c.setEmail("a".repeat(152) + "@mail.com");
        assertThat(campos(validador.validarReceptor(c))).containsExactly("email");
    }

    @Test
    @DisplayName("ubicación: completa y válida pasa; otras señas de 161 caracteres o de 4 la invalidan; catálogo")
    void ubicacion() {
        Cliente c = clienteValido();
        c.setProvincia(provincia("1"));
        c.setCanton("01");
        c.setDistrito("02");
        c.setOtrasSenas("Frente a la iglesia");
        assertThat(validador.validarReceptor(c)).isEmpty();

        c.setOtrasSenas("x".repeat(161));
        assertThat(campos(validador.validarReceptor(c))).containsExactly("otrasSenas");
        c.setOtrasSenas("1234");
        assertThat(campos(validador.validarReceptor(c))).containsExactly("otrasSenas");

        c.setOtrasSenas("Frente a la iglesia");
        c.setCanton("00");
        assertThat(campos(validador.validarReceptor(c))).containsExactly("canton", "distrito");
        c.setCanton("01");
        c.setProvincia(provincia("9"));
        assertThat(campos(validador.validarReceptor(c))).containsExactly("provincia", "canton", "distrito");
    }

    @Test
    @DisplayName("ubicación parcial (solo otras señas) → problemas; ninguna parte → ninguno")
    void ubicacionParcial() {
        Cliente c = clienteValido();
        c.setOtrasSenas("Frente a la iglesia");
        assertThat(campos(validador.validarReceptor(c))).containsExactly("provincia", "canton", "distrito");
    }

    @ParameterizedTest(name = "actividad «{0}» → válida={1}")
    @CsvSource({"620100,true", "4773.0,false", "62010,false", "6201000,false", "abcdef,false"})
    void actividad(String codigo, boolean valida) {
        Cliente c = clienteValido();
        c.setCodigoActividad(codigo);
        assertThat(validador.validarReceptor(c).isEmpty()).isEqualTo(valida);
    }

    // ── Emisor ──────────────────────────────────────────────────────────────

    private static ConfiguracionEmpresa emisorValido() {
        return DatosFeDePrueba.emisor(new ConfiguracionEmpresa());
    }

    @Test
    @DisplayName("emisor completo → sin problemas")
    void emisorValido_sinProblemas() {
        assertThat(validador.validarEmisor(emisorValido())).isEmpty();
    }

    @Test
    @DisplayName("emisor sin distrito, sin tipo, sin teléfono, sin correo y sin otras señas → todos obligatorios")
    void emisorIncompleto() {
        ConfiguracionEmpresa e = emisorValido();
        e.setDistrito(null);
        e.setTipoIdentificacion(null);
        e.setTelefono("0000000000");
        e.setEmail(null);
        e.setOtrasSenas("abc");
        List<ProblemaDatoFe> p = validador.validarEmisor(e);
        assertThat(campos(p)).containsExactlyInAnyOrder("tipoIdentificacion", "distrito", "otrasSenas", "telefono",
            "email");
        assertThat(p).allMatch(ProblemaDatoFe::obligatorio);
    }

    @Test
    @DisplayName("emisor: razón social de 4 caracteres, número jurídico de 9 dígitos, otras señas de 251")
    void emisorLimites() {
        ConfiguracionEmpresa e = emisorValido();
        e.setRazonSocial("abcd");
        e.setNumeroIdentificacion("310123456");
        e.setOtrasSenas("x".repeat(251));
        assertThat(campos(validador.validarEmisor(e))).containsExactlyInAnyOrder("razonSocial", "numeroIdentificacion",
            "otrasSenas");
        e.setOtrasSenas("x".repeat(250));
        assertThat(campos(validador.validarEmisor(e))).doesNotContain("otrasSenas");
    }

    @Test
    @DisplayName("emisor: sin otras señas no se recurre a la dirección descriptiva")
    void emisorSinOtrasSenas() {
        ConfiguracionEmpresa e = emisorValido();
        e.setOtrasSenas(null);
        e.setDireccionCalle("Avenida Central 100 metros norte");
        assertThat(campos(validador.validarEmisor(e))).containsExactly("otrasSenas");
    }

    @Test
    @DisplayName("los mensajes son fijos: no incluyen el valor del dato")
    void mensajesSinDatos() {
        Cliente c = clienteValido();
        c.setNumeroIdentificacion("12345");
        c.setTelefono("0000000000");
        c.setEmail("secreto-sin-arroba");
        assertThat(validador.validarReceptor(c)).allSatisfy(x -> assertThat(x.mensaje())
            .doesNotContain("12345").doesNotContain("0000000000").doesNotContain("secreto"));
    }
}
