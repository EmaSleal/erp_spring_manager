## 🧪 Testing Manual

### Caso 1: Costa Rica (CRC - Colones)
1. Ir a `/configuracion/facturacion`
2. Configurar `moneda_predeterminada = "CRC"`
3. Ir a `/reportes/ventas`
4. **Esperado:** Ver `₡ 1,234.56` en todos los montos
5. **Resultado:** ✅ PASS

### Caso 2: México (MXN - Pesos)
1. Cambiar configuración a `"MXN"`
2. Refrescar reportes
3. **Esperado:** Ver `$ 1,234.56`
4. **Resultado:** ✅ PASS

### Caso 3: Sin Configuración
1. Eliminar configuración de facturación
2. Refrescar reportes
3. **Esperado:** Ver `₡` (fallback)
4. **Resultado:** ✅ PASS

---

