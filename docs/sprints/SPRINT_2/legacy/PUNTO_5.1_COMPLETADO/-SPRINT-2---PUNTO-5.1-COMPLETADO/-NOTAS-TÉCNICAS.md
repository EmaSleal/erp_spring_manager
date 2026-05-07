## 💡 NOTAS TÉCNICAS

### **¿Por qué usar variables de entorno?**

1. **Seguridad:** Las credenciales no se guardan en el código
2. **Flexibilidad:** Fácil cambiar configuración sin recompilar
3. **Entornos:** Diferentes configuraciones para dev/test/prod
4. **Secrets:** Compatible con Docker Secrets, Kubernetes, etc.

### **¿Por qué Gmail por defecto?**

1. **Disponibilidad:** Casi todos tienen una cuenta de Gmail
2. **Gratuito:** 500 emails/día sin costo
3. **Confiabilidad:** Alta disponibilidad de Google
4. **Facilidad:** Simple de configurar

### **¿Contraseña de aplicación vs contraseña normal?**

**Contraseña de aplicación:**
- ✅ Más segura (limitada a una aplicación)
- ✅ Revocable sin cambiar contraseña principal
- ✅ No expone contraseña principal
- ✅ Requerida si tienes 2FA activo

**Contraseña normal:**
- ❌ Menos segura
- ❌ No recomendada
- ❌ No funciona con 2FA

---

