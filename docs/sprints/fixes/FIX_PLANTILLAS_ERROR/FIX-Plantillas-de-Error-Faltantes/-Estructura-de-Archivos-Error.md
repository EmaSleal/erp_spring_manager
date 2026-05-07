## 📁 Estructura de Archivos Error

```
src/main/resources/templates/error/
├── 403.html    ✅ Ya existía (Acceso Denegado)
├── 404.html    ✅ CREADO (Página No Encontrada)  
└── 500.html    ✅ CREADO (Error Interno)
```

### Convenciones de Spring Boot

Spring Boot busca automáticamente plantillas de error en:
- `templates/error/[código].html` (específico)
- `templates/error/4xx.html` (genérico para 400-499)
- `templates/error/5xx.html` (genérico para 500-599)
- `templates/error.html` (fallback general)

Nuestra implementación usa códigos específicos para mejor control.

