package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.cliente.service.ClienteService;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.exception.ConfiguracionHaciendaNoDisponibleException;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.CertificadoHaciendaValidator;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.producto.service.ArticuloMaestroService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * T-043 (criterios 6, 7 y 8): la tarjeta de Hacienda del panel con la configuración activa legible, sin configuración
 * activa y con credenciales ilegibles, y la lectura del certificado (vigente, por vencer, vencido, ilegible).
 */
@DisplayName("T-043 — tarjeta de Hacienda del panel de Facturación")
class PanelFacturacionHaciendaTest {

    private static final String PIN = "pin-t043";
    private static final String USUARIO = "usuario-secreto";
    private static final String CLAVE = "clave-secreta";
    private static final String TOKEN = "token-secreto";

    @TempDir
    static Path dir;
    private static Path vigente, porVencer, vencido, corrupto;

    private final ConfiguracionHaciendaService configuracionService = mock(ConfiguracionHaciendaService.class);
    private final ConfiguracionEmpresaRepository empresaRepository = mock(ConfiguracionEmpresaRepository.class);
    private final PanelFacturacionService service = new PanelFacturacionService(
        mock(ComprobanteElectronicoRepository.class), mock(NotaFacturaRepository.class), mock(FacturaRepository.class),
        mock(ArticuloMaestroService.class), mock(ClienteService.class), configuracionService, empresaRepository,
        new CertificadoHaciendaValidator(), null,
        mock(api.astro.whats_orders_manager.modules.facturacion.correo.repository.EntregaComprobanteRepository.class),
        java.time.Clock.systemDefaultZone());

