## 🔄 Activar/Desactivar Usuarios

### Toggle de Estado

**¿Qué hace?**
- Alterna entre Activo ↔ Inactivo
- Sin eliminar el usuario
- Reversible en cualquier momento

**Acceso:**
- Botón **🔄 Toggle** en el listado
- O desde formulario de edición

### Desactivar Usuario

**Escenario:** Empleado de vacaciones o permiso temporal

**Paso 1: Clic en toggle**
```
│ Juan Pérez │juan@empresa.com│USER│🟢 Activo│[🔄]│
                                          ↑
                                        CLIC
```

**Paso 2: Confirmación**
```
┌──────────────────────────────────────────────┐
│  ⚠️ Desactivar Usuario                      │
├──────────────────────────────────────────────┤
│                                              │
│  ¿Desactivar a Juan Pérez?                  │
│                                              │
│  El usuario no podrá iniciar sesión hasta   │
│  que sea reactivado.                         │
│                                              │
│  Sus datos se conservarán.                   │
│                                              │
│  [Cancelar]        [Sí, desactivar]         │
└──────────────────────────────────────────────┘
```

**Paso 3: Usuario desactivado**
```
✅ Usuario desactivado
   Juan Pérez no podrá acceder al sistema.
```

**Nuevo estado en listado:**
```
│ Juan Pérez │juan@empresa.com│USER│🔴 Inactivo│[🔄]│
```

### Reactivar Usuario

**Proceso idéntico al desactivar:**

1. Clic en toggle del usuario inactivo
2. Confirmar reactivación
3. Usuario vuelve a estado activo

```
✅ Usuario reactivado
   Juan Pérez puede acceder nuevamente al sistema.
```

### Efectos de Desactivación

**Usuario desactivado NO puede:**
- ❌ Iniciar sesión
- ❌ Recibir notificaciones
- ❌ Aparecer en asignaciones

**Se CONSERVA:**
- ✅ Todos sus datos personales
- ✅ Historial de facturas creadas
- ✅ Clientes asignados
- ✅ Registros de auditoría

### Desactivación Masiva

**Para desactivar múltiples usuarios:**

1. **Opción A:** Usar filtros
   ```
   Estado: [Inactivo ▼]
   ```
   Muestra solo inactivos

2. **Opción B:** Checkbox (si está implementado)
   ```
   [✓] Juan Pérez
   [✓] María García  
   [ ] Pedro López
   
   [Desactivar seleccionados]
   ```

---

