## ⚠️ RIESGOS Y MITIGACIONES

### Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| API de tasas caída | Media | Medio | Caché de 24h, tasas manuales de respaldo |
| Complejidad conversión histórica | Alta | Medio | Validación exhaustiva, tests unitarios |
| Kardex con alto volumen de datos | Media | Medio | Paginación, índices en BD |
| Integración multi-divisa con FE CR | Baja | Alto | Convertir todo a CRC antes de facturar |
| Enum `AJUSTAR_INVENTARIO` con lógica no documentada | Alta | Bajo | Revisar código, documentar antes de usar |

---

