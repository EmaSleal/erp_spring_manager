## 🔄 FLUJO DE FUNCIONAMIENTO

### 1. Usuario Inicia Sesión
```
Usuario → POST /auth/login
↓
Spring Security → AuthenticationManager
↓
UserDetailsServiceImpl.loadUserByUsername(telefono)
```

### 2. Autenticación
```
loadUserByUsername(telefono)
↓
usuarioRepository.findByTelefono(telefono)
↓
Usuario encontrado
↓
Verificar usuario.activo == true
```

### 3. Actualización de Último Acceso
```
actualizarUltimoAcceso(usuario)
↓
Timestamp now = new Timestamp(System.currentTimeMillis())
↓
usuario.setUltimoAcceso(now)
↓
usuarioRepository.save(usuario)
↓
log.info("Último acceso actualizado para usuario: ...")
```

### 4. Base de Datos
```sql
UPDATE usuario 
SET ultimo_acceso = '2025-10-20 11:37:45.123' 
WHERE id_usuario = 1;
```

### 5. Vista de Usuarios
```
GET /usuarios
↓
UsuarioController → List<Usuario> usuarios
↓
Template Thymeleaf → usuarios.html
↓
Renderizar tabla con usuario.ultimoAcceso
↓
Formato: ${#temporals.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')}
```

---

