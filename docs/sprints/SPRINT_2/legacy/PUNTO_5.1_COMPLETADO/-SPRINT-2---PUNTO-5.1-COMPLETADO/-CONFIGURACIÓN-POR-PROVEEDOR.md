## 🔧 CONFIGURACIÓN POR PROVEEDOR

### Gmail (Recomendado)

```yaml
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=tu-email@gmail.com
EMAIL_PASSWORD=contraseña-de-aplicacion-16-caracteres
```

**Requisitos:**
1. Cuenta de Gmail activa
2. Verificación en dos pasos activada
3. Contraseña de aplicación generada

**Ventajas:**
- ✅ Gratuito
- ✅ Confiable
- ✅ Fácil de configurar
- ✅ 500 emails/día (gratuito)

---

### Outlook/Hotmail

```yaml
EMAIL_HOST=smtp-mail.outlook.com
EMAIL_PORT=587
EMAIL_USERNAME=tu-email@outlook.com
EMAIL_PASSWORD=tu-contraseña-normal
```

**Ventajas:**
- ✅ No requiere contraseña de aplicación
- ✅ Integración con Microsoft 365
- ✅ Límite generoso de envíos

---

### Yahoo Mail

```yaml
EMAIL_HOST=smtp.mail.yahoo.com
EMAIL_PORT=465
EMAIL_USERNAME=tu-email@yahoo.com
EMAIL_PASSWORD=contraseña-de-aplicacion
```

**Requisitos:**
- Contraseña de aplicación generada

---

### Servidor SMTP Personalizado

```yaml
EMAIL_HOST=smtp.tudominio.com
EMAIL_PORT=587
EMAIL_USERNAME=noreply@tudominio.com
EMAIL_PASSWORD=tu-contraseña
```

**Ventajas:**
- ✅ Control total
- ✅ Sin límites de envío
- ✅ Email corporativo
- ✅ Mayor profesionalismo

---

