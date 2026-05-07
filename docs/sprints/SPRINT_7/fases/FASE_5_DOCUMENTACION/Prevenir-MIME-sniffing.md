# Prevenir MIME sniffing
X-Content-Type-Options: nosniff
```

### CORS

```java
// Configuración CORS
@CrossOrigin(origins = "https://erp.miempresa.com")
```

---

## 📊 Auditoría

### Eventos Auditados

Todos los siguientes eventos se registran:

- ✅ Login exitoso/fallido
- ✅ Logout
- ✅ Habilitación/deshabilitación 2FA
- ✅ Cambio de contraseña
- ✅ Creación/modificación/eliminación de usuarios
- ✅ Cambios en permisos/roles
- ✅ Bloqueo/desbloqueo de cuentas
- ✅ Intentos de acceso no autorizado

### Consultar Auditoría

**Para administradores:**

1. Ir a **Seguridad > Auditoría**
2. Filtrar por:
   - Usuario
   - Tipo de evento
   - Rango de fechas
   - Resultado (éxito/fallo)
   - IP
3. Ver detalles de cada evento

### Retención

- Eventos se guardan por **2 años**
- Backup semanal de tabla de auditoría

---

## ✅ Buenas Prácticas

### Para Usuarios

1. ✅ Usar contraseñas fuertes y únicas
2. ✅ Habilitar 2FA
3. ✅ Guardar códigos de recuperación
4. ✅ Cerrar sesión al terminar (especialmente en dispositivos compartidos)
5. ✅ No compartir contraseñas
6. ✅ Reportar actividad sospechosa

### Para Administradores

1. ✅ Forzar 2FA para usuarios con permisos elevados
2. ✅ Revisar logs de auditoría regularmente
3. ✅ Implementar política de cambio de contraseña cada 90 días
4. ✅ Revocar acceso de empleados que dejan la empresa
5. ✅ Mantener sistema actualizado
6. ✅ Realizar auditorías de seguridad periódicas

---

## 🆘 Soporte

Para problemas de seguridad:
- **Email:** seguridad@erp.com
- **Urgencias:** +506 XXXX-XXXX
- **Documentación:** https://docs.erp.com/seguridad

---

**Última actualización:** Enero 2026  
**Versión:** 7.0.0
```

---

