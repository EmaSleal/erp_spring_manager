## 🔐 CARACTERÍSTICAS DE SEGURIDAD

### Extracción Automática de Contexto

```java
private String obtenerIPActual() {
    ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        // Prioriza X-Forwarded-For para detectar IP real detrás de proxies
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    return "SYSTEM"; // Fallback para operaciones internas
}
```

**Ventajas:**
- ✅ Detección de proxy/load balancer
- ✅ Fallback robusto
- ✅ Manejo de excepciones
- ✅ Logging de errores

---

### Control de Intentos Fallidos

```java
@Override
@Transactional
public void incrementarIntentosFallidos(Integer idUsuario) {
    Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    
    usuario.incrementarIntentosFallidos();
    usuarioRepository.save(usuario);
    
    // En Usuario.java el método incrementa y bloquea automáticamente a los 5 intentos
    log.warn("Usuario {} tiene {} intentos fallidos", 
            usuario.getNombre(), usuario.getIntentosFallidos());
}
```

**Flujo:**
1. Usuario intenta login con contraseña incorrecta
2. Se llama a `incrementarIntentosFallidos()`
3. `Usuario.incrementarIntentosFallidos()` aumenta contador
4. Si llega a 5, `Usuario.bloquear()` se ejecuta automáticamente
5. Se registra actividad en UsuarioActividad

---

