## 🔧 Solución de Problemas

### Problema: No recibo notificaciones web

**Causas posibles:**
- Sesión desconectada
- WebSocket bloqueado
- Preferencias desactivadas

**Solución:**
1. Verificar que está logueado
2. Refresh de la página (F5)
3. Revisar preferencias (debe estar ✅ WEB activado)
4. Limpiar caché del navegador
5. Probar en modo incógnito

---

### Problema: No llegan emails

**Síntomas:**
- Notificaciones web funcionan
- Emails no llegan a la bandeja

**Verificar:**

1. **Email del usuario:**
   - Perfil → Email debe estar correcto
   - Confirmar que no hay typos

2. **Configuración del sistema:**
   - Configuración → Notificaciones
   - "Activar email" debe estar ✅

3. **Preferencias:**
   - Preferencias → Canal EMAIL debe estar ✅

4. **Carpeta SPAM:**
   - Revisar bandeja de correo no deseado
   - Marcar como "No es spam"

5. **Configuración SMTP (admin):**
   - Verificar credenciales SMTP
   - Probar con "Probar Email"

---

### Problema: Badge no se actualiza

**Síntomas:**
- Contador no disminuye al leer notificaciones
- Número incorrecto

**Solución:**
1. Marcar notificación como leída explícitamente
2. Refresh de página (F5)
3. Cerrar sesión y volver a entrar
4. Contactar al administrador si persiste

---

### Problema: WhatsApp no envía

**Causas:**
- Cliente sin teléfono
- Plantilla no aprobada
- API de WhatsApp desconectada

**Verificar:**
1. Cliente tiene teléfono registrado
2. Formato: +51987654321 (con código de país)
3. Plantilla está aprobada por Meta
4. Integración WhatsApp está activa (admin)

**Mensajes de error comunes:**

| Error | Significado | Solución |
|-------|-------------|----------|
| `Teléfono no válido` | Formato incorrecto | Usar +5198765432 1 |
| `Plantilla no encontrada` | Plantilla no existe | Contactar admin |
| `API no disponible` | Servicio caído | Esperar y reintentar |

---

### Problema: Demasiadas notificaciones

**Síntoma:**
- Recibo muchas notificaciones
- Interrumpen el trabajo

**Solución:**

1. **Ajustar frecuencia:**
   - Preferencias → Frecuencia
   - Cambiar a "Resumen diario"

2. **Desactivar tipos específicos:**
   - Preferencias → Tipos
   - Desmarcar tipos no importantes

3. **Desactivar canales:**
   - Mantener solo WEB
   - Desactivar EMAIL si es excesivo

4. **Horario laboral:**
   - Activar "Solo horario laboral"
   - No recibir fuera de horas

---

