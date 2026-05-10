## 🔍 Causa Raíz

### Inconsistencia de Tipos

El problema se debía a una **inconsistencia entre el tipo que retorna `AuditorAwareImpl` y el tipo de los campos de auditoría**:

**1. AuditorAwareImpl configurado para retornar Integer:**

```java
@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<Integer> {
    
    @Override
    public Optional<Integer> getCurrentAuditor() {
        // Retorna el ID del usuario (Integer)
        return usuarioRepository.findByTelefonoWithoutFlush(telefono)
                .map(Usuario::getIdUsuario); // getIdUsuario() retorna Integer
    }
}
```

**2. ConfiguracionNotificaciones con campos de auditoría String:**

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class ConfiguracionNotificaciones {
    
    @CreatedBy
    @Column(name = "create_by", updatable = false)
    private String createBy; // ❌ INCORRECTO: Esperaba Integer
    
    @LastModifiedBy
    @Column(name = "update_by")
    private String updateBy; // ❌ INCORRECTO: Esperaba Integer
}
```

**3. Base de datos con VARCHAR:**

```sql
CREATE TABLE configuracion_notificaciones (
    create_by VARCHAR(50),  -- ❌ INCORRECTO: Debía ser INT
    update_by VARCHAR(50)   -- ❌ INCORRECTO: Debía ser INT
);
```

### Por Qué Ocurrió

1. **Todas las demás entidades** del sistema tienen campos de auditoría como `Integer`
2. **ConfiguracionNotificaciones** se creó recientemente con campos `String` por error
3. Spring Data JPA no puede convertir automáticamente `Integer` a `String`
4. El error no se detectó hasta intentar guardar (la transacción falla en el commit)

---

