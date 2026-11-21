package api.astro.whats_orders_manager.modules.facturacion;

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
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
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
 * T-049 (ADR-058, fase B2 de ADR-053) — devolución en dinero del saldo a favor de punta a punta sobre MySQL.
 * cuentas por cobrar y saldo a favor, aplicación manual a otra factura (CRC y USD con diferencial por tramos), todas las
 * validaciones, tipo de cambio faltante, parámetro ausente, concurrencia y reversión.
 *
 * Base: MySQL 8 en Testcontainers (t049-it-mysql, tmpfs, sin volúmenes); esquema de Hibernate. El generador de XML, la
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
@DisplayName("T-049 — devolución en dinero del saldo a favor (MySQL)")
class DevolucionSaldoFavorIT {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("t049")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("t049-it-mysql"))
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("database/routines/stored_procedures.sql")),
                    "/docker-entrypoint-initdb.d/01-routines.sql");

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

    // ═════════════════════════════════════════════════════════════════════════
    // T-049: devolución en dinero de un saldo a favor (B2)
    // ═════════════════════════════════════════════════════════════════════════

    private static final String SOL = "sol.t049@example.invalid";
    private static final String APR = "apr.t049@example.invalid";
    private static final String PAG = "pag.t049@example.invalid";
    private static final String SIN_PERMISO = "ven.t049@example.invalid";
    private static final String NOMBRE_APR = "Aprobador T049";

    private static final String MSG_EXCEDE_DISPONIBLE = "El monto excede el saldo disponible del saldo a favor "
            + "(saldo vivo menos lo reservado por devoluciones en trámite)";
    private static final String MSG_IBAN = "El IBAN no es válido: debe ser una cuenta costarricense (CR y 20 dígitos) "
            + "con dígito de control correcto";
    private static final String MSG_IDENTIFICACION = "La identificación del titular debe ser la del cliente del saldo a favor";
    private static final String MSG_TITULAR = "El nombre del titular debe tener entre 3 y 100 caracteres";
    private static final String MSG_SOLICITUD = "El medio y la fecha de la solicitud del cliente son obligatorios y la "
            + "fecha no puede ser futura";
    private static final String MSG_SEG_APROBAR = "La devolución debe aprobarla un usuario distinto del que la solicitó "
            + "y del que emitió la nota";
    private static final String MSG_SEG_PAGAR = "El pago de la devolución debe hacerlo un usuario distinto del que la solicitó";
    private static final String MSG_TRANSICION = "La devolución no está en un estado que permita esta operación";
    private static final String MSG_DECLARACION = "Debe declarar que verificó que la cuenta pertenece al cliente";
    private static final String MSG_RAZON_DEV = "La razón debe tener entre 3 y 180 caracteres";
    private static final String MSG_COMPROBANTE = "El número y la fecha del comprobante bancario son obligatorios y la "
            + "fecha no puede ser futura";
    private static final String MSG_CUENTA = "La cuenta bancaria de la empresa es obligatoria, debe estar activa y en la "
            + "misma moneda del saldo a favor";

    @Autowired private api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository usuarioRepository;
    @Autowired private api.astro.whats_orders_manager.modules.seguridad.repository.RolRepository rolRepository;
    @Autowired private api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository cuentaBancariaRepository;
    @Autowired private api.astro.whats_orders_manager.modules.configuracion.repository.EmpresaRepository empresaDeCuentasRepository;
    @Autowired private api.astro.whats_orders_manager.modules.contabilidad.service.CalculoContableService calculoContableService;
    @Autowired @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
    private org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping handlerMapping;

    static String ibanValido(String bban18) {
        java.math.BigInteger n = new java.math.BigInteger(bban18 + "122700");
        int control = 98 - n.mod(java.math.BigInteger.valueOf(97)).intValue();
        return "CR" + String.format("%02d", control) + bban18;
    }

    private static final String IBAN = ibanValido("015202001026284066");

    /** Crea (una vez) los usuarios de la prueba: tres con rol ADMIN (todos los permisos) y uno VENDEDOR (sin pagos). */
    private void usuarios() {
        TransactionTemplate t = new TransactionTemplate(transactionManager);
        t.executeWithoutResult(s -> {
            usuario(SOL, "Solicitante T049", "50611110001", "ADMIN");
            usuario(APR, NOMBRE_APR, "50611110002", "ADMIN");
            usuario(PAG, "Pagador T049", "50611110003", "ADMIN");
            usuario(SIN_PERMISO, "Vendedor T049", "50611110004", "VENDEDOR");
        });
    }

    private void usuario(String email, String nombre, String telefono, String rol) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            return;
        }
        api.astro.whats_orders_manager.modules.seguridad.model.Usuario u =
                new api.astro.whats_orders_manager.modules.seguridad.model.Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setTelefono(telefono);
        u.setPassword("x");
        u.setRol(rol);
        u.setRolEntity(rolRepository.findByCodigo(rol).orElseThrow());
        u.setActivo(true);
        u.setBloqueado(false);
        u.setIntentosFallidos(0);
        usuarioRepository.save(u);
    }

    private int idUsuarioDe(String email) {
        return jdbc.queryForObject("SELECT id_usuario FROM usuario WHERE email = ?", Integer.class, email);
    }

    /** Cuenta bancaria de la empresa con su hoja contable (ADR-018); idempotente por entidad. */
    private int cuentaBancaria(String entidad, String moneda, String codigoHoja, boolean activa) {
        TransactionTemplate t = new TransactionTemplate(transactionManager);
        return t.execute(s -> {
            java.util.List<Integer> ids = jdbc.queryForList("SELECT id_cuenta_bancaria FROM cuenta_bancaria "
                    + "WHERE entidad = ?", Integer.class, entidad);
            if (!ids.isEmpty()) {
                return ids.get(0);
            }
            api.astro.whats_orders_manager.modules.configuracion.model.Empresa empresa = empresaDeCuentasRepository
                    .findAll().stream().findFirst().orElseGet(() -> {
                        api.astro.whats_orders_manager.modules.configuracion.model.Empresa e =
                                new api.astro.whats_orders_manager.modules.configuracion.model.Empresa();
                        e.setNombreEmpresa("Empresa T049");
                        return empresaDeCuentasRepository.save(e);
                    });
            api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria c =
                    new api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria();
            c.setEmpresa(empresa);
            c.setEntidad(entidad);
            c.setMoneda(moneda);
            c.setActiva(activa);
            c.setCuentaContable(cuenta(codigoHoja, entidad, TipoCuenta.ACTIVO));
            return cuentaBancariaRepository.save(c).getIdCuentaBancaria();
        });
    }

    private int bancoCrc() {
        return cuentaBancaria("Banco T049 CRC", "CRC", "9.1.01.002.901", true);
    }

    private int bancoUsd() {
        return cuentaBancaria("Banco T049 USD", "USD", "9.1.01.002.902", true);
    }

    private long hojaDe(int idCuentaBancaria) {
        return jdbc.queryForObject("SELECT cuenta_contable_id FROM cuenta_bancaria WHERE id_cuenta_bancaria = ?",
                Long.class, idCuentaBancaria);
    }

    private static Map<String, Object> cuerpoSolicitud(String monto, String iban, String titular, String ident,
                                                       String medio, String fecha) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("monto", monto == null ? null : new BigDecimal(monto));
        m.put("iban", iban);
        m.put("nombreTitular", titular);
        m.put("identificacionTitular", ident);
        m.put("medioSolicitud", medio);
        m.put("fechaSolicitud", fecha);
        return m;
    }

    private static Map<String, Object> cuerpoOk(String monto) {
        return cuerpoSolicitud(monto, IBAN, "Cliente T048 Titular", "1-0999-0999", "CORREO", LocalDate.now().toString());
    }

    private MvcResult enviar(String usuario, String url, Object cuerpo) throws Exception {
        return mockMvc.perform(post(url).with(user(usuario).roles(SIN_PERMISO.equals(usuario) ? "VENDEDOR" : "ADMIN"))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cuerpo))).andReturn();
    }

    private MvcResult solicitar(String usuario, long idSaldo, Map<String, Object> cuerpo) throws Exception {
        usuarios();
        return enviar(usuario, "/api/facturas/saldos-favor/" + idSaldo + "/devoluciones", cuerpo);
    }

    private long solicitarOk(long idSaldo, String monto) throws Exception {
        MvcResult r = solicitar(SOL, idSaldo, cuerpoOk(monto));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    private MvcResult aprobar(String usuario, long id, boolean verifico) throws Exception {
        return enviar(usuario, "/api/facturas/saldos-favor/devoluciones/" + id + "/aprobar",
                Map.of("verificoCuenta", verifico));
    }

    private void aprobarOk(long id) throws Exception {
        MvcResult r = aprobar(APR, id, true);
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
    }

    private MvcResult rechazar(String usuario, long id, String razon) throws Exception {
        return enviar(usuario, "/api/facturas/saldos-favor/devoluciones/" + id + "/rechazar", Map.of("razon", razon));
    }

    private MvcResult anular(String usuario, long id, String razon) throws Exception {
        return enviar(usuario, "/api/facturas/saldos-favor/devoluciones/" + id + "/anular", Map.of("razon", razon));
    }

    private MvcResult pagar(String usuario, long id, Integer idCuenta, String comprobante, String fecha) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idCuentaBancaria", idCuenta);
        m.put("numeroComprobante", comprobante);
        m.put("fechaComprobante", fecha);
        return enviar(usuario, "/api/facturas/saldos-favor/devoluciones/" + id + "/pagar", m);
    }

    private MvcResult pagarOk(long id, int idCuenta) throws Exception {
        MvcResult r = pagar(PAG, id, idCuenta, "SINPE-998877", LocalDate.now().toString());
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return r;
    }

    private BigDecimal reservado(long idSaldo) {
        return jdbc.queryForObject("SELECT monto_reservado FROM saldos_favor_cliente WHERE id = ?", BigDecimal.class,
                idSaldo);
    }

    private String estadoDev(long id) {
        return jdbc.queryForObject("SELECT estado FROM devoluciones_saldo_favor WHERE id = ?", String.class, id);
    }

    private int asientos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables", Integer.class);
    }

    private int devolucionesDe(long idSaldo) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM devoluciones_saldo_favor WHERE saldo_favor_id = ?",
                Integer.class, idSaldo);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void assertConflicto(MvcResult r, String mensaje) throws Exception {
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(409);
        assertThat(mensaje(r)).isEqualTo(mensaje);
    }

    // ── Criterio 1 y 2: modelo y permiso ─────────────────────────────────────

    @Test
    @DisplayName("T-049 c1/c2: existen la tabla y el permiso SALDO_FAVOR_DEVOLUCION_APROBAR, asignado al rol ADMIN")
    void modeloYPermiso() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'devoluciones_saldo_favor'", Integer.class)).isGreaterThan(20);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'saldos_favor_cliente' AND column_name = 'monto_reservado'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permiso WHERE codigo = 'SALDO_FAVOR_DEVOLUCION_APROBAR'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rol_permiso rp JOIN rol r ON r.id_rol = rp.id_rol "
                + "JOIN permiso p ON p.id_permiso = rp.id_permiso WHERE r.codigo = 'ADMIN' "
                + "AND p.codigo = 'SALDO_FAVOR_DEVOLUCION_APROBAR'", Integer.class)).isEqualTo(1);
    }

    // ── Criterio 3: solicitud ────────────────────────────────────────────────

    @Test
    @DisplayName("T-049 c3: la solicitud válida reserva el monto, normaliza IBAN e identificación y no genera asiento")
    void solicitudValida() throws Exception {
        Saldo s = saldoCrc(null);
        int asientosAntes = asientos();
        Map<String, Object> cuerpo = cuerpoSolicitud("200.00", IBAN.substring(0, 4) + " " + IBAN.substring(4, 8) + " "
                + IBAN.substring(8).toLowerCase(), "  Cliente T048 Titular ", "1 0999 0999", "WHATSAPP",
                LocalDate.now().minusDays(2).toString());

        MvcResult r = solicitar(SOL, s.id(), cuerpo);

        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode d = objectMapper.readTree(r.getResponse().getContentAsString()).get("data");
        assertThat(d.get("estado").asText()).isEqualTo("SOLICITADA");
        assertThat(d.get("monto").decimalValue()).isEqualByComparingTo("200.00");
        Map<String, Object> fila = jdbc.queryForMap("SELECT * FROM devoluciones_saldo_favor WHERE id = ?",
                d.get("id").asLong());
        assertThat(fila.get("iban")).isEqualTo(IBAN);
        assertThat(fila.get("identificacion_titular")).isEqualTo("109990999");
        assertThat(fila.get("nombre_titular")).isEqualTo("Cliente T048 Titular");
        assertThat(fila.get("medio_solicitud")).isEqualTo("WHATSAPP");
        assertThat(fila.get("fecha_solicitud").toString()).isEqualTo(LocalDate.now().minusDays(2).toString());
        assertThat(((Number) fila.get("solicitado_por")).intValue()).isEqualTo(idUsuarioDe(SOL));
        assertThat(fila.get("fecha_creacion")).isNotNull();
        assertThat(fila.get("asiento_id")).isNull();
        assertThat(reservado(s.id())).isEqualByComparingTo("200.00");
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("500.00");
        assertThat(asientos()).as("ningún asiento antes de pagar").isEqualTo(asientosAntes);
    }

    @Test
    @DisplayName("T-049 c3: cada validación de la solicitud responde 409 con su mensaje fijo y no crea ni reserva nada")
    void solicitudInvalida() throws Exception {
        Saldo s = saldoCrc(null);
        String hoy = LocalDate.now().toString();
        String ibanMalo = "CR" + String.format("%02d", (Integer.parseInt(IBAN.substring(2, 4)) + 1) % 100)
                + IBAN.substring(4);
        record Caso(String nombre, Map<String, Object> cuerpo, String mensaje) {
        }
        List<Caso> casos = List.of(
                new Caso("monto 0", cuerpoSolicitud("0", IBAN, "Titular Ok", "109990999", "CORREO", hoy), MSG_MONTO),
                new Caso("monto negativo", cuerpoSolicitud("-5.00", IBAN, "Titular Ok", "109990999", "CORREO", hoy), MSG_MONTO),
                new Caso("tres decimales", cuerpoSolicitud("10.123", IBAN, "Titular Ok", "109990999", "CORREO", hoy), MSG_MONTO),
                new Caso("monto null", cuerpoSolicitud(null, IBAN, "Titular Ok", "109990999", "CORREO", hoy), MSG_MONTO),
                new Caso("excede", cuerpoSolicitud("500.01", IBAN, "Titular Ok", "109990999", "CORREO", hoy), MSG_EXCEDE_DISPONIBLE),
                new Caso("iban formato", cuerpoSolicitud("10", "CR123", "Titular Ok", "109990999", "CORREO", hoy), MSG_IBAN),
                new Caso("iban otro país", cuerpoSolicitud("10", "DE" + IBAN.substring(2), "Titular Ok", "109990999", "CORREO", hoy), MSG_IBAN),
                new Caso("iban control", cuerpoSolicitud("10", ibanMalo, "Titular Ok", "109990999", "CORREO", hoy), MSG_IBAN),
                new Caso("iban null", cuerpoSolicitud("10", null, "Titular Ok", "109990999", "CORREO", hoy), MSG_IBAN),
                new Caso("titular distinto", cuerpoSolicitud("10", IBAN, "Titular Ok", "1-1111-1111", "CORREO", hoy), MSG_IDENTIFICACION),
                new Caso("identificación null", cuerpoSolicitud("10", IBAN, "Titular Ok", null, "CORREO", hoy), MSG_IDENTIFICACION),
                new Caso("nombre corto", cuerpoSolicitud("10", IBAN, "Ab", "109990999", "CORREO", hoy), MSG_TITULAR),
                new Caso("nombre largo", cuerpoSolicitud("10", IBAN, "x".repeat(101), "109990999", "CORREO", hoy), MSG_TITULAR),
                new Caso("sin medio", cuerpoSolicitud("10", IBAN, "Titular Ok", "109990999", null, hoy), MSG_SOLICITUD),
                new Caso("sin fecha", cuerpoSolicitud("10", IBAN, "Titular Ok", "109990999", "CARTA", null), MSG_SOLICITUD),
                new Caso("fecha futura", cuerpoSolicitud("10", IBAN, "Titular Ok", "109990999", "CARTA",
                        LocalDate.now().plusDays(1).toString()), MSG_SOLICITUD));
        Foto antes = foto(s.id());
        int devAntes = devolucionesDe(s.id());

        for (Caso c : casos) {
            MvcResult r = solicitar(SOL, s.id(), c.cuerpo());
            assertThat(r.getResponse().getStatus()).as(c.nombre() + " " + r.getResponse().getContentAsString())
                    .isEqualTo(409);
            assertThat(mensaje(r)).as(c.nombre()).isEqualTo(c.mensaje());
        }
        assertThat(foto(s.id())).isEqualTo(antes);
        assertThat(devolucionesDe(s.id())).isEqualTo(devAntes);
        assertThat(reservado(s.id())).isEqualByComparingTo("0");

        // un saldo agotado tampoco admite solicitudes
        jdbc.update("UPDATE saldos_favor_cliente SET saldo_vivo = 0, estado = 'AGOTADO' WHERE id = ?", s.id());
        assertConflicto(solicitar(SOL, s.id(), cuerpoOk("10")), MSG_NO_VIGENTE);
        assertThat(devolucionesDe(s.id())).isEqualTo(devAntes);
    }

    @Test
    @DisplayName("T-049 c3: un medio fuera del catálogo o un JSON mal formado responde 400; saldo inexistente 404; sin permiso 403")
    void solicitudFormatoYPermiso() throws Exception {
        Saldo s = saldoCrc(null);
        Map<String, Object> raro = cuerpoOk("10");
        raro.put("medioSolicitud", "PALOMA");
        assertThat(solicitar(SOL, s.id(), raro).getResponse().getStatus()).isEqualTo(400);
        assertThat(solicitar(SOL, 9_999_999L, cuerpoOk("10")).getResponse().getStatus()).isEqualTo(404);
        assertThat(solicitar(SIN_PERMISO, s.id(), cuerpoOk("10")).getResponse().getStatus()).isEqualTo(403);
        assertThat(devolucionesDe(s.id())).isZero();
    }

    // ── Criterio 4: reserva y concurrencia ───────────────────────────────────

    @Test
    @DisplayName("T-049 c4: con una reserva, ni otra solicitud ni la aplicación de B1 pueden usar más que vivo − reservado")
    void reservaLimitaElDisponible() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        solicitarOk(s.id(), "300.00");

        assertConflicto(solicitar(SOL, s.id(), cuerpoOk("300.00")), MSG_EXCEDE_DISPONIBLE);
        assertConflicto(aplicar(s.id(), f2, "300.00"), MSG_EXCEDE_DISPONIBLE);
        assertThat(aplicar(s.id(), f2, "200.00").getResponse().getStatus()).isEqualTo(201);
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("300.00");
        assertThat(reservado(s.id())).isEqualByComparingTo("300.00");
        // lo que quedó reservado se puede devolver por completo
        JsonNode datos = getData("/api/facturas/saldos-favor/cliente/" + s.idCliente());
        JsonNode saldo = datos.get(0).get("saldo");
        assertThat(saldo.get("reservado").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(saldo.get("disponible").decimalValue()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("T-049 c4: solicitud y aplicación simultáneas que juntas exceden el disponible: una pasa y la otra recibe 409")
    void solicitudYAplicacionConcurrentes() throws Exception {
        Saldo s = saldoCrc(null);
        int f2 = facturaAceptada(s.idCliente(), MonedaFE.CRC, "1");
        usuarios();
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> a = pool.submit(() -> {
                salida.await();
                return solicitar(SOL, s.id(), cuerpoOk("300.00")).getResponse().getStatus();
            });
            Future<Integer> b = pool.submit(() -> {
                salida.await();
                return aplicar(s.id(), f2, "300.00").getResponse().getStatus();
            });
            salida.countDown();
            List<Integer> estados = List.of(a.get(60, TimeUnit.SECONDS), b.get(60, TimeUnit.SECONDS));
            assertThat(estados).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
        BigDecimal vivo = saldoVivo(s.id());
        BigDecimal res = reservado(s.id());
        assertThat(vivo.subtract(res)).as("vivo − reservado nunca negativo").isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(vivo.compareTo(new BigDecimal("200")) == 0 && res.signum() == 0
                || vivo.compareTo(new BigDecimal("500")) == 0 && res.compareTo(new BigDecimal("300")) == 0).isTrue();
    }

    @Test
    @DisplayName("T-049 c4: dos solicitudes concurrentes que juntas exceden el disponible: una pasa y la otra recibe 409")
    void dosSolicitudesConcurrentes() throws Exception {
        Saldo s = saldoCrc(null);
        usuarios();
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> fs = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                fs.add(pool.submit(() -> {
                    salida.await();
                    return solicitar(SOL, s.id(), cuerpoOk("300.00")).getResponse().getStatus();
                }));
            }
            salida.countDown();
            assertThat(List.of(fs.get(0).get(60, TimeUnit.SECONDS), fs.get(1).get(60, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(reservado(s.id())).isEqualByComparingTo("300.00");
        assertThat(devolucionesDe(s.id())).isEqualTo(1);
    }

    /** Lanza dos tareas a la vez y devuelve sus estados HTTP. */
    private List<Integer> enParalelo(Callable<Integer> a, Callable<Integer> b) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> fa = pool.submit(() -> {
                salida.await();
                return a.call();
            });
            Future<Integer> fb = pool.submit(() -> {
                salida.await();
                return b.call();
            });
            salida.countDown();
            return List.of(fa.get(60, TimeUnit.SECONDS), fb.get(60, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("T-049 rework: dos pagos simultáneos de la misma devolución: uno 200 y otro 409 (transición), un solo asiento, saldo y reserva correctos")
    void pagoContraPagoConcurrente() throws Exception {
        Saldo s = saldoCrc(null);
        long id = solicitarOk(s.id(), "200.00");
        aprobarOk(id);
        int banco = bancoCrc();
        int asientosAntes = asientos();
        String hoy = LocalDate.now().toString();
        Callable<Integer> pagar = () -> pagar(PAG, id, banco, "C-77", hoy).getResponse().getStatus();
        List<Integer> estados = enParalelo(pagar, pagar);
        assertThat(estados).containsExactlyInAnyOrder(200, 409);
        assertThat(asientos()).isEqualTo(asientosAntes + 1);
        assertThat(estadoDev(id)).isEqualTo("PAGADA");
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("300.00");
        assertThat(reservado(s.id())).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("T-049 rework: anular y pagar simultáneos sobre una APROBADA: gana uno, el otro 409; saldo, reserva y asiento coherentes")
    void anularContraPagoConcurrente() throws Exception {
        Saldo s = saldoCrc(null);
        long id = solicitarOk(s.id(), "200.00");
        aprobarOk(id);
        int banco = bancoCrc();
        int asientosAntes = asientos();
        String hoy = LocalDate.now().toString();
        List<Integer> estados = enParalelo(
                () -> pagar(PAG, id, banco, "C-78", hoy).getResponse().getStatus(),
                () -> anular(APR, id, "Se anula en paralelo").getResponse().getStatus());
        assertThat(estados).containsExactlyInAnyOrder(200, 409);
        assertThat(reservado(s.id())).isEqualByComparingTo("0");
        if ("PAGADA".equals(estadoDev(id))) {
            assertThat(saldoVivo(s.id())).isEqualByComparingTo("300.00");
            assertThat(asientos()).isEqualTo(asientosAntes + 1);
        } else {
            assertThat(estadoDev(id)).isEqualTo("ANULADA");
            assertThat(saldoVivo(s.id())).isEqualByComparingTo("500.00");
            assertThat(asientos()).isEqualTo(asientosAntes);
        }
    }

    @Test
    @DisplayName("T-049 rework: rechazar y aprobar simultáneos sobre una SOLICITADA: gana uno, el otro 409; la reserva queda coherente")
    void rechazarContraAprobarConcurrente() throws Exception {
        Saldo s = saldoCrc(null);
        long id = solicitarOk(s.id(), "200.00");
        List<Integer> estados = enParalelo(
                () -> aprobar(APR, id, true).getResponse().getStatus(),
                () -> rechazar(PAG, id, "Rechazo en paralelo").getResponse().getStatus());
        assertThat(estados).containsExactlyInAnyOrder(200, 409);
        if ("RECHAZADA".equals(estadoDev(id))) {
            assertThat(reservado(s.id())).isEqualByComparingTo("0");
        } else {
            assertThat(estadoDev(id)).isEqualTo("APROBADA");
            assertThat(reservado(s.id())).isEqualByComparingTo("200.00");
        }
    }

    // ── Criterio 5: aprobación ───────────────────────────────────────────────

    @Test
    @DisplayName("T-049 c5: aprobar exige el permiso (403), la declaración (400) y un usuario distinto del solicitante y del emisor de la nota (409)")
    void aprobacion() throws Exception {
        Saldo s = saldoCrc(null);
        long id = solicitarOk(s.id(), "200.00");

        assertThat(aprobar(SIN_PERMISO, id, true).getResponse().getStatus()).isEqualTo(403);
        MvcResult sinDeclaracion = aprobar(APR, id, false);
        assertThat(sinDeclaracion.getResponse().getStatus()).isEqualTo(400);
        assertThat(mensaje(sinDeclaracion)).isEqualTo(MSG_DECLARACION);
        assertThat(enviar(APR, "/api/facturas/saldos-favor/devoluciones/" + id + "/aprobar", Map.of())
                .getResponse().getStatus()).isEqualTo(400);
        assertConflicto(aprobar(SOL, id, true), MSG_SEG_APROBAR);          // mismo solicitante
        assertConflicto(aprobar(USUARIO, id, true), MSG_SEG_APROBAR);      // emitió la nota (por email)
        assertThat(estadoDev(id)).isEqualTo("SOLICITADA");
        assertThat(aprobar(APR, 9_999_999L, true).getResponse().getStatus()).isEqualTo(404);

        // si la nota quedó registrada con el nombre del aprobador (en otra mayúscula), tampoco puede aprobar
        jdbc.update("UPDATE nota_factura SET usuario_creador = ? WHERE id = ?", NOMBRE_APR.toUpperCase(), s.idNota());
        assertConflicto(aprobar(APR, id, true), MSG_SEG_APROBAR);
        jdbc.update("UPDATE nota_factura SET usuario_creador = ? WHERE id = ?", USUARIO, s.idNota());

        MvcResult ok = aprobar(APR, id, true);
        assertThat(ok.getResponse().getStatus()).as(ok.getResponse().getContentAsString()).isEqualTo(200);
        Map<String, Object> fila = jdbc.queryForMap("SELECT * FROM devoluciones_saldo_favor WHERE id = ?", id);
        assertThat(fila.get("estado")).isEqualTo("APROBADA");
        assertThat(((Number) fila.get("aprobado_por")).intValue()).isEqualTo(idUsuarioDe(APR));
        assertThat(fila.get("fecha_aprobacion")).isNotNull();
        assertThat(fila.get("verifico_cuenta")).isIn(true, 1, (Object) Boolean.TRUE);
        assertThat(fila.get("fecha_verificacion")).isNotNull();
        assertThat(reservado(s.id())).as("la reserva se mantiene").isEqualByComparingTo("200.00");
        assertConflicto(aprobar(APR, id, true), MSG_TRANSICION);
    }

    // ── Criterio 6: rechazar y anular ────────────────────────────────────────

    @Test
    @DisplayName("T-049 c6: rechazar (desde SOLICITADA) y anular (desde SOLICITADA o APROBADA) piden razón de 3 a 180, liberan la reserva y no borran")
    void rechazarYAnular() throws Exception {
        Saldo s = saldoCrc(null);
        int asientosAntes = asientos();
        long a = solicitarOk(s.id(), "100.00");
        long b = solicitarOk(s.id(), "100.00");
        long c = solicitarOk(s.id(), "100.00");
        assertThat(reservado(s.id())).isEqualByComparingTo("300.00");

        // razón fuera de rango → 400
        for (String razon : new String[]{"ab", "x".repeat(181), "   "}) {
            MvcResult r = rechazar(APR, a, razon);
            assertThat(r.getResponse().getStatus()).isEqualTo(400);
            assertThat(mensaje(r)).isEqualTo(MSG_RAZON_DEV);
            assertThat(anular(APR, a, razon).getResponse().getStatus()).isEqualTo(400);
        }
        assertThat(rechazar(SIN_PERMISO, a, "No corresponde").getResponse().getStatus()).isEqualTo(403);
        assertThat(anular(SIN_PERMISO, a, "No corresponde").getResponse().getStatus()).isEqualTo(403);

        // rechazar desde SOLICITADA
        MvcResult r = rechazar(APR, a, "La cuenta no es del cliente");
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        Map<String, Object> fila = jdbc.queryForMap("SELECT * FROM devoluciones_saldo_favor WHERE id = ?", a);
        assertThat(fila.get("estado")).isEqualTo("RECHAZADA");
        assertThat(fila.get("razon")).isEqualTo("La cuenta no es del cliente");
        assertThat(((Number) fila.get("rechazado_por")).intValue()).isEqualTo(idUsuarioDe(APR));
        assertThat(fila.get("fecha_rechazo")).isNotNull();
        assertThat(reservado(s.id())).isEqualByComparingTo("200.00");

        // anular desde SOLICITADA
        assertThat(anular(SOL, b, "El cliente desistió").getResponse().getStatus()).isEqualTo(200);
        fila = jdbc.queryForMap("SELECT * FROM devoluciones_saldo_favor WHERE id = ?", b);
        assertThat(fila.get("estado")).isEqualTo("ANULADA");
        assertThat(fila.get("razon")).isEqualTo("El cliente desistió");
        assertThat(((Number) fila.get("anulado_por")).intValue()).isEqualTo(idUsuarioDe(SOL));
        assertThat(fila.get("fecha_anulacion")).isNotNull();
        assertThat(reservado(s.id())).isEqualByComparingTo("100.00");

        // anular desde APROBADA
        aprobarOk(c);
        assertThat(anular(PAG, c, "Datos bancarios erróneos").getResponse().getStatus()).isEqualTo(200);
        assertThat(estadoDev(c)).isEqualTo("ANULADA");
        assertThat(reservado(s.id())).isEqualByComparingTo("0");

        // transiciones no permitidas → 409; nada se borró; ningún asiento
        assertConflicto(rechazar(APR, a, "Otra vez"), MSG_TRANSICION);
        assertConflicto(anular(APR, a, "Otra vez"), MSG_TRANSICION);
        assertConflicto(aprobar(APR, b, true), MSG_TRANSICION);
        assertConflicto(rechazar(APR, c, "Ya anulada"), MSG_TRANSICION);
        assertConflicto(pagar(PAG, b, bancoCrc(), "X-1", LocalDate.now().toString()), MSG_TRANSICION);
        assertThat(devolucionesDe(s.id())).isEqualTo(3);
        assertThat(asientos()).as("ninguna transición anterior a PAGADA genera asiento").isEqualTo(asientosAntes);
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("T-049 c6: rechazar desde APROBADA es 409; el aprobador puede rechazar a otro solicitante")
    void rechazarDesdeAprobada() throws Exception {
        Saldo s = saldoCrc(null);
        long id = solicitarOk(s.id(), "100.00");
        aprobarOk(id);
        assertConflicto(rechazar(APR, id, "Tarde para rechazar"), MSG_TRANSICION);
        assertThat(estadoDev(id)).isEqualTo("APROBADA");
        assertThat(reservado(s.id())).isEqualByComparingTo("100.00");
    }

    // ── Criterio 7: pago CRC ─────────────────────────────────────────────────

    @Test
    @DisplayName("T-049 c7: pagar en CRC: baja el saldo vivo, libera la reserva, un asiento DR 2.1.05 / CR hoja del banco, sin diferencial")
    void pagarCrc() throws Exception {
        Saldo s = saldoCrc(null);
        long id = solicitarOk(s.id(), "200.00");
        aprobarOk(id);
        int banco = bancoCrc();
        int asientosAntes = asientos();

        MvcResult r = pagarOk(id, banco);

        assertThat(asientos()).isEqualTo(asientosAntes + 1);
        Map<String, Object> d = jdbc.queryForMap("SELECT * FROM devoluciones_saldo_favor WHERE id = ?", id);
        assertThat(d.get("estado")).isEqualTo("PAGADA");
        assertThat(((Number) d.get("pagado_por")).intValue()).isEqualTo(idUsuarioDe(PAG));
        assertThat(d.get("fecha_pago")).isNotNull();
        assertThat(((Number) d.get("cuenta_bancaria_id")).intValue()).isEqualTo(banco);
        assertThat(d.get("numero_comprobante")).isEqualTo("SINPE-998877");
        assertThat(d.get("fecha_comprobante").toString()).isEqualTo(LocalDate.now().toString());
        assertThat((BigDecimal) d.get("diferencial_neto")).isEqualByComparingTo("0");
        assertThat(d.get("tc_dia_valor")).isNull();
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("300.00");
        assertThat(reservado(s.id())).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT estado FROM saldos_favor_cliente WHERE id = ?", String.class, s.id()))
                .isEqualTo("VIGENTE");
        long asiento = ((Number) d.get("asiento_id")).longValue();
        assertThat(debeEn(asiento, "9.2.05.001")).isEqualByComparingTo("200.00");
        assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(haber), 0) FROM detalles_asiento WHERE asiento_id = ? "
                + "AND cuenta_id = ?", BigDecimal.class, asiento, hojaDe(banco))).isEqualByComparingTo("200.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM detalles_asiento WHERE asiento_id = ?", Integer.class,
                asiento)).as("sin líneas de diferencial").isEqualTo(2);
        assertCuadra(asiento);
        assertThat(jdbc.queryForObject("SELECT estado FROM asientos_contables WHERE id = ?", String.class, asiento))
                .isEqualTo("CONTABILIZADO");
        JsonNode data = objectMapper.readTree(r.getResponse().getContentAsString()).get("data");
        assertThat(data.get("estado").asText()).isEqualTo("PAGADA");
        assertThat(data.get("idAsiento").asLong()).isEqualTo(asiento);
    }

    @Test
    @DisplayName("T-049 c7: pagar por el total deja el saldo AGOTADO")
    void pagarTotal() throws Exception {
        Saldo s = saldoCrc(null);
        long id = solicitarOk(s.id(), "500.00");
        aprobarOk(id);
        pagarOk(id, bancoCrc());
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("0");
        assertThat(reservado(s.id())).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT estado FROM saldos_favor_cliente WHERE id = ?", String.class, s.id()))
                .isEqualTo("AGOTADO");
    }

    @Test
    @DisplayName("T-049 c7: pagar valida permiso, pagador distinto del solicitante, cuenta (moneda, activa), comprobante y estado; no crea nada")
    void pagarValidaciones() throws Exception {
        Saldo s = saldoCrc(null);
        long id = solicitarOk(s.id(), "200.00");
        int banco = bancoCrc();
        int usd = bancoUsd();
        int inactiva = cuentaBancaria("Banco T049 inactivo", "CRC", "9.1.01.002.903", false);
        String hoy = LocalDate.now().toString();

        // desde SOLICITADA no se paga
        assertConflicto(pagar(PAG, id, banco, "C-1", hoy), MSG_TRANSICION);
        aprobarOk(id);
        Foto antes = foto(s.id());

        assertThat(pagar(SIN_PERMISO, id, banco, "C-1", hoy).getResponse().getStatus()).isEqualTo(403);
        assertConflicto(pagar(SOL, id, banco, "C-1", hoy), MSG_SEG_PAGAR);
        assertConflicto(pagar(PAG, id, null, "C-1", hoy), MSG_CUENTA);
        assertConflicto(pagar(PAG, id, usd, "C-1", hoy), MSG_CUENTA);
        assertConflicto(pagar(PAG, id, inactiva, "C-1", hoy), MSG_CUENTA);
        assertConflicto(pagar(PAG, id, 9_999_999, "C-1", hoy), MSG_CUENTA);
        assertConflicto(pagar(PAG, id, banco, null, hoy), MSG_COMPROBANTE);
        assertConflicto(pagar(PAG, id, banco, "   ", hoy), MSG_COMPROBANTE);
        assertConflicto(pagar(PAG, id, banco, "C-1", null), MSG_COMPROBANTE);
        assertConflicto(pagar(PAG, id, banco, "C-1", LocalDate.now().plusDays(1).toString()), MSG_COMPROBANTE);
        assertThat(pagar(PAG, 9_999_999L, banco, "C-1", hoy).getResponse().getStatus()).isEqualTo(404);

        assertThat(foto(s.id())).isEqualTo(antes);
        assertThat(estadoDev(id)).isEqualTo("APROBADA");
        assertThat(reservado(s.id())).isEqualByComparingTo("200.00");
        // el mismo usuario que aprobó sí puede pagar (la regla solo separa al solicitante)
        assertThat(pagar(APR, id, banco, "C-2", hoy).getResponse().getStatus()).isEqualTo(200);
        // y una PAGADA no se anula ni se reversa en B2
        assertConflicto(anular(APR, id, "Error de pago"), MSG_TRANSICION);
        assertConflicto(pagar(PAG, id, banco, "C-3", hoy), MSG_TRANSICION);
    }

    // ── Criterio 7: pago USD ─────────────────────────────────────────────────

    @Test
    @DisplayName("T-049 c7: pagar en USD con el TC del día más alto: pérdida realizada (DR gasto), tramo y neto guardados, el asiento cuadra")
    void pagarUsdPerdida() throws Exception {
        Saldo s = saldoUsd("500");
        long id = solicitarOk(s.id(), "100.00");
        aprobarOk(id);
        tcDelDia("520.00000");
        int banco = bancoUsd();

        pagarOk(id, banco);

        Map<String, Object> d = jdbc.queryForMap("SELECT * FROM devoluciones_saldo_favor WHERE id = ?", id);
        long asiento = ((Number) d.get("asiento_id")).longValue();
        assertThat(debeEn(asiento, "9.2.05.002")).isEqualByComparingTo("50000.00");
        assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(haber), 0) FROM detalles_asiento WHERE asiento_id = ? "
                + "AND cuenta_id = ?", BigDecimal.class, asiento, hojaDe(banco))).isEqualByComparingTo("52000.00");
        assertThat(debeEn(asiento, "9.5.20.001")).as("pérdida realizada").isEqualByComparingTo("2000.00");
        assertThat(haberEn(asiento, "9.4.20.001")).isZero();
        assertCuadra(asiento);
        assertThat((BigDecimal) d.get("tipo_cambio_origen")).isEqualByComparingTo("500");
        assertThat((BigDecimal) d.get("tc_dia_valor")).isEqualByComparingTo("520");
        assertThat(d.get("tc_dia_fecha").toString()).isEqualTo(LocalDate.now().toString());
        assertThat(d.get("tc_dia_tipo")).isEqualTo("VENTA");
        assertThat(d.get("tc_dia_fuente")).isEqualTo("BCCR");
        assertThat(d.get("tc_dia_origen")).isEqualTo("API");
        assertThat((BigDecimal) d.get("diferencial_saldo")).isEqualByComparingTo("2000.00");
        assertThat((BigDecimal) d.get("diferencial_neto")).as("debe − haber del asiento").isEqualByComparingTo("-2000.00");
        assertThat(saldoVivo(s.id())).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("T-049 c7: pagar en USD con el TC del día más bajo (fuente manual): ganancia realizada (CR ingreso)")
    void pagarUsdGanancia() throws Exception {
        Saldo s = saldoUsd("500");
        long id = solicitarOk(s.id(), "100.00");
        aprobarOk(id);
        tcDelDia("480.00000", FuenteTipoCambio.MANUAL);
        int banco = bancoUsd();

        pagarOk(id, banco);

        Map<String, Object> d = jdbc.queryForMap("SELECT * FROM devoluciones_saldo_favor WHERE id = ?", id);
        long asiento = ((Number) d.get("asiento_id")).longValue();
        assertThat(debeEn(asiento, "9.2.05.002")).isEqualByComparingTo("50000.00");
        assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(haber), 0) FROM detalles_asiento WHERE asiento_id = ? "
                + "AND cuenta_id = ?", BigDecimal.class, asiento, hojaDe(banco))).isEqualByComparingTo("48000.00");
        assertThat(haberEn(asiento, "9.4.20.001")).as("ganancia realizada").isEqualByComparingTo("2000.00");
        assertThat(debeEn(asiento, "9.5.20.001")).isZero();
        assertCuadra(asiento);
        assertThat(d.get("tc_dia_origen")).isEqualTo("MANUAL");
        assertThat((BigDecimal) d.get("diferencial_saldo")).isEqualByComparingTo("-2000.00");
        assertThat((BigDecimal) d.get("diferencial_neto")).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("T-049 c7: USD sin tipo de cambio de la fecha del pago (aunque haya de ayer): 409 y no se crea nada")
    void pagarUsdSinTipoDeCambio() throws Exception {
        Saldo s = saldoUsd("500");
        long id = solicitarOk(s.id(), "100.00");
        aprobarOk(id);
        tcDelDia("540.00000");
        jdbc.update("UPDATE tipos_cambio SET fecha = ? WHERE fecha = ?", LocalDate.now().minusDays(1).toString(),
                LocalDate.now().toString());
        int banco = bancoUsd();
        Foto antes = foto(s.id());

        MvcResult r = pagar(PAG, id, banco, "C-9", LocalDate.now().toString());

        assertConflicto(r, "No hay tipo de cambio registrado para " + LocalDate.now()
                + "; regístrelo antes de pagar la devolución");
        assertThat(foto(s.id())).isEqualTo(antes);
        assertThat(estadoDev(id)).isEqualTo("APROBADA");
        assertThat(reservado(s.id())).isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject("SELECT asiento_id FROM devoluciones_saldo_favor WHERE id = ?", Long.class, id))
                .isNull();
    }

    // ── Criterio 9: dónde aparece la devolución pagada ───────────────────────

    @Test
    @DisplayName("T-049 c9: la devolución pagada sale en el libro mayor de la hoja del banco y no en caja ni en pagos del día")
    void salidaDeBancosYNoEnCaja() throws Exception {
        Saldo s = saldoCrc(null);
        long id = solicitarOk(s.id(), "200.00");
        aprobarOk(id);
        int banco = bancoCrc();
        int pagosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM pagos", Integer.class);
        var cajaAntes = pagoService.buildReporteCaja(LocalDate.now());
        var pagosDelDiaAntes = pagoService.findByFechaPagoBetween(LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay());

        pagarOk(id, banco);

        long asiento = jdbc.queryForObject("SELECT asiento_id FROM devoluciones_saldo_favor WHERE id = ?", Long.class, id);
        String numero = jdbc.queryForObject("SELECT numero FROM asientos_contables WHERE id = ?", String.class, asiento);
        var movimientos = calculoContableService.obtenerLibroMayor(hojaDe(banco), null, null);
        assertThat(movimientos).anySatisfy(m -> {
            assertThat(m.getAsientoNumero()).isEqualTo(numero);
            assertThat(m.getHaber()).isEqualByComparingTo("200.00");
            assertThat(m.getDebe()).isEqualByComparingTo("0");
        });
        MvcResult page = mockMvc.perform(get("/contabilidad/reportes/libro-mayor").param("cuentaId", String.valueOf(hojaDe(banco)))
                .with(user(USUARIO).roles("ADMIN"))).andReturn();
        assertThat(page.getResponse().getStatus()).isEqualTo(200);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pagos", Integer.class)).isEqualTo(pagosAntes);
        var cajaDespues = pagoService.buildReporteCaja(LocalDate.now());
        assertThat(nz(cajaDespues.getTotalIngresos())).isEqualByComparingTo(nz(cajaAntes.getTotalIngresos()));
        assertThat(cajaDespues.getCantidadPagos()).isEqualTo(cajaAntes.getCantidadPagos());
        assertThat(nz(cajaDespues.getTotalTransferencia())).isEqualByComparingTo(nz(cajaAntes.getTotalTransferencia()));
        assertThat(nz(cajaDespues.getTotalOtros())).isEqualByComparingTo(nz(cajaAntes.getTotalOtros()));
        assertThat(nz(cajaDespues.getTotalEfectivo())).isEqualByComparingTo(nz(cajaAntes.getTotalEfectivo()));
        assertThat(pagoService.findByFechaPagoBetween(LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay())).hasSameSizeAs(pagosDelDiaAntes);
    }

    // ── Consultas para la interfaz ───────────────────────────────────────────

    @Test
    @DisplayName("T-049 c12 (API): el estado de cuenta trae por saldo disponible, reservado y las devoluciones con su historial")
    void consultaConDevoluciones() throws Exception {
        Saldo s = saldoCrc(null);
        long a = solicitarOk(s.id(), "100.00");
        long b = solicitarOk(s.id(), "50.00");
        aprobarOk(b);
        anular(SOL, a, "El cliente desistió");

        JsonNode fila = getData("/api/facturas/saldos-favor/cliente/" + s.idCliente()).get(0);
        assertThat(fila.get("saldo").get("reservado").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(fila.get("saldo").get("disponible").decimalValue()).isEqualByComparingTo("450.00");
        JsonNode devs = fila.get("devoluciones");
        assertThat(devs).hasSize(2);
        assertThat(devs.get(0).get("id").asLong()).isEqualTo(a);
        assertThat(devs.get(0).get("estado").asText()).isEqualTo("ANULADA");
        assertThat(devs.get(0).get("razon").asText()).isEqualTo("El cliente desistió");
        assertThat(devs.get(1).get("estado").asText()).isEqualTo("APROBADA");
        assertThat(devs.get(1).get("aprobadoPor").asInt()).isEqualTo(idUsuarioDe(APR));
        JsonNode lista = getData("/api/facturas/saldos-favor/" + s.id() + "/devoluciones");
        assertThat(lista).hasSize(2);
        JsonNode paraFactura = getData("/api/facturas/saldos-favor/para-factura/" + facturaAceptada(s.idCliente(),
                MonedaFE.CRC, "1")).get("saldos").get(0);
        assertThat(paraFactura.get("disponible").decimalValue()).isEqualByComparingTo("450.00");
    }

    // ── Criterios 10, 11 y 18: antigüedad ────────────────────────────────────

    @Test
    @DisplayName("T-049 c10/c11: antigüedad con saldos en los límites de cada tramo (7/8, 30/31, 90/91, 180/181), totales, reservado y moneda")
    void antiguedadLimites() throws Exception {
        Saldo crc = saldoCrc(null);
        Saldo usd = saldoUsd("500");
        solicitarOk(crc.id(), "100.00");
        // saldos de otras pruebas no deben mezclarse: se aísla por cliente
        record Limite(int dias, int columna) {
        }
        List<Limite> limites = List.of(new Limite(0, 0), new Limite(7, 0), new Limite(8, 1), new Limite(30, 1),
                new Limite(31, 2), new Limite(90, 2), new Limite(91, 3), new Limite(180, 3), new Limite(181, 4),
                new Limite(400, 4));
        String[] nombres = {"tramo0a7", "tramo8a30", "tramo31a90", "tramo91a180", "tramoMas180"};
        for (Limite l : limites) {
            // el corte nunca es futuro (T-049 review 1, c20): la edad se logra moviendo el origen al pasado
            jdbc.update("UPDATE saldos_favor_cliente SET fecha_origen = ? WHERE id IN (?, ?)",
                    LocalDate.now().minusDays(l.dias()).toString(), crc.id(), usd.id());
            LocalDate corte = LocalDate.now();
            JsonNode datos = getData("/reportes/api/saldos-favor-antiguedad?fechaCorte=" + corte);
            JsonNode filaCrc = filaDe(datos, crc.idCliente(), "CRC");
            JsonNode filaUsd = filaDe(datos, usd.idCliente(), "USD");
            for (int i = 0; i < nombres.length; i++) {
                BigDecimal esperado = i == l.columna() ? new BigDecimal("500.00") : BigDecimal.ZERO;
                assertThat(filaCrc.get(nombres[i]).decimalValue()).as(l.dias() + " días CRC " + nombres[i])
                        .isEqualByComparingTo(esperado);
                assertThat(filaUsd.get(nombres[i]).decimalValue()).as(l.dias() + " días USD " + nombres[i])
                        .isEqualByComparingTo(esperado);
            }
            assertThat(filaCrc.get("total").decimalValue()).isEqualByComparingTo("500.00");
            assertThat(filaCrc.get("reservado").decimalValue()).isEqualByComparingTo("100.00");
            assertThat(filaCrc.get("disponible").decimalValue()).isEqualByComparingTo("400.00");
            assertThat(filaUsd.get("reservado").decimalValue()).isEqualByComparingTo("0");
            // totales por tramo y moneda: incluyen al menos estos saldos
            JsonNode totCrc = totalDe(datos, "CRC");
            JsonNode totUsd = totalDe(datos, "USD");
            assertThat(totCrc.get(nombres[l.columna()]).decimalValue()).isGreaterThanOrEqualTo(new BigDecimal("500.00"));
            assertThat(totUsd.get(nombres[l.columna()]).decimalValue()).isGreaterThanOrEqualTo(new BigDecimal("500.00"));
            assertThat(totCrc.get("reservado").decimalValue()).isGreaterThanOrEqualTo(new BigDecimal("100.00"));
        }
        // un saldo posterior a la fecha de corte no existía todavía: no aparece (y ninguna edad es negativa)
        jdbc.update("UPDATE saldos_favor_cliente SET fecha_origen = ? WHERE id IN (?, ?)", LocalDate.now().toString(),
                crc.id(), usd.id());
        JsonNode ayer = getData("/reportes/api/saldos-favor-antiguedad?fechaCorte=" + LocalDate.now().minusDays(1));
        assertThat(filaOpcional(ayer, crc.idCliente(), "CRC")).isNull();
        assertThat(filaOpcional(ayer, usd.idCliente(), "USD")).isNull();
        for (JsonNode f : ayer.get("filas")) {
            for (String t : nombres) {
                assertThat(f.get(t).decimalValue()).as("ningún importe en tramo negativo").isGreaterThanOrEqualTo(BigDecimal.ZERO);
            }
        }
        // por defecto, el corte es hoy
        JsonNode hoy = getData("/reportes/api/saldos-favor-antiguedad");
        assertThat(hoy.get("fechaCorte").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(filaDe(hoy, crc.idCliente(), "CRC").get("tramo0a7").decimalValue()).isEqualByComparingTo("500.00");
        // un saldo agotado no es vivo: no aparece
        jdbc.update("UPDATE saldos_favor_cliente SET saldo_vivo = 0, estado = 'AGOTADO' WHERE id = ?", usd.id());
        assertThat(filaOpcional(getData("/reportes/api/saldos-favor-antiguedad"), usd.idCliente(), "USD")).isNull();
    }

    static final String MENSAJE_CORTE_FUTURO = "La fecha de corte no puede ser futura";
    static final String ROTULO_SALDO_VIVO = "Importes al saldo vivo de hoy; la fecha de corte solo define la antigüedad";

    @Test
    @DisplayName("T-049 review 1 c20: una fecha de corte futura responde 400 con mensaje fijo en la página, el JSON y las tres exportaciones")
    void antiguedadCorteFuturo400() throws Exception {
        saldoCrc(null);
        String manana = LocalDate.now().plusDays(1).toString();
        MvcResult api = mockMvc.perform(get("/reportes/api/saldos-favor-antiguedad?fechaCorte=" + manana)
                .with(user(USUARIO).roles("ADMIN"))).andReturn();
        assertThat(api.getResponse().getStatus()).isEqualTo(400);
        assertThat(api.getResponse().getContentAsString()).contains(MENSAJE_CORTE_FUTURO);
        MvcResult page = mockMvc.perform(get("/reportes/saldos-favor-antiguedad?fechaCorte=" + manana)
                .with(user(USUARIO).roles("ADMIN"))).andReturn();
        assertThat(page.getResponse().getStatus()).isEqualTo(400);
        assertThat(page.getResponse().getContentAsString()).contains(MENSAJE_CORTE_FUTURO);
        for (String formato : new String[]{"excel", "csv", "pdf"}) {
            MvcResult r = mockMvc.perform(get("/reportes/saldos-favor-antiguedad/exportar/" + formato + "?fechaCorte="
                    + manana).with(user(USUARIO).roles("ADMIN"))).andReturn();
            assertThat(r.getResponse().getStatus()).as(formato).isEqualTo(400);
            assertThat(r.getResponse().getContentAsString()).as(formato).contains(MENSAJE_CORTE_FUTURO);
        }
        // hoy sigue siendo válido
        assertThat(mockMvc.perform(get("/reportes/api/saldos-favor-antiguedad?fechaCorte=" + LocalDate.now())
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("T-049 review 1 c20: la página y las exportaciones rotulan que los importes son al saldo vivo de hoy")
    void antiguedadRotuloSaldoVivoDeHoy() throws Exception {
        saldoCrc(null);
        String corte = "?fechaCorte=" + LocalDate.now().minusDays(3);
        String html = mockMvc.perform(get("/reportes/saldos-favor-antiguedad" + corte)
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getContentAsString();
        assertThat(html).contains(ROTULO_SALDO_VIVO);
        String csv = new String(mockMvc.perform(get("/reportes/saldos-favor-antiguedad/exportar/csv" + corte)
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getContentAsByteArray(),
                java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains(ROTULO_SALDO_VIVO);
        byte[] excel = mockMvc.perform(get("/reportes/saldos-favor-antiguedad/exportar/excel" + corte)
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getContentAsByteArray();
        try (org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new java.io.ByteArrayInputStream(excel))) {
            StringBuilder texto = new StringBuilder();
            wb.getSheetAt(0).forEach(row -> row.forEach(c -> texto.append(c.toString()).append('\n')));
            assertThat(texto.toString()).contains(ROTULO_SALDO_VIVO);
        }
        byte[] pdf = mockMvc.perform(get("/reportes/saldos-favor-antiguedad/exportar/pdf" + corte)
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getContentAsByteArray();
        try (com.itextpdf.kernel.pdf.PdfDocument doc = new com.itextpdf.kernel.pdf.PdfDocument(
                new com.itextpdf.kernel.pdf.PdfReader(new java.io.ByteArrayInputStream(pdf)))) {
            StringBuilder texto = new StringBuilder();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                texto.append(com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(doc.getPage(i)));
            }
            assertThat(texto.toString().replace("\n", " ")).contains(ROTULO_SALDO_VIVO);
        }
    }

    @Test
    @DisplayName("T-049 review 1 c19: el asiento de una devolución pagada tiene el tipo propio AUTOMATICO_DEVOLUCION_SALDO_FAVOR, es automático y no cuenta como cobro")
    void asientoDeLaDevolucionTieneTipoPropio() throws Exception {
        Saldo s = saldoCrc(null);
        long id = solicitarOk(s.id(), "200.00");
        aprobarOk(id);
        pagarOk(id, bancoCrc());
        long asiento = jdbc.queryForObject("SELECT asiento_id FROM devoluciones_saldo_favor WHERE id = ?", Long.class, id);
        assertThat(jdbc.queryForObject("SELECT tipo FROM asientos_contables WHERE id = ?", String.class, asiento))
                .isEqualTo("AUTOMATICO_DEVOLUCION_SALDO_FAVOR");
        assertThat(jdbc.queryForObject("SELECT pago_id FROM asientos_contables WHERE id = ?", Long.class, asiento))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asientos_contables WHERE id = ? AND tipo = "
                + "'AUTOMATICO_PAGO'", Integer.class, asiento)).isZero();
        assertThat(api.astro.whats_orders_manager.modules.contabilidad.enums.TipoAsiento
                .valueOf("AUTOMATICO_DEVOLUCION_SALDO_FAVOR").esAutomatico()).isTrue();
    }

    @Test
    @DisplayName("T-049 c10: la página y las exportaciones (Excel, CSV, PDF) del reporte de antigüedad responden")
    void antiguedadPaginaYExportaciones() throws Exception {
        Saldo crc = saldoCrc(null);
        MvcResult page = mockMvc.perform(get("/reportes/saldos-favor-antiguedad").with(user(USUARIO).roles("ADMIN")))
                .andReturn();
        assertThat(page.getResponse().getStatus()).isEqualTo(200);
        assertThat(page.getResponse().getContentAsString()).contains("Antigüedad de saldos a favor");
        for (String formato : new String[]{"excel", "csv", "pdf"}) {
            MvcResult r = mockMvc.perform(get("/reportes/saldos-favor-antiguedad/exportar/" + formato)
                    .with(user(USUARIO).roles("ADMIN"))).andReturn();
            assertThat(r.getResponse().getStatus()).as(formato).isEqualTo(200);
            assertThat(r.getResponse().getContentAsByteArray().length).as(formato).isGreaterThan(50);
        }
        String csv = mockMvc.perform(get("/reportes/saldos-favor-antiguedad/exportar/csv")
                .with(user(USUARIO).roles("ADMIN"))).andReturn().getResponse().getContentAsString();
        assertThat(csv).contains("0-7").contains("8-30").contains("31-90").contains("91-180").contains(">180")
                .contains("Reservado");
        assertThat(csv).contains(String.valueOf(crc.idCliente()).isEmpty() ? "" : "CRC");
    }

    @Test
    @DisplayName("T-049 c18: el reporte de antigüedad no ofrece revertir saldos a ingresos ni ninguna acción: solo GET, sin botones ni formularios")
    void antiguedadSinReversion() throws Exception {
        saldoCrc(null);
        handlerMapping.getHandlerMethods().forEach((info, metodo) -> {
            boolean esDelReporte = info.getPatternValues().stream().anyMatch(p -> p.contains("saldos-favor-antiguedad"));
            if (esDelReporte) {
                assertThat(info.getMethodsCondition().getMethods().stream().map(Enum::name))
                        .as(info.toString()).containsOnly("GET");
                assertThat(info.getPatternValues()).noneMatch(p -> p.toLowerCase().matches(".*(revert|ingreso|ajuste|reclasific).*"));
            }
        });
        // ninguna ruta de ninguna parte habla de revertir saldos a ingresos
        handlerMapping.getHandlerMethods().keySet().forEach(info -> assertThat(info.getPatternValues())
                .noneMatch(p -> p.toLowerCase().contains("saldos-favor") && p.toLowerCase().matches(".*(revert|ingreso).*")));
        for (String metodo : new String[]{"post", "put", "delete"}) {
            var req = switch (metodo) {
                case "post" -> post("/reportes/saldos-favor-antiguedad");
                case "put" -> org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/reportes/saldos-favor-antiguedad");
                default -> org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/reportes/saldos-favor-antiguedad");
            };
            int estado = mockMvc.perform(req.with(user(USUARIO).roles("ADMIN")).with(csrf())).andReturn().getResponse().getStatus();
            assertThat(estado).as(metodo).isIn(404, 405);
        }
        String html = mockMvc.perform(get("/reportes/saldos-favor-antiguedad").with(user(USUARIO).roles("ADMIN")))
                .andReturn().getResponse().getContentAsString().toLowerCase();
        // el contenido de la página (sin navbar ni sidebar): solo lectura, un filtro GET por fecha y enlaces de exportación
        String contenido = html.substring(html.indexOf("<main"), html.indexOf("</main>"));
        assertThat(contenido).doesNotContain("revert").doesNotContain("ingresos").doesNotContain("method=\"post\"")
                .doesNotContain("fetch(").doesNotContain("onclick");
    }

    private static JsonNode filaOpcional(JsonNode datos, int idCliente, String moneda) {
        for (JsonNode f : datos.get("filas")) {
            if (f.get("idCliente").asInt() == idCliente && moneda.equals(f.get("moneda").asText())) {
                return f;
            }
        }
        return null;
    }

    private static JsonNode filaDe(JsonNode datos, int idCliente, String moneda) {
        JsonNode f = filaOpcional(datos, idCliente, moneda);
        assertThat(f).as("fila del cliente " + idCliente + " " + moneda).isNotNull();
        return f;
    }

    private static JsonNode totalDe(JsonNode datos, String moneda) {
        for (JsonNode f : datos.get("totales")) {
            if (moneda.equals(f.get("moneda").asText())) {
                return f;
            }
        }
        throw new AssertionError("sin total de " + moneda);
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
            p.setFechaPago(LocalDateTime.now().minusMinutes(1));
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
        pago.put("fechaPago", LocalDateTime.now().withNano(0).minusMinutes(1).toString());
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
