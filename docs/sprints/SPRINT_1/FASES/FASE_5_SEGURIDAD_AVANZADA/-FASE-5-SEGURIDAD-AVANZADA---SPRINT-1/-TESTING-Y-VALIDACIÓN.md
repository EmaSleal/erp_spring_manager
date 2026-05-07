## 🧪 TESTING Y VALIDACIÓN

### Checklist de Testing

#### Punto 5.1 (SecurityConfig)

- [ ] Login con credenciales válidas → Redirige a `/dashboard`
- [ ] Login con credenciales inválidas → Redirige a `/auth/login?error=true`
- [ ] Logout → Invalida sesión y redirige a `/auth/login?logout`
- [ ] Acceso a `/dashboard` sin autenticación → Redirige a `/auth/login`
- [ ] Acceso a `/configuracion` con rol USER → 403 Forbidden
- [ ] Acceso a `/configuracion` con rol ADMIN → 200 OK
- [ ] Múltiples logins del mismo usuario → Cierra sesión anterior
- [ ] Cookie JSESSIONID eliminada después del logout

#### Punto 5.2 (CSRF Token)

- [ ] Meta tag `_csrf` presente en HTML
- [ ] Meta tag `_csrf_header` presente en HTML
- [ ] JavaScript puede leer token CSRF
- [ ] Requests AJAX incluyen token CSRF

#### Punto 5.3 (Último Acceso)

- [ ] Campo `ultimo_acceso` se actualiza al login
- [ ] Timestamp es correcto
- [ ] No genera errores en consola
- [ ] Visible en `/perfil`

---

