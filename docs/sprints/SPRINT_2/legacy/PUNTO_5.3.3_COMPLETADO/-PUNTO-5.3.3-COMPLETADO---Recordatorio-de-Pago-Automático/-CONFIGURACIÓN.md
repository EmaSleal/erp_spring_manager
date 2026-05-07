## 🔧 CONFIGURACIÓN

### Cron Expression
```java
@Scheduled(cron = "0 0 9 * * *")
```
- **Formato:** segundo minuto hora día mes día-semana
- **Actual:** 0 0 9 * * * = Todos los días a las 9:00:00 AM
- **Personalizable:** Cambiar horario editando el cron

### Variables de Contexto (Template)
```java
context.setVariable("empresa", empresa);
context.setVariable("factura", factura);
context.setVariable("cliente", factura.getCliente());
context.setVariable("lineas", lineas);
context.setVariable("diasRetraso", diasRetraso);
context.setVariable("fechaActual", java.time.LocalDate.now());
```

---

