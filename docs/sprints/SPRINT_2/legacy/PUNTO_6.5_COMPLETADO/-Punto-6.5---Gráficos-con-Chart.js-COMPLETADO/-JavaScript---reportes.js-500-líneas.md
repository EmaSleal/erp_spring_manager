## 🎨 JavaScript - reportes.js (500+ líneas)

### **Configuración Global**

```javascript
// Colores corporativos
const COLORS = {
    primary: '#0d6efd',
    success: '#198754',
    warning: '#ffc107',
    danger: '#dc3545',
    info: '#0dcaf0',
    secondary: '#6c757d',
    gradient: {
        blue: ['rgba(13, 110, 253, 0.8)', 'rgba(13, 110, 253, 0.2)'],
        green: ['rgba(25, 135, 84, 0.8)', 'rgba(25, 135, 84, 0.2)'],
        orange: ['rgba(255, 193, 7, 0.8)', 'rgba(255, 193, 7, 0.2)']
    }
};

// Configuración global de Chart.js
Chart.defaults.font.family = "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif";
Chart.defaults.font.size = 12;
Chart.defaults.color = '#666';
```

---

### **Gráfico 1: Ventas por Mes (Línea)**

```javascript
function cargarGraficoVentasPorMes() {
    fetch('/reportes/api/ventas-por-mes?meses=12')
        .then(response => response.json())
        .then(data => renderizarGraficoVentasPorMes(data))
        .catch(error => {
            console.error('Error:', error);
            mostrarErrorGrafico('ventasPorMesChart', 'Error al cargar datos');
        });
}

function renderizarGraficoVentasPorMes(data) {
    const ctx = document.getElementById('ventasPorMesChart');
    
    // Crear gradiente
    const gradient = ctx.getContext('2d').createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, COLORS.gradient.blue[0]);
    gradient.addColorStop(1, COLORS.gradient.blue[1]);

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Ventas (S/)',
                data: data.data,
                backgroundColor: gradient,
                borderColor: COLORS.primary,
                borderWidth: 3,
                fill: true,
                tension: 0.4,
                pointRadius: 5,
                pointHoverRadius: 7,
                pointBackgroundColor: COLORS.primary,
                pointBorderColor: '#fff',
                pointBorderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return 'Ventas: S/ ' + context.parsed.y.toLocaleString('es-PE', {
                                minimumFractionDigits: 2,
                                maximumFractionDigits: 2
                            });
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return 'S/ ' + value.toLocaleString('es-PE');
                        }
                    }
                }
            },
            animation: {
                duration: 1500,
                easing: 'easeInOutQuart'
            }
        }
    });
}
```

**Características:**
- Gradiente de azul (corporativo) degradado
- Línea suave con `tension: 0.4`
- Puntos circulares con borde blanco
- Tooltip formateado como moneda
- Eje Y con formato de moneda
- Animación de 1.5 segundos

---

### **Gráfico 2: Clientes Nuevos (Barras)**

```javascript
function cargarGraficoClientesNuevos() {
    fetch('/reportes/api/clientes-nuevos?meses=12')
        .then(response => response.json())
        .then(data => renderizarGraficoClientesNuevos(data))
        .catch(error => {
            console.error('Error:', error);
            mostrarErrorGrafico('clientesNuevosChart', 'Error al cargar datos');
        });
}

function renderizarGraficoClientesNuevos(data) {
    const ctx = document.getElementById('clientesNuevosChart');

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Clientes Nuevos',
                data: data.data,
                backgroundColor: COLORS.success,
                borderColor: 'rgba(25, 135, 84, 1)',
                borderWidth: 2,
                borderRadius: 6,
                hoverBackgroundColor: 'rgba(25, 135, 84, 0.9)'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1
                    }
                }
            },
            animation: {
                duration: 1500,
                easing: 'easeInOutQuart'
            }
        }
    });
}
```

**Características:**
- Barras verticales en verde (éxito)
- Bordes redondeados (`borderRadius: 6`)
- Efecto hover con cambio de color
- Eje Y con incrementos de 1 en 1
- Responsive y animado

---

### **Gráfico 3: Productos Más Vendidos (Barras Horizontales)**

```javascript
function cargarGraficoProductosMasVendidos() {
    fetch('/reportes/api/productos-mas-vendidos?limite=10')
        .then(response => response.json())
        .then(data => renderizarGraficoProductosMasVendidos(data))
        .catch(error => {
            console.error('Error:', error);
            mostrarErrorGrafico('productosMasVendidosChart', 'Error al cargar datos');
        });
}

function renderizarGraficoProductosMasVendidos(data) {
    const ctx = document.getElementById('productosMasVendidosChart');

    // Crear array de colores para cada barra (arcoíris)
    const colors = data.data.map((_, index) => {
        const hue = (index * 360 / data.data.length);
        return `hsla(${hue}, 70%, 60%, 0.8)`;
    });

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Cantidad Vendida',
                data: data.data,
                backgroundColor: colors,
                borderColor: colors.map(c => c.replace('0.8', '1')),
                borderWidth: 2,
                borderRadius: 6
            }]
        },
        options: {
            indexAxis: 'y', // Barras horizontales
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    beginAtZero: true
                }
            },
            animation: {
                duration: 1500,
                easing: 'easeInOutQuart'
            }
        }
    });
}
```

**Características:**
- Barras horizontales (`indexAxis: 'y'`)
- Colores dinámicos en espectro arcoíris
- Cada producto tiene un color único
- Tooltip con formato de cantidad
- Perfecto para comparar rankings

---

### **Gráfico 4: Ventas Filtradas (Dinámica)**

Implementado en `reportes/ventas.html` con datos inline de Thymeleaf:

```javascript
function renderizarGraficoVentas() {
    const ctx = document.getElementById('ventasChart');
    if (!ctx) return;

    // Datos de ventas (Thymeleaf inline)
    const facturas = /*[[${facturas}]]*/ [];
    
    if (facturas.length === 0) return;

    // Agrupar ventas por día
    const ventasPorDia = {};
    facturas.forEach(factura => {
        const fecha = new Date(factura.fechaEmision).toLocaleDateString('es-PE');
        if (!ventasPorDia[fecha]) {
            ventasPorDia[fecha] = 0;
        }
        ventasPorDia[fecha] += parseFloat(factura.total || 0);
    });

    // Convertir a arrays
    const labels = Object.keys(ventasPorDia);
    const data = Object.values(ventasPorDia);

    // Crear gráfico
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Ventas (S/)',
                data: data,
                backgroundColor: gradient,
                borderColor: '#0d6efd',
                borderWidth: 3,
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            // ... opciones similares al gráfico 1
        }
    });
}
```

**Características:**
- Se carga con datos de Thymeleaf (inline)
- Agrupa ventas por día automáticamente
- Se actualiza según los filtros aplicados
- Solo se muestra si hay datos

---

