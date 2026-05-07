## 🎬 EJECUCIÓN DE TESTS

### **Ejecutar todos los tests**
```bash
mvn test
```

### **Ejecutar solo tests unitarios**
```bash
mvn test -Dtest=*Test
```

### **Ejecutar solo tests de integración**
```bash
mvn test -Dtest=*IntegrationTest
```

### **Ejecutar test específico**
```bash
mvn test -Dtest=NotificacionServiceTest
```

### **Con cobertura (JaCoCo)**
```bash
mvn clean test jacoco:report
# Ver reporte en: target/site/jacoco/index.html
```

---

