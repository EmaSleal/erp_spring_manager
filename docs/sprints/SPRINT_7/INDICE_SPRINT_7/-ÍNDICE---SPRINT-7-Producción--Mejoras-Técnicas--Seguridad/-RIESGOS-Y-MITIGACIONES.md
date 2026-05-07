## ⚠️ RIESGOS Y MITIGACIONES

### Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Migración de username rompe login | Alta | Crítico | Tests exhaustivos, rollback plan, migración gradual |
| Timestamp a LocalDateTime afecta queries | Alta | Alto | Revisar todos los queries, tests de regresión |
| 2FA complica UX | Media | Medio | Hacer opcional por usuario, guía clara |
| Producción innecesaria para mayoría | Alta | Bajo | Marcar como OPCIONAL, priorizar Fases 2 y 3 |
| Migración de datos falla | Media | Crítico | Backup completo antes, dry-run en staging |

---

