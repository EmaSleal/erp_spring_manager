## 🔍 HALLAZGOS DEL CÓDIGO A INTEGRAR

### ✅ Código Existente a Aprovechar

#### 1. **Método `formatearMoneda()` ya existe**
**Ubicación:** Utilidad de formateo (a identificar)  
**Estado:** ✅ Implementado  
**Acción Sprint 6:**
- 🔧 Extender para soportar símbolo de múltiples divisas
- 🔧 Integrar con nuevo modelo `Moneda.java`
- 🔧 Usar en todos los reportes multi-divisa

**Tareas relacionadas:**
- FASE 1.2.5: Integrar `formatearMoneda()` con multi-divisa
- FASE 1.3.4: Actualizar reportes para usar formato multi-divisa

---

### ⚠️ Funcionalidad Existente No Utilizada

#### 2. **Enum `PRODUCTO_AJUSTAR_INVENTARIO` no se usa**
**Ubicación:** Modelo `Producto.java` o enums relacionados  
**Estado:** ⚠️ Definido pero no utilizado  
**Acción Sprint 6:**
- 🔧 Activar en formularios de ajuste de inventario
- 🔧 Vincular con `MovimientoInventario.java`
- 🔧 Validar en servicios de inventario

**Tareas relacionadas:**
- FASE 2.1.3: Activar enum `PRODUCTO_AJUSTAR_INVENTARIO`
- FASE 2.2.6: Integrar con flujo de ajustes de inventario
- FASE 2.3.2: Validar en `InventarioService`

**Valor:** Evita crear nueva funcionalidad, aprovecha código existente

---

#### 3. **Filtro `stockBajo` comentado como TODO en reportes**
**Ubicación:** Controllers o servicios de reportes  
**Estado:** ⚠️ TODO pendiente  
**Acción Sprint 6:**
- 🔧 Completar implementación del filtro
- 🔧 Crear endpoint `/reportes/stock-bajo`
- 🔧 Añadir parámetro de umbral configurable

**Tareas relacionadas:**
- FASE 2.4.1: Implementar filtro `stockBajo` en reportes
- FASE 2.4.2: Crear vista de productos con stock bajo
- FASE 2.4.3: Configurar umbral de stock mínimo por producto

**Valor:** Funcionalidad crítica para alertas de inventario

---

