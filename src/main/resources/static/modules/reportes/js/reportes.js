/**
 * ============================================================================
 * REPORTES.JS - JavaScript para Gráficos de Reportes
 * WhatsApp Orders Manager - Sprint 2 Fase 6
 * ============================================================================
 * Funcionalidades:
 * - Carga de datos desde API REST
 * - Renderizado de gráficos con Chart.js
 * - Configuración de estilos y colores
 * - Gráficos: Ventas por mes, Clientes nuevos, Productos más vendidos
 * ============================================================================
 * @author GitHub Copilot
 * @version 1.0
 * @since 18/10/2025
 */

// ============================================================================
// CONFIGURACIÓN GLOBAL DE CHART.JS
// ============================================================================

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

// ============================================================================
// GRÁFICO 1: VENTAS POR MES
// ============================================================================

/**
 * Carga y renderiza el gráfico de ventas por mes (línea)
 */
function cargarGraficoVentasPorMes() {
    fetch('/reportes/api/ventas-por-mes?meses=12')
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al cargar datos de ventas');
            }
            return response.json();
        })
        .then(data => {
            renderizarGraficoVentasPorMes(data);
        })
        .catch(error => {
            console.error('Error al cargar gráfico de ventas:', error);
            mostrarErrorGrafico('ventasPorMesChart', 'Error al cargar datos de ventas');
        });
}

/**
 * Renderiza el gráfico de ventas por mes
 * @param {Object} data - Datos con labels y data
 */
function renderizarGraficoVentasPorMes(data) {
    const ctx = document.getElementById('ventasPorMesChart');
    if (!ctx) {
        console.error('Canvas ventasPorMesChart no encontrado');
        return;
    }

    // Crear gradiente
    const gradient = ctx.getContext('2d').createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, COLORS.gradient.blue[0]);
    gradient.addColorStop(1, COLORS.gradient.blue[1]);

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: data.labels,
            datasets: [{
                label: `Ventas (${window.simboloMoneda || '₡'})`,
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
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        font: {
                            size: 13,
                            weight: 'bold'
                        },
                        padding: 15
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    cornerRadius: 8,
                    titleFont: {
                        size: 14,
                        weight: 'bold'
                    },
                    bodyFont: {
                        size: 13
                    },
                    callbacks: {
                        label: function(context) {
                            return `Ventas: ${window.simboloMoneda || '₡'} ` + context.parsed.y.toLocaleString('es-CR', {
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
                            return `${window.simboloMoneda || '₡'} ` + value.toLocaleString('es-CR');
                        },
                        font: {
                            size: 11
                        }
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    }
                },
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        font: {
                            size: 11
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

// ============================================================================
// GRÁFICO 2: CLIENTES NUEVOS POR MES
// ============================================================================

/**
 * Carga y renderiza el gráfico de clientes nuevos por mes (barras)
 */
function cargarGraficoClientesNuevos() {
    fetch('/reportes/api/clientes-nuevos?meses=12')
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al cargar datos de clientes nuevos');
            }
            return response.json();
        })
        .then(data => {
            renderizarGraficoClientesNuevos(data);
        })
        .catch(error => {
            console.error('Error al cargar gráfico de clientes nuevos:', error);
            mostrarErrorGrafico('clientesNuevosChart', 'Error al cargar datos de clientes');
        });
}

/**
 * Renderiza el gráfico de clientes nuevos
 * @param {Object} data - Datos con labels y data
 */
function renderizarGraficoClientesNuevos(data) {
    const ctx = document.getElementById('clientesNuevosChart');
    if (!ctx) {
        console.error('Canvas clientesNuevosChart no encontrado');
        return;
    }

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
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        font: {
                            size: 13,
                            weight: 'bold'
                        },
                        padding: 15
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    cornerRadius: 8,
                    titleFont: {
                        size: 14,
                        weight: 'bold'
                    },
                    bodyFont: {
                        size: 13
                    },
                    callbacks: {
                        label: function(context) {
                            return 'Clientes: ' + context.parsed.y;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1,
                        font: {
                            size: 11
                        }
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    }
                },
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        font: {
                            size: 11
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

// ============================================================================
// GRÁFICO 3: PRODUCTOS MÁS VENDIDOS
// ============================================================================

/**
 * Carga y renderiza el gráfico de productos más vendidos (barras horizontales)
 */
function cargarGraficoProductosMasVendidos() {
    fetch('/reportes/api/productos-mas-vendidos?limite=10')
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al cargar datos de productos');
            }
            return response.json();
        })
        .then(data => {
            renderizarGraficoProductosMasVendidos(data);
        })
        .catch(error => {
            console.error('Error al cargar gráfico de productos:', error);
            mostrarErrorGrafico('productosMasVendidosChart', 'Error al cargar datos de productos');
        });
}

