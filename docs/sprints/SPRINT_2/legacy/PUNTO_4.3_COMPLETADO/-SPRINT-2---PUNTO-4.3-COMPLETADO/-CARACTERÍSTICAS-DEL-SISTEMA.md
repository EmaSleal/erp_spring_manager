## 🎨 CARACTERÍSTICAS DEL SISTEMA

### **ModuloDTO - Estructura de Datos**

Cada módulo contiene:
- **Nombre**: Título del módulo
- **Descripción**: Texto descriptivo
- **Icono**: Clase FontAwesome (ej: `fas fa-users`)
- **Color**: Color hex para la tarjeta (ej: `#4CAF50`)
- **URL**: Ruta del módulo (ej: `/clientes`)
- **Habilitado**: `true` si está implementado, `false` si está próximamente
- **Visible**: `true` si el usuario tiene permiso para verlo

### **Lógica de Filtrado**

```java
boolean esAdmin = "ADMIN".equals(rol);
boolean esUser = "USER".equals(rol);
boolean esVendedor = "VENDEDOR".equals(rol);
boolean esVisualizador = "VISUALIZADOR".equals(rol);
```

Cada módulo evalúa si el rol actual tiene permiso:
```java
esAdmin || esUser || esVendedor || esVisualizador
```

---

