## 🔧 Variables de Entorno

### Tabla de configuración

| Variable | Descripción | Valor por defecto | Requerido |
|----------|-------------|-------------------|-----------|
| `EMAIL_HOST` | Servidor SMTP | `smtp.gmail.com` | ✅ |
| `EMAIL_PORT` | Puerto SMTP (587=TLS, 465=SSL) | `587` | ✅ |
| `EMAIL_USERNAME` | Dirección de email | - | ✅ |
| `EMAIL_PASSWORD` | Contraseña de aplicación | - | ✅ |

### Métodos de configuración

#### 1. Variables de entorno del sistema

**Ventaja:** Más seguro, no se guardan en archivos  
**Desventaja:** Hay que configurarlas en cada terminal

#### 2. Archivo .env

**Ventaja:** Fácil de configurar, persiste entre sesiones  
**Desventaja:** Hay que tener cuidado de no subirlo al repositorio

#### 3. IDE (IntelliJ/Eclipse)

Configura las variables en "Run Configuration" → "Environment Variables"

---

