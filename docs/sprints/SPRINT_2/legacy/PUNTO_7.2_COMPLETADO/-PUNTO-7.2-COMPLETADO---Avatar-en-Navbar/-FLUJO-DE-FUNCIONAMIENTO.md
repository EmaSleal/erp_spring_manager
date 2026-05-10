## 🔄 FLUJO DE FUNCIONAMIENTO

### 1. Request del Usuario
```
Usuario → GET /dashboard → Spring Security → Controller
```

### 2. GlobalControllerAdvice Intercepta
```
@ControllerAdvice
↓
@ModelAttribute addGlobalAttributes()
↓
Authentication → UserDetails → Usuario.telefono
↓
UsuarioService.findByTelefono(telefono)
↓
Usuario completo recuperado
```

### 3. Datos Agregados al Modelo
```
Model:
├── userName: "Juan Pérez"
├── userRole: "ADMIN"
├── userInitials: "JP" (calculado)
├── userAvatar: "/uploads/avatars/juan.jpg" (o null)
└── usuarioActual: Usuario {...}

Session:
├── userName: "Juan Pérez"
├── userRole: "ADMIN"
├── userInitials: "JP"
└── userAvatar: "/uploads/avatars/juan.jpg"
```

### 4. Vista Renderiza
```
Thymeleaf:
├── th:if="${userAvatar != null and userAvatar != ''}"
│   → Renderiza <img> con avatar
└── th:if="${userAvatar == null or userAvatar == ''}"
    → Renderiza <span> con iniciales
```

### 5. CSS Aplica Estilos
```
.user-avatar.has-image
├── background-color: transparent
└── <img> circular con object-fit: cover

.user-avatar.has-initials
├── background: linear-gradient(...)
└── <span> con iniciales en mayúsculas
```

---

