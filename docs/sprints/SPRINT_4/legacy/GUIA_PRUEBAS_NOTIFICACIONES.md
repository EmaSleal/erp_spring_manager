# 🧪 GUÍA DE PRUEBAS - Sistema de Notificaciones
**Sprint 4 - Fase 3.8: Testing**  
WhatsApp Orders Manager

---

## 📋 ÍNDICE

1. [Módulos a Probar](#módulos-a-probar)
2. [Proceso de Pruebas](#proceso-de-pruebas)
3. [Casos de Prueba por Módulo](#casos-de-prueba-por-módulo)
4. [Pruebas de Integración](#pruebas-de-integración)
5. [Flujo E2E Completo](#flujo-e2e-completo)
6. [Herramientas y Configuración](#herramientas-y-configuración)

---

## 🎯 MÓDULOS A PROBAR

### 1. **NotificacionService** (PRIORITARIO ⭐⭐⭐)
**Ubicación:** `services/impl/NotificacionServiceImpl.java`

**Métodos críticos a probar:**
```java
✅ enviarNotificacion(Usuario, TipoNotificacion, CanalNotificacion, String, String)
✅ enviarNotificacionConAccion(Usuario, TipoNotificacion, CanalNotificacion, String, String, String, String)
✅ enviarNotificacionConPlantilla(Usuario, TipoNotificacion, CanalNotificacion, Integer, Map)
✅ marcarComoLeida(Integer idNotificacion)
✅ marcarTodasComoLeidas(Integer idUsuario)
✅ findNoLeidasByUsuarioId(Integer idUsuario, Pageable)
✅ countNoLeidasByUsuarioId(Integer idUsuario)
```

**¿Por qué es prioritario?**
- Es el núcleo del sistema de notificaciones
- Maneja la lógica de negocio principal
- Interactúa con múltiples servicios (Email, WebSocket)
- Gestiona la persistencia en BD

---

### 2. **NotificacionListener** (PRIORITARIO ⭐⭐⭐)
**Ubicación:** `listeners/NotificacionListener.java`

**Funcionalidades a probar:**
```java
✅ procesarNotificacion(NotificacionEvent) - @Async
✅ procesarNotificacionBroadcast(NotificacionEvent) - @Async
✅ resolverUsuario() - Obtiene usuario por ID o objeto
✅ determinarCanales() - Selecciona canales según configuración
✅ enviarPorCanal() - Delega envío según canal
```

**¿Por qué es prioritario?**
- Procesa eventos asíncronamente
- Distribuye notificaciones por múltiples canales
- Punto crítico de la arquitectura event-driven

---

### 3. **EmailService** (PRIORITARIO ⭐⭐)
**Ubicación:** `services/impl/EmailServiceImpl.java`

**Métodos a probar:**
```java
✅ enviarEmail(String destinatario, String asunto, String cuerpo)
✅ enviarEmailConPlantilla(String destinatario, String plantilla, Map variables)
✅ enviarRecordatorioPago(Factura) - Integración con scheduler
```

**¿Por qué es prioritario?**
- Canal principal de notificaciones
- Usa plantillas Thymeleaf
- Configuración SMTP crítica

---

### 4. **NotificacionWebSocketController** (MEDIO ⭐⭐)
**Ubicación:** `controllers/NotificacionWebSocketController.java`

**Endpoints WebSocket a probar:**
```java
✅ /app/notificaciones/broadcast → /topic/notificaciones
✅ /app/notificaciones/marcar-leida → /user/queue/notificaciones/leida
✅ /app/notificaciones/no-leidas → /user/queue/notificaciones/lista
✅ enviarNotificacionAUsuario(Integer, NotificacionDTO)
✅ notificarContadorNoLeidas(Integer, Long)
```

**¿Por qué es medio?**
- Requiere configuración de WebSocket test
- Menos crítico que lógica de negocio
- UI funciona sin WebSocket (polling fallback)

---

### 5. **NotificacionEvent + FacturaService** (MEDIO ⭐⭐)
**Ubicación:** 
- `events/NotificacionEvent.java`
- `services/impl/FacturaServiceImpl.java`
- `schedulers/RecordatorioPagoScheduler.java`

**Flujos de eventos a probar:**
```java
✅ Crear factura → Publicar evento FACTURA_CREADA
✅ Scheduler detecta vencida → Publicar FACTURA_VENCIDA
✅ Scheduler detecta próxima vencer → Publicar FACTURA_PROXIMA_VENCER
✅ Listener captura eventos → Envía notificaciones
```

**¿Por qué es medio?**
- Integración entre múltiples componentes
- Scheduler requiere configuración especial
- Eventos asíncronos dificultan testing

---

### 6. **PreferenciaNotificacionService** (BAJO ⭐)
**Ubicación:** `services/impl/PreferenciaNotificacionServiceImpl.java`

**Métodos a probar:**
```java
✅ findByUsuarioAndTipo(Integer, TipoNotificacion)
✅ estaActivo(Integer, TipoNotificacion, CanalNotificacion)
✅ actualizarPreferencia(Integer, TipoNotificacion, boolean, boolean, boolean)
✅ crearPreferenciasPorDefecto(Integer)
```

**¿Por qué es bajo?**
- CRUD simple
- Sin lógica de negocio compleja
- Ya probado manualmente

---

## 🔄 PROCESO DE PRUEBAS (Orden Recomendado)

### **FASE 1: Unit Tests (Aislados)** ⏱️ 2-3 horas

```
1. NotificacionServiceTest
   ├─ Test de creación de notificación
   ├─ Test de marcar como leída
   ├─ Test de contador no leídas
   └─ Test con usuario null (validación)

2. PreferenciaNotificacionServiceTest
   ├─ Test de creación por defecto
   ├─ Test de consulta por usuario/tipo
   └─ Test de actualización

3. NotificacionEventTest
   ├─ Test de Builder pattern
   ├─ Test de getDatoRelacionado()
   └─ Test de validación de campos
```

**Herramientas:**
- JUnit 5
- Mockito (para mock de repositories)
- AssertJ (assertions fluidas)

---

### **FASE 2: Integration Tests (Con BD)** ⏱️ 2-3 horas

```
4. NotificacionServiceIntegrationTest
   ├─ Test completo con BD H2
   ├─ Test de persistencia
   ├─ Test de consultas paginadas
   └─ Test de transacciones

5. EmailServiceIntegrationTest
   ├─ Test con MockMail (GreenMail)
   ├─ Test de plantillas Thymeleaf
   └─ Test de adjuntos
```

**Herramientas:**
- @SpringBootTest
- H2 Database (en memoria)
- GreenMail (SMTP mock)
- @Transactional

---

### **FASE 3: Event & Listener Tests** ⏱️ 1-2 horas

```
6. NotificacionListenerTest
   ├─ Test de procesamiento asíncrono (@Async)
   ├─ Test de resolución de usuario
   ├─ Test de determinación de canales
   └─ Test de manejo de errores

7. FacturaEventIntegrationTest
   ├─ Test: Crear factura → Evento publicado
   ├─ Test: Listener captura evento
   └─ Test: Notificación enviada
```

**Herramientas:**
- @EnableAsync
- ApplicationEventPublisher (mock)
- Awaitility (esperar eventos async)

---

### **FASE 4: WebSocket Tests** ⏱️ 1-2 horas

```
8. NotificacionWebSocketTest
   ├─ Test de conexión WebSocket
   ├─ Test de envío a usuario específico
   ├─ Test de broadcast a /topic
   └─ Test de actualización de contador
```

**Herramientas:**
- Spring WebSocket Test
- StompSession (cliente test)
- @WebSocketTest (si disponible)

---

### **FASE 5: E2E Tests (Flujo Completo)** ⏱️ 2-3 horas

```
9. NotificacionE2ETest
   └─ Flujo completo:
      1. Crear factura
      2. Evento publicado
      3. Listener procesa
      4. Notificación guardada en BD
      5. Email enviado (mock)
      6. WebSocket notifica (mock)
      7. Usuario recibe en frontend

10. SchedulerE2ETest
    └─ Flujo scheduler:
       1. Crear factura vencida (fecha pasada)
       2. Ejecutar scheduler manualmente
       3. Evento FACTURA_VENCIDA publicado
       4. Notificación enviada
       5. Verificar en BD
```

**Herramientas:**
- @SpringBootTest(webEnvironment = RANDOM_PORT)
- TestRestTemplate
- Selenium (opcional, para frontend)

---

## 📝 CASOS DE PRUEBA POR MÓDULO

### **1. NotificacionServiceTest**

```java
@Test
void deberiaCrearNotificacionExitosamente()
    // GIVEN: Usuario válido y datos de notificación
    // WHEN: Llamar enviarNotificacion()
    // THEN: Notificación guardada en BD, retorna DTO

@Test
void deberiaMarcarComoLeida()
    // GIVEN: Notificación no leída existente
    // WHEN: Llamar marcarComoLeida(id)
    // THEN: Campo 'leida' = true, fechaLeida != null

@Test
void deberiaContarNoLeidas()
    // GIVEN: 3 notificaciones no leídas para usuario
    // WHEN: Llamar countNoLeidasByUsuarioId()
    // THEN: Retorna 3

@Test
void deberiaMarcarTodasComoLeidas()
    // GIVEN: 5 notificaciones no leídas
    // WHEN: Llamar marcarTodasComoLeidas()
    // THEN: Todas con leida = true

@Test
void deberiaRechazarUsuarioNull()
    // GIVEN: Usuario = null
    // WHEN: Llamar enviarNotificacion()
    // THEN: Lanza IllegalArgumentException

@Test
void deberiaEnviarPorCanalActivo()
    // GIVEN: Preferencia WEB=true, EMAIL=false
    // WHEN: Llamar enviarNotificacion() con WEB
    // THEN: Notificación enviada por WEB solamente

@Test
void deberiaUsarPlantillaCorrectamente()
    // GIVEN: Plantilla con variables {nombre}, {total}
    // WHEN: Llamar enviarNotificacionConPlantilla()
    // THEN: Variables reemplazadas correctamente
```

---

### **2. NotificacionListenerTest**

```java
@Test
void deberiaProcesarEventoAsincronamente()
    // GIVEN: Evento FACTURA_CREADA
    // WHEN: Publicar evento
    // THEN: Listener procesa en thread separado

@Test
void deberiaResolverUsuarioPorId()
    // GIVEN: Evento con idUsuario=1
    // WHEN: Listener resuelve usuario
    // THEN: Usuario cargado desde BD

@Test
void deberiaDeterminarCanalesSegunPreferencias()
    // GIVEN: Preferencias WEB + EMAIL activos
    // WHEN: Determinar canales
    // THEN: Retorna [WEB, EMAIL]

@Test
void deberiaManejarErrorGracefully()
    // GIVEN: Usuario inexistente
    // WHEN: Procesar evento
    // THEN: Log error pero no lanza excepción

@Test
void deberiaProcesarBroadcast()
    // GIVEN: Evento sin usuario específico
    // WHEN: Procesar evento
    // THEN: Enviado solo por canal WEB
```

---

### **3. EmailServiceTest**

```java
@Test
void deberiaEnviarEmailSimple()
    // GIVEN: Destinatario, asunto, cuerpo
    // WHEN: Llamar enviarEmail()
    // THEN: Email enviado (verificar con GreenMail)

@Test
void deberiaUsarPlantillaThymeleaf()
    // GIVEN: Plantilla 'recordatorio-pago.html'
    // WHEN: Enviar con variables
    // THEN: HTML generado correctamente

@Test
void deberiaRechazarEmailInvalido()
    // GIVEN: Email = "invalido"
    // WHEN: Intentar enviar
    // THEN: Lanza MessagingException

@Test
void deberiaEnviarRecordatorioPago()
    // GIVEN: Factura vencida
    // WHEN: Llamar enviarRecordatorioPago()
    // THEN: Email enviado con datos de factura
```

---

### **4. WebSocket Tests**

```java
@Test
void deberiaConectarWebSocket()
    // GIVEN: Cliente WebSocket
    // WHEN: Conectar a /ws-notificaciones
    // THEN: Conexión establecida

@Test
void deberiaEnviarAUsuarioEspecifico()
    // GIVEN: Usuario ID=1 conectado
    // WHEN: Enviar a /user/queue/notificaciones
    // THEN: Solo usuario 1 recibe

@Test
void deberiaBroadcastATodos()
    // GIVEN: 3 usuarios conectados
    // WHEN: Enviar a /topic/notificaciones
    // THEN: Los 3 reciben mensaje

@Test
void deberiaActualizarContador()
    // GIVEN: 5 notificaciones no leídas
    // WHEN: Enviar contador a usuario
    // THEN: Recibe contador=5
```

---

### **5. Event Integration Tests**

```java
@Test
void deberiaPublicarEventoAlCrearFactura()
    // GIVEN: Nueva factura
    // WHEN: Llamar facturaService.save()
    // THEN: Evento FACTURA_CREADA publicado

@Test
void deberiaListenerCapturarEvento()
    // GIVEN: Listener configurado
    // WHEN: Publicar NotificacionEvent
    // THEN: Método procesarNotificacion() llamado

@Test
void deberiaCrearNotificacionDesdeEvento()
    // GIVEN: Evento con datos completos
    // WHEN: Listener procesa
    // THEN: Notificación guardada en BD

@Test
void schedulerDeberiaPublicarEventoVencida()
    // GIVEN: Factura con fechaPago < hoy
    // WHEN: Ejecutar scheduler
    // THEN: Evento FACTURA_VENCIDA publicado

@Test
void schedulerDeberiaPublicarEventoProximaVencer()
    // GIVEN: Factura con fechaPago = hoy + 2 días
    // WHEN: Ejecutar scheduler
    // THEN: Evento FACTURA_PROXIMA_VENCER publicado
```

---

## 🔗 PRUEBAS DE INTEGRACIÓN

### **Escenario 1: Flujo Completo de Factura Nueva**

```
INPUT: POST /api/facturas (crear factura)
       ↓
STEP 1: FacturaService.save()
       ↓
STEP 2: Publica NotificacionEvent(FACTURA_CREADA)
       ↓
STEP 3: NotificacionListener.procesarNotificacion()
       ↓
STEP 4: NotificacionService.enviarNotificacion()
       ↓
STEP 5: Guarda en BD (notificacion table)
       ↓
STEP 6: EmailService.enviarEmail() (si activo)
       ↓
STEP 7: WebSocketController.enviarNotificacionAUsuario()
       ↓
OUTPUT: 
- Notificación en BD ✅
- Email enviado ✅
- WebSocket broadcast ✅
- Cliente recibe en navbar ✅
```

**Validaciones:**
- ✅ Factura guardada con ID
- ✅ Notificación creada con idFactura en datosRelacionados
- ✅ Email recibido en bandeja (GreenMail)
- ✅ Contador de badge actualizado

---

### **Escenario 2: Scheduler de Recordatorios**

```
SETUP: Crear factura vencida (fechaPago = ayer)
       ↓
INPUT: Ejecutar scheduler manualmente
       RecordatorioPagoScheduler.enviarRecordatoriosPago()
       ↓
STEP 1: Busca facturas vencidas en BD
       ↓
STEP 2: Por cada factura:
        - Publica NotificacionEvent(FACTURA_VENCIDA)
       ↓
STEP 3: Listener procesa eventos
       ↓
STEP 4: Envía notificaciones por canales activos
       ↓
OUTPUT:
- Eventos publicados ✅
- Notificaciones en BD ✅
- Emails enviados ✅
```

**Validaciones:**
- ✅ Solo facturas vencidas procesadas
- ✅ Eventos con datos correctos (diasVencida)
- ✅ Notificaciones con tipo FACTURA_VENCIDA
- ✅ Usuario correcto como destinatario

---

### **Escenario 3: Configuración de Preferencias**

```
INPUT: PUT /api/preferencias-notificacion/guardar
       Body: [
         { tipo: "FACTURA_CREADA", 
           activoWeb: true, 
           activoEmail: false, 
           activoWhatsapp: false }
       ]
       ↓
STEP 1: PreferenciaNotificacionService.actualizarPreferencia()
       ↓
STEP 2: Guarda en BD
       ↓
INPUT 2: Crear factura (dispara notificación)
       ↓
STEP 3: NotificacionService verifica preferencias
       ↓
STEP 4: Envía SOLO por canal WEB (email desactivado)
       ↓
OUTPUT:
- Notificación guardada ✅
- Email NO enviado ✅
- WebSocket SI enviado ✅
```

**Validaciones:**
- ✅ Preferencias guardadas correctamente
- ✅ Solo canales activos usados
- ✅ Canales inactivos ignorados

---

## 🎯 FLUJO E2E COMPLETO (Recomendado)

### **Test: Flujo Completo de Notificación**

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class NotificacionE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private NotificacionRepository notificacionRepository;
    
    @Autowired
    private FacturaRepository facturaRepository;
    
    @Test
    void flujCompleto_CrearFactura_NotificarUsuario() {
        // PASO 1: Crear usuario y cliente
        Usuario usuario = crearUsuarioTest();
        Cliente cliente = crearClienteTest(usuario);
        
        // PASO 2: Configurar preferencias (WEB + EMAIL activos)
        configurarPreferencias(usuario, true, true, false);
        
        // PASO 3: Crear factura vía API REST
        Factura facturaRequest = new Factura();
        facturaRequest.setCliente(cliente);
        facturaRequest.setSubtotal(new BigDecimal("1000"));
        
        ResponseEntity<Factura> response = restTemplate
            .postForEntity("/api/facturas", facturaRequest, Factura.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Factura facturaCreada = response.getBody();
        
        // PASO 4: Esperar procesamiento asíncrono
        await().atMost(5, SECONDS).untilAsserted(() -> {
            // Verificar notificación en BD
            List<Notificacion> notificaciones = 
                notificacionRepository.findByUsuario(usuario);
            
            assertThat(notificaciones).hasSize(1);
            
            Notificacion notif = notificaciones.get(0);
            assertThat(notif.getTipo()).isEqualTo(TipoNotificacion.FACTURA_CREADA);
            assertThat(notif.getLeida()).isFalse();
            assertThat(notif.getTitulo()).contains("Nueva Factura");
        });
        
        // PASO 5: Verificar email enviado (si GreenMail configurado)
        // Message[] messages = greenMail.getReceivedMessages();
        // assertThat(messages).hasSize(1);
        
        // PASO 6: Verificar contador vía API
        ResponseEntity<Long> contadorResponse = restTemplate
            .getForEntity("/api/notificaciones/contador-no-leidas", Long.class);
        
        assertThat(contadorResponse.getBody()).isEqualTo(1L);
        
        // PASO 7: Marcar como leída
        restTemplate.put(
            "/api/notificaciones/" + notificaciones.get(0).getIdNotificacion() + "/marcar-leida", 
            null
        );
        
        // PASO 8: Verificar contador = 0
        contadorResponse = restTemplate
            .getForEntity("/api/notificaciones/contador-no-leidas", Long.class);
        
        assertThat(contadorResponse.getBody()).isEqualTo(0L);
    }
}
```

---

## 🛠️ HERRAMIENTAS Y CONFIGURACIÓN

### **1. Dependencias Maven (pom.xml)**

```xml
<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito (incluido en spring-boot-starter-test) -->
<!-- JUnit 5 (incluido en spring-boot-starter-test) -->

<!-- Awaitility (para tests asíncronos) -->
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>

<!-- GreenMail (Mock SMTP para email tests) -->
<dependency>
    <groupId>com.icegreen</groupId>
    <artifactId>greenmail-spring</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>

<!-- H2 Database (para integration tests) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

---

### **2. Configuración de Test (application-test.yml)**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  mail:
    host: localhost
    port: 3025  # Puerto GreenMail
    username: test
    password: test
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false

# Deshabilitar scheduler en tests
scheduler:
  enabled: false

# WebSocket test config
websocket:
  test-mode: true
```

---

### **3. Estructura de Carpetas Test**

```
src/test/java/api/astro/whats_orders_manager/
├── unit/                           # Tests unitarios
│   ├── service/
│   │   ├── NotificacionServiceTest.java
│   │   └── PreferenciaNotificacionServiceTest.java
│   ├── listener/
│   │   └── NotificacionListenerTest.java
│   └── events/
│       └── NotificacionEventTest.java
│
├── integration/                    # Tests de integración
│   ├── NotificacionServiceIntegrationTest.java
│   ├── EmailServiceIntegrationTest.java
│   └── WebSocketIntegrationTest.java
│
├── e2e/                           # Tests end-to-end
│   ├── NotificacionE2ETest.java
│   └── SchedulerE2ETest.java
│
└── config/                        # Configuración de tests
    ├── TestConfig.java
    └── GreenMailConfig.java
```

---

## 📊 PRIORIZACIÓN DE TESTS

### **Críticos (DEBE hacerse)** ⭐⭐⭐
1. ✅ NotificacionServiceTest - Lógica de negocio core
2. ✅ NotificacionListenerTest - Procesamiento de eventos
3. ✅ EmailServiceIntegrationTest - Canal principal
4. ✅ Flujo E2E: Crear Factura → Notificación

### **Importantes (DEBERÍA hacerse)** ⭐⭐
5. ✅ WebSocket Integration Test
6. ✅ Scheduler E2E Test
7. ✅ PreferenciaNotificacionService Test

### **Opcionales (PUEDE hacerse)** ⭐
8. ⏸️ NotificacionEvent Builder Test
9. ⏸️ Performance Tests (carga)
10. ⏸️ Security Tests (autorización)

---

## 🎬 EJECUCIÓN DE TESTS

### **Ejecutar todos los tests**
```bash
mvn test
```

### **Ejecutar solo tests unitarios**
```bash
mvn test -Dtest=*Test
```

### **Ejecutar solo tests de integración**
```bash
mvn test -Dtest=*IntegrationTest
```

### **Ejecutar test específico**
```bash
mvn test -Dtest=NotificacionServiceTest
```

### **Con cobertura (JaCoCo)**
```bash
mvn clean test jacoco:report
# Ver reporte en: target/site/jacoco/index.html
```

---

## 📈 COBERTURA ESPERADA

```
Objetivo de cobertura:
├─ NotificacionService:      > 80%
├─ NotificacionListener:      > 75%
├─ EmailService:              > 70%
├─ PreferenciaService:        > 80%
├─ WebSocketController:       > 60%
└─ Events/Scheduler:          > 50%

Total proyecto:               > 70%
```

---

## ✅ CHECKLIST DE VALIDACIÓN

Antes de considerar las pruebas completas, verificar:

- [ ] Todos los métodos públicos de NotificacionService tienen test
- [ ] Eventos asíncronos probados con Awaitility
- [ ] Tests de email usan GreenMail (no envían emails reales)
- [ ] BD usa H2 en memoria (no afecta BD real)
- [ ] Tests son independientes (no dependen de orden)
- [ ] Tests limpian datos después de ejecutar (@Transactional)
- [ ] Cobertura > 70% en servicios críticos
- [ ] Al menos 1 test E2E completo funciona
- [ ] Tests pasan en CI/CD (si aplica)
- [ ] Documentación de tests actualizada

---

## 🚀 PRÓXIMOS PASOS

1. **Empezar con NotificacionServiceTest** (Unit)
2. **Continuar con NotificacionListenerTest** (Unit)
3. **EmailServiceIntegrationTest** (Integration)
4. **NotificacionE2ETest** (E2E)
5. **WebSocket tests** (si hay tiempo)
6. **Scheduler tests** (si hay tiempo)

---

**Tiempo estimado total:** 10-15 horas  
**Archivos a crear:** 6-10 clases de test  
**Líneas de código:** ~1,500-2,000 líneas

---

**Autor:** EmaSleal  
**Fecha:** Sprint 4 - Fase 3.8  
**Última actualización:** 2 dic 2025
