## 💡 Lecciones Aprendidas

### **1. Validación de Queries en Startup**
Spring Boot valida todas las queries de JPA repositories al iniciar la aplicación. Esto es bueno porque detecta errores temprano, pero requiere actualizar TODAS las queries cuando cambias el modelo.

### **2. Búsqueda Exhaustiva**
Cuando eliminas un campo, no basta con actualizar el modelo:
- ✅ Buscar en @Query
- ✅ Buscar en métodos derivados (findByFecha...)
- ✅ Buscar en código de servicio
- ✅ Buscar en vistas

### **3. Campos de Auditoría vs Campos de Negocio**
- **Auditoría:** `createDate`, `updateDate` (Spring Data los maneja)
- **Negocio:** `fechaPago`, `fechaEntrega` (tú los manejas)

No duplicar información. Si `fecha` = `createDate`, eliminar la redundancia.

---

