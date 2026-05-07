## 🚀 Problema Resuelto

### Error Inicial
```
java.lang.ClassNotFoundException: jakarta.mail.MessagingException
```

### Causa
Maven no había descargado las dependencias transitivas de `spring-boot-starter-mail`.

### Solución
```bash
mvn clean install -U
```

El flag `-U` fuerza la actualización de todas las dependencias desde los repositorios remotos.

---

