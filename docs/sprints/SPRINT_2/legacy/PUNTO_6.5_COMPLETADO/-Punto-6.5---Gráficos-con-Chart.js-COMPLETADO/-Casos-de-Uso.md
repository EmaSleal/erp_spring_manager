## 🔍 Casos de Uso

### **Caso 1: Ver Dashboard de Reportes**
```
1. Usuario navega a /reportes
2. Sistema carga automáticamente 3 gráficos:
   - Ventas por mes (últimos 12 meses)
   - Clientes nuevos por mes (últimos 12 meses)
   - Top 10 productos más vendidos
3. Gráficos se renderizan con animación
4. Usuario puede interactuar con tooltips al pasar el mouse
5. Datos se actualizan dinámicamente desde API REST
```

### **Caso 2: Filtrar Ventas y Ver Gráfico**
```
1. Usuario navega a /reportes/ventas
2. Aplica filtros: fechaInicio=2025-01-01, fechaFin=2025-03-31
3. Sistema genera tabla de ventas filtradas
4. Sistema renderiza gráfico de ventas por día
5. Gráfico muestra solo las ventas del período seleccionado
6. Usuario puede exportar tanto la tabla como analizar el gráfico
```

### **Caso 3: Identificar Productos Más Vendidos**
```
1. Usuario consulta dashboard
2. Observa gráfico de productos más vendidos
3. Identifica visualmente los top 10 productos
4. Cada producto tiene un color único para fácil identificación
5. Tooltip muestra cantidad exacta vendida al pasar el mouse
6. Usuario puede tomar decisiones de inventario basadas en el gráfico
```

---

