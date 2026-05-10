## 🔧 TIPS DE USO

### Para IntelliJ IDEA

1. **Find & Replace in Files:**
   ```
   Ctrl + Shift + R
   ```

2. **Scope recomendado:**
   ```
   Whole Project
   ```

3. **File mask:**
   ```
   *.java
   ```

4. **Opciones:**
   - ✅ Case sensitive
   - ✅ Match case
   - ❌ Regex (a menos que lo necesites)

### Orden de Ejecución

1. ✅ Ejecutar primero los **Models** (Factura, Usuario, Cliente, etc.)
2. ✅ Luego **Repositories**
3. ✅ Luego **Services**
4. ✅ Luego **Controllers**
5. ✅ Finalmente **DTOs y Enums**

### Verificar Cambios

Después de cada grupo de reemplazos:
```bash
mvn clean compile
```

Si hay errores, usa IntelliJ:
```
Ctrl + Shift + F → Buscar el import antiguo
Alt + Enter → Import class
```

---

