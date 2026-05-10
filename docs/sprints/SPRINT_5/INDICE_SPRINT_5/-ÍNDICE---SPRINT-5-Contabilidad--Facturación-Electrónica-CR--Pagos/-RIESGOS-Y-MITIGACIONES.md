## ⚠️ RIESGOS Y MITIGACIONES

### Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Certificado de firma no disponible | Media | Alto | Trabajar con Sandbox sin firma temporal |
| Complejidad del XML v4.4 | ✅ **RESUELTO** | - | Análisis completado, XSD validados |
| Datos faltantes en BD | ✅ **IDENTIFICADO** | Medio | Fases 4-7 con scripts SQL listos |
| API de Hacienda caída | Baja | Alto | Circuit breaker, cola de reintentos |
| Testing complejo de FE | Alta | Medio | Mocks extensivos, Sandbox dedicado |
| Tiempo insuficiente | Media | Alto | 20-26h estimadas, priorizar migraciones |
| Catálogo CABYS extenso | Alta | Medio | Importar solo categorías relevantes inicialmente |

---

