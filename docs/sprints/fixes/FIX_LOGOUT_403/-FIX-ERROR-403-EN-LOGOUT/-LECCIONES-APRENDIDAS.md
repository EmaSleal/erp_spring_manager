## 📚 LECCIONES APRENDIDAS

1. **Seguir convenciones del framework:** Spring Security tiene convenciones (como `/logout`) que es mejor seguir
2. **Leer la documentación:** El nombre del parámetro CSRF es `_csrf`, no una transformación del header
3. **Testing temprano:** Probar el logout inmediatamente después de implementarlo habría detectado este error antes
4. **Consistencia:** Si cambias una URL en el backend, actualízala en el frontend

---

