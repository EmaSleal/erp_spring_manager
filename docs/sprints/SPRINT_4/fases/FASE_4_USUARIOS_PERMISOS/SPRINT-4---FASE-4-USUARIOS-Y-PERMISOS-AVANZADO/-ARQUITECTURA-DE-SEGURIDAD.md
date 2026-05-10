## 🏗️ ARQUITECTURA DE SEGURIDAD

### Flujo de Autenticación y Autorización

```
┌──────────────────────────────────────────────────────────────────┐
│                        USUARIO INICIA SESIÓN                      │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                    SPRING SECURITY FILTER CHAIN                   │
├──────────────────────────────────────────────────────────────────┤
│  1. UsernamePasswordAuthenticationFilter                         │
│     - Captura credenciales (email/password)                      │
│                                                                   │
│  2. CustomUserDetailsService                                     │
│     - Carga usuario desde BD                                     │
│     - Valida estado (activo/bloqueado)                           │
│     - Carga rol y permisos                                       │
│                                                                   │
│  3. PasswordEncoder (BCrypt)                                     │
│     - Valida contraseña hasheada                                 │
│                                                                   │
│  4. AuthenticationManager                                        │
│     - Autentica usuario                                          │
│     - Crea Authentication object                                 │
│                                                                   │
│  5. SecurityContextHolder                                        │
│     - Almacena Authentication en sesión                          │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                   USUARIO ACCEDE A UN RECURSO                     │
├──────────────────────────────────────────────────────────────────┤
│  Ejemplo: GET /admin/usuarios/gestionar                          │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                      VALIDACIÓN DE PERMISOS                       │
├──────────────────────────────────────────────────────────────────┤
│  @PreAuthorize("hasAuthority('USUARIOS_VER')")                   │
│                                                                   │
│  1. Spring Security intercepta la petición                       │
│  2. Obtiene authorities del usuario autenticado                  │
│  3. Verifica si tiene permiso 'USUARIOS_VER'                     │
│  4. Si SÍ → Permite acceso                                       │
│  5. Si NO → Lanza AccessDeniedException (403 Forbidden)          │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                         EJECUCIÓN DEL MÉTODO                      │
├──────────────────────────────────────────────────────────────────┤
│  UsuarioController.gestionar()                                   │
│    └─→ UsuarioService.listarTodos()                             │
│          └─→ UsuarioRepository.findAll()                        │
│                └─→ Retorna List<Usuario>                        │
└──────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────┐
│                       AUDITORÍA AUTOMÁTICA                        │
├──────────────────────────────────────────────────────────────────┤
│  @EntityListeners(AuditingEntityListener.class)                  │
│                                                                   │
│  - @CreatedBy: Registra quién creó el registro                  │
│  - @CreatedDate: Registra cuándo se creó                        │
│  - @LastModifiedBy: Registra última modificación por            │
│  - @LastModifiedDate: Registra cuándo se modificó               │
└──────────────────────────────────────────────────────────────────┘
```

---

