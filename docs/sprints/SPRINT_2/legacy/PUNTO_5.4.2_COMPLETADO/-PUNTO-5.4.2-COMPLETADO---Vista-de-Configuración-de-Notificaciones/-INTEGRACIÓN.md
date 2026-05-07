## 🔗 INTEGRACIÓN

### Con ConfiguracionNotificacionesService
```java
ConfiguracionNotificaciones config = 
    configuracionNotificacionesService.getOrCreateConfiguracion();
```

### Con EmailService
```java
boolean enviado = emailService.enviarEmailPrueba(emailDestino);
```

### Con RecordatorioPagoScheduler
```java
recordatorioPagoScheduler.ejecutarManualmente();
```

### CSRF Protection
```javascript
const csrfToken = /*[[${_csrf.token}]]*/ '';
const csrfHeader = /*[[${_csrf.headerName}]]*/ '';
fetch(url, {
    method: 'POST',
    headers: { [csrfHeader]: csrfToken }
});
```

---

