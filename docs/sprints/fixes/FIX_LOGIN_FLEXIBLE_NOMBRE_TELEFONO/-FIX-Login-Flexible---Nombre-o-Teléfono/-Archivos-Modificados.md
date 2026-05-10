## 📁 Archivos Modificados

### UserDetailsServiceImpl.java

**Ubicación:** `src/main/java/api/astro/whats_orders_manager/services/impl/UserDetailsServiceImpl.java`

**Líneas modificadas:** 28-46 (método `loadUserByUsername`)

**Cambio:**
```diff
- Usuario usuario = usuarioRepository.findByTelefono(telefono)
+ Usuario usuario = usuarioRepository.findByTelefono(usernameOrPhone)
+         .or(() -> usuarioRepository.findByNombre(usernameOrPhone))
```

---

