package api.astro.whats_orders_manager.modules.facturacion;

import api.astro.whats_orders_manager.config.MysqlZonaCostaRica;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.model.ParametroContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.service.AsientoContableService;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.dto.RespuestaHaciendaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MensajeHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.ResultadoEnvioHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.TipoComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ConfiguracionHaciendaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.FirmaDigitalService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.XmlGeneratorService;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.FuenteTipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.enums.MetodoPago;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoPago;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.Pago;
import api.astro.whats_orders_manager.modules.facturacion.model.TipoCambio;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.PagoService;
import api.astro.whats_orders_manager.modules.facturacion.service.TipoCambioService;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ArticuloMaestroRepository;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import api.astro.whats_orders_manager.config.RelojCostaRicaFijable;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * T-048 (ADR-053, 054, 055, 056, 057) — saldo a favor del cliente de punta a punta sobre MySQL: reparto de la nota entre
 * cuentas por cobrar y saldo a favor, aplicación manual a otra factura (CRC y USD con diferencial por tramos), todas las
 * validaciones, tipo de cambio faltante, parámetro ausente, concurrencia y reversión.
 *
 * Base: MySQL 8 en Testcontainers (t048-it-mysql, tmpfs, sin volúmenes); esquema de Hibernate. El generador de XML, la
 * firma y el validador XSD son los reales; el cliente de Hacienda está simulado y ninguna prueba lo llama.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("T-048 — saldo a favor del cliente (MySQL)")
class SaldoFavorIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = MysqlZonaCostaRica.contenedorConRutinas("t048", "t048-it-mysql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    private static final String USUARIO = "admin.bootstrap@example.invalid";

    // Mensajes fijos del 409 (criterio 9 y 11)
    private static final String MSG_CLIENTE = "El saldo a favor solo se aplica a facturas del mismo cliente";
    private static final String MSG_MONEDA = "El saldo a favor solo se aplica a facturas en la misma moneda";
    private static final String MSG_MONTO = "El monto debe ser mayor a 0 y tener a lo sumo dos decimales";
    private static final String MSG_EXCEDE_SALDO = "El monto excede el saldo vivo del saldo a favor";
    private static final String MSG_EXCEDE_FACTURA = "El monto excede el saldo pendiente de la factura destino";
    private static final String MSG_ANULADA = "La factura destino está anulada";
    private static final String MSG_ACREDITADA = "La factura destino está acreditada totalmente";
    private static final String MSG_ORIGEN = "El saldo a favor no se aplica a la factura que lo originó";
    private static final String MSG_NO_VIGENTE = "El saldo a favor no está vigente";

    @MockitoBean private HaciendaApiService haciendaApi;
    @MockitoBean private ConfiguracionHaciendaService configuracionHaciendaService;

    /** T-050: reloj de la aplicación en America/Costa_Rica; una prueba lo fija a las 20:00 de CR (02:00 UTC del día siguiente). */
    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        RelojCostaRicaFijable relojCostaRica() {
            return new RelojCostaRicaFijable();
        }
    }

    @Autowired private RelojCostaRicaFijable reloj;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ArticuloMaestroRepository articuloMaestroRepository;
    @Autowired private CuentaContableRepository cuentaRepository;
    @Autowired private ParametroContableRepository parametroRepository;
    @Autowired private ConfiguracionEmpresaRepository empresaRepository;
    @Autowired private ComprobanteElectronicoRepository comprobanteRepository;
    @Autowired private ComprobanteElectronicoService comprobanteService;
    @Autowired private ConfiguracionHaciendaRepository configuracionHaciendaRepository;
    @Autowired private XmlGeneratorService xmlGenerator;
    @Autowired private FirmaDigitalService firmaDigital;
    @Autowired private PagoService pagoService;
    @Autowired private AsientoContableService asientoContableService;
    @Autowired private AsientoContableRepository asientoRepository;
    @Autowired private TipoCambioService tipoCambioService;

    private TransactionTemplate tx;
    private Producto gravado13;
    private Producto exento;
    private ConfiguracionHacienda configHacienda;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(s -> {
            if (parametroRepository.findByClave("VENTA_CXC_CLIENTES").isEmpty()) {
                param("VENTA_CXC_CLIENTES", cuenta("9.1.02.001", "Clientes", TipoCuenta.ACTIVO));
                param("VENTA_INGRESOS", cuenta("9.4.01.001", "Ventas", TipoCuenta.INGRESO));
                param("VENTA_IVA_POR_PAGAR", cuenta("9.2.01.002", "IVA por pagar", TipoCuenta.PASIVO));
                param("NOTA_CREDITO_DEVOLUCIONES", cuenta("9.4.02.001", "Devoluciones", TipoCuenta.INGRESO));
                param("NOTA_CREDITO_ANULACIONES_DESCUENTOS", cuenta("9.4.02.002", "Anulaciones", TipoCuenta.INGRESO));
                param("COBRO_CAJA_EFECTIVO", cuenta("9.1.01.001", "Caja", TipoCuenta.ACTIVO));
                param("DIFERENCIAL_CAMBIARIO_INGRESO", cuenta("9.4.20.001", "Dif. ganado", TipoCuenta.INGRESO));
                param("DIFERENCIAL_CAMBIARIO_GASTO", cuenta("9.5.20.001", "Dif. perdido", TipoCuenta.EGRESO));
            }
            if (parametroRepository.findByClave("SALDO_FAVOR_CLIENTES_CRC").isEmpty()) {
                param("SALDO_FAVOR_CLIENTES_CRC", cuenta("9.2.05.001", "Saldos a favor CRC", TipoCuenta.PASIVO));
                param("SALDO_FAVOR_CLIENTES_USD", cuenta("9.2.05.002", "Saldos a favor USD", TipoCuenta.PASIVO));
            }
            ConfiguracionEmpresa empresa = empresaRepository.findFirstByOrderByIdConfiguracionAsc()
                    .orElseGet(() -> ConfiguracionEmpresa.builder().razonSocial("Empresa T048 S.A.").build());
            empresa.setNumeroIdentificacion("3101234567");
            empresa.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_JURIDICA);
            empresa.setCodigoActividad("4773.0");
            empresa.setEmail("facturacion@t048.example");
            empresa.setCodigoProvincia("1");
            empresa.setCanton("01");
            empresa.setDistrito("01");
            empresa.setTelefono("22223333");
            empresa.setOtrasSenas("Avenida Central, San José");
            api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionSembrado.sembrar(jdbc);
            empresa = empresaRepository.save(empresa);
            if (configHacienda == null) {
                final ConfiguracionEmpresa empresaDeLaConfig = empresa;
                configHacienda = configuracionHaciendaRepository.findByActivaTrueOrderByIdAsc().stream().findFirst()
                        .orElseGet(() -> configuracionHaciendaRepository.save(ConfiguracionHacienda.builder()
                                .empresa(empresaDeLaConfig)
                                .ambiente(api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda.SANDBOX)
                                .usuarioHacienda("cpj-3-101-234567@stag.comprobanteselectronicos.go.cr")
                                .passwordHacienda("clave-de-prueba").activa(true)
                                .rutaCertificado(CertificadoDePrueba.ruta()).pinCertificado(CertificadoDePrueba.PIN)
                                .build()));
            }
            Long idConfig = configHacienda.getId();
            when(configuracionHaciendaService.obtenerConfiguracionActiva())
                    .thenAnswer(inv -> configuracionHaciendaRepository.findById(idConfig).orElseThrow());
            gravado13 = producto("T48 Producto A gravado 13 %", "1130.00", true);
            exento = producto("T48 Producto B exento", "500.00", false);
        });
        when(haciendaApi.enviarComprobante(anyString(), anyString(), anyLong())).thenReturn(timeoutDeLectura());
    }

    // ── Criterio 6, 7, 8: reparto de la nota ─────────────────────────────────

    @Test
    @DisplayName("reparto sin pagos: todo el total de la nota va a CxC, sin saldo a favor ni línea 2.1.05")
    void reparto_sinPagos() throws Exception {
        int f = facturaAceptada(null, MonedaFE.CRC, "1");
        Nota n = emitirParcialOk(f, "02", "Sin pagos", acreditar(lineas(f).get(0), "565.00"));
        aceptar(n);

        assertThat(estadoNota(n.id())).isEqualTo("APLICADA");
        long asiento = asientoDeNota(n.id());
        assertThat(haberEn(asiento, "9.1.02.001")).isEqualByComparingTo("565.00");
        assertThat(haberEn(asiento, "9.2.05.001")).isZero();
        assertCuadra(asiento);
        assertThat(saldosDeNota(n.id())).isZero();
        Map<String, Object> fila = jdbc.queryForMap("SELECT monto_credito_cxc, monto_saldo_favor FROM nota_factura "
                + "WHERE id = ?", n.id());
        assertThat((BigDecimal) fila.get("monto_credito_cxc")).isEqualByComparingTo("565.00");
        assertThat((BigDecimal) fila.get("monto_saldo_favor")).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("reparto con pago parcial: CxC = saldo pendiente, excedente a 2.1.05.001 y saldo a favor con su auxiliar")
    void reparto_conPagoParcial() throws Exception {
        int f = facturaAceptada(null, MonedaFE.CRC, "1");
        pagar(f, "1000.00", MonedaFE.CRC, null);
        Nota n = emitirParcialOk(f, "02", "Con pago parcial", acreditar(lineas(f).get(0), "1130.00"));
        aceptar(n);

        long asiento = asientoDeNota(n.id());
        assertThat(debeEn(asiento, "9.4.02.002")).isEqualByComparingTo("1000.00");
        assertThat(debeEn(asiento, "9.2.01.002")).isEqualByComparingTo("130.00");
        assertThat(haberEn(asiento, "9.1.02.001")).as("saldo pendiente 630").isEqualByComparingTo("630.00");
        assertThat(haberEn(asiento, "9.2.05.001")).as("excedente").isEqualByComparingTo("500.00");
        assertCuadra(asiento);
        Map<String, Object> s = jdbc.queryForMap("SELECT * FROM saldos_favor_cliente WHERE nota_factura_id = ?", n.id());
        assertThat((BigDecimal) s.get("monto_original")).isEqualByComparingTo("500.00");
        assertThat((BigDecimal) s.get("saldo_vivo")).isEqualByComparingTo("500.00");
        assertThat((BigDecimal) s.get("tipo_cambio_origen")).isEqualByComparingTo("1");
        assertThat(s.get("moneda")).isEqualTo("CRC");
        assertThat(s.get("estado")).isEqualTo("VIGENTE");
        assertThat(s.get("clave_nota")).isEqualTo(claveDeLaNota(n.id()));
        assertThat(((Number) s.get("factura_origen_id")).intValue()).isEqualTo(f);
        assertThat(s.get("cliente_id")).isEqualTo(clienteDe(f));
        assertThat(s.get("fecha_origen").toString()).isEqualTo(LocalDate.now().toString());
        // saldo de la factura: nunca negativo
        assertThat(saldoPendiente(f)).isEqualByComparingTo("0");
        Map<String, Object> fila = jdbc.queryForMap("SELECT monto_credito_cxc, monto_saldo_favor FROM nota_factura "
                + "WHERE id = ?", n.id());
        assertThat((BigDecimal) fila.get("monto_credito_cxc")).isEqualByComparingTo("630.00");
        assertThat((BigDecimal) fila.get("monto_saldo_favor")).isEqualByComparingTo("500.00");
        // el detalle de la nota expone el reparto
        JsonNode detalle = detalleNota(n.id());
        assertThat(detalle.get("montoCreditoCxc").decimalValue()).isEqualByComparingTo("630.00");
        assertThat(detalle.get("montoSaldoFavor").decimalValue()).isEqualByComparingTo("500.00");
        assertThat(detalle.get("idSaldoFavor").asLong()).isEqualTo(((Number) s.get("id")).longValue());
    }

    @Test
    @DisplayName("reparto con la factura pagada por completo: el saldo a favor es igual al total de la nota parcial")
    void reparto_pagadaPorCompleto() throws Exception {
        int f = facturaAceptada(null, MonedaFE.CRC, "1");
        pagar(f, "1630.00", MonedaFE.CRC, null);
        Nota n = emitirParcialOk(f, "02", "Pagada por completo", acreditar(lineas(f).get(0), "565.00"));
        aceptar(n);

        long asiento = asientoDeNota(n.id());
        assertThat(haberEn(asiento, "9.1.02.001")).isZero();
        assertThat(haberEn(asiento, "9.2.05.001")).isEqualByComparingTo("565.00");
        assertCuadra(asiento);
        assertThat(jdbc.queryForObject("SELECT monto_original FROM saldos_favor_cliente WHERE nota_factura_id = ?",
                BigDecimal.class, n.id())).isEqualByComparingTo("565.00");
    }

    @Test
    @DisplayName("nota total sobre factura pagada en parte: se anula como hoy, todo lo pagado pasa a saldo a favor y los cobros no se tocan")
    void reparto_notaTotalSobrePagada() throws Exception {
        int f = facturaAceptada(null, MonedaFE.CRC, "1");
        long idPago = pagar(f, "1000.00", MonedaFE.CRC, null);
        long asientoPago = jdbc.queryForObject("SELECT id FROM asientos_contables WHERE pago_id = ?", Long.class, idPago);
        int asientosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables", Integer.class);

        Nota n = emitirOk(f, "01", "Anulación total con pagos");
        aceptar(n);

        assertThat(estadoNota(n.id())).isEqualTo("APLICADA");
        assertThat(jdbc.queryForObject("SELECT anulada FROM factura WHERE id_factura = ?", Boolean.class, f)).isTrue();
        long asiento = asientoDeNota(n.id());
        assertThat(haberEn(asiento, "9.1.02.001")).isEqualByComparingTo("630.00");
        assertThat(haberEn(asiento, "9.2.05.001")).isEqualByComparingTo("1000.00");
        assertCuadra(asiento);
        assertThat(jdbc.queryForObject("SELECT monto_original FROM saldos_favor_cliente WHERE nota_factura_id = ?",
                BigDecimal.class, n.id())).isEqualByComparingTo("1000.00");
        // los pagos originales y sus asientos no se modifican: solo se agregó el asiento de la nota
        assertThat(jdbc.queryForObject("SELECT estado FROM pagos WHERE id_pago = ?", String.class, idPago))
                .isEqualTo("CONFIRMADO");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE asiento_revertido_id = ?",
                Integer.class, asientoPago)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables", Integer.class))
                .isEqualTo(asientosAntes + 1);
    }

    @Test
    @DisplayName("nota RECHAZADA: sin saldo a favor ni asiento")
    void notaRechazada_sinSaldoNiAsiento() throws Exception {
        int f = facturaAceptada(null, MonedaFE.CRC, "1");
        pagar(f, "1000.00", MonedaFE.CRC, null);
        Nota n = emitirParcialOk(f, "02", "Rechazada", acreditar(lineas(f).get(0), "1130.00"));
        when(haciendaApi.consultarComprobante(eq(n.clave()), anyLong())).thenReturn(consulta("rechazado"));
        comprobanteService.consultarYActualizarEstado(n.idComprobante());

        assertThat(estadoNota(n.id())).isEqualTo("RECHAZADA");
        assertThat(saldosDeNota(n.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE nota_factura_id = ?",
                Integer.class, n.id())).isZero();
    }

    @Test
    @DisplayName("el reparto se calcula al aplicar la nota: anular un pago con la nota EN_TRAMITE deja el excedente en 0")
    void reparto_seCalculaAlAplicar() throws Exception {
        int f = facturaAceptada(null, MonedaFE.CRC, "1");
        long idPago = pagar(f, "1000.00", MonedaFE.CRC, null);
        // Al emitir, el saldo es 630: el excedente sería 500
        Nota n = emitirParcialOk(f, "02", "Reparto al aplicar", acreditar(lineas(f).get(0), "1130.00"));
        assertThat(estadoNota(n.id())).isEqualTo("EN_TRAMITE");
        // ADR-037 §3 sigue vigente: no se registran pagos con la nota en trámite
        assertThat(registrarPagoRest(f).getResponse().getStatus()).isEqualTo(409);
        // pero un pago sí se puede anular por el camino normal: el saldo vuelve a 1630
        tx.executeWithoutResult(s -> pagoService.anularPago(idPago, "Pago registrado por error", 1));

        aceptar(n);

        long asiento = asientoDeNota(n.id());
        assertThat(haberEn(asiento, "9.1.02.001")).isEqualByComparingTo("1130.00");
        assertThat(haberEn(asiento, "9.2.05.001")).isZero();
        assertCuadra(asiento);
        assertThat(saldosDeNota(n.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT monto_saldo_favor FROM nota_factura WHERE id = ?", BigDecimal.class,
                n.id())).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("parámetro de saldos a favor ausente: la nota con excedente queda EN_TRAMITE con el mensaje; sin excedente se aplica")
    void reparto_parametroAusente(CapturedOutput salida) throws Exception {
        int f = facturaAceptada(null, MonedaFE.CRC, "1");
        pagar(f, "1000.00", MonedaFE.CRC, null);
        Nota n = emitirParcialOk(f, "02", "Sin parámetro", acreditar(lineas(f).get(0), "1130.00"));
        Long idCuenta = jdbc.queryForObject("SELECT cuenta_contable_id FROM parametros_contables WHERE clave = "
                + "'SALDO_FAVOR_CLIENTES_CRC'", Long.class);
        jdbc.update("DELETE FROM parametros_contables WHERE clave = 'SALDO_FAVOR_CLIENTES_CRC'");
        try {
            when(haciendaApi.consultarComprobante(eq(n.clave()), anyLong())).thenReturn(consulta("aceptado"));
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> comprobanteService.consultarYActualizarEstado(n.idComprobante()));

            assertThat(estadoNota(n.id())).isEqualTo("EN_TRAMITE");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE nota_factura_id = ?",
                    Integer.class, n.id())).isZero();
            assertThat(saldosDeNota(n.id())).isZero();
            assertThat(salida.getAll()).contains("Parámetro contable no configurado: SALDO_FAVOR_CLIENTES_CRC");
        } finally {
            jdbc.update("INSERT INTO parametros_contables (clave, cuenta_contable_id) VALUES (?, ?)",
                    "SALDO_FAVOR_CLIENTES_CRC", idCuenta);
        }
        comprobanteService.consultarYActualizarEstado(n.idComprobante());
        assertThat(estadoNota(n.id())).isEqualTo("APLICADA");
        assertThat(saldosDeNota(n.id())).isEqualTo(1);
    }

    // ── Criterio 3 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("el saldo vivo nunca es negativo: la base lo rechaza")
    void saldoVivoNoNegativo() throws Exception {
        Saldo s = saldoCrc(null);
        // MySQL 8.0.32 informa el CHECK violado (error 3819) como SQLException genérica
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("UPDATE saldos_favor_cliente SET saldo_vivo = -0.01 WHERE id = ?", s.id()));
    }

    // ── Criterio 9, 10: aplicación en CRC ────────────────────────────────────

    @Test
    @DisplayName("aplicación en CRC: pago NOTA_CREDITO, saldo vivo baja, asiento DR 2.1.05.001 / CR CxC sin diferencial; al agotarse queda AGOTADO")
    void aplicarCrc() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");

        MvcResult r = aplicar(s.id(), f2, "300.00");

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        long idAplicacion = objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
        Map<String, Object> a = jdbc.queryForMap("SELECT * FROM aplicaciones_saldo_favor WHERE id = ?", idAplicacion);
        assertThat(a.get("estado")).isEqualTo("VIGENTE");
        assertThat((BigDecimal) a.get("monto")).isEqualByComparingTo("300.00");
        assertThat((BigDecimal) a.get("tipo_cambio_origen")).isEqualByComparingTo("1");
        assertThat((BigDecimal) a.get("tipo_cambio_factura")).isEqualByComparingTo("1");
        assertThat(a.get("tc_dia_valor")).isNull();
        assertThat((BigDecimal) a.get("diferencial_neto")).isEqualByComparingTo("0");
        assertThat(a.get("usuario_id")).isNotNull();
        Map<String, Object> pago = jdbc.queryForMap("SELECT tipo_pago, estado, monto, monto_aplicado, moneda, id_factura "
                + "FROM pagos WHERE id_pago = ?", a.get("pago_id"));
        assertThat(pago.get("tipo_pago")).isEqualTo("NOTA_CREDITO");
        assertThat(pago.get("estado")).isEqualTo("CONFIRMADO");
        assertThat((BigDecimal) pago.get("monto_aplicado")).isEqualByComparingTo("300.00");
        assertThat(((Number) pago.get("id_factura")).intValue()).isEqualTo(f2);
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("200.00");
        assertThat(saldoPendiente(f2)).isEqualByComparingTo("1330.00");
        assertThat(jdbc.queryForObject("SELECT estado_pago FROM factura WHERE id_factura = ?", String.class, f2))
                .isEqualTo("PAGADO_PARCIAL");
        long asiento = ((Number) a.get("asiento_id")).longValue();
        assertThat(debeEn(asiento, "9.2.05.001")).isEqualByComparingTo("300.00");
        assertThat(haberEn(asiento, "9.1.02.001")).isEqualByComparingTo("300.00");
        assertThat(debeEn(asiento, "9.5.20.001")).isZero();
        assertThat(haberEn(asiento, "9.4.20.001")).isZero();
        assertThat(haberEn(asiento, "9.1.01.001")).as("no es un cobro en caja").isZero();
        assertCuadra(asiento);
        // un NOTA_CREDITO no es dinero: no entra en el reporte de caja ni en los ingresos del día
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE pago_id = ?", Integer.class,
                a.get("pago_id"))).isEqualTo(1);

        // el resto agota el saldo
        MvcResult r2 = aplicar(s.id(), f2, "200.00");
        assertThat(r2.getResponse().getStatus()).as(r2.getResponse().getContentAsString()).isEqualTo(201);
        assertThat(saldoVivo(s.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT estado FROM saldos_favor_cliente WHERE id = ?", String.class, s.id()))
                .isEqualTo("AGOTADO");
    }

    @Test
    @DisplayName("un pago NOTA_CREDITO no se puede registrar, rechazar ni anular por el módulo de pagos: solo se reversa desde la aplicación")
    void pagoNotaCredito_protegido() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        aplicar(s.id(), f2, "100.00");
        long idPago = jdbc.queryForObject("SELECT id_pago FROM pagos WHERE tipo_pago = 'NOTA_CREDITO' AND id_factura = ?",
                Long.class, f2);

        MvcResult anular = mockMvc.perform(put("/pagos/" + idPago + "/anular").param("motivo", "Intento directo")
                .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn();

        assertThat(anular.getResponse().getStatus()).isGreaterThanOrEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT estado FROM pagos WHERE id_pago = ?", String.class, idPago))
                .isEqualTo("CONFIRMADO");
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("400.00");
    }

    // ── Criterio 11: USD con TC distintos ────────────────────────────────────

    @Test
    @DisplayName("USD, diferencial perdido: TC origen 500, factura destino 520, día 540 → tramos 8000 y 4000, neto -4000 = DR gasto 4000")
    void aplicarUsd_diferencialPerdido() throws Exception {
        Saldo s = saldoUsd("500");
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.USD, "520");
        tcDelDia("540.00000");

        MvcResult r = aplicar(s.id(), f2, "200.00");

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        Map<String, Object> a = jdbc.queryForMap("SELECT * FROM aplicaciones_saldo_favor WHERE saldo_favor_id = ?",
                s.id());
        assertThat((BigDecimal) a.get("tipo_cambio_origen")).isEqualByComparingTo("500");
        assertThat((BigDecimal) a.get("tipo_cambio_factura")).isEqualByComparingTo("520");
        assertThat(a.get("tc_dia_fecha").toString()).isEqualTo(LocalDate.now().toString());
        assertThat((BigDecimal) a.get("tc_dia_valor")).isEqualByComparingTo("540");
        assertThat(a.get("tc_dia_tipo")).isEqualTo("VENTA");
        assertThat(a.get("tc_dia_fuente")).isEqualTo("BCCR");
        assertThat(a.get("tc_dia_origen")).isEqualTo("API");
        assertThat((BigDecimal) a.get("diferencial_saldo")).isEqualByComparingTo("8000.00");
        assertThat((BigDecimal) a.get("diferencial_cxc")).isEqualByComparingTo("4000.00");
        assertThat((BigDecimal) a.get("diferencial_neto")).isEqualByComparingTo("-4000.00");
        long asiento = ((Number) a.get("asiento_id")).longValue();
        assertThat(debeEn(asiento, "9.2.05.002")).isEqualByComparingTo("100000.00");
        assertThat(haberEn(asiento, "9.1.02.001")).isEqualByComparingTo("104000.00");
        assertThat(debeEn(asiento, "9.5.20.001")).isEqualByComparingTo("4000.00");
        assertThat(haberEn(asiento, "9.4.20.001")).isZero();
        assertCuadra(asiento);
        // el neto de los tramos es la diferencia del asiento (ganancia positiva, pérdida negativa)
        assertThat(((BigDecimal) a.get("diferencial_neto")))
                .isEqualByComparingTo(haberEn(asiento, "9.4.20.001").subtract(debeEn(asiento, "9.5.20.001")));
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("300.00");
        assertThat(saldoPendiente(f2)).isEqualByComparingTo("1430.00");
    }

    @Test
    @DisplayName("USD, diferencial ganado: TC origen 500, factura destino 480, día 540 (MANUAL) → tramos 8000 y 12000, neto +4000 = CR ingreso 4000")
    void aplicarUsd_diferencialGanado() throws Exception {
        Saldo s = saldoUsd("500");
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.USD, "480");
        tcDelDia("540.00000", FuenteTipoCambio.MANUAL);

        MvcResult r = aplicar(s.id(), f2, "200.00");

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        Map<String, Object> a = jdbc.queryForMap("SELECT * FROM aplicaciones_saldo_favor WHERE saldo_favor_id = ?",
                s.id());
        assertThat(a.get("tc_dia_fuente")).isEqualTo("MANUAL");
        assertThat(a.get("tc_dia_origen")).isEqualTo("MANUAL");
        assertThat((BigDecimal) a.get("diferencial_saldo")).isEqualByComparingTo("8000.00");
        assertThat((BigDecimal) a.get("diferencial_cxc")).isEqualByComparingTo("12000.00");
        assertThat((BigDecimal) a.get("diferencial_neto")).isEqualByComparingTo("4000.00");
        long asiento = ((Number) a.get("asiento_id")).longValue();
        assertThat(debeEn(asiento, "9.2.05.002")).isEqualByComparingTo("100000.00");
        assertThat(haberEn(asiento, "9.1.02.001")).isEqualByComparingTo("96000.00");
        assertThat(haberEn(asiento, "9.4.20.001")).isEqualByComparingTo("4000.00");
        assertCuadra(asiento);
    }

    @Test
    @DisplayName("USD sin tipo de cambio de la fecha exacta: 409 con el mensaje, sin fallback a la tasa anterior y sin crear nada")
    void aplicarUsd_sinTipoDeCambioDelDia() throws Exception {
        Saldo s = saldoUsd("500");
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.USD, "520");
        // hay tasa de ayer, pero no de hoy
        tcDelDia("540.00000");
        jdbc.update("UPDATE tipos_cambio SET fecha = ? WHERE fecha = ?", LocalDate.now().minusDays(1).toString(),
                LocalDate.now().toString());
        Foto antes = foto(s.id());

        MvcResult r = aplicar(s.id(), f2, "200.00");

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(mensaje(r)).isEqualTo("No hay tipo de cambio registrado para " + LocalDate.now()
                + "; regístrelo antes de aplicar el saldo");
        assertThat(foto(s.id())).isEqualTo(antes);
    }

    // ── Criterio 9: validaciones ─────────────────────────────────────────────

    @Test
    @DisplayName("validaciones del 409: cada una con su mensaje fijo y sin crear nada")
    void validaciones() throws Exception {
        Saldo s = saldoCrc(null);
        int destino = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        int otroCliente = facturaAceptada(null, MonedaFE.CRC, "1");
        int enUsd = facturaAceptada(s.idCliente(), MonedaFE.USD, "500");
        int anulada = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        jdbc.update("UPDATE factura SET anulada = b'1' WHERE id_factura = ?", anulada);
        int acreditada = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        jdbc.update("UPDATE factura SET acreditada_totalmente = b'1', monto_acreditado = 1630 WHERE id_factura = ?",
                acreditada);
        int casiPagada = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        pagar(casiPagada, "1500.00", MonedaFE.CRC, null);
        Foto antes = foto(s.id());

        Map<String, Object[]> casos = new LinkedHashMap<>();
        casos.put("otro cliente", new Object[]{otroCliente, "100.00", MSG_CLIENTE});
        casos.put("otra moneda", new Object[]{enUsd, "100.00", MSG_MONEDA});
        casos.put("monto cero", new Object[]{destino, "0", MSG_MONTO});
        casos.put("monto negativo", new Object[]{destino, "-5.00", MSG_MONTO});
        casos.put("tres decimales", new Object[]{destino, "10.001", MSG_MONTO});
        casos.put("excede el saldo vivo", new Object[]{destino, "500.01", MSG_EXCEDE_SALDO});
        casos.put("excede el pendiente", new Object[]{casiPagada, "200.00", MSG_EXCEDE_FACTURA});
        casos.put("destino anulada", new Object[]{anulada, "100.00", MSG_ANULADA});
        casos.put("destino acreditada", new Object[]{acreditada, "100.00", MSG_ACREDITADA});
        casos.put("factura de origen", new Object[]{s.idFacturaOrigen(), "100.00", MSG_ORIGEN});
        for (Map.Entry<String, Object[]> caso : casos.entrySet()) {
            Object[] c = caso.getValue();
            MvcResult r = aplicar(s.id(), (Integer) c[0], (String) c[1]);
            assertThat(r.getResponse().getStatus()).as(caso.getKey() + ": " + r.getResponse().getContentAsString())
                    .isEqualTo(409);
            assertThat(mensaje(r)).as(caso.getKey()).isEqualTo(c[2]);
            assertThat(foto(s.id())).as(caso.getKey()).isEqualTo(antes);
        }

        // saldo agotado
        jdbc.update("UPDATE saldos_favor_cliente SET estado = 'AGOTADO', saldo_vivo = 0 WHERE id = ?", s.id());
        Foto agotado = foto(s.id());
        MvcResult r = aplicar(s.id(), destino, "1.00");
        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(mensaje(r)).isEqualTo(MSG_NO_VIGENTE);
        assertThat(foto(s.id())).isEqualTo(agotado);
    }

    @Test
    @DisplayName("la factura destino con una nota EN_TRAMITE no admite la aplicación (ADR-037 §3): 409 sin crear nada")
    void destinoConNotaEnTramite() throws Exception {
        Saldo s = saldoCrc(null);
        int destino = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        Nota n = emitirParcialOk(destino, "02", "En trámite", acreditar(lineas(destino).get(0), "113.00"));
        Foto antes = foto(s.id());

        MvcResult r = aplicar(s.id(), destino, "100.00");

        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(mensaje(r)).contains("nota de crédito " + n.id());
        assertThat(foto(s.id())).isEqualTo(antes);
    }

    @Test
    @DisplayName("parámetro contable del saldo ausente al aplicar: 409 «Parámetro contable no configurado: <clave>» y no se crea nada")
    void aplicar_parametroAusente() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        Foto antes = foto(s.id());
        Long idCuenta = jdbc.queryForObject("SELECT cuenta_contable_id FROM parametros_contables WHERE clave = "
                + "'SALDO_FAVOR_CLIENTES_CRC'", Long.class);
        jdbc.update("DELETE FROM parametros_contables WHERE clave = 'SALDO_FAVOR_CLIENTES_CRC'");
        try {
            MvcResult r = aplicar(s.id(), f2, "100.00");

            assertThat(r.getResponse().getStatus()).isEqualTo(409);
            assertThat(mensaje(r)).isEqualTo("Parámetro contable no configurado: SALDO_FAVOR_CLIENTES_CRC");
            assertThat(foto(s.id())).isEqualTo(antes);
        } finally {
            jdbc.update("INSERT INTO parametros_contables (clave, cuenta_contable_id) VALUES (?, ?)",
                    "SALDO_FAVOR_CLIENTES_CRC", idCuenta);
        }
    }

    // ── Criterio 12: concurrencia ────────────────────────────────────────────

    @Test
    @DisplayName("dos aplicaciones concurrentes que juntas exceden el saldo: una se registra y la otra recibe 409")
    void aplicacionesConcurrentes() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        int f3 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tareas = new ArrayList<>();
            for (int destino : new int[]{f2, f3}) {
                tareas.add(() -> {
                    salida.await();
                    return aplicar(s.id(), destino, "300.00").getResponse().getStatus();
                });
            }
            List<Future<Integer>> futuros = new ArrayList<>();
            for (Callable<Integer> t : tareas) {
                futuros.add(pool.submit(t));
            }
            salida.countDown();
            List<Integer> estados = new ArrayList<>();
            for (Future<Integer> f : futuros) {
                estados.add(f.get(60, TimeUnit.SECONDS));
            }
            assertThat(estados).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("200.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aplicaciones_saldo_favor WHERE saldo_favor_id = ? "
                + "AND estado = 'VIGENTE'", Integer.class, s.id())).isEqualTo(1);
    }

    // ── Criterio 13: reversión ───────────────────────────────────────────────

    @Test
    @DisplayName("reversión: asiento espejo con fecha de hoy, pago anulado, saldo restituido a VIGENTE, factura recalculada; nada se borra; doble reversión → 409")
    void reversion() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        aplicar(s.id(), f2, "500.00");
        assertThat(jdbc.queryForObject("SELECT estado FROM saldos_favor_cliente WHERE id = ?", String.class, s.id()))
                .isEqualTo("AGOTADO");
        Map<String, Object> a = jdbc.queryForMap("SELECT * FROM aplicaciones_saldo_favor WHERE saldo_favor_id = ?",
                s.id());
        long idAplicacion = ((Number) a.get("id")).longValue();
        long asientoOriginal = ((Number) a.get("asiento_id")).longValue();
        long idPago = ((Number) a.get("pago_id")).longValue();
        int pagosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM pagos", Integer.class);
        int asientosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables", Integer.class);

        MvcResult r = reversar(idAplicacion, "Aplicada a la factura equivocada");

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        Map<String, Object> despues = jdbc.queryForMap("SELECT * FROM aplicaciones_saldo_favor WHERE id = ?",
                idAplicacion);
        assertThat(despues.get("estado")).isEqualTo("REVERSADA");
        assertThat(despues.get("razon_reversion")).isEqualTo("Aplicada a la factura equivocada");
        assertThat(despues.get("reversado_por")).isNotNull();
        assertThat(despues.get("fecha_reversion")).isNotNull();
        long asientoReversion = ((Number) despues.get("asiento_reversion_id")).longValue();
        assertThat(jdbc.queryForObject("SELECT asiento_revertido_id FROM asientos_contables WHERE id = ?", Long.class,
                asientoReversion)).isEqualTo(asientoOriginal);
        assertThat(jdbc.queryForObject("SELECT fecha FROM asientos_contables WHERE id = ?", java.sql.Date.class,
                asientoReversion).toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(haberEn(asientoReversion, "9.2.05.001")).isEqualByComparingTo("500.00");
        assertThat(debeEn(asientoReversion, "9.1.02.001")).isEqualByComparingTo("500.00");
        assertCuadra(asientoReversion);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE asiento_revertido_id = ?",
                Integer.class, asientoOriginal)).as("revertido una sola vez").isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT estado FROM asientos_contables WHERE id = ?", String.class,
                asientoOriginal)).isEqualTo("CONTABILIZADO");
        assertThat(jdbc.queryForObject("SELECT estado FROM pagos WHERE id_pago = ?", String.class, idPago))
                .isEqualTo("ANULADO");
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("500.00");
        assertThat(jdbc.queryForObject("SELECT estado FROM saldos_favor_cliente WHERE id = ?", String.class, s.id()))
                .isEqualTo("VIGENTE");
        assertThat(saldoPendiente(f2)).isEqualByComparingTo("1630.00");
        assertThat(jdbc.queryForObject("SELECT estado_pago FROM factura WHERE id_factura = ?", String.class, f2))
                .isEqualTo("PENDIENTE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pagos", Integer.class)).isEqualTo(pagosAntes);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables", Integer.class))
                .isEqualTo(asientosAntes + 1);

        MvcResult otra = reversar(idAplicacion, "Otra vez");
        assertThat(otra.getResponse().getStatus()).isEqualTo(409);
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("500.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables", Integer.class))
                .isEqualTo(asientosAntes + 1);
        // el saldo restituido se puede volver a aplicar
        assertThat(aplicar(s.id(), f2, "500.00").getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("reversión: la razón es obligatoria de 3 a 180 caracteres (400) y no cambia nada")
    void reversion_razonInvalida() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        aplicar(s.id(), f2, "100.00");
        long idAplicacion = jdbc.queryForObject("SELECT id FROM aplicaciones_saldo_favor WHERE saldo_favor_id = ?",
                Long.class, s.id());

        for (String razon : new String[]{"", "ab", "x".repeat(181), "   "}) {
            MvcResult r = reversar(idAplicacion, razon);
            assertThat(r.getResponse().getStatus()).as("razón [" + razon.length() + "]").isEqualTo(400);
        }
        assertThat(jdbc.queryForObject("SELECT estado FROM aplicaciones_saldo_favor WHERE id = ?", String.class,
                idAplicacion)).isEqualTo("VIGENTE");
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("400.00");
        assertThat(reversar(idAplicacion, "x".repeat(180)).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("reversión en USD: el asiento espejo restituye también el diferencial")
    void reversionUsd() throws Exception {
        Saldo s = saldoUsd("500");
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.USD, "520");
        tcDelDia("540.00000");
        aplicar(s.id(), f2, "200.00");
        long idAplicacion = jdbc.queryForObject("SELECT id FROM aplicaciones_saldo_favor WHERE saldo_favor_id = ?",
                Long.class, s.id());

        assertThat(reversar(idAplicacion, "Reversión en dólares").getResponse().getStatus()).isEqualTo(200);

        long reversion = jdbc.queryForObject("SELECT asiento_reversion_id FROM aplicaciones_saldo_favor WHERE id = ?",
                Long.class, idAplicacion);
        assertThat(haberEn(reversion, "9.5.20.001")).isEqualByComparingTo("4000.00");
        assertCuadra(reversion);
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("500.00");
    }

    // ── Criterio 14 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("nota total sobre una factura con un saldo a favor aplicado: el pago NOTA_CREDITO cuenta como pagado en el reparto")
    void notaSobreFacturaConSaldoAplicado() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        aplicar(s.id(), f2, "300.00");

        Nota n = emitirOk(f2, "01", "Anulación de la factura con saldo aplicado");
        aceptar(n);

        assertThat(estadoNota(n.id())).isEqualTo("APLICADA");
        long asiento = asientoDeNota(n.id());
        assertThat(haberEn(asiento, "9.1.02.001")).isEqualByComparingTo("1330.00");
        assertThat(haberEn(asiento, "9.2.05.001")).isEqualByComparingTo("300.00");
        assertCuadra(asiento);
        assertThat(jdbc.queryForObject("SELECT monto_original FROM saldos_favor_cliente WHERE nota_factura_id = ?",
                BigDecimal.class, n.id())).isEqualByComparingTo("300.00");
        // la aplicación original y su saldo no cambian
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("200.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aplicaciones_saldo_favor WHERE saldo_favor_id = ? AND "
                + "estado = 'VIGENTE'", Integer.class, s.id())).isEqualTo(1);
    }

    // ── Consultas para la interfaz ───────────────────────────────────────────

    @Test
    @DisplayName("consultas: saldos VIGENTES por cliente y moneda, para una factura (sin su propio origen) y por cliente con sus aplicaciones")
    void consultas() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        aplicar(s.id(), f2, "100.00");

        JsonNode lista = getData("/api/facturas/saldos-favor?idCliente=" + s.idCliente() + "&moneda=CRC");
        assertThat(lista).hasSize(1);
        JsonNode item = lista.get(0);
        assertThat(item.get("id").asLong()).isEqualTo(s.id());
        assertThat(item.get("saldoVivo").decimalValue()).isEqualByComparingTo("400.00");
        assertThat(item.get("montoOriginal").decimalValue()).isEqualByComparingTo("500.00");
        assertThat(item.get("idNota").asLong()).isEqualTo(s.idNota());
        assertThat(item.get("claveNota").asText()).hasSize(50);
        assertThat(item.get("fechaOrigen").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(getData("/api/facturas/saldos-favor?idCliente=" + s.idCliente() + "&moneda=USD")).isEmpty();

        JsonNode paraDestino = getData("/api/facturas/saldos-favor/para-factura/" + f2);
        assertThat(paraDestino.get("saldos")).hasSize(1);
        assertThat(paraDestino.get("saldoPendiente").decimalValue()).isEqualByComparingTo("1530.00");
        JsonNode paraOrigen = getData("/api/facturas/saldos-favor/para-factura/" + s.idFacturaOrigen());
        assertThat(paraOrigen.get("saldos")).as("no se ofrece sobre su propia factura de origen").isEmpty();

        JsonNode cliente = getData("/api/facturas/saldos-favor/cliente/" + s.idCliente());
        assertThat(cliente).hasSize(1);
        JsonNode aplicaciones = cliente.get(0).get("aplicaciones");
        assertThat(aplicaciones).hasSize(1);
        assertThat(aplicaciones.get(0).get("estado").asText()).isEqualTo("VIGENTE");
        assertThat(aplicaciones.get(0).get("monto").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(aplicaciones.get(0).get("idFactura").asInt()).isEqualTo(f2);
        // el pago NOTA_CREDITO se identifica en el detalle de la factura destino con su nota de origen
        MvcResult det = mockMvc.perform(get("/facturas/detalle/" + f2).with(user(USUARIO).roles("ADMIN"))).andReturn();
        JsonNode pagos = objectMapper.readTree(det.getResponse().getContentAsString()).get("pagos");
        assertThat(pagos).hasSize(1);
        assertThat(pagos.get(0).get("tipoPago").asText()).isEqualTo("NOTA_CREDITO");
        assertThat(pagos.get(0).get("idNotaOrigen").asLong()).isEqualTo(s.idNota());
        assertThat(pagos.get(0).get("claveNotaOrigen").asText()).hasSize(50);
    }

    // ── Revisión 1 (criterios 23 a 25) ───────────────────────────────────────

    @Test
    @DisplayName("criterio 23: la aplicación de un saldo (NOTA_CREDITO) no entra en el detalle de caja ni en los pagos del día; el detalle suma igual que el total")
    void cajaYPagosDelDia_excluyenAplicacionesDeSaldo() throws Exception {
        // T-050 (criterio 11): reloj fijo a las 20:00 de Costa Rica (02:00 UTC del día siguiente), el caso que antes
        // fallaba después de las 18:00 porque la base contaba «hoy» con su propio reloj en UTC
        reloj.fijar(java.time.ZonedDateTime.of(2026, 11, 20, 20, 0, 0, 0, RelojCostaRicaFijable.CR));
        try {
            cajaYPagosDelDiaConElRelojFijo();
        } finally {
            reloj.liberar();
        }
    }

    private void cajaYPagosDelDiaConElRelojFijo() throws Exception {
        LocalDate hoy = LocalDate.now(reloj);
        assertThat(hoy).isEqualTo(LocalDate.of(2026, 11, 20));
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        long idEfectivo = pagar(f2, "1000.00", MonedaFE.CRC, null);
        MvcResult r = aplicar(s.id(), f2, "300.00");
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        long idNc = jdbc.queryForObject("SELECT id_pago FROM pagos WHERE id_factura = ? AND tipo_pago = 'NOTA_CREDITO'",
                Long.class, f2);

        var reporte = pagoService.buildReporteCaja(hoy);
        List<Long> idsDetalle = reporte.getPagosDetalle().stream().map(p -> p.getIdPago()).toList();
        assertThat(idsDetalle).contains(idEfectivo).doesNotContain(idNc);
        // el detalle (válidos, en CRC como los totales; el día puede traer pagos de otras pruebas) suma igual que el total
        assertThat(reporte.getPagosDetalle().stream().filter(p -> p.esValido())
                .map(p -> api.astro.whats_orders_manager.modules.facturacion.util.ConversionMoneda
                        .montoEnCrcReporte(p.getMonto(), p.getMoneda(), p.getTipoCambio()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)).as("el detalle suma igual que el total de caja")
                .isEqualByComparingTo(reporte.getTotalIngresos());
        assertThat(reporte.getPagosDetalle().stream().filter(p -> p.esValido()).count())
                .isEqualTo(reporte.getCantidadPagos());

        List<Long> delDia = pagoService.findPagosDelDia().stream().map(p -> p.getIdPago()).toList();
        assertThat(delDia).contains(idEfectivo).doesNotContain(idNc);
        assertThat(pagoService.countPagosDelDia()).isEqualTo(delDia.size());
        assertThat(pagoService.findByFechaPagoBetween(hoy.atStartOfDay(),
                hoy.atTime(23, 59, 59)).stream().map(p -> p.getIdPago()).toList()).doesNotContain(idNc);
        assertThat(pagoService.countByMetodoPagoAndFecha(MetodoPago.OTROS, hoy.atStartOfDay(),
                hoy.atTime(23, 59, 59))).as("OTROS solo lo usa la aplicación del saldo").isZero();
    }

    private static final String MSG_SIN_ASIENTO = "La factura tiene pagos y no tiene asiento de venta vigente; "
            + "regularice su contabilidad antes de emitir la nota";

    @Test
    @DisplayName("criterio 24: factura con pagos y sin asiento de venta vigente: la nota (total o parcial) responde 409 con el mensaje fijo, sin nota ni comprobante")
    void notaSobreFacturaConPagosSinAsientoDeVenta() throws Exception {
        int f = facturaAceptada(null, MonedaFE.CRC, "1");
        pagar(f, "1000.00", MonedaFE.CRC, null);
        Long idVenta = jdbc.queryForObject("SELECT id FROM asientos_contables WHERE factura_id = ? "
                + "AND tipo = 'AUTOMATICO_VENTA' AND asiento_revertido_id IS NULL", Long.class, f);
        tx.executeWithoutResult(st -> asientoContableService.generarAsientoReversion(
                asientoRepository.findById(idVenta).orElseThrow(), "Histórico T-048"));
        int notasAntes = jdbc.queryForObject("SELECT COUNT(*) FROM nota_factura", Integer.class);
        int comprobantesAntes = jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico", Integer.class);

        for (Map<String, Object> cuerpo : List.of(
                Map.<String, Object>of("idFactura", f, "codigoReferencia", "01", "razon", "Anulación total"),
                Map.<String, Object>of("idFactura", f, "codigoReferencia", "02", "razon", "Parcial",
                        "lineas", List.of(acreditar(lineas(f).get(0), "100.00"))))) {
            MvcResult r = mockMvc.perform(post("/api/facturas/notas-credito")
                    .with(user(USUARIO).roles("ADMIN")).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cuerpo))).andReturn();
            assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(409);
            assertThat(mensaje(r)).isEqualTo(MSG_SIN_ASIENTO);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM nota_factura", Integer.class)).isEqualTo(notasAntes);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comprobante_electronico", Integer.class))
                .isEqualTo(comprobantesAntes);
    }

    @Test
    @DisplayName("criterio 25: el pago NOTA_CREDITO del detalle de la factura trae numeroNotaOrigen con el consecutivo de la nota, no la clave")
    void numeroNotaOrigenEnElDetalle() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        aplicar(s.id(), f2, "100.00");
        String consecutivo = jdbc.queryForObject("SELECT consecutivo FROM comprobante_electronico "
                + "WHERE nota_factura_id = ? AND vigente = b'1'", String.class, s.idNota());

        MvcResult det = mockMvc.perform(get("/facturas/detalle/" + f2).with(user(USUARIO).roles("ADMIN"))).andReturn();
        JsonNode pago = objectMapper.readTree(det.getResponse().getContentAsString()).get("pagos").get(0);

        assertThat(pago.get("numeroNotaOrigen").asText()).isEqualTo(consecutivo).hasSize(20);
        assertThat(pago.get("claveNotaOrigen").asText()).hasSize(50);
    }

    // ── Apoyo: escenarios ────────────────────────────────────────────────────

    record Saldo(long id, int idCliente, int idFacturaOrigen, long idNota) {
    }

    /** Saldo a favor CRC de 500: factura de 1630 con pago de 1000 y nota parcial de 1130 (CxC 630, excedente 500). */
    private Saldo saldoCrc(Integer idCliente) throws Exception {
        return crearSaldo(idCliente, MonedaFE.CRC, "1");
    }

    /** Saldo a favor USD de 500 a TC de origen {@code tc}. */
    private Saldo saldoUsd(String tc) throws Exception {
        return crearSaldo(null, MonedaFE.USD, tc);
    }

    private Saldo crearSaldo(Integer idCliente, MonedaFE moneda, String tc) throws Exception {
        int f1 = facturaAceptada(idCliente, moneda, tc);
        pagar(f1, "1000.00", moneda, moneda == MonedaFE.USD ? tc : null);
        Nota n = emitirParcialOk(f1, "02", "Genera saldo a favor", acreditar(lineas(f1).get(0), "1130.00"));
        aceptar(n);
        long id = jdbc.queryForObject("SELECT id FROM saldos_favor_cliente WHERE nota_factura_id = ?", Long.class,
                n.id());
        assertThat(saldoVivo(id)).isEqualByComparingTo("500.00");
        return new Saldo(id, clienteDe(f1), f1, n.id());
    }

    /** Estado observable que una aplicación rechazada no debe cambiar. */
    record Foto(int pagos, int aplicaciones, int asientos, String saldoVivo, String estadoSaldo) {
    }

    private Foto foto(long idSaldo) {
        return new Foto(
                jdbc.queryForObject("SELECT COUNT(*) FROM pagos", Integer.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM aplicaciones_saldo_favor", Integer.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables", Integer.class),
                saldoVivo(idSaldo).toPlainString(),
                jdbc.queryForObject("SELECT estado FROM saldos_favor_cliente WHERE id = ?", String.class, idSaldo));
    }

    private MvcResult aplicar(long idSaldo, int idFactura, String monto) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idFactura", idFactura);
        cuerpo.put("monto", new BigDecimal(monto));
        return mockMvc.perform(post("/api/facturas/saldos-favor/" + idSaldo + "/aplicaciones")
                .with(user(USUARIO).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cuerpo))).andReturn();
    }

    private MvcResult reversar(long idAplicacion, String razon) throws Exception {
        return mockMvc.perform(post("/api/facturas/saldos-favor/aplicaciones/" + idAplicacion + "/reversion")
                .with(user(USUARIO).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("razon", razon)))).andReturn();
    }

    private JsonNode getData(String url) throws Exception {
        MvcResult r = mockMvc.perform(get(url).with(user(USUARIO).roles("ADMIN"))).andReturn();
        assertThat(r.getResponse().getStatus()).as(url + " " + r.getResponse().getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data");
    }

    private String mensaje(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("message").asText();
    }

    private JsonNode detalleNota(Long idNota) throws Exception {
        return getData("/api/facturas/notas-credito/" + idNota + "/detalle");
    }

    /** Tasa de venta de USD de hoy (BCCR); desactiva la que hubiera. */
    private void tcDelDia(String venta) {
        tcDelDia(venta, FuenteTipoCambio.BCCR);
    }

    private void tcDelDia(String venta, FuenteTipoCambio fuente) {
        tx.executeWithoutResult(s -> tipoCambioService.registrar(TipoCambio.builder()
                .monedaOrigen(MonedaFE.CRC).monedaDestino(MonedaFE.USD).fecha(LocalDate.now())
                .tasaCompra(new BigDecimal(venta).subtract(BigDecimal.TEN)).tasaVenta(new BigDecimal(venta))
                .fuente(fuente).creadoPor("test").build()));
    }

    /** Pago CONFIRMADO en efectivo por el servicio (con su asiento de cobro). */
    private long pagar(int idFactura, String monto, MonedaFE moneda, String tipoCambio) {
        return tx.execute(s -> {
            Factura factura = facturaRepository.findById(idFactura).orElseThrow();
            Pago p = new Pago();
            p.setCliente(factura.getCliente());
            p.setFactura(factura);
            p.setTipoPago(TipoPago.PARCIAL);
            p.setMonto(new BigDecimal(monto));
            p.setMoneda(moneda);
            if (tipoCambio != null) {
                p.setTipoCambio(new BigDecimal(tipoCambio));
            }
            p.setMetodoPago(MetodoPago.EFECTIVO);
            p.setFechaPago(LocalDateTime.now(reloj).minusMinutes(1));
            p.setEstado(EstadoPago.CONFIRMADO);
            return pagoService.registrarPago(p).getIdPago();
        });
    }

    /** POST /pagos/registrar de 100 en efectivo (debe ser rechazado con una nota en trámite). */
    private MvcResult registrarPagoRest(int idFactura) throws Exception {
        Map<String, Object> pago = new LinkedHashMap<>();
        pago.put("idCliente", clienteDe(idFactura));
        pago.put("idFactura", idFactura);
        pago.put("tipoPago", "PARCIAL");
        pago.put("monto", 100);
        pago.put("metodoPago", "EFECTIVO");
        pago.put("fechaPago", LocalDateTime.now(reloj).withNano(0).minusMinutes(1).toString());
        pago.put("estado", "CONFIRMADO");
        return mockMvc.perform(post("/pagos/registrar").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pago))
                .with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn();
    }

    private BigDecimal saldoVivo(long idSaldo) {
        return jdbc.queryForObject("SELECT saldo_vivo FROM saldos_favor_cliente WHERE id = ?", BigDecimal.class,
                idSaldo);
    }

    private int saldosDeNota(Long idNota) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM saldos_favor_cliente WHERE nota_factura_id = ?",
                Integer.class, idNota);
    }

    private BigDecimal saldoPendiente(int idFactura) {
        return tx.execute(s -> facturaRepository.findById(idFactura).orElseThrow().calcularSaldoPendiente());
    }

    private Integer clienteDe(int idFactura) {
        return jdbc.queryForObject("SELECT id_cliente FROM factura WHERE id_factura = ?", Integer.class, idFactura);
    }

    private long asientoDeNota(Long idNota) {
        return jdbc.queryForObject("SELECT id FROM asientos_contables WHERE nota_factura_id = ? "
                + "AND asiento_revertido_id IS NULL", Long.class, idNota);
    }

    private String claveDeLaNota(Long idNota) {
        return jdbc.queryForObject("SELECT clave_numerica FROM comprobante_electronico WHERE nota_factura_id = ? "
                + "AND vigente = b'1'", String.class, idNota);
    }

    private String estadoNota(Long id) {
        return jdbc.queryForObject("SELECT estado FROM nota_factura WHERE id = ?", String.class, id);
    }

    private List<Integer> lineas(int idFactura) {
        return jdbc.queryForList("SELECT id_linea_factura FROM linea_factura WHERE id_factura = ? ORDER BY numero_linea",
                Integer.class, idFactura);
    }

    // ── Apoyo: facturas y notas (como NotaCreditoIT) ─────────────────────────

    record Nota(Long id, Long idComprobante, String clave) {
    }

    private static Map<String, Object> acreditar(int idLineaFactura, String monto) {
        Map<String, Object> l = new LinkedHashMap<>();
        l.put("idLineaFactura", idLineaFactura);
        l.put("montoAcreditar", new BigDecimal(monto));
        return l;
    }

    @SafeVarargs
    private Nota emitirParcialOk(int idFactura, String codigo, String razon, Map<String, Object>... lineas)
            throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idFactura", idFactura);
        cuerpo.put("codigoReferencia", codigo);
        cuerpo.put("razon", razon);
        cuerpo.put("lineas", List.of(lineas));
        return emitirCuerpo(cuerpo);
    }

    private Nota emitirOk(int idFactura, String codigo, String razon) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idFactura", idFactura);
        cuerpo.put("codigoReferencia", codigo);
        cuerpo.put("razon", razon);
        return emitirCuerpo(cuerpo);
    }

    private Nota emitirCuerpo(Map<String, Object> cuerpo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/facturas/notas-credito")
                .with(user(USUARIO).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cuerpo))).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return new Nota(data.get("id").asLong(), data.get("idComprobante").asLong(),
                data.get("claveComprobante").asText());
    }

    private void aceptar(Nota nota) {
        when(haciendaApi.consultarComprobante(eq(nota.clave()), anyLong())).thenReturn(consulta("aceptado"));
        comprobanteService.consultarYActualizarEstado(nota.idComprobante());
    }

    /** Factura A (gravado 13 %, 1130) + B (exento, 500) = 1630 en la moneda y TC indicados, con comprobante ACEPTADO. */
    private int facturaAceptada(Integer idCliente, MonedaFE moneda, String tc) throws Exception {
        int idFactura = tx.execute(s -> {
            Cliente cliente;
            if (idCliente != null) {
                cliente = clienteRepository.findById(idCliente).orElseThrow();
            } else {
                cliente = new Cliente();
                cliente.setNombre("Cliente T048 " + UUID.randomUUID());
                cliente.setTipoIdentificacion(api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion.CEDULA_FISICA);
                cliente.setNumeroIdentificacion("109990999");
                cliente.setTelefono("88887777");
                cliente.setEmail("cliente@t048.example");
                cliente = clienteRepository.save(cliente);
            }
            Factura factura = new Factura();
            factura.setCliente(cliente);
            factura.setNumeroFactura("T048-" + UUID.randomUUID().toString().substring(0, 8));
            factura.setSerie("T048");
            factura.setMonedaFE(moneda);
            factura.setTipoCambio(new BigDecimal(tc));
            factura.setSubtotal(BigDecimal.ZERO);
            factura.setIgv(BigDecimal.ZERO);
            factura.setTotal(BigDecimal.ZERO);
            factura.setAnulada(false);
            return facturaRepository.save(factura).getIdFactura();
        });
        List<Map<String, Object>> lineas = new ArrayList<>();
        lineas.add(linea(-1, idFactura, gravado13, 1, "1130.00", 1));
        lineas.add(linea(-2, idFactura, exento, 1, "500.00", 2));
        MvcResult result = mockMvc.perform(put("/lineas-factura/actualizar")
                .with(user(USUARIO).roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lineas))).andReturn();
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT total FROM factura WHERE id_factura = ?", BigDecimal.class, idFactura))
                .isEqualByComparingTo("1630.00");
        comprobanteDeFactura(idFactura);
        return idFactura;
    }

    private static Map<String, Object> linea(int idLinea, int idFactura, Producto producto, int cantidad, String precio,
                                             int numero) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("id_factura", idFactura);
        linea.put("id_linea_factura", idLinea);
        linea.put("numero_linea", numero);
        linea.put("id_producto", producto.getIdProducto());
        linea.put("cantidad", cantidad);
        linea.put("precioUnitario", new BigDecimal(precio));
        linea.put("subtotal", new BigDecimal(precio));
        return linea;
    }

    private void comprobanteDeFactura(int idFactura) {
        tx.executeWithoutResult(s -> {
            Factura factura = facturaRepository.findById(idFactura).orElseThrow();
            ComprobanteElectronico c = ComprobanteElectronico.builder()
                    .factura(factura)
                    .empresa(configHacienda.getEmpresa())
                    .tipoComprobante(TipoComprobanteElectronico.FACTURA_ELECTRONICA)
                    .ambiente(api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda.SANDBOX)
                    .claveNumerica(clave())
                    .consecutivo(String.format("%020d", ThreadLocalRandom.current().nextLong(1, 1_000_000_000L)))
                    .fechaEmision(LocalDateTime.now().minusDays(1).withNano(0))
                    .fechaEnvio(LocalDateTime.now().minusDays(1))
                    .estado(EstadoComprobante.ACEPTADO)
                    .vigente(true)
                    .build();
            c.setXmlComprobante(firmaDigital.firmarXml(xmlGenerator.generarXmlFactura(c),
                    configHacienda.getRutaCertificado(), configHacienda.getPinCertificado()));
            comprobanteRepository.save(c);
        });
    }

    private BigDecimal debeEn(long idAsiento, String codigoCuenta) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(d.debe), 0) FROM detalles_asiento d JOIN cuentas_contables c "
                + "ON c.id = d.cuenta_id WHERE d.asiento_id = ? AND c.codigo = ?", BigDecimal.class, idAsiento,
                codigoCuenta);
    }

    private BigDecimal haberEn(long idAsiento, String codigoCuenta) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(d.haber), 0) FROM detalles_asiento d JOIN cuentas_contables c "
                + "ON c.id = d.cuenta_id WHERE d.asiento_id = ? AND c.codigo = ?", BigDecimal.class, idAsiento,
                codigoCuenta);
    }

    private void assertCuadra(long idAsiento) {
        Map<String, Object> t = jdbc.queryForMap("SELECT COALESCE(SUM(debe), 0) AS debe, COALESCE(SUM(haber), 0) AS haber "
                + "FROM detalles_asiento WHERE asiento_id = ?", idAsiento);
        assertThat((BigDecimal) t.get("debe")).isEqualByComparingTo((BigDecimal) t.get("haber"));
        assertThat((BigDecimal) t.get("debe")).isPositive();
    }

    private static RespuestaHaciendaDTO timeoutDeLectura() {
        return RespuestaHaciendaDTO.builder().resultadoEnvio(ResultadoEnvioHacienda.ENVIO_INCIERTO).exitoso(false)
                .mensaje("Read timed out").build();
    }

    private static RespuestaHaciendaDTO consulta(String indEstado) {
        return RespuestaHaciendaDTO.builder().indicadorEstado(indEstado).codigoHttp(200).exitoso(false)
                .codigoMensaje(MensajeHacienda.PROCESANDO).mensaje("ind-estado " + indEstado).build();
    }

    private static String clave() {
        StringBuilder clave = new StringBuilder("506");
        while (clave.length() < 50) {
            clave.append(ThreadLocalRandom.current().nextInt(10));
        }
        return clave.toString();
    }

    @SuppressWarnings("deprecation")
    private Producto producto(String descripcion, String precio, boolean gravado) {
        Producto p = new Producto();
        p.setCodigo("T48-" + UUID.randomUUID().toString().substring(0, 8));
        p.setDescripcion(descripcion);
        ArticuloMaestro maestro = articuloMaestroRepository.findByDescripcionContainingIgnoreCase(descripcion).stream()
                .filter(m -> descripcion.equals(m.getDescripcion())).findFirst().orElseGet(ArticuloMaestro::new);
        maestro.setDescripcion(descripcion);
        maestro.setCodigoCabys(gravado ? "1234567890123" : "9876543210987");
        p.setArticuloMaestro(articuloMaestroRepository.save(maestro));
        p.setPrecioInstitucional(new BigDecimal(precio));
        p.setPrecioMayorista(new BigDecimal(precio));
        p.setActive(true);
        p.setGravado(gravado);
        p.setPorcentajeImpuesto(gravado ? new BigDecimal("13") : BigDecimal.ZERO);
        p.setPrecioIncluyeIva(true);
        return productoRepository.save(p);
    }

    private CuentaContable cuenta(String codigo, String nombre, TipoCuenta tipo) {
        CuentaContable c = new CuentaContable();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setNaturaleza(NaturalezaCuenta.fromTipoCuenta(tipo));
        c.setNivel(codigo.split("\\.").length);
        c.setActiva(true);
        c.setAceptaMovimientos(true);
        return cuentaRepository.save(c);
    }

    private void param(String clave, CuentaContable cuenta) {
        parametroRepository.save(ParametroContable.builder().clave(clave).cuentaContable(cuenta).build());
    }
}
