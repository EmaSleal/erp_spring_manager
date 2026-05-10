## 🔧 ARCHIVOS MODIFICADOS

### 1. UsuarioRepository.java
**Cambio:** Corregida duplicación de package  
**Líneas Agregadas:** 6 queries  

**Antes:**
```java
package api.astro.whats_orders_manager.repositories;
// ... imports ...
package api.astro.whats_orders_manager.repositories; // ❌ DUPLICADO
```

**Después:**
```java
package api.astro.whats_orders_manager.repositories;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    List<Usuario> findByRol(String rol);
    List<Usuario> findByActivo(Boolean activo);
    List<Usuario> findByBloqueado(Boolean bloqueado);
    long countByActivo(Boolean activo);
    long countByBloqueado(Boolean bloqueado);
    long countByRol(String rol);
}
```

---

### 2. UsuarioService.java
**Cambio:** Corregida duplicación de package, removido @Service  
**Métodos Totales:** 20

**Antes:**
```java
@Service // ❌ Las interfaces no llevan @Service
public interface UsuarioService {
```

**Después:**
```java
public interface UsuarioService {
    // CRUD Básico
    Page<Usuario> findAll(Pageable pageable);
    
    // Gestión Admin
    Usuario bloquearUsuario(Integer idUsuario, String razon, Integer adminId);
    Usuario desbloquearUsuario(Integer idUsuario);
    Usuario cambiarRol(Integer idUsuario, String nuevoRol);
    
    // Seguridad
    void incrementarIntentosFallidos(Integer idUsuario);
    void resetearIntentosFallidos(Integer idUsuario);
    void forzarCambioPassword(Integer idUsuario);
    
    // Estadísticas
    long count();
    long countByActivo(Boolean activo);
    long countByBloqueado(Boolean bloqueado);
    long countByRol(String rol);
}
```

---

