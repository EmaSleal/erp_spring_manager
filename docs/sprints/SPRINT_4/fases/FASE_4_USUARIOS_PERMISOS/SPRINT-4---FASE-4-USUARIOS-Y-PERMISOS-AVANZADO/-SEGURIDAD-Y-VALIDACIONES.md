## 🔒 SEGURIDAD Y VALIDACIONES

### Anotaciones de Seguridad

```java
// En controladores
@PreAuthorize("hasAuthority('USUARIOS_VER')")
@GetMapping("/gestionar")
public String gestionar(Model model) { ... }

@PreAuthorize("hasAuthority('USUARIOS_CREAR')")
@PostMapping("/crear")
public String crear(@Valid UsuarioDTO dto) { ... }

@PreAuthorize("hasAuthority('USUARIOS_EDITAR')")
@PutMapping("/{id}")
public String actualizar(@PathVariable Long id, @Valid UsuarioDTO dto) { ... }

// En servicios
@PreAuthorize("hasAuthority('USUARIOS_CAMBIAR_ROL')")
public void cambiarRol(Long usuarioId, Rol nuevoRol) { ... }
```

### Validaciones DTO

```java
public class UsuarioDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Teléfono inválido")
    private String telefono;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;
}
```

---

