## 📊 Contexto del Cambio

### **Refactorización Previa**

Este error surge de un cambio mayor en el modelo `Factura`:

#### **Campo Eliminado**
```java
private Timestamp fecha;  // ❌ ELIMINADO
```

#### **Campo Agregado**
```java
@Column(name = "fechaPago")
private Date fechaPago;  // ✅ AGREGADO
```

#### **Campos de Auditoría (Ya existentes)**
```java
@CreatedDate
@Column(name = "createDate", updatable = false)
private Timestamp createDate;  // ✅ REEMPLAZA a 'fecha'

@CreatedDate
@Column(name = "updateDate")
private Timestamp updateDate;
```

---

