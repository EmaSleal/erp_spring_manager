## 💡 DECISIONES DE DISEÑO

### 1. **Mostrar vs Ocultar**
**Decisión:** Ocultar completamente los elementos no permitidos  
**Razón:** Evita confusión del usuario y mejora UX

**Alternativa descartada:** Mostrar elementos deshabilitados
- ❌ Genera frustración
- ❌ Hace la UI más confusa
- ❌ Ocupa espacio innecesario

### 2. **Badges informativos**
**Decisión:** Mostrar badge "Solo lectura" cuando no hay acciones disponibles  
**Razón:** Comunica claramente las limitaciones del rol

**Ventajas:**
- ✅ Usuario entiende por qué no ve botones
- ✅ Reduce tickets de soporte
- ✅ Mejora experiencia de usuario

### 3. **Indicadores en Sidebar**
**Decisión:** Usar badges pequeños (iconos) en el menú lateral  
**Razón:** Anticipar restricciones antes de entrar al módulo

**Iconos elegidos:**
- 👁️ Ojo = Solo lectura
- ➕ Plus = Puede crear

### 4. **VENDEDOR puede ver botón "Nueva Factura"**
**Decisión:** VENDEDOR tiene acceso a crear facturas  
**Razón:** Es su función principal en el sistema

**Restricción:** No puede eliminar facturas creadas
- ✅ Previene eliminación accidental
- ✅ Mantiene auditoría
- ✅ ADMIN/USER controlan eliminaciones

### 5. **JavaScript para Productos**
**Decisión:** Usar lógica JavaScript en vez de solo Thymeleaf  
**Razón:** La tabla se renderiza dinámicamente con JS

**Implementación:**
- Pasar `userRole` desde Thymeleaf a JavaScript
- Aplicar `display:none` según rol
- Mostrar badge condicionalmente

---

