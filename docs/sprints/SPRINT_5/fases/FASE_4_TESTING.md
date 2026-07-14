# 🧪 FASE 4: Testing Automatizado

**Sprint:** 5  
**Fase:** 4 de 5  
**Duración estimada:** 4-5 días  
**Prioridad:** ⭐⭐ ALTA  
**Estado:** 📋 PENDIENTE (0/32 tareas)

---

## 📋 OBJETIVO DE LA FASE

Implementar suite completa de testing automatizado para garantizar:
- Cobertura de código mínima del 80%
- Tests unitarios de servicios críticos
- Tests de integración end-to-end
- Tests de integración con Hacienda Sandbox
- CI/CD con GitHub Actions

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/32] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Tests Unitarios - Pagos        [0/8]  ░░░░░░░░░░ 0%
├─ 2. Tests Unitarios - Contabilidad [0/10] ░░░░░░░░░░ 0%
├─ 3. Tests Unitarios - FE CR        [0/8]  ░░░░░░░░░░ 0%
├─ 4. Tests de Integración           [0/4]  ░░░░░░░░░░ 0%
└─ 5. CI/CD                          [0/2]  ░░░░░░░░░░ 0%
```

---

## 📦 1. TESTS UNITARIOS - MÓDULO DE PAGOS (8 tareas)

### 1.1. `PagoServiceTest.java`

**Archivo:** `src/test/java/com/erp/service/PagoServiceTest.java`

#### Tareas:

- [ ] **1.1.1** Setup de test con Mockito
  - Mock de repositories
  - Mock de AsientoContableService

```java
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {
    
    @Mock
    private PagoRepository pagoRepository;
    
    @Mock
    private FacturaRepository facturaRepository;
    
    @Mock
    private AsientoContableService asientoContableService;
    
    @InjectMocks
    private PagoServiceImpl pagoService;
    
    private Factura factura;
    private Cliente cliente;
    
    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Juan Pérez");
        
        factura = new Factura();
        factura.setId(1L);
        factura.setNumero("F-001");
        factura.setCliente(cliente);
        factura.setSubtotal(new BigDecimal("10000"));
        factura.setIva(new BigDecimal("1300"));
        factura.setTotal(new BigDecimal("11300"));
        factura.setPagos(new ArrayList<>());
    }
}
```

- [ ] **1.1.2** Test: `testRegistrarPago_Exitoso()`
  - Given: Factura con saldo pendiente
  - When: Registrar pago válido
  - Then: Pago guardado, estado de factura actualizado, asiento creado

```java
@Test
void testRegistrarPago_Exitoso() {
    // Given
    PagoDTO pagoDTO = new PagoDTO();
    pagoDTO.setFacturaId(1L);
    pagoDTO.setMonto(new BigDecimal("5000"));
    pagoDTO.setMetodoPago(MetodoPago.EFECTIVO);
    pagoDTO.setFechaPago(LocalDateTime.now());
    
    when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));
    when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> i.getArguments()[0]);
    
    // When
    PagoDTO resultado = pagoService.registrarPago(pagoDTO);
    
    // Then
    assertNotNull(resultado);
    assertEquals(new BigDecimal("5000"), resultado.getMonto());
    verify(pagoRepository, times(1)).save(any(Pago.class));
    verify(facturaRepository, times(1)).save(factura);
    verify(asientoContableService, times(1)).registrarAsientoPago(any(Pago.class));
    assertEquals(EstadoPagoFactura.PAGADO_PARCIAL, factura.getEstadoPago());
}
```

- [ ] **1.1.3** Test: `testRegistrarPago_MontoExcedeSaldo_DeberiaLanzarExcepcion()`
  - Validar que no se permita pago mayor al saldo

```java
@Test
void testRegistrarPago_MontoExcedeSaldo_DeberiaLanzarExcepcion() {
    // Given
    PagoDTO pagoDTO = new PagoDTO();
    pagoDTO.setFacturaId(1L);
    pagoDTO.setMonto(new BigDecimal("20000")); // Excede total de factura
    pagoDTO.setMetodoPago(MetodoPago.EFECTIVO);
    
    when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));
    
    // When & Then
    assertThrows(BusinessException.class, () -> {
        pagoService.registrarPago(pagoDTO);
    });
    
    verify(pagoRepository, never()).save(any(Pago.class));
}
```

- [ ] **1.1.4** Test: `testRegistrarPago_FacturaNoExiste_DeberiaLanzarExcepcion()`

- [ ] **1.1.5** Test: `testAnularPago_Exitoso()`
  - Validar reversión de asiento contable

- [ ] **1.1.6** Test: `testObtenerEstadoCuentaCliente()`
  - Verificar cálculos correctos

- [ ] **1.1.7** Test: `testCalcularTotalPagado()`
  - Solo suma pagos CONFIRMADO y CONCILIADO

- [ ] **1.1.8** Test: `testActualizarEstadoPago_Factura()`
  - PENDIENTE → PAGADO_PARCIAL → PAGADO_TOTAL

---

## 📦 2. TESTS UNITARIOS - CONTABILIDAD (10 tareas)

### 2.1. `AsientoContableServiceTest.java`

**Archivo:** `src/test/java/com/erp/service/contabilidad/AsientoContableServiceTest.java`

#### Tareas:

- [ ] **2.1.1** Test: `testCrearAsientoManual_Cuadrado_Exitoso()`
  - Asiento con debe = haber

```java
@Test
void testCrearAsientoManual_Cuadrado_Exitoso() {
    // Given
    AsientoContableDTO asientoDTO = new AsientoContableDTO();
    asientoDTO.setFecha(LocalDate.now());
    asientoDTO.setConcepto("Asiento de prueba");
    
    DetalleAsientoDTO detalle1 = new DetalleAsientoDTO();
    detalle1.setCuentaId(1L); // Caja
    detalle1.setDebe(new BigDecimal("10000"));
    detalle1.setHaber(BigDecimal.ZERO);
    
    DetalleAsientoDTO detalle2 = new DetalleAsientoDTO();
    detalle2.setCuentaId(2L); // Ventas
    detalle2.setDebe(BigDecimal.ZERO);
    detalle2.setHaber(new BigDecimal("10000"));
    
    asientoDTO.setDetalles(Arrays.asList(detalle1, detalle2));
    
    when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaCaja));
    when(cuentaRepository.findById(2L)).thenReturn(Optional.of(cuentaVentas));
    when(asientoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    
    // When
    AsientoContableDTO resultado = asientoService.crearAsientoManual(asientoDTO);
    
    // Then
    assertNotNull(resultado);
    verify(asientoRepository, times(1)).save(any(AsientoContable.class));
}
```

- [ ] **2.1.2** Test: `testCrearAsientoManual_Descuadrado_DeberiaLanzarExcepcion()`
  - Debe != Haber

- [ ] **2.1.3** Test: `testCrearAsientoManual_CuentaNoExiste_DeberiaLanzarExcepcion()`

- [ ] **2.1.4** Test: `testCrearAsientoManual_CuentaNoAceptaMovimientos_DeberiaLanzarExcepcion()`
  - Cuentas de agrupación no permiten movimientos

- [ ] **2.1.5** Test: `testRegistrarAsientoVenta_Exitoso()`
  - Verificar estructura del asiento automático

```java
@Test
void testRegistrarAsientoVenta_Exitoso() {
    // Given
    Factura factura = crearFacturaEjemplo();
    when(cuentaRepository.findByCodigo("1.1.02.001")).thenReturn(Optional.of(cuentaCobrar));
    when(cuentaRepository.findByCodigo("4.1.01.001")).thenReturn(Optional.of(cuentaVentas));
    when(cuentaRepository.findByCodigo("2.1.02.001")).thenReturn(Optional.of(cuentaIVA));
    
    // When
    asientoService.registrarAsientoVenta(factura);
    
    // Then
    ArgumentCaptor<AsientoContable> captor = ArgumentCaptor.forClass(AsientoContable.class);
    verify(asientoRepository).save(captor.capture());
    
    AsientoContable asiento = captor.getValue();
    assertEquals(3, asiento.getDetalles().size()); // Cobrar, Ventas, IVA
    assertEquals(factura.getTotal(), asiento.getTotalDebe());
    assertEquals(factura.getTotal(), asiento.getTotalHaber());
}
```

- [ ] **2.1.6** Test: `testRegistrarAsientoPago_Efectivo()`
  - Validar cuenta Caja

- [ ] **2.1.7** Test: `testRegistrarAsientoPago_Transferencia()`
  - Validar cuenta Banco

- [ ] **2.1.8** Test: `testAnularAsiento_Exitoso()`

- [ ] **2.1.9** Test: `testGenerarNumeroConsecutivo()`
  - Formato correcto: ASI-2026-00001

- [ ] **2.1.10** Test: `testContabilizarAsiento_CambiaEstado()`

---

### 2.2. `LibroContableServiceTest.java`

#### Tareas adicionales en sección 2:

Ya cubiertos en las 10 tareas anteriores.

---

## 📦 3. TESTS UNITARIOS - FACTURACIÓN ELECTRÓNICA CR (8 tareas)

### 3.1. `XMLGeneratorServiceTest.java`

**Archivo:** `src/test/java/com/erp/service/facturacion/XMLGeneratorServiceTest.java`

#### Tareas:

- [ ] **3.1.1** Test: `testGenerarXMLFactura_EstructuraCorrecta()`
  - Validar que tiene todos los elementos requeridos

```java
@Test
void testGenerarXMLFactura_EstructuraCorrecta() throws Exception {
    // Given
    Factura factura = crearFacturaCompleta();
    String claveNumerica = "50601012026123456789012345678901234567890123456789";
    String consecutivo = "001-001-0000000001";
    
    // When
    String xml = xmlGenerator.generarXMLFactura(factura, claveNumerica, consecutivo);
    
    // Then
    assertNotNull(xml);
    assertTrue(xml.contains("<FacturaElectronica"));
    assertTrue(xml.contains("<Clave>" + claveNumerica + "</Clave>"));
    assertTrue(xml.contains("<NumeroConsecutivo>" + consecutivo + "</NumeroConsecutivo>"));
    assertTrue(xml.contains("<Emisor>"));
    assertTrue(xml.contains("<Receptor>"));
    assertTrue(xml.contains("<DetalleServicio>"));
    assertTrue(xml.contains("<ResumenFactura>"));
}
```

- [ ] **3.1.2** Test: `testValidarXMLcontraXSD_XMLValido()`
  - XML generado pasa validación XSD

- [ ] **3.1.3** Test: `testValidarXMLcontraXSD_XMLInvalido()`
  - XML malformado falla validación

- [ ] **3.1.4** Test: `testGenerarClaveNumerica_Formato50Digitos()`
  - Clave tiene exactamente 50 dígitos

```java
@Test
void testGenerarClaveNumerica_Formato50Digitos() {
    // Given
    Factura factura = crearFacturaEjemplo();
    String consecutivo = "001-001-0000000001";
    
    // When
    String clave = xmlGenerator.generarClaveNumerica(factura, consecutivo);
    
    // Then
    assertEquals(50, clave.length());
    assertTrue(clave.matches("\\d{50}")); // Solo dígitos
    assertTrue(clave.startsWith("506")); // País CR
}
```

- [ ] **3.1.5** Test: `testGenerarConsecutivo_FormatoOK()`
  - Formato 001-001-0000000001

---

### 3.2. `FirmaDigitalServiceTest.java`

#### Tareas:

- [ ] **3.2.1** Test: `testFirmarXML_ConCertificadoTest()`
  - Usar certificado de prueba

- [ ] **3.2.2** Test: `testValidarFirma_FirmaValida()`

- [ ] **3.2.3** Test: `testFirmarXML_CertificadoInvalido_DeberiaLanzarExcepcion()`

---

## 📦 4. TESTS DE INTEGRACIÓN (4 tareas)

### 4.1. Tests con TestContainers (MySQL)

**Archivo:** `src/test/java/com/erp/integration/PagoIntegrationTest.java`

#### Tareas:

- [ ] **4.1.1** Configurar TestContainers con MySQL

```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PagoIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("erp_test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Autowired
    private PagoService pagoService;
    
    @Autowired
    private FacturaRepository facturaRepository;
    
    @Test
    void testFlujoPagoCompleto_E2E() {
        // Given: Crear factura en BD
        Factura factura = new Factura();
        // ... configurar factura
        factura = facturaRepository.save(factura);
        
        // When: Registrar pago
        PagoDTO pagoDTO = new PagoDTO();
        pagoDTO.setFacturaId(factura.getId());
        pagoDTO.setMonto(factura.getTotal());
        pagoDTO.setMetodoPago(MetodoPago.EFECTIVO);
        pagoDTO.setFechaPago(LocalDateTime.now());
        
        PagoDTO resultado = pagoService.registrarPago(pagoDTO);
        
        // Then: Verificar en BD
        Factura facturaActualizada = facturaRepository.findById(factura.getId()).get();
        assertEquals(EstadoPagoFactura.PAGADO_TOTAL, facturaActualizada.getEstadoPago());
        assertNotNull(resultado.getId());
    }
}
```

- [ ] **4.1.2** Test E2E: Flujo completo de factura + pago + contabilidad
  - Crear factura → Registrar pago → Verificar asientos

---

### 4.2. Tests con Hacienda Sandbox

**Archivo:** `src/test/java/com/erp/integration/HaciendaIntegrationTest.java`

#### Tareas:

- [ ] **4.2.1** Test: `testEnviarComprobanteASandbox()`
  - Envío real a API Sandbox
  - Requiere credenciales de prueba

```java
@SpringBootTest
@ActiveProfiles("test")
class HaciendaIntegrationTest {
    
