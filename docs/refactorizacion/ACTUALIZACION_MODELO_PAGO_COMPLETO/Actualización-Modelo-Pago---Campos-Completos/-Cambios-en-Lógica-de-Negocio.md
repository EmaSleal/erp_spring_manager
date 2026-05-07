## 🔧 Cambios en Lógica de Negocio

### 1. **Factura Ahora es Opcional**

**Antes**:
```java
@ManyToOne(optional = false)  // Siempre requerida
private Factura factura;
```

**Después**:
```java
@ManyToOne  // Opcional para adelantos
private Factura factura;
```

**Impacto**: Permite registrar adelantos de clientes sin factura asignada.

---

### 2. **Nueva Relación Directa con Cliente**

**Agregado**:
```java
@ManyToOne(optional = false)  // Siempre requerido
@JoinColumn(name = "clienteId")
private Cliente cliente;
```

**Ventajas**:
- Adelantos no necesitan factura ficticia
- Consultas directas de pagos por cliente
- Mejor integridad de datos

---

### 3. **Validación Mejorada de Factura**

**Antes**: Factura siempre obligatoria  
**Después**: Depende del tipo de pago

```java
// Se valida automáticamente en @PrePersist
if (tipoPago.requiereFactura() && factura == null) {
    throw new IllegalStateException("Requiere factura");
}
```

Tipos que **requieren** factura:
- ✅ TOTAL
- ✅ PARCIAL
- ✅ NOTA_CREDITO

Tipos que **NO requieren** factura:
- ❌ ADELANTO

---

### 4. **Anulación con Trazabilidad**

**Antes**: Solo cambiar estado
```java
pago.setEstado(EstadoPago.ANULADO);
```

**Después**: Método de negocio completo
```java
pago.anular("Duplicado", usuarioId);
// Registra: quién, cuándo, por qué
```

---

