## 🔧 COMANDOS DE TESTING

### Ejecutar Tests Unitarios
```bash
mvn test -Dtest=NotificacionServiceTest
```

### Ejecutar Todos los Tests
```bash
mvn test
```

### Generar Reporte de Cobertura
```bash
mvn jacoco:report
# Ver en: target/site/jacoco/index.html
```

### Tests con Perfil de Desarrollo
```bash
mvn test -Dspring.profiles.active=dev
```

---

