## 📚 Casos de Uso Prácticos

### Caso 1: Nuevo empleado ingresa

**Objetivo:** Crear cuenta para vendedor nuevo

**Pasos:**
1. Usuarios → ➕ Nuevo Usuario
2. Datos:
   ```
   Nombre: Carlos Mendoza
   Email: carlos.mendoza@empresa.com
   Teléfono: 987654321
   Rol: VENDEDOR
   ```
3. Generar contraseña (clic en 🎲)
4. Marcar: [✓] Enviar credenciales por email
5. Guardar
6. Verificar que recibió el email
7. Ayudarlo en su primer login

---

### Caso 2: Empleado de vacaciones

**Objetivo:** Bloquear acceso temporalmente (1 mes)

**Pasos:**
1. Buscar usuario: "Juan Pérez"
2. Clic en 🔄 Toggle
3. Usuario queda 🔴 Inactivo
4. Guardar fecha de reactivación: 01/02/2026
5. En esa fecha: 🔄 Toggle nuevamente
6. Usuario vuelve a 🟢 Activo

**No hacer:**
- ❌ Eliminar usuario
- ❌ Cambiar contraseña

---

### Caso 3: Promover empleado

**Objetivo:** Cambiar vendedor a admin

**Pasos:**
1. Usuarios → Buscar: "María García"
2. Clic en ✏️ Editar
3. Cambiar rol: VENDEDOR → ADMIN
4. Guardar
5. Notificar al usuario:
   ```
   Hola María,
   
   Has sido promovida a Administradora.
   Ahora tienes acceso a:
   - Gestión de usuarios
   - Configuración del sistema
   - Reportes completos
   
   Saludos,
   Gerencia
   ```

---

### Caso 4: Empleado olvidó contraseña

**Objetivo:** Resetear y enviar nueva contraseña

**Pasos:**
1. Usuarios → Buscar empleado
2. Clic en 🔐 Resetear
3. Confirmar
4. Sistema genera: `mK8#xP2q`
5. Se envía email automático
6. Informar al empleado:
   ```
   Hola Pedro,
   
   Se ha reseteado tu contraseña.
   Revisa tu email para las nuevas credenciales.
   
   Te recomiendo cambiarla en tu primer login:
   Perfil > Seguridad > Cambiar Contraseña
   ```

---

### Caso 5: Auditoría trimestral

**Objetivo:** Limpiar usuarios inactivos

**Pasos:**
1. Filtrar: Estado = Inactivo
2. Revisar lista de inactivos
3. Verificar última actividad
4. Criterios:
   ```
   ✅ Eliminar si:
      - Sin login en 6+ meses
      - Sin facturas creadas
      - Cuenta de prueba
      
   ❌ Mantener si:
      - Tiene historial
      - Empleado temporal
      - Puede volver
   ```
5. Eliminar usuarios innecesarios
6. Documentar en reporte
7. Exportar listado final

---

