## 🎯 OBJETIVOS DEL SPRINT

### Objetivo Principal
Expandir las capacidades comerciales del sistema mediante soporte multi-divisa para facturación internacional, implementar un sistema robusto de control de inventario, y establecer la gestión de proveedores con cuentas por pagar.

### Objetivos Específicos

1. **💱 Multi-Divisa:**
   - Soporte para múltiples monedas (USD, EUR, CRC, etc.)
   - Tipos de cambio históricos
   - Actualización automática de tasas (API externa)
   - Conversión automática en transacciones
   - Reportes consolidados multi-divisa
   - **Aprovechar `formatearMoneda()` existente**

2. **📦 Inventario Avanzado:**
   - Kardex detallado por producto
   - Gestión de lotes y fechas de vencimiento
   - Movimientos de entrada/salida
   - Alertas de stock mínimo y bajo
   - Ajustes de inventario
   - **Activar `PRODUCTO_AJUSTAR_INVENTARIO`**
   - **Implementar filtro `stockBajo` en reportes**

3. **🏭 Proveedores y Compras:**
   - Catálogo de proveedores completo
   - Órdenes de compra
   - Cuentas por pagar
   - Historial de pagos
   - Evaluación de proveedores

4. **🧪 Testing:**
   - Cobertura del 75%+
   - Tests de conversión de divisas
   - Tests de movimientos de inventario

5. **📚 Documentación:**
   - Manuales técnicos completos
   - Guías de configuración

---

