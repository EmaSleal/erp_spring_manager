## 📚 Lecciones Aprendidas

### 1. Consistencia en Tipos de Auditoría

**Regla:** Todas las entidades deben usar el mismo tipo para campos de auditoría que el configurado en `AuditorAware<T>`.

```java
// Si AuditorAware<Integer>
public class AuditorAwareImpl implements AuditorAware<Integer> { }

// Entonces TODAS las entidades deben tener:
@CreatedBy
private Integer createBy;

@LastModifiedBy
private Integer updateBy;
```

### 2. Validar Nuevas Entidades

Al crear una nueva entidad con auditoría:

✅ **Checklist:**
- [ ] Campos de auditoría del mismo tipo que `AuditorAware<T>`
- [ ] Anotación `@EntityListeners(AuditingEntityListener.class)`
- [ ] Columnas en base de datos del tipo correcto (INT si AuditorAware<Integer>)
- [ ] Foreign keys a tabla usuario (opcional pero recomendado)

### 3. Valores NULL para Registros del Sistema

Es **aceptable** que `createBy` sea NULL para registros creados por el sistema:

```java
// En lugar de:
nuevaConfig.setCreateBy("SYSTEM"); // ❌ Incorrecto si AuditorAware<Integer>

// Mejor:
// No setear nada, dejar que Spring Data JPA intente obtenerlo del AuditorAware
// Si no hay usuario autenticado, será NULL automáticamente
```

### 4. Error de Commit vs Error de Validación

Este error ocurre en el **commit de la transacción**, no al validar:
- No se detecta en compilación
- No se detecta al ejecutar el método
- Solo falla cuando JPA intenta persistir los cambios

Por eso es importante:
- Probar guardado de entidades después de crearlas
- Verificar logs de transacciones
- Revisar stacktraces completos

---