    @Autowired
    private HaciendaAPIService haciendaAPI;
    
    @Autowired
    private ComprobanteElectronicoService comprobanteService;
    
    @Test
    @Disabled("Requiere credenciales ATV de Sandbox")
    void testEnviarComprobanteASandbox() {
        // Given
        Factura factura = crearFacturaPrueba();
        
        // When
        ComprobanteElectronico comprobante = comprobanteService.procesarFactura(factura.getId());
        
        // Then
        assertNotNull(comprobante.getClaveNumerica());
        assertEquals(EstadoComprobante.ENVIADO, comprobante.getEstado());
        
        // Esperar respuesta
        Thread.sleep(5000);
        
        String estado = haciendaAPI.consultarEstado(comprobante.getClaveNumerica());
        assertTrue(Arrays.asList("aceptado", "procesando").contains(estado));
    }
}
```

- [ ] **4.2.2** Test: `testConsultarEstadoComprobante()`
  - Consulta de estado en Sandbox

---

## 📦 5. CI/CD (2 tareas)

### 5.1. GitHub Actions

**Archivo:** `.github/workflows/maven-test.yml`

#### Tareas:

- [ ] **5.1.1** Crear workflow de CI
  - Ejecutar tests en cada push/PR
  - Generar reporte de cobertura

```yaml
name: Java CI with Maven

