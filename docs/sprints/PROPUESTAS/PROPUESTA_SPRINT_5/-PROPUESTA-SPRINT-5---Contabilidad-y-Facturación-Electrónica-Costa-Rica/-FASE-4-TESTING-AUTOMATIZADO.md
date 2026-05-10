## 🧪 FASE 4: TESTING AUTOMATIZADO

**Duración:** 4-5 días  
**Prioridad:** ⭐⭐ ALTA  
**Progreso estimado:** 0/32 tareas

### Objetivos

- Implementar testing automatizado completo
- Cobertura mínima del 80%
- Tests unitarios con JUnit 5 + Mockito
- Tests de integración con TestContainers
- CI/CD básico con GitHub Actions

### 4.1 Configuración de Testing (6 tareas)

- [ ] 4.1.1 - Actualizar `pom.xml` con dependencias de testing
  - JUnit 5 (Jupiter)
  - Mockito
  - AssertJ
  - TestContainers (MySQL)
  - Spring Boot Test
  - H2 para tests
- [ ] 4.1.2 - Crear `application-test.yml` para perfil de testing
- [ ] 4.1.3 - Configurar TestContainers para MySQL
- [ ] 4.1.4 - Crear clase base `BaseIntegrationTest`
- [ ] 4.1.5 - Configurar Jacoco para cobertura de código
- [ ] 4.1.6 - Configurar Maven Surefire para ejecución de tests

### 4.2 Tests Unitarios - Pagos (5 tareas)

- [ ] 4.2.1 - `PagoServiceTest` - Registrar pago válido
- [ ] 4.2.2 - `PagoServiceTest` - Validar monto mayor a saldo (debe fallar)
- [ ] 4.2.3 - `PagoServiceTest` - Anular pago y verificar reversión de saldo
- [ ] 4.2.4 - `PagoServiceTest` - Múltiples pagos parciales
- [ ] 4.2.5 - `EstadoCuentaServiceTest` - Cálculo de saldo correcto

### 4.3 Tests Unitarios - Contabilidad (6 tareas)

- [ ] 4.3.1 - `ContabilidadServiceTest` - Asiento cuadrado (debe = haber)
- [ ] 4.3.2 - `ContabilidadServiceTest` - Asiento descuadrado (debe fallar)
- [ ] 4.3.3 - `ContabilidadServiceTest` - Asiento desde factura automático
- [ ] 4.3.4 - `ContabilidadServiceTest` - Asiento desde pago automático
- [ ] 4.3.5 - `ContabilidadServiceTest` - Anular asiento
- [ ] 4.3.6 - `CuentaContableServiceTest` - Validar jerarquía de cuentas

### 4.4 Tests Unitarios - Facturación Electrónica (5 tareas)

- [ ] 4.4.1 - `XmlGeneratorServiceTest` - Generar XML válido
- [ ] 4.4.2 - `XmlGeneratorServiceTest` - Validar contra XSD v4.4
- [ ] 4.4.3 - `FirmaDigitalServiceTest` - Firmar XML (mock de certificado)
- [ ] 4.4.4 - `HaciendaApiServiceTest` - Obtener token (mock)
- [ ] 4.4.5 - `FacturaElectronicaServiceTest` - Workflow completo (mocks)

### 4.5 Tests de Integración (6 tareas)

- [ ] 4.5.1 - `PagoIntegrationTest` - CRUD completo con DB real (TestContainers)
- [ ] 4.5.2 - `ContabilidadIntegrationTest` - Asientos con DB real
- [ ] 4.5.3 - `FacturaElectronicaIntegrationTest` - Envío a SANDBOX Hacienda
- [ ] 4.5.4 - `ReporteIntegrationTest` - Generación de reportes
- [ ] 4.5.5 - `NotificacionIntegrationTest` - Envío de notificaciones
- [ ] 4.5.6 - Test E2E: Factura → Pago → Asiento → Hacienda

### 4.6 CI/CD con GitHub Actions (4 tareas)

- [ ] 4.6.1 - Crear `.github/workflows/maven.yml`
- [ ] 4.6.2 - Configurar pipeline: build → test → coverage
- [ ] 4.6.3 - Integrar Jacoco report en GitHub Actions
- [ ] 4.6.4 - Badge de coverage en README.md

---

