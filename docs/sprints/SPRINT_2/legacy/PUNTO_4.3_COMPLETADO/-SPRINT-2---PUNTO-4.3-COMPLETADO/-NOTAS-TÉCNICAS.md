## 📝 NOTAS TÉCNICAS

### **Decisiones de Diseño**

1. **Variables booleanas por rol**: Mejora legibilidad
   ```java
   boolean esAdmin = "ADMIN".equals(rol);
   ```

2. **Expresiones OR para permisos**: Declarativo y fácil de entender
   ```java
   esAdmin || esUser || esVendedor || esVisualizador
   ```

3. **Orden de módulos**: Según importancia operativa
   - Clientes → Productos → Facturas → Usuarios
   - Pedidos → Reportes → Configuración

4. **Iconos FontAwesome**: Consistencia visual
   - `fas fa-users` (Clientes)
   - `fas fa-box` (Productos)
   - `fas fa-file-invoice-dollar` (Facturas)
   - `fas fa-user-cog` (Usuarios)

5. **Colores diferenciados**: Identificación rápida
   - Verde `#4CAF50` (Clientes)
   - Naranja `#FF9800` (Productos)
   - Morado `#9C27B0` (Facturas)
   - Azul `#3F51B5` (Usuarios)

---

