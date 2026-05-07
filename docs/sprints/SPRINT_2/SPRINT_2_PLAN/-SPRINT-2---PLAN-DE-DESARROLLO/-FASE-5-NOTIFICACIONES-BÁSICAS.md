## 📦 FASE 5: NOTIFICACIONES BÁSICAS

### Objetivo
Implementar sistema de notificaciones por email.

### Tareas

#### 5.1 Configuración de Email
**Archivo:** `application.yml`

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### 5.2 Servicio de Email
**Archivo:** `EmailService.java`

```java
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void enviarFacturaPorEmail(Factura factura, String emailDestino) {
        // Implementación
    }
    
    public void enviarCredencialesUsuario(Usuario usuario, String password) {
        // Implementación
    }
    
    public void enviarRecordatorioPago(Factura factura) {
        // Implementación
    }
}
```

#### 5.3 Tipos de Notificaciones
1. **Nueva factura creada** → Enviar PDF por email al cliente
2. **Usuario creado** → Enviar credenciales por email
3. **Recordatorio de pago** → Email automático para facturas pendientes
4. **Cambio de contraseña** → Notificar al usuario

#### 5.4 Configuración de Notificaciones
**Vista:** `configuracion/notificaciones.html`

**Opciones:**
- ☑️ Activar notificaciones por email
- ☑️ Enviar factura automáticamente por email al crearla
- ☑️ Enviar recordatorio de pago (cada X días)
- ☑️ Notificar cuando se registra un nuevo cliente
- ☑️ Notificar cuando un usuario cambia su contraseña

---

