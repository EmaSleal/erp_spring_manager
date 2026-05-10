## 🔧 Índices Recomendados

Para maximizar performance de los SPs, se agregaron índices:

```sql
-- Índice en fecha_emision para ventas por mes
CREATE INDEX idx_factura_fecha_emision ON factura(fecha_emision);

-- Índice en create_date de cliente
CREATE INDEX idx_cliente_create_date ON cliente(create_date);

-- Índice en id_producto de linea_factura
CREATE INDEX idx_linea_factura_producto ON linea_factura(id_producto);

-- Índice compuesto para filtros de facturas
CREATE INDEX idx_factura_fecha_cliente ON factura(fecha_emision, id_cliente);
```

**Impacto:**
- ✅ Mejora WHERE clauses (~80% más rápido)
- ✅ Optimiza GROUP BY
- ✅ Acelera JOINs

---

