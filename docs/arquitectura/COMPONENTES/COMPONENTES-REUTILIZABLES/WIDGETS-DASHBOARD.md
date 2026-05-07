##  WIDGETS DASHBOARD

###  Ubicación
```
templates/dashboard/dashboard.html
static/css/dashboard.css
static/js/dashboard.js
```

###  Propósito
Widgets de estadísticas con:
- Título
- Valor numérico
- Icono
- Color personalizado

###  Uso

```html
<div class="stats-grid">
    <!-- Widget 1 -->
    <div class="stat-card">
        <div class="stat-icon" style="background-color: #2196F3;">
            <i class="fas fa-users"></i>
        </div>
        <div class="stat-content">
            <div class="stat-value" th:text="${totalClientes}">0</div>
            <div class="stat-label">Clientes</div>
        </div>
    </div>
    
    <!-- Widget 2 -->
    <div class="stat-card">
        <div class="stat-icon" style="background-color: #4CAF50;">
            <i class="fas fa-box"></i>
        </div>
        <div class="stat-content">
            <div class="stat-value" th:text="${totalProductos}">0</div>
            <div class="stat-label">Productos</div>
        </div>
    </div>
</div>
```

###  Estilos (dashboard.css)

```css
.stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.5rem;
    margin-bottom: 2rem;
}

.stat-card {
    background: white;
    border-radius: 12px;
    padding: 1.5rem;
    display: flex;
    align-items: center;
    gap: 1rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.stat-icon i {
    font-size: 1.8rem;
    color: white;
}

.stat-content {
    flex: 1;
}

.stat-value {
    font-size: 2rem;
    font-weight: 700;
    color: #212121;
    line-height: 1;
}

.stat-label {
    font-size: 0.9rem;
    color: #757575;
    margin-top: 0.25rem;
}
```

###  Auto-refresh (dashboard.js)

```javascript
// Actualizar estadísticas cada 30 segundos
setInterval(() => {
    fetch('/api/dashboard/stats')
        .then(res => res.json())
        .then(data => {
            document.querySelectorAll('.stat-value').forEach((el, index) => {
                el.textContent = Object.values(data)[index];
            });
        });
}, 30000);
```

---

