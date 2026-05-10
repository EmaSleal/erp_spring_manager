## 🔧 CONFIGURACIÓN NECESARIA

### 1. Campo Avatar en Usuario
El modelo `Usuario` debe tener un campo `avatar`:
```java
@Entity
public class Usuario {
    // ... otros campos
    
    private String avatar; // URL del avatar: "/uploads/avatars/usuario.jpg"
    
    // getters y setters
}
```

### 2. Upload de Avatares (Opcional - Próxima fase)
Para subir avatares, se puede implementar:
```java
@PostMapping("/perfil/avatar/subir")
public String subirAvatar(@RequestParam("file") MultipartFile file) {
    String filename = fileStorageService.storeFile(file);
    Usuario usuario = getCurrentUser();
    usuario.setAvatar("/uploads/avatars/" + filename);
    usuarioService.save(usuario);
    return "redirect:/perfil";
}
```

### 3. Directorio de Almacenamiento
Configurar en `application.yml`:
```yaml
file:
  upload-dir: uploads/avatars
```

---