/**
 * Renderiza el gráfico de productos más vendidos
 * @param {Object} data - Datos con labels y data
 */
function renderizarGraficoProductosMasVendidos(data) {
    const ctx = document.getElementById('productosMasVendidosChart');
    if (!ctx) {
        console.error('Canvas productosMasVendidosChart no encontrado');
        return;
    }

    // Crear array de colores para cada barra
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
            indexAxis: 'y', // Hace las barras horizontales
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        font: {
                            size: 13,
                            weight: 'bold'
                        },
                        padding: 15
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    cornerRadius: 8,
                    titleFont: {
                        size: 14,
                        weight: 'bold'
                    },
                    bodyFont: {
                        size: 13
                    },
                    callbacks: {
                        label: function(context) {
                            return 'Cantidad: ' + context.parsed.x.toLocaleString('es-PE');
                        }
                    }
                }
            },
            scales: {
                x: {
                    beginAtZero: true,
                    ticks: {
                        font: {
                            size: 11
                        }
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    }
                },
                y: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        font: {
                            size: 11
                        },
                        autoSkip: false
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

// ============================================================================
// GRÁFICO 4: ESTADO DE FACTURAS (PIE CHART)
// ============================================================================

/**
 * Carga y renderiza el gráfico de estado de facturas (pie/doughnut)
 */
function cargarGraficoEstadoFacturas() {
    fetch('/reportes/api/estado-facturas')
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al cargar datos de estado de facturas');
            }
            return response.json();
        })
        .then(data => {
            renderizarGraficoEstadoFacturas(data);
        })
        .catch(error => {
            console.error('Error al cargar gráfico de estado de facturas:', error);
            mostrarErrorGrafico('estadoFacturasChart', 'Error al cargar estado de facturas');
        });
}

/**
 * Renderiza el gráfico de estado de facturas
 * @param {Object} data - Datos con labels, data y backgroundColor
 */
