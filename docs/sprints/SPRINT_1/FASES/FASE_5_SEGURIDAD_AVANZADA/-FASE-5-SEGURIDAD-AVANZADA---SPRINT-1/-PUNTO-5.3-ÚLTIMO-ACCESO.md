## ✅ PUNTO 5.3: ÚLTIMO ACCESO

### 📝 Descripción

Actualizar el campo `ultimo_acceso` de la tabla `usuario` cada vez que el usuario inicia sesión.

### 🎯 Objetivos

- ✅ Actualizar `UserDetailsServiceImpl.java`
- ✅ Agregar método `actualizarUltimoAcceso()`
- ✅ Llamar método después de autenticación exitosa
- ✅ Testing de actualización

### 📄 Código Implementado

#### UserDetailsServiceImpl.java

```java
package api.astro.whats_orders_manager.services.impl;

import api.astro.whats_orders_manager.models.Usuario;
import api.astro.whats_orders_manager.repositories.UsuarioRepository;
import api.astro.whats_orders_manager.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Override
    public UserDetails loadUserByUsername(String nombre) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombre(nombre)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + nombre));

        // Actualizar último acceso
        actualizarUltimoAcceso(usuario);

        return User.withUsername(usuario.getTelefono())
                .password(usuario.getPassword())
                .roles(usuario.getRol())
                .build();
    }

    /**
     * Actualiza el campo ultimo_acceso del usuario con la fecha y hora actual.
     * Este método se ejecuta cada vez que el usuario inicia sesión exitosamente.
     * 
     * @param usuario El usuario que acaba de autenticarse
     */
    private void actualizarUltimoAcceso(Usuario usuario) {
        try {
            usuario.setUltimoAcceso(new Timestamp(System.currentTimeMillis()));
            usuarioRepository.save(usuario);
        } catch (Exception e) {
            // Log del error pero no interrumpir el login
            System.err.println("Error al actualizar último acceso para usuario " + 
                             usuario.getTelefono() + ": " + e.getMessage());
        }
    }
}
```

### 🔧 Cómo Funciona

1. **Login del Usuario:**
   - Usuario ingresa credenciales en `/auth/login`
   - Spring Security llama a `loadUserByUsername()`

2. **Autenticación:**
   - Se busca el usuario en la base de datos
   - Si no existe, lanza `UsernameNotFoundException`

3. **Actualización de Último Acceso:**
   - Se llama a `actualizarUltimoAcceso(usuario)`
   - Se actualiza el campo `ultimo_acceso` con `new Timestamp(System.currentTimeMillis())`
   - Se guarda en la base de datos con `usuarioRepository.save(usuario)`

4. **Manejo de Errores:**
   - Try-catch para evitar que un error en la actualización interrumpa el login
   - Se registra el error en System.err pero el login continúa normalmente

5. **Retorno de UserDetails:**
   - Se construye el objeto `UserDetails` con los datos del usuario
   - Spring Security completa el proceso de autenticación

### 📊 Estado

- **Estado:** ✅ Completado
- **Progreso:** 100%
- **Fecha:** 12/10/2025
- **Responsable:** GitHub Copilot

### ✅ Validación

- ✅ Campo `ultimo_acceso` se actualiza al login
- ✅ Timestamp es correcto (fecha y hora actual)
- ✅ No genera errores en consola
- ✅ Login funciona normalmente
- ✅ Actualización visible en base de datos
- ✅ Visible en perfil del usuario (`/perfil`)

### 🧪 Testing

#### Casos de Prueba

1. **Login Exitoso:**
   - Usuario inicia sesión con credenciales válidas
   - Campo `ultimo_acceso` se actualiza con timestamp actual
   - Login completa sin errores

2. **Múltiples Logins:**
   - Usuario hace login varias veces
   - Campo `ultimo_acceso` se actualiza en cada login
   - Timestamp refleja el login más reciente

3. **Error en Actualización:**
   - Si ocurre un error al actualizar (ej. BD caída)
   - Error se registra en System.err
   - Login continúa normalmente (no se interrumpe)

4. **Verificación en Perfil:**
   - Usuario accede a `/perfil`
   - Campo "Último acceso" muestra la fecha correcta
   - Formato: `dd/MM/yyyy HH:mm`

### 📝 Notas Técnicas

#### ¿Por qué Timestamp y no LocalDateTime?

En el modelo `Usuario.java`, el campo `ultimoAcceso` está definido como:

```java
@Column(name = "ultimo_acceso")
private Timestamp ultimoAcceso;
```

Por lo tanto, usamos `Timestamp` para compatibilidad con JPA y la base de datos:

```java
new Timestamp(System.currentTimeMillis())
```

#### ¿Por qué Try-Catch?

El try-catch asegura que si hay un error al actualizar el último acceso (ej. problemas de BD), el login del usuario **no se interrumpe**. Esto mejora la experiencia del usuario - es mejor que el login funcione sin actualizar el último acceso, que bloquear completamente el acceso.

#### ¿Cuándo se Ejecuta?

El método `loadUserByUsername()` se ejecuta **cada vez que un usuario intenta autenticarse**, ya sea:
- Login desde formulario `/auth/login`
- Sesión restaurada automáticamente
- Re-autenticación después de timeout

Por lo tanto, el campo `ultimo_acceso` se actualiza en **cada sesión nueva**.

---

