## 🗑️ Eliminar Usuarios

### ⚠️ Advertencia Importante

**La eliminación es PERMANENTE e IRREVERSIBLE**

```
╔════════════════════════════════════════════╗
║  ⚠️ PRECAUCIÓN: ELIMINACIÓN PERMANENTE    ║
╠════════════════════════════════════════════╣
║                                            ║
║  • Los datos del usuario se borrarán       ║
║  • No se puede deshacer                    ║
║  • Afecta registros relacionados           ║
║                                            ║
║  💡 Recomendación:                         ║
║     Mejor DESACTIVAR en lugar de eliminar  ║
║                                            ║
╚════════════════════════════════════════════╝
```

### ¿Cuándo Eliminar?

**✅ Eliminar SI:**
- Usuario creado por error
- Cuenta de prueba
- Usuario duplicado
- Empleado que nunca usó el sistema

**❌ NO Eliminar SI:**
- Usuario tiene facturas creadas
- Usuario tiene clientes asignados
- Solo necesita bloquear acceso → **DESACTIVAR**
- Empleado renunció pero tiene historial → **DESACTIVAR**

### Procedimiento de Eliminación

**Paso 1: Clic en eliminar**
```
│ Pedro López│pedro@empresa.com│VEND.│🔴 Inactivo│[🗑️]│
                                                  ↑
                                                AQUÍ
```

**Paso 2: Primera confirmación**
```
┌──────────────────────────────────────────────┐
│  🗑️ Eliminar Usuario                        │
├──────────────────────────────────────────────┤
│                                              │
│  ¿Eliminar a Pedro López?                    │
│                                              │
│  ⚠️ ESTA ACCIÓN NO SE PUEDE DESHACER        │
│                                              │
│  Usuario: pedro@empresa.com                  │
│  Rol: VENDEDOR                               │
│  Estado: Inactivo                            │
│                                              │
│  Se eliminarán:                              │
│  • Datos personales                          │
│  • Configuración de cuenta                   │
│  • Preferencias                              │
│                                              │
│  Se CONSERVARÁN (con referencia):            │
│  • Facturas creadas (autor: [eliminado])    │
│  • Clientes asignados (sin asignar)         │
│                                              │
│  [Cancelar]          [Continuar]             │
└──────────────────────────────────────────────┘
```

**Paso 3: Segunda confirmación (seguridad)**
```
┌──────────────────────────────────────────────┐
│  ⚠️ CONFIRMACIÓN FINAL                      │
├──────────────────────────────────────────────┤
│                                              │
│  Para confirmar la eliminación, escriba:     │
│                                              │
│  ELIMINAR                                    │
│                                              │
│  [____________________]                      │
│                                              │
│  [Cancelar]          [Eliminar Usuario]      │
│                                (deshabilitado hasta escribir)│
└──────────────────────────────────────────────┘
```

**Paso 4: Usuario eliminado**
```
✅ Usuario eliminado exitosamente
   
   Pedro López ha sido eliminado del sistema.
   
   ℹ️ Los registros relacionados se han actualizado
      para mantener la integridad de los datos.
```

### Restricciones de Eliminación

#### 1. No puedes eliminarte a ti mismo

```
❌ Error: Auto-eliminación no permitida
   
   No puedes eliminar tu propia cuenta.
   
   Solicita a otro administrador que lo haga.
```

**Solución:** Otro admin debe eliminarte

---

#### 2. Último SUPER_ADMIN

```
❌ Error: No se puede eliminar
   
   Este es el único SUPER_ADMIN del sistema.
   
   Debe haber al menos un SUPER_ADMIN activo.
```

**Solución:** 
1. Promover a otro usuario a SUPER_ADMIN
2. Luego eliminar este usuario

---

#### 3. Usuario con datos críticos

```
⚠️ Advertencia: Usuario con registros
   
   Este usuario tiene:
   • 45 facturas creadas
   • 12 clientes asignados
   • 8 productos modificados
   
   ¿Seguro que desea eliminar?
   
   💡 Recomendación: DESACTIVAR en su lugar
   
   [Cancelar] [Desactivar] [Eliminar de todos modos]
```

---

### Impacto de Eliminación

**Datos eliminados:**
```
❌ Datos personales (nombre, email, teléfono)
❌ Credenciales de acceso
❌ Preferencias de notificaciones
❌ Avatar/foto de perfil
❌ Configuración personal
```

**Datos que se preservan (con referencia nula):**
```
✅ Facturas creadas → autor: [Usuario eliminado]
✅ Clientes asignados → sin asignar
✅ Logs del sistema → ID de usuario + [eliminado]
✅ Auditoría → Se mantiene para historial
```

**Ejemplo de factura después de eliminar usuario:**
```
Factura F001-00125
Cliente: ABC Company
Total: S/ 1,250.00
Creado por: [Usuario eliminado] (ID: 5)
Fecha: 15/01/2025
```

### Recuperación de Usuario Eliminado

**⚠️ NO ES POSIBLE**

Una vez eliminado, el usuario **no puede recuperarse**.

**Alternativas:**
1. **Crear nuevo usuario** con los mismos datos
   - Tendrá un ID diferente
   - No tendrá el historial anterior
   
2. **Restaurar desde backup** (si existe)
   - Requiere intervención técnica
   - Solo si hay backup reciente

**💡 Por eso recomendamos DESACTIVAR en lugar de eliminar**

---

