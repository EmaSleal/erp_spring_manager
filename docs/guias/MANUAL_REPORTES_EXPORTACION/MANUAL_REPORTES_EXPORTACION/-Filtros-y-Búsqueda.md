## 🔍 Filtros y Búsqueda

### Tipos de Filtros

#### 1. Filtros por Fecha

**Disponible en:** Reporte de Ventas

**Opciones:**
- **Rango personalizado:** Fecha inicio + Fecha fin
- **Mes actual:** Automático al abrir
- **Mes anterior:** Un clic para seleccionar
- **Último año:** Análisis anual

**Ejemplo de uso:**
```
Ver ventas del primer trimestre 2025:
  Fecha Inicio: 01/01/2025
  Fecha Fin:    31/03/2025
```

#### 2. Filtros por Cliente

**Disponible en:** Reporte de Ventas

**Cómo funciona:**
1. Dropdown con lista de todos los clientes
2. Seleccione uno para ver solo sus facturas
3. O deje en blanco para ver todos

**Uso práctico:**
- Analizar compras de un cliente específico
- Verificar deuda de un cliente
- Generar estado de cuenta

#### 3. Filtros por Estado

**Disponible en:** Reporte de Clientes

**Opciones:**
- **Activos:** Clientes con compras recientes
- **Inactivos:** Sin compras en período largo
- **Todos:** Todos los clientes

**Criterio de "activo":**
- Cliente con al menos 1 compra en los últimos 6 meses

#### 4. Filtros por Deuda

**Disponible en:** Reporte de Clientes

**Opciones:**
- **Con deuda:** Clientes con facturas pendientes
- **Sin deuda:** Clientes al día
- **Todos:** Todos los clientes

**Uso práctico:**
- Gestión de cobranza
- Identificar cuentas por cobrar
- Seguimiento de pagos

#### 5. Filtros de Stock

**Disponible en:** Reporte de Productos

**Opciones:**
- **Stock bajo:** Productos debajo del mínimo
- **Sin ventas:** Productos sin movimiento
- **Todos:** Todo el inventario

**Alertas generadas:**
```
Stock Bajo detectado:
  - Producto A: 5 unidades (Mínimo: 20)
  - Producto B: 2 unidades (Mínimo: 10)
  
Requiere reabastecimiento urgente
```

### Combinación de Filtros

Puede combinar múltiples filtros para análisis específicos:

**Ejemplo 1:** Ventas de un cliente en diciembre
```
Tipo: Ventas
Fecha Inicio: 01/12/2025
Fecha Fin: 31/12/2025
Cliente: Cliente ABC
```

**Ejemplo 2:** Clientes activos con deuda
```
Tipo: Clientes
Estado: Activo
Con Deuda: Sí
```

**Ejemplo 3:** Productos con stock crítico
```
Tipo: Productos
Stock Bajo: Sí
Sin Ventas: No
```

### Limpiar Filtros

Para resetear todos los filtros:
1. Haga clic en el botón **"Limpiar Filtros"** 🔄
2. O recargue la página (F5)

---