    @BeforeAll
    static void keystores() throws Exception {
        vigente = genkey("vigente", "-validity", "400");
        porVencer = genkey("porvencer", "-validity", "10");
        vencido = genkey("vencido", "-startdate", "-400d", "-validity", "30");
        corrupto = dir.resolve("corrupto.p12");
        java.nio.file.Files.write(corrupto, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
    }

    private static Path genkey(String nombre, String... extra) throws Exception {
        Path p = dir.resolve(nombre + ".p12");
        List<String> cmd = new java.util.ArrayList<>(List.of(
            Path.of(System.getProperty("java.home"), "bin", "keytool").toString(), "-genkeypair", "-alias", "k",
            "-keyalg", "RSA", "-keysize", "2048", "-dname", "CN=Empresa T043"));
        cmd.addAll(List.of(extra));
        cmd.addAll(List.of("-keystore", p.toString(), "-storetype", "PKCS12", "-storepass", PIN, "-keypass", PIN));
        Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        proc.getInputStream().readAllBytes();
        assertThat(proc.waitFor(60, TimeUnit.SECONDS)).isTrue();
        assertThat(proc.exitValue()).isZero();
        return p;
    }

    private ConfiguracionHacienda config(AmbienteHacienda ambiente, Path cert, LocalDateTime tokenExpira,
                                         String token) {
        return ConfiguracionHacienda.builder().ambiente(ambiente).usuarioHacienda(USUARIO).passwordHacienda(CLAVE)
            .rutaCertificado(cert.toString()).pinCertificado(PIN).accessToken(token).tokenExpira(tokenExpira)
            .activa(true).build();
    }

    @Test
    @DisplayName("activa legible en SANDBOX con certificado vigente: sin alerta, token vigente con hora")
    void activaLegible() {
        LocalDateTime expira = LocalDateTime.now().plusHours(1);
        when(configuracionService.obtenerConfiguracionActiva())
            .thenReturn(config(AmbienteHacienda.SANDBOX, vigente, expira, TOKEN));

        var tarjeta = service.tarjetaHacienda(Instant.now());

        assertThat(tarjeta.disponible()).isTrue();
        assertThat(tarjeta.ambiente()).isEqualTo("SANDBOX");
        assertThat(tarjeta.produccion()).isFalse();
        assertThat(tarjeta.certificadoAlerta()).isFalse();
        assertThat(tarjeta.diasParaVencer()).isGreaterThan(300);
        assertThat(tarjeta.certificadoTexto()).startsWith("Vence el ");
        assertThat(tarjeta.tokenVigente()).isTrue();
        assertThat(tarjeta.tokenExpira()).isEqualTo(expira);
    }

    @Test
    @DisplayName("PRODUCCION se marca; sin token el estado es «no»")
    void produccionSinToken() {
        when(configuracionService.obtenerConfiguracionActiva())
            .thenReturn(config(AmbienteHacienda.PRODUCCION, vigente, null, null));

        var tarjeta = service.tarjetaHacienda(Instant.now());

        assertThat(tarjeta.produccion()).isTrue();
        assertThat(tarjeta.tokenVigente()).isFalse();
    }

    @Test
    @DisplayName("la tarjeta no lleva usuario, contraseña, PIN ni token")
    void sinDatosSensibles() {
        when(configuracionService.obtenerConfiguracionActiva())
            .thenReturn(config(AmbienteHacienda.SANDBOX, vigente, LocalDateTime.now().plusHours(1), TOKEN));

        String texto = service.tarjetaHacienda(Instant.now()).toString();

        assertThat(texto).doesNotContain(USUARIO, CLAVE, PIN, TOKEN);
    }

    @Test
    @DisplayName("certificado a menos de 30 días: alerta")
    void porVencer() {
        when(configuracionService.obtenerConfiguracionActiva())
            .thenReturn(config(AmbienteHacienda.SANDBOX, porVencer, null, null));

        var tarjeta = service.tarjetaHacienda(Instant.now());

        assertThat(tarjeta.certificadoAlerta()).isTrue();
        assertThat(tarjeta.diasParaVencer()).isBetween(8L, 10L);
        assertThat(tarjeta.certificadoTexto()).startsWith("Vence el ");
    }

    @Test
    @DisplayName("certificado vencido: dice «Vencido» y alerta (validar() lo rechazaría, leerVencimiento no)")
    void vencido() {
        when(configuracionService.obtenerConfiguracionActiva())
            .thenReturn(config(AmbienteHacienda.SANDBOX, vencido, null, null));

        var tarjeta = service.tarjetaHacienda(Instant.now());

        assertThat(tarjeta.disponible()).isTrue();
        assertThat(tarjeta.certificadoAlerta()).isTrue();
        assertThat(tarjeta.certificadoTexto()).startsWith("Vencido el ");
        assertThat(tarjeta.diasParaVencer()).isNegative();
    }

    @Test
    @DisplayName("keystore que no abre: «No se pudo leer el certificado» y el resto de la tarjeta sigue")
    void certificadoIlegible() {
        when(configuracionService.obtenerConfiguracionActiva())
            .thenReturn(config(AmbienteHacienda.PRODUCCION, corrupto, null, null));

        var tarjeta = service.tarjetaHacienda(Instant.now());

        assertThat(tarjeta.disponible()).isTrue();
        assertThat(tarjeta.ambiente()).isEqualTo("PRODUCCION");
        assertThat(tarjeta.certificadoTexto()).isEqualTo(PanelFacturacionService.MENSAJE_CERTIFICADO_ILEGIBLE);
        assertThat(tarjeta.certificadoAlerta()).isTrue();
    }

    @Test
    @DisplayName("ruta de certificado nula: «No se pudo leer el certificado», sin excepción")
    void sinRutaDeCertificado() {
        var cfg = config(AmbienteHacienda.SANDBOX, vigente, null, null);
        cfg.setRutaCertificado(null);
        when(configuracionService.obtenerConfiguracionActiva()).thenReturn(cfg);

        assertThat(service.tarjetaHacienda(Instant.now()).certificadoTexto())
            .isEqualTo(PanelFacturacionService.MENSAJE_CERTIFICADO_ILEGIBLE);
    }

    @Test
    @DisplayName("sin configuración activa: aviso fijo de T-041, sin lanzar")
    void sinActiva() {
        when(configuracionService.obtenerConfiguracionActiva()).thenThrow(
            new ConfiguracionHaciendaNoDisponibleException("no hay",
                ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA));

        var tarjeta = service.tarjetaHacienda(Instant.now());

        assertThat(tarjeta.disponible()).isFalse();
        assertThat(tarjeta.aviso()).isEqualTo(ConfiguracionHaciendaNoDisponibleException.MENSAJE_NO_VALIDA);
    }

    @Test
    @DisplayName("credenciales ilegibles: aviso fijo de T-041, sin lanzar")
    void ilegible() {
        when(configuracionService.obtenerConfiguracionActiva())
            .thenThrow(new IllegalStateException("Tag mismatch"));

        var tarjeta = service.tarjetaHacienda(Instant.now());

        assertThat(tarjeta.disponible()).isFalse();
        assertThat(tarjeta.aviso()).isEqualTo(ConfiguracionHaciendaNoDisponibleException.MENSAJE_ILEGIBLE);
    }

    @Test
    @DisplayName("la lectura de la empresa que falla tampoco rompe la tarjeta")
    void fallaLaEmpresa() {
        when(empresaRepository.findFirstByOrderByIdConfiguracionAsc()).thenThrow(new IllegalStateException("db"));

        var tarjeta = service.tarjetaHacienda(Instant.now());

        assertThat(tarjeta.disponible()).isFalse();
        assertThat(Optional.ofNullable(tarjeta.aviso())).isPresent();
    }
}
