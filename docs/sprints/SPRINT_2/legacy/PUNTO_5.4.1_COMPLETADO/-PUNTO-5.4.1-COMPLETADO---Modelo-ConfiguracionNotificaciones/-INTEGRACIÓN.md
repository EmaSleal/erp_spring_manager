## 🔗 INTEGRACIÓN

### Próximas Integraciones

**RecordatorioPagoScheduler:**
```java
@Autowired
private ConfiguracionNotificacionesService configService;

if (configService.debeEnviarRecordatorios()) {
    int dias = configService.getDiasRecordatorioPago();
    // Buscar facturas vencidas...
}
```

**FacturaController (envío automático):**
```java
@PostMapping("/save")
public String guardarFactura(@ModelAttribute Factura factura) {
    Factura guardada = facturaService.save(factura);
    
    if (configService.debeEnviarFacturaAutomatica()) {
        emailService.enviarFacturaPorEmail(guardada);
    }
    
    return "redirect:/facturas";
}
```

**EmailServiceImpl (BCC):**
```java
String emailBCC = configuracion.getEmailCopiaFacturasOrNull();
if (emailBCC != null) {
    helper.setBcc(emailBCC);
}
```

---

