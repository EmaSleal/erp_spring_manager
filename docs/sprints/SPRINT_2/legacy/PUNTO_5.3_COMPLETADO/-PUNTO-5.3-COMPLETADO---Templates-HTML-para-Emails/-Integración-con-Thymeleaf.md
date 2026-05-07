## 🔧 Integración con Thymeleaf

### Configuración Necesaria

Los templates usan Thymeleaf con el namespace:
```html
xmlns:th="http://www.thymeleaf.org"
```

### Uso desde el Servicio

**Ejemplo de uso:**

```java
@Autowired
private TemplateEngine templateEngine;

public void enviarFacturaPorEmail(Factura factura, Empresa empresa) {
    // Preparar el contexto
    Context context = new Context();
    context.setVariable("factura", factura);
    context.setVariable("empresa", empresa);
    
    // Procesar el template
    String htmlContent = templateEngine.process("email/factura", context);
    
    // Enviar el email
    emailService.enviarEmailHtml(
        factura.getCliente().getEmail(),
        "Factura #" + factura.getNumero(),
        htmlContent
    );
}
```

### Variables Requeridas por Template

**factura.html:**
- `factura` (Objeto Factura completo)
- `empresa` (Objeto Empresa completo)

**credenciales-usuario.html:**
- `usuario` (Objeto Usuario)
- `contrasena` (String - contraseña temporal)
- `urlLogin` (String - URL del sistema)
- `empresaEmail` (String)
- `empresaTelefono` (String - opcional)

**recordatorio-pago.html:**
- `cliente` (Objeto Cliente)
- `factura` (Objeto Factura)
- `diasVencidos` (Integer - 0 si no está vencida)
- `urlFactura` (String - URL para ver factura)
- `empresa` (Objeto Empresa completo con datos bancarios)

---

