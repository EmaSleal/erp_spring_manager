## 🐛 Troubleshooting

### Error: "Authentication failed"

**Causa:** Credenciales incorrectas o contraseña de aplicación mal configurada.

**Solución:**
1. Verifica que `EMAIL_USERNAME` sea correcto
2. Verifica que `EMAIL_PASSWORD` sea la contraseña de aplicación (no la contraseña normal)
3. Verifica que la verificación en dos pasos esté activa (Gmail)

### Error: "Could not connect to SMTP host"

**Causa:** Host o puerto incorrecto, o firewall bloqueando.

**Solución:**
1. Verifica `EMAIL_HOST` y `EMAIL_PORT`
2. Verifica tu conexión a internet
3. Verifica que tu firewall no bloquee el puerto 587/465

### Error: "Connection timed out"

**Causa:** Puerto bloqueado o configuración SSL/TLS incorrecta.

**Solución:**
1. Intenta cambiar el puerto:
   - Puerto 587 (TLS) → Puerto 465 (SSL)
   - O viceversa
2. Verifica configuración de firewall

### Las variables no se cargan

**Causa:** Variables no configuradas en la sesión actual.

**Solución Windows:**
```powershell
# Configurar permanentemente (requiere reiniciar terminal)
[System.Environment]::SetEnvironmentVariable("EMAIL_USERNAME", "tu-email@gmail.com", "User")
[System.Environment]::SetEnvironmentVariable("EMAIL_PASSWORD", "tu-password", "User")
```

**Solución Linux/Mac:**
```bash
# Agregar a ~/.bashrc o ~/.zshrc
echo 'export EMAIL_USERNAME="tu-email@gmail.com"' >> ~/.bashrc
echo 'export EMAIL_PASSWORD="tu-password"' >> ~/.bashrc
source ~/.bashrc
```

---

