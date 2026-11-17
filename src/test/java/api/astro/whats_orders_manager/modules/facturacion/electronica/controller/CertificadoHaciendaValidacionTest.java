package api.astro.whats_orders_manager.modules.facturacion.electronica.controller;

import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ConfiguracionHaciendaMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.CertificadoHaciendaValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl.ConfiguracionHaciendaServiceImpl;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-035 (criterios 7-10): el certificado se abre con su PIN al guardar la configuración de Hacienda. Los keystores
 * se generan aquí con keytool (nunca el certificado real). Controlador y servicio reales, repositorios simulados.
 */
@DisplayName("T-035 — validación del certificado con su PIN al configurar Hacienda")
class CertificadoHaciendaValidacionTest {

    private static final String PIN = "pin-secreto-35";
    private static final String CEDULA = "3101234567";

    @TempDir
    static Path dir;

    private static Path vigente, vigenteOtraCedula, vencido, futuro, sinClave, corrupto;

    private final ConfiguracionHaciendaRepository repository = mock(ConfiguracionHaciendaRepository.class);
    private final ConfiguracionEmpresaRepository empresaRepository = mock(ConfiguracionEmpresaRepository.class);
    private MockMvc mvc;
    private ConfiguracionEmpresa empresa;

    @BeforeAll
    static void keystores() throws Exception {
        vigente = genkey("vigente", "CN=Empresa Uno, SERIALNUMBER=CPJ-3-101-234567", "-validity", "30");
        vigenteOtraCedula = genkey("otra", "CN=Otra Persona, SERIALNUMBER=CPF-1-1111-1111", "-validity", "30");
        vencido = genkey("vencido", "CN=Vencida, SERIALNUMBER=CPJ-3-101-234567", "-startdate", "-400d", "-validity", "30");
        futuro = genkey("futuro", "CN=Futura, SERIALNUMBER=CPJ-3-101-234567", "-startdate", "+10d", "-validity", "30");
        // Sin clave privada: PKCS12 con solo el certificado de confianza
        Path origen = genkey("origen", "CN=Solo Cert, SERIALNUMBER=CPJ-3-101-234567", "-validity", "30");
        Path cert = dir.resolve("origen.cer");
        keytool(origen, "-exportcert", "-alias", "k", "-file", cert.toString());
        sinClave = dir.resolve("sinclave.p12");
        keytool(sinClave, "-importcert", "-noprompt", "-alias", "c", "-file", cert.toString());
        corrupto = dir.resolve("corrupto.p12");
        Files.write(corrupto, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
    }

    private static Path genkey(String nombre, String dname, String... extra) throws Exception {
        Path p = dir.resolve(nombre + ".p12");
        List<String> args = new java.util.ArrayList<>(List.of("-genkeypair", "-alias", "k", "-keyalg", "RSA",
                "-keysize", "2048", "-dname", dname));
        args.addAll(List.of(extra));
        keytool(p, args.toArray(new String[0]));
        return p;
    }

    private static void keytool(Path store, String... args) throws Exception {
        List<String> cmd = new java.util.ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "keytool").toString()));
        cmd.addAll(List.of(args));
        cmd.addAll(List.of("-keystore", store.toString(), "-storetype", "PKCS12", "-storepass", PIN));
        if (List.of(args).contains("-genkeypair")) {
            cmd.addAll(List.of("-keypass", PIN));
        }
        Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        proc.getInputStream().readAllBytes();
        assertThat(proc.waitFor(60, TimeUnit.SECONDS)).isTrue();
        assertThat(proc.exitValue()).as("keytool %s", List.of(args)).isZero();
    }

    @BeforeEach
    void setUp() {
        empresa = ConfiguracionEmpresa.builder().idConfiguracion(1).razonSocial("Empresa")
                .numeroIdentificacion("3-101-234567").build();
        when(empresaRepository.findById(1)).thenReturn(Optional.of(empresa));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var service = new ConfiguracionHaciendaServiceImpl(repository, empresaRepository,
                new ConfiguracionHaciendaMapper(), mock(HaciendaApiService.class), new CertificadoHaciendaValidator());
        mvc = MockMvcBuilders.standaloneSetup(new ConfiguracionHaciendaController(service, new ConfiguracionHaciendaMapper()))
                .build();
    }

    private static String body(Path ruta, String pin) {
        return """
                {"empresaId":1,"ambiente":"SANDBOX","usuarioHacienda":"u","passwordHacienda":"p",
                 "rutaCertificado":"%s","pinCertificado":"%s","activa":true}""".formatted(ruta.toString().replace("\\", "\\\\"), pin);
    }

    private void crear(Path ruta, String pin, int estado, String mensaje) throws Exception {
        var r = mvc.perform(post("/api/facturas/configuracion-hacienda").contentType(MediaType.APPLICATION_JSON)
                .content(body(ruta, pin))).andExpect(status().is(estado));
        if (mensaje != null) {
            r.andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.message").value(mensaje));
        }
    }

    @Test
    @DisplayName("PIN correcto: se guarda y la respuesta trae vencimiento y titular sin aviso ni PIN")
    void pinCorrecto_seGuarda() throws Exception {
        mvc.perform(post("/api/facturas/configuracion-hacienda").contentType(MediaType.APPLICATION_JSON)
                        .content(body(vigente, PIN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.certificadoTitular").value("Empresa Uno"))
                .andExpect(jsonPath("$.certificadoVencimiento").isNotEmpty())
                .andExpect(jsonPath("$.certificadoAviso").doesNotExist())
                .andExpect(jsonPath("$.pinCertificado").value("********"));
        verify(repository).save(any(ConfiguracionHacienda.class));
    }

    @Test
    @DisplayName("PIN incorrecto: 400 con mensaje fijo y no se guarda ni se desactiva nada")
    void pinIncorrecto_400() throws Exception {
        crear(vigente, "otro-pin", 400, "El PIN es incorrecto o el archivo del certificado no se puede leer");
        verify(repository, never()).save(any());
        verify(repository, never()).findByActivaTrueOrderByIdAsc();
    }

    @Test
    @DisplayName("archivo corrupto: 400")
    void corrupto_400() throws Exception {
        crear(corrupto, PIN, 400, "El PIN es incorrecto o el archivo del certificado no se puede leer");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("archivo inexistente: 400 sin revelar la ruta ni la excepción")
    void inexistente_400() throws Exception {
        crear(dir.resolve("no-existe.p12"), PIN, 400, "El PIN es incorrecto o el archivo del certificado no se puede leer");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("sin clave privada: 400")
    void sinClavePrivada_400() throws Exception {
        crear(sinClave, PIN, 400, "El certificado no contiene una clave privada");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("vencido: 400")
    void vencido_400() throws Exception {
        crear(vencido, PIN, 400, "El certificado está vencido o aún no es vigente");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("aún no vigente: 400")
    void futuro_400() throws Exception {
        crear(futuro, PIN, 400, "El certificado está vencido o aún no es vigente");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("vigente con otra cédula: se guarda y la respuesta lleva un aviso")
    void otraCedula_seGuardaConAviso() throws Exception {
        mvc.perform(post("/api/facturas/configuracion-hacienda").contentType(MediaType.APPLICATION_JSON)
                        .content(body(vigenteOtraCedula, PIN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.certificadoTitular").value("Otra Persona"))
                .andExpect(jsonPath("$.certificadoAviso").isNotEmpty());
        verify(repository).save(any(ConfiguracionHacienda.class));
    }

    @Test
    @DisplayName("actualizar con PIN enmascarado y misma ruta no revalida; ruta nueva usa el PIN guardado (descifrado)")
    void actualizar_usaPinGuardado() throws Exception {
        ConfiguracionHacienda guardada = ConfiguracionHacienda.builder().id(3L).empresa(empresa)
                .ambiente(AmbienteHacienda.SANDBOX).usuarioHacienda("u").passwordHacienda("p").activa(true)
                .rutaCertificado(vigente.toString()).pinCertificado(PIN)
                .consecutivoFactura(1L).consecutivoTiquete(1L).consecutivoNotaCredito(1L).consecutivoNotaDebito(1L).build();
        when(repository.findById(3L)).thenReturn(Optional.of(guardada));

        // ruta nueva, PIN "********": se valida con el PIN guardado -> otra cédula pero válido
        mvc.perform(put("/api/facturas/configuracion-hacienda/3").contentType(MediaType.APPLICATION_JSON)
                        .content(body(vigenteOtraCedula, "********")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificadoTitular").value("Otra Persona"));
        assertThat(guardada.getRutaCertificado()).isEqualTo(vigenteOtraCedula.toString());
        assertThat(guardada.getPinCertificado()).isEqualTo(PIN);

        // ruta nueva que no abre con el PIN guardado: 400 y la configuración conserva la ruta anterior
        mvc.perform(put("/api/facturas/configuracion-hacienda/3").contentType(MediaType.APPLICATION_JSON)
                        .content(body(corrupto, "********")))
                .andExpect(status().isBadRequest());
        assertThat(guardada.getRutaCertificado()).isEqualTo(vigenteOtraCedula.toString());
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("el PIN nunca aparece en el log, ni en éxito ni en fallo")
    void pinNoSeLoguea() throws Exception {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        var nivel = root.getLevel();
        root.setLevel(ch.qos.logback.classic.Level.ALL);
        try {
            crear(vigente, PIN, 201, null);
            crear(vigente, "pin-malo-99", 400, null);
            crear(sinClave, PIN, 400, null);
        } finally {
            root.detachAppender(appender);
            root.setLevel(nivel);
        }
        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().contains(PIN)
                || e.getFormattedMessage().contains("pin-malo-99"));
    }
}
