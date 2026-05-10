## 🔧 Solución de Problemas

### Problema: No puedo crear usuarios

**Síntoma:**
```
❌ Acceso denegado
   No tienes permisos para esta acción
```

**Causas posibles:**
- No tienes rol ADMIN
- Tu sesión expiró
- Tu cuenta fue desactivada

**Solución:**
1. Verifica tu rol (debe ser ADMIN o SUPER_ADMIN)
2. Cierra sesión y vuelve a entrar
3. Contacta al super administrador

---

### Problema: Email "ya registrado"

**Síntoma:**
```
❌ El email ya está registrado
   El email juan@empresa.com ya pertenece a otro usuario
```

**Causas:**
- El email ya existe en el sistema
- Usuario fue eliminado pero email sigue en uso (raro)

**Solución:**

**Opción 1:** Verificar en el listado
```
1. Ir a listado de usuarios
2. Buscar: juan@empresa.com
3. Si aparece:
   a) Editar ese usuario en lugar de crear nuevo
   b) O usar otro email
```

**Opción 2:** Agregar variación
```
Original: juan@empresa.com
Variaciones:
  - juan.perez@empresa.com
  - jperez@empresa.com  
  - juan.p@empresa.com
```

---

### Problema: No se envían emails

**Síntoma:**
```
✅ Usuario creado exitosamente
⚠️ No se pudo enviar el email de notificación
```

**Causas:**
1. Configuración SMTP incorrecta
2. Email del usuario inválido
3. Servidor de correo caído

**Verificar:**

**1. Configuración SMTP (Admin)**
```
application.properties:
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=tu-email@gmail.com
spring.mail.password=tu-app-password ← Verificar
```

**2. Email válido**
```
✅ juan@empresa.com
❌ juan@empresa (sin dominio)
```

**3. Probar envío**
```
Configuración > Notificaciones > [Probar Email]
```

**Solución temporal:**
- Desmarcar "Enviar credenciales por email"
- Comunicar credenciales manualmente

---

### Problema: Usuario no puede iniciar sesión

**Síntoma:**
```
Usuario: juan@empresa.com
Contraseña: *******
[Iniciar Sesión]

❌ Credenciales inválidas
```

**Causas y soluciones:**

#### Causa 1: Usuario inactivo
```
Estado del usuario: 🔴 Inactivo

Solución:
1. Editar usuario
2. Marcar [✓] Usuario activo
3. Guardar
```

#### Causa 2: Contraseña incorrecta
```
Solución:
1. Resetear contraseña (botón 🔐)
2. Enviar nuevas credenciales
3. Usuario intenta con la nueva contraseña
```

#### Causa 3: Email incorrecto
```
Email registrado: juan.perez@empresa.com
Usuario intenta: juan@empresa.com ← Diferente

Solución:
- Verificar email exacto en el listado
- Usar el email correcto para login
```

#### Causa 4: Mayúsculas/minúsculas
```
Nota: El sistema NO distingue mayúsculas en email

Juan@Empresa.com = juan@empresa.com ✅
```

---

### Problema: Error al eliminar usuario

**Síntoma:**
```
❌ Error al eliminar usuario
   No se pudo completar la operación
```

**Causas:**

#### 1. Intentas eliminarte a ti mismo
```
❌ No puedes eliminar tu propia cuenta

Solución:
- Solicita a otro admin que te elimine
```

#### 2. Último SUPER_ADMIN
```
❌ Debe haber al menos un SUPER_ADMIN

Solución:
1. Promover a otro usuario a SUPER_ADMIN
2. Luego eliminar este usuario
```

#### 3. Usuario con relaciones críticas
```
⚠️ Usuario tiene facturas asociadas

Solución recomendada:
- DESACTIVAR en lugar de eliminar
- Se preserva el historial
```

---

### Problema: Contraseña generada muy compleja

**Síntoma:**
```
Contraseña generada: xK9m2#pL$wQ&

Usuario dice: "Es muy difícil de recordar"
```

**Soluciones:**

**Opción 1:** Generar otra contraseña
```
1. Clic en 🎲 Generar varias veces
2. Hasta obtener una más simple
```

**Opción 2:** Crear contraseña manual
```
En lugar de generar:
1. Escribir contraseña personalizada
2. Ejemplo: Empresa2025
3. Confirmar la misma
```

**Opción 3:** Usuario cambia después
```
1. Crear con contraseña generada
2. Usuario inicia sesión
3. Usuario va a Perfil > Cambiar Contraseña
4. Pone una contraseña que recuerde
```

---

### Problema: Muchos usuarios inactivos

**Síntoma:**
```
Estadísticas:
Total: 50
Activos: 20
Inactivos: 30 ← Muchos
```

**Recomendaciones:**

**Limpieza periódica:**
```
1. Filtrar por: Estado = Inactivo
2. Revisar usuarios inactivos
3. Eliminar los que no se necesitan
4. Mantener solo los relevantes
```

**Criterios de eliminación:**
```
✅ Eliminar:
   - Usuarios de prueba
   - Duplicados
   - Empleados que nunca usaron el sistema
   - Cuentas sin facturas ni clientes

❌ NO Eliminar:
   - Empleados con historial
   - Usuarios con facturas creadas
   - Cuentas con datos importantes
```

**Automatización futura:**
```
💡 Configurar:
   - Desactivar automáticamente usuarios sin login en 6 meses
   - Alertas de usuarios inactivos
   - Revisión trimestral
```

---

