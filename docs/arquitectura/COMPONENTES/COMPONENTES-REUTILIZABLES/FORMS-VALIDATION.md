##  FORMS VALIDATION

###  Ubicación
```
static/css/forms.css
static/js/common.js (initFormValidation)
```

###  Propósito
Formularios con validación en tiempo real:
- HTML5 validation
- Estados visuales (valid/invalid)
- Mensajes de error personalizados

###  Uso

```html
<form th:action="@{/ruta}" method="post" class="needs-validation" novalidate>
    <!-- CSRF Token -->
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    
    <!-- Campo con validación -->
    <div class="mb-3">
        <label for="nombre" class="form-label">
            <i class="fas fa-user me-1"></i>Nombre
        </label>
        <input 
            type="text" 
            class="form-control" 
            id="nombre" 
            name="nombre"
            required
            minlength="3"
            maxlength="100"
            placeholder="Ingrese el nombre">
        <div class="invalid-feedback">
            El nombre debe tener entre 3 y 100 caracteres
        </div>
        <div class="valid-feedback">
            ¡Correcto!
        </div>
    </div>
    
    <!-- Botones -->
    <div class="d-flex gap-2">
        <button type="submit" class="btn btn-primary">
            <i class="fas fa-save me-1"></i>Guardar
        </button>
        <a th:href="@{/ruta-volver}" class="btn btn-secondary">
            <i class="fas fa-times me-1"></i>Cancelar
        </a>
    </div>
</form>

<script>
    // Inicializar validación
    document.addEventListener('DOMContentLoaded', () => {
        AppUtils.initFormValidation();
    });
</script>
```

###  JavaScript (common.js)

```javascript
const AppUtils = {
    // Inicializar validación de formularios
    initFormValidation: function() {
        const forms = document.querySelectorAll('.needs-validation');
        
        forms.forEach(form => {
            form.addEventListener('submit', (event) => {
                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                
                form.classList.add('was-validated');
            }, false);
            
            // Validación en tiempo real
            form.querySelectorAll('input, textarea, select').forEach(input => {
                input.addEventListener('blur', () => {
                    if (input.value) {
                        input.classList.add('was-validated');
                    }
                });
            });
        });
    }
};
```

###  Validaciones Comunes

#### **Email:**
```html
<input 
    type="email" 
    pattern="[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$"
    required>
<div class="invalid-feedback">
    Ingrese un email válido
</div>
```

#### **Teléfono (10 dígitos):**
```html
<input 
    type="tel" 
    pattern="[0-9]{10}"
    required>
<div class="invalid-feedback">
    El teléfono debe tener 10 dígitos
</div>
```

#### **Contraseña (mínimo 6 caracteres):**
```html
<input 
    type="password" 
    minlength="6"
    required>
<div class="invalid-feedback">
    La contraseña debe tener al menos 6 caracteres
</div>
```

#### **Número positivo:**
```html
<input 
    type="number" 
    min="0"
    step="0.01"
    required>
<div class="invalid-feedback">
    Ingrese un número positivo
</div>
```

---

