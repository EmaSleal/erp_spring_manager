## 📝 Código de Ejemplo de Uso

### 1. Email Simple
```java
@Autowired
private EmailService emailService;

public void enviarNotificacionSimple() {
    emailService.enviarEmail(
        "cliente@empresa.com",
        "Pedido Confirmado",
        "Su pedido #123 ha sido confirmado."
    );
}
```

### 2. Email HTML
```java
public void enviarFactura() {
    String html = """
        <h1>Factura #001</h1>
        <p>Total: $1,500.00</p>
        """;
    
    emailService.enviarEmailHtml(
        "cliente@empresa.com",
        "Factura de Venta",
        html
    );
}
```

### 3. Email con Adjunto
```java
public void enviarFacturaConPDF() {
    byte[] pdfBytes = generarPDFFactura();
    
    emailService.enviarEmailHtmlConAdjunto(
        "cliente@empresa.com",
        "Factura de Venta",
        "<h1>Ver factura adjunta</h1>",
        pdfBytes,
        "factura-001.pdf"
    );
}
```

### 4. Email de Prueba
```java
public void probarConfiguracion() {
    boolean exitoso = emailService.enviarEmailPrueba("admin@empresa.com");
    
    if (exitoso) {
        System.out.println("✅ Email configurado correctamente");
    } else {
        System.out.println("❌ Error en configuración de email");
    }
}
```

---