function renderizarGraficoEstadoFacturas(data) {
    const ctx = document.getElementById('estadoFacturasChart');
    if (!ctx) {
        console.error('Canvas estadoFacturasChart no encontrado');
        return;
    }

    new Chart(ctx, {
        type: 'pie',
        data: {
            labels: data.labels,
            datasets: [{
                data: data.data,
                backgroundColor: data.backgroundColor || [
                    COLORS.success,  // Verde para Pagadas
                    COLORS.warning,  // Amarillo para Pendientes
                    COLORS.danger    // Rojo para Vencidas
                ],
                borderColor: '#fff',
                borderWidth: 2,
                hoverOffset: 10
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        padding: 15,
                        usePointStyle: true,
                        font: {
                            size: 12,
                            weight: 'bold'
                        }
                    }
                },
                title: {
                    display: true,
                    text: 'Estado de Facturas',
                    font: {
                        size: 16,
                        weight: 'bold'
                    },
                    padding: {
                        top: 10,
                        bottom: 20
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.parsed || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
    
    console.log('✅ Gráfico de estado de facturas renderizado');
}

// ============================================================================
// GRÁFICO 5: COMPARATIVA INGRESOS (MIXED CHART)
// ============================================================================

/**
 * Carga y renderiza el gráfico comparativo de ingresos (barras + línea)
 */
function cargarGraficoComparativaIngresos() {
    fetch('/reportes/api/comparativa-ingresos?meses=6')
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al cargar datos de comparativa de ingresos');
            }
            return response.json();
        })
        .then(data => {
            renderizarGraficoComparativaIngresos(data);
        })
        .catch(error => {
            console.error('Error al cargar gráfico comparativo:', error);
            mostrarErrorGrafico('comparativaIngresosChart', 'Error al cargar comparativa');
        });
}

/**
 * Renderiza el gráfico comparativo de ingresos
 * @param {Object} data - Datos con labels, datasets, promedio y cumplimiento
 */
function renderizarGraficoComparativaIngresos(data) {
    const ctx = document.getElementById('comparativaIngresosChart');
    if (!ctx) {
        console.error('Canvas comparativaIngresosChart no encontrado');
        return;
    }

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: data.labels,
            datasets: data.datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false
            },
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        padding: 15,
                        usePointStyle: true,
                        font: {
                            size: 12,
                            weight: 'bold'
                        }
                    }
                },
                title: {
                    display: true,
                    text: 'Ingresos vs Meta Promedio',
                    font: {
                        size: 16,
                        weight: 'bold'
                    },
                    padding: {
                        top: 10,
                        bottom: 20
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ': ';
                            }
                            if (context.parsed.y !== null) {
                                label += `${window.simboloMoneda || '₡'} ` + context.parsed.y.toLocaleString('es-CR', {
                                    minimumFractionDigits: 2,
                                    maximumFractionDigits: 2
                                });
                            }
                            
                            // Agregar porcentaje de cumplimiento si es el dataset de ingresos
                            if (context.datasetIndex === 0 && data.cumplimiento && data.cumplimiento[context.dataIndex]) {
                                const porcentaje = data.cumplimiento[context.dataIndex];
                                label += ` (${porcentaje}% de la meta)`;
                            }
                            
                            return label;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return `${window.simboloMoneda || '₡'} ` + value.toLocaleString('es-CR');
                        }
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
    
    console.log('✅ Gráfico comparativo de ingresos renderizado');
}

// ============================================================================
// FUNCIONES AUXILIARES
// ============================================================================

/**
 * Muestra un mensaje de error en el canvas del gráfico
 * @param {string} canvasId - ID del canvas
 * @param {string} mensaje - Mensaje de error
 */
function mostrarErrorGrafico(canvasId, mensaje) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);
    ctx.fillStyle = '#dc3545';
    ctx.font = '14px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(mensaje, width / 2, height / 2);
}

/**
 * Actualiza todos los gráficos
 */
function actualizarGraficos() {
    console.log('Actualizando todos los gráficos...');
    cargarGraficoVentasPorMes();
    cargarGraficoClientesNuevos();
    cargarGraficoProductosMasVendidos();
    cargarGraficoEstadoFacturas();
    cargarGraficoComparativaIngresos();
}

// ============================================================================
// EXPORTAR FUNCIONES GLOBALES
// ============================================================================

// Hacer funciones accesibles globalmente
window.cargarGraficoVentasPorMes = cargarGraficoVentasPorMes;
window.cargarGraficoClientesNuevos = cargarGraficoClientesNuevos;
window.cargarGraficoProductosMasVendidos = cargarGraficoProductosMasVendidos;
window.cargarGraficoEstadoFacturas = cargarGraficoEstadoFacturas;
window.cargarGraficoComparativaIngresos = cargarGraficoComparativaIngresos;
window.actualizarGraficos = actualizarGraficos;

console.log('reportes.js cargado correctamente');
