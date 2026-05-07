## 📝 LOGS GENERADOS

### Log de Login Exitoso
```
2025-10-20 11:37:45.123 INFO  --- [UserDetailsServiceImpl] : Último acceso actualizado para usuario: Juan Pérez (ID: 1) - Timestamp: 2025-10-20 11:37:45.123
```

### Log de Error (Base de Datos no disponible)
```
2025-10-20 11:37:45.123 ERROR --- [UserDetailsServiceImpl] : Error al actualizar último acceso para usuario Juan Pérez (ID: 1): Connection refused
org.hibernate.exception.JDBCConnectionException: Unable to acquire JDBC Connection
    at org.hibernate.exception.internal.SQLStateConversionDelegate.convert(SQLStateConversionDelegate.java:98)
    ...
```

---

