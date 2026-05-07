## 🔧 Implementación Futura (Punto 5.3.3)

### **Lógica de Recordatorios**

```java
@Scheduled(cron = "0 0 9 * * ?")  // Cada día a las 9 AM
public void enviarRecordatoriosPago() {
    LocalDate hoy = LocalDate.now();
    
    // Buscar facturas vencidas:
    // - tipo_factura = PENDIENTE
    // - fecha_pago < hoy
    // - entregado = true
    // - cliente.email != null
    
    List<Factura> facturasVencidas = facturaRepository
        .findFacturasConPagoVencido(Date.valueOf(hoy));
    
    for (Factura factura : facturasVencidas) {
        emailService.enviarRecordatorioPago(factura);
    }
}
```

### **Query en FacturaRepository**

```java
@Query("SELECT f FROM Factura f " +
       "WHERE f.tipoFactura = 'PENDIENTE' " +
       "AND f.fechaPago < :fechaHoy " +
       "AND f.entregado = true " +
       "AND f.cliente.email IS NOT NULL")
List<Factura> findFacturasConPagoVencido(@Param("fechaHoy") Date fechaHoy);
```

---

