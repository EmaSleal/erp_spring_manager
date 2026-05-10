## ✅ CHECKLIST DE VALIDACIÓN

Antes de considerar las pruebas completas, verificar:

- [ ] Todos los métodos públicos de NotificacionService tienen test
- [ ] Eventos asíncronos probados con Awaitility
- [ ] Tests de email usan GreenMail (no envían emails reales)
- [ ] BD usa H2 en memoria (no afecta BD real)
- [ ] Tests son independientes (no dependen de orden)
- [ ] Tests limpian datos después de ejecutar (@Transactional)
- [ ] Cobertura > 70% en servicios críticos
- [ ] Al menos 1 test E2E completo funciona
- [ ] Tests pasan en CI/CD (si aplica)
- [ ] Documentación de tests actualizada

---

