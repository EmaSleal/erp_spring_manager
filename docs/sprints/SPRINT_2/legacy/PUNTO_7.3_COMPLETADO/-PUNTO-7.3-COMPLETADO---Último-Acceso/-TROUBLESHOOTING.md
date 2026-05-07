## 🐛 TROUBLESHOOTING

### Problema 1: Último acceso no se actualiza
**Causa:** `actualizarUltimoAcceso()` no se está ejecutando  
**Solución:** 
1. Verificar que `loadUserByUsername()` llama a `actualizarUltimoAcceso()`
2. Verificar logs para ver si hay errores
3. Verificar que el repositorio tiene permisos de escritura

### Problema 2: Muestra "Nunca" aunque el usuario ya inició sesión
**Causa:** `ultimo_acceso` es null en base de datos  
**Solución:**
1. Verificar que el UPDATE se ejecutó: `SELECT ultimo_acceso FROM usuario WHERE id_usuario = 1`
2. Verificar logs de `actualizarUltimoAcceso()`
3. Verificar que no hay transacción rollback

### Problema 3: Formato de fecha incorrecto
**Causa:** Thymeleaf `#temporals` no reconoce el tipo `Timestamp`  
**Solución:**
- ✅ Ya implementado: `${#temporals.format(usuario.ultimoAcceso, 'dd/MM/yyyy HH:mm')}`
- El tipo `java.sql.Timestamp` es compatible con Thymeleaf Temporals

### Problema 4: Error "Usuario no encontrado"
**Causa:** Cambio de `findByNombre()` a `findByTelefono()`  
**Solución:**
- ✅ Ya corregido en este punto
- Ahora busca correctamente por teléfono (username del sistema)

---

