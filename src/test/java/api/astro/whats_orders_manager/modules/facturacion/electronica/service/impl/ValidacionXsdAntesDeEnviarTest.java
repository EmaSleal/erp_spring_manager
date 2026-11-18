package api.astro.whats_orders_manager.modules.facturacion.electronica.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.DatosFeDePrueba;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionDePrueba;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import api.astro.whats_orders_manager.modules.facturacion.electronica.controller.ComprobanteElectronicoController;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.ComprobanteElectronicoDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.mapper.ComprobanteElectronicoMapper;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ValidadorXsdComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.XmlGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.CalculadoraImpuestoService;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * T-036 (criterios 1 a 3, 5 y 6): el XML firmado se valida contra el XSD v4.4 antes de cualquier llamada a Hacienda.
 * Generador y firma son los reales (certificado de prueba generado con keytool); el cliente de Hacienda, el servicio de
 * configuración y el repositorio son simulados, de modo que ninguna prueba llega a Hacienda.
 */
@DisplayName("T-036 — validación contra el XSD antes de enviar a Hacienda")
class ValidacionXsdAntesDeEnviarTest {

    private static final String CLAVE = "50617112600310123456700100001010000000001100000001";
    private static final String CONSECUTIVO = "00100001010000000001";
    private static final String PIN = "t036pin";

    @TempDir
    static Path dir;
    private static Path p12;

    private final XmlGeneratorServiceImpl generadorReal =
        new XmlGeneratorServiceImpl(new CalculadoraImpuestoService(null),
            CatalogoUbicacionDePrueba.validador());
    private final ComprobanteElectronicoRepository repository = mock(ComprobanteElectronicoRepository.class);
    private final ConfiguracionHaciendaService configuracion = mock(ConfiguracionHaciendaService.class);
    private final HaciendaApiService hacienda = mock(HaciendaApiService.class);

    private ComprobanteElectronico comprobante;
    private Presentacion presentacion;
    private UnaryOperator<String> alteraXml = xml -> xml;

    @BeforeAll
    static void certificado() throws Exception {
        p12 = dir.resolve("t036-prueba.p12");
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process proceso = new ProcessBuilder(keytool, "-genkeypair", "-alias", "t036", "-keyalg", "RSA",
            "-keysize", "2048", "-validity", "30", "-storetype", "PKCS12", "-keystore", p12.toString(),
            "-storepass", PIN, "-keypass", PIN,
            "-dname", "CN=EMPRESA T036 PRUEBA, OU=CPJ, O=PERSONA JURIDICA, C=CR, SERIALNUMBER=CPJ-3-101-234567")
            .redirectErrorStream(true).start();
        assertThat(proceso.waitFor(60, TimeUnit.SECONDS)).isTrue();
        assertThat(proceso.exitValue()).as(new String(proceso.getInputStream().readAllBytes())).isZero();
    }

    @BeforeEach
    void setUp() {
        ConfiguracionEmpresa empresa = ConfiguracionEmpresa.builder().idConfiguracion(1)
            .razonSocial("Empresa T036 S.A.").numeroIdentificacion("3101234567").codigoActividad("4773.0")
            .codigoProvincia("1").canton("01").distrito("01").otrasSenas("Avenida Central, San José")
            .email("facturacion@empresa-t036.example").telefono("22223333").build();
        DatosFeDePrueba.emisor(empresa);
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente T036");
        cliente.setNumeroIdentificacion("109990999");
        cliente.setEmail("cliente@t036.example");
        DatosFeDePrueba.cliente(cliente);

        Producto producto = new Producto();
        producto.setDescripcion("Aceite");
        ArticuloMaestro maestro = new ArticuloMaestro();
        maestro.setDescripcion("Aceite");
        maestro.setCodigoCabys("1234567890123");
        producto.setArticuloMaestro(maestro);
        producto.setGravado(true);
        producto.setPorcentajeImpuesto(new BigDecimal("13"));
        producto.setPrecioIncluyeIva(true);
        presentacion = new Presentacion();
        presentacion.setNombre("Litro");
        presentacion.setEmpacado(false);
        presentacion.setCodigoUnidadFE("L");
        producto.setPresentacion(presentacion);

        Factura factura = new Factura();
        factura.setIdFactura(77);
        factura.setNumeroFactura("F-T036-77");
        factura.setCliente(cliente);
        factura.setMonedaFE(MonedaFE.CRC);
        factura.setTipoCambio(BigDecimal.ONE);
        factura.setAnulada(false);
        LineaFactura linea = new LineaFactura();
        linea.setIdLineaFactura(1);
        linea.setNumeroLinea(1);
        linea.setFactura(factura);
        linea.setProducto(producto);
        linea.setCantidad(2);
        linea.setSubtotal(new BigDecimal("2260.00"));
        linea.setPrecioUnitario(new BigDecimal("1130.00"));
        factura.getLineas().add(linea);
        factura.setTotal(new BigDecimal("2260.00"));

        comprobante = ComprobanteElectronico.builder().id(5L).factura(factura).empresa(empresa)
            .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA)
            .claveNumerica(CLAVE).consecutivo(CONSECUTIVO)
            .fechaEmision(LocalDateTime.of(2026, 11, 17, 9, 30, 15)).estado(EstadoComprobante.GENERADO).build();