on:
  push:
    branches: [ master, develop, feature/** ]
  pull_request:
    branches: [ master, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: erp_test
        ports:
          - 3306:3306
        options: --health-cmd="mysqladmin ping" --health-interval=10s --health-timeout=5s --health-retries=3
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven
    
    - name: Run tests with Maven
      run: mvn clean test
    
    - name: Generate JaCoCo Coverage Report
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        files: ./target/site/jacoco/jacoco.xml
        fail_ci_if_error: true
    
    - name: Check coverage threshold
      run: |
        mvn jacoco:check -Djacoco.minimum.coverage=0.80
```

- [ ] **5.1.2** Configurar JaCoCo en `pom.xml`
  - Cobertura mínima del 80%

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ Cobertura de código >= 80% (medida por JaCoCo)  
✅ Todos los servicios críticos tienen tests unitarios  
✅ Tests de integración con BD (TestContainers) funcionan  
✅ CI/CD ejecuta tests automáticamente en cada push  
✅ No hay tests flakey (intermitentes)  
✅ Tests de Hacienda Sandbox pasan (con credenciales)  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Todos los servicios de Fases 1, 2 y 3 implementados
- ✅ Base de datos configurada
- ⚠️ Credenciales ATV Sandbox (para tests de FE)

---

## 🔄 PRÓXIMOS PASOS

Una vez completada esta fase:
1. ✅ Verificar cobertura de tests
2. ✅ Ejecutar CI/CD localmente
3. 🚀 Continuar con **FASE 5: Documentación**

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** Equipo de QA y Desarrollo
