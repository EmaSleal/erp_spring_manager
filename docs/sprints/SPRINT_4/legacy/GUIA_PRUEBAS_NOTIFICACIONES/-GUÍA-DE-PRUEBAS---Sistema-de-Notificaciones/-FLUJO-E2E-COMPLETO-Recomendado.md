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

