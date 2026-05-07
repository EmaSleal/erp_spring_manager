## 📋 Template HTML (`form.html`)

El template ya tenía el select correcto:

```html
<div class="col-md-6 mb-3">
    <label for="entregado" class="form-label">
        <i class="fas fa-truck text-primary me-2"></i>Estado de Entrega
    </label>
    <select id="entregado" name="entregado" class="form-select">
        <option th:selected="${factura.entregado == true}" value="true">Entregado</option>
        <option th:selected="${factura.entregado == false}" value="false">Pendiente</option>
    </select>
</div>
```

**Importante:** Los valores son strings `"true"` y `"false"`, por eso en JavaScript se convierte con:
```javascript
entregadoSelect.value === 'true'
```

---

