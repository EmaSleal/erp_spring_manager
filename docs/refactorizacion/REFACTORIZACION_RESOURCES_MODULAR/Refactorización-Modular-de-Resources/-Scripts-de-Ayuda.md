## 🔧 Scripts de Ayuda

### Script para buscar y reemplazar en templates

```powershell
# Actualizar referencias CSS compartidos
Get-ChildItem -Path "src/main/resources/templates" -Filter "*.html" -Recurse | ForEach-Object {
    (Get-Content $_.FullName) -replace '@\{/css/common\.css\}', '@{/shared/css/common.css}' | Set-Content $_.FullName
    (Get-Content $_.FullName) -replace '@\{/css/forms\.css\}', '@{/shared/css/forms.css}' | Set-Content $_.FullName
    (Get-Content $_.FullName) -replace '@\{/css/navbar\.css\}', '@{/shared/css/navbar.css}' | Set-Content $_.FullName
    # ... etc
}

# Actualizar referencias JS compartidos
Get-ChildItem -Path "src/main/resources/templates" -Filter "*.html" -Recurse | ForEach-Object {
    (Get-Content $_.FullName) -replace '@\{/js/common\.js\}', '@{/shared/js/common.js}' | Set-Content $_.FullName
    (Get-Content $_.FullName) -replace '@\{/js/sidebar\.js\}', '@{/shared/js/sidebar.js}' | Set-Content $_.FullName
    # ... etc
}
```

### Script para actualizar controladores

```powershell
# Actualizar returns en controladores
Get-ChildItem -Path "src/main/java" -Filter "*Controller.java" -Recurse | ForEach-Object {
    (Get-Content $_.FullName) -replace 'return "clientes/', 'return "modules/cliente/' | Set-Content $_.FullName
    (Get-Content $_.FullName) -replace 'return "productos/', 'return "modules/producto/' | Set-Content $_.FullName
    (Get-Content $_.FullName) -replace 'return "facturas/', 'return "modules/facturacion/' | Set-Content $_.FullName
    # ... etc
}
```

---

**¿Procedemos con la reorganización?** 🚀

**Opciones:**
1. ✅ **Ejecutar Fase 1** (Preparación) - 15 min
2. ✅ **Ejecutar Fases 1-2** (Preparación + CSS) - 45 min
3. ✅ **Ejecutar Fases 1-3** (Preparación + CSS + JS) - 1h 30min
4. ✅ **Ejecutar completo** (Todas las fases) - 3h
5. ⏸️ **Revisar más antes de empezar**