        ConfiguracionHacienda config = ConfiguracionHacienda.builder().id(9L).empresa(empresa)
            .rutaCertificado(p12.toString()).pinCertificado(PIN).build();
        when(configuracion.obtenerConfiguracionActiva()).thenReturn(config);
        when(repository.findById(5L)).thenAnswer(inv -> Optional.of(comprobante));
        EnvioHaciendaDePrueba.simularBloqueoYMarca(repository, () -> comprobante);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(hacienda.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(RespuestaHaciendaDTO.builder()
            .codigoHttp(202).resultadoEnvio(ResultadoEnvioHacienda.RECIBIDO).build());
    }

    /** El único servicio de envío (T-037) sobre el servicio de comprobantes de la prueba, sin BD ni TaskScheduler. */
    private EnvioHaciendaServiceImpl envio(ComprobanteElectronicoServiceImpl service) {
        return EnvioHaciendaDePrueba.crear(repository, service, hacienda, Clock.systemUTC());
    }

    /** Servicio con generador real cuyo XML se puede alterar (para producir XML fuera del XSD) y firma real. */
    private ComprobanteElectronicoServiceImpl servicio() {
        XmlGeneratorService generador = mock(XmlGeneratorService.class);
        when(generador.generarXmlFactura(any())).thenAnswer(inv ->
            alteraXml.apply(generadorReal.generarXmlFactura(inv.getArgument(0))));
        ComprobanteElectronicoServiceImpl service = new ComprobanteElectronicoServiceImpl(repository, null, null,
            configuracion, null, generador, new FirmaDigitalServiceImpl(), hacienda, null,
            new ComprobanteElectronicoMapper(), null, null, Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"),
            ZoneOffset.UTC), null, null, new ValidadorXsdComprobante(), (c, t) -> c.siguienteConsecutivo(t),
            CatalogoUbicacionDePrueba.validador());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "directorioBase",
            dir.resolve("comprobantes-" + UUID.randomUUID()).toString());
        return service;
    }

    // ── El XML firmado válido sí se transmite ────────────────────────────────

    @Test
    @DisplayName("XML firmado válido contra el XSD: se transmite una vez con la clave del comprobante")
    void xmlValido_seTransmite() {
        ComprobanteElectronicoDTO resultado = envio(servicio()).enviar(5L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoComprobante.ENVIADO);
        assertThat(XmlValidator.erroresContraXsd(comprobante.getXmlComprobante(),
            TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
        verify(hacienda, times(1)).enviarComprobante(anyString(), anyString(), anyLong());
    }

    // ── Elemento fuera de orden y valor fuera del enum: no se transmiten ─────

    @Test
    @DisplayName("elemento fuera de orden: ERROR, sin ninguna llamada a Hacienda (ni token), errores con línea y columna")
    void elementoFueraDeOrden_noSeTransmite() {
        alteraXml = xml -> {
            String condicion = "<CondicionVenta>01</CondicionVenta>";
            assertThat(xml).contains(condicion);
            return xml.replace(condicion, "").replaceFirst("(</Clave>)", "$1" + condicion);
        };

        ComprobanteElectronicoDTO resultado = envio(servicio()).enviar(5L);

        verificarRechazoLocalPorXsd(resultado, "CondicionVenta");
    }

    @Test
    @DisplayName("UnidadMedida 'l' (fuera del enum): ERROR, sin ninguna llamada a Hacienda, con línea y columna")
    void unidadFueraDelEnum_noSeTransmite() {
        presentacion.setCodigoUnidadFE("l");

        ComprobanteElectronicoDTO resultado = envio(servicio()).enviar(5L);

        assertThat(comprobante.getXmlComprobante()).contains("<UnidadMedida>l</UnidadMedida>");
        verificarRechazoLocalPorXsd(resultado, "UnidadMedida");
    }

    private void verificarRechazoLocalPorXsd(ComprobanteElectronicoDTO resultado, String elemento) {
        verifyNoInteractions(hacienda);
        verify(configuracion, never()).verificarYRenovarToken(anyLong());
        assertThat(resultado.getEstado()).isEqualTo(EstadoComprobante.ERROR);
        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobante.ERROR);
        assertThat(comprobante.getClaveNumerica()).isEqualTo(CLAVE);
        assertThat(comprobante.getConsecutivo()).isEqualTo(CONSECUTIVO);
        assertThat(comprobante.getFechaEnvio()).isNull();
        assertThat(comprobante.getUltimoError())
            .startsWith(ComprobanteElectronicoServiceImpl.ENCABEZADO_XML_INVALIDO_XSD)
            .containsPattern("línea \\d+, columna \\d+: ")
            .contains(elemento)
            .doesNotContain("Exception").doesNotContain("\tat ");
        assertThat(comprobante.getIntentosEnvio()).isEqualTo(1);
        assertThat(resultado.getUltimoError()).isEqualTo(comprobante.getUltimoError());
    }

    @Test
    @DisplayName("más de 50 errores: se guardan 50 y el resto se resume")
    void erroresAcotados() {
        ValidadorXsdComprobante validador = mock(ValidadorXsdComprobante.class);
        when(validador.validar(anyString(), any())).thenReturn(java.util.stream.IntStream.rangeClosed(1, 80)
            .mapToObj(i -> "línea " + i + ", columna 1: error " + i).toList());
        ComprobanteElectronicoServiceImpl service = new ComprobanteElectronicoServiceImpl(repository, null, null,
            configuracion, null, mock(XmlGeneratorService.class), new FirmaDigitalServiceImpl(), hacienda, null,
            new ComprobanteElectronicoMapper(), null, null, Clock.systemUTC(), null, null, validador, (c, t) -> c.siguienteConsecutivo(t),
            CatalogoUbicacionDePrueba.validador());
        comprobante.setEstado(EstadoComprobante.FIRMADO);
        comprobante.setXmlComprobante("<x/>");

        envio(service).enviar(5L);

        assertThat(comprobante.getUltimoError().lines().count()).isEqualTo(1 + 50 + 1);
        assertThat(comprobante.getUltimoError()).contains("error 50").doesNotContain("error 51")
            .contains("y 30 errores más");
        verifyNoInteractions(hacienda);
    }

    // ── Criterio 3: corregir el dato y reenviar con la misma clave ───────────

    @Test
    @DisplayName("XML inválido → ERROR → se corrige el código ('L') → reenviar transmite una vez con la MISMA clave")
    void corregidoElDato_reenviaConLaMismaClave() {
        ComprobanteElectronicoServiceImpl service = servicio();
        presentacion.setCodigoUnidadFE("l");
        envio(service).enviar(5L);
        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobante.ERROR);
        verifyNoInteractions(hacienda);

        presentacion.setCodigoUnidadFE("L");
        ComprobanteElectronicoDTO reenviado = envio(service).reenviar(5L);

        verify(hacienda, times(1)).enviarComprobante(
            org.mockito.ArgumentMatchers.contains("<UnidadMedida>L</UnidadMedida>"),
            org.mockito.ArgumentMatchers.eq(CLAVE), org.mockito.ArgumentMatchers.eq(9L));
        assertThat(reenviado.getEstado()).isEqualTo(EstadoComprobante.ENVIADO);
        assertThat(comprobante.getClaveNumerica()).isEqualTo(CLAVE);
        assertThat(comprobante.getConsecutivo()).isEqualTo(CONSECUTIVO);
        assertThat(comprobante.getUltimoError()).isNull();
    }

    // ── Mensaje fijo al usuario ──────────────────────────────────────────────

    @Test
    @DisplayName("enviar y reenviar responden con un mensaje fijo, sin texto del validador, y 422")
    @SuppressWarnings("unchecked")
    void mensajeFijoAlUsuario() {
        presentacion.setCodigoUnidadFE("l");
        ComprobanteElectronicoServiceImpl service = servicio();
        ComprobanteElectronicoController controller = new ComprobanteElectronicoController(service, envio(service), null);

        for (ResponseEntity<Map<String, Object>> r : java.util.List.of(controller.enviarAHacienda(5L),
                controller.reenviar(5L))) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(r.getBody()).containsEntry("success", false)
                .containsEntry("message", ComprobanteElectronicoServiceImpl.MENSAJE_XML_INVALIDO_XSD);
            assertThat((String) r.getBody().get("message")).doesNotContain("línea");
        }
        verifyNoInteractions(hacienda);
    }

    // ── El mismo punto de validación sirve a tiquete, nota de crédito y nota de débito ──

    @Test
    @DisplayName("el tiquete (04) pasa por la misma validación antes de enviar")
    void tiquete_tambienSeValida() {
        comprobante.setTipoComprobante(TipoComprobanteElectronico.TIQUETE_ELECTRONICO);
        XmlGeneratorService generador = mock(XmlGeneratorService.class);
        when(generador.generarXmlTiquete(any())).thenAnswer(inv ->
            generadorReal.generarXmlTiquete(inv.getArgument(0)).replace("<CondicionVenta>01</CondicionVenta>", ""));
        ComprobanteElectronicoServiceImpl service = new ComprobanteElectronicoServiceImpl(repository, null, null,
            configuracion, null, generador, new FirmaDigitalServiceImpl(), hacienda, null,
            new ComprobanteElectronicoMapper(), null, null, Clock.systemUTC(), null, null,
            new ValidadorXsdComprobante(), (c, t) -> c.siguienteConsecutivo(t),
            CatalogoUbicacionDePrueba.validador());
        org.springframework.test.util.ReflectionTestUtils.setField(service, "directorioBase",
            dir.resolve("tiquetes").toString());

        envio(service).enviar(5L);

        assertThat(comprobante.getEstado()).isEqualTo(EstadoComprobante.ERROR);
        assertThat(comprobante.getUltimoError()).contains("CondicionVenta");
        verifyNoInteractions(hacienda);
    }

    @Test
    @DisplayName("regresión por tipo: el XML firmado de factura y tiquete valida; la nota de débito solo falla por la InformacionReferencia que falta")
    void xmlFirmadoPorTipo() {
        FirmaDigitalServiceImpl firma = new FirmaDigitalServiceImpl();
        String factura = firma.firmarXml(generadorReal.generarXmlFactura(comprobante), p12.toString(), PIN);
        assertThat(XmlValidator.erroresContraXsd(factura, TipoComprobanteElectronico.FACTURA_ELECTRONICA)).isEmpty();
        comprobante.setTipoComprobante(TipoComprobanteElectronico.TIQUETE_ELECTRONICO);
        String tiquete = firma.firmarXml(generadorReal.generarXmlTiquete(comprobante), p12.toString(), PIN);
        assertThat(XmlValidator.erroresContraXsd(tiquete, TipoComprobanteElectronico.TIQUETE_ELECTRONICO)).isEmpty();
        comprobante.setTipoComprobante(TipoComprobanteElectronico.NOTA_DEBITO);
        String debito = firma.firmarXml(generadorReal.generarXmlNotaDebito(comprobante), p12.toString(), PIN);
        // El generador de la ND no emite InformacionReferencia (obligatoria en el XSD; P-073, fuera de T-036): solo
        // ese error. Con la validación previa, una ND así queda en ERROR en lugar de transmitirse inválida.
        assertThat(XmlValidator.erroresContraXsd(debito, TipoComprobanteElectronico.NOTA_DEBITO))
            .hasSize(1).allSatisfy(e -> assertThat(e).contains("InformacionReferencia"));
    }
}
