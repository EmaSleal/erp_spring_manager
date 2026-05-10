## 🔄 FLUJO DE TRABAJO

### **1. Configuración (Una vez)**
```
Admin → /configuracion → Tab Facturación
│
├─ Configura serie (F001)
├─ Configura IGV (18%)
├─ Configura moneda (PEN, S/)
├─ Configura términos
└─ Guarda
   └─ Sistema valida y almacena
```

### **2. Creación de Facturas (Automático)**
```
Usuario crea factura
│
├─ Sistema obtiene configuración activa
├─ Genera número: "F001-00001"
├─ Calcula IGV: subtotal * 18% = S/ 18.00
├─ Calcula total: S/ 100.00 + S/ 18.00 = S/ 118.00
├─ Guarda factura
└─ Incrementa número a 2
```

### **3. Consulta (Rápido)**
```
Cliente pregunta: "¿Mi factura F001-00025?"
│
└─ Sistema busca por número
   └─ facturaRepository.findByNumeroFactura("F001-00025")
```

---

