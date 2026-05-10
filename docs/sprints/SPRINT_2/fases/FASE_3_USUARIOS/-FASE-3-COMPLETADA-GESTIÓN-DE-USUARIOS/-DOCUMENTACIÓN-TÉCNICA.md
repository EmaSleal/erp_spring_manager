## 📚 DOCUMENTACIÓN TÉCNICA

### Dependencias Utilizadas:

**Backend:**
- Spring Boot 3.5.0
- Spring Security 6.5.0
- Spring Data JPA 3.5.0
- Hibernate 6.6.4
- Lombok 1.18.36
- BCrypt (incluido en Spring Security)

**Frontend:**
- Bootstrap 5.3.0
- Bootstrap Icons 1.10.0
- Font Awesome 6.4.0
- jQuery 3.6.0
- SweetAlert2 11.x

### Convenciones de Código:

**Nomenclatura:**
- Controladores: `{Entidad}Controller`
- Servicios: `{Entidad}Service` / `{Entidad}ServiceImpl`
- Repositorios: `{Entidad}Repository`
- Vistas: `{entidad}/{accion}.html`
- CSS: `{modulo}.css`
- JS: `{modulo}.js`

**Estructura de URLs:**
```
GET  /{entidad}              → Lista
GET  /{entidad}/form         → Crear
GET  /{entidad}/form/{id}    → Editar
POST /{entidad}/save         → Guardar
POST /{entidad}/delete/{id}  → Eliminar
POST /{entidad}/toggle-*     → Toggle estados
```

**Mensajes:**
- Success: RedirectAttributes.addFlashAttribute("mensaje", ...)
- Error: RedirectAttributes.addFlashAttribute("error", ...)
- AJAX: ResponseEntity.ok() / ResponseEntity.badRequest()

---

