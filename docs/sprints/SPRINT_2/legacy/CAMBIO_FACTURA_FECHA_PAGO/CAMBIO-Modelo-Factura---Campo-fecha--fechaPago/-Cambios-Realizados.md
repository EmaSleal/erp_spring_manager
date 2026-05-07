## 🔄 Cambios Realizados

### **Modelo Factura.java**

#### Campo ELIMINADO ❌
```java
private Timestamp fecha;  // Fecha de emisión (redundante con createDate)
```

**Razón de eliminación:**
- Redundante con `createDate` (auditoría)
- Los stored procedures fueron actualizados manualmente
- No aportaba valor de negocio único

#### Campo AGREGADO ✅
```java
@Column(name = "fechaPago")
private Date fechaPago;  // Fecha límite de pago
```

**Propósito:**
- Define cuándo debe pagar el cliente
- Base para recordatorios de pago (Punto 5.3.3)
- Cálculo: `fechaEntrega + días de crédito` (ej: 7 días)

---

