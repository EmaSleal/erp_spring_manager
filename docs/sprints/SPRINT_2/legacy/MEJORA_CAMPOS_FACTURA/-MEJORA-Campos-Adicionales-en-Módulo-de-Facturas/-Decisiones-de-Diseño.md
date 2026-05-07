## 💡 Decisiones de Diseño

### 1. ¿Por qué hacer los campos opcionales?

**Decisión:** Permitir que el usuario los deje vacíos y se auto-generen.

**Razones:**
- ✅ Flexibilidad: usuarios avanzados pueden personalizar
- ✅ Simplicidad: usuarios básicos no se confunden
- ✅ Automatización: reduce errores humanos
- ✅ Compatibilidad: funciona con facturas antiguas sin estos campos

---

### 2. ¿Por qué +7 días para fecha de pago?

**Decisión:** Default de 7 días, pero editable.

**Razones:**
- ✅ Estándar común en negocios
- ✅ Usuario puede modificarlo si necesita
- ✅ Facilita recordatorios de pago
- ✅ Mejor que no tener fecha

**Alternativas consideradas:**
- 15 días - Demasiado largo para mayoría de casos
- 30 días - Solo para facturas corporativas
- Sin default - Usuario tiene que calcular manualmente ❌

---

### 3. ¿Por qué resumen de totales en Paso 2?

**Decisión:** Mostrar totales acumulativos en tiempo real.

**Razones:**
- ✅ Feedback inmediato al agregar productos
- ✅ Evita sorpresas al guardar
- ✅ Facilita verificación de montos
- ✅ UX más profesional

---

### 4. ¿Por qué columna separada para N° Factura en tabla?

**Decisión:** Columna nueva destacada en azul.

**Razones:**
- ✅ Identificador más importante que ID interno
- ✅ Facilita búsqueda visual
- ✅ Cumple requisitos fiscales
- ✅ Mejora trazabilidad

---

