## 📋 Problema Identificado

Los reportes del sistema tenían el símbolo de moneda **hardcoded** en múltiples lugares:
- ❌ `reportes/ventas.html` usaba **"S/"** (Soles peruanos)
- ❌ `reportes/productos.html` usaba **"S/"** (Soles peruanos)
- ❌ No se utilizaba la configuración de moneda de `configuracion_facturacion`
- ❌ Sistema configurado para Costa Rica debería usar **"₡"** (Colones)

**Impacto:**
- Confusión para usuarios en diferentes países
- Inconsistencia con la configuración del sistema
- Valores monetarios mostrados con símbolo incorrecto

---

