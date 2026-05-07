## 📊 Logs Esperados

### ✅ Logs de Éxito:

```
2025-10-12T18:50:00.000-06:00  INFO ... Actualizando líneas: [LineaFacturaR[...], ...]
Hibernate: call sp_actualizar_linea_factura(?, ?, ?, ?, ?, ?, ?, ?)
Hibernate: call sp_actualizar_linea_factura(?, ?, ?, ?, ?, ?, ?, ?)
...
2025-10-12T18:50:00.100-06:00  INFO ... Estado de factura 3 actualizado a: true
```

### ❌ Problema Anterior (logs sin actualización de estado):

```
2025-10-12T18:27:36.192-06:00  INFO ... Actualizando líneas: [LineaFacturaR[...], ...]
Hibernate: call sp_actualizar_linea_factura(?, ?, ?, ?, ?, ?, ?, ?)
Hibernate: call sp_actualizar_linea_factura(?, ?, ?, ?, ?, ?, ?, ?)
...
// ❌ No había log de actualización de estado
```

---

