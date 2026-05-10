## 📊 VALIDACIONES

### A Nivel de Modelo (JPA)
```java
@NotNull(message = "El campo activar email es requerido")
@Min(value = 0, message = "Los días no pueden ser negativos")
@Min(value = 1, message = "La frecuencia debe ser al menos 1 día")
```

### A Nivel de Base de Datos
```sql
activar_email BOOLEAN NOT NULL
dias_recordatorio_preventivo INT DEFAULT 3
dias_recordatorio_pago INT DEFAULT 0
frecuencia_recordatorios INT DEFAULT 7
activo BOOLEAN NOT NULL
```

### A Nivel de Servicio
- Solo un registro puede estar activo
- Al activar uno, los demás se desactivan automáticamente
- Si no existe configuración, se crea con valores por defecto

---

